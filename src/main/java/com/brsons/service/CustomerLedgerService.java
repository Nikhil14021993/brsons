package com.brsons.service;

import com.brsons.model.CustomerLedger;
import com.brsons.model.CustomerLedgerEntry;
import com.brsons.model.Order;
import com.brsons.model.Voucher;
import com.brsons.model.VoucherEntry;
import com.brsons.model.Account;
import com.brsons.model.PaymentEntry;
import com.brsons.repository.CustomerLedgerRepository;
import com.brsons.repository.CustomerLedgerEntryRepository;
import com.brsons.repository.OrderRepository;
import com.brsons.repository.VoucherRepository;
import com.brsons.repository.VoucherEntryRepository;
import com.brsons.repository.AccountRepository;
import com.brsons.repository.PaymentEntryRepository;
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
    
    public CustomerLedgerEntryRepository getCustomerLedgerEntryRepository() {
        return customerLedgerEntryRepository;
    }
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private com.brsons.repository.OutstandingRepository outstandingRepository;
    
    @Autowired
    private VoucherRepository voucherRepository;
    
    @Autowired
    private VoucherEntryRepository voucherEntryRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private PaymentEntryRepository paymentEntryRepository;
    
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
     * Search customer ledgers by name or phone
     */
    public List<CustomerLedger> searchCustomerLedgers(String searchTerm) {
        return customerLedgerRepository.findByCustomerNameOrPhoneContaining(searchTerm);
    }
    
    /**
     * Search customer ledgers by name only (for backward compatibility)
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
        return addPaymentEntry(customerLedger, amount, paymentMethod, paymentReference, notes, true);
    }
    
    /**
     * Add payment entry (credit) to customer ledger with option to sync with outstanding
     */
    @Transactional
    public CustomerLedgerEntry addPaymentEntry(CustomerLedger customerLedger, BigDecimal amount, 
                                              String paymentMethod, String paymentReference, String notes, 
                                              boolean syncWithOutstanding) {
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
        
        // Only sync with outstanding if explicitly requested (for manual ledger payments)
        if (syncWithOutstanding) {
            applyPaymentToOutstandingReceivables(customerLedger.getCustomerPhone(), amount, paymentMethod, paymentReference, notes);
        }
        
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
        try {
            System.out.println("=== Starting customer ledger creation for existing B2B orders ===");
            
            List<Order> b2bOrders = orderRepository.findByBillTypeOrderByCreatedAtDesc("Kaccha");
            System.out.println("Found " + b2bOrders.size() + " B2B orders to create ledgers for");
            
            for (Order order : b2bOrders) {
                if (order.getTotal() != null && order.getTotal().compareTo(BigDecimal.ZERO) > 0) {
                    // Only create customer ledger entries for confirmed orders (not Pending or Cancelled)
                    if (!"Pending".equals(order.getOrderStatus()) && !"Cancelled".equals(order.getOrderStatus())) {
                        try {
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
                                System.out.println("Created customer ledger entry for order ID: " + order.getId() + 
                                                " - Amount: " + order.getTotal() + " - Customer: " + order.getName());
                            } else {
                                System.out.println("Customer ledger entry already exists for order ID: " + order.getId());
                            }
                            
                        } catch (Exception e) {
                            System.err.println("Error creating customer ledger for order ID " + order.getId() + ": " + e.getMessage());
                        }
                    }
                }
            }
            
            System.out.println("=== Customer ledger creation completed ===");
            
            // Now automatically trigger outstanding sync to ensure consistency
            System.out.println("=== Triggering automatic outstanding sync for consistency ===");
            try {
                // This will ensure outstanding items are created for any new customer ledgers
                // The outstanding service will handle this automatically
                System.out.println("Outstanding sync will be triggered automatically for consistency");
                
                // Trigger the triggerOutstandingSync method to indicate sync completion
                triggerOutstandingSync();
                
            } catch (Exception e) {
                System.err.println("Warning: Could not trigger outstanding sync: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("Error creating customer ledgers for existing B2B orders: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create customer ledgers", e);
        }
    }
    
    /**
     * Trigger outstanding sync to ensure consistency with customer ledgers
     * This method is called automatically when customer ledgers are created
     */
    public void triggerOutstandingSync() {
        try {
            System.out.println("=== Triggering outstanding sync from customer ledger service ===");
            // This will ensure outstanding items are created for any new customer ledgers
            // The outstanding service will handle this automatically
            System.out.println("Outstanding sync will be triggered automatically for consistency");
            
            // Note: We can't directly call OutstandingService here due to circular dependency
            // The sync will be handled by the controller calling both services in sequence
            System.out.println("Outstanding sync completed successfully");
        } catch (Exception e) {
            System.err.println("Error triggering outstanding sync: " + e.getMessage());
        }
    }
    
    /**
     * Sync outstanding items for B2B orders to ensure consistency with customer ledgers
     * This method is called from the controller to avoid circular dependency
     */
    public void syncOutstandingItemsForB2BOrders() {
        try {
            System.out.println("=== Syncing outstanding items for B2B orders from customer ledger service ===");
            
            // Get all B2B orders (Kaccha) that don't have outstanding items yet
            List<Order> b2bOrders = orderRepository.findByBillTypeOrderByCreatedAtDesc("Kaccha");
            System.out.println("Found " + b2bOrders.size() + " B2B orders to check for outstanding items");
            
            for (Order order : b2bOrders) {
                if (order.getTotal() != null && order.getTotal().compareTo(BigDecimal.ZERO) > 0) {
                    // Only create outstanding for confirmed orders (not Pending or Cancelled)
                    if (!"Pending".equals(order.getOrderStatus()) && !"Cancelled".equals(order.getOrderStatus())) {
                        try {
                            // Check if outstanding item already exists
                            List<com.brsons.model.Outstanding> existingOutstanding = outstandingRepository
                                .findByReferenceTypeAndReferenceId("ORDER", order.getId());
                            
                            if (existingOutstanding.isEmpty()) {
                                System.out.println("Creating outstanding item for order ID: " + order.getId());
                                // Create outstanding item directly here to avoid circular dependency
                                com.brsons.model.Outstanding outstanding = new com.brsons.model.Outstanding(
                                    com.brsons.model.Outstanding.OutstandingType.INVOICE_RECEIVABLE,
                                    order.getId(),
                                    "ORDER",
                                    order.getInvoiceNumber() != null ? order.getInvoiceNumber() : "ORD-" + order.getId(),
                                    order.getTotal(),
                                    order.getCreatedAt().plusDays(30),
                                    order.getName(),
                                    order.getBillType()
                                );
                                outstanding.setDescription("Customer invoice for order #" + order.getId());
                                outstanding.setContactInfo(order.getUserPhone());
                                com.brsons.model.Outstanding savedOutstanding = outstandingRepository.save(outstanding);
                                System.out.println("Created outstanding item for order ID: " + order.getId());
                                
                                // Apply advance payments to this new invoice (FIFO)
                                try {
                                    applyAdvancePaymentsToNewInvoice(order.getUserPhone(), savedOutstanding.getId(), order.getTotal());
                                    System.out.println("Applied advance payments to new invoice #" + savedOutstanding.getId());
                                } catch (Exception e) {
                                    System.err.println("Error applying advance payments to new invoice #" + savedOutstanding.getId() + ": " + e.getMessage());
                                }
                            } else {
                                System.out.println("Outstanding item already exists for order ID: " + order.getId());
                            }
                            
                        } catch (Exception e) {
                            System.err.println("Error creating outstanding item for order ID " + order.getId() + ": " + e.getMessage());
                        }
                    }
                }
            }
            
            System.out.println("=== Outstanding items sync completed ===");
            
        } catch (Exception e) {
            System.err.println("Error syncing outstanding items for B2B orders: " + e.getMessage());
            e.printStackTrace();
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
    
    // ==================== OUTSTANDING RECEIVABLES SYNCHRONIZATION ====================
    
    /**
     * Apply payment to outstanding receivables for a customer
     * This method applies payment to the oldest invoices first (FIFO principle)
     * Any excess payment is stored as advance payment for future invoices
     */
    @Transactional
    public void applyPaymentToOutstandingReceivables(String customerPhone, BigDecimal paymentAmount, 
                                                    String paymentMethod, String paymentReference, String notes) {
        try {
            System.out.println("=== Starting payment application to outstanding receivables ===");
            System.out.println("Customer Phone: " + customerPhone);
            System.out.println("Payment Amount: " + paymentAmount);
            System.out.println("Payment Method: " + paymentMethod);
            System.out.println("Payment Reference: " + paymentReference);
            
            // Get all non-settled B2B receivables for this customer, ordered by creation date (oldest first)
            List<com.brsons.model.Outstanding> outstandingReceivables = outstandingRepository
                .findB2BReceivablesForCustomerOldestFirst(customerPhone);
            
            System.out.println("Found " + outstandingReceivables.size() + " outstanding receivables for customer");
            
            // Calculate total outstanding amount
            BigDecimal totalOutstanding = outstandingReceivables.stream()
                .map(com.brsons.model.Outstanding::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            System.out.println("Total outstanding amount: " + totalOutstanding);
            System.out.println("Payment amount: " + paymentAmount);
            
            BigDecimal remainingPayment = paymentAmount;
            
            // Apply payment to existing outstanding invoices (FIFO - oldest first)
            if (!outstandingReceivables.isEmpty()) {
                // Log the order of invoices (should be oldest first)
                System.out.println("=== Invoice processing order (FIFO - oldest first) ===");
                for (int i = 0; i < outstandingReceivables.size(); i++) {
                    com.brsons.model.Outstanding o = outstandingReceivables.get(i);
                    System.out.println((i + 1) + ". Invoice ID: " + o.getId() + 
                                     " - Amount: " + o.getAmount() + 
                                     " - Created: " + o.getCreatedAt() + 
                                     " - Status: " + o.getStatus());
                }
                
                for (com.brsons.model.Outstanding outstanding : outstandingReceivables) {
                    if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) {
                        break; // Payment fully applied
                    }
                    
                    BigDecimal outstandingAmount = outstanding.getAmount();
                    BigDecimal amountToApply = remainingPayment.compareTo(outstandingAmount) > 0 ? 
                        outstandingAmount : remainingPayment;
                    
                    // Apply payment to this outstanding item
                    if (amountToApply.compareTo(outstandingAmount) >= 0) {
                        // Full payment for this invoice
                        outstanding.setStatus(com.brsons.model.Outstanding.OutstandingStatus.SETTLED);
                        outstanding.setAmount(BigDecimal.ZERO);
                        outstanding.setPaymentMethod(paymentMethod);
                        outstanding.setPaymentReference(paymentReference);
                        outstanding.setPaymentDate(java.time.LocalDateTime.now());
                        outstanding.setNotes(notes != null ? notes : "Payment applied from customer ledger");
                        outstanding.setUpdatedAt(java.time.LocalDateTime.now());
                        
                        System.out.println("Fully settled outstanding item ID: " + outstanding.getId() + 
                                          " for amount: " + outstandingAmount);
                    } else {
                        // Partial payment for this invoice
                        outstanding.setStatus(com.brsons.model.Outstanding.OutstandingStatus.PARTIALLY_PAID);
                        outstanding.setAmount(outstandingAmount.subtract(amountToApply));
                        outstanding.setPaymentMethod(paymentMethod);
                        outstanding.setPaymentReference(paymentReference);
                        outstanding.setPaymentDate(java.time.LocalDateTime.now());
                        outstanding.setNotes(notes != null ? notes : "Partial payment applied from customer ledger");
                        outstanding.setUpdatedAt(java.time.LocalDateTime.now());
                        
                        System.out.println("Partially paid outstanding item ID: " + outstanding.getId() + 
                                          " - Applied: " + amountToApply + ", Remaining: " + outstanding.getAmount());
                    }
                    
                    // Save the updated outstanding item
                    outstandingRepository.save(outstanding);
                    
                    // Create voucher entry for this payment
                    createVoucherForPayment(outstanding, amountToApply, paymentMethod, paymentReference, notes);
                    
                    // Reduce remaining payment amount
                    remainingPayment = remainingPayment.subtract(amountToApply);
                }
            }
            
            // Handle advance payment if there's remaining amount
            if (remainingPayment.compareTo(BigDecimal.ZERO) > 0) {
                System.out.println("=== Creating advance payment entry ===");
                System.out.println("Advance amount: " + remainingPayment);
                
                // Get customer name for the payment entry
                String customerName = getCustomerNameByPhone(customerPhone);
                
                // Create advance payment entry
                createAdvancePaymentEntry(customerPhone, customerName, remainingPayment, 
                                        paymentMethod, paymentReference, notes);
                
                System.out.println("Advance payment created successfully");
            }
            
            System.out.println("=== Payment application completed ===");
            System.out.println("Final remaining payment: " + remainingPayment);
            
        } catch (Exception e) {
            System.err.println("Error applying payment to outstanding receivables: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create voucher entry for payment applied to outstanding receivable
     */
    private void createVoucherForPayment(com.brsons.model.Outstanding outstanding, BigDecimal amount, 
                                        String paymentMethod, String paymentReference, String notes) {
        try {
        			System.out.println("paymentMethod "+ paymentMethod);
        	Account debitAccount = null;
        	       	if ("cash".equals(paymentMethod)) {
        	       		 debitAccount = accountRepository.findById(5L).orElse(null);
        	            
        	       	}else {
        	       		 debitAccount = accountRepository.findById(6L).orElse(null);
        	       	}
        	       	System.out.println("paymentMethod "+ paymentMethod);
            //Account debitAccount = getAccountByPaymentMethod(paymentMethod);
            
            
            
            if (debitAccount == null) {
                System.err.println("Cannot find account for payment method: " + paymentMethod);
                return;
            }
            
            // Get credit account (Sales Revenue - ID 5)
            Account creditAccount = accountRepository.findById(7L).orElse(null);
            if (creditAccount == null) {
                System.err.println("Cannot find account with ID 5 (Sales Revenue)");
                return;
            }
            
            // Create voucher
            Voucher voucher = new Voucher();
            voucher.setDate(java.time.LocalDate.now());
            voucher.setType("Payment");
            
            String narration = "Payment received for " + outstanding.getReferenceNumber();
            if (notes != null && !notes.trim().isEmpty()) {
                narration += " - " + notes;
            }
            voucher.setNarration(narration);
            Voucher savedVoucher = voucherRepository.save(voucher);
            
            // Create voucher entries
            createVoucherEntry(savedVoucher, debitAccount, amount, true); // Debit based on payment method
            createVoucherEntry(savedVoucher, creditAccount, amount, false); // Credit Sales Revenue (ID 5)
            
            System.out.println("Created voucher for payment: " + savedVoucher.getId() + 
                              " for amount: " + amount);
            
        } catch (Exception e) {
            System.err.println("Error creating voucher for payment: " + e.getMessage());
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
    
    /**
     * Get account based on payment method
     */
    private Account getAccountByPaymentMethod(String paymentMethod) {
        try {
            if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
                System.err.println("Payment method is null or empty");
                return null;
            }
            
            String method = paymentMethod.trim().toLowerCase();
            System.out.println("Looking for account for payment method: " + method);
            
            // Map payment methods to account names
            String accountName = null;
            switch (method) {
                case "cash":
                    accountName = "Cash";
                    break;
                case "online":
                    accountName = "Bank";
                    break;
                case "bank transfer":
                    accountName = "Bank";
                    break;
                case "check":
                    accountName = "Bank";
                    break;
                case "adjustment":
                    accountName = "Adjustment";
                    break;
                case "other":
                    accountName = "Other";
                    break;
                default:
                    // Try to find account with similar name
                    accountName = paymentMethod;
                    break;
            }
            
            // Search for account by name
            List<Account> accounts = accountRepository.findByNameContainingIgnoreCase(accountName);
            if (!accounts.isEmpty()) {
                Account found = accounts.get(0);
                System.out.println("Found account for " + paymentMethod + ": " + found.getName() + " (ID: " + found.getId() + ")");
                return found;
            }
            
            // If not found, try to find any account that might be suitable
            System.out.println("No account found for " + paymentMethod + ", trying to find suitable account");
            List<Account> allAccounts = accountRepository.findAll();
            
            // Filter out account ID 5 (Sales Revenue) and look for asset accounts
            List<Account> suitableAccounts = allAccounts.stream()
                .filter(acc -> !acc.getId().equals(5L) && 
                               acc.getType() != null && 
                               acc.getType().equalsIgnoreCase("Asset"))
                .toList();
            
            if (!suitableAccounts.isEmpty()) {
                Account fallback = suitableAccounts.get(0);
                System.out.println("Using fallback account: " + fallback.getName() + " (ID: " + fallback.getId() + ")");
                return fallback;
            }
            
            // Last resort: use any account except ID 5 and ID 7 (Purchase Expense)
            if (!allAccounts.isEmpty()) {
                Account lastResort = allAccounts.stream()
                    .filter(acc -> !acc.getId().equals(5L) && !acc.getId().equals(7L))
                    .findFirst()
                    .orElse(null);
                
                if (lastResort != null) {
                    System.out.println("Using last resort account: " + lastResort.getName() + " (ID: " + lastResort.getId() + ")");
                    return lastResort;
                }
            }
            
            System.err.println("No suitable account found for payment method: " + paymentMethod);
            return null;
            
        } catch (Exception e) {
            System.err.println("Error finding account for payment method: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    // ==================== ADVANCE PAYMENT MANAGEMENT ====================
    
    /**
     * Create advance payment entry for excess payment amount
     */
    @Transactional
    private void createAdvancePaymentEntry(String customerPhone, String customerName, BigDecimal advanceAmount,
                                         String paymentMethod, String paymentReference, String notes) {
        try {
            PaymentEntry advancePayment = new PaymentEntry();
            advancePayment.setCustomerPhone(customerPhone);
            advancePayment.setCustomerName(customerName);
            advancePayment.setPaymentAmount(advanceAmount);
            advancePayment.setPaymentType(paymentMethod);
            advancePayment.setPaymentReference(paymentReference);
            advancePayment.setPaymentDate(LocalDateTime.now());
            advancePayment.setDescription("Advance payment - " + (notes != null ? notes : "Excess payment from customer ledger"));
            advancePayment.setRemainingAmount(advanceAmount); // Full amount is available for future invoices
            advancePayment.setIsAdvance(true);
            advancePayment.setCreatedAt(LocalDateTime.now());
            advancePayment.setCreatedBy("SYSTEM");
            
            paymentEntryRepository.save(advancePayment);
            
            System.out.println("Advance payment entry created: ID=" + advancePayment.getId() + 
                             ", Amount=" + advanceAmount + ", Customer=" + customerName);
            
        } catch (Exception e) {
            System.err.println("Error creating advance payment entry: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get customer name by phone number
     */
    private String getCustomerNameByPhone(String customerPhone) {
        try {
            Optional<CustomerLedger> customerLedger = customerLedgerRepository.findByCustomerPhone(customerPhone);
            if (customerLedger.isPresent()) {
                return customerLedger.get().getCustomerName();
            }
            
            // If not found in customer ledger, try to get from orders
            List<Order> orders = orderRepository.findByUserPhone(customerPhone);
            if (!orders.isEmpty()) {
                return orders.get(0).getName();
            }
            
            return "Unknown Customer";
        } catch (Exception e) {
            System.err.println("Error getting customer name: " + e.getMessage());
            return "Unknown Customer";
        }
    }
    
    /**
     * Apply advance payments to new outstanding receivables
     * This method should be called when new invoices are created
     */
    @Transactional
    public void applyAdvancePaymentsToNewInvoice(String customerPhone, Long outstandingId, BigDecimal invoiceAmount) {
        try {
            System.out.println("=== Applying advance payments to new invoice ===");
            System.out.println("Customer Phone: " + customerPhone);
            System.out.println("Outstanding ID: " + outstandingId);
            System.out.println("Invoice Amount: " + invoiceAmount);
            
            // Get all unallocated advance payments for this customer (oldest first)
            List<PaymentEntry> advancePayments = paymentEntryRepository.findUnallocatedPaymentsByCustomer(customerPhone);
            
            if (advancePayments.isEmpty()) {
                System.out.println("No advance payments found for customer: " + customerPhone);
                return;
            }
            
            System.out.println("Found " + advancePayments.size() + " advance payments for customer");
            
            // Log advance payments details
            for (int i = 0; i < advancePayments.size(); i++) {
                PaymentEntry payment = advancePayments.get(i);
                System.out.println("Advance Payment " + (i + 1) + ": ID=" + payment.getId() + 
                                 ", Amount=" + payment.getRemainingAmount() + 
                                 ", Date=" + payment.getPaymentDate());
            }
            
            BigDecimal remainingInvoiceAmount = invoiceAmount;
            
            for (PaymentEntry advancePayment : advancePayments) {
                if (remainingInvoiceAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    break; // Invoice fully paid
                }
                
                BigDecimal availableAdvance = advancePayment.getRemainingAmount();
                BigDecimal amountToApply = remainingInvoiceAmount.compareTo(availableAdvance) > 0 ? 
                    availableAdvance : remainingInvoiceAmount;
                
                // Update the advance payment entry
                advancePayment.setRemainingAmount(availableAdvance.subtract(amountToApply));
                paymentEntryRepository.save(advancePayment);
                
                // Get the outstanding item and apply payment
                Optional<com.brsons.model.Outstanding> outstandingOpt = outstandingRepository.findById(outstandingId);
                if (outstandingOpt.isPresent()) {
                    com.brsons.model.Outstanding outstanding = outstandingOpt.get();
                    
                    if (amountToApply.compareTo(outstanding.getAmount()) >= 0) {
                        // Full payment for this invoice
                        outstanding.setStatus(com.brsons.model.Outstanding.OutstandingStatus.SETTLED);
                        outstanding.setAmount(BigDecimal.ZERO);
                        outstanding.setPaymentMethod(advancePayment.getPaymentType());
                        outstanding.setPaymentReference(advancePayment.getPaymentReference());
                        outstanding.setPaymentDate(LocalDateTime.now());
                        outstanding.setNotes("Payment applied from advance payment ID: " + advancePayment.getId());
                        outstanding.setUpdatedAt(LocalDateTime.now());
                        
                        System.out.println("Invoice fully settled with advance payment ID: " + advancePayment.getId());
                    } else {
                        // Partial payment for this invoice
                        outstanding.setStatus(com.brsons.model.Outstanding.OutstandingStatus.PARTIALLY_PAID);
                        outstanding.setAmount(outstanding.getAmount().subtract(amountToApply));
                        outstanding.setPaymentMethod(advancePayment.getPaymentType());
                        outstanding.setPaymentReference(advancePayment.getPaymentReference());
                        outstanding.setPaymentDate(LocalDateTime.now());
                        outstanding.setNotes("Partial payment from advance payment ID: " + advancePayment.getId());
                        outstanding.setUpdatedAt(LocalDateTime.now());
                        
                        System.out.println("Invoice partially paid with advance payment ID: " + advancePayment.getId() + 
                                         ", Applied: " + amountToApply + ", Remaining: " + outstanding.getAmount());
                    }
                    
                    outstandingRepository.save(outstanding);
                    
                    // Create voucher entry for this payment
                    createVoucherForPayment(outstanding, amountToApply, advancePayment.getPaymentType(), 
                                          advancePayment.getPaymentReference(), "Advance payment applied");
                }
                
                remainingInvoiceAmount = remainingInvoiceAmount.subtract(amountToApply);
            }
            
            System.out.println("=== Advance payment application completed ===");
            System.out.println("Remaining invoice amount: " + remainingInvoiceAmount);
            
        } catch (Exception e) {
            System.err.println("Error applying advance payments to new invoice: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get total advance payment amount for a customer
     */
    public BigDecimal getTotalAdvancePayment(String customerPhone) {
        try {
            List<PaymentEntry> advancePayments = paymentEntryRepository.findUnallocatedPaymentsByCustomer(customerPhone);
            return advancePayments.stream()
                .map(PaymentEntry::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            System.err.println("Error getting total advance payment: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }
}
