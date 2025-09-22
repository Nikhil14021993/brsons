package com.brsons.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "customer_ledger")
public class CustomerLedger {

    // Tax Type Enum for GST calculation
    public enum TaxType {
        CGST_SGST("CGST + SGST", "Intra-state transactions"),
        IGST("IGST", "Inter-state transactions");
        
        private final String displayName;
        private final String description;
        
        TaxType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
        // Business Configuration - Your business state code (Rajasthan)
        private static final String BUSINESS_STATE_CODE = "08"; // Rajasthan
    
    // Common Indian State Codes
    private static final Map<String, String> STATE_CODES = new HashMap<String, String>() {{
        put("Maharashtra", "27");
        put("Gujarat", "24");
        put("Karnataka", "29");
        put("Tamil Nadu", "33");
        put("Delhi", "07");
        put("West Bengal", "19");
        put("Rajasthan", "08");
        put("Uttar Pradesh", "09");
        put("Madhya Pradesh", "23");
        put("Andhra Pradesh", "28");
        put("Telangana", "36");
        put("Kerala", "32");
        put("Punjab", "03");
        put("Haryana", "06");
    }};
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "customer_name", nullable = false)
    private String customerName;
    
    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;
    
    @Column(name = "customer_email")
    private String customerEmail;
    
    // Address Information for Tax Type Determination
    @Column(name = "address_line1")
    private String addressLine1;
    
    @Column(name = "address_line2")
    private String addressLine2;
    
    @Column(name = "city")
    private String city;
    
    @Column(name = "state")
    private String state;
    
    @Column(name = "state_code")
    private String stateCode;
    
    @Column(name = "zip_code")
    private String zipCode;
    
    @Column(name = "gstin")
    private String gstin;
    
    @Column(name = "opening_balance", precision = 19, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;
    
    @Column(name = "current_balance", precision = 19, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO;
    
    @Column(name = "total_debits", precision = 19, scale = 2)
    private BigDecimal totalDebits = BigDecimal.ZERO;
    
    @Column(name = "total_credits", precision = 19, scale = 2)
    private BigDecimal totalCredits = BigDecimal.ZERO;
    
    @Column(name = "status")
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, SUSPENDED
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructor
    public CustomerLedger() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public CustomerLedger(String customerName, String customerPhone) {
        this();
        this.customerName = customerName;
        this.customerPhone = customerPhone;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCustomerName() {
        return customerName;
    }
    
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    
    public String getCustomerPhone() {
        return customerPhone;
    }
    
    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }
    
    public String getCustomerEmail() {
        return customerEmail;
    }
    
    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }
    
    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }
    
    public void setOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = openingBalance;
    }
    
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }
    
    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
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
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Address Getters & Setters
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
    
    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getState() { return state; }
    public void setState(String state) { 
        this.state = state;
        // Auto-update state code when state is set
        if (state != null && STATE_CODES.containsKey(state)) {
            this.stateCode = STATE_CODES.get(state);
        }
    }
    
    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }
    
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    
    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }
    
    // Business methods
    public void addDebit(BigDecimal amount) {
        this.totalDebits = this.totalDebits.add(amount);
        this.currentBalance = this.currentBalance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }
    
    public void addCredit(BigDecimal amount) {
        this.totalCredits = this.totalCredits.add(amount);
        this.currentBalance = this.currentBalance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean hasOutstandingBalance() {
        return this.currentBalance.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public BigDecimal getOutstandingAmount() {
        return this.currentBalance.max(BigDecimal.ZERO);
    }
    
    // Tax Type Determination Logic
    public TaxType determineTaxType() {
        if (this.stateCode == null || this.stateCode.trim().isEmpty()) {
            return TaxType.CGST_SGST; // Default to intra-state if state code not available
        }
        
        if (BUSINESS_STATE_CODE.equals(this.stateCode)) {
            return TaxType.CGST_SGST; // Same state - intra-state transaction
        } else {
            return TaxType.IGST; // Different state - inter-state transaction
        }
    }
    
    // Helper method to get full address
    public String getFullAddress() {
        StringBuilder address = new StringBuilder();
        if (addressLine1 != null && !addressLine1.trim().isEmpty()) {
            address.append(addressLine1);
        }
        if (addressLine2 != null && !addressLine2.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(addressLine2);
        }
        if (city != null && !city.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(city);
        }
        if (state != null && !state.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(state);
        }
        if (zipCode != null && !zipCode.trim().isEmpty()) {
            if (address.length() > 0) address.append(" - ");
            address.append(zipCode);
        }
        return address.toString();
    }
    
    // Static helper method to get state code from state name
    public static String getStateCodeFromStateName(String stateName) {
        return STATE_CODES.get(stateName);
    }
    
    // Static helper method to get all state codes
    public static Map<String, String> getAllStateCodes() {
        return STATE_CODES;
    }
}
