package com.brsons.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_ledger_entry")
public class CustomerLedgerEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_ledger_id", nullable = false)
    private CustomerLedger customerLedger;
    
    @Column(name = "entry_date", nullable = false)
    private LocalDateTime entryDate;
    
    @Column(name = "particulars", nullable = false)
    private String particulars;
    
    @Column(name = "reference_type")
    private String referenceType; // ORDER, PAYMENT, ADJUSTMENT
    
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
    public CustomerLedgerEntry() {
        this.entryDate = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }
    
    public CustomerLedgerEntry(CustomerLedger customerLedger, String particulars, 
                              String referenceType, Long referenceId, String referenceNumber) {
        this();
        this.customerLedger = customerLedger;
        this.particulars = particulars;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.referenceNumber = referenceNumber;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public CustomerLedger getCustomerLedger() {
        return customerLedger;
    }
    
    public void setCustomerLedger(CustomerLedger customerLedger) {
        this.customerLedger = customerLedger;
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
        return debitAmount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean isCredit() {
        return creditAmount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public BigDecimal getAmount() {
        if (isDebit()) {
            return debitAmount;
        } else if (isCredit()) {
            return creditAmount;
        }
        return BigDecimal.ZERO;
    }
    
    public String getEntryType() {
        if (isDebit()) {
            return "DEBIT";
        } else if (isCredit()) {
            return "CREDIT";
        }
        return "ZERO";
    }
}
