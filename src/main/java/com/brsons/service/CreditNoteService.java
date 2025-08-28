package com.brsons.service;

import com.brsons.model.CreditNote;
import com.brsons.model.CreditNoteItem;
import com.brsons.model.Order;
import com.brsons.model.Supplier;
import com.brsons.repository.CreditNoteRepository;
import com.brsons.repository.OrderRepository;
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

@Service
public class CreditNoteService {
    
    @Autowired
    private CreditNoteRepository creditNoteRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private SupplierRepository supplierRepository;
    
    @Autowired
    private InventoryService inventoryService;
    
    // Create new credit note
    @Transactional
    public CreditNote createCreditNote(CreditNote creditNote) {
        // Generate unique credit note number
        String creditNoteNumber = generateCreditNoteNumber();
        creditNote.setCreditNoteNumber(creditNoteNumber);
        creditNote.setCreatedAt(LocalDateTime.now());
        creditNote.setStatus("Draft");
        
        // Calculate total credit amount
        calculateCreditAmount(creditNote);
        
        // Save the credit note first
        CreditNote savedCreditNote = creditNoteRepository.save(creditNote);
        
        // Note: Stock changes are now handled in updateCreditNoteStatus method
        // when status changes from Draft to Issued
        
        return savedCreditNote;
    }
    
    // Update credit note
    @Transactional
    public CreditNote updateCreditNote(Long id, CreditNote creditNoteDetails) {
        CreditNote existingCreditNote = creditNoteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Credit Note not found"));
        
        existingCreditNote.setCreditDate(creditNoteDetails.getCreditDate());
        existingCreditNote.setPurchaseOrder(creditNoteDetails.getPurchaseOrder());
        existingCreditNote.setSupplier(creditNoteDetails.getSupplier());
        existingCreditNote.setReason(creditNoteDetails.getReason());
        existingCreditNote.setNotes(creditNoteDetails.getNotes());
        existingCreditNote.setUpdatedAt(LocalDateTime.now());
        
        // Update items if provided
        if (creditNoteDetails.getCreditNoteItems() != null) {
            existingCreditNote.getCreditNoteItems().clear();
            for (CreditNoteItem item : creditNoteDetails.getCreditNoteItems()) {
                existingCreditNote.addCreditNoteItem(item);
            }
        }
        
        // Recalculate credit amount
        calculateCreditAmount(existingCreditNote);
        
        return creditNoteRepository.save(existingCreditNote);
    }
    
    // Get credit note by ID
    public Optional<CreditNote> getCreditNoteById(Long id) {
        return creditNoteRepository.findById(id);
    }
    
    // Get all credit notes
    public List<CreditNote> getAllCreditNotes() {
        return creditNoteRepository.findAllWithPurchaseOrderAndSupplier();
    }
    
    // Get credit notes by status
    public List<CreditNote> getCreditNotesByStatus(String status) {
        return creditNoteRepository.findByStatusOrderByCreatedAtDesc(status);
    }
    
    // Get credit notes by supplier
    public List<CreditNote> getCreditNotesBySupplier(Long supplierId) {
        return creditNoteRepository.findBySupplierIdOrderByCreatedAtDesc(supplierId);
    }
    
    // Get credit notes by purchase order
    public List<CreditNote> getCreditNotesByPurchaseOrder(Long purchaseOrderId) {
        return creditNoteRepository.findByPurchaseOrderIdOrderByCreatedAtDesc(purchaseOrderId);
    }
    
    // Update credit note status
    @Transactional
    public CreditNote updateCreditNoteStatus(Long id, String newStatus) {
        CreditNote creditNote = creditNoteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Credit Note not found"));
        
        String oldStatus = creditNote.getStatus();
        
        // Handle stock changes based on status transition
        if ("Draft".equals(oldStatus) && "Issued".equals(newStatus)) {
            // Moving from Draft to Issued - decrease stock for returned items
            System.out.println("Credit Note " + creditNote.getCreditNoteNumber() + " status changed from " + oldStatus + " to " + newStatus + " - decreasing stock");
            inventoryService.handleCreditNoteCreation(creditNote);
        } else if ("Issued".equals(oldStatus) && "Draft".equals(newStatus)) {
            // Moving back from Issued to Draft - increase stock back (reverse the decrease)
            System.out.println("Credit Note " + creditNote.getCreditNoteNumber() + " status changed from " + oldStatus + " to " + newStatus + " - increasing stock back");
            reverseStockChanges(creditNote);
        } else if ("Issued".equals(oldStatus) && "Cancelled".equals(newStatus)) {
            // Moving from Issued to Cancelled - increase stock back (reverse the decrease)
            System.out.println("Credit Note " + creditNote.getCreditNoteNumber() + " status changed from " + oldStatus + " to " + newStatus + " - increasing stock back");
            reverseStockChanges(creditNote);
        }
        
        creditNote.setStatus(newStatus);
        creditNote.setUpdatedAt(LocalDateTime.now());
        
        return creditNoteRepository.save(creditNote);
    }
    
    // Delete credit note
    @Transactional
    public void deleteCreditNote(Long id) {
        CreditNote creditNote = creditNoteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Credit Note not found"));
        
        // Only allow deletion of draft credit notes
        if (!"Draft".equals(creditNote.getStatus())) {
            throw new RuntimeException("Only draft credit notes can be deleted");
        }
        
        creditNoteRepository.delete(creditNote);
    }
    
    // Generate unique credit note number
    private String generateCreditNoteNumber() {
        String prefix = "CN";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + "-" + timestamp + "-" + random;
    }
    
    // Calculate total credit amount
    private void calculateCreditAmount(CreditNote creditNote) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        if (creditNote.getCreditNoteItems() != null) {
            for (CreditNoteItem item : creditNote.getCreditNoteItems()) {
                BigDecimal itemTotal = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
                
                // Apply discount if any
                if (item.getDiscountPercentage() != null && item.getDiscountPercentage().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal discount = itemTotal.multiply(item.getDiscountPercentage())
                        .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
                    itemTotal = itemTotal.subtract(discount);
                }
                
                // Apply tax if any
                if (item.getTaxPercentage() != null && item.getTaxPercentage().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal tax = itemTotal.multiply(item.getTaxPercentage())
                        .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
                    itemTotal = itemTotal.add(tax);
                }
                
                item.setTotalAmount(itemTotal);
                totalAmount = totalAmount.add(itemTotal);
            }
        }
        
        creditNote.setCreditAmount(totalAmount);
    }
    
    // Reverse stock changes when Credit Note status is reverted or cancelled
    private void reverseStockChanges(CreditNote creditNote) {
        if (creditNote.getCreditNoteItems() != null && !creditNote.getCreditNoteItems().isEmpty()) {
            for (CreditNoteItem item : creditNote.getCreditNoteItems()) {
                int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
                if (quantity > 0) {
                    inventoryService.increaseStock(
                        item.getProduct().getId(),
                        quantity,
                        "Stock reversal - Credit Note status reverted/cancelled - " + creditNote.getCreditNoteNumber(),
                        "CREDIT_NOTE",
                        creditNote.getId()
                    );
                }
            }
        }
    }
    
    // Get credit note statistics
    public CreditNoteStatistics getCreditNoteStatistics() {
        long totalCreditNotes = creditNoteRepository.count();
        long draftCreditNotes = creditNoteRepository.countByStatus("Draft");
        long issuedCreditNotes = creditNoteRepository.countByStatus("Issued");
        long appliedCreditNotes = creditNoteRepository.countByStatus("Applied");
        long cancelledCreditNotes = creditNoteRepository.countByStatus("Cancelled");
        
        return new CreditNoteStatistics(
            totalCreditNotes, 
            draftCreditNotes, 
            issuedCreditNotes, 
            appliedCreditNotes, 
            cancelledCreditNotes
        );
    }
    
    // Inner class for statistics
    public static class CreditNoteStatistics {
        private final long totalCreditNotes;
        private final long draftCreditNotes;
        private final long issuedCreditNotes;
        private final long appliedCreditNotes;
        private final long cancelledCreditNotes;
        
        public CreditNoteStatistics(long totalCreditNotes, long draftCreditNotes, 
                                 long issuedCreditNotes, long appliedCreditNotes, 
                                 long cancelledCreditNotes) {
            this.totalCreditNotes = totalCreditNotes;
            this.draftCreditNotes = draftCreditNotes;
            this.issuedCreditNotes = issuedCreditNotes;
            this.appliedCreditNotes = appliedCreditNotes;
            this.cancelledCreditNotes = cancelledCreditNotes;
        }
        
        // Getters
        public long getTotalCreditNotes() { return totalCreditNotes; }
        public long getDraftCreditNotes() { return draftCreditNotes; }
        public long getIssuedCreditNotes() { return issuedCreditNotes; }
        public long getAppliedCreditNotes() { return appliedCreditNotes; }
        public long getCancelledCreditNotes() { return cancelledCreditNotes; }
    }
}
