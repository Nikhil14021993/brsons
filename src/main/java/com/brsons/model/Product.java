package com.brsons.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "retail_price")
    private Double retailPrice;
    
    @Column(name = "b2b_price")
    private Double b2bPrice;
    
    private Double discount;
    
    @Column(name = "stock_quantity")
    private Integer stockQuantity;
    
    @Column(name = "reserved_quantity")
    private Integer reservedQuantity;
    
    @Column(name = "price", precision = 10, scale = 2)
    private java.math.BigDecimal price;
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    private String status;

    // Image URLs
    private String image1; // Mandatory
    private String image2;
    private String image3;
    private String image4;
    private String image5;

    @Column(name = "main_photo")
    private String mainPhoto;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants;

    // Pre-persist method to set created_at
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    // Pre-update method to set updated_at
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Double getRetailPrice() { return retailPrice; }
    public void setRetailPrice(Double retailPrice) { this.retailPrice = retailPrice; }
    
    public Double getB2bPrice() { return b2bPrice; }
    public void setB2bPrice(Double b2bPrice) { this.b2bPrice = b2bPrice; }
    
    public Double getDiscount() { return discount; }
    public void setDiscount(Double discount) { this.discount = discount; }
    
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    
    public Integer getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; }
    
    public java.math.BigDecimal getPrice() { return price; }
    public void setPrice(java.math.BigDecimal price) { this.price = price; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getImage1() { return image1; }
    public void setImage1(String image1) { this.image1 = image1; }
    public String getImage2() { return image2; }
    public void setImage2(String image2) { this.image2 = image2; }
    public String getImage3() { return image3; }
    public void setImage3(String image3) { this.image3 = image3; }
    public String getImage4() { return image4; }
    public void setImage4(String image4) { this.image4 = image4; }
    public String getImage5() { return image5; }
    public void setImage5(String image5) { this.image5 = image5; }

    public String getMainPhoto() { return mainPhoto; }
    public void setMainPhoto(String mainPhoto) { this.mainPhoto = mainPhoto; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    
    public List<ProductVariant> getVariants() { return variants; }
    public void setVariants(List<ProductVariant> variants) { this.variants = variants; }
    
    // Helper methods for discounted prices
    public Double getDiscountedRetailPrice() {
        if (discount != null && discount > 0) {
            return retailPrice != null ? retailPrice * (1 - discount / 100) : null;
        }
        return retailPrice;
    }
    
    public Double getDiscountedB2bPrice() {
        if (discount != null && discount > 0) {
            return b2bPrice != null ? b2bPrice * (1 - discount / 100) : null;
        }
        return b2bPrice;
    }
}
