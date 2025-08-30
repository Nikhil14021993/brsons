package com.brsons.controller;

import com.brsons.model.Outstanding;
import com.brsons.service.OutstandingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/outstanding")
public class OutstandingController {
    
    @Autowired
    private OutstandingService outstandingService;
    
    // ==================== OUTSTANDING DASHBOARD ====================
    
    @GetMapping("/dashboard")
    public String outstandingDashboard(Model model, HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Get outstanding dashboard data for retail orders (Pakka bill type)
        Map<String, Object> dashboard = outstandingService.getOutstandingDashboard();
        
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("user", user);
        
        return "admin-outstanding-dashboard";
    }
    
    @GetMapping("/dashboard/b2b")
    public String b2bOutstandingDashboard(Model model, HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Get B2B outstanding dashboard data (Kaccha orders + Purchase Orders)
        Map<String, Object> dashboard = outstandingService.getB2BOutstandingDashboard();
        
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("user", user);
        
        return "admin-outstanding-b2b-dashboard";
    }
    
    // ==================== OUTSTANDING LISTING ====================
    
    @GetMapping("/list")
    public String outstandingList(@RequestParam(required = false) String type,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(required = false) String search,
                                 @RequestParam(required = false) BigDecimal minAmount,
                                 @RequestParam(required = false) BigDecimal maxAmount,
                                 Model model, HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Convert string parameters to enums
        Outstanding.OutstandingType outstandingType = null;
        Outstanding.OutstandingStatus outstandingStatus = null;
        
        if (type != null && !type.isEmpty()) {
            try {
                outstandingType = Outstanding.OutstandingType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid type, ignore
            }
        }
        
        if (status != null && !status.isEmpty()) {
            try {
                outstandingStatus = Outstanding.OutstandingStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status, ignore
            }
        }
        
        // Search outstanding items
        List<Outstanding> outstandingItems = outstandingService.searchOutstandingItems(
            search, outstandingType, outstandingStatus, minAmount, maxAmount);
        
        // Get summary statistics
        Map<String, Object> dashboard = outstandingService.getOutstandingDashboard();
        
        model.addAttribute("outstandingItems", outstandingItems);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("searchTerm", search);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("minAmount", minAmount);
        model.addAttribute("maxAmount", maxAmount);
        model.addAttribute("user", user);
        
        return "admin-outstanding-list";
    }
    
    // ==================== OUTSTANDING BY TYPE ====================
    
    @GetMapping("/receivables")
    public String receivablesList(Model model, HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Get retail receivables (Pakka orders only)
        List<Outstanding> receivables = outstandingService.getRetailReceivables();
        Map<String, Object> dashboard = outstandingService.getOutstandingDashboard();
        
        model.addAttribute("receivables", receivables);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("user", user);
        model.addAttribute("isRetail", true);
        
        return "admin-outstanding-receivables";
    }
    
    @GetMapping("/receivables/b2b")
    public String b2bReceivablesList(Model model, HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Get B2B receivables (Kaccha orders only)
        List<Outstanding> b2bReceivables = outstandingService.getB2BReceivables();
        Map<String, Object> dashboard = outstandingService.getB2BOutstandingDashboard();
        
        model.addAttribute("receivables", b2bReceivables);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("user", user);
        model.addAttribute("isB2B", true);
        
        return "admin-outstanding-b2b-receivables";
    }
    
    @GetMapping("/payables")
    public String payablesList(Model model, HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Get both INVOICE_PAYABLE and PURCHASE_ORDER items since both represent money we owe
        List<Outstanding> invoicePayables = outstandingService.getOutstandingByType(Outstanding.OutstandingType.INVOICE_PAYABLE);
        List<Outstanding> purchaseOrders = outstandingService.getOutstandingByType(Outstanding.OutstandingType.PURCHASE_ORDER);
        
        // Combine both lists
        List<Outstanding> payables = new ArrayList<>();
        payables.addAll(invoicePayables);
        payables.addAll(purchaseOrders);
        
        Map<String, Object> dashboard = outstandingService.getOutstandingDashboard();
        
        model.addAttribute("payables", payables);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("user", user);
        
        // Add specific counts for payables
        model.addAttribute("totalPayables", payables.size());
        model.addAttribute("invoicePayablesCount", invoicePayables.size());
        model.addAttribute("purchaseOrderCount", purchaseOrders.size());
        
        return "admin-outstanding-payables";
    }
    
    @GetMapping("/payables/b2b")
    public String b2bPayablesList(Model model, HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Get B2B payables (Purchase Orders only)
        List<Outstanding> b2bPayables = outstandingService.getB2BPayables();
        Map<String, Object> dashboard = outstandingService.getB2BOutstandingDashboard();
        
        model.addAttribute("payables", b2bPayables);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("user", user);
        model.addAttribute("isB2B", true);
        
        return "admin-outstanding-b2b-payables";
    }
    
    @GetMapping("/purchase-orders")
    public String purchaseOrdersList(Model model, HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        List<Outstanding> pos = outstandingService.getOutstandingByType(Outstanding.OutstandingType.PURCHASE_ORDER);
        Map<String, Object> dashboard = outstandingService.getOutstandingDashboard();
        
        model.addAttribute("purchaseOrders", pos);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("user", user);
        
        return "admin-outstanding-purchase-orders";
    }
    
    // ==================== OVERDUE ITEMS ====================
    
    @GetMapping("/overdue")
    public String overdueList(Model model, HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        List<Outstanding> overdueItems = outstandingService.getOverdueItems();
        Map<String, Object> dashboard = outstandingService.getOutstandingDashboard();
        
        model.addAttribute("overdueItems", overdueItems);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("user", user);
        
        return "admin-outstanding-overdue";
    }
    
    @GetMapping("/due-soon")
    public String dueSoonList(@RequestParam(defaultValue = "7") int days, Model model, HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        List<Outstanding> dueSoonItems = outstandingService.getItemsDueWithinDays(days);
        Map<String, Object> dashboard = outstandingService.getOutstandingDashboard();
        
        model.addAttribute("dueSoonItems", dueSoonItems);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("days", days);
        model.addAttribute("user", user);
        
        return "admin-outstanding-due-soon";
    }
    
    // ==================== OUTSTANDING ACTIONS ====================
    
    @PostMapping("/update-status/{id}")
    @ResponseBody
    public String updateStatus(@PathVariable Long id, 
                              @RequestParam String newStatus,
                              @RequestParam(required = false) String notes,
                              HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "unauthorized";
        }
        
        try {
            Outstanding.OutstandingStatus status = Outstanding.OutstandingStatus.valueOf(newStatus.toUpperCase());
            outstandingService.updateOutstandingStatus(id, status, notes);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
    
    @PostMapping("/mark-partially-paid/{id}")
    @ResponseBody
    public String markPartiallyPaid(@PathVariable Long id,
                                   @RequestParam BigDecimal paidAmount,
                                   @RequestParam(required = false) String notes,
                                   @RequestParam String paymentMethod,
                                   @RequestParam(required = false) String paymentReference,
                                   HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "unauthorized";
        }
        
        try {
            outstandingService.markPartiallyPaid(id, paidAmount, notes, paymentMethod, paymentReference);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
    
    @PostMapping("/mark-settled/{id}")
    @ResponseBody
    public String markSettled(@PathVariable Long id,
                             @RequestParam(required = false) String notes,
                             @RequestParam String paymentMethod,
                             @RequestParam(required = false) String paymentReference,
                             HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "unauthorized";
        }
        
        try {
            outstandingService.markAsSettled(id, notes, paymentMethod, paymentReference);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
    
    // ==================== UTILITY ENDPOINTS ====================
    
    @PostMapping("/create-existing")
    public String createOutstandingForExistingItems(HttpSession session, RedirectAttributes redirectAttributes) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        try {
            outstandingService.createOutstandingForExistingItems();
            redirectAttributes.addFlashAttribute("successMessage", "Outstanding items created successfully for existing retail orders (Pakka) and POs!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating outstanding items: " + e.getMessage());
        }
        
        return "redirect:/admin/outstanding/dashboard";
    }
    
    @PostMapping("/create-b2b-existing")
    public String createB2BOutstandingForExistingItems(HttpSession session, RedirectAttributes redirectAttributes) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        try {
            outstandingService.createB2BOutstandingForExistingItems();
            redirectAttributes.addFlashAttribute("successMessage", "B2B outstanding items created successfully for existing Kaccha orders and POs!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating B2B outstanding items: " + e.getMessage());
        }
        
        return "redirect:/admin/outstanding/dashboard/b2b";
    }
    
    @GetMapping("/export")
    public String exportOutstanding(@RequestParam(required = false) String type,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) String format,
                                   Model model, HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // This will be implemented later for Excel/CSV export
        model.addAttribute("user", user);
        model.addAttribute("exportType", type);
        model.addAttribute("exportStatus", status);
        model.addAttribute("exportFormat", format);
        
        return "admin-outstanding-export";
    }
    
    // ==================== DEBUG ENDPOINTS ====================
    
    @GetMapping("/debug/accounts")
    @ResponseBody
    public String debugAccounts(HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "unauthorized";
        }
        
        try {
            return outstandingService.debugAccounts();
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
    
    @GetMapping("/debug/create-accounts")
    @ResponseBody
    public String createDefaultAccounts(HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "unauthorized";
        }
        
        try {
            outstandingService.createDefaultAccountsIfNeeded();
            return "Default accounts created/verified successfully. Check console for details.";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
}
