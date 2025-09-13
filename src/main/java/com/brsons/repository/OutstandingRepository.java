package com.brsons.repository;

import com.brsons.model.Outstanding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutstandingRepository extends JpaRepository<Outstanding, Long> {
    
    // Find by type
    List<Outstanding> findByType(Outstanding.OutstandingType type);
    
    // Find by status
    List<Outstanding> findByStatus(Outstanding.OutstandingStatus status);
    
    // Find by type and status
    List<Outstanding> findByTypeAndStatus(Outstanding.OutstandingType type, Outstanding.OutstandingStatus status);
    
    // Find by type and order type
    List<Outstanding> findByTypeAndOrderType(Outstanding.OutstandingType type, String orderType);
    
    // Find overdue items
    List<Outstanding> findByStatusAndDueDateBefore(Outstanding.OutstandingStatus status, LocalDateTime date);
    
    // Find by customer/supplier name
    List<Outstanding> findByCustomerSupplierNameContainingIgnoreCase(String name);
    
    // Find by reference type and ID
    List<Outstanding> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
    
    // Find by reference number
    List<Outstanding> findByReferenceNumber(String referenceNumber);
    
    // Find critical overdue items (more than 30 days)
    @Query("SELECT o FROM Outstanding o WHERE o.daysOverdue > 30 AND o.status != 'SETTLED' ORDER BY o.daysOverdue DESC")
    List<Outstanding> findCriticalOverdueItems();
    
    // Find items due within next X days
    @Query("SELECT o FROM Outstanding o WHERE o.dueDate BETWEEN :startDate AND :endDate AND o.status != 'SETTLED' ORDER BY o.dueDate ASC")
    List<Outstanding> findItemsDueBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Find items due today
    @Query("SELECT o FROM Outstanding o " +
    	       "WHERE DATE(o.dueDate) = :today " +
    	       "AND o.status <> 'SETTLED' " +
    	       "ORDER BY o.amount DESC")
    	List<Outstanding> findItemsDueToday(@Param("today") LocalDate today);
    
    // Find items due this week
    @Query("SELECT o FROM Outstanding o WHERE o.dueDate BETWEEN :weekStart AND :weekEnd AND o.status != 'SETTLED' ORDER BY o.dueDate ASC")
    List<Outstanding> findItemsDueThisWeek(@Param("weekStart") LocalDateTime weekStart, @Param("weekEnd") LocalDateTime weekEnd);
    
    // Find items due this month
    @Query("SELECT o FROM Outstanding o WHERE o.dueDate BETWEEN :monthStart AND :monthEnd AND o.status != 'SETTLED' ORDER BY o.dueDate ASC")
    List<Outstanding> findItemsDueThisMonth(@Param("monthStart") LocalDateTime monthStart, @Param("monthEnd") LocalDateTime monthEnd);
    
    // Calculate total outstanding amount by type
    @Query("SELECT SUM(o.amount) FROM Outstanding o WHERE o.type = :type AND o.status != 'SETTLED'")
    BigDecimal calculateTotalOutstandingByType(@Param("type") Outstanding.OutstandingType type);
    
    // Calculate total overdue amount
    @Query("SELECT SUM(o.amount) FROM Outstanding o WHERE o.status = 'OVERDUE'")
    BigDecimal calculateTotalOverdueAmount();
    
    // Calculate total receivable (money owed to us)
    @Query("SELECT SUM(o.amount) FROM Outstanding o WHERE o.type = 'INVOICE_RECEIVABLE' AND o.status != 'SETTLED'")
    BigDecimal calculateTotalReceivable();
    
    // Calculate total payable (money we owe)
    @Query("SELECT SUM(o.amount) FROM Outstanding o WHERE o.type = 'INVOICE_PAYABLE' AND o.status != 'SETTLED'")
    BigDecimal calculateTotalPayable();
    
    // Find items by amount range
    @Query("SELECT o FROM Outstanding o WHERE o.amount BETWEEN :minAmount AND :maxAmount AND o.status != 'SETTLED' ORDER BY o.amount DESC")
    List<Outstanding> findByAmountRange(@Param("minAmount") BigDecimal minAmount, @Param("maxAmount") BigDecimal maxAmount);
    
    // Find items by days overdue range
    @Query("SELECT o FROM Outstanding o WHERE o.daysOverdue BETWEEN :minDays AND :maxDays AND o.status != 'SETTLED' ORDER BY o.daysOverdue DESC")
    List<Outstanding> findByDaysOverdueRange(@Param("minDays") Integer minDays, @Param("maxDays") Integer maxDays);
    
    // Count items by status
    @Query("SELECT COUNT(o) FROM Outstanding o WHERE o.status = :status")
    Long countByStatus(@Param("status") Outstanding.OutstandingStatus status);
    
    // Count overdue items
    @Query("SELECT COUNT(o) FROM Outstanding o WHERE o.status = 'OVERDUE'")
    Long countOverdueItems();
    
    // Count critical overdue items
    @Query("SELECT COUNT(o) FROM Outstanding o WHERE o.daysOverdue > 30 AND o.status != 'SETTLED'")
    Long countCriticalOverdueItems();
    
    // Find all non-settled B2B (Kaccha) receivables for a customer, oldest first
    @Query("SELECT o FROM Outstanding o WHERE o.contactInfo = :contactInfo AND o.type = 'INVOICE_RECEIVABLE' AND o.orderType = 'Kaccha' AND o.status IN ('PENDING', 'OVERDUE', 'PARTIALLY_PAID') ORDER BY o.createdAt ASC")
    List<Outstanding> findB2BReceivablesForCustomerOldestFirst(@Param("contactInfo") String contactInfo);
    
    // Find all non-settled payables for a supplier, oldest first
    @Query("SELECT o FROM Outstanding o WHERE o.contactInfo = :contactInfo AND (o.type = 'INVOICE_PAYABLE' OR o.type = 'PURCHASE_ORDER') AND o.status IN ('PENDING', 'OVERDUE', 'PARTIALLY_PAID') ORDER BY o.createdAt ASC")
    List<Outstanding> findPayablesForSupplierOldestFirst(@Param("contactInfo") String contactInfo);
}
