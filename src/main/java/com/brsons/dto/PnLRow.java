package com.brsons.dto;

import java.math.BigDecimal;

public class PnLRow {
    private String accountName;
    private String accountCode;
    private BigDecimal amount;
    private String accountType; // REVENUE, EXPENSE
    private int level; // 0 = main account, 1 = sub-account
    private boolean isTotal; // true for total rows
    private boolean isSubtotal; // true for subtotal rows
    private String parentAccount; // for grouping

    public PnLRow(String accountName, BigDecimal amount) {
        this.accountName = accountName;
        this.amount = amount;
        this.level = 0;
        this.isTotal = false;
        this.isSubtotal = false;
    }

    public PnLRow(String accountName, String accountCode, BigDecimal amount, String accountType, int level) {
        this.accountName = accountName;
        this.accountCode = accountCode;
        this.amount = amount;
        this.accountType = accountType;
        this.level = level;
        this.isTotal = false;
        this.isSubtotal = false;
    }

    // Static factory methods for special rows
    public static PnLRow createTotalRow(String label, BigDecimal amount) {
        PnLRow row = new PnLRow(label, amount);
        row.setTotal(true);
        return row;
    }

    public static PnLRow createSubtotalRow(String label, BigDecimal amount) {
        PnLRow row = new PnLRow(label, amount);
        row.setSubtotal(true);
        return row;
    }

    // getters & setters
    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean isTotal() {
        return isTotal;
    }

    public void setTotal(boolean total) {
        isTotal = total;
    }

    public boolean isSubtotal() {
        return isSubtotal;
    }

    public void setSubtotal(boolean subtotal) {
        isSubtotal = subtotal;
    }

    public String getParentAccount() {
        return parentAccount;
    }

    public void setParentAccount(String parentAccount) {
        this.parentAccount = parentAccount;
    }

    // Helper method to get formatted amount
    public String getFormattedAmount() {
        if (amount == null) return "0.00";
        return String.format("%.2f", amount);
    }

    // Helper method to get indented account name
    public String getIndentedName() {
        String indent = "  ".repeat(level);
        return indent + accountName;
    }
}