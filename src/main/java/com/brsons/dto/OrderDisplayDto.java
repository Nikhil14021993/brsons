package com.brsons.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderDisplayDto {
    private Long id;
    private String invoiceNumber;
    private LocalDateTime createdAt;
    private String name;
    private String userPhone;
    private String state;
    private String billType;
    private BigDecimal total;
    private String orderStatus;
    private boolean canModify = true; // Default to true
    
    // Constructor
    public OrderDisplayDto(Long id, String invoiceNumber, LocalDateTime createdAt, String name, 
                          String userPhone, String state, String billType, BigDecimal total, String orderStatus) {
        this.id = id;
        this.invoiceNumber = invoiceNumber;
        this.createdAt = createdAt;
        this.name = name;
        this.userPhone = userPhone;
        this.state = state;
        this.billType = billType;
        this.total = total;
        this.orderStatus = orderStatus;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getInvoiceNumber() {
        return invoiceNumber;
    }
    
    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getUserPhone() {
        return userPhone;
    }
    
    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public String getBillType() {
        return billType;
    }
    
    public void setBillType(String billType) {
        this.billType = billType;
    }
    
    public BigDecimal getTotal() {
        return total;
    }
    
    public void setTotal(BigDecimal total) {
        this.total = total;
    }
    
    public String getOrderStatus() {
        return orderStatus;
    }
    
    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }
    
    public boolean isCanModify() {
        return canModify;
    }
    
    public void setCanModify(boolean canModify) {
        this.canModify = canModify;
    }
}
