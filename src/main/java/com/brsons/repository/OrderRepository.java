package com.brsons.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.brsons.model.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
