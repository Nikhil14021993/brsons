package com.brsons.model;

import java.math.BigDecimal;

/**
 * TaxBreakdown class to handle tax calculations for cart and orders
 * Supports both CGST+SGST (intra-state) and IGST (inter-state) tax structures
 */
public class TaxBreakdown {
    
    private double cgstAmount = 0.0;
    private double sgstAmount = 0.0;
    private double igstAmount = 0.0;
    private double totalTaxAmount = 0.0;
    
    // Tax percentages (for display purposes)
    private double cgstPercentage = 0.0;
    private double sgstPercentage = 0.0;
    private double igstPercentage = 0.0;
    
    public TaxBreakdown() {
        // Default constructor
    }
    
    // Add CGST amount
    public void addCGST(BigDecimal percentage, double itemTotal) {
        if (percentage != null && percentage.compareTo(BigDecimal.ZERO) > 0) {
            double cgst = itemTotal * (percentage.doubleValue() / 100.0);
            this.cgstAmount += cgst;
            this.totalTaxAmount += cgst;
            this.cgstPercentage = Math.max(this.cgstPercentage, percentage.doubleValue());
        }
    }
    
    // Add SGST amount
    public void addSGST(BigDecimal percentage, double itemTotal) {
        if (percentage != null && percentage.compareTo(BigDecimal.ZERO) > 0) {
            double sgst = itemTotal * (percentage.doubleValue() / 100.0);
            this.sgstAmount += sgst;
            this.totalTaxAmount += sgst;
            this.sgstPercentage = Math.max(this.sgstPercentage, percentage.doubleValue());
        }
    }
    
    // Add IGST amount
    public void addIGST(BigDecimal percentage, double itemTotal) {
        if (percentage != null && percentage.compareTo(BigDecimal.ZERO) > 0) {
            double igst = itemTotal * (percentage.doubleValue() / 100.0);
            this.igstAmount += igst;
            this.totalTaxAmount += igst;
            this.igstPercentage = Math.max(this.igstPercentage, percentage.doubleValue());
        }
    }
    
    // Getters
    public double getCgstAmount() { return cgstAmount; }
    public double getSgstAmount() { return sgstAmount; }
    public double getIgstAmount() { return igstAmount; }
    public double getTotalTaxAmount() { return totalTaxAmount; }
    
    public double getCgstPercentage() { return cgstPercentage; }
    public double getSgstPercentage() { return sgstPercentage; }
    public double getIgstPercentage() { return igstPercentage; }
    
    // Setters
    public void setCgstAmount(double cgstAmount) { this.cgstAmount = cgstAmount; }
    public void setSgstAmount(double sgstAmount) { this.sgstAmount = sgstAmount; }
    public void setIgstAmount(double igstAmount) { this.igstAmount = igstAmount; }
    public void setTotalTaxAmount(double totalTaxAmount) { this.totalTaxAmount = totalTaxAmount; }
    
    public void setCgstPercentage(double cgstPercentage) { this.cgstPercentage = cgstPercentage; }
    public void setSgstPercentage(double sgstPercentage) { this.sgstPercentage = sgstPercentage; }
    public void setIgstPercentage(double igstPercentage) { this.igstPercentage = igstPercentage; }
    
    // Helper methods
    public boolean isIntraState() {
        return cgstAmount > 0 || sgstAmount > 0;
    }
    
    public boolean isInterState() {
        return igstAmount > 0;
    }
    
    public String getTaxType() {
        if (isIntraState()) {
            return "CGST_SGST";
        } else if (isInterState()) {
            return "IGST";
        } else {
            return "NO_TAX";
        }
    }
    
    @Override
    public String toString() {
        return String.format("TaxBreakdown{CGST: ₹%.2f, SGST: ₹%.2f, IGST: ₹%.2f, Total: ₹%.2f}", 
                           cgstAmount, sgstAmount, igstAmount, totalTaxAmount);
    }
}
