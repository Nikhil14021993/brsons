package com.brsons.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DaybookSummaryDto {
    private int totalEntries;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private LocalDate startDate;
    private LocalDate endDate;

    // Constructors
    public DaybookSummaryDto() {}

    public DaybookSummaryDto(int totalEntries, BigDecimal totalDebits, BigDecimal totalCredits, 
                           LocalDate startDate, LocalDate endDate) {
        this.totalEntries = totalEntries;
        this.totalDebits = totalDebits;
        this.totalCredits = totalCredits;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Getters and Setters
    public int getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(int totalEntries) {
        this.totalEntries = totalEntries;
    }

    public BigDecimal getTotalDebits() {
        return totalDebits;
    }

    public void setTotalDebits(BigDecimal totalDebits) {
        this.totalDebits = totalDebits;
    }

    public BigDecimal getTotalCredits() {
        return totalCredits;
    }

    public void setTotalCredits(BigDecimal totalCredits) {
        this.totalCredits = totalCredits;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getBalance() {
        return totalDebits.subtract(totalCredits);
    }

    public boolean isBalanced() {
        return totalDebits.compareTo(totalCredits) == 0;
    }

    @Override
    public String toString() {
        return "DaybookSummaryDto{" +
                "totalEntries=" + totalEntries +
                ", totalDebits=" + totalDebits +
                ", totalCredits=" + totalCredits +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", balance=" + getBalance() +
                ", isBalanced=" + isBalanced() +
                '}';
    }
}
