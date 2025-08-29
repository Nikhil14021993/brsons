package com.brsons.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "grn_items")
public class GRNItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grn_id", nullable = false)
    private GoodsReceivedNote grn;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "ordered_quantity", nullable = false)
    private Integer orderedQuantity;
    
    @Column(name = "received_quantity", nullable = false)
    private Integer receivedQuantity;
    
    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage;
    
    @Column(name = "tax_percentage", precision = 5, scale = 2)
    private BigDecimal taxPercentage;
    
    public BigDecimal getTaxPercentage() {
		return taxPercentage;
	}

	public void setTaxPercentage(BigDecimal taxPercentage) {
		this.taxPercentage = taxPercentage;
	}

	public BigDecimal getDiscountPercentage() {
		return discountPercentage;
	}

	public void setDiscountPercentage(BigDecimal discountPercentage) {
		this.discountPercentage = discountPercentage;
	}

	@Column(name = "accepted_quantity", nullable = false)
    private Integer acceptedQuantity;
    
    @Column(name = "rejected_quantity", nullable = false)
    private Integer rejectedQuantity;
    
    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;
    
    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;
    
    @Column(name = "quality_status")
    private String qualityStatus;
    
    @Column(name = "rejection_reason")
    private String rejectionReason;
    
    @Column(name = "notes")
    private String notes;
    
    // Constructors
    public GRNItem() {
        this.orderedQuantity = 0;
        this.receivedQuantity = 0;
        this.acceptedQuantity = 0;
        this.rejectedQuantity = 0;
        this.unitPrice = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public GoodsReceivedNote getGrn() { return grn; }
    public void setGrn(GoodsReceivedNote grn) { this.grn = grn; }
    
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    
    
    public Integer getOrderedQuantity() { return orderedQuantity; }
    public void setOrderedQuantity(Integer orderedQuantity) { this.orderedQuantity = orderedQuantity; }
    
    public Integer getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(Integer receivedQuantity) { this.receivedQuantity = receivedQuantity; }
    
    public Integer getAcceptedQuantity() { return acceptedQuantity; }
    public void setAcceptedQuantity(Integer acceptedQuantity) { this.acceptedQuantity = acceptedQuantity; }
    
    public Integer getRejectedQuantity() { return rejectedQuantity; }
    public void setRejectedQuantity(Integer rejectedQuantity) { this.rejectedQuantity = rejectedQuantity; }
    
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public String getQualityStatus() { return qualityStatus; }
    public void setQualityStatus(String qualityStatus) { this.qualityStatus = qualityStatus; }
    
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    // Business Methods
    public void calculateTotals() {
        this.totalAmount = this.unitPrice.multiply(BigDecimal.valueOf(this.acceptedQuantity));
    }
    
    public boolean isFullyAccepted() {
        return this.acceptedQuantity >= this.receivedQuantity;
    }
    
    public boolean hasRejections() {
        return this.rejectedQuantity > 0;
    }
    
    public boolean isPartiallyAccepted() {
        return this.acceptedQuantity > 0 && this.acceptedQuantity < this.receivedQuantity;
    }
    
    public int getRemainingToReceive() {
        return Math.max(0, this.orderedQuantity - this.receivedQuantity);
    }
    
    public void updateQuantities(int received, int accepted, int rejected) {
        this.receivedQuantity = received;
        this.acceptedQuantity = accepted;
        this.rejectedQuantity = rejected;
        this.calculateTotals();
    }
    
    public boolean validateQuantities() {
        return this.receivedQuantity == (this.acceptedQuantity + this.rejectedQuantity);
    }
}
