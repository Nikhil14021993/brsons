package com.brsons.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.brsons.dto.BalanceSheetRow;
import com.brsons.dto.PnLRow;
import com.brsons.model.Account;
import com.brsons.model.Product;
import com.brsons.repository.ProductRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class AccountingReportService {

    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private ProductRepository productRepository;

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
    // Profit & Loss - Enhanced Tally-style report
    public List<PnLRow> getProfitAndLoss(LocalDate startDate, LocalDate endDate) {
        List<PnLRow> pnlRows = new ArrayList<>();
        
        // Get Revenue accounts with hierarchy
        List<Object[]> revenueResults = entityManager.createQuery(
            "SELECT a.name, a.code, a.type, a.parent, " +
            "SUM(CASE WHEN a.type IN ('REVENUE', 'INCOME') THEN COALESCE(e.credit,0) - COALESCE(e.debit,0) ELSE 0 END) " +
            "FROM VoucherEntry e JOIN e.account a " +
            "WHERE a.type IN ('REVENUE', 'INCOME') " +
            "AND e.voucher.date BETWEEN :startDate AND :endDate " +
            "GROUP BY a.name, a.code, a.type, a.parent " +
            "ORDER BY a.code", Object[].class)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();

        // Get Expense accounts with hierarchy
        List<Object[]> expenseResults = entityManager.createQuery(
            "SELECT a.name, a.code, a.type, a.parent, " +
            "SUM(CASE WHEN a.type = 'EXPENSE' THEN COALESCE(e.debit,0) - COALESCE(e.credit,0) ELSE 0 END) " +
            "FROM VoucherEntry e JOIN e.account a " +
            "WHERE a.type = 'EXPENSE' " +
            "AND e.voucher.date BETWEEN :startDate AND :endDate " +
            "GROUP BY a.name, a.code, a.type, a.parent " +
            "ORDER BY a.code", Object[].class)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();

        // Process Revenue section
        BigDecimal totalRevenue = BigDecimal.ZERO;
        if (!revenueResults.isEmpty()) {
            pnlRows.add(new PnLRow("INCOME", "", BigDecimal.ZERO, "HEADER", 0));
            pnlRows.add(new PnLRow("═══════════════════════════════════════════════════════════════", "", BigDecimal.ZERO, "SEPARATOR", 0));
            
            for (Object[] result : revenueResults) {
                String accountName = (String) result[0];
                String accountCode = (String) result[1];
                String accountType = (String) result[2];
                Account parent = (Account) result[3];
                Object value = result[4];
                BigDecimal amount = value != null ? new BigDecimal(value.toString()) : BigDecimal.ZERO;
                
                if (amount.compareTo(BigDecimal.ZERO) != 0) {
                    int level = (parent != null) ? 1 : 0;
                    String parentName = (parent != null) ? parent.getName() : null;
                    
                    PnLRow row = new PnLRow(accountName, accountCode, amount, accountType, level);
                    row.setParentAccount(parentName);
                    pnlRows.add(row);
                    totalRevenue = totalRevenue.add(amount);
                }
            }
            
            pnlRows.add(new PnLRow("═══════════════════════════════════════════════════════════════", "", BigDecimal.ZERO, "SEPARATOR", 0));
            pnlRows.add(PnLRow.createSubtotalRow("TOTAL INCOME", totalRevenue));
            pnlRows.add(new PnLRow("", "", BigDecimal.ZERO, "SPACER", 0));
        }

        // Process Expense section
        BigDecimal totalExpense = BigDecimal.ZERO;
        if (!expenseResults.isEmpty()) {
            pnlRows.add(new PnLRow("EXPENSES", "", BigDecimal.ZERO, "HEADER", 0));
            pnlRows.add(new PnLRow("═══════════════════════════════════════════════════════════════", "", BigDecimal.ZERO, "SEPARATOR", 0));
            
            for (Object[] result : expenseResults) {
                String accountName = (String) result[0];
                String accountCode = (String) result[1];
                String accountType = (String) result[2];
                Account parent = (Account) result[3];
                Object value = result[4];
                BigDecimal amount = value != null ? new BigDecimal(value.toString()) : BigDecimal.ZERO;
                
                if (amount.compareTo(BigDecimal.ZERO) != 0) {
                    int level = (parent != null) ? 1 : 0;
                    String parentName = (parent != null) ? parent.getName() : null;
                    
                    PnLRow row = new PnLRow(accountName, accountCode, amount, accountType, level);
                    row.setParentAccount(parentName);
                    pnlRows.add(row);
                    totalExpense = totalExpense.add(amount);
                }
            }
            
            pnlRows.add(new PnLRow("═══════════════════════════════════════════════════════════════", "", BigDecimal.ZERO, "SEPARATOR", 0));
            pnlRows.add(PnLRow.createSubtotalRow("TOTAL EXPENSES", totalExpense));
            pnlRows.add(new PnLRow("", "", BigDecimal.ZERO, "SPACER", 0));
        }

        // Calculate Net Profit/Loss
        BigDecimal netProfit = totalRevenue.subtract(totalExpense);
        pnlRows.add(new PnLRow("═══════════════════════════════════════════════════════════════", "", BigDecimal.ZERO, "SEPARATOR", 0));
        if (netProfit.compareTo(BigDecimal.ZERO) >= 0) {
            pnlRows.add(PnLRow.createTotalRow("NET PROFIT", netProfit));
        } else {
            pnlRows.add(PnLRow.createTotalRow("NET LOSS", netProfit.abs()));
        }

        return pnlRows;
    }

    // Diagnostic method to check all accounts and their balances
    public List<Object[]> getAllAccountBalances(LocalDate startDate, LocalDate endDate) {
        return entityManager.createQuery(
            "SELECT a.name, a.code, a.type, " +
            "SUM(COALESCE(e.debit,0)) as total_debit, " +
            "SUM(COALESCE(e.credit,0)) as total_credit, " +
            "SUM(COALESCE(e.debit,0) - COALESCE(e.credit,0)) as net_balance " +
            "FROM VoucherEntry e JOIN e.account a " +
            "WHERE e.voucher.date BETWEEN :startDate AND :endDate " +
            "GROUP BY a.name, a.code, a.type " +
            "ORDER BY a.type, a.code", Object[].class)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
    }

    // Method to check if we have any revenue/expense accounts
    public List<Object[]> getRevenueExpenseAccounts() {
        return entityManager.createQuery(
            "SELECT a.name, a.code, a.type " +
            "FROM Account a " +
            "WHERE a.type IN ('INCOME', 'EXPENSE', 'REVENUE') " +
            "ORDER BY a.type, a.code", Object[].class)
            .getResultList();
    }

    // ==================== STOCK ACCOUNTING METHODS ====================
    
    /**
     * Calculate opening stock value at the beginning of the period
     */
    public BigDecimal calculateOpeningStockValue(LocalDate startDate) {
        // For now, we'll calculate current stock as opening stock
        // In a real system, you'd store historical stock values
        return calculateCurrentStockValue();
    }
    
    /**
     * Calculate closing stock value at the end of the period
     */
    public BigDecimal calculateClosingStockValue(LocalDate endDate) {
        return calculateCurrentStockValue();
    }
    
    /**
     * Calculate current stock value based on current inventory
     */
    private BigDecimal calculateCurrentStockValue() {
        List<Product> products = productRepository.findAll();
        BigDecimal totalValue = BigDecimal.ZERO;
        
        for (Product product : products) {
            int stock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            if (stock > 0) {
                BigDecimal price = getProductCostPrice(product);
                BigDecimal productValue = price.multiply(BigDecimal.valueOf(stock));
                totalValue = totalValue.add(productValue);
            }
        }
        
        return totalValue;
    }
    
    /**
     * Calculate Cost of Goods Sold (COGS)
     * COGS = Opening Stock + Purchases - Closing Stock
     */
    public BigDecimal calculateCOGS(LocalDate startDate, LocalDate endDate) {
        BigDecimal openingStock = calculateOpeningStockValue(startDate);
        BigDecimal closingStock = calculateClosingStockValue(endDate);
        
        // Get total purchases during the period
        BigDecimal totalPurchases = getTotalPurchases(startDate, endDate);
        
        // COGS = Opening Stock + Purchases - Closing Stock
        return openingStock.add(totalPurchases).subtract(closingStock);
    }
    
    /**
     * Get total purchases during the period
     */
    private BigDecimal getTotalPurchases(LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = entityManager.createQuery(
            "SELECT SUM(COALESCE(e.debit,0)) " +
            "FROM VoucherEntry e JOIN e.account a " +
            "WHERE a.type = 'EXPENSE' " +
            "AND (a.name LIKE '%Purchase%' OR a.name LIKE '%COGS%' OR a.name LIKE '%Cost%') " +
            "AND e.voucher.date BETWEEN :startDate AND :endDate", Object[].class)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
        
        if (!results.isEmpty() && results.get(0)[0] != null) {
            return new BigDecimal(results.get(0)[0].toString());
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Get product cost price (preferably purchase price)
     */
    private BigDecimal getProductCostPrice(Product product) {
        // First try purchase price
        if (product.getPurchasePrice() != null && product.getPurchasePrice() > 0) {
            return BigDecimal.valueOf(product.getPurchasePrice());
        }
        
        // Fallback to retail price
        if (product.getRetailPrice() != null && product.getRetailPrice() > 0) {
            return BigDecimal.valueOf(product.getRetailPrice());
        }
        
        // Fallback to b2b price
        if (product.getB2bPrice() != null && product.getB2bPrice() > 0) {
            return BigDecimal.valueOf(product.getB2bPrice());
        }
        
        // Fallback to main price field
        if (product.getPrice() != null) {
            return product.getPrice();
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Enhanced P&L with stock information
     */
    public List<PnLRow> getProfitAndLossWithStock(LocalDate startDate, LocalDate endDate) {
        List<PnLRow> pnlRows = new ArrayList<>();
        
        // Calculate stock values
        BigDecimal openingStock = calculateOpeningStockValue(startDate);
        BigDecimal closingStock = calculateClosingStockValue(endDate);
        BigDecimal cogs = calculateCOGS(startDate, endDate);
        
        // Get Revenue accounts
        List<Object[]> revenueResults = entityManager.createQuery(
            "SELECT a.name, a.code, a.type, a.parent, " +
            "SUM(CASE WHEN a.type IN ('REVENUE', 'INCOME') THEN COALESCE(e.credit,0) - COALESCE(e.debit,0) ELSE 0 END) " +
            "FROM VoucherEntry e JOIN e.account a " +
            "WHERE a.type IN ('REVENUE', 'INCOME') " +
            "AND e.voucher.date BETWEEN :startDate AND :endDate " +
            "GROUP BY a.name, a.code, a.type, a.parent " +
            "ORDER BY a.code", Object[].class)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();

        // Get Expense accounts
        List<Object[]> expenseResults = entityManager.createQuery(
            "SELECT a.name, a.code, a.type, a.parent, " +
            "SUM(CASE WHEN a.type = 'EXPENSE' THEN COALESCE(e.debit,0) - COALESCE(e.credit,0) ELSE 0 END) " +
            "FROM VoucherEntry e JOIN e.account a " +
            "WHERE a.type = 'EXPENSE' " +
            "AND e.voucher.date BETWEEN :startDate AND :endDate " +
            "GROUP BY a.name, a.code, a.type, a.parent " +
            "ORDER BY a.code", Object[].class)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();

        // Process Revenue section
        BigDecimal totalRevenue = BigDecimal.ZERO;
        if (!revenueResults.isEmpty()) {
            pnlRows.add(new PnLRow("INCOME", "", BigDecimal.ZERO, "HEADER", 0));
            pnlRows.add(new PnLRow("═══════════════════════════════════════════════════════════════", "", BigDecimal.ZERO, "SEPARATOR", 0));
            
            for (Object[] result : revenueResults) {
                String accountName = (String) result[0];
                String accountCode = (String) result[1];
                String accountType = (String) result[2];
                Account parent = (Account) result[3];
                Object value = result[4];
                BigDecimal amount = value != null ? new BigDecimal(value.toString()) : BigDecimal.ZERO;
                
                if (amount.compareTo(BigDecimal.ZERO) != 0) {
                    int level = (parent != null) ? 1 : 0;
                    String parentName = (parent != null) ? parent.getName() : null;
                    
                    PnLRow row = new PnLRow(accountName, accountCode, amount, accountType, level);
                    row.setParentAccount(parentName);
                    pnlRows.add(row);
                    totalRevenue = totalRevenue.add(amount);
                }
            }
            
            pnlRows.add(new PnLRow("═══════════════════════════════════════════════════════════════", "", BigDecimal.ZERO, "SEPARATOR", 0));
            pnlRows.add(PnLRow.createSubtotalRow("TOTAL INCOME", totalRevenue));
            pnlRows.add(new PnLRow("", "", BigDecimal.ZERO, "SPACER", 0));
        }

        // Process Stock Information
        pnlRows.add(new PnLRow("STOCK INFORMATION", "", BigDecimal.ZERO, "HEADER", 0));
        pnlRows.add(new PnLRow("═══════════════════════════════════════════════════════════════", "", BigDecimal.ZERO, "SEPARATOR", 0));
        pnlRows.add(new PnLRow("Opening Stock", "", openingStock, "STOCK", 0));
        pnlRows.add(new PnLRow("Closing Stock", "", closingStock, "STOCK", 0));
        pnlRows.add(new PnLRow("═══════════════════════════════════════════════════════════════", "", BigDecimal.ZERO, "SEPARATOR", 0));
        pnlRows.add(PnLRow.createSubtotalRow("COST OF GOODS SOLD", cogs));
        pnlRows.add(new PnLRow("", "", BigDecimal.ZERO, "SPACER", 0));

        // Process Expense section
        BigDecimal totalExpense = BigDecimal.ZERO;
        if (!expenseResults.isEmpty()) {
            pnlRows.add(new PnLRow("EXPENSES", "", BigDecimal.ZERO, "HEADER", 0));
            pnlRows.add(new PnLRow("═══════════════════════════════════════════════════════════════", "", BigDecimal.ZERO, "SEPARATOR", 0));
            
            for (Object[] result : expenseResults) {
                String accountName = (String) result[0];
                String accountCode = (String) result[1];
                String accountType = (String) result[2];
                Account parent = (Account) result[3];
                Object value = result[4];
                BigDecimal amount = value != null ? new BigDecimal(value.toString()) : BigDecimal.ZERO;
                
                if (amount.compareTo(BigDecimal.ZERO) != 0) {
                    int level = (parent != null) ? 1 : 0;
                    String parentName = (parent != null) ? parent.getName() : null;
                    
                    PnLRow row = new PnLRow(accountName, accountCode, amount, accountType, level);
                    row.setParentAccount(parentName);
                    pnlRows.add(row);
                    totalExpense = totalExpense.add(amount);
                }
            }
            
            pnlRows.add(new PnLRow("═══════════════════════════════════════════════════════════════", "", BigDecimal.ZERO, "SEPARATOR", 0));
            pnlRows.add(PnLRow.createSubtotalRow("TOTAL EXPENSES", totalExpense));
            pnlRows.add(new PnLRow("", "", BigDecimal.ZERO, "SPACER", 0));
        }

        // Calculate Net Profit/Loss (Revenue - COGS - Other Expenses)
        // Note: COGS is already included in totalExpense, so we don't double-count it
        BigDecimal netProfit = totalRevenue.subtract(totalExpense);
        pnlRows.add(new PnLRow("═══════════════════════════════════════════════════════════════", "", BigDecimal.ZERO, "SEPARATOR", 0));
        if (netProfit.compareTo(BigDecimal.ZERO) >= 0) {
            pnlRows.add(PnLRow.createTotalRow("NET PROFIT", netProfit));
        } else {
            pnlRows.add(PnLRow.createTotalRow("NET LOSS", netProfit.abs()));
        }

        return pnlRows;
    }
}
