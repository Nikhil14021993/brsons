package com.brsons.repository;

import com.brsons.model.SupplierCreditNote;
import com.brsons.model.Supplier;
import com.brsons.model.PurchaseOrder;
import com.brsons.model.GoodsReceivedNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierCreditNoteRepository extends JpaRepository<SupplierCreditNote, Long> {
    
    // Find by credit note number
    Optional<SupplierCreditNote> findByCreditNoteNumber(String creditNoteNumber);
    
    // Find by supplier
    List<SupplierCreditNote> findBySupplier(Supplier supplier);
    
    // Find by supplier ID
    List<SupplierCreditNote> findBySupplierId(Long supplierId);
    
    // Find by purchase order
    List<SupplierCreditNote> findByPurchaseOrder(PurchaseOrder purchaseOrder);
    
    // Find by purchase order ID
    List<SupplierCreditNote> findByPurchaseOrderId(Long purchaseOrderId);
    
    // Find by GRN
    List<SupplierCreditNote> findByGrn(GoodsReceivedNote grn);
    
    // Find by GRN ID
    List<SupplierCreditNote> findByGrnId(Long grnId);
    
    // Find by type
    List<SupplierCreditNote> findByType(SupplierCreditNote.CreditNoteType type);
    
    // Find by status
    List<SupplierCreditNote> findByStatus(SupplierCreditNote.CreditNoteStatus status);
    
    // Find by multiple statuses
    List<SupplierCreditNote> findByStatusIn(List<SupplierCreditNote.CreditNoteStatus> statuses);
    
    // Find by created by
    List<SupplierCreditNote> findByCreatedBy(String createdBy);
    
    // Find by approved by
    List<SupplierCreditNote> findByApprovedBy(String approvedBy);
    
    // Find by credit note date range
    List<SupplierCreditNote> findByCreditNoteDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find by created date range
    List<SupplierCreditNote> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find by approved date range
    List<SupplierCreditNote> findByApprovedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find credit notes by supplier and status
    List<SupplierCreditNote> findBySupplierAndStatus(Supplier supplier, SupplierCreditNote.CreditNoteStatus status);
    
    // Find credit notes by supplier ID and status
    List<SupplierCreditNote> findBySupplierIdAndStatus(Long supplierId, SupplierCreditNote.CreditNoteStatus status);
    
    // Find credit notes by supplier and type
    List<SupplierCreditNote> findBySupplierAndType(Supplier supplier, SupplierCreditNote.CreditNoteType type);
    
    // Find credit notes by supplier ID and type
    List<SupplierCreditNote> findBySupplierIdAndType(Long supplierId, SupplierCreditNote.CreditNoteType type);
    
    // Find credit notes with total amount above threshold
    @Query("SELECT scn FROM SupplierCreditNote scn WHERE scn.totalAmount > :minAmount")
    List<SupplierCreditNote> findByTotalAmountAbove(@Param("minAmount") java.math.BigDecimal minAmount);
    
    // Find credit notes with total amount below threshold
    @Query("SELECT scn FROM SupplierCreditNote scn WHERE scn.totalAmount < :maxAmount")
    List<SupplierCreditNote> findByTotalAmountBelow(@Param("maxAmount") java.math.BigDecimal maxAmount);
    
    // Find credit notes by supplier and date range
    @Query("SELECT scn FROM SupplierCreditNote scn WHERE scn.supplier.id = :supplierId AND scn.creditNoteDate BETWEEN :startDate AND :endDate")
    List<SupplierCreditNote> findBySupplierAndDateRange(@Param("supplierId") Long supplierId, 
                                                       @Param("startDate") LocalDateTime startDate, 
                                                       @Param("endDate") LocalDateTime endDate);
    
    // Find credit notes that can be applied
    @Query("SELECT scn FROM SupplierCreditNote scn WHERE scn.status = 'APPROVED'")
    List<SupplierCreditNote> findCreditNotesReadyForApplication();
    
    // Find credit notes by reason
    List<SupplierCreditNote> findByReasonContaining(String reason);
    
    // Find credit notes by reference document
    List<SupplierCreditNote> findByReferenceDocument(String referenceDocument);
    
    // Count credit notes by status
    long countByStatus(SupplierCreditNote.CreditNoteStatus status);
    
    // Count credit notes by supplier
    long countBySupplier(Supplier supplier);
    
    // Count credit notes by type
    long countByType(SupplierCreditNote.CreditNoteType type);
    
    // Find credit notes with items
    @Query("SELECT DISTINCT scn FROM SupplierCreditNote scn LEFT JOIN FETCH scn.creditNoteItems WHERE scn.id = :creditNoteId")
    Optional<SupplierCreditNote> findByIdWithItems(@Param("creditNoteId") Long creditNoteId);
    
    // Find credit notes with items and supplier
    @Query("SELECT DISTINCT scn FROM SupplierCreditNote scn LEFT JOIN FETCH scn.creditNoteItems LEFT JOIN FETCH scn.supplier WHERE scn.id = :creditNoteId")
    Optional<SupplierCreditNote> findByIdWithItemsAndSupplier(@Param("creditNoteId") Long creditNoteId);
    
    // Find all credit notes with items and supplier
    @Query("SELECT DISTINCT scn FROM SupplierCreditNote scn LEFT JOIN FETCH scn.creditNoteItems LEFT JOIN FETCH scn.supplier")
    List<SupplierCreditNote> findAllWithItemsAndSupplier();
    
    // Find credit notes by supplier with items
    @Query("SELECT DISTINCT scn FROM SupplierCreditNote scn LEFT JOIN FETCH scn.creditNoteItems WHERE scn.supplier.id = :supplierId")
    List<SupplierCreditNote> findBySupplierIdWithItems(@Param("supplierId") Long supplierId);
    
    // Search credit notes by multiple criteria
    @Query("SELECT scn FROM SupplierCreditNote scn WHERE " +
           "scn.creditNoteNumber LIKE CONCAT('%', :query, '%') OR " +
           "scn.supplier.companyName LIKE CONCAT('%', :query, '%') OR " +
           "scn.reason LIKE CONCAT('%', :query, '%') OR " +
           "scn.notes LIKE CONCAT('%', :query, '%')")
    List<SupplierCreditNote> searchCreditNotes(@Param("query") String query);
    
    // Find credit notes by payment terms
    @Query("SELECT scn FROM SupplierCreditNote scn JOIN scn.supplier s WHERE s.paymentTerms = :paymentTerms")
    List<SupplierCreditNote> findByPaymentTerms(@Param("paymentTerms") String paymentTerms);
    
    // Find credit notes with high amounts (above average)
    @Query("SELECT scn FROM SupplierCreditNote scn WHERE scn.totalAmount > (SELECT AVG(scn2.totalAmount) FROM SupplierCreditNote scn2)")
    List<SupplierCreditNote> findCreditNotesAboveAverage();
    
    // Find credit notes by supplier rating
    @Query("SELECT scn FROM SupplierCreditNote scn JOIN scn.supplier s WHERE s.rating >= :minRating")
    List<SupplierCreditNote> findBySupplierRatingAbove(@Param("minRating") Integer minRating);
    
    // Find credit notes by supplier rating below
    @Query("SELECT scn FROM SupplierCreditNote scn JOIN scn.supplier s WHERE s.rating < :maxRating")
    List<SupplierCreditNote> findBySupplierRatingBelow(@Param("maxRating") Integer maxRating);
}
