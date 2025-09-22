package com.brsons.service;

import com.brsons.model.Product;
import com.brsons.model.TaxBreakdown;
import com.brsons.model.CartItemDetails;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for calculating taxes based on user location and product tax configuration
 */
@Service
public class TaxCalculationService {
    
    // Business state - Rajasthan (your actual business state)
    private static final String BUSINESS_STATE = "Rajasthan";
    
    /**
     * Calculate tax breakdown for a list of cart items based on user state
     */
    public TaxBreakdown calculateTaxForCart(List<CartItemDetails> cartItems, String userState) {
        System.out.println("=== TAX CALCULATION FOR CART ===");
        System.out.println("User State: '" + userState + "'");
        System.out.println("Cart Items Count: " + cartItems.size());
        
        TaxBreakdown breakdown = new TaxBreakdown();
        
        if (userState == null || userState.trim().isEmpty()) {
            System.out.println("No user state, returning empty breakdown");
            return breakdown; // Return empty breakdown if no state
        }
        
        String taxType = determineTaxType(userState);
        System.out.println("Tax Type for calculation: " + taxType);
        
        for (CartItemDetails item : cartItems) {
            Product product = item.getProduct();
            double itemTotal = item.getTotalPrice();
            
            System.out.println("Processing item: " + product.getProductName() + 
                             ", Total: " + itemTotal + 
                             ", CGST%: " + product.getCgstPercentage() + 
                             ", SGST%: " + product.getSgstPercentage() + 
                             ", IGST%: " + product.getIgstPercentage());
            
            if ("CGST_SGST".equals(taxType)) {
                // Intra-state: CGST + SGST
                breakdown.addCGST(product.getCgstPercentage(), itemTotal);
                breakdown.addSGST(product.getSgstPercentage(), itemTotal);
                System.out.println("Added CGST + SGST for intra-state");
            } else if ("IGST".equals(taxType)) {
                // Inter-state: IGST
                breakdown.addIGST(product.getIgstPercentage(), itemTotal);
                System.out.println("Added IGST for inter-state");
            }
        }
        
        System.out.println("Final Tax Breakdown: " + breakdown);
        System.out.println("=====================================");
        
        return breakdown;
    }
    
    /**
     * Calculate tax breakdown for a single product
     */
    public TaxBreakdown calculateTaxForProduct(Product product, int quantity, String userState) {
        TaxBreakdown breakdown = new TaxBreakdown();
        
        if (userState == null || userState.trim().isEmpty()) {
            return breakdown;
        }
        
        String taxType = determineTaxType(userState);
        double itemTotal = product.getRetailPrice() * quantity; // Use retail price as default
        
        if ("CGST_SGST".equals(taxType)) {
            breakdown.addCGST(product.getCgstPercentage(), itemTotal);
            breakdown.addSGST(product.getSgstPercentage(), itemTotal);
        } else if ("IGST".equals(taxType)) {
            breakdown.addIGST(product.getIgstPercentage(), itemTotal);
        }
        
        return breakdown;
    }
    
    /**
     * Determine tax type based on user state
     */
    public String determineTaxType(String userState) {
        System.out.println("=== TAX TYPE DETERMINATION ===");
        System.out.println("User State: '" + userState + "'");
        System.out.println("Business State: '" + BUSINESS_STATE + "'");
        
        if (userState == null || userState.trim().isEmpty()) {
            System.out.println("Result: UNKNOWN (no user state)");
            return "UNKNOWN";
        }
        
        String trimmedUserState = userState.trim();
        boolean isSameState = BUSINESS_STATE.equalsIgnoreCase(trimmedUserState);
        System.out.println("Trimmed User State: '" + trimmedUserState + "'");
        System.out.println("Is Same State: " + isSameState);
        
        String result = isSameState ? "CGST_SGST" : "IGST";
        System.out.println("Result: " + result);
        System.out.println("================================");
        
        return result;
    }
    
    /**
     * Get business state
     */
    public String getBusinessState() {
        return BUSINESS_STATE;
    }
    
    /**
     * Check if user is in same state as business
     */
    public boolean isIntraStateTransaction(String userState) {
        return BUSINESS_STATE.equalsIgnoreCase(userState);
    }
    
    /**
     * Get tax type display name
     */
    public String getTaxTypeDisplayName(String taxType) {
        switch (taxType) {
            case "CGST_SGST":
                return "CGST + SGST (Intra-state)";
            case "IGST":
                return "IGST (Inter-state)";
            case "UNKNOWN":
                return "Tax will be calculated at checkout";
            default:
                return "Unknown";
        }
    }
    
}