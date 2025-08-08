package com.brsons.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class CartProductEntry {
    
    private Long productId;
    private Integer quantity;
    private String userPhone;

    // Constructors
    public CartProductEntry() {}
    public CartProductEntry(Long productId, Integer quantity, String userPhone) {
        this.productId = productId;
        this.quantity = quantity;
        this.userPhone = userPhone;
    }

    // Getters and setters
    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }
}

