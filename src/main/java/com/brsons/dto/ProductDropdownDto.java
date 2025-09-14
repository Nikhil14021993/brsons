package com.brsons.dto;

public class ProductDropdownDto {
    private Long id;
    private String productName;
    private Double retailPrice;
    private Integer stockQuantity;
    private String sku;
    
    public ProductDropdownDto() {}
    
    public ProductDropdownDto(Long id, String productName, Double retailPrice, Integer stockQuantity, String sku) {
        this.id = id;
        this.productName = productName;
        this.retailPrice = retailPrice;
        this.stockQuantity = stockQuantity;
        this.sku = sku;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    
    public Double getRetailPrice() { return retailPrice; }
    public void setRetailPrice(Double retailPrice) { this.retailPrice = retailPrice; }
    
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
}
