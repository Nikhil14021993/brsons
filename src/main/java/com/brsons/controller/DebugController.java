package com.brsons.controller;

import com.brsons.model.Account;
import com.brsons.model.Voucher;
import com.brsons.model.VoucherEntry;
import com.brsons.repository.AccountRepository;
import com.brsons.repository.VoucherRepository;
import com.brsons.repository.VoucherEntryRepository;
import com.brsons.service.TrialBalanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/debug")
public class DebugController {

    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private VoucherRepository voucherRepository;
    
    @Autowired
    private VoucherEntryRepository voucherEntryRepository;
    
    @Autowired
    private TrialBalanceService trialBalanceService;

    @GetMapping("/accounts")
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    @GetMapping("/vouchers")
    public List<Voucher> getAllVouchers() {
        return voucherRepository.findAll();
    }

    @GetMapping("/voucher-entries")
    public List<VoucherEntry> getAllVoucherEntries() {
        return voucherEntryRepository.findAll();
    }

    @GetMapping("/trial-balance-test")
    public Object testTrialBalance() {
        try {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 12, 31);
            return trialBalanceService.getHierarchicalTrialBalance(startDate, endDate);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
