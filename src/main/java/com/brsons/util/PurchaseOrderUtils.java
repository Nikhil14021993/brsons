package com.brsons.util;

import com.brsons.model.PurchaseOrder;
import org.springframework.stereotype.Component;

@Component("poUtils")
public class PurchaseOrderUtils {
    
    // Get next status for purchase order
    public static String getNextStatus(PurchaseOrder.POStatus currentStatus) {
        if (currentStatus == null) return "DRAFT";
        
        switch (currentStatus) {
            case DRAFT:
                return "PENDING_APPROVAL";
            case PENDING_APPROVAL:
                return "APPROVED";
            case APPROVED:
                return "ORDERED";
            case ORDERED:
                return "PARTIALLY_RECEIVED";
            case PARTIALLY_RECEIVED:
                return "FULLY_RECEIVED";
            case FULLY_RECEIVED:
                return "CLOSED";
            case CANCELLED:
            case CLOSED:
            default:
                return "DRAFT"; // Reset to draft if terminal state
        }
    }
    
    // Get button class for next status button
    public static String getNextStatusButtonClass(PurchaseOrder.POStatus currentStatus) {
        if (currentStatus == null) return "primary";
        
        switch (currentStatus) {
            case DRAFT:
                return "info";
            case PENDING_APPROVAL:
                return "warning";
            case APPROVED:
                return "success";
            case ORDERED:
                return "primary";
            case PARTIALLY_RECEIVED:
                return "info";
            case FULLY_RECEIVED:
                return "success";
            case CANCELLED:
            case CLOSED:
            default:
                return "secondary";
        }
    }
    
    // Get text for next status button
    public static String getNextStatusText(PurchaseOrder.POStatus currentStatus) {
        if (currentStatus == null) return "Submit for Approval";
        
        switch (currentStatus) {
            case DRAFT:
                return "Submit for Approval";
            case PENDING_APPROVAL:
                return "Approve";
            case APPROVED:
                return "Mark as Ordered";
            case ORDERED:
                return "Mark Partially Received";
            case PARTIALLY_RECEIVED:
                return "Mark Fully Received";
            case FULLY_RECEIVED:
                return "Close PO";
            case CANCELLED:
            case CLOSED:
            default:
                return "Reset to Draft";
        }
    }
}
