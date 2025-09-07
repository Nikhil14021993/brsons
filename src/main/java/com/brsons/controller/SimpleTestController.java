package com.brsons.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class SimpleTestController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/db-connection")
    public Map<String, Object> testDatabaseConnection() {
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            result.put("status", "SUCCESS");
            result.put("message", "Database connection successful");
            
            // Test basic query
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM account")) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    result.put("account_count", rs.getInt(1));
                }
            }
            
            // Test voucher query
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM voucher")) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    result.put("voucher_count", rs.getInt(1));
                }
            }
            
            // Test voucher entry query
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM voucher_entry")) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    result.put("voucher_entry_count", rs.getInt(1));
                }
            }
            
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/trial-balance-sql")
    public List<Map<String, Object>> testTrialBalanceSQL() {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT a.id, a.name, a.code, a.type, a.parent_id, " +
                        "       COALESCE(SUM(e.debit), 0) as total_debit, " +
                        "       COALESCE(SUM(e.credit), 0) as total_credit " +
                        "FROM account a " +
                        "LEFT JOIN voucher_entry e ON e.account_id = a.id " +
                        "LEFT JOIN voucher v ON e.voucher_id = v.id " +
                        "WHERE a.is_active = true " +
                        "AND (v.date IS NULL OR v.date BETWEEN ? AND ?) " +
                        "GROUP BY a.id, a.name, a.code, a.type, a.parent_id " +
                        "ORDER BY a.code";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, "2024-01-01");
                stmt.setString(2, "2024-12-31");
                
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("name", rs.getString("name"));
                    row.put("code", rs.getString("code"));
                    row.put("type", rs.getString("type"));
                    row.put("parent_id", rs.getObject("parent_id"));
                    row.put("total_debit", rs.getBigDecimal("total_debit"));
                    row.put("total_credit", rs.getBigDecimal("total_credit"));
                    results.add(row);
                }
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            results.add(error);
        }
        return results;
    }
}
