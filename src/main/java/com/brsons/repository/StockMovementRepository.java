package com.brsons.repository;

import com.brsons.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    
    // Find movements by product
    List<StockMovement> findByProductIdOrderByMovementDateDesc(Long productId);
    
    // Find movements by product and date range
    List<StockMovement> findByProductIdAndMovementDateBetweenOrderByMovementDateDesc(
        Long productId, LocalDateTime startDate, LocalDateTime endDate);
    
    // Find movements by movement type
    List<StockMovement> findByMovementTypeOrderByMovementDateDesc(StockMovement.MovementType movementType);
    
    // Find movements by reference type and ID
    List<StockMovement> findByReferenceTypeAndReferenceIdOrderByMovementDateDesc(String referenceType, Long referenceId);
    
    // Find movements by date range
    List<StockMovement> findByMovementDateBetweenOrderByMovementDateDesc(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find movements by product and movement type
    List<StockMovement> findByProductIdAndMovementTypeOrderByMovementDateDesc(
        Long productId, StockMovement.MovementType movementType);
    
    // Count movements by product and movement type
    long countByProductIdAndMovementType(Long productId, StockMovement.MovementType movementType);
    
    // Find recent movements (last N days)
    @Query("SELECT sm FROM StockMovement sm WHERE sm.movementDate >= :startDate ORDER BY sm.movementDate DESC")
    List<StockMovement> findRecentMovements(@Param("startDate") LocalDateTime startDate);
    
    // Find movements by reason (search)
    @Query("SELECT sm FROM StockMovement sm WHERE sm.reason LIKE CONCAT('%', :searchTerm, '%') ORDER BY sm.movementDate DESC")
    List<StockMovement> findByReasonContaining(@Param("searchTerm") String searchTerm);
    
    // Get total quantity moved by product and movement type in date range
    @Query("SELECT SUM(sm.quantity) FROM StockMovement sm WHERE sm.product.id = :productId " +
           "AND sm.movementType = :movementType AND sm.movementDate BETWEEN :startDate AND :endDate")
    Integer getTotalQuantityByProductAndTypeInDateRange(
        @Param("productId") Long productId, 
        @Param("movementType") StockMovement.MovementType movementType,
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate);
    
    // Find movements with product details (for reporting)
    @Query("SELECT sm FROM StockMovement sm JOIN FETCH sm.product WHERE sm.movementDate BETWEEN :startDate AND :endDate ORDER BY sm.movementDate DESC")
    List<StockMovement> findMovementsWithProductInDateRange(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate);
}
