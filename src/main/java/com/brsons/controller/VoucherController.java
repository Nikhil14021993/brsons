package com.brsons.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.brsons.dto.VoucherEntryDto;
import com.brsons.model.Account;
import com.brsons.model.User;
import com.brsons.repository.AccountRepository;
import com.brsons.service.AccountingService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("admin/vouchers")
public class VoucherController {

    @Autowired
    private AccountingService accountingService;

    @Autowired
    private AccountRepository accountRepository;

    @GetMapping("/new")
    public String newVoucherForm(Model model,  HttpSession session) {
    	if (isAdmin(session)) {
        // First try to get all accounts, then filter active ones
        List<Account> allAccounts = accountRepository.findAll();
        System.out.println("Total accounts in database: " + allAccounts.size());
        
        List<Account> accounts = accountRepository.findByIsActiveTrue();
        System.out.println("Found " + accounts.size() + " active accounts for voucher form");
        
        // If no active accounts, use all accounts
        if (accounts.isEmpty()) {
            accounts = allAccounts;
            System.out.println("No active accounts found, using all accounts: " + accounts.size());
        }
        
        for (Account acc : accounts) {
            System.out.println("Account: " + acc.getName() + " (ID: " + acc.getId() + ", Type: " + acc.getType() + ", Active: " + acc.isActive() + ")");
        }
        model.addAttribute("accounts", accounts);
        return "voucher_form";
    	}
    	return "redirect:/";
    }

    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return user != null && "Admin".equalsIgnoreCase(user.getType());
    }
    @PostMapping("/save")
    public String saveVoucher(@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                              @RequestParam("narration") String narration,
                              @RequestParam("type") String type,
                              @RequestParam Map<String, String> allParams,
                              RedirectAttributes redirectAttributes) {

        try {
            System.out.println("=== VOUCHER SAVE DEBUG ===");
            System.out.println("Date: " + date);
            System.out.println("Narration: " + narration);
            System.out.println("Type: " + type);
            System.out.println("All params: " + allParams);

            // Parse dynamic entries from allParams
            List<VoucherEntryDto> entries = new java.util.ArrayList<>();
            
            // Find all entry indices by looking for accountId parameters
            Set<Integer> entryIndices = new java.util.HashSet<>();
            for (String key : allParams.keySet()) {
                if (key.startsWith("entries[") && key.contains("].accountId")) {
                    String indexStr = key.substring(key.indexOf('[') + 1, key.indexOf(']'));
                    try {
                        entryIndices.add(Integer.parseInt(indexStr));
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid entry index: " + indexStr);
                    }
                }
            }
            
            System.out.println("Found entry indices: " + entryIndices);
            
            for (int i : entryIndices) {
                String accountIdStr = allParams.get("entries[" + i + "].accountId");
                String debitStr = allParams.get("entries[" + i + "].debit");
                String creditStr = allParams.get("entries[" + i + "].credit");
                String description = allParams.get("entries[" + i + "].description");
                
                if (accountIdStr != null && !accountIdStr.trim().isEmpty()) {
                    try {
                        VoucherEntryDto entry = new VoucherEntryDto();
                        entry.setAccountId(Long.parseLong(accountIdStr));
                        
                        // Parse debit amount with safety check
                        if (debitStr != null && !debitStr.trim().isEmpty()) {
                            entry.setDebit(new BigDecimal(debitStr));
                        } else {
                            entry.setDebit(BigDecimal.ZERO);
                        }
                        
                        // Parse credit amount with safety check
                        if (creditStr != null && !creditStr.trim().isEmpty()) {
                            entry.setCredit(new BigDecimal(creditStr));
                        } else {
                            entry.setCredit(BigDecimal.ZERO);
                        }
                        
                        // Set description
                        entry.setDescription(description);
                        
                        System.out.println("Entry " + i + ": Account=" + entry.getAccountId() + 
                                         ", Debit=" + entry.getDebit() + 
                                         ", Credit=" + entry.getCredit() + 
                                         ", Description=" + entry.getDescription());
                        
                        entries.add(entry);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid account ID for entry " + i + ": " + accountIdStr);
                    }
                }
            }
            
            // Filter out entries with zero amounts and no account
            entries = entries.stream()
                .filter(entry -> entry.getAccountId() != null && 
                        (entry.getDebit().compareTo(BigDecimal.ZERO) > 0 || 
                         entry.getCredit().compareTo(BigDecimal.ZERO) > 0))
                .collect(java.util.stream.Collectors.toList());
            
            System.out.println("Filtered entries count: " + entries.size());
            if (entries.isEmpty()) {
                throw new IllegalArgumentException("No valid entries found. Please ensure at least one entry has an account selected and a non-zero amount.");
            }
            
            System.out.println("Total entries to save: " + entries.size());
            accountingService.createVoucherWithEntries(date, narration, type, entries);
            System.out.println("Voucher saved successfully!");
            redirectAttributes.addFlashAttribute("success", "Voucher saved successfully!");
        } catch (Exception e) {
            System.err.println("Error saving voucher: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error saving voucher: " + e.getMessage());
        }
        
        return "redirect:/admin/vouchers/new";
    }
    
    @GetMapping("/test-accounts")
    public String testAccounts(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/";
        }
        
        List<Account> allAccounts = accountRepository.findAll();
        model.addAttribute("accounts", allAccounts);
        return "test-accounts";
    }
    
    @GetMapping("/debug-accounts")
    @ResponseBody
    public List<Account> debugAccounts(HttpSession session) {
        if (!isAdmin(session)) {
            return List.of();
        }
        
        List<Account> allAccounts = accountRepository.findAll();
        System.out.println("Debug: Found " + allAccounts.size() + " accounts");
        for (Account acc : allAccounts) {
            System.out.println("Account: " + acc.getName() + " (ID: " + acc.getId() + ", Type: " + acc.getType() + ")");
        }
        return allAccounts;
    }
    
    @GetMapping("/simple-accounts")
    @ResponseBody
    public List<SimpleAccount> getSimpleAccounts(HttpSession session) {
        if (!isAdmin(session)) {
            return List.of();
        }
        
        List<Account> allAccounts = accountRepository.findAll();
        return allAccounts.stream()
            .map(acc -> new SimpleAccount(acc.getId(), acc.getName(), acc.getType(), 
                acc.getParent() != null ? acc.getParent().getName() : null))
            .collect(java.util.stream.Collectors.toList());
    }
    
    // Simple DTO for account display
    public static class SimpleAccount {
        public Long id;
        public String name;
        public String type;
        public String parentName;
        
        public SimpleAccount(Long id, String name, String type, String parentName) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.parentName = parentName;
        }
    }
}

