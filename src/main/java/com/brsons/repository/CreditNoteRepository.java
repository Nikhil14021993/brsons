package com.brsons.repository;

import com.brsons.model.CreditNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CreditNoteRepository extends JpaRepository<CreditNote, Long> {
    
    // Find by credit note number
    Optional<CreditNote> findByCreditNoteNumber(String creditNoteNumber);
    
    // Find by status
    List<CreditNote> findByStatusOrderByCreatedAtDesc(String status);
    
    // Find by supplier
    List<CreditNote> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);
    
    // Find by purchase order
    List<CreditNote> findByPurchaseOrderIdOrderByCreatedAtDesc(Long purchaseOrderId);
    
    // Find by date range
    @Query("SELECT cn FROM CreditNote cn WHERE cn.creditDate BETWEEN :startDate AND :endDate ORDER BY cn.creditDate DESC")
    List<CreditNote> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Find by status and date range
    @Query("SELECT cn FROM CreditNote cn WHERE cn.status = :status AND cn.creditDate BETWEEN :startDate AND :endDate ORDER BY cn.creditDate DESC")
    List<CreditNote> findByStatusAndDateRange(@Param("status") String status, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Find all with purchase order and supplier details
    @Query("SELECT cn FROM CreditNote cn LEFT JOIN FETCH cn.purchaseOrder po LEFT JOIN FETCH cn.supplier s ORDER BY cn.createdAt DESC")
    List<CreditNote> findAllWithPurchaseOrderAndSupplier();
    
    // Count by status
    long countByStatus(String status);
}
