package com.brsons.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.brsons.model.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
	List<Order> findByUserPhone(String userPhone);
	Optional<Order> findTopByUserPhoneOrderByIdDesc(String userPhone);
	List<Order> findByUserPhoneOrderByCreatedAtDesc(String userPhone);
	
	@Query("SELECT o FROM Order o WHERE DATE(o.createdAt) = :date")
	List<Order> findOrdersByDate(@Param("date") LocalDate date);

	// New methods for admin order management
	List<Order> findAllByOrderByCreatedAtDesc();
	
	List<Order> findByOrderStatusOrderByCreatedAtDesc(String orderStatus);
	
	@Query("SELECT o FROM Order o WHERE o.orderStatus = :status ORDER BY o.createdAt DESC")
	List<Order> findOrdersByStatusOrderByCreatedAtDesc(@Param("status") String status);
	
	// Filter orders by bill type (Pakka only)
	List<Order> findByBillTypeOrderByCreatedAtDesc(String billType);
	
}
