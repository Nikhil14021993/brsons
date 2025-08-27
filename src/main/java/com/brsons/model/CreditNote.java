package com.brsons.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.brsons.model.PurchaseOrder;

@Entity
@Table(name = "credit_notes")
public class CreditNote {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "credit_note_number", unique = true, nullable = false)
    private String creditNoteNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;
    
    @Column(name = "credit_date", nullable = false)
    private LocalDateTime creditDate;
    
    @Column(name = "credit_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal creditAmount;
    
    @Column(name = "reason", length = 500)
    private String reason;
    
    @Column(name = "status", length = 50, nullable = false)
    private String status; // Draft, Issued, Applied, Cancelled
    
    @Column(name = "notes", length = 1000)
    private String notes;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    
    @OneToMany(mappedBy = "creditNote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CreditNoteItem> creditNoteItems = new ArrayList<>();
    
    // Constructors
    public CreditNote() {
        this.creditDate = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.status = "Draft";
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getCreditNoteNumber() { return creditNoteNumber; }
    public void setCreditNoteNumber(String creditNoteNumber) { this.creditNoteNumber = creditNoteNumber; }
    
    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }
    
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    
    public LocalDateTime getCreditDate() { return creditDate; }
    public void setCreditDate(LocalDateTime creditDate) { this.creditDate = creditDate; }
    
    public BigDecimal getCreditAmount() { return creditAmount; }
    public void setCreditAmount(BigDecimal creditAmount) { this.creditAmount = creditAmount; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    
    public List<CreditNoteItem> getCreditNoteItems() { return creditNoteItems; }
    public void setCreditNoteItems(List<CreditNoteItem> creditNoteItems) { this.creditNoteItems = creditNoteItems; }
    
    // Helper methods
    public void addCreditNoteItem(CreditNoteItem item) {
        creditNoteItems.add(item);
        item.setCreditNote(this);
    }
    
    public void removeCreditNoteItem(CreditNoteItem item) {
        creditNoteItems.remove(item);
        item.setCreditNote(null);
    }
}
