package com.brsons.config;

import com.brsons.service.EnhancedInvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class InvoiceCleanupConfig {
    
    @Autowired
    private EnhancedInvoiceService enhancedInvoiceService;
    
    /**
     * Clean up expired invoices every day at 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredInvoices() {
        try {
            enhancedInvoiceService.cleanupExpiredInvoices();
            System.out.println("Expired invoices cleanup completed successfully");
        } catch (Exception e) {
            System.err.println("Error during expired invoices cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
