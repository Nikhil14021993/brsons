-- Customer Ledger System Migration Script
-- This script creates the necessary tables for the customer ledger and payment tracking system

-- 1. Create customer_ledger table
CREATE TABLE IF NOT EXISTS customer_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_phone VARCHAR(20) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    total_outstanding DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_advance DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    net_outstanding DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    last_payment_date DATETIME,
    last_invoice_date DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_customer_phone (customer_phone)
);

-- 2. Create payment_entries table
CREATE TABLE IF NOT EXISTS payment_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_phone VARCHAR(20) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    payment_amount DECIMAL(10,2) NOT NULL,
    payment_type VARCHAR(50) NOT NULL,
    payment_reference VARCHAR(255),
    payment_date DATETIME NOT NULL,
    description TEXT,
    remaining_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    is_advance BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

-- 3. Create invoice_settlements table
CREATE TABLE IF NOT EXISTS invoice_settlements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    invoice_number VARCHAR(100) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    payment_entry_id BIGINT NOT NULL,
    settlement_amount DECIMAL(10,2) NOT NULL,
    settlement_date DATETIME NOT NULL,
    is_full_settlement BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_id (order_id),
    INDEX idx_customer_phone (customer_phone),
    INDEX idx_payment_entry_id (payment_entry_id)
);

-- 4. Add indexes for better performance
CREATE INDEX idx_customer_ledger_phone ON customer_ledger(customer_phone);
CREATE INDEX idx_payment_entries_phone ON payment_entries(customer_phone);
CREATE INDEX idx_payment_entries_date ON payment_entries(payment_date);
CREATE INDEX idx_payment_entries_remaining ON payment_entries(remaining_amount);
CREATE INDEX idx_invoice_settlements_order ON invoice_settlements(order_id);
CREATE INDEX idx_invoice_settlements_customer ON invoice_settlements(customer_phone);

-- 5. Insert sample data (optional - for testing)
-- You can uncomment these lines if you want to insert sample data

/*
INSERT INTO customer_ledger (customer_phone, customer_name, total_outstanding, total_advance, net_outstanding) 
VALUES 
('9876543210', 'John Doe', 1500.00, 0.00, 1500.00),
('9876543211', 'Jane Smith', 0.00, 500.00, -500.00),
('9876543212', 'Bob Johnson', 2000.00, 300.00, 1700.00);
*/
