-- Customer Ledger System Migration Script
-- This script creates the necessary tables for the customer ledger and payment tracking system

-- 1. Create customer_ledger table
CREATE TABLE IF NOT EXISTS customer_ledger (
    id BIGSERIAL PRIMARY KEY,
    customer_phone VARCHAR(20) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    total_outstanding DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_advance DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    net_outstanding DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    last_payment_date TIMESTAMP,
    last_invoice_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_customer_phone UNIQUE (customer_phone)
);

-- 2. Create payment_entries table
CREATE TABLE IF NOT EXISTS payment_entries (
    id BIGSERIAL PRIMARY KEY,
    customer_phone VARCHAR(20) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    payment_amount DECIMAL(10,2) NOT NULL,
    payment_type VARCHAR(50) NOT NULL,
    payment_reference VARCHAR(255),
    payment_date TIMESTAMP NOT NULL,
    description TEXT,
    remaining_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    is_advance BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

-- 3. Create invoice_settlements table
CREATE TABLE IF NOT EXISTS invoice_settlements (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    invoice_number VARCHAR(100) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    payment_entry_id BIGINT NOT NULL,
    settlement_amount DECIMAL(10,2) NOT NULL,
    settlement_date TIMESTAMP NOT NULL,
    is_full_settlement BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Add indexes for better performance
CREATE INDEX IF NOT EXISTS idx_customer_ledger_phone ON customer_ledger(customer_phone);
CREATE INDEX IF NOT EXISTS idx_payment_entries_phone ON payment_entries(customer_phone);
CREATE INDEX IF NOT EXISTS idx_payment_entries_date ON payment_entries(payment_date);
CREATE INDEX IF NOT EXISTS idx_payment_entries_remaining ON payment_entries(remaining_amount);
CREATE INDEX IF NOT EXISTS idx_invoice_settlements_order ON invoice_settlements(order_id);
CREATE INDEX IF NOT EXISTS idx_invoice_settlements_customer ON invoice_settlements(customer_phone);
CREATE INDEX IF NOT EXISTS idx_invoice_settlements_payment ON invoice_settlements(payment_entry_id);
