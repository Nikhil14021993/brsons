package com.brsons.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "outstanding_items")
public class Outstanding {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "outstanding_type", nullable = false)
    private OutstandingType type;
    
    @Column(name = "reference_id", nullable = false)
    private Long referenceId; // ID of the related entity (Order, PurchaseOrder, etc.)
    
    @Column(name = "reference_type", nullable = false)
    private String referenceType; // "ORDER", "PURCHASE_ORDER", "INVOICE", etc.
    
    @Column(name = "reference_number", nullable = false)
    private String referenceNumber; // Invoice number, PO number, etc.
    
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "due_date")
    private LocalDateTime dueDate;
    
    @Column(name = "days_overdue")
    private Integer daysOverdue;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OutstandingStatus status;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "customer_supplier_name")
    private String customerSupplierName;
    
    @Column(name = "contact_info")
    private String contactInfo;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "notes")
    private String notes;
    
    // Enums
    public enum OutstandingType {
        INVOICE_RECEIVABLE,      // Money owed to us (customer invoices)
        INVOICE_PAYABLE,         // Money we owe (supplier invoices)
        PURCHASE_ORDER,          // Outstanding POs
        ADVANCE_PAYMENT,         // Advance payments made/received
        EXPENSE,                 // Outstanding expenses
        LOAN,                    // Outstanding loans
        OTHER                    // Other outstanding items
    }
    
    public enum OutstandingStatus {
        PENDING,                 // Not yet due
        OVERDUE,                 // Past due date
        PARTIALLY_PAID,          // Partially settled
        DISPUTED,                // Under dispute
        SETTLED,                 // Fully paid/settled
        CANCELLED                // Cancelled/voided
    }
    
    // Constructors
    public Outstanding() {
        this.createdAt = LocalDateTime.now();
        this.status = OutstandingStatus.PENDING;
    }
    
    public Outstanding(OutstandingType type, Long referenceId, String referenceType, 
                      String referenceNumber, BigDecimal amount, LocalDateTime dueDate, 
                      String customerSupplierName) {
        this();
        this.type = type;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.referenceNumber = referenceNumber;
        this.amount = amount;
        this.dueDate = dueDate;
        this.customerSupplierName = customerSupplierName;
        this.updateDaysOverdue();
    }
    
    // Business methods
    public void updateDaysOverdue() {
        if (this.dueDate != null && this.status != OutstandingStatus.SETTLED) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(this.dueDate)) {
                this.daysOverdue = (int) java.time.Duration.between(this.dueDate, now).toDays();
                if (this.status == OutstandingStatus.PENDING) {
                    this.status = OutstandingStatus.OVERDUE;
                }
            } else {
                this.daysOverdue = 0;
            }
        }
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isOverdue() {
        return this.status == OutstandingStatus.OVERDUE;
    }
    
    public boolean isCritical() {
        return this.daysOverdue != null && this.daysOverdue > 30;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public OutstandingType getType() { return type; }
    public void setType(OutstandingType type) { this.type = type; }
    
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    
    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { 
        this.dueDate = dueDate; 
        this.updateDaysOverdue();
    }
    
    public Integer getDaysOverdue() { return daysOverdue; }
    public void setDaysOverdue(Integer daysOverdue) { this.daysOverdue = daysOverdue; }
    
    public OutstandingStatus getStatus() { return status; }
    public void setStatus(OutstandingStatus status) { this.status = status; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCustomerSupplierName() { return customerSupplierName; }
    public void setCustomerSupplierName(String customerSupplierName) { this.customerSupplierName = customerSupplierName; }
    
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
