package com.brsons.repository;

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
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    
    // Find by PO number
    Optional<PurchaseOrder> findByPoNumber(String poNumber);
    
    // Find by supplier
    List<PurchaseOrder> findBySupplier(Supplier supplier);
    
    // Find by supplier ID
    List<PurchaseOrder> findBySupplierId(Long supplierId);
    
    // Find by status
    List<PurchaseOrder> findByStatus(PurchaseOrder.POStatus status);
    
    // Find by multiple statuses
    List<PurchaseOrder> findByStatusIn(List<PurchaseOrder.POStatus> statuses);
    
    // Find by created by
    List<PurchaseOrder> findByCreatedBy(String createdBy);
    
    // Find by approved by
    List<PurchaseOrder> findByApprovedBy(String approvedBy);
    
    // Find by order date range
    List<PurchaseOrder> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find by expected delivery date range
    List<PurchaseOrder> findByExpectedDeliveryDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find POs created in date range
    List<PurchaseOrder> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find POs by supplier and status
    List<PurchaseOrder> findBySupplierAndStatus(Supplier supplier, PurchaseOrder.POStatus status);
    
    // Find POs by supplier ID and status
    List<PurchaseOrder> findBySupplierIdAndStatus(Long supplierId, PurchaseOrder.POStatus status);
    
    // Find POs with total amount above threshold
    @Query("SELECT po FROM PurchaseOrder po WHERE po.totalAmount > :minAmount")
    List<PurchaseOrder> findByTotalAmountAbove(@Param("minAmount") java.math.BigDecimal minAmount);
    
    // Find POs with total amount below threshold
    @Query("SELECT po FROM PurchaseOrder po WHERE po.totalAmount < :maxAmount")
    List<PurchaseOrder> findByTotalAmountBelow(@Param("maxAmount") java.math.BigDecimal maxAmount);
    
    // Find POs by supplier and date range
    @Query("SELECT po FROM PurchaseOrder po WHERE po.supplier.id = :supplierId AND po.orderDate BETWEEN :startDate AND :endDate")
    List<PurchaseOrder> findBySupplierAndDateRange(@Param("supplierId") Long supplierId, 
                                                  @Param("startDate") LocalDateTime startDate, 
                                                  @Param("endDate") LocalDateTime endDate);
    
    // Find POs that can be received
    @Query("SELECT po FROM PurchaseOrder po WHERE po.status IN ('ORDERED', 'PARTIALLY_RECEIVED')")
    List<PurchaseOrder> findPOsReadyForReceipt();
    
    // Find POs overdue for delivery
    @Query("SELECT po FROM PurchaseOrder po WHERE po.expectedDeliveryDate < :currentDate AND po.status IN ('ORDERED', 'PARTIALLY_RECEIVED')")
    List<PurchaseOrder> findOverduePOs(@Param("currentDate") LocalDateTime currentDate);
    
    // Count POs by status
    long countByStatus(PurchaseOrder.POStatus status);
    
    // Count POs by supplier
    long countBySupplier(Supplier supplier);
    
    // Find POs with items
    @Query("SELECT DISTINCT po FROM PurchaseOrder po LEFT JOIN FETCH po.orderItems WHERE po.id = :poId")
    Optional<PurchaseOrder> findByIdWithItems(@Param("poId") Long poId);
    
    // Find POs with items and supplier
    @Query("SELECT DISTINCT po FROM PurchaseOrder po LEFT JOIN FETCH po.orderItems LEFT JOIN FETCH po.supplier WHERE po.id = :poId")
    Optional<PurchaseOrder> findByIdWithItemsAndSupplier(@Param("poId") Long poId);
    
    // Find all POs with items and supplier
    @Query("SELECT DISTINCT po FROM PurchaseOrder po LEFT JOIN FETCH po.orderItems LEFT JOIN FETCH po.supplier")
    List<PurchaseOrder> findAllWithItemsAndSupplier();
    
    // Find POs by supplier with items
    @Query("SELECT DISTINCT po FROM PurchaseOrder po LEFT JOIN FETCH po.orderItems WHERE po.supplier.id = :supplierId")
    List<PurchaseOrder> findBySupplierIdWithItems(@Param("supplierId") Long supplierId);
    
    // Search POs by multiple criteria
    @Query("SELECT po FROM PurchaseOrder po WHERE " +
           "po.poNumber LIKE CONCAT('%', :query, '%') OR " +
           "po.supplier.companyName LIKE CONCAT('%', :query, '%') OR " +
           "po.notes LIKE CONCAT('%', :query, '%')")
    List<PurchaseOrder> searchPOs(@Param("query") String query);
    
    // Find POs by payment terms
    List<PurchaseOrder> findByPaymentTerms(String paymentTerms);
    
    // Find POs with specific delivery address
    List<PurchaseOrder> findByDeliveryAddressContaining(String deliveryAddress);
}
