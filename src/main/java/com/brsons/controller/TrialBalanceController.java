package com.brsons.controller;

import com.brsons.dto.TrialBalanceRow;
import com.brsons.service.TrialBalanceService;
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

    @GetMapping("/trial-balance-ui")
    public String trialBalancePage() {
        return "trial_balance"; // trial_balance.html
    }

    @GetMapping("/trial-balance")
    @ResponseBody
    public List<TrialBalanceRow> getTrialBalance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return trialBalanceService.getTrialBalance(startDate, endDate);
    }
}
