package com.brsons.service;

import com.brsons.dto.OrderDisplayDto;
import com.brsons.model.Order;
import com.brsons.repository.OrderRepository;
import com.brsons.repository.OrderItemRepository;
import com.brsons.repository.ProductRepository;
import com.brsons.model.OrderItem;
import com.brsons.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    
    public List<OrderDisplayDto> getAllOrders() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        
        return orders.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<OrderDisplayDto> getOrdersByStatus(String status) {
        List<Order> orders = orderRepository.findByOrderStatusOrderByCreatedAtDesc(status);
        
        return orders.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public OrderDisplayDto getOrderById(Long id) {
        Order order = orderRepository.findById(id).orElse(null);
        return order != null ? convertToDto(order) : null;
    }
    
    public boolean updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            String oldStatus = order.getOrderStatus();
            
            // Handle stock management based on status change
            if ("Cancelled".equals(newStatus) && !"Cancelled".equals(oldStatus)) {
                // Order is being cancelled - restore stock quantities
                restoreStockQuantities(order);
            } else if (!"Cancelled".equals(newStatus) && "Cancelled".equals(oldStatus)) {
                // Order is being uncancelled - reduce stock quantities again
                reduceStockQuantities(order);
            }
            
            order.setOrderStatus(newStatus);
            orderRepository.save(order);
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
        List<Order> allOrders = orderRepository.findAll();
        
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
}
