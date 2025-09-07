package com.brsons.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.brsons.dto.VoucherEntryDto;
import com.brsons.model.Account;
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
        drEntry.setCredit(BigDecimal.ZERO);
        voucherEntryRepository.save(drEntry);

        // Step 3: Credit Entry
        VoucherEntry crEntry = new VoucherEntry();
        crEntry.setVoucher(voucher);
        crEntry.setAccount(accountRepository.findById(creditAccountId).orElseThrow());
        crEntry.setDebit(BigDecimal.ZERO);
        crEntry.setCredit(amount);
        voucherEntryRepository.save(crEntry);
    }

    @Transactional
    public void createVoucherWithEntries(LocalDate date, String narration, String type,
                                         List<VoucherEntryDto> entries) {
        
        System.out.println("=== ACCOUNTING SERVICE DEBUG ===");
        System.out.println("Date: " + date);
        System.out.println("Narration: " + narration);
        System.out.println("Type: " + type);
        System.out.println("Entries count: " + (entries != null ? entries.size() : "null"));
        
        // Validate entries
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("At least one voucher entry is required");
        }

        // Calculate totals
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        
        for (VoucherEntryDto entry : entries) {
            System.out.println("Processing entry: Account=" + entry.getAccountId() + 
                             ", Debit=" + entry.getDebit() + 
                             ", Credit=" + entry.getCredit());
            
            if (entry.getDebit() != null) {
                totalDebit = totalDebit.add(entry.getDebit());
            }
            if (entry.getCredit() != null) {
                totalCredit = totalCredit.add(entry.getCredit());
            }
        }

        System.out.println("Total Debit: " + totalDebit);
        System.out.println("Total Credit: " + totalCredit);

        // Check if voucher is balanced
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new IllegalArgumentException("Voucher is not balanced. Total Debit: " + 
                totalDebit + ", Total Credit: " + totalCredit);
        }

        // Step 1: Save voucher
        Voucher voucher = new Voucher();
        voucher.setDate(date);
        voucher.setNarration(narration);
        voucher.setType(type);
        Voucher savedVoucher = voucherRepository.save(voucher);
        System.out.println("Voucher saved with ID: " + savedVoucher.getId());

        // Step 2: Create voucher entries
        for (VoucherEntryDto entryDto : entries) {
            if (entryDto.getAccountId() == null) {
                System.out.println("Skipping entry with null account ID");
                continue; // Skip empty entries
            }

            Account account = accountRepository.findById(entryDto.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + entryDto.getAccountId()));

            VoucherEntry voucherEntry = new VoucherEntry();
            voucherEntry.setVoucher(voucher);
            voucherEntry.setAccount(account);
            voucherEntry.setDebit(entryDto.getDebit() != null ? entryDto.getDebit() : BigDecimal.ZERO);
            voucherEntry.setCredit(entryDto.getCredit() != null ? entryDto.getCredit() : BigDecimal.ZERO);
            voucherEntry.setDescription(entryDto.getDescription());
            
            VoucherEntry savedEntry = voucherEntryRepository.save(voucherEntry);
            System.out.println("Voucher entry saved with ID: " + savedEntry.getId() + 
                             " for account: " + account.getName());
        }
        
        System.out.println("Voucher creation completed successfully!");
    }

}
