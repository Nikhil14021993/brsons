package com.brsons.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_items")
public class PurchaseOrderItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "ordered_quantity", nullable = false)
    private Integer orderedQuantity;
    
    @Column(name = "received_quantity", nullable = false)
    private Integer receivedQuantity;
    
    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;
    
    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage;
    
    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;
    
    @Column(name = "tax_percentage", precision = 5, scale = 2)
    private BigDecimal taxPercentage;
    
    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount;
    
    @Column(name = "cgst_percentage", precision = 5, scale = 2)
    private BigDecimal cgstPercentage;
    
    @Column(name = "sgst_percentage", precision = 5, scale = 2)
    private BigDecimal sgstPercentage;
    
    @Column(name = "igst_percentage", precision = 5, scale = 2)
    private BigDecimal igstPercentage;
    
    @Column(name = "cgst_amount", precision = 10, scale = 2)
    private BigDecimal cgstAmount;
    
    @Column(name = "sgst_amount", precision = 10, scale = 2)
    private BigDecimal sgstAmount;
    
    @Column(name = "igst_amount", precision = 10, scale = 2)
    private BigDecimal igstAmount;
    
    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;
    
    @Column(name = "notes")
    private String notes;
    
    // Constructors
    public PurchaseOrderItem() {
        this.receivedQuantity = 0;
        this.orderedQuantity = 0;
        this.unitPrice = BigDecimal.ZERO;
        this.discountPercentage = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.taxPercentage = BigDecimal.ZERO;
        this.taxAmount = BigDecimal.ZERO;
        this.cgstPercentage = BigDecimal.ZERO;
        this.sgstPercentage = BigDecimal.ZERO;
        this.igstPercentage = BigDecimal.ZERO;
        this.cgstAmount = BigDecimal.ZERO;
        this.sgstAmount = BigDecimal.ZERO;
        this.igstAmount = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }
    
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    
    public Integer getOrderedQuantity() { return orderedQuantity; }
    public void setOrderedQuantity(Integer orderedQuantity) { this.orderedQuantity = orderedQuantity; }
    
    public Integer getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(Integer receivedQuantity) { this.receivedQuantity = receivedQuantity; }
    
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    
    public BigDecimal getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(BigDecimal discountPercentage) { this.discountPercentage = discountPercentage; }
    
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    
    public BigDecimal getTaxPercentage() { return taxPercentage; }
    public void setTaxPercentage(BigDecimal taxPercentage) { this.taxPercentage = taxPercentage; }
    
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    
    public BigDecimal getCgstPercentage() { return cgstPercentage; }
    public void setCgstPercentage(BigDecimal cgstPercentage) { this.cgstPercentage = cgstPercentage; }
    
    public BigDecimal getSgstPercentage() { return sgstPercentage; }
    public void setSgstPercentage(BigDecimal sgstPercentage) { this.sgstPercentage = sgstPercentage; }
    
    public BigDecimal getIgstPercentage() { return igstPercentage; }
    public void setIgstPercentage(BigDecimal igstPercentage) { this.igstPercentage = igstPercentage; }
    
    public BigDecimal getCgstAmount() { return cgstAmount; }
    public void setCgstAmount(BigDecimal cgstAmount) { this.cgstAmount = cgstAmount; }
    
    public BigDecimal getSgstAmount() { return sgstAmount; }
    public void setSgstAmount(BigDecimal sgstAmount) { this.sgstAmount = sgstAmount; }
    
    public BigDecimal getIgstAmount() { return igstAmount; }
    public void setIgstAmount(BigDecimal igstAmount) { this.igstAmount = igstAmount; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    // Business Methods
    public void calculateTotals() {
        // Calculate base amount
        BigDecimal baseAmount = this.unitPrice.multiply(BigDecimal.valueOf(this.orderedQuantity));
        
        // Calculate discount
        if (this.discountPercentage != null && this.discountPercentage.compareTo(BigDecimal.ZERO) > 0) {
            this.discountAmount = baseAmount.multiply(this.discountPercentage)
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        } else {
            this.discountAmount = BigDecimal.ZERO;
        }
        
        // Calculate amount after discount
        BigDecimal amountAfterDiscount = baseAmount.subtract(this.discountAmount);
        
        // Reset all tax amounts
        this.cgstAmount = BigDecimal.ZERO;
        this.sgstAmount = BigDecimal.ZERO;
        this.igstAmount = BigDecimal.ZERO;
        this.taxAmount = BigDecimal.ZERO;
        
        // Calculate tax based on supplier's tax type
        Supplier supplier = null;
        if (this.purchaseOrder != null) {
            supplier = this.purchaseOrder.getSupplier();
        }
        if (supplier != null && supplier.getTaxType() != null) {
            switch (supplier.getTaxType()) {
                case CGST_SGST:
                    if (this.cgstPercentage != null && this.cgstPercentage.compareTo(BigDecimal.ZERO) > 0) {
                        this.cgstAmount = amountAfterDiscount.multiply(this.cgstPercentage)
                            .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                    }
                    if (this.sgstPercentage != null && this.sgstPercentage.compareTo(BigDecimal.ZERO) > 0) {
                        this.sgstAmount = amountAfterDiscount.multiply(this.sgstPercentage)
                            .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                    }
                    this.taxAmount = this.cgstAmount.add(this.sgstAmount);
                    break;
                case IGST:
                    if (this.igstPercentage != null && this.igstPercentage.compareTo(BigDecimal.ZERO) > 0) {
                        this.igstAmount = amountAfterDiscount.multiply(this.igstPercentage)
                            .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                    }
                    this.taxAmount = this.igstAmount;
                    break;
            }
        } else {
            // Fallback to old tax calculation
            if (this.taxPercentage != null && this.taxPercentage.compareTo(BigDecimal.ZERO) > 0) {
                this.taxAmount = amountAfterDiscount.multiply(this.taxPercentage)
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            }
        }
        
        // Calculate total
        this.totalAmount = amountAfterDiscount.add(this.taxAmount);
    }
    
    public boolean isFullyReceived() {
        return this.receivedQuantity >= this.orderedQuantity;
    }
    
    public boolean isPartiallyReceived() {
        return this.receivedQuantity > 0 && this.receivedQuantity < this.orderedQuantity;
    }
    
    public boolean isNotReceived() {
        return this.receivedQuantity == 0;
    }
    
    public int getRemainingQuantity() {
        return Math.max(0, this.orderedQuantity - this.receivedQuantity);
    }
    
    public BigDecimal getReceivedAmount() {
        if (this.orderedQuantity == 0) return BigDecimal.ZERO;
        return this.totalAmount.multiply(BigDecimal.valueOf(this.receivedQuantity))
            .divide(BigDecimal.valueOf(this.orderedQuantity), 2, java.math.RoundingMode.HALF_UP);
    }
}
