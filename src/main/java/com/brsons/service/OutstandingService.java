package com.brsons.service;

import com.brsons.model.Outstanding;

import com.brsons.model.Order;
import com.brsons.model.PurchaseOrder;
import com.brsons.model.Supplier;
import com.brsons.repository.OutstandingRepository;
import com.brsons.repository.OrderRepository;
import com.brsons.repository.PurchaseOrderRepository;
import com.brsons.repository.SupplierRepository;
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
            order.getName()
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
    public Outstanding markPartiallyPaid(Long outstandingId, BigDecimal paidAmount, String notes) {
        Outstanding outstanding = outstandingRepository.findById(outstandingId)
            .orElseThrow(() -> new RuntimeException("Outstanding item not found"));
        
        outstanding.setStatus(Outstanding.OutstandingStatus.PARTIALLY_PAID);
        outstanding.setAmount(outstanding.getAmount().subtract(paidAmount));
        if (notes != null) {
            outstanding.setNotes(notes);
        }
        outstanding.setUpdatedAt(LocalDateTime.now());
        
        // If fully paid, mark as settled
        if (outstanding.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            outstanding.setStatus(Outstanding.OutstandingStatus.SETTLED);
        }
        
        return outstandingRepository.save(outstanding);
    }
    
    /**
     * Mark outstanding item as settled
     */
    @Transactional
    public Outstanding markAsSettled(Long outstandingId, String notes) {
        Outstanding outstanding = outstandingRepository.findById(outstandingId)
            .orElseThrow(() -> new RuntimeException("Outstanding item not found"));
        
        outstanding.setStatus(Outstanding.OutstandingStatus.SETTLED);
        outstanding.setAmount(BigDecimal.ZERO);
        if (notes != null) {
            outstanding.setNotes(notes);
        }
        outstanding.setUpdatedAt(LocalDateTime.now());
        
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
     * Get outstanding items by type
     */
    public List<Outstanding> getOutstandingByType(Outstanding.OutstandingType type) {
        return outstandingRepository.findByType(type);
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
}
