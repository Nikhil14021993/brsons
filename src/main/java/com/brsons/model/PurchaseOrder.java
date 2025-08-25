package com.brsons.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "po_number", unique = true, nullable = false)
    private String poNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;
    
    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;
    
    @Column(name = "expected_delivery_date")
    private LocalDateTime expectedDeliveryDate;
    
    @Column(name = "delivery_address")
    private String deliveryAddress;
    
    @Column(name = "payment_terms")
    private String paymentTerms;
    
    @Column(name = "subtotal", precision = 10, scale = 2)
    private BigDecimal subtotal;
    
    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount;
    
    @Column(name = "shipping_cost", precision = 10, scale = 2)
    private BigDecimal shippingCost;
    
    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;
    
    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private POStatus status;
    
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
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderItem> orderItems;
    
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL)
    private List<GoodsReceivedNote> grnList;
    
    // Enums
    public enum POStatus {
        DRAFT, PENDING_APPROVAL, APPROVED, ORDERED, PARTIALLY_RECEIVED, 
        FULLY_RECEIVED, CANCELLED, CLOSED
    }
    
    // Constructors
    public PurchaseOrder() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = POStatus.DRAFT;
        this.orderDate = LocalDateTime.now();
        this.subtotal = BigDecimal.ZERO;
        this.taxAmount = BigDecimal.ZERO;
        this.shippingCost = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getPoNumber() { return poNumber; }
    public void setPoNumber(String poNumber) { this.poNumber = poNumber; }
    
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    
    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
    
    public LocalDateTime getExpectedDeliveryDate() { return expectedDeliveryDate; }
    public void setExpectedDeliveryDate(LocalDateTime expectedDeliveryDate) { this.expectedDeliveryDate = expectedDeliveryDate; }
    
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    
    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }
    
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    
    public BigDecimal getShippingCost() { return shippingCost; }
    public void setShippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; }
    
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public POStatus getStatus() { return status; }
    public void setStatus(POStatus status) { this.status = status; }
    
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
    
    public List<PurchaseOrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<PurchaseOrderItem> items) {
        // Reset and re-link every child to this parent
        if (this.orderItems == null) {
            this.orderItems = new ArrayList<>();
        } else {
            // orphanRemoval=true will delete them on flush
            this.orderItems.clear();
        }
        if (items != null) {
            for (PurchaseOrderItem i : items) {
                this.addItem(i); // ensures i.setPurchaseOrder(this)
            }
        }
    }
        
    public List<GoodsReceivedNote> getGrnList() { return grnList; }
    public void setGrnList(List<GoodsReceivedNote> grnList) { this.grnList = grnList; }
    
    // Business Methods
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Bidirectional relationship management
    public void addItem(PurchaseOrderItem item) {
        if (orderItems == null) {
            orderItems = new ArrayList<>();
        }
        orderItems.add(item);
        item.setPurchaseOrder(this);
    }
    
    public void removeItem(PurchaseOrderItem item) {
        if (orderItems != null) {
            orderItems.remove(item);
            item.setPurchaseOrder(null);
        }
    }
    
    public void calculateTotals() {
        this.subtotal = BigDecimal.ZERO;
        if (this.orderItems != null) {
            for (PurchaseOrderItem item : this.orderItems) {
                this.subtotal = this.subtotal.add(item.getTotalAmount());
            }
        }
        
        this.totalAmount = this.subtotal
            .add(this.taxAmount != null ? this.taxAmount : BigDecimal.ZERO)
            .add(this.shippingCost != null ? this.shippingCost : BigDecimal.ZERO)
            .subtract(this.discountAmount != null ? this.discountAmount : BigDecimal.ZERO);
    }
    
    public boolean canBeApproved() {
        return POStatus.PENDING_APPROVAL.equals(this.status);
    }
    
    public boolean canBeOrdered() {
        return POStatus.APPROVED.equals(this.status);
    }
    
    public boolean canBeReceived() {
        return POStatus.ORDERED.equals(this.status) || 
               POStatus.PARTIALLY_RECEIVED.equals(this.status);
    }
    
    public boolean isFullyReceived() {
        if (this.orderItems == null || this.orderItems.isEmpty()) {
            return false;
        }
        
        for (PurchaseOrderItem item : this.orderItems) {
            if (item.getReceivedQuantity() < item.getOrderedQuantity()) {
                return false;
            }
        }
        return true;
    }
    
    public int getTotalOrderedQuantity() {
        if (this.orderItems == null) return 0;
        return this.orderItems.stream()
            .mapToInt(item -> item.getOrderedQuantity())
            .sum();
    }
    
    public int getTotalReceivedQuantity() {
        if (this.orderItems == null) return 0;
        return this.orderItems.stream()
            .mapToInt(item -> item.getReceivedQuantity())
            .sum();
    }
}
