package com.brsons.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.brsons.dto.BalanceSheetRow;
import com.brsons.dto.PnLRow;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class AccountingReportService {

    @PersistenceContext
    private EntityManager entityManager;

    // Balance Sheet
    public List<BalanceSheetRow> getBalanceSheet(LocalDate date) {
        List<Object[]> results = entityManager.createQuery(
            "SELECT a.name, SUM(COALESCE(e.debit,0) - COALESCE(e.credit,0)) " +
            "FROM VoucherEntry e JOIN e.account a " +
            "WHERE UPPER(a.type) IN ('ASSET', 'LIABILITY', 'EQUITY') " +
            "AND e.voucher.date <= :date " +
            "GROUP BY a.name", Object[].class)
            .setParameter("date", date)
            .getResultList();

        return results.stream()
                .map(r -> {
                    String accountName = (String) r[0];
                    Object value = r[1];
                    BigDecimal amount = value != null ? BigDecimal.valueOf(((Number) value).doubleValue()) : BigDecimal.ZERO;
                    return new BalanceSheetRow(accountName, amount);
                })
                .toList();
    }
    // Profit & Loss
    public List<PnLRow> getProfitAndLoss(LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = entityManager.createQuery(
            "SELECT a.name, SUM(CASE WHEN a.type = 'REVENUE' THEN COALESCE(e.credit,0) - COALESCE(e.debit,0) " +
            "WHEN a.type = 'EXPENSE' THEN COALESCE(e.debit,0) - COALESCE(e.credit,0) ELSE 0 END) " +
            "FROM VoucherEntry e JOIN e.account a " +
            "WHERE a.type IN ('REVENUE', 'EXPENSE') " +
            "AND e.voucher.date BETWEEN :startDate AND :endDate " +
            "GROUP BY a.name", Object[].class)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();

        return results.stream()
                .map(r -> {
                    String accountName = (String) r[0];
                    Object value = r[1]; // could be Long, Double, or BigDecimal
                    BigDecimal amount = value != null ? new BigDecimal(value.toString()) : BigDecimal.ZERO;
                    return new PnLRow(accountName, amount);
                })
                .toList();
    }
}
