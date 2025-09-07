package com.brsons.dto;

import java.math.BigDecimal;

public class VoucherEntryDto {
    private Long accountId;
    private BigDecimal debit;
    private BigDecimal credit;
    private String description;

    // Constructors
    public VoucherEntryDto() {}

    public VoucherEntryDto(Long accountId, BigDecimal debit, BigDecimal credit, String description) {
        this.accountId = accountId;
        this.debit = debit;
        this.credit = credit;
        this.description = description;
    }

    // Getters and Setters
    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public void setDebit(BigDecimal debit) {
        this.debit = debit;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
