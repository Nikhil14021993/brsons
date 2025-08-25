package com.brsons.repository;

import com.brsons.model.GoodsReceivedNote;
import com.brsons.model.PurchaseOrder;
import com.brsons.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GRNRepository extends JpaRepository<GoodsReceivedNote, Long> {
    
    // Find by GRN number
    Optional<GoodsReceivedNote> findByGrnNumber(String grnNumber);
    
    // Find by purchase order
    List<GoodsReceivedNote> findByPurchaseOrder(PurchaseOrder purchaseOrder);
    
    // Find by purchase order ID
    List<GoodsReceivedNote> findByPurchaseOrderId(Long purchaseOrderId);
    
    // Find by supplier
    List<GoodsReceivedNote> findBySupplier(Supplier supplier);
    
    // Find by supplier ID
    List<GoodsReceivedNote> findBySupplierId(Long supplierId);
    
    // Find by status
    List<GoodsReceivedNote> findByStatus(GoodsReceivedNote.GRNStatus status);
    
    // Find by multiple statuses
    List<GoodsReceivedNote> findByStatusIn(List<GoodsReceivedNote.GRNStatus> statuses);
    
    // Find by received by
    List<GoodsReceivedNote> findByReceivedBy(String receivedBy);
    
    // Find by inspected by
    List<GoodsReceivedNote> findByInspectedBy(String inspectedBy);
    
    // Find by received date range
    List<GoodsReceivedNote> findByReceivedDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find by created date range
    List<GoodsReceivedNote> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find GRNs by supplier and status
    List<GoodsReceivedNote> findBySupplierAndStatus(Supplier supplier, GoodsReceivedNote.GRNStatus status);
    
    // Find GRNs by supplier ID and status
    List<GoodsReceivedNote> findBySupplierIdAndStatus(Long supplierId, GoodsReceivedNote.GRNStatus status);
    
    // Find GRNs by purchase order and status
    List<GoodsReceivedNote> findByPurchaseOrderAndStatus(PurchaseOrder purchaseOrder, GoodsReceivedNote.GRNStatus status);
    
    // Find GRNs by purchase order ID and status
    List<GoodsReceivedNote> findByPurchaseOrderIdAndStatus(Long purchaseOrderId, GoodsReceivedNote.GRNStatus status);
    
    // Find GRNs with total amount above threshold
    @Query("SELECT grn FROM GoodsReceivedNote grn WHERE grn.totalAmount > :minAmount")
    List<GoodsReceivedNote> findByTotalAmountAbove(@Param("minAmount") java.math.BigDecimal minAmount);
    
    // Find GRNs with total amount below threshold
    @Query("SELECT grn FROM GoodsReceivedNote grn WHERE grn.totalAmount < :maxAmount")
    List<GoodsReceivedNote> findByTotalAmountBelow(@Param("maxAmount") java.math.BigDecimal maxAmount);
    
    // Find GRNs by supplier and date range
    @Query("SELECT grn FROM GoodsReceivedNote grn WHERE grn.supplier.id = :supplierId AND grn.receivedDate BETWEEN :startDate AND :endDate")
    List<GoodsReceivedNote> findBySupplierAndDateRange(@Param("supplierId") Long supplierId, 
                                                      @Param("startDate") LocalDateTime startDate, 
                                                      @Param("endDate") LocalDateTime endDate);
    
    // Find GRNs ready for approval
    @Query("SELECT grn FROM GoodsReceivedNote grn WHERE grn.status = 'INSPECTED'")
    List<GoodsReceivedNote> findGRNsReadyForApproval();
    
    // Find GRNs by delivery note number
    List<GoodsReceivedNote> findByDeliveryNoteNumber(String deliveryNoteNumber);
    
    // Find GRNs by vehicle number
    List<GoodsReceivedNote> findByVehicleNumber(String vehicleNumber);
    
    // Count GRNs by status
    long countByStatus(GoodsReceivedNote.GRNStatus status);
    
    // Count GRNs by supplier
    long countBySupplier(Supplier supplier);
    
    // Count GRNs by purchase order
    long countByPurchaseOrder(PurchaseOrder purchaseOrder);
    
    // Find GRNs with items
    @Query("SELECT DISTINCT grn FROM GoodsReceivedNote grn LEFT JOIN FETCH grn.grnItems WHERE grn.id = :grnId")
    Optional<GoodsReceivedNote> findByIdWithItems(@Param("grnId") Long grnId);
    
    // Find GRNs with items and supplier
    @Query("SELECT DISTINCT grn FROM GoodsReceivedNote grn LEFT JOIN FETCH grn.grnItems LEFT JOIN FETCH grn.supplier WHERE grn.id = :grnId")
    Optional<GoodsReceivedNote> findByIdWithItemsAndSupplier(@Param("grnId") Long grnId);
    
    
    // Find all GRNs with items and supplier
    @Query("SELECT DISTINCT grn FROM GoodsReceivedNote grn LEFT JOIN FETCH grn.grnItems LEFT JOIN FETCH grn.supplier")
    List<GoodsReceivedNote> findAllWithItemsAndSupplier();
    
    // Find GRNs by supplier with items
    @Query("SELECT DISTINCT grn FROM GoodsReceivedNote grn LEFT JOIN FETCH grn.grnItems WHERE grn.supplier.id = :supplierId")
    List<GoodsReceivedNote> findBySupplierIdWithItems(@Param("supplierId") Long supplierId);
    
    // Find GRNs by purchase order with items
    @Query("SELECT DISTINCT grn FROM GoodsReceivedNote grn LEFT JOIN FETCH grn.grnItems WHERE grn.purchaseOrder.id = :purchaseOrderId")
    List<GoodsReceivedNote> findByPurchaseOrderIdWithItems(@Param("purchaseOrderId") Long purchaseOrderId);
    
    // Search GRNs by multiple criteria
    @Query("SELECT grn FROM GoodsReceivedNote grn WHERE " +
           "grn.grnNumber LIKE CONCAT('%', :query, '%') OR " +
           "grn.supplier.companyName LIKE CONCAT('%', :query, '%') OR " +
           "grn.deliveryNoteNumber LIKE CONCAT('%', :query, '%') OR " +
           "grn.notes LIKE CONCAT('%', :query, '%')")
    List<GoodsReceivedNote> searchGRNs(@Param("query") String query);
    
    // Find GRNs with quality issues
    @Query("SELECT grn FROM GoodsReceivedNote grn WHERE grn.qualityRemarks IS NOT NULL AND grn.qualityRemarks != ''")
    List<GoodsReceivedNote> findGRNsWithQualityIssues();
    
    // Find GRNs by quality status
    @Query("SELECT grn FROM GoodsReceivedNote grn JOIN grn.grnItems item WHERE item.qualityStatus = :qualityStatus")
    List<GoodsReceivedNote> findByQualityStatus(@Param("qualityStatus") String qualityStatus);
}
