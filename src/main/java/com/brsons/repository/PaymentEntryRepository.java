package com.brsons.repository;

import com.brsons.model.PaymentEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentEntryRepository extends JpaRepository<PaymentEntry, Long> {
    
    List<PaymentEntry> findByCustomerPhoneOrderByPaymentDateDesc(String customerPhone);
    
    @Query("SELECT pe FROM PaymentEntry pe WHERE pe.remainingAmount > 0 ORDER BY pe.paymentDate ASC")
    List<PaymentEntry> findUnallocatedPayments();
    
    @Query("SELECT pe FROM PaymentEntry pe WHERE pe.customerPhone = :customerPhone AND pe.remainingAmount > 0 ORDER BY pe.paymentDate ASC")
    List<PaymentEntry> findUnallocatedPaymentsByCustomer(@Param("customerPhone") String customerPhone);
    
    @Query("SELECT pe FROM PaymentEntry pe WHERE pe.paymentDate BETWEEN :startDate AND :endDate ORDER BY pe.paymentDate DESC")
    List<PaymentEntry> findPaymentsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(pe.paymentAmount) FROM PaymentEntry pe WHERE pe.customerPhone = :customerPhone")
    Double getTotalPaymentsByCustomer(@Param("customerPhone") String customerPhone);
}
