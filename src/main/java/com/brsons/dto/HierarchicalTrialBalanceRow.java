package com.brsons.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class HierarchicalTrialBalanceRow {
    private Long accountId;
    private String accountName;
    private String accountCode;
    private String accountType;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private boolean isParentAccount;
    private boolean isExpanded;
    private int level; // 0 for parent, 1 for sub-account, etc.
    private List<HierarchicalTrialBalanceRow> subAccounts;
    private Long parentAccountId;

    public HierarchicalTrialBalanceRow() {
        this.subAccounts = new ArrayList<>();
        this.totalDebit = BigDecimal.ZERO;
        this.totalCredit = BigDecimal.ZERO;
        this.isExpanded = false;
        this.level = 0;
    }

    public HierarchicalTrialBalanceRow(Long accountId, String accountName, String accountCode, 
                                     String accountType, BigDecimal totalDebit, BigDecimal totalCredit, 
                                     boolean isParentAccount, int level, Long parentAccountId) {
        this();
        this.accountId = accountId;
        this.accountName = accountName;
        this.accountCode = accountCode;
        this.accountType = accountType;
        this.totalDebit = totalDebit != null ? totalDebit : BigDecimal.ZERO;
        this.totalCredit = totalCredit != null ? totalCredit : BigDecimal.ZERO;
        this.isParentAccount = isParentAccount;
        this.level = level;
        this.parentAccountId = parentAccountId;
    }

    // Getters and Setters
    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

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

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
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

    public boolean isParentAccount() {
        return isParentAccount;
    }

    public void setParentAccount(boolean parentAccount) {
        isParentAccount = parentAccount;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public List<HierarchicalTrialBalanceRow> getSubAccounts() {
        return subAccounts;
    }

    public void setSubAccounts(List<HierarchicalTrialBalanceRow> subAccounts) {
        this.subAccounts = subAccounts;
    }

    public Long getParentAccountId() {
        return parentAccountId;
    }

    public void setParentAccountId(Long parentAccountId) {
        this.parentAccountId = parentAccountId;
    }

    // Helper methods
    public void addSubAccount(HierarchicalTrialBalanceRow subAccount) {
        this.subAccounts.add(subAccount);
        subAccount.setLevel(this.level + 1);
    }

    public BigDecimal getTotalDebitIncludingSubs() {
        BigDecimal total = this.totalDebit;
        for (HierarchicalTrialBalanceRow sub : subAccounts) {
            total = total.add(sub.getTotalDebitIncludingSubs());
        }
        return total;
    }

    public BigDecimal getTotalCreditIncludingSubs() {
        BigDecimal total = this.totalCredit;
        for (HierarchicalTrialBalanceRow sub : subAccounts) {
            total = total.add(sub.getTotalCreditIncludingSubs());
        }
        return total;
    }

    public String getDisplayName() {
        if (isParentAccount) {
            return accountName;
        } else {
            return "  ".repeat(level) + "└─ " + accountName;
        }
    }

    public String getIndentedName() {
        return "  ".repeat(level) + accountName;
    }
}
