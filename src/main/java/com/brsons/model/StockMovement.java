package com.brsons.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
public class StockMovement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false)
    private MovementType movementType;
    
    @Column(name = "reason", length = 500)
    private String reason;
    
    @Column(name = "reference_type", length = 100)
    private String referenceType;
    
    @Column(name = "reference_id")
    private Long referenceId;
    
    @Column(name = "before_quantity", nullable = false)
    private Integer beforeQuantity;
    
    @Column(name = "after_quantity", nullable = false)
    private Integer afterQuantity;
    
    @Column(name = "movement_date", nullable = false)
    private LocalDateTime movementDate;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "notes", length = 1000)
    private String notes;
    
    // Enums
    public enum MovementType {
        IN,           // Stock increase (GRN received, purchase, etc.)
        OUT,          // Stock decrease (sale, return, credit note, etc.)
        RESERVE,      // Stock reserved (PO approved, etc.)
        RELEASE,      // Reservation released (PO cancelled, etc.)
        ADJUSTMENT,   // Manual adjustment
        TRANSFER,     // Stock transfer between locations
        DAMAGED,      // Stock marked as damaged
        EXPIRED       // Stock expired
    }
    
    // Constructors
    public StockMovement() {
        this.movementDate = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public MovementType getMovementType() { return movementType; }
    public void setMovementType(MovementType movementType) { this.movementType = movementType; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    
    public Integer getBeforeQuantity() { return beforeQuantity; }
    public void setBeforeQuantity(Integer beforeQuantity) { this.beforeQuantity = beforeQuantity; }
    
    public Integer getAfterQuantity() { return afterQuantity; }
    public void setAfterQuantity(Integer afterQuantity) { this.afterQuantity = afterQuantity; }
    
    public LocalDateTime getMovementDate() { return movementDate; }
    public void setMovementDate(LocalDateTime movementDate) { this.movementDate = movementDate; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    // Helper methods
    public String getMovementTypeDisplay() {
        switch (movementType) {
            case IN: return "Stock In";
            case OUT: return "Stock Out";
            case RESERVE: return "Reserved";
            case RELEASE: return "Released";
            case ADJUSTMENT: return "Adjustment";
            case TRANSFER: return "Transfer";
            case DAMAGED: return "Damaged";
            case EXPIRED: return "Expired";
            default: return movementType.toString();
        }
    }
    
    public String getQuantityDisplay() {
        String prefix = movementType == MovementType.IN || movementType == MovementType.RELEASE ? "+" : "-";
        return prefix + quantity;
    }
}
