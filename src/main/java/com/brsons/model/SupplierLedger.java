package com.brsons.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "supplier_ledger")
public class SupplierLedger {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "supplier_name", nullable = false)
    private String supplierName;
    
    @Column(name = "supplier_phone", nullable = false)
    private String supplierPhone;
    
    @Column(name = "supplier_email")
    private String supplierEmail;
    
    @Column(name = "supplier_code")
    private String supplierCode;
    
    @Column(name = "opening_balance", precision = 19, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;
    
    @Column(name = "current_balance", precision = 19, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO;
    
    @Column(name = "total_debits", precision = 19, scale = 2)
    private BigDecimal totalDebits = BigDecimal.ZERO;
    
    @Column(name = "total_credits", precision = 19, scale = 2)
    private BigDecimal totalCredits = BigDecimal.ZERO;
    
    @Column(name = "credit_limit", precision = 19, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;
    
    @Column(name = "status")
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, SUSPENDED
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructor
    public SupplierLedger() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public SupplierLedger(String supplierName, String supplierPhone) {
        this();
        this.supplierName = supplierName;
        this.supplierPhone = supplierPhone;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getSupplierName() {
        return supplierName;
    }
    
    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }
    
    public String getSupplierPhone() {
        return supplierPhone;
    }
    
    public void setSupplierPhone(String supplierPhone) {
        this.supplierPhone = supplierPhone;
    }
    
    public String getSupplierEmail() {
        return supplierEmail;
    }
    
    public void setSupplierEmail(String supplierEmail) {
        this.supplierEmail = supplierEmail;
    }
    
    public String getSupplierCode() {
        return supplierCode;
    }
    
    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
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
    
    public BigDecimal getCreditLimit() {
        return creditLimit;
    }
    
    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
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
    
    public boolean hasCreditAvailable() {
        return this.creditLimit.compareTo(BigDecimal.ZERO) == 0 || 
               this.currentBalance.compareTo(this.creditLimit) < 0;
    }
    
    public BigDecimal getAvailableCredit() {
        if (this.creditLimit.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(Double.MAX_VALUE);
        }
        return this.creditLimit.subtract(this.currentBalance).max(BigDecimal.ZERO);
    }
}
