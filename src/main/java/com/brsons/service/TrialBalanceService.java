package com.brsons.service;

import com.brsons.dto.TrialBalanceRow;
import com.brsons.dto.HierarchicalTrialBalanceRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class TrialBalanceService {

    @PersistenceContext
    private EntityManager entityManager;

    public List<TrialBalanceRow> getTrialBalance(LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = entityManager.createNativeQuery(
            "SELECT a.name, " +
            "       COALESCE(SUM(CASE WHEN v.date IS NULL OR v.date BETWEEN :startDate AND :endDate THEN e.debit ELSE 0 END), 0), " +
            "       COALESCE(SUM(CASE WHEN v.date IS NULL OR v.date BETWEEN :startDate AND :endDate THEN e.credit ELSE 0 END), 0) " +
            "FROM account a " +
            "LEFT JOIN voucher_entry e ON e.account_id = a.id " +
            "LEFT JOIN voucher v ON e.voucher_id = v.id " +
            "WHERE a.is_active = true " +
            "GROUP BY a.name")
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();

        return results.stream()
                .map(r -> new TrialBalanceRow(
                        (String) r[0],
                        toBigDecimal(r[1]),
                        toBigDecimal(r[2])
                ))
                .toList();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return BigDecimal.ZERO;
    }

    public List<HierarchicalTrialBalanceRow> getHierarchicalTrialBalance(LocalDate startDate, LocalDate endDate) {
        System.out.println("=== HIERARCHICAL TRIAL BALANCE DEBUG ===");
        System.out.println("Start Date: " + startDate);
        System.out.println("End Date: " + endDate);
        
        // Get all accounts with their balances using native SQL
        List<Object[]> results = entityManager.createNativeQuery(
            "SELECT a.id, a.name, a.code, a.type, a.parent_id, " +
            "       COALESCE(SUM(CASE WHEN v.date IS NULL OR v.date BETWEEN :startDate AND :endDate THEN e.debit ELSE 0 END), 0), " +
            "       COALESCE(SUM(CASE WHEN v.date IS NULL OR v.date BETWEEN :startDate AND :endDate THEN e.credit ELSE 0 END), 0) " +
            "FROM account a " +
            "LEFT JOIN voucher_entry e ON e.account_id = a.id " +
            "LEFT JOIN voucher v ON e.voucher_id = v.id " +
            "WHERE a.is_active = true " +
            "GROUP BY a.id, a.name, a.code, a.type, a.parent_id " +
            "ORDER BY a.code")
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();

        System.out.println("Query results count: " + results.size());
        for (Object[] row : results) {
            System.out.println("Account: " + row[1] + " (ID: " + row[0] + "), Debit: " + row[5] + ", Credit: " + row[6]);
        }

        // Create account balance map
        Map<Long, HierarchicalTrialBalanceRow> accountMap = new HashMap<>();
        List<HierarchicalTrialBalanceRow> parentAccounts = new ArrayList<>();

        for (Object[] row : results) {
            Long accountId = (Long) row[0];
            String accountName = (String) row[1];
            String accountCode = (String) row[2];
            String accountType = (String) row[3];
            Long parentId = (Long) row[4];
            BigDecimal debit = toBigDecimal(row[5]);
            BigDecimal credit = toBigDecimal(row[6]);

            HierarchicalTrialBalanceRow accountRow = new HierarchicalTrialBalanceRow(
                accountId, accountName, accountCode, accountType, 
                debit, credit, parentId == null, 0, parentId
            );

            accountMap.put(accountId, accountRow);

            if (parentId == null) {
                parentAccounts.add(accountRow);
            }
        }

        // Build hierarchy - process all accounts and build the tree structure
        for (HierarchicalTrialBalanceRow account : accountMap.values()) {
            if (account.getParentAccountId() != null) {
                HierarchicalTrialBalanceRow parent = accountMap.get(account.getParentAccountId());
                if (parent != null) {
                    parent.addSubAccount(account);
                    System.out.println("Added " + account.getAccountName() + " as child of " + parent.getAccountName());
                } else {
                    System.out.println("Parent not found for account: " + account.getAccountName() + " (Parent ID: " + account.getParentAccountId() + ")");
                }
            }
        }
        
        // Sort sub-accounts within each parent
        for (HierarchicalTrialBalanceRow parent : accountMap.values()) {
            if (parent.getSubAccounts() != null && !parent.getSubAccounts().isEmpty()) {
                parent.getSubAccounts().sort((a, b) -> a.getAccountCode().compareTo(b.getAccountCode()));
                System.out.println("Sorted sub-accounts for " + parent.getAccountName() + ": " + 
                    parent.getSubAccounts().stream().map(HierarchicalTrialBalanceRow::getAccountName).toList());
            }
        }

        // Calculate hierarchical totals for all parent accounts
        calculateHierarchicalTotals(parentAccounts);

        // Sort parent accounts by type and code
        parentAccounts.sort((a, b) -> {
            int typeComparison = a.getAccountType().compareTo(b.getAccountType());
            if (typeComparison != 0) return typeComparison;
            return a.getAccountCode().compareTo(b.getAccountCode());
        });

        System.out.println("Final parent accounts count: " + parentAccounts.size());
        for (HierarchicalTrialBalanceRow parent : parentAccounts) {
            System.out.println("Parent: " + parent.getAccountName() + " (Type: " + parent.getAccountType() + 
                             ", Debit: " + parent.getTotalDebit() + ", Credit: " + parent.getTotalCredit() + 
                             ", SubAccounts: " + parent.getSubAccounts().size() + ")");
        }

        return parentAccounts;
    }

    private void calculateHierarchicalTotals(List<HierarchicalTrialBalanceRow> parentAccounts) {
        System.out.println("=== CALCULATING HIERARCHICAL TOTALS ===");
        
        for (HierarchicalTrialBalanceRow parent : parentAccounts) {
            calculateAccountTotals(parent);
            System.out.println("Calculated totals for " + parent.getAccountName() + 
                             ": Debit=" + parent.getTotalDebit() + ", Credit=" + parent.getTotalCredit());
        }
    }

    private void calculateAccountTotals(HierarchicalTrialBalanceRow account) {
        BigDecimal totalDebit = account.getTotalDebit();
        BigDecimal totalCredit = account.getTotalCredit();
        
        // Add totals from all sub-accounts recursively
        if (account.getSubAccounts() != null && !account.getSubAccounts().isEmpty()) {
            for (HierarchicalTrialBalanceRow subAccount : account.getSubAccounts()) {
                // Recursively calculate totals for sub-accounts first
                calculateAccountTotals(subAccount);
                
                // Add sub-account totals to parent
                totalDebit = totalDebit.add(subAccount.getTotalDebit());
                totalCredit = totalCredit.add(subAccount.getTotalCredit());
            }
        }
        
        // Update the account with calculated totals
        account.setTotalDebit(totalDebit);
        account.setTotalCredit(totalCredit);
        
        System.out.println("Updated " + account.getAccountName() + 
                         " totals: Debit=" + totalDebit + ", Credit=" + totalCredit);
    }
}
