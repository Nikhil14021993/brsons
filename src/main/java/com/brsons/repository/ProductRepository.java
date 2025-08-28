package com.brsons.repository;

import com.brsons.model.Product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryIdAndStatus(Long categoryId, String status);
    
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.id = :id")
    Optional<Product> findByIdWithCategory(@Param("id") Long id);
    
    // Find products by category ID only (without status filter)
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.category.id = :categoryId")
    List<Product> findByCategoryId(@Param("categoryId") Long categoryId);
    
    // Find all products with categories for inventory management
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category")
    List<Product> findAllWithCategory();
    
    // Find products with low stock (below threshold)
    List<Product> findByStockQuantityLessThan(Integer threshold);
    
    // Find products with stock quantity less than or equal to zero
    List<Product> findByStockQuantityLessThanEqual(Integer threshold);
    
    // Find products by stock status
    List<Product> findByStockQuantityGreaterThan(Integer threshold);
    
    // Find products with reserved stock
    List<Product> findByReservedQuantityGreaterThan(Integer threshold);
    
    // Find products by price range
    List<Product> findByPriceBetween(java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice);
    
    // Find products by name containing (search)
    List<Product> findByProductNameContainingIgnoreCase(String productName);
    
    // Find products by status
    List<Product> findByStatus(String status);
    
    // Count products by stock level
    long countByStockQuantityLessThan(Integer threshold);
    
    // Count products by status
    long countByStatus(String status);
}
