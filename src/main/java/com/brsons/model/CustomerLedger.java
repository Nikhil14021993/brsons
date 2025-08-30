package com.brsons.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_ledger")
public class CustomerLedger {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "customer_name", nullable = false)
    private String customerName;
    
    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;
    
    @Column(name = "customer_email")
    private String customerEmail;
    
    @Column(name = "opening_balance", precision = 19, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;
    
    @Column(name = "current_balance", precision = 19, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO;
    
    @Column(name = "total_debits", precision = 19, scale = 2)
    private BigDecimal totalDebits = BigDecimal.ZERO;
    
    @Column(name = "total_credits", precision = 19, scale = 2)
    private BigDecimal totalCredits = BigDecimal.ZERO;
    
    @Column(name = "status")
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, SUSPENDED
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructor
    public CustomerLedger() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public CustomerLedger(String customerName, String customerPhone) {
        this();
        this.customerName = customerName;
        this.customerPhone = customerPhone;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCustomerName() {
        return customerName;
    }
    
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    
    public String getCustomerPhone() {
        return customerPhone;
    }
    
    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }
    
    public String getCustomerEmail() {
        return customerEmail;
    }
    
    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }
    
    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }
    
    public void setOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = openingBalance;
    }
    
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }
    
    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }
    
    public BigDecimal getTotalDebits() {
        return totalDebits;
    }
    
    public void setTotalDebits(BigDecimal totalDebits) {
        this.totalDebits = totalDebits;
    }
    
    public BigDecimal getTotalCredits() {
        return totalCredits;
    }
    
    public void setTotalCredits(BigDecimal totalCredits) {
        this.totalCredits = totalCredits;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Business methods
    public void addDebit(BigDecimal amount) {
        this.totalDebits = this.totalDebits.add(amount);
        this.currentBalance = this.currentBalance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }
    
    public void addCredit(BigDecimal amount) {
        this.totalCredits = this.totalCredits.add(amount);
        this.currentBalance = this.currentBalance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean hasOutstandingBalance() {
        return this.currentBalance.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public BigDecimal getOutstandingAmount() {
        return this.currentBalance.max(BigDecimal.ZERO);
    }
}
