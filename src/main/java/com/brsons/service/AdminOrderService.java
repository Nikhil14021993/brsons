package com.brsons.service;

import com.brsons.dto.OrderDisplayDto;
import com.brsons.model.Order;
import com.brsons.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminOrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
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
            order.setOrderStatus(newStatus);
            orderRepository.save(order);
            return true;
        }
        return false;
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
