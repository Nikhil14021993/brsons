package com.brsons.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "credit_note_items")
public class CreditNoteItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_note_id", nullable = false)
    private CreditNote creditNote;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "quantity_type", length = 50)
    private String quantityType; // "ACCEPTED", "RECEIVED", "REJECTED"
    
    @Column(name = "original_grn_item_id")
    private Long originalGrnItemId; // Reference to the GRN item this credit note is based on
    
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
    
    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage;
    
    @Column(name = "tax_percentage", precision = 5, scale = 2)
    private BigDecimal taxPercentage;
    
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "reason", length = 500)
    private String reason;
    
    // Constructors
    public CreditNoteItem() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public CreditNote getCreditNote() { return creditNote; }
    public void setCreditNote(CreditNote creditNote) { this.creditNote = creditNote; }
    
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public String getQuantityType() { return quantityType; }
    public void setQuantityType(String quantityType) { this.quantityType = quantityType; }
    
    public Long getOriginalGrnItemId() { return originalGrnItemId; }
    public void setOriginalGrnItemId(Long originalGrnItemId) { this.originalGrnItemId = originalGrnItemId; }
    
    // Business Methods
    public void calculateTotalAmount() {
        if (this.quantity != null && this.unitPrice != null) {
            BigDecimal subtotal = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
            
            // Apply discount if present
            if (this.discountPercentage != null && this.discountPercentage.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal discountAmount = subtotal.multiply(this.discountPercentage.divide(BigDecimal.valueOf(100)));
                subtotal = subtotal.subtract(discountAmount);
            }
            
            // Apply tax if present
            if (this.taxPercentage != null && this.taxPercentage.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal taxAmount = subtotal.multiply(this.taxPercentage.divide(BigDecimal.valueOf(100)));
                subtotal = subtotal.add(taxAmount);
            }
            
            this.totalAmount = subtotal;
        }
    }
    
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    
    public BigDecimal getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(BigDecimal discountPercentage) { this.discountPercentage = discountPercentage; }
    
    public BigDecimal getTaxPercentage() { return taxPercentage; }
    public void setTaxPercentage(BigDecimal taxPercentage) { this.taxPercentage = taxPercentage; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
