package com.brsons.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "goods_received_notes")
public class GoodsReceivedNote {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "grn_number", unique = true, nullable = false)
    private String grnNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;
    
    @Column(name = "received_date", nullable = false)
    private LocalDateTime receivedDate;
    
    @Column(name = "delivery_note_number")
    private String deliveryNoteNumber;
    
    @Column(name = "vehicle_number")
    private String vehicleNumber;
    
    @Column(name = "received_by")
    private String receivedBy;
    
    @Column(name = "inspected_by")
    private String inspectedBy;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GRNStatus status;
    
    @Column(name = "subtotal", precision = 10, scale = 2)
    private BigDecimal subtotal;
    
    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount;
    
    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;
    
    @Column(name = "quality_remarks")
    private String qualityRemarks;
    
    @Column(name = "notes")
    private String notes;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationships
    @OneToMany(mappedBy = "grn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GRNItem> grnItems;
    
    // Enums
    public enum GRNStatus {
        DRAFT, RECEIVED, INSPECTED, APPROVED, REJECTED, CANCELLED
    }
    
    // Constructors
    public GoodsReceivedNote() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = GRNStatus.DRAFT;
        this.receivedDate = LocalDateTime.now();
        this.subtotal = BigDecimal.ZERO;
        this.taxAmount = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getGrnNumber() { return grnNumber; }
    public void setGrnNumber(String grnNumber) { this.grnNumber = grnNumber; }
    
    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }
    
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    
    public LocalDateTime getReceivedDate() { return receivedDate; }
    public void setReceivedDate(LocalDateTime receivedDate) { this.receivedDate = receivedDate; }
    
    public String getDeliveryNoteNumber() { return deliveryNoteNumber; }
    public void setDeliveryNoteNumber(String deliveryNoteNumber) { this.deliveryNoteNumber = deliveryNoteNumber; }
    
    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }
    
    public String getReceivedBy() { return receivedBy; }
    public void setReceivedBy(String receivedBy) { this.receivedBy = receivedBy; }
    
    public String getInspectedBy() { return inspectedBy; }
    public void setInspectedBy(String inspectedBy) { this.inspectedBy = inspectedBy; }
    
    public GRNStatus getStatus() { return status; }
    public void setStatus(GRNStatus status) { this.status = status; }
    
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public String getQualityRemarks() { return qualityRemarks; }
    public void setQualityRemarks(String qualityRemarks) { this.qualityRemarks = qualityRemarks; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public List<GRNItem> getGrnItems() { return grnItems; }
    public void setGrnItems(List<GRNItem> grnItems) { this.grnItems = grnItems; }
    
    // Business Methods
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public void calculateTotals() {
        this.subtotal = BigDecimal.ZERO;
        if (this.grnItems != null) {
            for (GRNItem item : this.grnItems) {
                this.subtotal = this.subtotal.add(item.getTotalAmount());
            }
        }
        
        this.totalAmount = this.subtotal.add(this.taxAmount != null ? this.taxAmount : BigDecimal.ZERO);
    }
    
    public boolean canBeApproved() {
        return GRNStatus.INSPECTED.equals(this.status);
    }
    
    public boolean canBeRejected() {
        return GRNStatus.INSPECTED.equals(this.status);
    }
    
    public boolean isApproved() {
        return GRNStatus.APPROVED.equals(this.status);
    }
    
    public boolean isRejected() {
        return GRNStatus.REJECTED.equals(this.status);
    }
    
    public int getTotalReceivedQuantity() {
        if (this.grnItems == null) return 0;
        return this.grnItems.stream()
            .mapToInt(item -> item.getReceivedQuantity())
            .sum();
    }
    
    public int getTotalAcceptedQuantity() {
        if (this.grnItems == null) return 0;
        return this.grnItems.stream()
            .mapToInt(item -> item.getAcceptedQuantity())
            .sum();
    }
    
    public int getTotalRejectedQuantity() {
        if (this.grnItems == null) return 0;
        return this.grnItems.stream()
            .mapToInt(item -> item.getRejectedQuantity())
            .sum();
    }
}
