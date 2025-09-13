package com.brsons.repository;

import com.brsons.model.SupplierLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SupplierLedgerEntryRepository extends JpaRepository<SupplierLedgerEntry, Long> {
    
    /**
     * Find all entries for a specific supplier ledger
     */
    List<SupplierLedgerEntry> findBySupplierLedgerIdOrderByEntryDateDesc(Long supplierLedgerId);
    
    /**
     * Find entries by reference type and reference ID
     */
    List<SupplierLedgerEntry> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
    
    /**
     * Find entries by reference type and reference number
     */
    List<SupplierLedgerEntry> findByReferenceTypeAndReferenceNumber(String referenceType, String referenceNumber);
    
    /**
     * Find entries within a date range
     */
    @Query("SELECT sle FROM SupplierLedgerEntry sle WHERE sle.entryDate BETWEEN :startDate AND :endDate ORDER BY sle.entryDate DESC")
    List<SupplierLedgerEntry> findByEntryDateBetween(@Param("startDate") LocalDateTime startDate, 
                                                     @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find entries for a specific supplier within date range
     */
    @Query("SELECT sle FROM SupplierLedgerEntry sle WHERE sle.supplierLedger.id = :supplierLedgerId " +
           "AND sle.entryDate BETWEEN :startDate AND :endDate ORDER BY sle.entryDate DESC")
    List<SupplierLedgerEntry> findBySupplierLedgerIdAndEntryDateBetween(@Param("supplierLedgerId") Long supplierLedgerId,
                                                                        @Param("startDate") LocalDateTime startDate,
                                                                        @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find payment entries for a specific supplier
     */
    @Query("SELECT sle FROM SupplierLedgerEntry sle WHERE sle.supplierLedger.id = :supplierLedgerId " +
           "AND sle.referenceType = 'PAYMENT' ORDER BY sle.entryDate DESC")
    List<SupplierLedgerEntry> findPaymentEntriesBySupplierLedgerId(@Param("supplierLedgerId") Long supplierLedgerId);
    
    /**
     * Find purchase order entries for a specific supplier
     */
    @Query("SELECT sle FROM SupplierLedgerEntry sle WHERE sle.supplierLedger.id = :supplierLedgerId " +
           "AND sle.referenceType = 'PURCHASE_ORDER' ORDER BY sle.entryDate DESC")
    List<SupplierLedgerEntry> findPurchaseOrderEntriesBySupplierLedgerId(@Param("supplierLedgerId") Long supplierLedgerId);
    
    /**
     * Find credit note entries for a specific supplier
     */
    @Query("SELECT sle FROM SupplierLedgerEntry sle WHERE sle.supplierLedger.id = :supplierLedgerId " +
           "AND sle.referenceType = 'CREDIT_NOTE' ORDER BY sle.entryDate DESC")
    List<SupplierLedgerEntry> findCreditNoteEntriesBySupplierLedgerId(@Param("supplierLedgerId") Long supplierLedgerId);
    
    /**
     * Calculate total debits for a supplier ledger
     */
    @Query("SELECT COALESCE(SUM(sle.debitAmount), 0) FROM SupplierLedgerEntry sle WHERE sle.supplierLedger.id = :supplierLedgerId")
    Double calculateTotalDebitsBySupplierLedgerId(@Param("supplierLedgerId") Long supplierLedgerId);
    
    /**
     * Calculate total credits for a supplier ledger
     */
    @Query("SELECT COALESCE(SUM(sle.creditAmount), 0) FROM SupplierLedgerEntry sle WHERE sle.supplierLedger.id = :supplierLedgerId")
    Double calculateTotalCreditsBySupplierLedgerId(@Param("supplierLedgerId") Long supplierLedgerId);
    
    /**
     * Find entries by payment method
     */
    List<SupplierLedgerEntry> findByPaymentMethod(String paymentMethod);
    
    /**
     * Find entries by payment reference
     */
    List<SupplierLedgerEntry> findByPaymentReference(String paymentReference);
}
