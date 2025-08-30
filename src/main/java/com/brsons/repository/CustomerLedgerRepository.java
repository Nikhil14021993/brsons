package com.brsons.repository;

import com.brsons.model.CustomerLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerLedgerRepository extends JpaRepository<CustomerLedger, Long> {
    
    /**
     * Find customer ledger by phone number
     */
    Optional<CustomerLedger> findByCustomerPhone(String customerPhone);
    
    /**
     * Find customer ledger by name (case-insensitive)
     */
    List<CustomerLedger> findByCustomerNameContainingIgnoreCase(String customerName);
    
    /**
     * Find customer ledgers by name or phone (case-insensitive)
     */
    @Query("SELECT cl FROM CustomerLedger cl WHERE " +
           "LOWER(cl.customerName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "cl.customerPhone LIKE CONCAT('%', :searchTerm, '%')")
    List<CustomerLedger> findByCustomerNameOrPhoneContaining(@Param("searchTerm") String searchTerm);
    
    /**
     * Find active customer ledgers
     */
    List<CustomerLedger> findByStatus(String status);
    
    /**
     * Find customer ledgers with outstanding balance
     */
    @Query("SELECT cl FROM CustomerLedger cl WHERE cl.currentBalance > 0 AND cl.status = 'ACTIVE'")
    List<CustomerLedger> findActiveLedgersWithOutstandingBalance();
    
    /**
     * Calculate total outstanding amount across all active ledgers
     */
    @Query("SELECT COALESCE(SUM(cl.currentBalance), 0) FROM CustomerLedger cl WHERE cl.status = 'ACTIVE'")
    BigDecimal calculateTotalOutstandingAmount();
    
    /**
     * Count active ledgers with outstanding balance
     */
    @Query("SELECT COUNT(cl) FROM CustomerLedger cl WHERE cl.currentBalance > 0 AND cl.status = 'ACTIVE'")
    Long countActiveLedgersWithOutstandingBalance();
    
    /**
     * Find ledgers by balance range
     */
    @Query("SELECT cl FROM CustomerLedger cl WHERE cl.currentBalance BETWEEN :minAmount AND :maxAmount AND cl.status = 'ACTIVE'")
    List<CustomerLedger> findByBalanceRange(@Param("minAmount") BigDecimal minAmount, @Param("maxAmount") BigDecimal maxAmount);
    
    /**
     * Find ledgers with balance greater than specified amount
     */
    @Query("SELECT cl FROM CustomerLedger cl WHERE cl.currentBalance > :amount AND cl.status = 'ACTIVE'")
    List<CustomerLedger> findByBalanceGreaterThan(@Param("amount") BigDecimal amount);
}
