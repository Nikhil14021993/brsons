package com.brsons.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.brsons.model.Voucher;
import com.brsons.model.VoucherEntry;
import com.brsons.repository.AccountRepository;
import com.brsons.repository.VoucherEntryRepository;
import com.brsons.repository.VoucherRepository;

import jakarta.transaction.Transactional;

@Service
public class AccountingService {

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private VoucherEntryRepository voucherEntryRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Transactional
    public void createVoucher(LocalDate date, String narration, String type,
                               Long debitAccountId, Long creditAccountId,
                               BigDecimal amount) {

        // Step 1: Save voucher
        Voucher voucher = new Voucher();
        voucher.setDate(date);
        voucher.setNarration(narration);
        voucher.setType(type);
        voucherRepository.save(voucher);

        // Step 2: Debit Entry
        VoucherEntry drEntry = new VoucherEntry();
        drEntry.setVoucher(voucher);
        drEntry.setAccount(accountRepository.findById(debitAccountId).orElseThrow());
        drEntry.setDebit(amount);
        drEntry.setCredit(null);
        voucherEntryRepository.save(drEntry);

        // Step 3: Credit Entry
        VoucherEntry crEntry = new VoucherEntry();
        crEntry.setVoucher(voucher);
        crEntry.setAccount(accountRepository.findById(creditAccountId).orElseThrow());
        crEntry.setDebit(null);
        crEntry.setCredit(amount);
        voucherEntryRepository.save(crEntry);
    }

}
