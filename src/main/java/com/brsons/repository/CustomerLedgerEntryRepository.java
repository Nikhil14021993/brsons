package com.brsons.repository;

import com.brsons.model.CustomerLedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CustomerLedgerEntryRepository extends JpaRepository<CustomerLedgerEntry, Long> {
    
    /**
     * Find all entries for a specific customer ledger
     */
    List<CustomerLedgerEntry> findByCustomerLedgerIdOrderByEntryDateDesc(Long customerLedgerId);
    
    /**
     * Find entries for a specific customer ledger with pagination
     */
    Page<CustomerLedgerEntry> findByCustomerLedgerIdOrderByEntryDateDesc(Long customerLedgerId, Pageable pageable);
    
    /**
     * Find entries by reference type and reference ID
     */
    List<CustomerLedgerEntry> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
    
    /**
     * Find entries by reference type
     */
    List<CustomerLedgerEntry> findByReferenceType(String referenceType);
    
    /**
     * Find entries within a date range for a specific customer ledger
     */
    @Query("SELECT cle FROM CustomerLedgerEntry cle WHERE cle.customerLedger.id = :ledgerId AND cle.entryDate BETWEEN :startDate AND :endDate ORDER BY cle.entryDate DESC")
    List<CustomerLedgerEntry> findByCustomerLedgerAndDateRange(
        @Param("ledgerId") Long ledgerId, 
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Find entries by payment method
     */
    List<CustomerLedgerEntry> findByPaymentMethod(String paymentMethod);
    
    /**
     * Count entries for a specific customer ledger
     */
    Long countByCustomerLedgerId(Long customerLedgerId);
    
    /**
     * Find the latest entry for a customer ledger
     */
    @Query("SELECT cle FROM CustomerLedgerEntry cle WHERE cle.customerLedger.id = :ledgerId ORDER BY cle.entryDate DESC LIMIT 1")
    CustomerLedgerEntry findLatestEntryByCustomerLedgerId(@Param("ledgerId") Long ledgerId);
    
    /**
     * Find entries with specific particulars
     */
    List<CustomerLedgerEntry> findByParticularsContainingIgnoreCase(String particulars);
}
