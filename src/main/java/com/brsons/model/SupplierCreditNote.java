package com.brsons.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "supplier_credit_notes")
public class SupplierCreditNote {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "credit_note_number", unique = true, nullable = false)
    private String creditNoteNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id")
    private PurchaseOrder purchaseOrder;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grn_id")
    private GoodsReceivedNote grn;
    
    @Column(name = "credit_note_date", nullable = false)
    private LocalDateTime creditNoteDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private CreditNoteType type;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CreditNoteStatus status;
    
    @Column(name = "subtotal", precision = 10, scale = 2)
    private BigDecimal subtotal;
    
    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount;
    
    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;
    
    @Column(name = "reason")
    private String reason;
    
    @Column(name = "reference_document")
    private String referenceDocument;
    
    @Column(name = "notes")
    private String notes;
    
    @Column(name = "created_by")
    private String createdBy;
    
    @Column(name = "approved_by")
    private String approvedBy;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationships
    @OneToMany(mappedBy = "creditNote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CreditNoteItem> creditNoteItems;
    
    // Enums
    public enum CreditNoteType {
        RETURN, DISCOUNT, CORRECTION, DAMAGED_GOODS, QUALITY_ISSUE, PRICE_ADJUSTMENT
    }
    
    public enum CreditNoteStatus {
        DRAFT, PENDING_APPROVAL, APPROVED, REJECTED, CANCELLED, APPLIED
    }
    
    // Constructors
    public SupplierCreditNote() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.creditNoteDate = LocalDateTime.now();
        this.status = CreditNoteStatus.DRAFT;
        this.subtotal = BigDecimal.ZERO;
        this.taxAmount = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getCreditNoteNumber() { return creditNoteNumber; }
    public void setCreditNoteNumber(String creditNoteNumber) { this.creditNoteNumber = creditNoteNumber; }
    
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    
    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }
    
    public GoodsReceivedNote getGrn() { return grn; }
    public void setGrn(GoodsReceivedNote grn) { this.grn = grn; }
    
    public LocalDateTime getCreditNoteDate() { return creditNoteDate; }
    public void setCreditNoteDate(LocalDateTime creditNoteDate) { this.creditNoteDate = creditNoteDate; }
    
    public CreditNoteType getType() { return type; }
    public void setType(CreditNoteType type) { this.type = type; }
    
    public CreditNoteStatus getStatus() { return status; }
    public void setStatus(CreditNoteStatus status) { this.status = status; }
    
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getReferenceDocument() { return referenceDocument; }
    public void setReferenceDocument(String referenceDocument) { this.referenceDocument = referenceDocument; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public List<CreditNoteItem> getCreditNoteItems() { return creditNoteItems; }
    public void setCreditNoteItems(List<CreditNoteItem> creditNoteItems) { this.creditNoteItems = creditNoteItems; }
    
    // Business Methods
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public void calculateTotals() {
        this.subtotal = BigDecimal.ZERO;
        if (this.creditNoteItems != null) {
            for (CreditNoteItem item : this.creditNoteItems) {
                this.subtotal = this.subtotal.add(item.getTotalAmount());
            }
        }
        
        this.totalAmount = this.subtotal.add(this.taxAmount != null ? this.taxAmount : BigDecimal.ZERO);
    }
    
    public boolean canBeApproved() {
        return CreditNoteStatus.PENDING_APPROVAL.equals(this.status);
    }
    
    public boolean canBeRejected() {
        return CreditNoteStatus.PENDING_APPROVAL.equals(this.status);
    }
    
    public boolean canBeApplied() {
        return CreditNoteStatus.APPROVED.equals(this.status);
    }
    
    public boolean isApproved() {
        return CreditNoteStatus.APPROVED.equals(this.status);
    }
    
    public boolean isApplied() {
        return CreditNoteStatus.APPLIED.equals(this.status);
    }
    
    public void approve(String approvedBy) {
        this.status = CreditNoteStatus.APPROVED;
        this.approvedBy = approvedBy;
        this.approvedAt = LocalDateTime.now();
    }
    
    public void apply() {
        this.status = CreditNoteStatus.APPLIED;
        // Update supplier balance
        if (this.supplier != null) {
            Double currentBalance = this.supplier.getCurrentBalance();
            this.supplier.setCurrentBalance(currentBalance - this.totalAmount.doubleValue());
        }
    }
    
    public int getTotalItems() {
        if (this.creditNoteItems == null) return 0;
        return this.creditNoteItems.size();
    }
    
    public String getTypeDescription() {
        switch (this.type) {
            case RETURN: return "Goods Return";
            case DISCOUNT: return "Discount";
            case CORRECTION: return "Price Correction";
            case DAMAGED_GOODS: return "Damaged Goods";
            case QUALITY_ISSUE: return "Quality Issue";
            case PRICE_ADJUSTMENT: return "Price Adjustment";
            default: return "Unknown";
        }
    }
}
