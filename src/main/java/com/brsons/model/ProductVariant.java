package com.brsons.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_variants")
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private String size;
    private String color;
    private String fabric;
    private String style;
    private String pattern;
    
    @Column(name = "care_instruction", length = 500)
    private String careInstruction;
    
    private String occasion;
    
    @Column(name = "stock_quantity")
    private Integer stockQuantity;
    
    @Column(name = "retail_price")
    private Double retailPrice; // Variant-specific retail price (optional, can override product price)
    
    @Column(name = "b2b_price")
    private Double b2bPrice; // Variant-specific B2B price (optional, can override product price)
    
    @Column(name = "variant_discount")
    private Double variantDiscount; // Variant-specific discount (optional, can override product discount)
    
    @Column(name = "sku")
    private String sku; // Stock Keeping Unit for this variant
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    private String status; // Active, Inactive, Out of Stock, etc.

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

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getFabric() { return fabric; }
    public void setFabric(String fabric) { this.fabric = fabric; }

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public String getCareInstruction() { return careInstruction; }
    public void setCareInstruction(String careInstruction) { this.careInstruction = careInstruction; }

    public String getOccasion() { return occasion; }
    public void setOccasion(String occasion) { this.occasion = occasion; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public Double getRetailPrice() { return retailPrice; }
    public void setRetailPrice(Double retailPrice) { this.retailPrice = retailPrice; }

    public Double getB2bPrice() { return b2bPrice; }
    public void setB2bPrice(Double b2bPrice) { this.b2bPrice = b2bPrice; }

    public Double getVariantDiscount() { return variantDiscount; }
    public void setVariantDiscount(Double variantDiscount) { this.variantDiscount = variantDiscount; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Helper method to get effective retail price (variant price or product price)
    public Double getEffectiveRetailPrice() {
        if (retailPrice != null) {
            return retailPrice;
        }
        return product != null ? product.getRetailPrice() : null;
    }

    // Helper method to get effective B2B price (variant price or product price)
    public Double getEffectiveB2bPrice() {
        if (b2bPrice != null) {
            return b2bPrice;
        }
        return product != null ? product.getB2bPrice() : null;
    }

    // Helper method to get effective discount (variant discount or product discount)
    public Double getEffectiveDiscount() {
        if (variantDiscount != null) {
            return variantDiscount;
        }
        return product != null ? product.getDiscount() : null;
    }

    // Helper method to calculate discounted retail price
    public Double getDiscountedRetailPrice() {
        Double effectivePrice = getEffectiveRetailPrice();
        Double effectiveDiscount = getEffectiveDiscount();
        
        if (effectivePrice != null && effectiveDiscount != null && effectiveDiscount > 0) {
            return effectivePrice * (1 - effectiveDiscount / 100.0);
        }
        return effectivePrice;
    }

    // Helper method to calculate discounted B2B price
    public Double getDiscountedB2bPrice() {
        Double effectivePrice = getEffectiveB2bPrice();
        Double effectiveDiscount = getEffectiveDiscount();
        
        if (effectivePrice != null && effectiveDiscount != null && effectiveDiscount > 0) {
            return effectivePrice * (1 - effectiveDiscount / 100.0);
        }
        return effectivePrice;
    }

    // Helper method to check if variant is in stock
    public boolean isInStock() {
        return stockQuantity != null && stockQuantity > 0;
    }

    // Helper method to get stock status
    public String getStockStatus() {
        if (stockQuantity == null || stockQuantity <= 0) {
            return "Out of Stock";
        } else if (stockQuantity <= 5) {
            return "Low Stock";
        } else {
            return "In Stock";
        }
    }
}
