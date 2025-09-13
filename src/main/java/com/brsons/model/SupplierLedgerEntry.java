package com.brsons.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "supplier_ledger_entry")
public class SupplierLedgerEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_ledger_id", nullable = false)
    private SupplierLedger supplierLedger;
    
    @Column(name = "entry_date", nullable = false)
    private LocalDateTime entryDate;
    
    @Column(name = "particulars", nullable = false)
    private String particulars;
    
    @Column(name = "reference_type")
    private String referenceType; // PURCHASE_ORDER, PAYMENT, ADJUSTMENT, CREDIT_NOTE
    
    @Column(name = "reference_id")
    private Long referenceId;
    
    @Column(name = "reference_number")
    private String referenceNumber;
    
    @Column(name = "debit_amount", precision = 19, scale = 2)
    private BigDecimal debitAmount = BigDecimal.ZERO;
    
    @Column(name = "credit_amount", precision = 19, scale = 2)
    private BigDecimal creditAmount = BigDecimal.ZERO;
    
    @Column(name = "balance_after", precision = 19, scale = 2)
    private BigDecimal balanceAfter;
    
    @Column(name = "payment_method")
    private String paymentMethod;
    
    @Column(name = "payment_reference")
    private String paymentReference;
    
    @Column(name = "notes")
    private String notes;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Constructor
    public SupplierLedgerEntry() {
        this.createdAt = LocalDateTime.now();
        this.entryDate = LocalDateTime.now();
    }
    
    public SupplierLedgerEntry(SupplierLedger supplierLedger, String particulars, String referenceType, 
                              Long referenceId, String referenceNumber, BigDecimal debitAmount, 
                              BigDecimal creditAmount, BigDecimal balanceAfter) {
        this();
        this.supplierLedger = supplierLedger;
        this.particulars = particulars;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.referenceNumber = referenceNumber;
        this.debitAmount = debitAmount;
        this.creditAmount = creditAmount;
        this.balanceAfter = balanceAfter;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public SupplierLedger getSupplierLedger() {
        return supplierLedger;
    }
    
    public void setSupplierLedger(SupplierLedger supplierLedger) {
        this.supplierLedger = supplierLedger;
    }
    
    public LocalDateTime getEntryDate() {
        return entryDate;
    }
    
    public void setEntryDate(LocalDateTime entryDate) {
        this.entryDate = entryDate;
    }
    
    public String getParticulars() {
        return particulars;
    }
    
    public void setParticulars(String particulars) {
        this.particulars = particulars;
    }
    
    public String getReferenceType() {
        return referenceType;
    }
    
    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }
    
    public Long getReferenceId() {
        return referenceId;
    }
    
    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }
    
    public String getReferenceNumber() {
        return referenceNumber;
    }
    
    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }
    
    public BigDecimal getDebitAmount() {
        return debitAmount;
    }
    
    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = debitAmount;
    }
    
    public BigDecimal getCreditAmount() {
        return creditAmount;
    }
    
    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = creditAmount;
    }
    
    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }
    
    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getPaymentReference() {
        return paymentReference;
    }
    
    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    // Business methods
    public boolean isDebit() {
        return this.debitAmount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean isCredit() {
        return this.creditAmount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public BigDecimal getNetAmount() {
        return this.debitAmount.subtract(this.creditAmount);
    }
}
