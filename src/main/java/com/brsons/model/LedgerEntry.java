package com.brsons.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    @Column(length = 10)
    private String billType;         // Kaccha or Pakka

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;       // total

    private String description;      // e.g. "Sale - INV PK-2025-000123"
    private LocalDateTime createdAt = LocalDateTime.now();

    public LedgerEntry() {}
    public LedgerEntry(Long orderId, String billType, BigDecimal amount, String description) {
        this.orderId = orderId;
        this.billType = billType;
        this.amount = amount;
        this.description = description;
    }

    // getters/setters
    // ...
}
