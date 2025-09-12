package com.brsons.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.brsons.dto.BalanceSheetRow;
import com.brsons.dto.PnLRow;
import com.brsons.model.User;
import com.brsons.service.AccountingReportService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/accounting")
public class AccountingReportController {

    private final AccountingReportService reportService;

    public AccountingReportController(AccountingReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/balance-sheet-ui")
    public String balanceSheetPage(HttpSession session) {
    	if (isAdmin(session)) {
        return "balance_sheet"; // Thymeleaf template
    	}
    	return "redirect:/";
    }

    @GetMapping("/balance-sheet")
    @ResponseBody
    public List<BalanceSheetRow> getBalanceSheet(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reportService.getBalanceSheet(date);
    }

    @GetMapping("/pnl-ui")
    public String pnlPage(HttpSession session) {
    	if (isAdmin(session)) {
        return "pnl"; // Thymeleaf template
    	}
    	return "redirect:/";
    }
    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return user != null && "Admin".equalsIgnoreCase(user.getType());
    }
    
    @GetMapping("/pnl")
    @ResponseBody
    public List<PnLRow> getPnL(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return reportService.getProfitAndLoss(startDate, endDate);
    }

    @GetMapping("/pnl-with-stock")
    @ResponseBody
    public List<PnLRow> getPnLWithStock(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return reportService.getProfitAndLossWithStock(startDate, endDate);
    }

    @GetMapping("/diagnostic")
    @ResponseBody
    public Map<String, Object> getDiagnostic(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();
        
        // Get all account balances
        List<Object[]> allBalances = reportService.getAllAccountBalances(startDate, endDate);
        result.put("allAccountBalances", allBalances);
        
        // Get revenue/expense accounts
        List<Object[]> revenueExpenseAccounts = reportService.getRevenueExpenseAccounts();
        result.put("revenueExpenseAccounts", revenueExpenseAccounts);
        
        // Get P&L data
        List<PnLRow> pnlData = reportService.getProfitAndLoss(startDate, endDate);
        result.put("pnlData", pnlData);
        
        return result;
    }
}

