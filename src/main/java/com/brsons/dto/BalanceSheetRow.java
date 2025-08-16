package com.brsons.dto;

import java.math.BigDecimal;

public class BalanceSheetRow {
    private String accountName;
    private BigDecimal balance;

    public BalanceSheetRow(String accountName, BigDecimal balance) {
        this.accountName = accountName;
        this.balance = balance;
    }
    // getters & setters

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}
}
