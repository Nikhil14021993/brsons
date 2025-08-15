package com.brsons.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.brsons.model.Order;
import com.brsons.model.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
	List<OrderItem> findByOrder(Order order);
	
}
