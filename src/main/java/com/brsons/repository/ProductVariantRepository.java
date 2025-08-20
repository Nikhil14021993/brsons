package com.brsons.repository;

import com.brsons.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    
    // Find all variants for a specific product
    List<ProductVariant> findByProductId(Long productId);
    
    // Find variants by product ID and status
    List<ProductVariant> findByProductIdAndStatus(Long productId, String status);
    
    // Find variants by size and color for a specific product
    Optional<ProductVariant> findByProductIdAndSizeAndColor(Long productId, String size, String color);
    
    // Find variants with low stock (quantity <= 5)
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.stockQuantity <= 5 AND pv.stockQuantity > 0")
    List<ProductVariant> findLowStockVariants();
    
    // Find out of stock variants
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.stockQuantity <= 0 OR pv.stockQuantity IS NULL")
    List<ProductVariant> findOutOfStockVariants();
    
    // Find variants by SKU
    Optional<ProductVariant> findBySku(String sku);
    
    // Find active variants for a product
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.product.id = :productId AND pv.status = 'Active'")
    List<ProductVariant> findActiveVariantsByProductId(@Param("productId") Long productId);
    
    // Find variants with specific size
    List<ProductVariant> findBySize(String size);
    
    // Find variants with specific color
    List<ProductVariant> findByColor(String color);
    
    // Find variants by price range (retail price)
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.retailPrice BETWEEN :minPrice AND :maxPrice")
    List<ProductVariant> findByRetailPriceRange(@Param("minPrice") Double minPrice, @Param("maxPrice") Double maxPrice);
    
    // Find variants by price range (B2B price)
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.b2bPrice BETWEEN :minPrice AND :maxPrice")
    List<ProductVariant> findByB2bPriceRange(@Param("minPrice") Double minPrice, @Param("maxPrice") Double maxPrice);
    
    // Count variants by product ID
    long countByProductId(Long productId);
    
    // Count active variants by product ID
    long countByProductIdAndStatus(Long productId, String status);
}
