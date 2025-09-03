package com.brsons.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_settlements")
public class InvoiceSettlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "invoice_number", nullable = false)
    private String invoiceNumber;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @Column(name = "payment_entry_id", nullable = false)
    private Long paymentEntryId;

    @Column(name = "settlement_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal settlementAmount;

    @Column(name = "settlement_date", nullable = false)
    private LocalDateTime settlementDate;

    @Column(name = "is_full_settlement", nullable = false)
    private Boolean isFullSettlement = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public Long getPaymentEntryId() {
        return paymentEntryId;
    }

    public void setPaymentEntryId(Long paymentEntryId) {
        this.paymentEntryId = paymentEntryId;
    }

    public BigDecimal getSettlementAmount() {
        return settlementAmount;
    }

    public void setSettlementAmount(BigDecimal settlementAmount) {
        this.settlementAmount = settlementAmount;
    }

    public LocalDateTime getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDateTime settlementDate) {
        this.settlementDate = settlementDate;
    }

    public Boolean getIsFullSettlement() {
        return isFullSettlement;
    }

    public void setIsFullSettlement(Boolean isFullSettlement) {
        this.isFullSettlement = isFullSettlement;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
