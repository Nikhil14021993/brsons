package com.brsons.service;

import com.brsons.model.PurchaseOrder;
import com.brsons.model.PurchaseOrderItem;
import com.brsons.model.Product;
import com.brsons.model.Supplier;
import com.brsons.repository.PurchaseOrderRepository;
import com.brsons.repository.PurchaseOrderItemRepository;
import com.brsons.repository.ProductRepository;
import com.brsons.repository.SupplierRepository;
import com.brsons.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.brsons.model.CreditNote;
import com.brsons.repository.CreditNoteRepository;
import com.brsons.model.GoodsReceivedNote;
import com.brsons.repository.GRNRepository;

@Service
@Transactional
public class PurchaseOrderService {
    
    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;
    
    @Autowired
    private PurchaseOrderItemRepository purchaseOrderItemRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private CreditNoteRepository creditNoteRepository;
    
    @Autowired
    private GRNRepository grnRepository;
    
    @Autowired
    private InventoryService inventoryService;
    
    // Create new purchase order
    public PurchaseOrder createPurchaseOrder(PurchaseOrder purchaseOrder) {
        // Generate PO number if needed
        if (purchaseOrder.getPoNumber() == null || purchaseOrder.getPoNumber().trim().isEmpty()) {
            purchaseOrder.setPoNumber(generatePONumber());
        }

        purchaseOrder.setCreatedAt(LocalDateTime.now());
        purchaseOrder.setUpdatedAt(LocalDateTime.now());
        purchaseOrder.setStatus(PurchaseOrder.POStatus.DRAFT);
        purchaseOrder.setOrderDate(LocalDateTime.now());

        // Ensure children are linked to parent and totals calculated
        if (purchaseOrder.getOrderItems() != null) {
            // Re-link via setter so every item gets purchaseOrder set
            purchaseOrder.setOrderItems(purchaseOrder.getOrderItems());
            for (PurchaseOrderItem item : purchaseOrder.getOrderItems()) {
                item.calculateTotals();
            }
        }

        purchaseOrder.calculateTotals();

        // Single save — cascade will persist items
        return purchaseOrderRepository.save(purchaseOrder);
    }


    public List<PurchaseOrder> getAllWithItems() {
        return purchaseOrderRepository.findAllWithItems();
    }
    
    public Optional<PurchaseOrder> getByIdWithItems(Long id) {
        return purchaseOrderRepository.findByIdWithItems(id);
    }
    
    // Create new purchase order with items (separate method for controller use)
    public PurchaseOrder createPurchaseOrderWithItems(PurchaseOrder purchaseOrder, List<PurchaseOrderItem> orderItems) {
        if (purchaseOrder.getPoNumber() == null || purchaseOrder.getPoNumber().trim().isEmpty()) {
            purchaseOrder.setPoNumber(generatePONumber());
        }

        purchaseOrder.setCreatedAt(LocalDateTime.now());
        purchaseOrder.setUpdatedAt(LocalDateTime.now());
        purchaseOrder.setStatus(PurchaseOrder.POStatus.DRAFT);
        purchaseOrder.setOrderDate(LocalDateTime.now());

        // Attach items safely (links parent on each)
        purchaseOrder.setOrderItems(orderItems);

        if (purchaseOrder.getOrderItems() != null) {
            for (PurchaseOrderItem item : purchaseOrder.getOrderItems()) {
                item.calculateTotals();
            }
        }

        purchaseOrder.calculateTotals();
        return purchaseOrderRepository.save(purchaseOrder); // cascade persists items
    }
    
    // Update existing purchase order
    @Transactional
    public PurchaseOrder updatePurchaseOrder(Long id, PurchaseOrder details) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase Order not found with id: " + id));

        // Scalars
        po.setPoNumber(details.getPoNumber());
        po.setSupplier(details.getSupplier());
        po.setExpectedDeliveryDate(details.getExpectedDeliveryDate());
        po.setDeliveryAddress(details.getDeliveryAddress());
        po.setPaymentTerms(details.getPaymentTerms());
        po.setShippingCost(details.getShippingCost());
        po.setNotes(details.getNotes());
        po.setUpdatedAt(LocalDateTime.now());

        // Items (only if provided)
        if (details.getOrderItems() != null) {
            for (PurchaseOrderItem i : details.getOrderItems()) {
                i.calculateTotals();
            }
            // ✅ This clears and re-links children; orphanRemoval will delete old rows
            po.setOrderItems(details.getOrderItems());
        }

        po.calculateTotals();
        return purchaseOrderRepository.save(po);
    }
    
    // Get purchase order by ID
    public Optional<PurchaseOrder> getPurchaseOrderById(Long id) {
        return purchaseOrderRepository.findByIdWithItemsAndSupplier(id);
    }
    
    // Get all purchase orders
    public List<PurchaseOrder> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAllWithItemsAndSupplier();
    }
    
    // Get purchase orders by status
    public List<PurchaseOrder> getPurchaseOrdersByStatus(PurchaseOrder.POStatus status) {
        return purchaseOrderRepository.findByStatus(status);
    }
    
    // Get purchase orders by supplier
    public List<PurchaseOrder> getPurchaseOrdersBySupplier(Long supplierId) {
        return purchaseOrderRepository.findBySupplierId(supplierId);
    }
    
    // Search purchase orders
    public List<PurchaseOrder> searchPurchaseOrders(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllPurchaseOrders();
        }
        return purchaseOrderRepository.searchPOs(query.trim());
    }
    
    // Update purchase order status
    public PurchaseOrder updatePurchaseOrderStatus(Long id, PurchaseOrder.POStatus newStatus) {
        Optional<PurchaseOrder> existingPO = purchaseOrderRepository.findById(id);
        if (existingPO.isPresent()) {
            PurchaseOrder purchaseOrder = existingPO.get();
            PurchaseOrder.POStatus oldStatus = purchaseOrder.getStatus();
            
            // Validate status transition
            if (!canTransitionToStatus(purchaseOrder.getStatus(), newStatus)) {
                throw new IllegalArgumentException("Invalid status transition from " + purchaseOrder.getStatus() + " to " + newStatus);
            }
            
            purchaseOrder.setStatus(newStatus);
            purchaseOrder.setUpdatedAt(LocalDateTime.now());
            
            // Set approval info if being approved
            if (newStatus == PurchaseOrder.POStatus.APPROVED) {
                // This would typically come from the authenticated user
                purchaseOrder.setApprovedAt(LocalDateTime.now());
                
                // Reserve stock when PO is approved
                inventoryService.handlePOApproval(purchaseOrder);
            }
            
            // Handle stock reservation release if PO is cancelled
            if (newStatus == PurchaseOrder.POStatus.CANCELLED && oldStatus == PurchaseOrder.POStatus.APPROVED) {
                inventoryService.handlePOCancellation(purchaseOrder);
            }
            
            return purchaseOrderRepository.save(purchaseOrder);
        }
        throw new RuntimeException("Purchase Order not found with id: " + id);
    }
    
    // Delete purchase order (soft delete by setting status to CANCELLED)
    public PurchaseOrder deletePurchaseOrder(Long id) {
        return updatePurchaseOrderStatus(id, PurchaseOrder.POStatus.CANCELLED);
    }
    
    // Hard delete purchase order from database
    public void hardDeletePurchaseOrder(Long id) {
        Optional<PurchaseOrder> existingPO = purchaseOrderRepository.findById(id);
        if (existingPO.isPresent()) {
            PurchaseOrder po = existingPO.get();
            
            // Only allow deletion of POs in DRAFT or CANCELLED status
            if (po.getStatus() != PurchaseOrder.POStatus.DRAFT && po.getStatus() != PurchaseOrder.POStatus.CANCELLED) {
                throw new IllegalStateException("Cannot delete Purchase Order in status: " + po.getStatus() + 
                    ". Only DRAFT or CANCELLED POs can be deleted.");
            }
            
            // First, delete associated CreditNotes to avoid foreign key constraint violations
            List<CreditNote> associatedCreditNotes = creditNoteRepository.findByPurchaseOrderIdOrderByCreatedAtDesc(po.getId());
            if (!associatedCreditNotes.isEmpty()) {
                for (CreditNote creditNote : associatedCreditNotes) {
                    creditNoteRepository.delete(creditNote);
                }
            }

            // Then, delete associated GoodsReceivedNotes
            List<GoodsReceivedNote> associatedGRNs = grnRepository.findByPurchaseOrderId(po.getId());
            if (!associatedGRNs.isEmpty()) {
                for (GoodsReceivedNote grn : associatedGRNs) {
                    grnRepository.delete(grn);
                }
            }
            
            // Now delete the PurchaseOrder (this will also delete PurchaseOrderItems due to cascade)
            purchaseOrderRepository.deleteById(id);
        } else {
            throw new RuntimeException("Purchase Order not found with id: " + id);
        }
    }
    
    // Get purchase order statistics
    public PurchaseOrderStatistics getPurchaseOrderStatistics() {
        long totalPOs = purchaseOrderRepository.count();
        long draftPOs = purchaseOrderRepository.countByStatus(PurchaseOrder.POStatus.DRAFT);
        long pendingApprovalPOs = purchaseOrderRepository.countByStatus(PurchaseOrder.POStatus.PENDING_APPROVAL);
        long approvedPOs = purchaseOrderRepository.countByStatus(PurchaseOrder.POStatus.APPROVED);
        long orderedPOs = purchaseOrderRepository.countByStatus(PurchaseOrder.POStatus.ORDERED);
        long partiallyReceivedPOs = purchaseOrderRepository.countByStatus(PurchaseOrder.POStatus.PARTIALLY_RECEIVED);
        long fullyReceivedPOs = purchaseOrderRepository.countByStatus(PurchaseOrder.POStatus.FULLY_RECEIVED);
        long cancelledPOs = purchaseOrderRepository.countByStatus(PurchaseOrder.POStatus.CANCELLED);
        
        return new PurchaseOrderStatistics(
            totalPOs, draftPOs, pendingApprovalPOs, approvedPOs, 
            orderedPOs, partiallyReceivedPOs, fullyReceivedPOs, cancelledPOs
        );
    }
    
    // Validate status transition
    private boolean canTransitionToStatus(PurchaseOrder.POStatus currentStatus, PurchaseOrder.POStatus newStatus) {
        switch (currentStatus) {
            case DRAFT:
                return newStatus == PurchaseOrder.POStatus.PENDING_APPROVAL || newStatus == PurchaseOrder.POStatus.CANCELLED;
            case PENDING_APPROVAL:
                return newStatus == PurchaseOrder.POStatus.APPROVED || newStatus == PurchaseOrder.POStatus.CANCELLED;
            case APPROVED:
                return newStatus == PurchaseOrder.POStatus.ORDERED || newStatus == PurchaseOrder.POStatus.CANCELLED;
            case ORDERED:
                return newStatus == PurchaseOrder.POStatus.PARTIALLY_RECEIVED || newStatus == PurchaseOrder.POStatus.FULLY_RECEIVED || newStatus == PurchaseOrder.POStatus.CANCELLED;
            case PARTIALLY_RECEIVED:
                return newStatus == PurchaseOrder.POStatus.FULLY_RECEIVED || newStatus == PurchaseOrder.POStatus.CANCELLED;
            case FULLY_RECEIVED:
                return newStatus == PurchaseOrder.POStatus.CLOSED;
            case CANCELLED:
            case CLOSED:
                return false; // Terminal states
            default:
                return false;
        }
    }
    
    // Generate unique PO number
    private String generatePONumber() {
        String prefix = "PO";
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8); // Last 4 digits
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + timestamp + random;
    }
    
    // Statistics class
    public static class PurchaseOrderStatistics {
        private final long totalPOs;
        private final long draftPOs;
        private final long pendingApprovalPOs;
        private final long approvedPOs;
        private final long orderedPOs;
        private final long partiallyReceivedPOs;
        private final long fullyReceivedPOs;
        private final long cancelledPOs;
        
        public PurchaseOrderStatistics(long totalPOs, long draftPOs, long pendingApprovalPOs, 
                                    long approvedPOs, long orderedPOs, long partiallyReceivedPOs, 
                                    long fullyReceivedPOs, long cancelledPOs) {
            this.totalPOs = totalPOs;
            this.draftPOs = draftPOs;
            this.pendingApprovalPOs = pendingApprovalPOs;
            this.approvedPOs = approvedPOs;
            this.orderedPOs = orderedPOs;
            this.partiallyReceivedPOs = partiallyReceivedPOs;
            this.fullyReceivedPOs = fullyReceivedPOs;
            this.cancelledPOs = cancelledPOs;
        }
        
        // Getters
        public long getTotalPOs() { return totalPOs; }
        public long getDraftPOs() { return draftPOs; }
        public long getPendingApprovalPOs() { return pendingApprovalPOs; }
        public long getApprovedPOs() { return approvedPOs; }
        public long getOrderedPOs() { return orderedPOs; }
        public long getPartiallyReceivedPOs() { return partiallyReceivedPOs; }
        public long getFullyReceivedPOs() { return fullyReceivedPOs; }
        public long getCancelledPOs() { return cancelledPOs; }
    }
}
