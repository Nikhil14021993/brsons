package com.brsons.service;

import com.brsons.dto.OrderDisplayDto;
import com.brsons.model.Account;
import com.brsons.model.Order;
import com.brsons.model.Voucher;
import com.brsons.model.VoucherEntry;
import com.brsons.repository.AccountRepository;
import com.brsons.repository.OrderRepository;
import com.brsons.repository.OrderItemRepository;
import com.brsons.repository.ProductRepository;
import com.brsons.repository.VoucherEntryRepository;
import com.brsons.repository.VoucherRepository;
import com.brsons.model.OrderItem;
import com.brsons.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminOrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private OutstandingService outstandingService;
    
    @Autowired
    private CustomerLedgerService customerLedgerService;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private VoucherRepository voucherRepository;
    
    @Autowired
    private VoucherEntryRepository voucherEntryRepository;
    
    	public List<OrderDisplayDto> getAllOrders() {
		// Filter to show only orders with bill_type = 'Pakka'
		List<Order> orders = orderRepository.findByBillTypeOrderByCreatedAtDesc("Pakka");
		
		return orders.stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());
	}
    
    	public List<OrderDisplayDto> getOrdersByStatus(String status) {
		// First get all orders with bill_type = 'Pakka', then filter by status
		List<Order> pakkaOrders = orderRepository.findByBillTypeOrderByCreatedAtDesc("Pakka");
		
		return pakkaOrders.stream()
				.filter(o -> status.equals(o.getOrderStatus()))
				.map(this::convertToDto)
				.collect(Collectors.toList());
	}
	
	// New method to get B2B orders (Kaccha bill type only)
	public List<OrderDisplayDto> getB2BOrders() {
		// Filter to show only orders with bill_type = 'Kaccha'
		List<Order> orders = orderRepository.findByBillTypeOrderByCreatedAtDesc("Kaccha");
		
		return orders.stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());
	}
	
	// New method to get B2B order statistics (Kaccha bill type only)
	public OrderStatistics getB2BOrderStatistics() {
		// Filter to show only orders with bill_type = 'Kaccha'
		List<Order> allOrders = orderRepository.findByBillTypeOrderByCreatedAtDesc("Kaccha");
		
		long totalOrders = allOrders.size();
		long pendingOrders = allOrders.stream()
				.filter(o -> "Pending".equals(o.getOrderStatus()))
				.count();
		long deliveredOrders = allOrders.stream()
				.filter(o -> "Delivered".equals(o.getOrderStatus()))
				.count();
		
		BigDecimal totalRevenue = allOrders.stream()
				.map(Order::getTotal)
				.filter(total -> total != null)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		
		return new OrderStatistics(totalOrders, pendingOrders, deliveredOrders, totalRevenue);
	}
    
    public OrderDisplayDto getOrderById(Long id) {
        Order order = orderRepository.findById(id).orElse(null);
        return order != null ? convertToDto(order) : null;
    }
    
    @Transactional
    public boolean updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            String oldStatus = order.getOrderStatus();
            
            // Check if order can be modified based on outstanding status
            if (!outstandingService.canModifyOrder(orderId)) {
                throw new RuntimeException("Cannot modify order that has been fully settled");
            }
            
            // Handle stock management based on status change
            if ("Cancelled".equals(newStatus) && !"Cancelled".equals(oldStatus)) {
                // Order is being cancelled - handle outstanding and ledger reversal
                try {
                    outstandingService.handleOrderCancellation(order);
                } catch (Exception e) {
                    throw new RuntimeException("Cannot cancel order: " + e.getMessage());
                }
                // Restore stock quantities
                restoreStockQuantities(order);
            } else if (!"Cancelled".equals(newStatus) && "Cancelled".equals(oldStatus)) {
                // Order is being uncancelled - reduce stock quantities again
                reduceStockQuantities(order);
                // Note: Outstanding items are not recreated when uncancelling
                // This would require recreating the original outstanding item
            }
            
            order.setOrderStatus(newStatus);
            orderRepository.save(order);
            
            // If order is being confirmed and it's a B2B order (Kaccha), create outstanding item and customer ledger entry
            if ("Confirmed".equals(newStatus) && "Kaccha".equals(order.getBillType()) && 
                order.getTotal() != null && order.getTotal().compareTo(BigDecimal.ZERO) > 0) {
                try {
                    // Check if outstanding item already exists
                    List<com.brsons.model.Outstanding> existingOutstanding = outstandingService.getOutstandingRepository()
                        .findByReferenceTypeAndReferenceId("ORDER", order.getId());
                    
                    if (existingOutstanding.isEmpty()) {
                        System.out.println("Creating outstanding item for newly confirmed order ID: " + order.getId());
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
                        outstandingService.getOutstandingRepository().save(outstanding);
                        System.out.println("Created outstanding item for newly confirmed order ID: " + order.getId());
                    }
                    
                    // Also create customer ledger entry for B2B orders
                    try {
                        com.brsons.model.CustomerLedger customerLedger = customerLedgerService.findOrCreateCustomerLedger(
                            order.getName(), 
                            order.getUserPhone(), 
                            null // Order doesn't have email field
                        );
                        
                        // Check if customer ledger entry already exists
                        List<com.brsons.model.CustomerLedgerEntry> existingEntries = customerLedgerService.getCustomerLedgerEntryRepository()
                            .findByReferenceTypeAndReferenceId("ORDER", order.getId());
                        
                        if (existingEntries.isEmpty()) {
                            customerLedgerService.addInvoiceEntry(customerLedger, order, order.getTotal());
                            System.out.println("Created customer ledger entry for newly confirmed order ID: " + order.getId());
                        }
                    } catch (Exception e) {
                        System.err.println("Error creating customer ledger entry for confirmed order ID " + order.getId() + ": " + e.getMessage());
                    }
                    
                    // Create voucher entry for B2B order confirmation
                    try {
                        createVoucherEntryForB2BOrder(order);
                        System.out.println("Created voucher entry for confirmed B2B order ID: " + order.getId());
                    } catch (Exception e) {
                        System.err.println("Error creating voucher entry for confirmed order ID " + order.getId() + ": " + e.getMessage());
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error creating outstanding item for confirmed order ID " + order.getId() + ": " + e.getMessage());
                }
            }
            
            // If order is being confirmed and it's a Retail order (Pakka), create voucher entry
            if ("Confirmed".equals(newStatus) && "Pakka".equals(order.getBillType()) && 
                order.getTotal() != null && order.getTotal().compareTo(BigDecimal.ZERO) > 0) {
                try {
                    createVoucherEntryForRetailOrder(order);
                    System.out.println("Created voucher entry for confirmed Retail order ID: " + order.getId());
                } catch (Exception e) {
                    System.err.println("Error creating voucher entry for confirmed Retail order ID " + order.getId() + ": " + e.getMessage());
                }
            }
            
            return true;
        }
        return false;
    }
    
    private void restoreStockQuantities(Order order) {
        List<OrderItem> orderItems = orderItemRepository.findByOrder(order);
        for (OrderItem item : orderItems) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product != null) {
                // Restore the quantity that was ordered
                int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
                int restoredStock = currentStock + item.getQuantity();
                product.setStockQuantity(restoredStock);
                
                // Update product status if it was "Out of Stock" and now has stock
                if ("Out of Stock".equals(product.getStatus()) && restoredStock > 0) {
                    product.setStatus("Active");
                }
                
                // Save updated product
                productRepository.save(product);
            }
        }
    }
    
    private void reduceStockQuantities(Order order) {
        List<OrderItem> orderItems = orderItemRepository.findByOrder(order);
        for (OrderItem item : orderItems) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product != null) {
                // Reduce stock quantity
                int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
                int newStockQuantity = currentStock - item.getQuantity();
                
                // Ensure stock doesn't go negative
                if (newStockQuantity >= 0) {
                    product.setStockQuantity(newStockQuantity);
                    
                    // Update product status to "Out of Stock" if stock becomes 0
                    if (newStockQuantity <= 0) {
                        product.setStatus("Out of Stock");
                    }
                    
                    // Save updated product
                    productRepository.save(product);
                }
            }
        }
    }
    
    	// New method to get order statistics
	public OrderStatistics getOrderStatistics() {
		// Filter to show only orders with bill_type = 'Pakka'
		List<Order> allOrders = orderRepository.findByBillTypeOrderByCreatedAtDesc("Pakka");
		
		long totalOrders = allOrders.size();
		long pendingOrders = allOrders.stream()
				.filter(o -> "Pending".equals(o.getOrderStatus()))
				.count();
		long deliveredOrders = allOrders.stream()
				.filter(o -> "Delivered".equals(o.getOrderStatus()))
				.count();
		
		BigDecimal totalRevenue = allOrders.stream()
				.map(Order::getTotal)
				.filter(total -> total != null)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		
		return new OrderStatistics(totalOrders, pendingOrders, deliveredOrders, totalRevenue);
	}
    
    private OrderDisplayDto convertToDto(Order order) {
        return new OrderDisplayDto(
            order.getId(),
            order.getInvoiceNumber(),
            order.getCreatedAt(),
            order.getName(),
            order.getUserPhone(),
            order.getState(),
            order.getBillType(),
            order.getTotal(),
            order.getOrderStatus()
        );
    }
    
    // Inner class for statistics
    public static class OrderStatistics {
        private final long totalOrders;
        private final long pendingOrders;
        private final long deliveredOrders;
        private final BigDecimal totalRevenue;
        
        public OrderStatistics(long totalOrders, long pendingOrders, long deliveredOrders, BigDecimal totalRevenue) {
            this.totalOrders = totalOrders;
            this.pendingOrders = pendingOrders;
            this.deliveredOrders = deliveredOrders;
            this.totalRevenue = totalRevenue;
        }
        
        public long getTotalOrders() { return totalOrders; }
        public long getPendingOrders() { return pendingOrders; }
        public long getDeliveredOrders() { return deliveredOrders; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
    }
    
    /**
     * Create voucher entry for B2B order confirmation
     * Debit: Accounts Receivable/Debtors (1001.01)
     * Credit: Sales (3001)
     */
    private void createVoucherEntryForB2BOrder(Order order) {
        try {
            // Find the required accounts
            Account accountsReceivable = accountRepository.findById(7L).orElse(null);;
            Account salesAccount = accountRepository.findByCode("3001");
            
            if (accountsReceivable == null) {
                System.err.println("Accounts Receivable account (1001.01) not found. Creating it...");
                accountsReceivable = createAccountIfNotExists("1001.01", "Accounts Receivable", "ASSET", "Money owed by customers");
            }
            
            if (salesAccount == null) {
                System.err.println("Sales account (3001) not found. Creating it...");
                salesAccount = createAccountIfNotExists("3001", "Sales", "INCOME", "Sales Revenue");
            }
            
            if (accountsReceivable == null || salesAccount == null) {
                throw new RuntimeException("Could not find or create required accounts for voucher entry");
            }
            
            // Create voucher
            Voucher voucher = new Voucher();
            voucher.setDate(LocalDate.now());
            voucher.setNarration("B2B Order Confirmation - Invoice: " + order.getInvoiceNumber());
            voucher.setType("SALES");
            Voucher savedVoucher = voucherRepository.save(voucher);
            
            // Create debit entry (Accounts Receivable)
            VoucherEntry debitEntry = new VoucherEntry();
            debitEntry.setVoucher(savedVoucher);
            debitEntry.setAccount(accountsReceivable);
            debitEntry.setDebit(order.getTotal());
            debitEntry.setCredit(BigDecimal.ZERO);
            debitEntry.setDescription("Debtors - Order #" + order.getId() + " - " + order.getName());
            voucherEntryRepository.save(debitEntry);
            
            // Create credit entry (Sales)
            VoucherEntry creditEntry = new VoucherEntry();
            creditEntry.setVoucher(savedVoucher);
            creditEntry.setAccount(salesAccount);
            creditEntry.setDebit(BigDecimal.ZERO);
            creditEntry.setCredit(order.getTotal());
            creditEntry.setDescription("Sales - Order #" + order.getId() + " - " + order.getName());
            voucherEntryRepository.save(creditEntry);
            
            System.out.println("Successfully created voucher entry for B2B order ID: " + order.getId() + 
                             " - Debit: " + accountsReceivable.getName() + " (" + order.getTotal() + 
                             "), Credit: " + salesAccount.getName() + " (" + order.getTotal() + ")");
            
        } catch (Exception e) {
            System.err.println("Error creating voucher entry for B2B order ID " + order.getId() + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Create voucher entry for Retail order confirmation
     * Debit: Bank Account (1001.02)
     * Credit: Sales (3001)
     */
    private void createVoucherEntryForRetailOrder(Order order) {
        try {
            // Find the required accounts
            Account bankAccount = accountRepository.findById(6L).orElse(null);
            Account salesAccount = accountRepository.findByCode("3001");
            
            if (bankAccount == null) {
                System.err.println("Bank Account (1001.02) not found. Creating it...");
                bankAccount = createAccountIfNotExists("1001.02", "Bank Account", "ASSET", "Bank Account");
            }
            
            if (salesAccount == null) {
                System.err.println("Sales account (3001) not found. Creating it...");
                salesAccount = createAccountIfNotExists("3001", "Sales", "INCOME", "Sales Revenue");
            }
            
            if (bankAccount == null || salesAccount == null) {
                throw new RuntimeException("Could not find or create required accounts for retail order voucher entry");
            }
            
            // Create voucher
            Voucher voucher = new Voucher();
            voucher.setDate(LocalDate.now());
            voucher.setNarration("Retail Order Confirmation - Invoice: " + order.getInvoiceNumber());
            voucher.setType("SALES");
            Voucher savedVoucher = voucherRepository.save(voucher);
            
            // Create debit entry (Bank Account)
            VoucherEntry debitEntry = new VoucherEntry();
            debitEntry.setVoucher(savedVoucher);
            debitEntry.setAccount(bankAccount);
            debitEntry.setDebit(order.getTotal());
            debitEntry.setCredit(BigDecimal.ZERO);
            debitEntry.setDescription("Bank Account - Order #" + order.getId() + " - " + order.getName());
            voucherEntryRepository.save(debitEntry);
            
            // Create credit entry (Sales)
            VoucherEntry creditEntry = new VoucherEntry();
            creditEntry.setVoucher(savedVoucher);
            creditEntry.setAccount(salesAccount);
            creditEntry.setDebit(BigDecimal.ZERO);
            creditEntry.setCredit(order.getTotal());
            creditEntry.setDescription("Sales - Order #" + order.getId() + " - " + order.getName());
            voucherEntryRepository.save(creditEntry);
            
            System.out.println("Successfully created voucher entry for Retail order ID: " + order.getId() + 
                             " - Debit: " + bankAccount.getName() + " (" + order.getTotal() + 
                             "), Credit: " + salesAccount.getName() + " (" + order.getTotal() + ")");
            
        } catch (Exception e) {
            System.err.println("Error creating voucher entry for Retail order ID " + order.getId() + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Create account if it doesn't exist
     */
    private Account createAccountIfNotExists(String code, String name, String type, String description) {
        try {
            // Try to find parent account based on code
            Account parent = null;
            if (code.startsWith("1001")) {
                parent = accountRepository.findByCode("1001");
                if (parent == null) {
                    // Create parent account first
                    parent = new Account();
                    parent.setCode("1001");
                    parent.setName("Current Assets");
                    parent.setType("ASSET");
                    parent.setDescription("Current Assets");
                    parent.setActive(true);
                    parent = accountRepository.save(parent);
                }
            } else if (code.startsWith("3001")) {
                parent = accountRepository.findByCode("3000");
                if (parent == null) {
                    // Create parent account first
                    parent = new Account();
                    parent.setCode("3000");
                    parent.setName("INCOME");
                    parent.setType("INCOME");
                    parent.setDescription("All Income");
                    parent.setActive(true);
                    parent = accountRepository.save(parent);
                }
            }
            
            Account account = new Account();
            account.setCode(code);
            account.setName(name);
            account.setType(type);
            account.setDescription(description);
            account.setActive(true);
            account.setParent(parent);
            
            return accountRepository.save(account);
        } catch (Exception e) {
            System.err.println("Error creating account " + code + ": " + e.getMessage());
            return null;
        }
    }
}
