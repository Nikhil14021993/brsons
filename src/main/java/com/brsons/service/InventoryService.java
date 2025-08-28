package com.brsons.service;

import com.brsons.model.Product;
import com.brsons.model.StockMovement;
import com.brsons.model.PurchaseOrder;
import com.brsons.model.GoodsReceivedNote;
import com.brsons.model.CreditNote;
import com.brsons.repository.ProductRepository;
import com.brsons.repository.StockMovementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class InventoryService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private StockMovementRepository stockMovementRepository;
    
    // ==================== BASIC STOCK OPERATIONS ====================
    
    /**
     * Increase stock for a product
     */
    public void increaseStock(Long productId, int quantity, String reason, String referenceType, Long referenceId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        
        int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        int newStock = currentStock + quantity;
        
        product.setStockQuantity(newStock);
        product.setLastUpdated(LocalDateTime.now());
        productRepository.save(product);
        
        // Record stock movement
        recordStockMovement(product, quantity, "IN", reason, referenceType, referenceId, currentStock, newStock);
    }
    
    /**
     * Decrease stock for a product
     */
    public void decreaseStock(Long productId, int quantity, String reason, String referenceType, Long referenceId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        
        int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        if (currentStock < quantity) {
            throw new IllegalStateException("Insufficient stock for product " + product.getProductName() + 
                ". Current: " + currentStock + ", Requested: " + quantity);
        }
        
        int newStock = currentStock - quantity;
        product.setStockQuantity(newStock);
        product.setLastUpdated(LocalDateTime.now());
        productRepository.save(product);
        
        // Record stock movement
        recordStockMovement(product, quantity, "OUT", reason, referenceType, referenceId, currentStock, newStock);
    }
    
    /**
     * Reserve stock for a purchase order (doesn't reduce actual stock)
     */
    public void reserveStock(Long productId, int quantity, Long purchaseOrderId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        
        int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        int currentReserved = product.getReservedQuantity() != null ? product.getReservedQuantity() : 0;
        
        if (currentStock < quantity) {
            throw new IllegalStateException("Insufficient stock for reservation. Available: " + currentStock + ", Requested: " + quantity);
        }
        
        int newReserved = currentReserved + quantity;
        product.setReservedQuantity(newReserved);
        product.setLastUpdated(LocalDateTime.now());
        productRepository.save(product);
        
        // Record stock movement
        recordStockMovement(product, quantity, "RESERVE", "Stock reserved for PO", "PURCHASE_ORDER", purchaseOrderId, currentStock, currentStock);
    }
    
    /**
     * Release reserved stock
     */
    public void releaseReservedStock(Long productId, int quantity, Long purchaseOrderId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        
        int currentReserved = product.getReservedQuantity() != null ? product.getReservedQuantity() : 0;
        if (currentReserved < quantity) {
            throw new IllegalStateException("Insufficient reserved stock. Reserved: " + currentReserved + ", Requested: " + quantity);
        }
        
        int newReserved = currentReserved - quantity;
        product.setReservedQuantity(newReserved);
        product.setLastUpdated(LocalDateTime.now());
        productRepository.save(product);
        
        // Record stock movement
        recordStockMovement(product, quantity, "RELEASE", "Stock reservation released", "PURCHASE_ORDER", purchaseOrderId, 
            product.getStockQuantity(), product.getStockQuantity());
    }
    
    // ==================== BUSINESS LOGIC INTEGRATION ====================
    
    /**
     * Handle GRN status changes and update stock accordingly
     */
    public void handleGRNStatusChange(GoodsReceivedNote grn, String oldStatus, String newStatus) {
        if ("DRAFT".equals(oldStatus) && "RECEIVED".equals(newStatus)) {
            // Goods received - increase stock
            for (var item : grn.getGrnItems()) {
                increaseStock(
                    item.getProduct().getId(),
                    item.getReceivedQuantity(),
                    "Goods received from GRN",
                    "GRN",
                    grn.getId()
                );
            }
        } else if ("INSPECTED".equals(oldStatus) && "REJECTED".equals(newStatus)) {
            // Goods rejected - decrease stock (return to supplier)
            for (var item : grn.getGrnItems()) {
                decreaseStock(
                    item.getProduct().getId(),
                    item.getReceivedQuantity(),
                    "Goods rejected and returned to supplier",
                    "GRN",
                    grn.getId()
                );
            }
        }
    }
    
    /**
     * Handle Credit Note creation and update stock
     */
    public void handleCreditNoteCreation(CreditNote creditNote) {
        for (var item : creditNote.getCreditNoteItems()) {
            decreaseStock(
                item.getProduct().getId(),
                item.getQuantity(),
                "Credit note - goods returned",
                "CREDIT_NOTE",
                creditNote.getId()
            );
        }
    }
    
    /**
     * Handle Purchase Order approval and reserve stock
     */
    public void handlePOApproval(PurchaseOrder purchaseOrder) {
        for (var item : purchaseOrder.getOrderItems()) {
            reserveStock(
                item.getProduct().getId(),
                item.getOrderedQuantity(),
                purchaseOrder.getId()
            );
        }
    }
    
    /**
     * Handle Purchase Order cancellation and release reserved stock
     */
    public void handlePOCancellation(PurchaseOrder purchaseOrder) {
        for (var item : purchaseOrder.getOrderItems()) {
            releaseReservedStock(
                item.getProduct().getId(),
                item.getOrderedQuantity(),
                purchaseOrder.getId()
            );
        }
    }
    
    // ==================== STOCK MOVEMENT TRACKING ====================
    
    private void recordStockMovement(Product product, int quantity, String movementType, String reason, 
                                   String referenceType, Long referenceId, int beforeQuantity, int afterQuantity) {
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setQuantity(quantity);
        movement.setMovementType(StockMovement.MovementType.valueOf(movementType));
        movement.setReason(reason);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setBeforeQuantity(beforeQuantity);
        movement.setAfterQuantity(afterQuantity);
        movement.setMovementDate(LocalDateTime.now());
        
        stockMovementRepository.save(movement);
    }
    
    // ==================== STOCK QUERIES ====================
    
    /**
     * Get current stock level for a product
     */
    public int getCurrentStock(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        return product.getStockQuantity() != null ? product.getStockQuantity() : 0;
    }
    
    /**
     * Get available stock (current - reserved)
     */
    public int getAvailableStock(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        
        int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        int reservedStock = product.getReservedQuantity() != null ? product.getReservedQuantity() : 0;
        
        return currentStock - reservedStock;
    }
    
    /**
     * Get stock movements for a product
     */
    public List<StockMovement> getStockMovements(Long productId) {
        if (productId != null) {
            return stockMovementRepository.findByProductIdOrderByMovementDateDesc(productId);
        } else {
            // Return all movements if no product specified
            return stockMovementRepository.findAll();
        }
    }
    
    /**
     * Get all stock movements
     */
    public List<StockMovement> getAllStockMovements() {
        return stockMovementRepository.findAll();
    }
    
    /**
     * Get low stock products (below threshold)
     */
    public List<Product> getLowStockProducts(int threshold) {
        return productRepository.findByStockQuantityLessThan(threshold);
    }
    
    /**
     * Get out of stock products
     */
    public List<Product> getOutOfStockProducts() {
        return productRepository.findByStockQuantityLessThanEqual(0);
    }
    
    /**
     * Get recent stock movements (last N days)
     */
    public List<StockMovement> getRecentMovements(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        return stockMovementRepository.findByMovementDateBetweenOrderByMovementDateDesc(startDate, LocalDateTime.now());
    }
    
    /**
     * Get stock movements for dashboard (limited count)
     */
    public List<StockMovement> getDashboardMovements(int limit) {
        List<StockMovement> allMovements = stockMovementRepository.findAll();
        if (allMovements.size() <= limit) {
            return allMovements;
        }
        return allMovements.subList(0, limit);
    }
    
    // ==================== STOCK VALUATION ====================
    
    /**
     * Calculate total stock value
     */
    public BigDecimal calculateTotalStockValue() {
        List<Product> products = productRepository.findAll();
        BigDecimal totalValue = BigDecimal.ZERO;
        
        for (Product product : products) {
            int stock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            BigDecimal price = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
            totalValue = totalValue.add(price.multiply(BigDecimal.valueOf(stock)));
        }
        
        return totalValue;
    }
    
    /**
     * Calculate stock value for a specific product
     */
    public BigDecimal calculateProductStockValue(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        
        int stock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        BigDecimal price = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
        
        return price.multiply(BigDecimal.valueOf(stock));
    }
}
