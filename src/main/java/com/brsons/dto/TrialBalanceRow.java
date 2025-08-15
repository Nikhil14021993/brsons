package com.brsons.dto;

import java.math.BigDecimal;

public class TrialBalanceRow {
    private String accountName;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;

    public TrialBalanceRow(String accountName, BigDecimal totalDebit, BigDecimal totalCredit) {
        this.accountName = accountName;
        this.totalDebit = totalDebit != null ? totalDebit : BigDecimal.ZERO;
        this.totalCredit = totalCredit != null ? totalCredit : BigDecimal.ZERO;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public BigDecimal getTotalDebit() {
        return totalDebit;
    }

    public void setTotalDebit(BigDecimal totalDebit) {
        this.totalDebit = totalDebit;
    }

    public BigDecimal getTotalCredit() {
        return totalCredit;
    }

    public void setTotalCredit(BigDecimal totalCredit) {
        this.totalCredit = totalCredit;
    }
}
