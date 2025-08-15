package com.brsons.service;

import com.brsons.dto.TrialBalanceRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TrialBalanceService {

    @PersistenceContext
    private EntityManager entityManager;

    public List<TrialBalanceRow> getTrialBalance(LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = entityManager.createQuery(
            "SELECT a.name, " +
            "       COALESCE(SUM(e.debit), 0), " +
            "       COALESCE(SUM(e.credit), 0) " +
            "FROM Account a " +
            "LEFT JOIN VoucherEntry e ON e.account = a " +
            "     AND e.voucher.date BETWEEN :startDate AND :endDate " +
            "GROUP BY a.name", Object[].class)
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
}
