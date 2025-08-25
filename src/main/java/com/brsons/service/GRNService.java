package com.brsons.service;

import com.brsons.model.GoodsReceivedNote;
import com.brsons.model.GRNItem;
import com.brsons.model.PurchaseOrder;
import com.brsons.repository.GRNRepository;
import com.brsons.repository.PurchaseOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class GRNService {
    
    @Autowired
    private GRNRepository grnRepository;
    
    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;
    
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
        return grnRepository.save(grn);
    }
    
    // Get GRN by ID
    public Optional<GoodsReceivedNote> getGRNById(Long id) {
        return grnRepository.findById(id);
    }
    
    // Get all GRNs
    public List<GoodsReceivedNote> getAllGRNs() {
        return grnRepository.findAll();
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
            
            // Validate status transition
            if (!canTransitionToStatus(grn.getStatus(), newStatus)) {
                throw new IllegalArgumentException("Invalid status transition from " + grn.getStatus() + " to " + newStatus);
            }
            
            grn.setStatus(newStatus);
            grn.setUpdatedAt(LocalDateTime.now());
            
            return grnRepository.save(grn);
        }
        throw new RuntimeException("GRN not found with id: " + id);
    }
    
    // Delete GRN (soft delete by setting status to CANCELLED)
    public GoodsReceivedNote deleteGRN(Long id) {
        return updateGRNStatus(id, GoodsReceivedNote.GRNStatus.CANCELLED);
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
            case REJECTED:
            case CANCELLED:
                return false; // Terminal states
            default:
                return false;
        }
    }
    
    // Generate unique GRN number
    private String generateGRNNumber() {
        String prefix = "GRN";
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8); // Last 4 digits
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + timestamp + random;
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
