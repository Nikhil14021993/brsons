package com.brsons.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.brsons.dto.BalanceSheetRow;
import com.brsons.dto.PnLRow;
import com.brsons.service.AccountingReportService;

@Controller
@RequestMapping("/accounting")
public class AccountingReportController {

    private final AccountingReportService reportService;

    public AccountingReportController(AccountingReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/balance-sheet-ui")
    public String balanceSheetPage() {
        return "balance_sheet"; // Thymeleaf template
    }

    @GetMapping("/balance-sheet")
    @ResponseBody
    public List<BalanceSheetRow> getBalanceSheet(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reportService.getBalanceSheet(date);
    }

    @GetMapping("/pnl-ui")
    public String pnlPage() {
        return "pnl"; // Thymeleaf template
    }

    @GetMapping("/pnl")
    @ResponseBody
    public List<PnLRow> getPnL(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return reportService.getProfitAndLoss(startDate, endDate);
    }
}

