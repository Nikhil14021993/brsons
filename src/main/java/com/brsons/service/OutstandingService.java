package com.brsons.service;

import com.brsons.model.Outstanding;

import com.brsons.model.Order;
import com.brsons.model.PurchaseOrder;
import com.brsons.model.Supplier;
import com.brsons.model.Voucher;
import com.brsons.model.VoucherEntry;
import com.brsons.model.Account;
import com.brsons.repository.OutstandingRepository;
import com.brsons.repository.OrderRepository;
import com.brsons.repository.PurchaseOrderRepository;
import com.brsons.repository.SupplierRepository;
import com.brsons.repository.VoucherRepository;
import com.brsons.repository.VoucherEntryRepository;
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
        
        return outstandingRepository.save(outstanding);
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
     * Create outstanding item for purchase order
     */
    @Transactional
    public Outstanding createPurchaseOrderOutstanding(PurchaseOrder po) {
        // Check if outstanding already exists
        List<Outstanding> existing = outstandingRepository.findByReferenceTypeAndReferenceId("PURCHASE_ORDER", po.getId());
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        
        Supplier supplier = po.getSupplier();
        BigDecimal totalAmount = po.getTotalAmount();
        
        // Set due date based on payment terms
        LocalDateTime dueDate = LocalDateTime.now().plusDays(30); // Default 30 days
        if (po.getPaymentTerms() != null && po.getPaymentTerms().contains("days")) {
            try {
                String daysStr = po.getPaymentTerms().replaceAll("[^0-9]", "");
                int days = Integer.parseInt(daysStr);
                dueDate = LocalDateTime.now().plusDays(days);
            } catch (NumberFormatException e) {
                // Use default 30 days
            }
        }
        
        Outstanding outstanding = new Outstanding(
            Outstanding.OutstandingType.PURCHASE_ORDER,
            po.getId(),
            "PURCHASE_ORDER",
            po.getPoNumber() != null ? po.getPoNumber() : "PO-" + po.getId(),
            totalAmount,
            dueDate,
            supplier != null ? supplier.getCompanyName() : "Unknown Supplier"
        );
        
        outstanding.setDescription("Purchase Order #" + po.getId());
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
     * Create outstanding items for existing orders and POs
     */
    @Transactional
    public void createOutstandingForExistingItems() {
        // Create outstanding for retail orders (Pakka bill type) without outstanding items
        List<Order> retailOrders = orderRepository.findByBillTypeOrderByCreatedAtDesc("Pakka");
        for (Order order : retailOrders) {
            if (order.getTotal() != null && order.getTotal().compareTo(BigDecimal.ZERO) > 0) {
                // Check if outstanding already exists
                List<Outstanding> existing = outstandingRepository.findByReferenceTypeAndReferenceId("ORDER", order.getId());
                if (existing.isEmpty()) {
                    // Set due date as 30 days from order creation
                    LocalDateTime dueDate = order.getCreatedAt().plusDays(30);
                    createCustomerOutstanding(order, dueDate);
                }
            }
        }
        
        // Create outstanding for POs without outstanding items
        List<PurchaseOrder> pos = purchaseOrderRepository.findAll();
        for (PurchaseOrder po : pos) {
            if (po.getTotalAmount() != null && po.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
                // Check if outstanding already exists
                List<Outstanding> existing = outstandingRepository.findByReferenceTypeAndReferenceId("PURCHASE_ORDER", po.getId());
                if (existing.isEmpty()) {
                    createPurchaseOrderOutstanding(po);
                }
            }
        }
    }
    
    /**
     * Create outstanding items for B2B orders (Kaccha bill type) and purchase orders
     */
    @Transactional
    public void createB2BOutstandingForExistingItems() {
        // Create outstanding for B2B orders (Kaccha bill type) without outstanding items
        List<Order> b2bOrders = orderRepository.findByBillTypeOrderByCreatedAtDesc("Kaccha");
        for (Order order : b2bOrders) {
            if (order.getTotal() != null && order.getTotal().compareTo(BigDecimal.ZERO) > 0) {
                // Check if outstanding already exists
                List<Outstanding> existing = outstandingRepository.findByReferenceTypeAndReferenceId("ORDER", order.getId());
                if (existing.isEmpty()) {
                    // Set due date as 30 days from order creation
                    LocalDateTime dueDate = order.getCreatedAt().plusDays(30);
                    createCustomerOutstanding(order, dueDate);
                }
            }
        }
        
        // Create outstanding for purchase orders without outstanding items
        List<PurchaseOrder> pos = purchaseOrderRepository.findAll();
        for (PurchaseOrder po : pos) {
            if (po.getTotalAmount() != null && po.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
                // Check if outstanding already exists
                List<Outstanding> existing = outstandingRepository.findByReferenceTypeAndReferenceId("PURCHASE_ORDER", po.getId());
                if (existing.isEmpty()) {
                    createPurchaseOrderOutstanding(po);
                }
            }
        }
    }
    
    // ==================== AUTOMATIC VOUCHER CREATION ====================
    
    /**
     * Create settlement voucher when outstanding item is fully settled
     */
    private void createSettlementVoucher(Outstanding outstanding, String notes, BigDecimal amount) {
        try {
            System.out.println("Creating settlement voucher for outstanding item #" + outstanding.getId() + " with amount: " + amount);
            
            // Get account based on payment method for debit entry
            Account debitAccount = getAccountByPaymentMethod(outstanding.getPaymentMethod());
            if (debitAccount == null) {
                System.err.println("Cannot find account for payment method: " + outstanding.getPaymentMethod());
                return;
            }
            
            // Get appropriate credit account based on outstanding type
            Account creditAccount = null;
            if (outstanding.getType() == Outstanding.OutstandingType.INVOICE_RECEIVABLE) {
                // For receivables, credit Sales Revenue (ID 5)
                creditAccount = accountRepository.findById(5L).orElse(null);
                if (creditAccount == null) {
                    System.err.println("Cannot find account with ID 5 (Sales Revenue)");
                    return;
                }
            } else if (outstanding.getType() == Outstanding.OutstandingType.INVOICE_PAYABLE || 
                       outstanding.getType() == Outstanding.OutstandingType.PURCHASE_ORDER) {
                // For payables, credit Purchase Expense (ID 7)
                creditAccount = accountRepository.findById(7L).orElse(null);
                if (creditAccount == null) {
                    System.err.println("Cannot find account with ID 7 (Purchase Expense)");
                    return;
                }
            }
            
            System.out.println("Debit Account: " + debitAccount.getName() + " (ID: " + debitAccount.getId() + ")");
            System.out.println("Credit Account: " + creditAccount.getName() + " (ID: " + creditAccount.getId() + ")");
            
            Voucher voucher = new Voucher();
            voucher.setDate(java.time.LocalDate.now());
            voucher.setType("Settlement");
            
            String narration = "Settlement of " + outstanding.getReferenceType() + " #" + outstanding.getReferenceNumber();
            if (notes != null && !notes.trim().isEmpty()) {
                narration += " - " + notes;
            }
            voucher.setNarration(narration);
            voucherRepository.save(voucher);
            
            // Create voucher entries based on outstanding type
            if (outstanding.getType() == Outstanding.OutstandingType.INVOICE_RECEIVABLE) {
                // Customer payment received
                createVoucherEntry(voucher, debitAccount, amount, true); // Debit based on payment method
                createVoucherEntry(voucher, creditAccount, amount, false); // Credit Sales Revenue (ID 5)
            } else if (outstanding.getType() == Outstanding.OutstandingType.INVOICE_PAYABLE || 
                       outstanding.getType() == Outstanding.OutstandingType.PURCHASE_ORDER) {
                // Supplier payment made
                createVoucherEntry(voucher, debitAccount, amount, true); // Debit based on payment method
                createVoucherEntry(voucher, creditAccount, amount, false); // Credit Purchase Expense (ID 7)
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
            
            // Get account based on payment method for debit entry
            Account debitAccount = getAccountByPaymentMethod(outstanding.getPaymentMethod());
            if (debitAccount == null) {
                System.err.println("Cannot find account for payment method: " + outstanding.getPaymentMethod());
                return;
            }
            
            // Get appropriate credit account based on outstanding type
            Account creditAccount = null;
            if (outstanding.getType() == Outstanding.OutstandingType.INVOICE_RECEIVABLE) {
                // For receivables, credit Sales Revenue (ID 5)
                creditAccount = accountRepository.findById(5L).orElse(null);
                if (creditAccount == null) {
                    System.err.println("Cannot find account with ID 5 (Sales Revenue)");
                    return;
                }
            } else if (outstanding.getType() == Outstanding.OutstandingType.INVOICE_PAYABLE || 
                       outstanding.getType() == Outstanding.OutstandingType.PURCHASE_ORDER) {
                // For payables, credit Purchase Expense (ID 7)
                creditAccount = accountRepository.findById(7L).orElse(null);
                if (creditAccount == null) {
                    System.err.println("Cannot find account with ID 7 (Purchase Expense)");
                    return;
                }
            }
            
            System.out.println("Debit Account: " + debitAccount.getName() + " (ID: " + debitAccount.getId() + ")");
            System.out.println("Credit Account: " + creditAccount.getName() + " (ID: " + creditAccount.getId() + ")");
            
            Voucher voucher = new Voucher();
            voucher.setDate(java.time.LocalDate.now());
            voucher.setType("Partial Payment");
            
            String narration = "Partial payment of " + paidAmount + " for " + outstanding.getReferenceType() + " #" + outstanding.getReferenceNumber();
            if (notes != null && !notes.trim().isEmpty()) {
                narration += " - " + notes;
            }
            voucher.setNarration(narration);
            voucherRepository.save(voucher);
            
            // Create voucher entries based on outstanding type
            if (outstanding.getType() == Outstanding.OutstandingType.INVOICE_RECEIVABLE) {
                // Customer partial payment received
                createVoucherEntry(voucher, debitAccount, paidAmount, true); // Debit based on payment method
                createVoucherEntry(voucher, creditAccount, paidAmount, false); // Credit Sales Revenue (ID 5)
            } else if (outstanding.getType() == Outstanding.OutstandingType.INVOICE_PAYABLE || 
                       outstanding.getType() == Outstanding.OutstandingType.PURCHASE_ORDER) {
                // Supplier partial payment made
                createVoucherEntry(voucher, debitAccount, paidAmount, true); // Debit based on payment method
                createVoucherEntry(voucher, creditAccount, paidAmount, false); // Credit Purchase Expense (ID 7)
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
}

