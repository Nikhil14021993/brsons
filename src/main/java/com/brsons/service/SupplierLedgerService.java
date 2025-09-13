package com.brsons.service;

import com.brsons.model.SupplierLedger;
import com.brsons.model.SupplierLedgerEntry;
import com.brsons.model.PurchaseOrder;
import com.brsons.model.Voucher;
import com.brsons.model.VoucherEntry;
import com.brsons.model.Account;
import com.brsons.repository.SupplierLedgerRepository;
import com.brsons.repository.SupplierLedgerEntryRepository;
import com.brsons.repository.PurchaseOrderRepository;
import com.brsons.repository.VoucherRepository;
import com.brsons.repository.VoucherEntryRepository;
import com.brsons.repository.AccountRepository;
import com.brsons.repository.OutstandingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SupplierLedgerService {
    
    @Autowired
    private SupplierLedgerRepository supplierLedgerRepository;
    
    @Autowired
    private SupplierLedgerEntryRepository supplierLedgerEntryRepository;
    
    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;
    
    @Autowired
    private OutstandingRepository outstandingRepository;
    
    @Autowired
    private VoucherRepository voucherRepository;
    
    @Autowired
    private VoucherEntryRepository voucherEntryRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    // ==================== SUPPLIER LEDGER MANAGEMENT ====================
    
    /**
     * Create or find supplier ledger by phone number
     */
    @Transactional
    public SupplierLedger findOrCreateSupplierLedger(String supplierName, String supplierPhone, String supplierEmail, String supplierCode) {
        Optional<SupplierLedger> existingLedger = supplierLedgerRepository.findBySupplierPhone(supplierPhone);
        
        if (existingLedger.isPresent()) {
            SupplierLedger ledger = existingLedger.get();
            // Update supplier information if needed
            if (supplierName != null && !supplierName.equals(ledger.getSupplierName())) {
                ledger.setSupplierName(supplierName);
            }
            if (supplierEmail != null && !supplierEmail.equals(ledger.getSupplierEmail())) {
                ledger.setSupplierEmail(supplierEmail);
            }
            if (supplierCode != null && !supplierCode.equals(ledger.getSupplierCode())) {
                ledger.setSupplierCode(supplierCode);
            }
            return supplierLedgerRepository.save(ledger);
        } else {
            // Create new supplier ledger
            SupplierLedger newLedger = new SupplierLedger(supplierName, supplierPhone);
            newLedger.setSupplierEmail(supplierEmail);
            newLedger.setSupplierCode(supplierCode);
            return supplierLedgerRepository.save(newLedger);
        }
    }
    
    /**
     * Get supplier ledger by phone number
     */
    public Optional<SupplierLedger> getSupplierLedgerByPhone(String supplierPhone) {
        return supplierLedgerRepository.findBySupplierPhone(supplierPhone);
    }
    
    /**
     * Get supplier ledger by ID
     */
    public Optional<SupplierLedger> getSupplierLedgerById(Long id) {
        return supplierLedgerRepository.findById(id);
    }
    
    /**
     * Get all active supplier ledgers
     */
    public List<SupplierLedger> getAllActiveSupplierLedgers() {
        return supplierLedgerRepository.findByStatus("ACTIVE");
    }
    
    /**
     * Get supplier ledgers with outstanding balance
     */
    public List<SupplierLedger> getSupplierLedgersWithOutstandingBalance() {
        return supplierLedgerRepository.findActiveLedgersWithOutstandingBalance();
    }
    
    /**
     * Search supplier ledgers by name, phone, or code
     */
    public List<SupplierLedger> searchSupplierLedgers(String searchTerm) {
        return supplierLedgerRepository.findBySupplierNameOrPhoneOrCodeContaining(searchTerm);
    }
    
    /**
     * Get supplier ledger dashboard data
     */
    public SupplierLedgerDashboard getSupplierLedgerDashboard() {
        SupplierLedgerDashboard dashboard = new SupplierLedgerDashboard();
        
        try {
            // Get individual statistics using separate queries
            List<SupplierLedger> activeSuppliers = supplierLedgerRepository.findByStatus("ACTIVE");
            List<SupplierLedger> outstandingSuppliers = supplierLedgerRepository.findActiveLedgersWithOutstandingBalance();
            BigDecimal totalOutstanding = supplierLedgerRepository.calculateTotalOutstandingAmount();
            
            // Calculate totals
            BigDecimal totalDebits = BigDecimal.ZERO;
            BigDecimal totalCredits = BigDecimal.ZERO;
            
            for (SupplierLedger supplier : activeSuppliers) {
                if (supplier.getTotalDebits() != null) {
                    totalDebits = totalDebits.add(supplier.getTotalDebits());
                }
                if (supplier.getTotalCredits() != null) {
                    totalCredits = totalCredits.add(supplier.getTotalCredits());
                }
            }
            
            // Set dashboard values
            dashboard.setTotalSuppliers((long) activeSuppliers.size());
            dashboard.setSuppliersWithOutstanding((long) outstandingSuppliers.size());
            dashboard.setTotalOutstanding(totalOutstanding != null ? totalOutstanding : BigDecimal.ZERO);
            dashboard.setTotalDebits(totalDebits);
            dashboard.setTotalCredits(totalCredits);
            
        } catch (Exception e) {
            System.err.println("Error getting supplier ledger dashboard data: " + e.getMessage());
            e.printStackTrace();
            
            // Set default values on error
            dashboard.setTotalSuppliers(0L);
            dashboard.setSuppliersWithOutstanding(0L);
            dashboard.setTotalOutstanding(BigDecimal.ZERO);
            dashboard.setTotalDebits(BigDecimal.ZERO);
            dashboard.setTotalCredits(BigDecimal.ZERO);
        }
        
        return dashboard;
    }
    
    // ==================== SUPPLIER LEDGER ENTRIES ====================
    
    /**
     * Add purchase order entry to supplier ledger
     */
    @Transactional
    public void addPurchaseOrderEntry(SupplierLedger supplierLedger, PurchaseOrder purchaseOrder) {
        String particulars = "Purchase Order #" + purchaseOrder.getId();
        String referenceNumber = "PO-" + purchaseOrder.getId();
        
        // Calculate balance after this entry
        BigDecimal balanceAfter = supplierLedger.getCurrentBalance().add(purchaseOrder.getTotalAmount());
        
        SupplierLedgerEntry entry = new SupplierLedgerEntry(
            supplierLedger,
            particulars,
            "PURCHASE_ORDER",
            purchaseOrder.getId(),
            referenceNumber,
            purchaseOrder.getTotalAmount(), // Debit amount (what we owe)
            BigDecimal.ZERO, // Credit amount
            balanceAfter
        );
        
        supplierLedgerEntryRepository.save(entry);
        
        // Update supplier ledger
        supplierLedger.addDebit(purchaseOrder.getTotalAmount());
        supplierLedgerRepository.save(supplierLedger);
    }
    
    /**
     * Add payment entry to supplier ledger
     */
    @Transactional
    public void addPaymentEntry(SupplierLedger supplierLedger, BigDecimal paymentAmount, 
                               String paymentMethod, String paymentReference, String notes) {
        addPaymentEntry(supplierLedger, paymentAmount, paymentMethod, paymentReference, notes, true);
    }
    
    /**
     * Add payment entry to supplier ledger with option to sync with outstanding
     */
    @Transactional
    public void addPaymentEntry(SupplierLedger supplierLedger, BigDecimal paymentAmount, 
                               String paymentMethod, String paymentReference, String notes, 
                               boolean syncWithOutstanding) {
        String particulars = "Payment to " + supplierLedger.getSupplierName();
        String referenceNumber = "PAYMENT/" + supplierLedger.getId();
        
        // Calculate balance after this entry
        BigDecimal balanceAfter = supplierLedger.getCurrentBalance().subtract(paymentAmount);
        
        SupplierLedgerEntry entry = new SupplierLedgerEntry(
            supplierLedger,
            particulars,
            "PAYMENT",
            null,
            referenceNumber,
            BigDecimal.ZERO, // Debit amount
            paymentAmount, // Credit amount (payment reduces what we owe)
            balanceAfter
        );
        
        entry.setPaymentMethod(paymentMethod);
        entry.setPaymentReference(paymentReference);
        entry.setNotes(notes);
        
        supplierLedgerEntryRepository.save(entry);
        
        // Update supplier ledger
        supplierLedger.addCredit(paymentAmount);
        supplierLedgerRepository.save(supplierLedger);
        
        // Only sync with outstanding if explicitly requested (for manual ledger payments)
        if (syncWithOutstanding) {
            applyPaymentToOutstandingPayables(supplierLedger.getSupplierPhone(), paymentAmount, paymentMethod, paymentReference, notes);
        }
    }
    
    /**
     * Add credit note entry to supplier ledger
     */
    @Transactional
    public void addCreditNoteEntry(SupplierLedger supplierLedger, Long creditNoteId, 
                                  String creditNoteNumber, BigDecimal amount, String notes) {
        String particulars = "Credit Note #" + creditNoteNumber;
        
        // Calculate balance after this entry
        BigDecimal balanceAfter = supplierLedger.getCurrentBalance().subtract(amount);
        
        SupplierLedgerEntry entry = new SupplierLedgerEntry(
            supplierLedger,
            particulars,
            "CREDIT_NOTE",
            creditNoteId,
            creditNoteNumber,
            BigDecimal.ZERO, // Debit amount
            amount, // Credit amount (credit note reduces what we owe)
            balanceAfter
        );
        
        entry.setNotes(notes);
        
        supplierLedgerEntryRepository.save(entry);
        
        // Update supplier ledger
        supplierLedger.addCredit(amount);
        supplierLedgerRepository.save(supplierLedger);
    }
    
    /**
     * Get all entries for a supplier ledger
     */
    public List<SupplierLedgerEntry> getSupplierLedgerEntries(Long supplierLedgerId) {
        return supplierLedgerEntryRepository.findBySupplierLedgerIdOrderByEntryDateDesc(supplierLedgerId);
    }
    
    /**
     * Get entries by reference type and ID
     */
    public List<SupplierLedgerEntry> getEntriesByReference(String referenceType, Long referenceId) {
        return supplierLedgerEntryRepository.findByReferenceTypeAndReferenceId(referenceType, referenceId);
    }
    
    // ==================== PAYMENT PROCESSING ====================
    
    /**
     * Process payment for supplier
     */
    @Transactional
    public void processSupplierPayment(Long supplierLedgerId, BigDecimal paymentAmount, 
                                     String paymentMethod, String paymentReference, String notes) {
        Optional<SupplierLedger> ledgerOpt = supplierLedgerRepository.findById(supplierLedgerId);
        if (ledgerOpt.isEmpty()) {
            throw new RuntimeException("Supplier ledger not found");
        }
        
        SupplierLedger ledger = ledgerOpt.get();
        
        // Add payment entry
        addPaymentEntry(ledger, paymentAmount, paymentMethod, paymentReference, notes);
        
        // Create voucher for payment
        createPaymentVoucher(ledger, paymentAmount, paymentMethod, paymentReference, notes);
    }
    
    /**
     * Create voucher for supplier payment
     */
    private void createPaymentVoucher(SupplierLedger supplierLedger, BigDecimal paymentAmount, 
                                    String paymentMethod, String paymentReference, String notes) {
        try {
            // Get debit account based on payment method
        	System.out.println("paymentMethod ++"+paymentMethod);
            Account debitAccount = null;
            if ("cash".equals(paymentMethod)) {
                debitAccount = accountRepository.findById(5L).orElse(null);
            } else {
                debitAccount = accountRepository.findById(6L).orElse(null);
            }
            
            if (debitAccount == null) {
                System.err.println("Cannot find account for payment method: " + paymentMethod);
                return;
            }
            
            // Get credit account - Purchase / Cost of Goods Sold (ID 35)
            Account creditAccount = accountRepository.findById(22L).orElse(null);
            if (creditAccount == null) {
                System.err.println("Cannot find account with ID 35 (Purchase / Cost of Goods Sold)");
                return;
            }
            
            // Create voucher
            Voucher voucher = new Voucher();
            voucher.setDate(java.time.LocalDate.now());
            voucher.setType("Supplier Payment");
            
            String narration = "Payment to " + supplierLedger.getSupplierName() + " - " + paymentReference;
            if (notes != null && !notes.trim().isEmpty()) {
                narration += " - " + notes;
            }
            voucher.setNarration(narration);
            voucherRepository.save(voucher);
            
            // Create voucher entries - Debit Purchase/Cost of Goods Sold, Credit Cash/Bank
            createVoucherEntry(voucher, creditAccount, paymentAmount, true); // Debit Purchase/Cost of Goods Sold
            createVoucherEntry(voucher, debitAccount, paymentAmount, false); // Credit Cash/Bank
            
            System.out.println("Created supplier payment voucher for " + supplierLedger.getSupplierName());
            
        } catch (Exception e) {
            System.err.println("Error creating supplier payment voucher: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create voucher entry
     */
    private void createVoucherEntry(Voucher voucher, Account account, BigDecimal amount, boolean isDebit) {
        VoucherEntry entry = new VoucherEntry();
        entry.setVoucher(voucher);
        entry.setAccount(account);
        
        if (isDebit) {
            entry.setDebit(amount);
            entry.setCredit(BigDecimal.ZERO);
        } else {
            entry.setDebit(BigDecimal.ZERO);
            entry.setCredit(amount);
        }
        
        voucherEntryRepository.save(entry);
    }
    
    // ==================== OUTSTANDING PAYABLES SYNC ====================
    
    /**
     * Apply payment to outstanding payables for a supplier
     * This method applies payment to the oldest invoices first (FIFO principle)
     * Any excess payment is stored as advance payment for future invoices
     */
    @Transactional
    public void applyPaymentToOutstandingPayables(String supplierPhone, BigDecimal paymentAmount, 
                                                String paymentMethod, String paymentReference, String notes) {
        try {
            System.out.println("=== Starting payment application to outstanding payables ===");
            System.out.println("Supplier Phone: " + supplierPhone);
            System.out.println("Payment Amount: " + paymentAmount);
            System.out.println("Payment Method: " + paymentMethod);
            System.out.println("Payment Reference: " + paymentReference);
            
            // Get all non-settled payables for this supplier, ordered by creation date (oldest first)
            List<com.brsons.model.Outstanding> outstandingPayables = outstandingRepository
                .findPayablesForSupplierOldestFirst(supplierPhone);
            
            System.out.println("Found " + outstandingPayables.size() + " outstanding payables for supplier");
            
            // Calculate total outstanding amount
            BigDecimal totalOutstanding = outstandingPayables.stream()
                .map(com.brsons.model.Outstanding::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            System.out.println("Total outstanding amount: " + totalOutstanding);
            System.out.println("Payment amount: " + paymentAmount);
            
            BigDecimal remainingPayment = paymentAmount;
            int syncedCount = 0;
            
            // Apply payment to existing outstanding invoices (FIFO - oldest first)
            if (!outstandingPayables.isEmpty()) {
                for (com.brsons.model.Outstanding outstanding : outstandingPayables) {
                    if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) {
                        break; // No more payment to apply
                    }
                    
                    BigDecimal currentOutstandingAmount = outstanding.getAmount();
                    if (currentOutstandingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                        continue; // Skip already settled items
                    }
                    
                    // Calculate how much to pay for this invoice
                    BigDecimal paidAmount = remainingPayment.min(currentOutstandingAmount);
                    
                    System.out.println("Applying payment to outstanding item #" + outstanding.getId() + 
                                     " - Outstanding: ₹" + currentOutstandingAmount + 
                                     ", Paying: ₹" + paidAmount);
                    
                    // Update the outstanding item
                    outstanding.setAmount(currentOutstandingAmount.subtract(paidAmount));
                    outstanding.setPaymentMethod(paymentMethod);
                    outstanding.setPaymentReference(paymentReference);
                    outstanding.setNotes(notes);
                    
                    // If fully paid, mark as settled
                    if (outstanding.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                        outstanding.setStatus(com.brsons.model.Outstanding.OutstandingStatus.SETTLED);
                        System.out.println("Outstanding item #" + outstanding.getId() + " fully settled");
                    } else {
                        outstanding.setStatus(com.brsons.model.Outstanding.OutstandingStatus.PARTIALLY_PAID);
                        System.out.println("Outstanding item #" + outstanding.getId() + " partially paid - remaining: ₹" + outstanding.getAmount());
                    }
                    
                    // Save the updated outstanding item
                    outstandingRepository.save(outstanding);
                    
                    // Reduce remaining payment
                    remainingPayment = remainingPayment.subtract(paidAmount);
                    syncedCount++;
                    
                    System.out.println("Successfully applied payment to outstanding item #" + outstanding.getId() + 
                                     " - Paid: ₹" + paidAmount + ", Remaining: ₹" + outstanding.getAmount());
                }
            }
            
            // Handle any remaining payment as advance credit
            if (remainingPayment.compareTo(BigDecimal.ZERO) > 0) {
                System.out.println("Remaining payment amount: ₹" + remainingPayment + " - storing as advance credit");
                // Note: Advance credit handling can be implemented here if needed
                // For now, we'll just log it
            }
            
            System.out.println("=== Payment application completed ===");
            System.out.println("Synced " + syncedCount + " outstanding payables");
            System.out.println("Remaining payment: ₹" + remainingPayment);
            
        } catch (Exception e) {
            System.err.println("Error applying payment to outstanding payables: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to sync payment with outstanding payables", e);
        }
    }
    
    // ==================== DASHBOARD DATA CLASS ====================
    
    public static class SupplierLedgerDashboard {
        private Long totalSuppliers;
        private Long suppliersWithOutstanding;
        private BigDecimal totalOutstanding;
        private BigDecimal totalDebits;
        private BigDecimal totalCredits;
        
        // Getters and Setters
        public Long getTotalSuppliers() { return totalSuppliers; }
        public void setTotalSuppliers(Long totalSuppliers) { this.totalSuppliers = totalSuppliers; }
        
        public Long getSuppliersWithOutstanding() { return suppliersWithOutstanding; }
        public void setSuppliersWithOutstanding(Long suppliersWithOutstanding) { this.suppliersWithOutstanding = suppliersWithOutstanding; }
        
        public BigDecimal getTotalOutstanding() { return totalOutstanding; }
        public void setTotalOutstanding(BigDecimal totalOutstanding) { this.totalOutstanding = totalOutstanding; }
        
        public BigDecimal getTotalDebits() { return totalDebits; }
        public void setTotalDebits(BigDecimal totalDebits) { this.totalDebits = totalDebits; }
        
        public BigDecimal getTotalCredits() { return totalCredits; }
        public void setTotalCredits(BigDecimal totalCredits) { this.totalCredits = totalCredits; }
    }
}
