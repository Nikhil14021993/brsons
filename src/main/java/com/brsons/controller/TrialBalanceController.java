package com.brsons.controller;

import com.brsons.dto.TrialBalanceRow;
import com.brsons.dto.HierarchicalTrialBalanceRow;
import com.brsons.model.User;
import com.brsons.service.TrialBalanceService;

import jakarta.servlet.http.HttpSession;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/accounting")
public class TrialBalanceController {

    private final TrialBalanceService trialBalanceService;

    public TrialBalanceController(TrialBalanceService trialBalanceService) {
        this.trialBalanceService = trialBalanceService;
    }


    @GetMapping("/trial-balance-tally")
    public String trialBalanceTallyPage(HttpSession session) {
    	if (isAdmin(session)) {
        return "trial_balance_tally"; // trial_balance_tally.html
    	}
    	return "redirect:/";
    }
    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return user != null && "Admin".equalsIgnoreCase(user.getType());
    }

    LocalDate customDate = LocalDate.of(2024, 8, 16);
    
    @GetMapping("/trial-balance")
    @ResponseBody
    public List<TrialBalanceRow> getTrialBalance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, HttpSession session) {
    	if (isAdmin(session)) {
        return trialBalanceService.getTrialBalance(startDate, endDate);
    	}
    	return trialBalanceService.getTrialBalance(customDate, customDate);
    }

    @GetMapping("/hierarchical-trial-balance")
    @ResponseBody
    public List<HierarchicalTrialBalanceRow> getHierarchicalTrialBalance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, 
            HttpSession session) {
    	if (isAdmin(session)) {
        return trialBalanceService.getHierarchicalTrialBalance(startDate, endDate);
    	}
    	return trialBalanceService.getHierarchicalTrialBalance(customDate, customDate);
    }
}
