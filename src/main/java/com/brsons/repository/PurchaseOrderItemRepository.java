package com.brsons.repository;

import com.brsons.model.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {
    
    // Find items by purchase order
    List<PurchaseOrderItem> findByPurchaseOrderId(Long purchaseOrderId);
    
    // Find items by product
    List<PurchaseOrderItem> findByProductId(Long productId);
    
    // Find items with quantity greater than received quantity
    @Query("SELECT poi FROM PurchaseOrderItem poi WHERE poi.purchaseOrder.id = :poId AND poi.orderedQuantity > poi.receivedQuantity")
    List<PurchaseOrderItem> findPendingItemsByPurchaseOrder(@Param("poId") Long purchaseOrderId);
    
    // Find items by purchase order with product details
    @Query("SELECT poi FROM PurchaseOrderItem poi JOIN FETCH poi.product WHERE poi.purchaseOrder.id = :poId")
    List<PurchaseOrderItem> findByPurchaseOrderWithProduct(@Param("poId") Long purchaseOrderId);
    
    // Delete items by purchase order ID
    void deleteByPurchaseOrderId(Long purchaseOrderId);
}
