package com.brsons.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DaybookEntryDto {
    private LocalDate date;
    private LocalDateTime time;
    private String transactionType;
    private String transactionId;
    private String referenceNumber;
    private String accountName;
    private String accountCode;
    private String particulars;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private String voucherType;
    private String narration;

    // Constructors
    public DaybookEntryDto() {}

    public DaybookEntryDto(LocalDate date, LocalDateTime time, String transactionType, 
                          String transactionId, String referenceNumber, String accountName, 
                          String accountCode, String particulars, BigDecimal debitAmount, 
                          BigDecimal creditAmount, String voucherType, String narration) {
        this.date = date;
        this.time = time;
        this.transactionType = transactionType;
        this.transactionId = transactionId;
        this.referenceNumber = referenceNumber;
        this.accountName = accountName;
        this.accountCode = accountCode;
        this.particulars = particulars;
        this.debitAmount = debitAmount;
        this.creditAmount = creditAmount;
        this.voucherType = voucherType;
        this.narration = narration;
    }

    // Getters and Setters
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
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

    public String getParticulars() {
        return particulars;
    }

    public void setParticulars(String particulars) {
        this.particulars = particulars;
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = debitAmount;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = creditAmount;
    }

    public String getVoucherType() {
        return voucherType;
    }

    public void setVoucherType(String voucherType) {
        this.voucherType = voucherType;
    }

    public String getNarration() {
        return narration;
    }

    public void setNarration(String narration) {
        this.narration = narration;
    }

    @Override
    public String toString() {
        return "DaybookEntryDto{" +
                "date=" + date +
                ", time=" + time +
                ", transactionType='" + transactionType + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", referenceNumber='" + referenceNumber + '\'' +
                ", accountName='" + accountName + '\'' +
                ", accountCode='" + accountCode + '\'' +
                ", particulars='" + particulars + '\'' +
                ", debitAmount=" + debitAmount +
                ", creditAmount=" + creditAmount +
                ", voucherType='" + voucherType + '\'' +
                ", narration='" + narration + '\'' +
                '}';
    }
}
