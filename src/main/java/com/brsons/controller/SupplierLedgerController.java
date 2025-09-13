package com.brsons.controller;

import com.brsons.model.SupplierLedger;
import com.brsons.model.SupplierLedgerEntry;
import com.brsons.service.SupplierLedgerService;
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
@RequestMapping("/admin/supplier-ledger")
public class SupplierLedgerController {
    
    @Autowired
    private SupplierLedgerService supplierLedgerService;
    
    // ==================== DASHBOARD ====================
    
    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        var dashboardData = supplierLedgerService.getSupplierLedgerDashboard();
        model.addAttribute("dashboard", dashboardData);
        
        return "admin-supplier-ledger-dashboard";
    }
    
    // ==================== SUPPLIER LEDGER LIST ====================
    
    @GetMapping("/list")
    public String listSupplierLedgers(Model model, HttpSession session) {
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        List<SupplierLedger> supplierLedgers = supplierLedgerService.getAllActiveSupplierLedgers();
        model.addAttribute("supplierLedgers", supplierLedgers);
        
        return "admin-supplier-ledger-list";
    }
    
    @GetMapping("/outstanding")
    public String outstandingSuppliers(Model model, HttpSession session) {
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        List<SupplierLedger> outstandingSuppliers = supplierLedgerService.getSupplierLedgersWithOutstandingBalance();
        model.addAttribute("supplierLedgers", outstandingSuppliers);
        
        return "admin-supplier-ledger-outstanding";
    }
    
    // ==================== SUPPLIER LEDGER DETAILS ====================
    
    @GetMapping("/{id}")
    public String viewSupplierLedger(@PathVariable Long id, Model model, HttpSession session) {
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        Optional<SupplierLedger> ledgerOpt = supplierLedgerService.getSupplierLedgerById(id);
        if (ledgerOpt.isEmpty()) {
            return "redirect:/admin/supplier-ledger/list";
        }
        
        SupplierLedger ledger = ledgerOpt.get();
        List<SupplierLedgerEntry> entries = supplierLedgerService.getSupplierLedgerEntries(id);
        
        model.addAttribute("ledger", ledger);
        model.addAttribute("entries", entries);
        
        return "admin-supplier-ledger-details";
    }
    
    // ==================== PAYMENT PROCESSING ====================
    
    @GetMapping("/{id}/payment")
    public String paymentForm(@PathVariable Long id, Model model, HttpSession session) {
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        Optional<SupplierLedger> ledgerOpt = supplierLedgerService.getSupplierLedgerById(id);
        if (ledgerOpt.isEmpty()) {
            return "redirect:/admin/supplier-ledger/list";
        }
        
        model.addAttribute("ledger", ledgerOpt.get());
        return "admin-supplier-payment-form";
    }
    
    @PostMapping("/{id}/payment")
    public String processPayment(@PathVariable Long id,
                                @RequestParam BigDecimal paymentAmount,
                                @RequestParam String paymentMethod,
                                @RequestParam(required = false) String paymentReference,
                                @RequestParam(required = false) String notes,
                                RedirectAttributes redirectAttributes,
                                HttpSession session) {
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        try {
            supplierLedgerService.processSupplierPayment(id, paymentAmount, paymentMethod, 
                                                       paymentReference, notes);
            redirectAttributes.addFlashAttribute("success", "Payment processed successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error processing payment: " + e.getMessage());
        }
        
        return "redirect:/admin/supplier-ledger/" + id;
    }
    
    // ==================== SEARCH ====================
    
    @GetMapping("/search")
    public String searchSupplierLedgers(@RequestParam String searchTerm, Model model, HttpSession session) {
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        List<SupplierLedger> supplierLedgers = supplierLedgerService.searchSupplierLedgers(searchTerm);
        model.addAttribute("supplierLedgers", supplierLedgers);
        model.addAttribute("searchTerm", searchTerm);
        
        return "admin-supplier-ledger-list";
    }
    
    // ==================== AJAX ENDPOINTS ====================
    
    @GetMapping("/api/dashboard")
    @ResponseBody
    public SupplierLedgerService.SupplierLedgerDashboard getDashboardData(HttpSession session) {
        Object user = session.getAttribute("user");
        if (user == null) {
            return null;
        }
        
        return supplierLedgerService.getSupplierLedgerDashboard();
    }
    
    @GetMapping("/api/outstanding")
    @ResponseBody
    public List<SupplierLedger> getOutstandingSuppliers(HttpSession session) {
        Object user = session.getAttribute("user");
        if (user == null) {
            return null;
        }
        
        return supplierLedgerService.getSupplierLedgersWithOutstandingBalance();
    }
}
