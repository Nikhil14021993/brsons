package com.brsons.service;

import com.brsons.model.CustomerLedger;
import com.brsons.model.CustomerLedgerEntry;
import com.brsons.model.Order;
import com.brsons.repository.CustomerLedgerRepository;
import com.brsons.repository.CustomerLedgerEntryRepository;
import com.brsons.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerLedgerService {
    
    @Autowired
    private CustomerLedgerRepository customerLedgerRepository;
    
    @Autowired
    private CustomerLedgerEntryRepository customerLedgerEntryRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    // ==================== CUSTOMER LEDGER MANAGEMENT ====================
    
    /**
     * Create or find customer ledger by phone number
     */
    @Transactional
    public CustomerLedger findOrCreateCustomerLedger(String customerName, String customerPhone, String customerEmail) {
        Optional<CustomerLedger> existingLedger = customerLedgerRepository.findByCustomerPhone(customerPhone);
        
        if (existingLedger.isPresent()) {
            CustomerLedger ledger = existingLedger.get();
            // Update customer information if needed
            if (customerName != null && !customerName.equals(ledger.getCustomerName())) {
                ledger.setCustomerName(customerName);
            }
            if (customerEmail != null && !customerEmail.equals(ledger.getCustomerEmail())) {
                ledger.setCustomerEmail(customerEmail);
            }
            return customerLedgerRepository.save(ledger);
        } else {
            // Create new customer ledger
            CustomerLedger newLedger = new CustomerLedger(customerName, customerPhone);
            newLedger.setCustomerEmail(customerEmail);
            return customerLedgerRepository.save(newLedger);
        }
    }
    
    /**
     * Get customer ledger by phone number
     */
    public Optional<CustomerLedger> getCustomerLedgerByPhone(String customerPhone) {
        return customerLedgerRepository.findByCustomerPhone(customerPhone);
    }
    
    /**
     * Get customer ledger by ID
     */
    public Optional<CustomerLedger> getCustomerLedgerById(Long id) {
        return customerLedgerRepository.findById(id);
    }
    
    /**
     * Get all active customer ledgers
     */
    public List<CustomerLedger> getAllActiveCustomerLedgers() {
        return customerLedgerRepository.findByStatus("ACTIVE");
    }
    
    /**
     * Get customer ledgers with outstanding balance
     */
    public List<CustomerLedger> getCustomerLedgersWithOutstandingBalance() {
        return customerLedgerRepository.findActiveLedgersWithOutstandingBalance();
    }
    
    /**
     * Search customer ledgers by name
     */
    public List<CustomerLedger> searchCustomerLedgersByName(String customerName) {
        return customerLedgerRepository.findByCustomerNameContainingIgnoreCase(customerName);
    }
    
    // ==================== LEDGER ENTRIES MANAGEMENT ====================
    
    /**
     * Add invoice entry (debit) to customer ledger
     */
    @Transactional
    public CustomerLedgerEntry addInvoiceEntry(CustomerLedger customerLedger, Order order, BigDecimal amount) {
        // Create ledger entry
        CustomerLedgerEntry entry = new CustomerLedgerEntry(
            customerLedger, 
            "Invoice #" + (order.getInvoiceNumber() != null ? order.getInvoiceNumber() : "ORD-" + order.getId()),
            "ORDER",
            order.getId(),
            order.getInvoiceNumber() != null ? order.getInvoiceNumber() : "ORD-" + order.getId()
        );
        
        entry.setDebitAmount(amount);
        entry.setBalanceAfter(customerLedger.getCurrentBalance().add(amount));
        
        // Save entry
        CustomerLedgerEntry savedEntry = customerLedgerEntryRepository.save(entry);
        
        // Update customer ledger balance
        customerLedger.addDebit(amount);
        customerLedgerRepository.save(customerLedger);
        
        return savedEntry;
    }
    
    /**
     * Add payment entry (credit) to customer ledger
     */
    @Transactional
    public CustomerLedgerEntry addPaymentEntry(CustomerLedger customerLedger, BigDecimal amount, 
                                              String paymentMethod, String paymentReference, String notes) {
        // Create ledger entry
        CustomerLedgerEntry entry = new CustomerLedgerEntry(
            customerLedger,
            "Payment Received",
            "PAYMENT",
            null,
            paymentReference != null ? paymentReference : "PAY-" + System.currentTimeMillis()
        );
        
        entry.setCreditAmount(amount);
        entry.setBalanceAfter(customerLedger.getCurrentBalance().subtract(amount));
        entry.setPaymentMethod(paymentMethod);
        entry.setPaymentReference(paymentReference);
        entry.setNotes(notes);
        
        // Save entry
        CustomerLedgerEntry savedEntry = customerLedgerEntryRepository.save(entry);
        
        // Update customer ledger balance
        customerLedger.addCredit(amount);
        customerLedgerRepository.save(customerLedger);
        
        return savedEntry;
    }
    
    /**
     * Add adjustment entry to customer ledger
     */
    @Transactional
    public CustomerLedgerEntry addAdjustmentEntry(CustomerLedger customerLedger, BigDecimal amount, 
                                                 boolean isDebit, String reason, String notes) {
        String particulars = isDebit ? "Adjustment (Debit)" : "Adjustment (Credit)";
        
        CustomerLedgerEntry entry = new CustomerLedgerEntry(
            customerLedger,
            particulars + " - " + reason,
            "ADJUSTMENT",
            null,
            "ADJ-" + System.currentTimeMillis()
        );
        
        if (isDebit) {
            entry.setDebitAmount(amount);
            entry.setBalanceAfter(customerLedger.getCurrentBalance().add(amount));
        } else {
            entry.setCreditAmount(amount);
            entry.setBalanceAfter(customerLedger.getCurrentBalance().subtract(amount));
        }
        
        entry.setNotes(notes);
        
        // Save entry
        CustomerLedgerEntry savedEntry = customerLedgerEntryRepository.save(entry);
        
        // Update customer ledger balance
        if (isDebit) {
            customerLedger.addDebit(amount);
        } else {
            customerLedger.addCredit(amount);
        }
        customerLedgerRepository.save(customerLedger);
        
        return savedEntry;
    }
    
    // ==================== LEDGER QUERIES ====================
    
    /**
     * Get all entries for a customer ledger
     */
    public List<CustomerLedgerEntry> getCustomerLedgerEntries(Long customerLedgerId) {
        return customerLedgerEntryRepository.findByCustomerLedgerIdOrderByEntryDateDesc(customerLedgerId);
    }
    
    /**
     * Get customer ledger entries with pagination
     */
    public List<CustomerLedgerEntry> getCustomerLedgerEntries(Long customerLedgerId, int page, int size) {
        return customerLedgerEntryRepository.findByCustomerLedgerIdOrderByEntryDateDesc(
            customerLedgerId, 
            org.springframework.data.domain.PageRequest.of(page, size)
        ).getContent();
    }
    
    /**
     * Get entries within date range for a customer ledger
     */
    public List<CustomerLedgerEntry> getCustomerLedgerEntriesByDateRange(Long customerLedgerId, 
                                                                       LocalDateTime startDate, 
                                                                       LocalDateTime endDate) {
        return customerLedgerEntryRepository.findByCustomerLedgerAndDateRange(customerLedgerId, startDate, endDate);
    }
    
    /**
     * Get latest entry for a customer ledger
     */
    public CustomerLedgerEntry getLatestEntry(Long customerLedgerId) {
        return customerLedgerEntryRepository.findLatestEntryByCustomerLedgerId(customerLedgerId);
    }
    
    /**
     * Get customer ledger entries by reference type and ID
     */
    public List<CustomerLedgerEntry> getCustomerLedgerEntriesByReference(String referenceType, Long referenceId) {
        return customerLedgerEntryRepository.findByReferenceTypeAndReferenceId(referenceType, referenceId);
    }
    
    // ==================== AUTOMATIC LEDGER CREATION ====================
    
    /**
     * Create customer ledgers for existing B2B orders (Kaccha)
     */
    @Transactional
    public void createCustomerLedgersForExistingB2BOrders() {
        List<Order> b2bOrders = orderRepository.findByBillTypeOrderByCreatedAtDesc("Kaccha");
        
        for (Order order : b2bOrders) {
            if (order.getTotal() != null && order.getTotal().compareTo(BigDecimal.ZERO) > 0) {
                // Find or create customer ledger
                CustomerLedger customerLedger = findOrCreateCustomerLedger(
                    order.getName(), 
                    order.getUserPhone(), 
                    null // Order doesn't have email field
                );
                
                // Check if invoice entry already exists
                List<CustomerLedgerEntry> existingEntries = customerLedgerEntryRepository
                    .findByReferenceTypeAndReferenceId("ORDER", order.getId());
                
                if (existingEntries.isEmpty()) {
                    // Add invoice entry
                    addInvoiceEntry(customerLedger, order, order.getTotal());
                }
            }
        }
    }
    
    /**
     * Get dashboard summary for customer ledgers
     */
    public java.util.Map<String, Object> getCustomerLedgerDashboard() {
        java.util.Map<String, Object> dashboard = new java.util.HashMap<>();
        
        BigDecimal totalOutstanding = customerLedgerRepository.calculateTotalOutstandingAmount();
        Long activeLedgersCount = (long) customerLedgerRepository.findByStatus("ACTIVE").size();
        Long ledgersWithOutstandingCount = customerLedgerRepository.countActiveLedgersWithOutstandingBalance();
        Long totalLedgersCount = customerLedgerRepository.count();
        
        List<CustomerLedger> activeLedgers = customerLedgerRepository.findByStatus("ACTIVE");
        List<CustomerLedger> topOutstandingLedgers = customerLedgerRepository
            .findByBalanceGreaterThan(BigDecimal.valueOf(10000))
            .stream()
            .limit(10)
            .toList();
        
        dashboard.put("totalOutstanding", totalOutstanding != null ? totalOutstanding : BigDecimal.ZERO);
        dashboard.put("activeLedgersCount", activeLedgersCount);
        dashboard.put("ledgersWithOutstandingCount", ledgersWithOutstandingCount);
        dashboard.put("totalLedgersCount", totalLedgersCount);
        dashboard.put("activeLedgers", activeLedgers);
        dashboard.put("topOutstandingLedgers", topOutstandingLedgers);
        
        return dashboard;
    }
}
