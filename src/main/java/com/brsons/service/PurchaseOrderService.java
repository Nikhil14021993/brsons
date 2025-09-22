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

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

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
            // ✅ This clears and re-links children; orphanRemoval will delete old rows
            po.setOrderItems(details.getOrderItems());
            // Now calculate totals after items are linked to PO
            for (PurchaseOrderItem i : po.getOrderItems()) {
                i.calculateTotals();
            }
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
    
    // Get purchase orders ready for GRN creation (exclude DRAFT, PENDING_APPROVAL, APPROVED)
    public List<PurchaseOrder> getPurchaseOrdersReadyForGRN() {
        List<PurchaseOrder> allPOs = purchaseOrderRepository.findAllWithItems();
        return allPOs.stream()
                .filter(po -> po.getStatus() != PurchaseOrder.POStatus.DRAFT &&
                             po.getStatus() != PurchaseOrder.POStatus.PENDING_APPROVAL &&
                             po.getStatus() != PurchaseOrder.POStatus.APPROVED)
                .collect(Collectors.toList());
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
    
    // Delete purchase order (soft delete by setting status to CANCELLED)
    public PurchaseOrder deletePurchaseOrder(Long id) {
        return updatePOStatusManually(id, PurchaseOrder.POStatus.CANCELLED);
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
                return newStatus == PurchaseOrder.POStatus.PENDING_APPROVAL || 
                       newStatus == PurchaseOrder.POStatus.APPROVED || 
                       newStatus == PurchaseOrder.POStatus.CANCELLED;
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
    
    // Update PO status based on GRN creation and receipt quantities
    @Transactional
    public void updatePOStatusBasedOnGRN(Long poId) {
        System.out.println("=== Starting PO status update for PO ID: " + poId + " ===");
        
        if (poId == null) {
            System.out.println("PO ID is null, skipping PO status update");
            return;
        }
        
        Optional<PurchaseOrder> poOpt = purchaseOrderRepository.findById(poId);
        if (poOpt.isPresent()) {
            PurchaseOrder po = poOpt.get();
            System.out.println("PO found: " + po.getPoNumber() + " with current status: " + po.getStatus());
            
            // Get all GRNs for this PO
            List<GoodsReceivedNote> grns = grnRepository.findByPurchaseOrderId(poId);
            System.out.println("Found " + grns.size() + " GRNs for this PO");
            
            // If no GRNs exist, don't change the status - wait for GRNs to be created
            if (grns.isEmpty()) {
                System.out.println("No GRNs found for PO - status will remain unchanged");
                System.out.println("PO status is " + po.getStatus() + " - waiting for GRNs to be created");
                return;
            }
            
            // Calculate total received quantities for each item
            Map<Long, Integer> totalReceivedByProduct = new HashMap<>();
            Map<Long, Integer> orderedQuantities = new HashMap<>();
            
            // Get ordered quantities
            for (PurchaseOrderItem item : po.getOrderItems()) {
                orderedQuantities.put(item.getProduct().getId(), item.getOrderedQuantity());
                totalReceivedByProduct.put(item.getProduct().getId(), 0);
                System.out.println("PO Item: " + item.getProduct().getProductName() + " - Ordered: " + item.getOrderedQuantity());
            }
            
            // Sum up received quantities from all GRNs
            for (GoodsReceivedNote grn : grns) {
                System.out.println("Processing GRN: " + grn.getGrnNumber() + " with status: " + grn.getStatus());
                if (grn.getStatus() == GoodsReceivedNote.GRNStatus.INSPECTED || 
                    grn.getStatus() == GoodsReceivedNote.GRNStatus.APPROVED) {
                    for (var grnItem : grn.getGrnItems()) {
                        Long productId = grnItem.getProduct().getId();
                        int acceptedQty = grnItem.getAcceptedQuantity() != null ? grnItem.getAcceptedQuantity() : 0;
                        int currentReceived = totalReceivedByProduct.getOrDefault(productId, 0);
                        totalReceivedByProduct.put(productId, currentReceived + acceptedQty);
                        System.out.println("GRN Item: " + grnItem.getProduct().getProductName() + 
                            " - Accepted: " + acceptedQty + " (running total: " + (currentReceived + acceptedQty) + ")");
                    }
                } else {
                    System.out.println("Skipping GRN " + grn.getGrnNumber() + " - status " + grn.getStatus() + " not INSPECTED or APPROVED");
                }
            }
            
            // Determine PO status based on receipt quantities
            boolean allItemsFullyReceived = true;
            boolean anyItemsReceived = false;
            
            System.out.println("=== Analyzing receipt status ===");
            for (Map.Entry<Long, Integer> entry : orderedQuantities.entrySet()) {
                Long productId = entry.getKey();
                int orderedQty = entry.getValue();
                int receivedQty = totalReceivedByProduct.getOrDefault(productId, 0);
                
                System.out.println("Product ID " + productId + ": Ordered=" + orderedQty + ", Received=" + receivedQty);
                
                if (receivedQty > 0) {
                    anyItemsReceived = true;
                }
                
                if (receivedQty < orderedQty) {
                    allItemsFullyReceived = false;
                }
            }
            
            System.out.println("Analysis: anyItemsReceived=" + anyItemsReceived + ", allItemsFullyReceived=" + allItemsFullyReceived);
            
            // Update PO status
            PurchaseOrder.POStatus newStatus = po.getStatus();
            
            if (allItemsFullyReceived && anyItemsReceived) {
                newStatus = PurchaseOrder.POStatus.FULLY_RECEIVED;
                System.out.println("Setting status to FULLY_RECEIVED");
            } else if (anyItemsReceived) {
                newStatus = PurchaseOrder.POStatus.PARTIALLY_RECEIVED;
                System.out.println("Setting status to PARTIALLY_RECEIVED");
            } else if (po.getStatus() == PurchaseOrder.POStatus.APPROVED) {
                // When GRNs exist but no items received yet, change APPROVED to ORDERED
                newStatus = PurchaseOrder.POStatus.ORDERED;
                System.out.println("Setting status to ORDERED (GRNs exist but no items received yet)");
            } else {
                System.out.println("No status change needed, keeping: " + po.getStatus());
            }
            
            // Update status if it changed
            if (newStatus != po.getStatus()) {
                PurchaseOrder.POStatus oldStatus = po.getStatus();
                po.setStatus(newStatus);
                po.setUpdatedAt(LocalDateTime.now());
                purchaseOrderRepository.save(po);
                
                System.out.println("PO " + po.getPoNumber() + " status automatically updated: " + 
                    oldStatus + " → " + newStatus + " based on GRN quantities");
            } else {
                System.out.println("PO status unchanged: " + po.getStatus());
            }
        } else {
            System.out.println("PO not found with ID: " + poId);
        }
        
        System.out.println("=== Completed PO status update for PO ID: " + poId + " ===");
    }
    
    // Manual method to update PO status (for testing and manual updates)
    @Transactional
    public PurchaseOrder updatePOStatusManually(Long id, PurchaseOrder.POStatus newStatus) {
        System.out.println("=== Starting manual PO status update ===");
        System.out.println("PO ID: " + id);
        System.out.println("Requested new status: " + newStatus);
        
        Optional<PurchaseOrder> existingPO = purchaseOrderRepository.findById(id);
        if (existingPO.isPresent()) {
            PurchaseOrder purchaseOrder = existingPO.get();
            PurchaseOrder.POStatus oldStatus = purchaseOrder.getStatus();
            
            System.out.println("PO found: " + purchaseOrder.getPoNumber());
            System.out.println("Current status: " + oldStatus);
            System.out.println("Requested status: " + newStatus);
            
            // Validate status transition
            boolean canTransition = canTransitionToStatus(purchaseOrder.getStatus(), newStatus);
            
            if (!canTransition) {
                String errorMsg = "Invalid status transition from " + purchaseOrder.getStatus() + " to " + newStatus;
                throw new IllegalArgumentException(errorMsg);
            }
            
            purchaseOrder.setStatus(newStatus);
            purchaseOrder.setUpdatedAt(LocalDateTime.now());
            
            System.out.println("Status updated to: " + purchaseOrder.getStatus());
            
            // Set approval info if being approved
            if (newStatus == PurchaseOrder.POStatus.APPROVED) {
                System.out.println("PO is being approved. Setting approval timestamp and reserving stock...");
                purchaseOrder.setApprovedAt(LocalDateTime.now());
                
                // Reserve stock when PO is approved
                Boolean isSellerMode = false;
                try {
                	if (isSellerMode== true) {
                    inventoryService.handlePOApproval(purchaseOrder);
                	}
                    System.out.println("Stock reservation completed successfully");
                } catch (Exception e) {
                    System.err.println("Error reserving stock: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // Handle stock reservation release if PO is cancelled
            if (newStatus == PurchaseOrder.POStatus.CANCELLED && oldStatus == PurchaseOrder.POStatus.APPROVED) {
                System.out.println("PO is being cancelled. Releasing stock reservation...");
                try {
                    inventoryService.handlePOCancellation(purchaseOrder);
                    System.out.println("Stock reservation release completed successfully");
                } catch (Exception e) {
                    System.err.println("Error releasing stock reservation: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // After manual status update, also check if we need to update based on GRNs
            // BUT ONLY if there are existing GRNs - don't auto-change to ORDERED immediately
            if (newStatus == PurchaseOrder.POStatus.APPROVED) {
                System.out.println("PO is now APPROVED. Checking if there are existing GRNs...");
                try {
                    List<GoodsReceivedNote> existingGRNs = grnRepository.findByPurchaseOrderId(id);
                    if (!existingGRNs.isEmpty()) {
                        System.out.println("Found " + existingGRNs.size() + " existing GRNs, triggering automatic status update...");
                        // Only trigger automatic status update if there are existing GRNs
                        updatePOStatusBasedOnGRN(id);
                        System.out.println("Automatic status update completed");
                    } else {
                        System.out.println("No existing GRNs found, PO will remain APPROVED until GRNs are created");
                    }
                } catch (Exception e) {
                    System.err.println("Error in automatic status update: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            System.out.println("Saving PO to database...");
            PurchaseOrder savedPO = purchaseOrderRepository.save(purchaseOrder);
            System.out.println("PO saved successfully. Final status: " + savedPO.getStatus());
            
            System.out.println("=== Manual PO status update completed ===");
            return savedPO;
        } else {
            String errorMsg = "Purchase Order not found with id: " + id;
            System.err.println("ERROR: " + errorMsg);
            throw new RuntimeException(errorMsg);
        }
    }
    
    // Get receipt summary for a PO
    public Map<String, Object> getPOReceiptSummary(Long poId) {
        Optional<PurchaseOrder> poOpt = purchaseOrderRepository.findById(poId);
        if (poOpt.isPresent()) {
            PurchaseOrder po = poOpt.get();
            Map<String, Object> summary = new HashMap<>();
            
            // Get all GRNs for this PO
            List<GoodsReceivedNote> grns = grnRepository.findByPurchaseOrderId(poId);
            
            Map<Long, Integer> totalReceivedByProduct = new HashMap<>();
            Map<Long, Integer> orderedQuantities = new HashMap<>();
            
            // Get ordered quantities
            for (PurchaseOrderItem item : po.getOrderItems()) {
                orderedQuantities.put(item.getProduct().getId(), item.getOrderedQuantity());
                totalReceivedByProduct.put(item.getProduct().getId(), 0);
            }
            
            // Sum up received quantities from all GRNs
            for (GoodsReceivedNote grn : grns) {
                if (grn.getStatus() == GoodsReceivedNote.GRNStatus.INSPECTED || 
                    grn.getStatus() == GoodsReceivedNote.GRNStatus.APPROVED) {
                    for (var grnItem : grn.getGrnItems()) {
                        Long productId = grnItem.getProduct().getId();
                        int acceptedQty = grnItem.getAcceptedQuantity() != null ? grnItem.getAcceptedQuantity() : 0;
                        int currentReceived = totalReceivedByProduct.getOrDefault(productId, 0);
                        totalReceivedByProduct.put(productId, currentReceived + acceptedQty);
                    }
                }
            }
            
            // Calculate receipt percentages
            List<Map<String, Object>> itemReceipts = new ArrayList<>();
            double overallReceiptPercentage = 0.0;
            int totalOrdered = 0;
            int totalReceived = 0;
            
            for (PurchaseOrderItem item : po.getOrderItems()) {
                Long productId = item.getProduct().getId();
                int orderedQty = item.getOrderedQuantity();
                int receivedQty = totalReceivedByProduct.getOrDefault(productId, 0);
                double receiptPercentage = orderedQty > 0 ? (double) receivedQty / orderedQty * 100 : 0;
                
                Map<String, Object> itemReceipt = new HashMap<>();
                itemReceipt.put("productName", item.getProduct().getProductName());
                itemReceipt.put("orderedQuantity", orderedQty);
                itemReceipt.put("receivedQuantity", receivedQty);
                itemReceipt.put("receiptPercentage", Math.round(receiptPercentage * 100.0) / 100.0);
                itemReceipt.put("status", receivedQty == 0 ? "Not Received" : 
                               receivedQty == orderedQty ? "Fully Received" : "Partially Received");
                
                itemReceipts.add(itemReceipt);
                
                totalOrdered += orderedQty;
                totalReceived += receivedQty;
            }
            
            overallReceiptPercentage = totalOrdered > 0 ? (double) totalReceived / totalOrdered * 100 : 0;
            
            summary.put("poNumber", po.getPoNumber());
            summary.put("poStatus", po.getStatus());
            summary.put("totalOrdered", totalOrdered);
            summary.put("totalReceived", totalReceived);
            summary.put("overallReceiptPercentage", Math.round(overallReceiptPercentage * 100.0) / 100.0);
            summary.put("itemReceipts", itemReceipts);
            summary.put("grnCount", grns.size());
            
            return summary;
        }
        return null;
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
