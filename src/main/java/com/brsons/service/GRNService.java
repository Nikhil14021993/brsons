package com.brsons.service;

import com.brsons.model.GoodsReceivedNote;
import com.brsons.model.GRNItem;
import com.brsons.model.PurchaseOrder;
import com.brsons.model.Supplier;
import com.brsons.model.SupplierLedger;
import com.brsons.repository.GRNRepository;
import com.brsons.repository.PurchaseOrderRepository;
import com.brsons.service.InventoryService;
import com.brsons.service.PurchaseOrderService;
import com.brsons.service.SupplierLedgerService;
import com.brsons.service.OutstandingService;
import com.brsons.service.AccountingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import com.brsons.model.GoodsReceivedNote.GRNStatus;

@Service
@Transactional
public class GRNService {
    
    @Autowired
    private GRNRepository grnRepository;
    
    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;
    
    @Autowired
    private InventoryService inventoryService;
    
    @Autowired
    private PurchaseOrderService purchaseOrderService;
    
    @Autowired
    private SupplierLedgerService supplierLedgerService;
    
    @Autowired
    private OutstandingService outstandingService;
    
    @Autowired
    private AccountingService accountingService;
    
    // Create new GRN
    public GoodsReceivedNote createGRN(GoodsReceivedNote grn) {
        // Generate unique GRN number if not provided
        if (grn.getGrnNumber() == null || grn.getGrnNumber().trim().isEmpty()) {
            grn.setGrnNumber(generateGRNNumber());
        }
        
        // Set default values
        grn.setCreatedAt(LocalDateTime.now());
        grn.setUpdatedAt(LocalDateTime.now());
        grn.setStatus(GoodsReceivedNote.GRNStatus.DRAFT);
        
        // Calculate totals for each item first
        if (grn.getGrnItems() != null && !grn.getGrnItems().isEmpty()) {
            for (GRNItem item : grn.getGrnItems()) {
                item.calculateTotals();
            }
        }
        
        // Now calculate GRN totals
        grn.calculateTotals();
        
        // Save the GRN
        GoodsReceivedNote savedGRN = grnRepository.save(grn);
        
        // Automatically update PO status based on new GRN
        if (savedGRN.getPurchaseOrder() != null && savedGRN.getPurchaseOrder().getId() != null) {
            try {
                System.out.println("GRN " + savedGRN.getGrnNumber() + " created for PO " + savedGRN.getPurchaseOrder().getId() + 
                    " (current status: " + savedGRN.getPurchaseOrder().getStatus() + ")");
                System.out.println("Triggering automatic PO status update...");
                purchaseOrderService.updatePOStatusBasedOnGRN(savedGRN.getPurchaseOrder().getId());
                System.out.println("Automatic PO status update completed");
            } catch (Exception e) {
                System.err.println("Error updating PO status after GRN creation: " + e.getMessage());
                e.printStackTrace();
                // Don't fail the GRN creation if PO status update fails
            }
        } else {
            System.out.println("GRN " + savedGRN.getGrnNumber() + " has no associated PO or PO has no ID");
        }
        
        return savedGRN;
    }
    
    // Get GRN by ID
    public Optional<GoodsReceivedNote> getGRNById(Long id) {
        return grnRepository.findById(id);
    }
    
    // Get all GRNs
    public List<GoodsReceivedNote> getAllGRNs() {
        return grnRepository.findAll();
    }
    
    public GoodsReceivedNote getById(Long id) {
        return grnRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("GRN not found with id: " + id));
    }
    
    // Get GRNs by status
    public List<GoodsReceivedNote> getGRNsByStatus(GoodsReceivedNote.GRNStatus status) {
        return grnRepository.findByStatus(status);
    }
    
    // Get GRNs by purchase order
    public List<GoodsReceivedNote> getGRNsByPurchaseOrder(Long purchaseOrderId) {
        return grnRepository.findByPurchaseOrderId(purchaseOrderId);
    }
    
    // Get GRNs by supplier
    public List<GoodsReceivedNote> getGRNsBySupplier(Long supplierId) {
        return grnRepository.findBySupplierId(supplierId);
    }
    
    // Update GRN
    public GoodsReceivedNote updateGRN(Long id, GoodsReceivedNote grnDetails) {
        Optional<GoodsReceivedNote> existingGRN = grnRepository.findById(id);
        if (existingGRN.isPresent()) {
            GoodsReceivedNote grn = existingGRN.get();
            
            // Update basic fields
            grn.setGrnNumber(grnDetails.getGrnNumber());
            grn.setPurchaseOrder(grnDetails.getPurchaseOrder());
            grn.setSupplier(grnDetails.getSupplier());
            grn.setReceivedDate(grnDetails.getReceivedDate());
            grn.setDeliveryNoteNumber(grnDetails.getDeliveryNoteNumber());
            grn.setVehicleNumber(grnDetails.getVehicleNumber());
            grn.setReceivedBy(grnDetails.getReceivedBy());
            grn.setInspectedBy(grnDetails.getInspectedBy());
            grn.setQualityRemarks(grnDetails.getQualityRemarks());
            grn.setNotes(grnDetails.getNotes());
            grn.setUpdatedAt(LocalDateTime.now());
            
            // Update items
            if (grnDetails.getGrnItems() != null && !grnDetails.getGrnItems().isEmpty()) {
                grn.setGrnItems(grnDetails.getGrnItems());
            }
            
            // Recalculate totals
            grn.calculateTotals();
            
            return grnRepository.save(grn);
        }
        throw new RuntimeException("GRN not found with id: " + id);
    }
    
    // Update GRN status
    public GoodsReceivedNote updateGRNStatus(Long id, GoodsReceivedNote.GRNStatus newStatus) {
        Optional<GoodsReceivedNote> existingGRN = grnRepository.findById(id);
        if (existingGRN.isPresent()) {
            GoodsReceivedNote grn = existingGRN.get();
            GoodsReceivedNote.GRNStatus oldStatus = grn.getStatus();
            
            // Validate status transition
            if (!canTransitionToStatus(grn.getStatus(), newStatus)) {
                throw new IllegalArgumentException("Invalid status transition from " + grn.getStatus() + " to " + newStatus);
            }
            
            // Handle stock reversals ONLY when going BACK to previous statuses
            // NOT when moving forward to APPROVED/REJECTED (these are terminal states)
            // Business Logic:
            // - DRAFT -> RECEIVED -> INSPECTED -> APPROVED/REJECTED (forward flow)
            // - INSPECTED -> RECEIVED/DRAFT (backward flow - requires stock reversal)
            if (oldStatus == GoodsReceivedNote.GRNStatus.INSPECTED && 
                (newStatus == GoodsReceivedNote.GRNStatus.RECEIVED || newStatus == GoodsReceivedNote.GRNStatus.DRAFT)) {
                // Reverse previous stock updates only when going back
                System.out.println("Reversing stock updates - GRN " + grn.getGrnNumber() + " going back from INSPECTED to " + newStatus);
                reverseStockUpdates(grn);
            }
            
            grn.setStatus(newStatus);
            grn.setUpdatedAt(LocalDateTime.now());
            
            // Update inventory ONLY when status changes to INSPECTED (final stock update)
            if (newStatus == GoodsReceivedNote.GRNStatus.INSPECTED && oldStatus != GoodsReceivedNote.GRNStatus.INSPECTED) {
                System.out.println("Updating stock from inspection - GRN " + grn.getGrnNumber() + " status: " + oldStatus + " -> " + newStatus);
                updateStockFromInspection(grn);
            }
            
            // Handle stock decrease when GRN is REJECTED (all items go back to supplier)
            if (newStatus == GoodsReceivedNote.GRNStatus.REJECTED && oldStatus == GoodsReceivedNote.GRNStatus.INSPECTED) {
                System.out.println("Decreasing stock for rejected GRN - all items returned to supplier - GRN " + grn.getGrnNumber() + " status: " + oldStatus + " -> " + newStatus);
                decreaseStockForRejectedGRN(grn);
            }
            
            // Handle stock increase when GRN status changes back to INSPECTED from REJECTED
            if (newStatus == GoodsReceivedNote.GRNStatus.INSPECTED && oldStatus == GoodsReceivedNote.GRNStatus.REJECTED) {
                System.out.println("Re-increasing stock for items - GRN " + grn.getGrnNumber() + " status: " + oldStatus + " -> " + newStatus);
                updateStockFromInspection(grn);
            }
            
            // Create accounting entries when GRN is approved (goods are officially received and accepted)
            if (newStatus == GoodsReceivedNote.GRNStatus.APPROVED && oldStatus != GoodsReceivedNote.GRNStatus.APPROVED) {
                if (grn.getPurchaseOrder() != null && grn.getPurchaseOrder().getId() != null) {
                    System.out.println("GRN " + grn.getGrnNumber() + " approved - creating accounting entries for PO #" + grn.getPurchaseOrder().getId());
                } else {
                    System.out.println("GRN " + grn.getGrnNumber() + " approved - creating accounting entries for direct GRN (no PO)");
                }
                createAccountingEntriesForApprovedGRN(grn);
            }
            
            // Log status changes for debugging
            System.out.println("GRN " + grn.getGrnNumber() + " status changed: " + oldStatus + " -> " + newStatus);
            
            // Automatically update PO status based on GRN quantities
            if (grn.getPurchaseOrder() != null && grn.getPurchaseOrder().getId() != null) {
                try {
                    System.out.println("GRN " + grn.getGrnNumber() + " status changed to " + newStatus + 
                        " for PO " + grn.getPurchaseOrder().getId() + " (current PO status: " + grn.getPurchaseOrder().getStatus() + ")");
                    System.out.println("Triggering automatic PO status update after GRN status change...");
                    purchaseOrderService.updatePOStatusBasedOnGRN(grn.getPurchaseOrder().getId());
                    System.out.println("Automatic PO status update completed after GRN status change");
                } catch (Exception e) {
                    System.err.println("Error updating PO status: " + e.getMessage());
                    e.printStackTrace();
                    // Don't fail the GRN update if PO status update fails
                }
            } else {
                System.out.println("GRN " + grn.getGrnNumber() + " has no associated PO or PO has no ID");
            }
            
            return grnRepository.save(grn);
        }
        throw new RuntimeException("GRN not found with id: " + id);
    }
    
    // Update GRN item quantities and adjust stock if needed
    @Transactional
    public void updateGRNItemQuantities(Long grnId, Long itemId, int receivedQty, int acceptedQty, int rejectedQty) {
        Optional<GoodsReceivedNote> grnOpt = grnRepository.findById(grnId);
        if (grnOpt.isPresent()) {
            GoodsReceivedNote grn = grnOpt.get();
            
            // Only allow updates if GRN is in DRAFT or RECEIVED status
            if (grn.getStatus() != GoodsReceivedNote.GRNStatus.DRAFT && 
                grn.getStatus() != GoodsReceivedNote.GRNStatus.RECEIVED) {
                throw new IllegalStateException("Cannot update quantities for GRN in status: " + grn.getStatus());
            }
            
            // Find the specific item
            GRNItem item = grn.getGrnItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("GRN item not found"));
            
            // If GRN was previously INSPECTED, we need to reverse the previous stock update
            if (grn.getStatus() == GoodsReceivedNote.GRNStatus.INSPECTED) {
                int previousAccepted = item.getAcceptedQuantity() != null ? item.getAcceptedQuantity() : 0;
                if (previousAccepted > 0) {
                    inventoryService.decreaseStock(
                        item.getProduct().getId(),
                        previousAccepted,
                        "Stock reversal - GRN item quantities updated",
                        "GRN",
                        grn.getId()
                    );
                }
            }
            
            // Update the item quantities
            item.updateQuantities(receivedQty, acceptedQty, rejectedQty);
            
            // If GRN is INSPECTED, update stock with new accepted quantity
            if (grn.getStatus() == GoodsReceivedNote.GRNStatus.INSPECTED) {
                int newAccepted = item.getAcceptedQuantity();
                if (newAccepted > 0) {
                    inventoryService.increaseStock(
                        item.getProduct().getId(),
                        newAccepted,
                        "Goods accepted from GRN inspection (updated) - " + grn.getGrnNumber(),
                        "GRN",
                        grn.getId()
                    );
                }
            }
            
            // Recalculate GRN totals
            grn.calculateTotals();
            grnRepository.save(grn);
            
            // Automatically update PO status based on updated GRN quantities
            if (grn.getPurchaseOrder() != null && grn.getPurchaseOrder().getId() != null) {
                try {
                    purchaseOrderService.updatePOStatusBasedOnGRN(grn.getPurchaseOrder().getId());
                } catch (Exception e) {
                    System.err.println("Error updating PO status after quantity update: " + e.getMessage());
                    // Don't fail the GRN update if PO status update fails
                }
            }
        }
    }
    
    // Handle stock updates when GRN is inspected
    private void updateStockFromInspection(GoodsReceivedNote grn) {
        if (grn.getGrnItems() != null && !grn.getGrnItems().isEmpty()) {
            for (GRNItem item : grn.getGrnItems()) {
                // Validate that quantities are properly set
                if (!item.validateQuantities()) {
                    throw new IllegalStateException("Invalid quantities for item " + item.getProduct().getProductName() + 
                        ". Received: " + item.getReceivedQuantity() + 
                        ", Accepted: " + item.getAcceptedQuantity() + 
                        ", Rejected: " + item.getRejectedQuantity() + 
                        ". Accepted + Rejected must equal Received.");
                }
                
                // Use ACCEPTED quantity for stock increase (these are the good items we keep)
                int acceptedQty = item.getAcceptedQuantity() != null ? item.getAcceptedQuantity() : 0;
                if (acceptedQty > 0) {
                    inventoryService.increaseStock(
                        item.getProduct().getId(),
                        acceptedQty,
                        "Goods accepted from GRN inspection - " + grn.getGrnNumber(),
                        "GRN",
                        grn.getId()
                    );
                }
                
                // Note: Rejected items are NOT added to stock (they go back to supplier)
                // The rejected quantity is tracked but doesn't affect our inventory
            }
        }
    }
    
    // Reverse stock updates when GRN status changes from INSPECTED
    private void reverseStockUpdates(GoodsReceivedNote grn) {
        if (grn.getGrnItems() != null && !grn.getGrnItems().isEmpty()) {
            for (GRNItem item : grn.getGrnItems()) {
                int acceptedQty = item.getAcceptedQuantity() != null ? item.getAcceptedQuantity() : 0;
                if (acceptedQty > 0) {
                    inventoryService.decreaseStock(
                        item.getProduct().getId(),
                        acceptedQty,
                        "Stock reversal - GRN status changed from INSPECTED",
                        "GRN",
                        grn.getId()
                    );
                }
            }
        }
    }

    // Decrease stock for rejected GRN when status changes to REJECTED
    private void decreaseStockForRejectedGRN(GoodsReceivedNote grn) {
        if (grn.getGrnItems() != null && !grn.getGrnItems().isEmpty()) {
            for (GRNItem item : grn.getGrnItems()) {
                // When GRN is REJECTED, ALL items (both accepted and rejected) go back to supplier
                // So we need to decrease stock by the ACCEPTED quantity that was previously added
                int acceptedQty = item.getAcceptedQuantity() != null ? item.getAcceptedQuantity() : 0;
                if (acceptedQty > 0) {
                    inventoryService.decreaseStock(
                        item.getProduct().getId(),
                        acceptedQty,
                        "Stock decrease for rejected GRN - all items returned to supplier - " + grn.getGrnNumber(),
                        "GRN",
                        grn.getId()
                    );
                }
            }
        }
    }
    
    // Delete GRN (soft delete by setting status to CANCELLED)
    public GoodsReceivedNote deleteGRN(Long id) {
        return updateGRNStatus(id, GoodsReceivedNote.GRNStatus.CANCELLED);
    }
    
    // Hard delete GRN from database
    public void hardDeleteGRN(Long id) {
        Optional<GoodsReceivedNote> existingGRN = grnRepository.findById(id);
        if (existingGRN.isPresent()) {
            GoodsReceivedNote grn = existingGRN.get();
            
            // Only allow deletion of GRNs in DRAFT or CANCELLED status
            if (grn.getStatus() != GRNStatus.DRAFT && grn.getStatus() != GRNStatus.CANCELLED) {
                throw new IllegalStateException("Cannot delete GRN in status: " + grn.getStatus() + 
                    ". Only DRAFT or CANCELLED GRNs can be deleted.");
            }
            
            grnRepository.deleteById(id);
        } else {
            throw new RuntimeException("GRN not found with id: " + id);
        }
    }
    
    // Get GRN statistics
    public GRNStatistics getGRNStatistics() {
        long totalGRNs = grnRepository.count();
        long draftGRNs = grnRepository.countByStatus(GoodsReceivedNote.GRNStatus.DRAFT);
        long receivedGRNs = grnRepository.countByStatus(GoodsReceivedNote.GRNStatus.RECEIVED);
        long inspectedGRNs = grnRepository.countByStatus(GoodsReceivedNote.GRNStatus.INSPECTED);
        long approvedGRNs = grnRepository.countByStatus(GoodsReceivedNote.GRNStatus.APPROVED);
        long rejectedGRNs = grnRepository.countByStatus(GoodsReceivedNote.GRNStatus.REJECTED);
        long cancelledGRNs = grnRepository.countByStatus(GoodsReceivedNote.GRNStatus.CANCELLED);
        
        return new GRNStatistics(
            totalGRNs, draftGRNs, receivedGRNs, inspectedGRNs, 
            approvedGRNs, rejectedGRNs, cancelledGRNs
        );
    }
    
    // Validate status transition
    private boolean canTransitionToStatus(GoodsReceivedNote.GRNStatus currentStatus, GoodsReceivedNote.GRNStatus newStatus) {
        switch (currentStatus) {
            case DRAFT:
                return newStatus == GoodsReceivedNote.GRNStatus.RECEIVED || newStatus == GoodsReceivedNote.GRNStatus.CANCELLED;
            case RECEIVED:
                return newStatus == GoodsReceivedNote.GRNStatus.INSPECTED || newStatus == GoodsReceivedNote.GRNStatus.CANCELLED;
            case INSPECTED:
                return newStatus == GoodsReceivedNote.GRNStatus.APPROVED || newStatus == GoodsReceivedNote.GRNStatus.REJECTED || newStatus == GoodsReceivedNote.GRNStatus.CANCELLED;
            case APPROVED:
                return false; // Terminal state
            case REJECTED:
                return newStatus == GoodsReceivedNote.GRNStatus.INSPECTED || newStatus == GoodsReceivedNote.GRNStatus.CANCELLED; // Can go back to INSPECTED
            case CANCELLED:
                return false; // Terminal state
            default:
                return false;
        }
    }
    
    // Create voucher entry for GRN approval
    private void createVoucherForGRNApproval(GoodsReceivedNote grn, PurchaseOrder po) {
        try {
            System.out.println("=== Creating split voucher for GRN approval ===");
            System.out.println("GRN: " + grn.getGrnNumber());
            System.out.println("PO: " + (po != null ? po.getId() : "Direct GRN (no PO)"));
            System.out.println("Subtotal: " + grn.getSubtotal());
            System.out.println("Tax Amount: " + grn.getTaxAmount());
            System.out.println("Total Amount: " + grn.getTotalAmount());
            
            String narration;
            if (po != null) {
                narration = "GRN Approval - " + grn.getGrnNumber() + " for PO #" + po.getId() + " - " + po.getSupplier().getCompanyName();
            } else {
                narration = "Direct GRN Approval - " + grn.getGrnNumber() + " - " + grn.getSupplier().getCompanyName();
            }
            
            // Find accounts by code
            Long purchaseAccountId = findAccountIdByCode("6001"); // Purchase / Cost of Goods Sold
            Long taxAccountId = findAccountIdByCode("7001"); // Duty and Taxes
            Long payableAccountId = findAccountIdByCode("2001.01"); // Accounts Payable
            
            if (purchaseAccountId == null || taxAccountId == null || payableAccountId == null) {
                System.err.println("Cannot create voucher - missing required accounts:");
                System.err.println("Purchase Account (6001): " + (purchaseAccountId != null ? "Found (ID: " + purchaseAccountId + ")" : "NOT FOUND"));
                System.err.println("Tax Account (2001.04): " + (taxAccountId != null ? "Found (ID: " + taxAccountId + ")" : "NOT FOUND"));
                System.err.println("Payable Account (2001.01): " + (payableAccountId != null ? "Found (ID: " + payableAccountId + ")" : "NOT FOUND"));
                System.err.println("Please run the create_grn_split_accounts.sql script to create the required accounts.");
                return;
            }
            
            // Calculate amounts
            BigDecimal netAmount = grn.getSubtotal() != null ? grn.getSubtotal() : BigDecimal.ZERO;
            BigDecimal taxAmount = grn.getTaxAmount() != null ? grn.getTaxAmount() : BigDecimal.ZERO;
            BigDecimal grandTotal = grn.getTotalAmount();
            
            System.out.println("=== Amount Calculation Debug ===");
            System.out.println("GRN Subtotal (Net Amount): " + netAmount);
            System.out.println("GRN Tax Amount: " + taxAmount);
            System.out.println("GRN Total Amount (Grand Total): " + grandTotal);
            System.out.println("Calculated Total (Net + Tax): " + netAmount.add(taxAmount));
            
            // Verify calculation: Net Amount + Tax Amount = Grand Total
            BigDecimal calculatedTotal = netAmount.add(taxAmount);
            if (calculatedTotal.compareTo(grandTotal) != 0) {
                System.err.println("Warning: Amount calculation mismatch!");
                System.err.println("Net Amount: " + netAmount);
                System.err.println("Tax Amount: " + taxAmount);
                System.err.println("Calculated Total: " + calculatedTotal);
                System.err.println("GRN Total: " + grandTotal);
                
                // Adjust tax amount to match grand total
                taxAmount = grandTotal.subtract(netAmount);
                System.out.println("Adjusted Tax Amount to: " + taxAmount);
            }
            
            // Create voucher entries list
            List<com.brsons.dto.VoucherEntryDto> entries = new ArrayList<>();
            
            // Debit Entry 1: Net Amount to Purchase/Cost of Goods Sold
            if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
                entries.add(new com.brsons.dto.VoucherEntryDto(
                    purchaseAccountId,
                    netAmount,
                    null,
                    "Purchase/Cost of Goods Sold - " + grn.getGrnNumber()
                ));
                System.out.println("Added debit entry: Purchase Account (ID: " + purchaseAccountId + ") - Amount: " + netAmount);
            }
            
            // Debit Entry 2: Tax Amount to Duty and Taxes
            if (taxAmount.compareTo(BigDecimal.ZERO) > 0) {
                entries.add(new com.brsons.dto.VoucherEntryDto(
                    taxAccountId,
                    taxAmount,
                    null,
                    "Duty and Taxes - " + grn.getGrnNumber()
                ));
                System.out.println("Added debit entry: Tax Account (ID: " + taxAccountId + ") - Amount: " + taxAmount);
            }
            
            // Credit Entry: Grand Total to Accounts Payable
            entries.add(new com.brsons.dto.VoucherEntryDto(
                payableAccountId,
                null,
                grandTotal,
                "Accounts Payable - " + grn.getGrnNumber()
            ));
            System.out.println("Added credit entry: Payable Account (ID: " + payableAccountId + ") - Amount: " + grandTotal);
            
            // Create voucher with multiple entries
            accountingService.createVoucherWithEntries(
                grn.getReceivedDate(), // Use GRN received date
                narration,
                "PURCHASE", // Voucher type
                entries
            );
            
            System.out.println("Split voucher created successfully for GRN approval");
            System.out.println("Net Amount (" + netAmount + ") → Debit Purchase Account (6001), Tax Amount (" + taxAmount + ") → Debit Tax Account (7001), Grand Total (" + grandTotal + ") → Credit Payable Account");
            
        } catch (Exception e) {
            System.err.println("Error creating split voucher for GRN approval: " + e.getMessage());
            e.printStackTrace();
            // Don't fail the GRN approval if voucher creation fails
        }
    }
    
    // Create accounting entries when GRN is approved
    private void createAccountingEntriesForApprovedGRN(GoodsReceivedNote grn) {
        try {
            PurchaseOrder po = grn.getPurchaseOrder();
            Supplier supplier = grn.getSupplier();
            
            if (supplier == null) {
                System.err.println("Cannot create accounting entries - GRN has no associated supplier");
                return;
            }
            
            System.out.println("=== Creating accounting entries for approved GRN ===");
            System.out.println("GRN: " + grn.getGrnNumber());
            System.out.println("PO: " + (po != null ? po.getId() : "Direct GRN (no PO)"));
            System.out.println("Supplier: " + supplier.getCompanyName());
            System.out.println("Amount: " + grn.getTotalAmount());
            
            // 1. Create or find supplier ledger
            SupplierLedger supplierLedger = supplierLedgerService.findOrCreateSupplierLedger(
                supplier.getCompanyName(),
                supplier.getPhone(),
                supplier.getEmail(),
                supplier.getSupplierCode()
            );
            
            if (po != null) {
                // 2. Add purchase order entry to supplier ledger (only if PO exists)
                supplierLedgerService.addPurchaseOrderEntry(supplierLedger, po);
                System.out.println("Created supplier ledger entry for PO #" + po.getId());
                
                // 3. Create outstanding payable (only if PO exists)
                outstandingService.createPurchaseOrderOutstanding(po);
                System.out.println("Created outstanding payable for PO #" + po.getId());
            } else {
                // For direct GRN, create direct GRN entries
                System.out.println("Creating direct GRN entries for GRN without PO");
                
                // 2. Add Direct GRN entry to supplier ledger
                supplierLedgerService.addDirectGRNEntry(supplierLedger, grn);
                System.out.println("Created supplier ledger entry for Direct GRN #" + grn.getGrnNumber());
                
                // 3. Create outstanding payable for Direct GRN
                outstandingService.createDirectGRNOutstanding(grn);
                System.out.println("Created outstanding payable for Direct GRN #" + grn.getGrnNumber());
            }
            
            // 4. Create voucher entry for double-entry bookkeeping
            createVoucherForGRNApproval(grn, po);
            System.out.println("Created voucher entry for GRN approval");
            
            System.out.println("=== Accounting entries created successfully ===");
            
        } catch (Exception e) {
            System.err.println("Error creating accounting entries for approved GRN: " + e.getMessage());
            e.printStackTrace();
            // Don't fail the GRN approval if accounting fails
        }
    }
    
    // Generate unique GRN number
    private String generateGRNNumber() {
        String prefix = "GRN";
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8); // Last 4 digits
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + timestamp + random;
    }
    
    // Helper method to find account ID by code
    private Long findAccountIdByCode(String accountCode) {
        try {
            // This is a simple implementation - you might want to inject AccountRepository
            // For now, we'll use a basic approach
            return accountingService.findAccountIdByCode(accountCode);
        } catch (Exception e) {
            System.err.println("Error finding account by code " + accountCode + ": " + e.getMessage());
            return null;
        }
    }
    
    // Statistics class
    public static class GRNStatistics {
        private final long totalGRNs;
        private final long draftGRNs;
        private final long receivedGRNs;
        private final long inspectedGRNs;
        private final long approvedGRNs;
        private final long rejectedGRNs;
        private final long cancelledGRNs;
        
        public GRNStatistics(long totalGRNs, long draftGRNs, long receivedGRNs, 
                           long inspectedGRNs, long approvedGRNs, long rejectedGRNs, long cancelledGRNs) {
            this.totalGRNs = totalGRNs;
            this.draftGRNs = draftGRNs;
            this.receivedGRNs = receivedGRNs;
            this.inspectedGRNs = inspectedGRNs;
            this.approvedGRNs = approvedGRNs;
            this.rejectedGRNs = rejectedGRNs;
            this.cancelledGRNs = cancelledGRNs;
        }
        
        // Getters
        public long getTotalGRNs() { return totalGRNs; }
        public long getDraftGRNs() { return draftGRNs; }
        public long getReceivedGRNs() { return receivedGRNs; }
        public long getInspectedGRNs() { return inspectedGRNs; }
        public long getApprovedGRNs() { return approvedGRNs; }
        public long getRejectedGRNs() { return rejectedGRNs; }
        public long getCancelledGRNs() { return cancelledGRNs; }
    }
}
