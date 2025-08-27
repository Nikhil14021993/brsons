package com.brsons.service;

import com.brsons.model.CreditNote;
import com.brsons.model.CreditNoteItem;
import com.brsons.model.Order;
import com.brsons.model.Supplier;
import com.brsons.repository.CreditNoteRepository;
import com.brsons.repository.OrderRepository;
import com.brsons.repository.SupplierRepository;
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
        
        return creditNoteRepository.save(creditNote);
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
