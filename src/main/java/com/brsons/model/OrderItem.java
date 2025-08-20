package com.brsons.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    private int quantity;

    // Store the actual price at the time of order placement
    @Column(precision = 10, scale = 2)
    private BigDecimal unitPrice; // Price per unit at order time
    
    @Column(precision = 10, scale = 2)
    private BigDecimal totalPrice; // Total price for this item (unitPrice * quantity)
    
    // Store user type for reference (Retail, B2B, Admin)
    @Column(length = 20)
    private String userType; // To know which pricing was used
    
    // Store the original product price type used
    @Column(length = 20)
    private String priceType; // "retail", "b2b", or "admin"

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Default constructor
    public OrderItem() {}

    // Constructor with price information
    public OrderItem(Long productId, int quantity, BigDecimal unitPrice, String userType, String priceType) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.userType = userType;
        this.priceType = priceType;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        // Recalculate total price when quantity changes
        if (this.unitPrice != null) {
            this.totalPrice = this.unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        // Recalculate total price when unit price changes
        if (this.quantity > 0) {
            this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(this.quantity));
        }
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getPriceType() {
        return priceType;
    }

    public void setPriceType(String priceType) {
        this.priceType = priceType;
    }

    // Helper method to calculate total price
    public void calculateTotalPrice() {
        if (this.unitPrice != null && this.quantity > 0) {
            this.totalPrice = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
        }
    }
}
