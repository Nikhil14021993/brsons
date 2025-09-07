package com.brsons.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.brsons.model.Account;
import com.brsons.model.User;
import com.brsons.repository.AccountRepository;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/accounts")
public class AccountController {

    @Autowired
    private AccountRepository accountRepository;

    @GetMapping
    public String listAccounts(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/";
        }
        
        List<Account> parentAccounts = accountRepository.findByParentIsNullAndIsActiveTrue();
        model.addAttribute("parentAccounts", parentAccounts);
        model.addAttribute("allAccounts", accountRepository.findAll());
        return "admin-accounts";
    }

    @GetMapping("/new")
    public String newAccountForm(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/";
        }
        
        List<Account> allAccounts = accountRepository.findByIsActiveTrue();
        model.addAttribute("parentAccounts", allAccounts);
        model.addAttribute("accountTypes", List.of("ASSET", "LIABILITY", "INCOME", "EXPENSE", "EQUITY"));
        return "admin-add-account";
    }

    @PostMapping("/save")
    public String saveAccount(@RequestParam("code") String code,
                             @RequestParam("name") String name,
                             @RequestParam("type") String type,
                             @RequestParam(value = "description", required = false) String description,
                             @RequestParam(value = "parentId", required = false) Long parentId,
                             RedirectAttributes redirectAttributes) {
        
        try {
            Account account = new Account();
            account.setCode(code);
            account.setName(name);
            account.setType(type);
            account.setDescription(description);
            account.setActive(true);
            
            if (parentId != null && parentId > 0) {
                Optional<Account> parentOpt = accountRepository.findById(parentId);
                if (parentOpt.isPresent()) {
                    account.setParent(parentOpt.get());
                }
            }
            
            accountRepository.save(account);
            redirectAttributes.addFlashAttribute("success", "Account saved successfully!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error saving account: " + e.getMessage());
        }
        
        return "redirect:/admin/accounts";
    }

    @GetMapping("/edit")
    public String editAccountForm(@RequestParam("id") Long id, Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/";
        }
        
        Optional<Account> accountOpt = accountRepository.findById(id);
        if (accountOpt.isPresent()) {
            model.addAttribute("account", accountOpt.get());
            List<Account> allAccounts = accountRepository.findByIsActiveTrue();
            model.addAttribute("parentAccounts", allAccounts);
            model.addAttribute("accountTypes", List.of("ASSET", "LIABILITY", "INCOME", "EXPENSE", "EQUITY"));
            return "admin-edit-account";
        }
        
        return "redirect:/admin/accounts";
    }

    @PostMapping("/update")
    public String updateAccount(@RequestParam("id") Long id,
                               @RequestParam("code") String code,
                               @RequestParam("name") String name,
                               @RequestParam("type") String type,
                               @RequestParam(value = "description", required = false) String description,
                               @RequestParam(value = "parentId", required = false) Long parentId,
                               RedirectAttributes redirectAttributes) {
        
        try {
            Optional<Account> accountOpt = accountRepository.findById(id);
            if (accountOpt.isPresent()) {
                Account account = accountOpt.get();
                account.setCode(code);
                account.setName(name);
                account.setType(type);
                account.setDescription(description);
                
                if (parentId != null && parentId > 0) {
                    Optional<Account> parentOpt = accountRepository.findById(parentId);
                    if (parentOpt.isPresent()) {
                        account.setParent(parentOpt.get());
                    }
                } else {
                    account.setParent(null);
                }
                
                accountRepository.save(account);
                redirectAttributes.addFlashAttribute("success", "Account updated successfully!");
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating account: " + e.getMessage());
        }
        
        return "redirect:/admin/accounts";
    }

    @PostMapping("/delete")
    public String deleteAccount(@RequestParam("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Optional<Account> accountOpt = accountRepository.findById(id);
            if (accountOpt.isPresent()) {
                Account account = accountOpt.get();
                account.setActive(false);
                accountRepository.save(account);
                redirectAttributes.addFlashAttribute("success", "Account deactivated successfully!");
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting account: " + e.getMessage());
        }
        
        return "redirect:/admin/accounts";
    }

    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return user != null && "Admin".equalsIgnoreCase(user.getType());
    }
}
