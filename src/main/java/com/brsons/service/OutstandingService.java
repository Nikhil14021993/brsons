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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.ArrayList;

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
        
        // Set payment details
        outstanding.setPaymentMethod(paymentMethod);
        outstanding.setPaymentReference(paymentReference);
        outstanding.setPaymentDate(LocalDateTime.now());
        
        outstanding.setStatus(Outstanding.OutstandingStatus.PARTIALLY_PAID);
        outstanding.setAmount(outstanding.getAmount().subtract(paidAmount));
        if (notes != null) {
            outstanding.setNotes(notes);
        }
        outstanding.setUpdatedAt(LocalDateTime.now());
        
        // Automatically create voucher for partial payment
        createPartialPaymentVoucher(outstanding, paidAmount, notes);
        
        // If fully paid, mark as settled
        if (outstanding.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            outstanding.setStatus(Outstanding.OutstandingStatus.SETTLED);
            // Create final settlement voucher
            createSettlementVoucher(outstanding, notes);
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
        
        outstanding.setStatus(Outstanding.OutstandingStatus.SETTLED);
        outstanding.setAmount(BigDecimal.ZERO);
        if (notes != null) {
            outstanding.setNotes(notes);
        }
        outstanding.setUpdatedAt(LocalDateTime.now());
        
        // Automatically create voucher for settlement
        createSettlementVoucher(outstanding, notes);
        
        return outstandingRepository.save(outstanding);
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
    private void createSettlementVoucher(Outstanding outstanding, String notes) {
        try {
            // Get default accounts (you may need to adjust these based on your chart of accounts)
            Account cashAccount = getDefaultCashAccount();
            Account customerAccount = getDefaultCustomerAccount();
            Account supplierAccount = getDefaultSupplierAccount();
            
            if (cashAccount == null || customerAccount == null || supplierAccount == null) {
                System.err.println("Default accounts not found. Cannot create voucher.");
                return;
            }
            
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
                createVoucherEntry(voucher, cashAccount, outstanding.getAmount(), true); // Debit Cash
                createVoucherEntry(voucher, customerAccount, outstanding.getAmount(), false); // Credit Customer
            } else if (outstanding.getType() == Outstanding.OutstandingType.INVOICE_PAYABLE || 
                       outstanding.getType() == Outstanding.OutstandingType.PURCHASE_ORDER) {
                // Supplier payment made
                createVoucherEntry(voucher, supplierAccount, outstanding.getAmount(), true); // Debit Supplier
                createVoucherEntry(voucher, cashAccount, outstanding.getAmount(), false); // Credit Cash
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
            // Get default accounts
            Account cashAccount = getDefaultCashAccount();
            Account customerAccount = getDefaultCustomerAccount();
            Account supplierAccount = getDefaultSupplierAccount();
            
            if (cashAccount == null || customerAccount == null || supplierAccount == null) {
                System.err.println("Default accounts not found. Cannot create voucher.");
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
            voucherRepository.save(voucher);
            
            // Create voucher entries based on outstanding type
            if (outstanding.getType() == Outstanding.OutstandingType.INVOICE_RECEIVABLE) {
                // Customer partial payment received
                createVoucherEntry(voucher, cashAccount, paidAmount, true); // Debit Cash
                createVoucherEntry(voucher, customerAccount, paidAmount, false); // Credit Customer
            } else if (outstanding.getType() == Outstanding.OutstandingType.INVOICE_PAYABLE || 
                       outstanding.getType() == Outstanding.OutstandingType.PURCHASE_ORDER) {
                // Supplier partial payment made
                createVoucherEntry(voucher, supplierAccount, paidAmount, true); // Debit Supplier
                createVoucherEntry(voucher, cashAccount, paidAmount, false); // Credit Cash
            }
            
            System.out.println("Created partial payment voucher for outstanding item #" + outstanding.getId());
            
        } catch (Exception e) {
            System.err.println("Error creating partial payment voucher: " + e.getMessage());
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
     * Get default cash account
     */
    private Account getDefaultCashAccount() {
        return accountRepository.findByNameContainingIgnoreCase("Cash")
            .stream().findFirst().orElse(null);
    }
    
    /**
     * Get default customer account
     */
    private Account getDefaultCustomerAccount() {
        return accountRepository.findByNameContainingIgnoreCase("Customer")
            .stream().findFirst().orElse(null);
    }
    
    /**
     * Get default supplier account
     */
    private Account getDefaultSupplierAccount() {
        return accountRepository.findByNameContainingIgnoreCase("Supplier")
            .stream().findFirst().orElse(null);
    }
}

