package com.brsons.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.brsons.model.Order;
import com.brsons.repository.OrderRepository;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    public OrderService(OrderRepository orderRepository) { this.orderRepository = orderRepository; }

    public List<Order> getOrdersByUserPhone(String userPhone) {
        return orderRepository.findByUserPhone(userPhone);
    }
    
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }
    
    public Order updateOrder(Order order) {
        return orderRepository.save(order);
    }
}