package com.brsons.repository;

import com.brsons.model.SupplierLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierLedgerRepository extends JpaRepository<SupplierLedger, Long> {
    
    /**
     * Find supplier ledger by phone number
     */
    Optional<SupplierLedger> findBySupplierPhone(String supplierPhone);
    
    /**
     * Find supplier ledger by supplier code
     */
    Optional<SupplierLedger> findBySupplierCode(String supplierCode);
    
    /**
     * Find supplier ledger by name (case-insensitive)
     */
    List<SupplierLedger> findBySupplierNameContainingIgnoreCase(String supplierName);
    
    /**
     * Find supplier ledgers by name or phone (case-insensitive)
     */
    @Query("SELECT sl FROM SupplierLedger sl WHERE " +
           "LOWER(sl.supplierName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "sl.supplierPhone LIKE CONCAT('%', :searchTerm, '%') OR " +
           "sl.supplierCode LIKE CONCAT('%', :searchTerm, '%')")
    List<SupplierLedger> findBySupplierNameOrPhoneOrCodeContaining(@Param("searchTerm") String searchTerm);
    
    /**
     * Find active supplier ledgers
     */
    List<SupplierLedger> findByStatus(String status);
    
    /**
     * Find supplier ledgers with outstanding balance
     */
    @Query("SELECT sl FROM SupplierLedger sl WHERE sl.currentBalance > 0 AND sl.status = 'ACTIVE'")
    List<SupplierLedger> findActiveLedgersWithOutstandingBalance();
    
    /**
     * Calculate total outstanding amount across all active ledgers
     */
    @Query("SELECT COALESCE(SUM(sl.currentBalance), 0) FROM SupplierLedger sl WHERE sl.status = 'ACTIVE'")
    BigDecimal calculateTotalOutstandingAmount();
    
    /**
     * Count active ledgers with outstanding balance
     */
    @Query("SELECT COUNT(sl) FROM SupplierLedger sl WHERE sl.currentBalance > 0 AND sl.status = 'ACTIVE'")
    Long countActiveLedgersWithOutstandingBalance();
    
    /**
     * Find supplier ledgers with credit limit exceeded
     */
    @Query("SELECT sl FROM SupplierLedger sl WHERE sl.creditLimit > 0 AND sl.currentBalance >= sl.creditLimit AND sl.status = 'ACTIVE'")
    List<SupplierLedger> findLedgersWithExceededCreditLimit();
    
    /**
     * Find supplier ledgers with low credit (less than 20% remaining)
     */
    @Query("SELECT sl FROM SupplierLedger sl WHERE sl.creditLimit > 0 AND sl.currentBalance >= (sl.creditLimit * 0.8) AND sl.status = 'ACTIVE'")
    List<SupplierLedger> findLedgersWithLowCredit();
    
}
