package com.brsons.controller;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
        model.addAttribute("accounts", accountRepository.findAll());
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
                              @RequestParam("debitAccount") Long debitAccountId,
                              @RequestParam("creditAccount") Long creditAccountId,
                              @RequestParam("amount") BigDecimal amount) {

        accountingService.createVoucher(date, narration, type, debitAccountId, creditAccountId, amount);
        return "redirect:/admin/vouchers/new?success";
    }
}

