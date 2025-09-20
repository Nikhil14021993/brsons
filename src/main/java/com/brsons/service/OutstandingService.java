package com.brsons.service;

import com.brsons.model.Outstanding;

import com.brsons.model.Order;
import com.brsons.model.PurchaseOrder;
import com.brsons.model.Supplier;
import com.brsons.model.Voucher;
import com.brsons.model.VoucherEntry;
import com.brsons.model.Account;
import com.brsons.model.CustomerLedger;
import com.brsons.model.CustomerLedgerEntry;
import com.brsons.model.SupplierLedger;
import com.brsons.repository.OutstandingRepository;
import com.brsons.repository.OrderRepository;
import com.brsons.repository.PurchaseOrderRepository;
import com.brsons.repository.SupplierRepository;
import com.brsons.repository.VoucherRepository;
import com.brsons.repository.VoucherEntryRepository;
import com.brsons.repository.GRNRepository;
import com.brsons.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Arrays;

@Service
public class OutstandingService {
    
    @Autowired
    private OutstandingRepository outstandingRepository;
    
    public OutstandingRepository getOutstandingRepository() {
        return outstandingRepository;
    }
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;
    
    @Autowired
    private SupplierRepository supplierRepository;
    
    @Autowired
    private VoucherRepository voucherRepository;
    
    @Autowired
    private VoucherEntryRepository voucherEntryRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private GRNRepository grnRepository;
    
    @Autowired
    private CustomerLedgerService customerLedgerService;
    
    @Autowired
    private SupplierLedgerService supplierLedgerService;
    
    @Autowired
    private com.brsons.repository.CustomerLedgerEntryRepository customerLedgerEntryRepository;
    
    @Autowired
    private com.brsons.repository.CustomerLedgerRepository customerLedgerRepository;
    
    // ==================== INITIALIZATION ====================
    
    @PostConstruct
    public void initialize() {
        try {
            System.out.println("Initializing OutstandingService...");
            createDefaultAccountsIfNeeded();
        } catch (Exception e) {
            System.err.println("Error during OutstandingService initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ==================== OUTSTANDING MANAGEMENT ====================
    
    /**
     * Check if a purchase order has approved GRNs
     */
    private boolean hasApprovedGRN(Long poId) {
        try {
            List<com.brsons.model.GoodsReceivedNote> approvedGRNs = grnRepository.findByPurchaseOrderIdAndStatus(
                poId, 
                com.brsons.model.GoodsReceivedNote.GRNStatus.APPROVED
            );
            return !approvedGRNs.isEmpty();
        } catch (Exception e) {
            System.err.println("Error checking for approved GRNs for PO #" + poId + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Create outstanding item for purchase order
     */
    @Transactional
    public Outstanding createPurchaseOrderOutstanding(PurchaseOrder po) {
        // Check if outstanding already exists
        List<Outstanding> existing = outstandingRepository.findByReferenceTypeAndReferenceId("PURCHASE_ORDER", po.getId());
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        
        Outstanding outstanding = new Outstanding(
            Outstanding.OutstandingType.INVOICE_PAYABLE,
            po.getId(),
            "PURCHASE_ORDER",
            po.getPoNumber() != null ? po.getPoNumber() : "PO-" + po.getId(),
            po.getTotalAmount(),
            po.getExpectedDeliveryDate() != null ? po.getExpectedDeliveryDate() : 
                po.getCreatedAt().plusDays(30),
            po.getSupplier().getCompanyName(),
            "Kaccha"
        );
        
        // Set contact info for supplier matching
        outstanding.setContactInfo(po.getSupplier().getPhone());
        
        outstanding = outstandingRepository.save(outstanding);
        
        // Note: Accounting entries (supplier ledger and outstanding payable) are now created 
        // when GRN is approved, not when PO is created. This follows proper accrual accounting principles.
        
        return outstanding;
    }
    
    /**
     * Create outstanding item for customer invoice
     */
    @Transactional
    public Outstanding createCustomerOutstanding(Order order, LocalDateTime dueDate) {
        // Check if outstanding already exists
        List<Outstanding> existing = outstandingRepository.findByReferenceTypeAndReferenceId("ORDER", order.getId());
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        
        Outstanding outstanding = new Outstanding(
            Outstanding.OutstandingType.INVOICE_RECEIVABLE,
            order.getId(),
            "ORDER",
            order.getInvoiceNumber() != null ? order.getInvoiceNumber() : "ORD-" + order.getId(),
            order.getTotal(),
            dueDate,
            order.getName(),
            order.getBillType() // Set the order type (Pakka/Kaccha)
        );
        
        outstanding.setDescription("Customer invoice for order #" + order.getId());
        outstanding.setContactInfo(order.getUserPhone());
        
        Outstanding savedOutstanding = outstandingRepository.save(outstanding);
        
        // If this is a B2B order (Kaccha), also create customer ledger entry
        if ("Kaccha".equals(order.getBillType()) && order.getTotal() != null && order.getTotal().compareTo(BigDecimal.ZERO) > 0) {
            try {
                // Check if customer ledger entry already exists for this order
                List<CustomerLedgerEntry> existingLedgerEntries = customerLedgerService.getCustomerLedgerEntriesByReference("ORDER", order.getId());
                
                if (existingLedgerEntries.isEmpty()) {
                    // Only create if no ledger entry exists
                    CustomerLedger customerLedger = customerLedgerService.findOrCreateCustomerLedger(
                        order.getName(), 
                        order.getUserPhone(), 
                        null
                    );
                    customerLedgerService.addInvoiceEntry(customerLedger, order, order.getTotal());
                    System.out.println("Created customer ledger entry for B2B order #" + order.getId());
                } else {
                    System.out.println("Customer ledger entry already exists for B2B order #" + order.getId() + ", skipping creation");
                }
                
                // Apply advance payments to this new invoice (FIFO)
                try {
                    customerLedgerService.applyAdvancePaymentsToNewInvoice(order.getUserPhone(), savedOutstanding.getId(), order.getTotal());
                    System.out.println("Applied advance payments to new invoice #" + savedOutstanding.getId());
                } catch (Exception e) {
                    System.err.println("Error applying advance payments to new invoice #" + savedOutstanding.getId() + ": " + e.getMessage());
                }
                
            } catch (Exception e) {
                System.err.println("Error creating customer ledger entry for order #" + order.getId() + ": " + e.getMessage());
            }
        }
        
        return savedOutstanding;
    }
    
    /**
     * Create outstanding item for supplier invoice
     */
    @Transactional
    public Outstanding createSupplierOutstanding(PurchaseOrder po, LocalDateTime dueDate) {
        // Check if outstanding already exists
        List<Outstanding> existing = outstandingRepository.findByReferenceTypeAndReferenceId("PURCHASE_ORDER", po.getId());
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        
        Supplier supplier = po.getSupplier();
        BigDecimal totalAmount = po.getTotalAmount();
        
        Outstanding outstanding = new Outstanding(
            Outstanding.OutstandingType.INVOICE_PAYABLE,
            po.getId(),
            "PURCHASE_ORDER",
            po.getPoNumber() != null ? po.getPoNumber() : "PO-" + po.getId(),
            totalAmount,
            dueDate,
            supplier != null ? supplier.getCompanyName() : "Unknown Supplier"
        );
        
        outstanding.setDescription("Supplier invoice for PO #" + po.getId());
        if (supplier != null) {
            outstanding.setContactInfo(supplier.getPhone());
        }
        
        return outstandingRepository.save(outstanding);
    }
    
    /**
     * Update outstanding item status
     */
    @Transactional
    public Outstanding updateOutstandingStatus(Long outstandingId, Outstanding.OutstandingStatus newStatus, String notes) {
        Outstanding outstanding = outstandingRepository.findById(outstandingId)
            .orElseThrow(() -> new RuntimeException("Outstanding item not found"));
        
        outstanding.setStatus(newStatus);
        if (notes != null) {
            outstanding.setNotes(notes);
        }
        outstanding.setUpdatedAt(LocalDateTime.now());
        
        return outstandingRepository.save(outstanding);
    }
    
    /**
     * Mark outstanding item as partially paid
     */
    @Transactional
    public Outstanding markPartiallyPaid(Long outstandingId, BigDecimal paidAmount, String notes, String paymentMethod, String paymentReference) {
        Outstanding outstanding = outstandingRepository.findById(outstandingId)
            .orElseThrow(() -> new RuntimeException("Outstanding item not found"));
        
        // Store original amount for voucher creation
        BigDecimal originalAmount = outstanding.getAmount();
        System.out.println("Partial payment - Outstanding ID: " + outstanding.getId() + 
                          ", Original Amount: " + originalAmount + 
                          ", Paid Amount: " + paidAmount + 
                          ", Payment Method: " + paymentMethod);
        
        // Set payment details
        outstanding.setPaymentMethod(paymentMethod);
        outstanding.setPaymentReference(paymentReference);
        outstanding.setPaymentDate(LocalDateTime.now());
        
        outstanding.setStatus(Outstanding.OutstandingStatus.PARTIALLY_PAID);
        outstanding.setAmount(outstanding.getAmount().subtract(paidAmount));
        System.out.println("After partial payment - Remaining Amount: " + outstanding.getAmount());
        
        if (notes != null) {
            outstanding.setNotes(notes);
        }
        outstanding.setUpdatedAt(LocalDateTime.now());
        
        // Automatically create voucher for partial payment
        createPartialPaymentVoucher(outstanding, paidAmount, notes);
        
        // If this is a B2B receivable, also update customer ledger
        if (outstanding.getType() == Outstanding.OutstandingType.INVOICE_RECEIVABLE && 
            "Kaccha".equals(outstanding.getOrderType())) {
            try {
                System.out.println("Syncing partial payment with customer ledger for B2B receivable...");
                Optional<CustomerLedger> customerLedger = customerLedgerService.getCustomerLedgerByPhone(outstanding.getContactInfo());
                if (customerLedger.isPresent()) {
                    // Create payment entry directly with proper reference information
                    CustomerLedgerEntry paymentEntry = new CustomerLedgerEntry(
                        customerLedger.get(),
                        "Partial payment for " + outstanding.getReferenceType() + " #" + outstanding.getReferenceNumber() + 
                        (notes != null ? " - " + notes : ""),
                        "PAYMENT", // Entry type
                        outstanding.getId(), // Reference the outstanding item
                        "PAY-" + System.currentTimeMillis()
                    );
                    
                    paymentEntry.setCreditAmount(paidAmount);
                    paymentEntry.setBalanceAfter(customerLedger.get().getCurrentBalance().subtract(paidAmount));
                    paymentEntry.setPaymentMethod(outstanding.getPaymentMethod());
                    paymentEntry.setPaymentReference(outstanding.getPaymentReference());
                    paymentEntry.setNotes(notes);
                    
                    // Update customer ledger balance
                    customerLedger.get().addCredit(paidAmount);
                    customerLedgerRepository.save(customerLedger.get());
                    
                    // Save the payment entry
                    customerLedgerEntryRepository.save(paymentEntry);
                    
                    System.out.println("Successfully updated customer ledger for partial payment of " + paidAmount + 
                                     " for customer: " + customerLedger.get().getCustomerName());
                } else {
                    System.err.println("Customer ledger not found for phone: " + outstanding.getContactInfo());
                }
            } catch (Exception e) {
                System.err.println("Error updating customer ledger for partial payment: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Not a B2B receivable - skipping customer ledger sync. Type: " + outstanding.getType() + 
                             ", Order Type: " + outstanding.getOrderType());
        }
        
        // If this is a payable (Purchase Order), also update supplier ledger
        if (outstanding.getType() == Outstanding.OutstandingType.INVOICE_PAYABLE || 
            outstanding.getType() == Outstanding.OutstandingType.PURCHASE_ORDER) {
            try {
                System.out.println("=== SYNC DEBUG: Partial Payment ===");
                System.out.println("Outstanding Type: " + outstanding.getType());
                System.out.println("Contact Info: " + outstanding.getContactInfo());
                System.out.println("Paid Amount: " + paidAmount);
                
                Optional<SupplierLedger> supplierLedger = supplierLedgerService.getSupplierLedgerByPhone(outstanding.getContactInfo());
                if (supplierLedger.isPresent()) {
                    System.out.println("Found supplier ledger: " + supplierLedger.get().getSupplierName());
                    // Add payment entry to supplier ledger (disable sync to avoid double sync)
                    supplierLedgerService.addPaymentEntry(
                        supplierLedger.get(),
                        paidAmount,
                        paymentMethod,
                        paymentReference,
                        notes,
                        false // Disable sync since this is coming from outstanding payables
                    );
                    System.out.println("Successfully updated supplier ledger for partial payment of " + paidAmount + 
                                     " for supplier: " + supplierLedger.get().getSupplierName());
                } else {
                    System.err.println("Supplier ledger not found for phone: " + outstanding.getContactInfo());
                    System.err.println("Creating supplier ledger for: " + outstanding.getContactInfo());
                    
                    // Try to create supplier ledger if it doesn't exist
                    try {
                        // Ensure we have valid contact info for supplier name and phone
                        String contactInfo = outstanding.getContactInfo();
                        if (contactInfo == null || contactInfo.trim().isEmpty()) {
                            contactInfo = "Unknown Supplier";
                        }
                        
                        SupplierLedger newSupplierLedger = supplierLedgerService.findOrCreateSupplierLedger(
                            contactInfo, // Use contact info as name
                            contactInfo, // Use contact info as phone
                            null, // No email
                            null  // No code
                        );
                        
                        // Add payment entry to the new supplier ledger
                        supplierLedgerService.addPaymentEntry(
                            newSupplierLedger,
                            paidAmount,
                            paymentMethod,
                            paymentReference,
                            notes,
                            false // Disable sync since this is coming from outstanding payables
                        );
                        
                        System.out.println("Created new supplier ledger and added payment entry");
                    } catch (Exception createException) {
                        System.err.println("Error creating supplier ledger: " + createException.getMessage());
                        createException.printStackTrace();
                    }
                }
            } catch (Exception e) {
                System.err.println("Error updating supplier ledger for partial payment: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Not a payable - skipping supplier ledger sync. Type: " + outstanding.getType());
        }
        
        // If fully paid, mark as settled
        if (outstanding.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Item fully paid through partial payment - marking as settled");
            outstanding.setStatus(Outstanding.OutstandingStatus.SETTLED);
            // No need to create another settlement voucher since we already created one for the partial payment
            // The item is now fully settled
        }
        
        return outstandingRepository.save(outstanding);
    }
    
    /**
     * Mark outstanding item as settled
     */
    @Transactional
    public Outstanding markAsSettled(Long outstandingId, String notes, String paymentMethod, String paymentReference) {
        Outstanding outstanding = outstandingRepository.findById(outstandingId)
            .orElseThrow(() -> new RuntimeException("Outstanding item not found"));
        
        // Set payment details
        outstanding.setPaymentMethod(paymentMethod);
        outstanding.setPaymentReference(paymentReference);
        outstanding.setPaymentDate(LocalDateTime.now());
        
        // Get the current outstanding amount (remaining amount to be settled)
        BigDecimal remainingAmount = outstanding.getAmount();
        System.out.println("Marking as settled - Outstanding ID: " + outstanding.getId() + 
                          ", Remaining Amount: " + remainingAmount + 
                          ", Payment Method: " + paymentMethod);
        
        outstanding.setStatus(Outstanding.OutstandingStatus.SETTLED);
        outstanding.setAmount(BigDecimal.ZERO);
        if (notes != null) {
            outstanding.setNotes(notes);
        }
        outstanding.setUpdatedAt(LocalDateTime.now());
        
        // Automatically create voucher for settlement
        // Use the remaining amount, not the original amount
        System.out.println("Creating settlement voucher for remaining amount: " + remainingAmount);
        createSettlementVoucher(outstanding, notes, remainingAmount);
        
        // If this is a B2B receivable, also update customer ledger
        if (outstanding.getType() == Outstanding.OutstandingType.INVOICE_RECEIVABLE && 
            "Kaccha".equals(outstanding.getOrderType())) {
            try {
                System.out.println("Syncing full settlement with customer ledger for B2B receivable...");
                Optional<CustomerLedger> customerLedger = customerLedgerService.getCustomerLedgerByPhone(outstanding.getContactInfo());
                if (customerLedger.isPresent()) {
                    // Create payment entry directly with proper reference information
                    CustomerLedgerEntry paymentEntry = new CustomerLedgerEntry(
                        customerLedger.get(),
                        "Full settlement for " + outstanding.getReferenceType() + " #" + outstanding.getReferenceNumber() + 
                        (notes != null ? " - " + notes : ""),
                        "PAYMENT", // Entry type
                        outstanding.getId(), // Reference the outstanding item
                        "PAY-" + System.currentTimeMillis()
                    );
                    
                    paymentEntry.setCreditAmount(remainingAmount);
                    paymentEntry.setBalanceAfter(customerLedger.get().getCurrentBalance().subtract(remainingAmount));
                    paymentEntry.setPaymentMethod(outstanding.getPaymentMethod());
                    paymentEntry.setPaymentReference(outstanding.getPaymentReference());
                    paymentEntry.setNotes(notes);
                    
                    // Update customer ledger balance
                    customerLedger.get().addCredit(remainingAmount);
                    customerLedgerRepository.save(customerLedger.get());
                    
                    // Save the payment entry
                    customerLedgerEntryRepository.save(paymentEntry);
                    System.out.println("Successfully updated customer ledger for full settlement of " + remainingAmount + 
                                     " for customer: " + customerLedger.get().getCustomerName());
                } else {
                    System.err.println("Customer ledger not found for phone: " + outstanding.getContactInfo());
                }
            } catch (Exception e) {
                System.err.println("Error updating customer ledger for settlement: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Not a B2B receivable - skipping customer ledger sync. Type: " + outstanding.getType() + 
                             ", Order Type: " + outstanding.getOrderType());
        }
        
        // If this is a payable (Purchase Order), also update supplier ledger
        if (outstanding.getType() == Outstanding.OutstandingType.INVOICE_PAYABLE || 
            outstanding.getType() == Outstanding.OutstandingType.PURCHASE_ORDER) {
            try {
                System.out.println("=== SYNC DEBUG: Full Settlement ===");
                System.out.println("Outstanding Type: " + outstanding.getType());
                System.out.println("Contact Info: " + outstanding.getContactInfo());
                System.out.println("Remaining Amount: " + remainingAmount);
                
                Optional<SupplierLedger> supplierLedger = supplierLedgerService.getSupplierLedgerByPhone(outstanding.getContactInfo());
                if (supplierLedger.isPresent()) {
                    System.out.println("Found supplier ledger: " + supplierLedger.get().getSupplierName());
                    // Add payment entry to supplier ledger (disable sync to avoid double sync)
                    supplierLedgerService.addPaymentEntry(
                        supplierLedger.get(),
                        remainingAmount,
                        paymentMethod,
                        paymentReference,
                        notes,
                        false // Disable sync since this is coming from outstanding payables
                    );
                    System.out.println("Successfully updated supplier ledger for full settlement of " + remainingAmount + 
                                     " for supplier: " + supplierLedger.get().getSupplierName());
                } else {
                    System.err.println("Supplier ledger not found for phone: " + outstanding.getContactInfo());
                    System.err.println("Creating supplier ledger for: " + outstanding.getContactInfo());
                    
                    // Try to create supplier ledger if it doesn't exist
                    try {
                        // Ensure we have valid contact info for supplier name and phone
                        String contactInfo = outstanding.getContactInfo();
                        if (contactInfo == null || contactInfo.trim().isEmpty()) {
                            contactInfo = "Unknown Supplier";
                        }
                        
                        SupplierLedger newSupplierLedger = supplierLedgerService.findOrCreateSupplierLedger(
                            contactInfo, // Use contact info as name
                            contactInfo, // Use contact info as phone
                            null, // No email
                            null  // No code
                        );
                        
                        // Add payment entry to the new supplier ledger
                        supplierLedgerService.addPaymentEntry(
                            newSupplierLedger,
                            remainingAmount,
                            paymentMethod,
                            paymentReference,
                            notes,
                            false // Disable sync since this is coming from outstanding payables
                        );
                        
                        System.out.println("Created new supplier ledger and added payment entry");
                    } catch (Exception createException) {
                        System.err.println("Error creating supplier ledger: " + createException.getMessage());
                        createException.printStackTrace();
                    }
                }
            } catch (Exception e) {
                System.err.println("Error updating supplier ledger for settlement: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Not a payable - skipping supplier ledger sync. Type: " + outstanding.getType());
        }
        
        return outstandingRepository.save(outstanding);
    }
    
    /**
     * Calculate the original amount for an outstanding item
     */
    private BigDecimal calculateOriginalAmount(Outstanding outstanding) {
        try {
            if (outstanding.getReferenceType().equals("ORDER")) {
                // For orders, get the total from the order
                Optional<Order> order = orderRepository.findById(outstanding.getReferenceId());
                if (order.isPresent()) {
                    return order.get().getTotal() != null ? order.get().getTotal() : BigDecimal.ZERO;
                }
            } else if (outstanding.getReferenceType().equals("PURCHASE_ORDER")) {
                // For purchase orders, get the total from the PO
                Optional<PurchaseOrder> po = purchaseOrderRepository.findById(outstanding.getReferenceId());
                if (po.isPresent()) {
                    return po.get().getTotalAmount() != null ? po.get().getTotalAmount() : BigDecimal.ZERO;
                }
            }
            
            // Fallback to current amount if reference not found
            return outstanding.getAmount();
            
        } catch (Exception e) {
            System.err.println("Error calculating original amount: " + e.getMessage());
            return outstanding.getAmount();
        }
    }
    
    // ==================== QUERIES AND REPORTS ====================
    
    /**
     * Get outstanding dashboard summary
     */
    public Map<String, Object> getOutstandingDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // Total amounts
        BigDecimal totalReceivable = outstandingRepository.calculateTotalReceivable();
        BigDecimal totalPayable = outstandingRepository.calculateTotalPayable();
        BigDecimal totalOverdue = outstandingRepository.calculateTotalOverdueAmount();
        
        // Counts
        Long totalOutstanding = outstandingRepository.countByStatus(Outstanding.OutstandingStatus.PENDING) +
                               outstandingRepository.countByStatus(Outstanding.OutstandingStatus.OVERDUE) +
                               outstandingRepository.countByStatus(Outstanding.OutstandingStatus.PARTIALLY_PAID);
        Long overdueCount = outstandingRepository.countOverdueItems();
        Long criticalCount = outstandingRepository.countCriticalOverdueItems();
        
        // Items due today
        List<Outstanding> dueToday = outstandingRepository.findItemsDueToday(LocalDate.now());
        
        // Critical overdue items
        List<Outstanding> criticalItems = outstandingRepository.findCriticalOverdueItems();
        
        dashboard.put("totalReceivable", totalReceivable != null ? totalReceivable : BigDecimal.ZERO);
        dashboard.put("totalPayable", totalPayable != null ? totalPayable : BigDecimal.ZERO);
        dashboard.put("totalOverdue", totalOverdue != null ? totalOverdue : BigDecimal.ZERO);
        dashboard.put("totalOutstanding", totalOutstanding);
        dashboard.put("overdueCount", overdueCount);
        dashboard.put("criticalCount", criticalCount);
        dashboard.put("dueToday", dueToday);
        dashboard.put("criticalItems", criticalItems);
        
        return dashboard;
    }
    
    /**
     * Get B2B outstanding dashboard data (Kaccha orders + Purchase Orders)
     * Calculates totals from Outstanding table to account for settlements and partial payments
     */
    public Map<String, Object> getB2BOutstandingDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // Get B2B receivables from Outstanding table (Kaccha orders only)
        List<Outstanding> b2bReceivables = outstandingRepository.findByTypeAndOrderType(
            Outstanding.OutstandingType.INVOICE_RECEIVABLE, 
            "Kaccha"
        );
        
        // Get B2B payables from Outstanding table (Purchase Orders only)
        List<Outstanding> b2bPayables = outstandingRepository.findByType(Outstanding.OutstandingType.PURCHASE_ORDER);
        
        BigDecimal totalB2BReceivable = BigDecimal.ZERO;
        BigDecimal totalPayable = BigDecimal.ZERO;
        BigDecimal totalOverdue = BigDecimal.ZERO;
        int criticalCount = 0;
        int totalB2BItems = 0;
        
        // Calculate B2B receivables from Outstanding table
        for (Outstanding receivable : b2bReceivables) {
            // Only include items that are not fully settled
            if (receivable.getStatus() != Outstanding.OutstandingStatus.SETTLED) {
                BigDecimal outstandingAmount = receivable.getAmount();
                
                // For partially paid items, calculate remaining amount
                if (receivable.getStatus() == Outstanding.OutstandingStatus.PARTIALLY_PAID) {
                    // You might need to add a field to track paid amount, or calculate it differently
                    // For now, we'll use the current amount which should be the remaining amount
                    outstandingAmount = receivable.getAmount();
                }
                
                totalB2BReceivable = totalB2BReceivable.add(outstandingAmount);
                totalB2BItems++;
                
                // Check if overdue
                if (receivable.getStatus() == Outstanding.OutstandingStatus.OVERDUE) {
                    totalOverdue = totalOverdue.add(outstandingAmount);
                    if (receivable.getDaysOverdue() > 30) {
                        criticalCount++;
                    }
                }
            }
        }
        
        // Calculate purchase order payables from Outstanding table
        for (Outstanding payable : b2bPayables) {
            // Only include items that are not fully settled
            if (payable.getStatus() != Outstanding.OutstandingStatus.SETTLED) {
                BigDecimal outstandingAmount = payable.getAmount();
                
                // For partially paid items, calculate remaining amount
                if (payable.getStatus() == Outstanding.OutstandingStatus.PARTIALLY_PAID) {
                    // You might need to add a field to track paid amount, or calculate it differently
                    // For now, we'll use the current amount which should be the remaining amount
                    outstandingAmount = payable.getAmount();
                }
                
                totalPayable = totalPayable.add(outstandingAmount);
                totalB2BItems++;
                
                // Check if overdue
                if (payable.getStatus() == Outstanding.OutstandingStatus.OVERDUE) {
                    totalOverdue = totalOverdue.add(outstandingAmount);
                    if (payable.getDaysOverdue() > 30) {
                        criticalCount++;
                    }
                }
            }
        }
        
        dashboard.put("totalB2BReceivable", totalB2BReceivable);
        dashboard.put("totalPayable", totalPayable);
        dashboard.put("totalOverdue", totalOverdue);
        dashboard.put("criticalCount", criticalCount);
        dashboard.put("totalB2BItems", totalB2BItems);
        dashboard.put("b2bOrdersCount", b2bReceivables.size());
        dashboard.put("purchaseOrdersCount", b2bPayables.size());
        
        return dashboard;
    }
    
    /**
     * Get outstanding items by type
     */
    public List<Outstanding> getOutstandingByType(Outstanding.OutstandingType type) {
        return outstandingRepository.findByType(type);
    }
    
    /**
     * Get B2B receivables (Kaccha orders only)
     */
    public List<Outstanding> getB2BReceivables() {
        // Get outstanding receivables with orderType "Kaccha"
        return outstandingRepository.findByTypeAndOrderType(
            Outstanding.OutstandingType.INVOICE_RECEIVABLE, 
            "Kaccha"
        );
    }
    
    /**
     * Get retail receivables (Pakka orders only)
     */
    public List<Outstanding> getRetailReceivables() {
        // Get outstanding receivables with orderType "Pakka"
        return outstandingRepository.findByTypeAndOrderType(
            Outstanding.OutstandingType.INVOICE_RECEIVABLE, 
            "Pakka"
        );
    }
    
    /**
     * Get B2B payables (Purchase Orders only)
     */
    public List<Outstanding> getB2BPayables() {
        // Get all outstanding purchase orders
        return outstandingRepository.findByType(Outstanding.OutstandingType.PURCHASE_ORDER);
    }
    
    /**
     * Get overdue items
     */
    public List<Outstanding> getOverdueItems() {
        return outstandingRepository.findByStatus(Outstanding.OutstandingStatus.OVERDUE);
    }
    
    /**
     * Get items due within next X days
     */
    public List<Outstanding> getItemsDueWithinDays(int days) {
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(days);
        return outstandingRepository.findItemsDueBetween(startDate, endDate);
    }
    
    /**
     * Get items due this week
     */
    public List<Outstanding> getItemsDueThisWeek() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStart = now.with(java.time.DayOfWeek.MONDAY).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime weekEnd = weekStart.plusDays(7).minusSeconds(1);
        return outstandingRepository.findItemsDueThisWeek(weekStart, weekEnd);
    }
    
    /**
     * Get items due this month
     */
    public List<Outstanding> getItemsDueThisMonth() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);
        return outstandingRepository.findItemsDueThisMonth(monthStart, monthEnd);
    }
    
    /**
     * Search outstanding items
     */
    public List<Outstanding> searchOutstandingItems(String searchTerm, Outstanding.OutstandingType type, 
                                                   Outstanding.OutstandingStatus status, BigDecimal minAmount, 
                                                   BigDecimal maxAmount) {
        // This is a simplified search - you can enhance it based on your needs
        if (type != null && status != null) {
            return outstandingRepository.findByTypeAndStatus(type, status);
        } else if (type != null) {
            return outstandingRepository.findByType(type);
        } else if (status != null) {
            return outstandingRepository.findByStatus(status);
        } else if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            return outstandingRepository.findByCustomerSupplierNameContainingIgnoreCase(searchTerm.trim());
        } else if (minAmount != null && maxAmount != null) {
            return outstandingRepository.findByAmountRange(minAmount, maxAmount);
        }
        
        return outstandingRepository.findAll();
    }
    
    // ==================== AUTOMATIC UPDATES ====================
    
    /**
     * Scheduled task to update overdue status daily
     */
    @Scheduled(cron = "0 0 1 * * ?") // Run at 1 AM daily
    @Transactional
    public void updateOverdueStatus() {
        List<Outstanding> pendingItems = outstandingRepository.findByStatus(Outstanding.OutstandingStatus.PENDING);
        
        for (Outstanding item : pendingItems) {
            item.updateDaysOverdue();
            outstandingRepository.save(item);
        }
        
        System.out.println("Updated overdue status for " + pendingItems.size() + " outstanding items");
    }
    
    /**
     * Handle order updates - adjust outstanding items and customer ledger
     */
    @Transactional
    public void handleOrderUpdate(Order oldOrder, Order newOrder) {
        try {
            // Find existing outstanding item for this order
            List<Outstanding> existingOutstanding = outstandingRepository.findByReferenceTypeAndReferenceId("ORDER", oldOrder.getId());
            
            if (!existingOutstanding.isEmpty()) {
                Outstanding outstanding = existingOutstanding.get(0);
                
                // Check if order can be modified (not fully settled)
                if (outstanding.getStatus() == Outstanding.OutstandingStatus.SETTLED) {
                    throw new RuntimeException("Cannot modify order that is fully settled");
                }
                
                // Calculate amount difference
                BigDecimal oldAmount = oldOrder.getTotal() != null ? oldOrder.getTotal() : BigDecimal.ZERO;
                BigDecimal newAmount = newOrder.getTotal() != null ? newOrder.getTotal() : BigDecimal.ZERO;
                BigDecimal amountDifference = newAmount.subtract(oldAmount);
                
                // Update outstanding amount
                BigDecimal currentAmount = outstanding.getAmount();
                BigDecimal newOutstandingAmount = currentAmount.add(amountDifference);
                
                // Ensure outstanding amount doesn't go negative
                if (newOutstandingAmount.compareTo(BigDecimal.ZERO) < 0) {
                    newOutstandingAmount = BigDecimal.ZERO;
                }
                
                outstanding.setAmount(newOutstandingAmount);
                outstanding.setUpdatedAt(LocalDateTime.now());
                outstanding.setDescription("Customer invoice for order #" + newOrder.getId() + " (Updated)");
                outstanding.setCustomerSupplierName(newOrder.getName());
                outstanding.setContactInfo(newOrder.getUserPhone());
                outstanding.setOrderType(newOrder.getBillType());
                
                outstandingRepository.save(outstanding);
                
                // Update customer ledger if this is a B2B order
                if ("Kaccha".equals(newOrder.getBillType()) && amountDifference.compareTo(BigDecimal.ZERO) != 0) {
                    try {
                        CustomerLedger customerLedger = customerLedgerService.findOrCreateCustomerLedger(
                            newOrder.getName(), 
                            newOrder.getUserPhone(), 
                            null
                        );
                        
                        // Create adjustment entry for the amount difference
                        String adjustmentReason = amountDifference.compareTo(BigDecimal.ZERO) > 0 ? 
                            "Order amount increased" : "Order amount decreased";
                        
                        customerLedgerService.addAdjustmentEntry(
                            customerLedger, 
                            amountDifference.abs(), 
                            amountDifference.compareTo(BigDecimal.ZERO) > 0, 
                            adjustmentReason, 
                            "Order #" + newOrder.getId() + " modification"
                        );
                        
                        System.out.println("Updated customer ledger for order #" + newOrder.getId() + " with adjustment: " + amountDifference);
                    } catch (Exception e) {
                        System.err.println("Error updating customer ledger for order #" + newOrder.getId() + ": " + e.getMessage());
                    }
                }
                
                System.out.println("Updated outstanding item for order #" + newOrder.getId() + " from " + oldAmount + " to " + newAmount);
            }
        } catch (Exception e) {
            System.err.println("Error handling order update for order #" + oldOrder.getId() + ": " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Handle order cancellation - reverse outstanding items and customer ledger
     */
    @Transactional
    public void handleOrderCancellation(Order order) {
        try {
            // Find existing outstanding item for this order
            List<Outstanding> existingOutstanding = outstandingRepository.findByReferenceTypeAndReferenceId("ORDER", order.getId());
            
            if (!existingOutstanding.isEmpty()) {
                Outstanding outstanding = existingOutstanding.get(0);
                
                // Check if order can be cancelled based on order status, not outstanding status
                Order orderForStatus = orderRepository.findById(outstanding.getReferenceId()).orElse(null);
                if (orderForStatus != null && "Delivered".equals(orderForStatus.getOrderStatus())) {
                    throw new RuntimeException("Cannot cancel order that has been delivered");
                }

                // Allow cancellation even if outstanding is settled, but log a warning
                if (outstanding.getStatus() == Outstanding.OutstandingStatus.SETTLED) {
                    System.out.println("Warning: Cancelling order with settled outstanding item - this will reverse the settlement");
                }
                
                // Check if already cancelled to prevent duplicate processing
                if (outstanding.getStatus() == Outstanding.OutstandingStatus.CANCELLED) {
                    System.out.println("Outstanding item for order #" + order.getId() + " is already cancelled, skipping duplicate processing");
                    return;
                }
                
                // Mark outstanding as cancelled
                outstanding.setStatus(Outstanding.OutstandingStatus.CANCELLED);
                outstanding.setUpdatedAt(LocalDateTime.now());
                outstanding.setNotes("Order cancelled - " + (outstanding.getNotes() != null ? outstanding.getNotes() : ""));
                outstanding.setAmount(BigDecimal.ZERO); // Set amount to zero for cancelled orders
                
                outstandingRepository.save(outstanding);
                
                // Reverse customer ledger entry if this is a B2B order
                if ("Kaccha".equals(order.getBillType()) && order.getTotal() != null && order.getTotal().compareTo(BigDecimal.ZERO) > 0) {
                    try {
                        CustomerLedger customerLedger = customerLedgerService.findOrCreateCustomerLedger(
                            order.getName(), 
                            order.getUserPhone(), 
                            null
                        );
                        
                        // Check if cancellation credit entry already exists to prevent duplicates
                        List<CustomerLedgerEntry> existingCancellationEntries = customerLedgerEntryRepository
                            .findByReferenceTypeAndReferenceId("ORDER", order.getId());
                        
                        boolean hasCancellationEntry = false;
                        for (CustomerLedgerEntry entry : existingCancellationEntries) {
                            if ("ADJUSTMENT".equals(entry.getReferenceType()) && 
                                entry.getParticulars() != null && 
                                entry.getParticulars().contains("Order cancellation")) {
                                hasCancellationEntry = true;
                                System.out.println("Found existing cancellation entry: " + entry.getParticulars());
                                break;
                            }
                        }
                        
                        System.out.println("Checking for cancellation entries for order #" + order.getId() + 
                                         " - Found " + existingCancellationEntries.size() + " entries, hasCancellation: " + hasCancellationEntry);
                        
                        if (!hasCancellationEntry) {
                            // Create reversal entry (credit to reverse the original debit)
                            customerLedgerService.addAdjustmentEntry(
                                customerLedger, 
                                order.getTotal(), 
                                false, // Credit entry to reverse the original debit
                                "Order cancellation", 
                                "Reversal for cancelled order #" + order.getId()
                            );
                            
                            System.out.println("Reversed customer ledger entry for cancelled order #" + order.getId());
                        } else {
                            System.out.println("Cancellation credit entry already exists for order #" + order.getId() + ", skipping duplicate creation");
                        }
                    } catch (Exception e) {
                        System.err.println("Error reversing customer ledger for cancelled order #" + order.getId() + ": " + e.getMessage());
                    }
                }
                
                System.out.println("Cancelled outstanding item for order #" + order.getId());
            }
            
            // Create reversal voucher for both B2B and Retail orders
            // Only create reversal if order was previously confirmed (had a voucher created)
            if (order.getOrderStatus() != null && 
                ("Confirmed".equals(order.getOrderStatus()) || 
                 "Shipped".equals(order.getOrderStatus()))) {
                try {
                    createReversalVoucherForOrderCancellation(order);
                    System.out.println("Created reversal voucher for cancelled order #" + order.getId());
                } catch (Exception e) {
                    System.err.println("Error creating reversal voucher for cancelled order #" + order.getId() + ": " + e.getMessage());
                    // Don't fail the cancellation if voucher creation fails
                }
            } else {
                System.out.println("Order #" + order.getId() + " was not confirmed, skipping voucher reversal");
            }
            
        } catch (Exception e) {
            System.err.println("Error handling order cancellation for order #" + order.getId() + ": " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Check if an order can be modified or cancelled
     * For both B2B and Retail orders: Allow cancellation until order reaches "Delivered" status
     * Status flow: Pending → Confirmed → Shipped → Delivered
     */
    public boolean canModifyOrder(Long orderId) {
        // First check if the order exists and get its status
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return false; // Order doesn't exist
        }
        
        Order order = orderOpt.get();
        String status = order.getOrderStatus();
        
        // Allow cancellation for: Pending, Confirmed, Shipped
        // Disallow cancellation for: Delivered, Cancelled, and any other status
        return "Pending".equals(status) || 
               "Confirmed".equals(status) || 
               "Shipped".equals(status);
    }
    
    /**
     * Get outstanding amount for an order
     */
    public BigDecimal getOutstandingAmountForOrder(Long orderId) {
        List<Outstanding> existingOutstanding = outstandingRepository.findByReferenceTypeAndReferenceId("ORDER", orderId);
        
        if (existingOutstanding.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return existingOutstanding.get(0).getAmount();
    }
    
    
    /**
     * Force sync customer ledgers with outstanding items for B2B receivables
     */
    @Transactional
    public String forceSyncCustomerLedgers() {
        try {
            System.out.println("=== Starting forced customer ledger sync ===");
            
            // Get all B2B receivables (INVOICE_RECEIVABLE with Kaccha order type)
            List<Outstanding> b2bReceivables = outstandingRepository.findByTypeAndOrderType(
                Outstanding.OutstandingType.INVOICE_RECEIVABLE, "Kaccha");
            System.out.println("Found " + b2bReceivables.size() + " B2B receivables to sync");
            
            int syncedCount = 0;
            int errorCount = 0;
            
            for (Outstanding outstanding : b2bReceivables) {
                try {
                    // Skip cancelled outstanding items - they should not be synced
                    if (outstanding.getStatus() == Outstanding.OutstandingStatus.CANCELLED) {
                        System.out.println("Skipping cancelled outstanding item #" + outstanding.getId());
                        continue;
                    }
                    
                    // Find or create customer ledger
                    CustomerLedger customerLedger = customerLedgerService.findOrCreateCustomerLedger(
                        outstanding.getCustomerSupplierName(),
                        outstanding.getContactInfo(),
                        null
                    );
                    
                    // Find the original invoice entry for this order
                    List<CustomerLedgerEntry> invoiceEntries = customerLedgerEntryRepository
                        .findByReferenceTypeAndReferenceId("ORDER", outstanding.getReferenceId());
                    
                    if (!invoiceEntries.isEmpty()) {
                        CustomerLedgerEntry invoiceEntry = invoiceEntries.get(0);
                        BigDecimal originalAmount = invoiceEntry.getDebitAmount();
                        BigDecimal currentOutstandingAmount = outstanding.getAmount();
                        BigDecimal paidAmount = originalAmount.subtract(currentOutstandingAmount);
                        
                        // Check if we need to create payment entries
                        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
                            // Check if payment entries already exist for this outstanding item
                            // Look for payment entries that reference this outstanding item
                            List<CustomerLedgerEntry> paymentEntries = customerLedgerEntryRepository
                                .findByReferenceTypeAndReferenceId("PAYMENT", outstanding.getId());
                            
                            // Also check for payment entries that might reference the order (legacy entries)
                            List<CustomerLedgerEntry> orderPaymentEntries = customerLedgerEntryRepository
                                .findByReferenceTypeAndReferenceId("ORDER", outstanding.getReferenceId());
                            
                            // Filter to only payment type entries from order references
                            List<CustomerLedgerEntry> allPaymentEntries = new ArrayList<>();
                            
                            // Add payment entries that directly reference the outstanding item
                            allPaymentEntries.addAll(paymentEntries);
                            
                            // Add payment entries that reference the order (legacy)
                            for (CustomerLedgerEntry entry : orderPaymentEntries) {
                                if ("PAYMENT".equals(entry.getReferenceType())) {
                                    allPaymentEntries.add(entry);
                                }
                            }
                            
                            // Check if we already have payment entries that match the paid amount
                            boolean hasMatchingPayment = false;
                            BigDecimal totalExistingPayments = BigDecimal.ZERO;
                            
                            System.out.println("=== Checking existing payment entries for outstanding item #" + outstanding.getId() + " ===");
                            System.out.println("Found " + allPaymentEntries.size() + " existing payment entries");
                            
                            for (CustomerLedgerEntry paymentEntry : allPaymentEntries) {
                                System.out.println("Payment entry: " + paymentEntry.getParticulars() + 
                                                 ", Amount: ₹" + paymentEntry.getCreditAmount() + 
                                                 ", Reference: " + paymentEntry.getReferenceType() + "/" + paymentEntry.getReferenceId());
                                if (paymentEntry.getCreditAmount() != null) {
                                    totalExistingPayments = totalExistingPayments.add(paymentEntry.getCreditAmount());
                                }
                            }
                            
                            // Also check for advance payments that might have been applied to this invoice
                            // Look for "Payment Received" entries that could be advance payments
                            List<CustomerLedgerEntry> allCustomerEntries = customerLedgerEntryRepository
                                .findByCustomerLedgerIdOrderByEntryDateDesc(customerLedger.getId());
                            
                            BigDecimal advancePaymentsApplied = BigDecimal.ZERO;
                            for (CustomerLedgerEntry entry : allCustomerEntries) {
                                if ("PAYMENT".equals(entry.getReferenceType()) && 
                                    entry.getParticulars() != null && 
                                    entry.getParticulars().contains("Payment Received") &&
                                    entry.getCreditAmount() != null) {
                                    advancePaymentsApplied = advancePaymentsApplied.add(entry.getCreditAmount());
                                }
                            }
                            
                            System.out.println("Total existing payments: ₹" + totalExistingPayments + 
                                             ", Advance payments: ₹" + advancePaymentsApplied + 
                                             ", Required: ₹" + paidAmount);
                            
                            // If the total existing payments + advance payments match the paid amount, don't create new entries
                            BigDecimal totalAvailablePayments = totalExistingPayments.add(advancePaymentsApplied);
                            if (totalAvailablePayments.compareTo(paidAmount) >= 0) {
                                hasMatchingPayment = true;
                                System.out.println("✓ Payment entries already exist (including advance payments) - skipping creation");
                            } else {
                                System.out.println("✗ Need to create payment entry - existing: ₹" + totalAvailablePayments + ", required: ₹" + paidAmount);
                            }
                            
                            if (!hasMatchingPayment) {
                                // Create payment entry for the paid amount
                                CustomerLedgerEntry paymentEntry = new CustomerLedgerEntry(
                                    customerLedger,
                                    "Payment for " + invoiceEntry.getParticulars() + " (Paid: ₹" + paidAmount + ", Remaining: ₹" + currentOutstandingAmount + ")",
                                    "PAYMENT",
                                    outstanding.getId(),
                                    "PAY-" + System.currentTimeMillis()
                                );
                                
                                paymentEntry.setCreditAmount(paidAmount);
                                paymentEntry.setBalanceAfter(customerLedger.getCurrentBalance().subtract(paidAmount));
                                paymentEntry.setPaymentMethod(outstanding.getPaymentMethod() != null ? outstanding.getPaymentMethod() : "Cash");
                                paymentEntry.setPaymentReference(outstanding.getPaymentReference());
                                
                                // Update customer ledger balance
                                customerLedger.addCredit(paidAmount);
                                customerLedgerRepository.save(customerLedger);
                                
                                // Save the payment entry
                                customerLedgerEntryRepository.save(paymentEntry);
                                
                                syncedCount++;
                                System.out.println("Created payment entry for outstanding item #" + outstanding.getId() + 
                                                 " - Paid: ₹" + paidAmount + ", Remaining: ₹" + currentOutstandingAmount);
                            } else {
                                System.out.println("Payment entries already exist for outstanding item #" + outstanding.getId() + 
                                                 " - Total existing payments: ₹" + totalExistingPayments + ", Required: ₹" + paidAmount);
                            }
                        } else {
                            System.out.println("No payment needed for outstanding item #" + outstanding.getId() + " (Amount: ₹" + currentOutstandingAmount + ")");
                        }
                    } else {
                        System.out.println("No invoice entry found for outstanding item #" + outstanding.getId());
                    }
                    
                } catch (Exception e) {
                    errorCount++;
                    System.err.println("Error syncing outstanding item #" + outstanding.getId() + ": " + e.getMessage());
                }
            }
            
            String result = "Sync completed. Synced: " + syncedCount + ", Errors: " + errorCount;
            System.out.println("=== " + result + " ===");
            return result;
            
        } catch (Exception e) {
            String error = "Error during forced sync: " + e.getMessage();
            System.err.println(error);
            e.printStackTrace();
            return error;
        }
    }

    /**
     * Create outstanding items for existing orders and POs
     */
    @Transactional
    public void createOutstandingForExistingItems() {
        // Create outstanding for retail orders (Pakka bill type) without outstanding items
        // Only create outstanding for orders that are not Pending or Cancelled
        List<Order> retailOrders = orderRepository.findByBillTypeOrderByCreatedAtDesc("Pakka");
        for (Order order : retailOrders) {
            if (order.getTotal() != null && order.getTotal().compareTo(BigDecimal.ZERO) > 0) {
                // Only create outstanding for confirmed orders (not Pending or Cancelled)
                if (!"Pending".equals(order.getOrderStatus()) && !"Cancelled".equals(order.getOrderStatus())) {
                    // Check if outstanding already exists
                    List<Outstanding> existing = outstandingRepository.findByReferenceTypeAndReferenceId("ORDER", order.getId());
                    if (existing.isEmpty()) {
                        // Set due date as 30 days from order creation
                        LocalDateTime dueDate = order.getCreatedAt().plusDays(30);
                        createCustomerOutstanding(order, dueDate);
                    }
                }
            }
        }
        
        // Create outstanding for POs that have approved GRNs (following new accounting workflow)
        List<PurchaseOrder> pos = purchaseOrderRepository.findAll();
        for (PurchaseOrder po : pos) {
            if (po.getTotalAmount() != null && po.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
                // Check if outstanding already exists
                List<Outstanding> existing = outstandingRepository.findByReferenceTypeAndReferenceId("PURCHASE_ORDER", po.getId());
                if (existing.isEmpty()) {
                    // Only create outstanding if PO has approved GRNs
                    if (hasApprovedGRN(po.getId())) {
                        createPurchaseOrderOutstanding(po);
                        System.out.println("Created outstanding for PO #" + po.getId() + " (has approved GRN)");
                    } else {
                        System.out.println("Skipped PO #" + po.getId() + " - no approved GRN found");
                    }
                }
            }
        }
    }
    
    /**
     * Create B2B outstanding items for existing Kaccha orders and POs
     */
    @Transactional
    public void createB2BOutstandingForExistingItems() {
        try {
            System.out.println("=== Starting B2B outstanding creation for existing items ===");
            
            // Create outstanding for Kaccha orders
            List<Order> kacchaOrders = orderRepository.findByBillTypeOrderByCreatedAtDesc("Kaccha");
            System.out.println("Found " + kacchaOrders.size() + " Kaccha orders to process");
            
            for (Order order : kacchaOrders) {
                if (order.getTotal() != null && order.getTotal().compareTo(BigDecimal.ZERO) > 0) {
                    // Only create outstanding for confirmed orders (not Pending or Cancelled)
                    if (!"Pending".equals(order.getOrderStatus()) && !"Cancelled".equals(order.getOrderStatus())) {
                        createCustomerOutstanding(order, order.getCreatedAt().plusDays(30));
                    }
                }
            }
            
            // Create outstanding for Purchase Orders that have approved GRNs (following new accounting workflow)
            List<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findAll();
            System.out.println("Found " + purchaseOrders.size() + " purchase orders to process");
            
            for (PurchaseOrder po : purchaseOrders) {
                if (po.getTotalAmount() != null && po.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
                    // Only create outstanding if PO has approved GRNs
                    if (hasApprovedGRN(po.getId())) {
                        createPurchaseOrderOutstanding(po);
                        System.out.println("Created outstanding for PO #" + po.getId() + " (has approved GRN)");
                    } else {
                        System.out.println("Skipped PO #" + po.getId() + " - no approved GRN found");
                    }
                }
            }
            
            System.out.println("=== B2B outstanding creation completed ===");
            
            // Now automatically trigger customer ledger sync to ensure consistency
            System.out.println("=== Triggering automatic customer ledger sync for consistency ===");
            try {
                triggerCustomerLedgerSync();
                System.out.println("Customer ledger sync triggered successfully");
            } catch (Exception e) {
                System.err.println("Warning: Could not trigger customer ledger sync: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("Error creating B2B outstanding items: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create B2B outstanding items", e);
        }
    }
    
    // ==================== AUTOMATIC VOUCHER CREATION ====================
    
    /**
     * Create settlement voucher when outstanding item is fully settled
     */
    private void createSettlementVoucher(Outstanding outstanding, String notes, BigDecimal amount) {
        try {
        	if ("Pakka".equals(outstanding.getOrderType())) {
                System.out.println("Skipping voucher creation for Retail order settlement - accounting already done at confirmation");
                return;
            }
            
            // Only create vouchers for B2B orders (Kaccha)
            if (!"Kaccha".equals(outstanding.getOrderType())) {
                System.out.println("Unknown order type: " + outstanding.getOrderType() + " - skipping voucher creation");
                return;
            }
            System.out.println("Creating settlement voucher for outstanding item #" + outstanding.getId() + " with amount: " + amount + "outstanding.getPaymentMethod() "+ outstanding.getPaymentMethod());
            
            Account debitAccount = null;
            
            if ("Cash".equals(outstanding.getPaymentMethod())) {
                // For cash payments, find Cash account by name
                debitAccount = accountRepository.findById(5L).orElse(null);
                if (debitAccount == null) {
                    System.err.println("Cash account not found, trying ID 5");
                    debitAccount = accountRepository.findById(5L).orElse(null);
                }
            } else {
                // For other payments, find Bank account by name
                debitAccount = accountRepository.findById(6L).orElse(null);
                if (debitAccount == null) {
                    System.err.println("Bank account not found, trying ID 6");
                    debitAccount = accountRepository.findById(6L).orElse(null);
                }
            }
	       	
            // Get account based on payment method for debit entry
           // Account debitAccount = getAccountByPaymentMethod(outstanding.getPaymentMethod());
            if (debitAccount == null) {
                System.err.println("Cannot find account for payment method: " + outstanding.getPaymentMethod());
                return;
            }
            
            // Get appropriate credit account based on outstanding type
            Account creditAccount = null;
            if (outstanding.getType() == Outstanding.OutstandingType.INVOICE_RECEIVABLE) {
                // For receivables, credit Accounts Receivable (1001.01)
                creditAccount = accountRepository.findById(7L).orElse(null);
                if (creditAccount == null) {
                    System.err.println("Cannot find account with code 1001.01 (Accounts Receivable)");
                    return;
                }
            } else if (outstanding.getType() == Outstanding.OutstandingType.INVOICE_PAYABLE || 
                       outstanding.getType() == Outstanding.OutstandingType.PURCHASE_ORDER) {
                // For payables, credit Purchase / Cost of Goods Sold (ID 22)
                creditAccount = accountRepository.findById(22L).orElse(null);
                if (creditAccount == null) {
                    System.err.println("Cannot find account with ID 35 (Purchase / Cost of Goods Sold)");
                    return;
                }
            }
            
            System.out.println("Debit Account: " + (debitAccount != null ? debitAccount.getName() + " (ID: " + debitAccount.getId() + ")" : "NULL"));
            System.out.println("Credit Account: " + (creditAccount != null ? creditAccount.getName() + " (ID: " + creditAccount.getId() + ")" : "NULL"));
            
            if (debitAccount == null || creditAccount == null) {
                System.err.println("Cannot create voucher - missing accounts. Debit: " + (debitAccount != null ? "OK" : "NULL") + 
                                 ", Credit: " + (creditAccount != null ? "OK" : "NULL"));
                return;
            }
            
            Voucher voucher = new Voucher();
            voucher.setDate(java.time.LocalDate.now());
            voucher.setType("Settlement");
            
            String narration = "Settlement of " + amount + " for " + outstanding.getReferenceType() + " #" + outstanding.getReferenceNumber();
            if (notes != null && !notes.trim().isEmpty()) {
                narration += " - " + notes;
            }
            voucher.setNarration(narration);
            Voucher savedVoucher = voucherRepository.save(voucher);
            System.out.println("Created settlement voucher with ID: " + savedVoucher.getId());
            
            // Create voucher entries based on outstanding type
            if (outstanding.getType() == Outstanding.OutstandingType.INVOICE_RECEIVABLE) {
                // Customer payment received
                createVoucherEntry(voucher, debitAccount, amount, true); // Debit Cash/Bank
                createVoucherEntry(voucher, creditAccount, amount, false); // Credit Accounts Receivable
            } else if (outstanding.getType() == Outstanding.OutstandingType.INVOICE_PAYABLE || 
                       outstanding.getType() == Outstanding.OutstandingType.PURCHASE_ORDER) {
                // Supplier payment made - Debit Purchase/Cost of Goods Sold, Credit Cash/Bank
                createVoucherEntry(voucher, creditAccount, amount, true); // Debit Purchase/Cost of Goods Sold
                createVoucherEntry(voucher, debitAccount, amount, false); // Credit Cash/Bank
            }
            
            System.out.println("Created settlement voucher for outstanding item #" + outstanding.getId());
            
        } catch (Exception e) {
            System.err.println("Error creating settlement voucher: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create partial payment voucher when outstanding item is partially paid
     */
    private void createPartialPaymentVoucher(Outstanding outstanding, BigDecimal paidAmount, String notes) {
        try {
            System.out.println("Creating partial payment voucher for outstanding item #" + outstanding.getId() + " with paid amount: " + paidAmount);
            
            if ("Pakka".equals(outstanding.getOrderType())) {
                System.out.println("Skipping voucher creation for Retail order settlement - accounting already done at confirmation");
                return;
            }
            
            // Only create vouchers for B2B orders (Kaccha)
            if (!"Kaccha".equals(outstanding.getOrderType())) {
                System.out.println("Unknown order type: " + outstanding.getOrderType() + " - skipping voucher creation");
                return;
            }
            
            System.out.println("Creating settlement voucher for B2B outstanding item #" + outstanding.getId() + " with amount: " + paidAmount);
            
            Account debitAccount = null;
            if ("Cash".equals(outstanding.getPaymentMethod())) {
                // For cash payments, find Cash account by name
                debitAccount = accountRepository.findById(5L).orElse(null);
                if (debitAccount == null) {
                    System.err.println("Cash account not found, trying ID 5");
                    debitAccount = accountRepository.findById(5L).orElse(null);
                }
            } else {
                // For other payments, find Bank account by name
                debitAccount = accountRepository.findById(6L).orElse(null);
                if (debitAccount == null) {
                    System.err.println("Bank account not found, trying ID 6");
                    debitAccount = accountRepository.findById(6L).orElse(null);
                }
            }
            // Get account based on payment method for debit entry
           // Account debitAccount = getAccountByPaymentMethod(outstanding.getPaymentMethod());
            if (debitAccount == null) {
                System.err.println("Cannot find account for payment method: " + outstanding.getPaymentMethod());
                return;
            }
            
            // Get appropriate credit account based on outstanding type
            Account creditAccount = null;
            if (outstanding.getType() == Outstanding.OutstandingType.INVOICE_RECEIVABLE) {
                // For receivables, credit Accounts Receivable (1001.01)
                creditAccount = accountRepository.findById(7L).orElse(null);
                if (creditAccount == null) {
                    System.err.println("Cannot find account with code 1001.01 (Accounts Receivable)");
                    return;
                }
            } else if (outstanding.getType() == Outstanding.OutstandingType.INVOICE_PAYABLE || 
                       outstanding.getType() == Outstanding.OutstandingType.PURCHASE_ORDER) {
                // For payables, credit Purchase / Cost of Goods Sold (ID 35)
                creditAccount = accountRepository.findById(22L).orElse(null);
                if (creditAccount == null) {
                    System.err.println("Cannot find account with ID 35 (Purchase / Cost of Goods Sold)");
                    return;
                }
            }
            
            System.out.println("Debit Account: " + (debitAccount != null ? debitAccount.getName() + " (ID: " + debitAccount.getId() + ")" : "NULL"));
            System.out.println("Credit Account: " + (creditAccount != null ? creditAccount.getName() + " (ID: " + creditAccount.getId() + ")" : "NULL"));
            
            if (debitAccount == null || creditAccount == null) {
                System.err.println("Cannot create voucher - missing accounts. Debit: " + (debitAccount != null ? "OK" : "NULL") + 
                                 ", Credit: " + (creditAccount != null ? "OK" : "NULL"));
                return;
            }
            
            Voucher voucher = new Voucher();
            voucher.setDate(java.time.LocalDate.now());
            voucher.setType("Partial Payment");
            
            String narration = "Partial payment of " + paidAmount + " for " + outstanding.getReferenceType() + " #" + outstanding.getReferenceNumber();
            if (notes != null && !notes.trim().isEmpty()) {
                narration += " - " + notes;
            }
            voucher.setNarration(narration);
            Voucher savedVoucher = voucherRepository.save(voucher);
            System.out.println("Created voucher with ID: " + savedVoucher.getId());
            
            // Create voucher entries based on outstanding type
            if (outstanding.getType() == Outstanding.OutstandingType.INVOICE_RECEIVABLE) {
                // Customer partial payment received
                createVoucherEntry(voucher, debitAccount, paidAmount, true); // Debit Cash/Bank
                createVoucherEntry(voucher, creditAccount, paidAmount, false); // Credit Accounts Receivable
            } else if (outstanding.getType() == Outstanding.OutstandingType.INVOICE_PAYABLE || 
                       outstanding.getType() == Outstanding.OutstandingType.PURCHASE_ORDER) {
                // Supplier partial payment made - Debit Purchase/Cost of Goods Sold, Credit Cash/Bank
                createVoucherEntry(voucher, creditAccount, paidAmount, true); // Debit Purchase/Cost of Goods Sold
                createVoucherEntry(voucher, debitAccount, paidAmount, false); // Credit Cash/Bank
            }
            
            System.out.println("Created partial payment voucher for outstanding item #" + outstanding.getId());
            
        } catch (Exception e) {
            System.err.println("Error creating partial payment voucher: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get account based on payment method
     */
    private Account getAccountByPaymentMethod(String paymentMethod) {
        try {
            System.out.println("Looking for account for payment method: " + paymentMethod);
            
            if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
                System.err.println("Payment method is null or empty");
                return null;
            }
            
            String method = paymentMethod.trim().toLowerCase();
            
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
    
    /**
     * Create voucher entry
     */
    private void createVoucherEntry(Voucher voucher, Account account, BigDecimal amount, boolean isDebit) {
        try {
            System.out.println("Creating voucher entry - Account: " + (account != null ? account.getName() + " (ID: " + account.getId() + ")" : "NULL"));
            System.out.println("Amount: " + amount + ", IsDebit: " + isDebit);
            
            VoucherEntry entry = new VoucherEntry();
            entry.setVoucher(voucher);
            entry.setAccount(account);
            
            if (isDebit) {
                entry.setDebit(amount);
                entry.setCredit(BigDecimal.ZERO);
                System.out.println("Set Debit: " + amount + ", Credit: 0");
            } else {
                entry.setDebit(BigDecimal.ZERO);
                entry.setCredit(amount);
                System.out.println("Set Debit: 0, Credit: " + amount);
            }
            
            VoucherEntry savedEntry = voucherEntryRepository.save(entry);
            System.out.println("Saved voucher entry with ID: " + savedEntry.getId());
            System.out.println("Saved entry - Debit: " + savedEntry.getDebit() + ", Credit: " + savedEntry.getCredit());
            System.out.println("Saved entry - Account ID: " + (savedEntry.getAccount() != null ? savedEntry.getAccount().getId() : "NULL"));
            
        } catch (Exception e) {
            System.err.println("Error creating voucher entry: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get default cash account
     */
    private Account getDefaultCashAccount() {
        // Try multiple common cash account names
        List<String> cashAccountNames = Arrays.asList("Cash", "Bank", "Cash in Hand", "Petty Cash", "Bank Account");
        for (String name : cashAccountNames) {
            List<Account> accounts = accountRepository.findByNameContainingIgnoreCase(name);
            System.out.println("Searching for cash account '" + name + "', found: " + accounts.size());
            if (!accounts.isEmpty()) {
                // Filter out accounts that are clearly not cash accounts
                List<Account> filteredAccounts = accounts.stream()
                    .filter(acc -> !acc.getName().toLowerCase().contains("revenue") && 
                                   !acc.getName().toLowerCase().contains("income") &&
                                   !acc.getName().toLowerCase().contains("sales"))
                    .toList();
                
                if (!filteredAccounts.isEmpty()) {
                    Account account = filteredAccounts.get(0);
                    System.out.println("Found suitable cash account: " + account.getName() + " (ID: " + account.getId() + ")");
                    return account;
                }
            }
        }
        System.err.println("No suitable cash account found. Please create a cash account in your chart of accounts.");
        return null;
    }
    
    /**
     * Get default customer account
     */
    private Account getDefaultCustomerAccount() {
        // Try multiple common customer account names
        List<String> customerAccountNames = Arrays.asList("Customer", "Accounts Receivable", "Sundry Debtors", "Trade Receivables");
        for (String name : customerAccountNames) {
            List<Account> accounts = accountRepository.findByNameContainingIgnoreCase(name);
            System.out.println("Searching for customer account '" + name + "', found: " + accounts.size());
            if (!accounts.isEmpty()) {
                // Filter out accounts that are clearly not customer accounts
                List<Account> filteredAccounts = accounts.stream()
                    .filter(acc -> !acc.getName().toLowerCase().contains("revenue") && 
                                   !acc.getName().toLowerCase().contains("income") &&
                                   !acc.getName().toLowerCase().contains("sales"))
                    .toList();
                
                if (!filteredAccounts.isEmpty()) {
                    Account account = filteredAccounts.get(0);
                    System.out.println("Found suitable customer account: " + account.getName() + " (ID: " + account.getId() + ")");
                    return account;
                }
            }
        }
        System.err.println("No suitable customer account found. Please create a customer account in your chart of accounts.");
        return null;
    }
    
    /**
     * Get default supplier account
     */
    private Account getDefaultSupplierAccount() {
        // Try multiple common supplier account names
        List<String> supplierAccountNames = Arrays.asList("Supplier", "Accounts Payable", "Sundry Creditors", "Trade Payables");
        for (String name : supplierAccountNames) {
            List<Account> accounts = accountRepository.findByNameContainingIgnoreCase(name);
            System.out.println("Searching for supplier account '" + name + "', found: " + accounts.size());
            if (!accounts.isEmpty()) {
                Account account = accounts.get(0);
                System.out.println("Found supplier account: " + account.getName() + " (ID: " + account.getId() + ")");
                return account;
            }
        }
        System.err.println("No supplier account found. Please create a supplier account in your chart of accounts.");
        return null;
    }
    
    /**
     * Create or find a fallback account if the default ones don't exist
     */
    private Account createOrFindFallbackAccount(String accountName, String accountType) {
        try {
            System.out.println("Looking for fallback account: " + accountName + " (" + accountType + ")");
            
            // First try to find an existing account with similar name
            List<Account> existingAccounts = accountRepository.findByNameContainingIgnoreCase(accountName);
            System.out.println("Found " + existingAccounts.size() + " accounts with similar name '" + accountName + "'");
            
            if (!existingAccounts.isEmpty()) {
                Account found = existingAccounts.get(0);
                System.out.println("Using account with similar name: " + found.getName() + " (ID: " + found.getId() + ")");
                return found;
            }
            
            // If no existing account, try to find any account of the same type
            List<Account> allAccounts = accountRepository.findAll();
            List<Account> typeAccounts = allAccounts.stream()
                .filter(acc -> acc.getType() != null && acc.getType().equalsIgnoreCase(accountType))
                .toList();
            
            System.out.println("Found " + typeAccounts.size() + " accounts of type '" + accountType + "'");
            
            if (!typeAccounts.isEmpty()) {
                Account found = typeAccounts.get(0);
                System.out.println("Using account of same type: " + found.getName() + " (ID: " + found.getId() + ")");
                return found;
            }
            
            // If still no account found, try to find any account that might be suitable
            System.out.println("No suitable account found, trying to find any available account");
            if (!allAccounts.isEmpty()) {
                Account fallback = allAccounts.get(0);
                System.out.println("Using fallback account: " + fallback.getName() + " (ID: " + fallback.getId() + ")");
                return fallback;
            }
            
            // If still no account found, return null
            System.err.println("No fallback account found for " + accountName + " (" + accountType + ")");
            return null;
            
        } catch (Exception e) {
            System.err.println("Error finding fallback account: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Create default accounts if they don't exist
     */
    public void createDefaultAccountsIfNeeded() {
        try {
            System.out.println("Checking if default accounts exist...");
            
            // Check if we have any accounts at all
            List<Account> allAccounts = accountRepository.findAll();
            if (allAccounts.isEmpty()) {
                System.out.println("No accounts found, creating default accounts...");
                
                // Create Cash account
                Account cashAccount = new Account();
                cashAccount.setName("Cash");
                cashAccount.setType("Asset");
                cashAccount.setCode("1001");
                accountRepository.save(cashAccount);
                System.out.println("Created Cash account with ID: " + cashAccount.getId());
                
                // Create Customer account
                Account customerAccount = new Account();
                customerAccount.setName("Accounts Receivable");
                customerAccount.setType("Asset");
                customerAccount.setCode("1200");
                accountRepository.save(customerAccount);
                System.out.println("Created Customer account with ID: " + customerAccount.getId());
                
                // Create Supplier account
                Account supplierAccount = new Account();
                supplierAccount.setName("Accounts Payable");
                supplierAccount.setType("Liability");
                supplierAccount.setCode("2000");
                accountRepository.save(supplierAccount);
                System.out.println("Created Supplier account with ID: " + supplierAccount.getId());
                
            } else {
                System.out.println("Found " + allAccounts.size() + " existing accounts");
                for (Account acc : allAccounts) {
                    System.out.println("- " + acc.getName() + " (ID: " + acc.getId() + ", Type: " + acc.getType() + ")");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error creating default accounts: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Debug method to check what accounts exist in the system
     */
    public String debugAccounts() {
        try {
            List<Account> allAccounts = accountRepository.findAll();
            StringBuilder sb = new StringBuilder();
            sb.append("Total accounts found: ").append(allAccounts.size()).append("\n\n");
            
            for (Account account : allAccounts) {
                sb.append("ID: ").append(account.getId())
                  .append(", Name: ").append(account.getName())
                  .append(", Type: ").append(account.getType())
                  .append("\n");
            }
            
            // Test the default account methods
            sb.append("\n=== Testing Default Account Methods ===\n");
            
            Account cashAccount = getDefaultCashAccount();
            sb.append("Cash Account: ").append(cashAccount != null ? cashAccount.getName() : "NOT FOUND").append("\n");
            
            Account customerAccount = getDefaultCustomerAccount();
            sb.append("Customer Account: ").append(customerAccount != null ? customerAccount.getName() : "NOT FOUND").append("\n");
            
            Account supplierAccount = getDefaultSupplierAccount();
            sb.append("Supplier Account: ").append(supplierAccount != null ? supplierAccount.getName() : "NOT FOUND").append("\n");
            
            return sb.toString();
            
        } catch (Exception e) {
            return "Error: " + e.getMessage() + "\n" + e.getStackTrace();
        }
    }
    
    /**
     * Sync customer ledgers for B2B orders to ensure consistency with outstanding items
     */
    @Transactional
    public void syncCustomerLedgersForB2BOrders() {
        try {
            System.out.println("=== Starting customer ledger sync for B2B orders ===");
            
            // Get all B2B orders (Kaccha)
            List<Order> b2bOrders = orderRepository.findByBillTypeOrderByCreatedAtDesc("Kaccha");
            System.out.println("Found " + b2bOrders.size() + " B2B orders to sync");
            
            for (Order order : b2bOrders) {
                if (order.getTotal() != null && order.getTotal().compareTo(BigDecimal.ZERO) > 0) {
                    // Only create customer ledger entries for confirmed orders (not Pending or Cancelled)
                    if (!"Pending".equals(order.getOrderStatus()) && !"Cancelled".equals(order.getOrderStatus())) {
                        try {
                            // Find or create customer ledger
                            CustomerLedger customerLedger = customerLedgerService.findOrCreateCustomerLedger(
                                order.getName(), 
                                order.getUserPhone(), 
                                null // Order doesn't have email field
                            );
                            
                            // Check if invoice entry already exists
                            List<CustomerLedgerEntry> existingEntries = customerLedgerEntryRepository
                                .findByReferenceTypeAndReferenceId("ORDER", order.getId());
                            
                            if (existingEntries.isEmpty()) {
                                // Add invoice entry
                                customerLedgerService.addInvoiceEntry(customerLedger, order, order.getTotal());
                                System.out.println("Created customer ledger entry for order ID: " + order.getId() + 
                                                " - Amount: " + order.getTotal() + " - Customer: " + order.getName());
                            } else {
                                System.out.println("Customer ledger entry already exists for order ID: " + order.getId());
                            }
                            
                        } catch (Exception e) {
                            System.err.println("Error syncing customer ledger for order ID " + order.getId() + ": " + e.getMessage());
                        }
                    }
                }
            }
            
            System.out.println("=== Customer ledger sync completed ===");
            
        } catch (Exception e) {
            System.err.println("Error syncing customer ledgers for B2B orders: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to sync customer ledgers", e);
        }
    }
    
    /**
     * Trigger customer ledger sync to ensure consistency with outstanding items
     * This method is called automatically when outstanding items are created
     */
    public void triggerCustomerLedgerSync() {
        try {
            System.out.println("=== Triggering customer ledger sync from outstanding service ===");
            // Actually call the customer ledger service to sync
            customerLedgerService.createCustomerLedgersForExistingB2BOrders();
            System.out.println("Customer ledger sync triggered successfully");
        } catch (Exception e) {
            System.err.println("Error triggering customer ledger sync: " + e.getMessage());
        }
    }
    
    /**
     * Create reversal voucher for order cancellation
     * Only creates reversal if order was previously confirmed (had a voucher created)
     * This reverses the original voucher created when order was confirmed
     */
    private void createReversalVoucherForOrderCancellation(Order order) {
        try {
            System.out.println("=== Creating reversal voucher for order cancellation ===");
            System.out.println("Order ID: " + order.getId());
            System.out.println("Order Type: " + order.getBillType());
            System.out.println("Amount: " + order.getTotal());
            
            if (order.getTotal() == null || order.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
                System.out.println("Order amount is zero or null, skipping voucher reversal");
                return;
            }
            
            if ("Kaccha".equals(order.getBillType())) {
                // B2B Order Reversal
                // Original: Debit Accounts Receivable (7), Credit Sales (3001)
                // Reversal: Credit Accounts Receivable (7), Debit Sales (3001)
                createB2BOrderReversalVoucher(order);
            } else if ("Pakka".equals(order.getBillType())) {
                // Retail Order Reversal  
                // Original: Debit Bank Account (6), Credit Sales (3001)
                // Reversal: Credit Bank Account (6), Debit Sales (3001)
                createRetailOrderReversalVoucher(order);
            }
            
            System.out.println("Reversal voucher created successfully for order #" + order.getId());
            
        } catch (Exception e) {
            System.err.println("Error creating reversal voucher for order #" + order.getId() + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Create reversal voucher for B2B order cancellation
     * Credit: Accounts Receivable (7) - to reverse the original debit
     * Debit: Sales (3001) - to reverse the original credit
     */
    private void createB2BOrderReversalVoucher(Order order) {
        // Find the required accounts
        Account accountsReceivable = accountRepository.findById(7L).orElse(null);
        Account salesAccount = accountRepository.findByCode("3001");
        
        if (accountsReceivable == null) {
            System.err.println("Accounts Receivable account (ID: 7) not found for reversal voucher");
            return;
        }
        
        if (salesAccount == null) {
            System.err.println("Sales account (3001) not found for reversal voucher");
            return;
        }
        
        // Create reversal voucher
        Voucher voucher = new Voucher();
        voucher.setDate(LocalDate.now());
        voucher.setNarration("Order Cancellation Reversal - Invoice: " + order.getInvoiceNumber());
        voucher.setType("REVERSAL");
        Voucher savedVoucher = voucherRepository.save(voucher);
        
        // Create credit entry (Accounts Receivable) - reverses original debit
        VoucherEntry creditEntry = new VoucherEntry();
        creditEntry.setVoucher(savedVoucher);
        creditEntry.setAccount(accountsReceivable);
        creditEntry.setDebit(BigDecimal.ZERO);
        creditEntry.setCredit(order.getTotal());
        creditEntry.setDescription("Reversal - Debtors - Order #" + order.getId() + " - " + order.getName());
        voucherEntryRepository.save(creditEntry);
        
        // Create debit entry (Sales) - reverses original credit
        VoucherEntry debitEntry = new VoucherEntry();
        debitEntry.setVoucher(savedVoucher);
        debitEntry.setAccount(salesAccount);
        debitEntry.setDebit(order.getTotal());
        debitEntry.setCredit(BigDecimal.ZERO);
        debitEntry.setDescription("Reversal - Sales - Order #" + order.getId() + " - " + order.getName());
        voucherEntryRepository.save(debitEntry);
        
        System.out.println("B2B reversal voucher created - Credit Accounts Receivable, Debit Sales");
    }
    
    /**
     * Create reversal voucher for Retail order cancellation
     * Credit: Bank Account (6) - to reverse the original debit
     * Debit: Sales (3001) - to reverse the original credit
     */
    private void createRetailOrderReversalVoucher(Order order) {
        // Find the required accounts
        Account bankAccount = accountRepository.findById(6L).orElse(null);
        Account salesAccount = accountRepository.findByCode("3001");
        
        if (bankAccount == null) {
            System.err.println("Bank Account (ID: 6) not found for reversal voucher");
            return;
        }
        
        if (salesAccount == null) {
            System.err.println("Sales account (3001) not found for reversal voucher");
            return;
        }
        
        // Create reversal voucher
        Voucher voucher = new Voucher();
        voucher.setDate(LocalDate.now());
        voucher.setNarration("Order Cancellation Reversal - Invoice: " + order.getInvoiceNumber());
        voucher.setType("REVERSAL");
        Voucher savedVoucher = voucherRepository.save(voucher);
        
        // Create credit entry (Bank Account) - reverses original debit
        VoucherEntry creditEntry = new VoucherEntry();
        creditEntry.setVoucher(savedVoucher);
        creditEntry.setAccount(bankAccount);
        creditEntry.setDebit(BigDecimal.ZERO);
        creditEntry.setCredit(order.getTotal());
        creditEntry.setDescription("Reversal - Bank Account - Order #" + order.getId() + " - " + order.getName());
        voucherEntryRepository.save(creditEntry);
        
        // Create debit entry (Sales) - reverses original credit
        VoucherEntry debitEntry = new VoucherEntry();
        debitEntry.setVoucher(savedVoucher);
        debitEntry.setAccount(salesAccount);
        debitEntry.setDebit(order.getTotal());
        debitEntry.setCredit(BigDecimal.ZERO);
        debitEntry.setDescription("Reversal - Sales - Order #" + order.getId() + " - " + order.getName());
        voucherEntryRepository.save(debitEntry);
        
        System.out.println("Retail reversal voucher created - Credit Bank Account, Debit Sales");
    }
}

