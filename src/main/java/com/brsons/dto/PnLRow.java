package com.brsons.dto;

import java.math.BigDecimal;

public class PnLRow {
    private String accountName;
    private BigDecimal amount;

    public PnLRow(String accountName, BigDecimal amount) {
        this.accountName = accountName;
        this.amount = amount;
    }
    // getters & setters

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
}