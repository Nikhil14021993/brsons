package com.brsons.controller;

import com.brsons.model.CustomerLedger;
import com.brsons.model.CustomerLedgerEntry;
import com.brsons.service.CustomerLedgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/customer-ledger")
public class CustomerLedgerController {
    
    @Autowired
    private CustomerLedgerService customerLedgerService;
    
    // ==================== DASHBOARD ====================
    
    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        var dashboardData = customerLedgerService.getCustomerLedgerDashboard();
        model.addAttribute("dashboard", dashboardData);
        
        return "admin-customer-ledger-dashboard";
    }
    
    // ==================== CUSTOMER LEDGER LIST ====================
    
    @GetMapping("/list")
    public String listCustomerLedgers(Model model, HttpSession session) {
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        List<CustomerLedger> customerLedgers = customerLedgerService.getAllActiveCustomerLedgers();
        model.addAttribute("customerLedgers", customerLedgers);
        
        return "admin-customer-ledger-list";
    }
    
    @GetMapping("/outstanding")
    public String listOutstandingLedgers(Model model, HttpSession session) {
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        List<CustomerLedger> outstandingLedgers = customerLedgerService.getCustomerLedgersWithOutstandingBalance();
        model.addAttribute("outstandingLedgers", outstandingLedgers);
        
        return "admin-customer-ledger-outstanding";
    }
    
    // ==================== CUSTOMER LEDGER DETAILS ====================
    
    @GetMapping("/{id}")
    public String viewCustomerLedger(@PathVariable Long id, Model model, HttpSession session) {
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        Optional<CustomerLedger> customerLedger = customerLedgerService.getCustomerLedgerById(id);
        if (customerLedger.isEmpty()) {
            return "redirect:/admin/customer-ledger/list";
        }
        
        List<CustomerLedgerEntry> entries = customerLedgerService.getCustomerLedgerEntries(id);
        
        model.addAttribute("customerLedger", customerLedger.get());
        model.addAttribute("entries", entries);
        
        return "admin-customer-ledger-detail";
    }
    
    // ==================== SEARCH ====================
    
    @GetMapping("/search")
    public String searchCustomerLedgers(@RequestParam(required = false) String query, Model model, HttpSession session) {
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        if (query == null || query.trim().isEmpty()) {
            // If no query provided, show empty search page
            model.addAttribute("searchResults", List.of());
            model.addAttribute("searchQuery", "");
            return "admin-customer-ledger-search";
        }
        
        List<CustomerLedger> searchResults = customerLedgerService.searchCustomerLedgers(query);
        model.addAttribute("searchResults", searchResults);
        model.addAttribute("searchQuery", query);
        
        return "admin-customer-ledger-search";
    }
    
    // ==================== PAYMENT ENTRY ====================
    
    @GetMapping("/{id}/payment")
    public String showPaymentForm(@PathVariable Long id, Model model, HttpSession session) {
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        Optional<CustomerLedger> customerLedger = customerLedgerService.getCustomerLedgerById(id);
        if (customerLedger.isEmpty()) {
            return "redirect:/admin/customer-ledger/list";
        }
        
        model.addAttribute("customerLedger", customerLedger.get());
        
        return "admin-customer-ledger-payment";
    }
    
    @PostMapping("/{id}/payment")
    public String processPayment(@PathVariable Long id, 
                               @RequestParam BigDecimal amount,
                               @RequestParam String paymentMethod,
                               @RequestParam(required = false) String paymentReference,
                               @RequestParam(required = false) String notes,
                               RedirectAttributes redirectAttributes,
                               HttpSession session) {
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        Optional<CustomerLedger> customerLedger = customerLedgerService.getCustomerLedgerById(id);
        if (customerLedger.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Customer ledger not found");
            return "redirect:/admin/customer-ledger/list";
        }
        
        try {
            customerLedgerService.addPaymentEntry(
                customerLedger.get(), 
                amount, 
                paymentMethod, 
                paymentReference, 
                notes
            );
            
            redirectAttributes.addFlashAttribute("success", "Payment recorded successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error recording payment: " + e.getMessage());
        }
        
        return "redirect:/admin/customer-ledger/" + id;
    }
    
    // ==================== ADJUSTMENT ENTRY ====================
    
    @GetMapping("/{id}/adjustment")
    public String showAdjustmentForm(@PathVariable Long id, Model model, HttpSession session) {
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        Optional<CustomerLedger> customerLedger = customerLedgerService.getCustomerLedgerById(id);
        if (customerLedger.isEmpty()) {
            return "redirect:/admin/customer-ledger/list";
        }
        
        model.addAttribute("customerLedger", customerLedger.get());
        
        return "admin-customer-ledger-adjustment";
    }
    
    @PostMapping("/{id}/adjustment")
    public String processAdjustment(@PathVariable Long id,
                                  @RequestParam BigDecimal amount,
                                  @RequestParam boolean isDebit,
                                  @RequestParam String reason,
                                  @RequestParam(required = false) String notes,
                                  RedirectAttributes redirectAttributes,
                                  HttpSession session) {
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        Optional<CustomerLedger> customerLedger = customerLedgerService.getCustomerLedgerById(id);
        if (customerLedger.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Customer ledger not found");
            return "redirect:/admin/customer-ledger/list";
        }
        
        try {
            customerLedgerService.addAdjustmentEntry(
                customerLedger.get(),
                amount,
                isDebit,
                reason,
                notes
            );
            
            redirectAttributes.addFlashAttribute("success", "Adjustment recorded successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error recording adjustment: " + e.getMessage());
        }
        
        return "redirect:/admin/customer-ledger/" + id;
    }
    
    // ==================== UTILITY ENDPOINTS ====================
    
    @GetMapping("/create-from-b2b-orders")
    @ResponseBody
    public String createFromB2BOrders(HttpSession session) {
        Object user = session.getAttribute("user");
        if (user == null) {
            return "unauthorized";
        }
        
        try {
            // First create customer ledgers
            customerLedgerService.createCustomerLedgersForExistingB2BOrders();
            
            // Then automatically trigger outstanding sync to ensure consistency
            // This ensures that when customer ledgers are created, outstanding items are also synced
            customerLedgerService.syncOutstandingItemsForB2BOrders();
            
            return "Customer ledgers created successfully from existing B2B orders. Outstanding items have also been synchronized for consistency.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
