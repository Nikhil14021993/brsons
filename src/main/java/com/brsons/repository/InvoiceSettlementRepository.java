package com.brsons.repository;

import com.brsons.model.InvoiceSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceSettlementRepository extends JpaRepository<InvoiceSettlement, Long> {
    
    List<InvoiceSettlement> findByOrderId(Long orderId);
    
    List<InvoiceSettlement> findByCustomerPhoneOrderBySettlementDateDesc(String customerPhone);
    
    List<InvoiceSettlement> findByPaymentEntryId(Long paymentEntryId);
    
    @Query("SELECT SUM(is.settlementAmount) FROM InvoiceSettlement is WHERE is.orderId = :orderId")
    Double getTotalSettledAmountByOrderId(@Param("orderId") Long orderId);
    
    @Query("SELECT is FROM InvoiceSettlement is WHERE is.customerPhone = :customerPhone ORDER BY is.settlementDate DESC")
    List<InvoiceSettlement> findSettlementsByCustomer(@Param("customerPhone") String customerPhone);
}
