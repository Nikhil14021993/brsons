-- Add Product Tax Configuration and Customer Address Migration
-- Version: 8.0
-- Description: Adds tax configuration fields to products table and address fields to customer_ledger table

-- ==================== PRODUCTS TABLE ====================
-- Add tax configuration fields to products table
ALTER TABLE products 
ADD COLUMN cgst_percentage DECIMAL(5,2) DEFAULT 0.00 COMMENT 'CGST percentage for this product',
ADD COLUMN sgst_percentage DECIMAL(5,2) DEFAULT 0.00 COMMENT 'SGST percentage for this product',
ADD COLUMN igst_percentage DECIMAL(5,2) DEFAULT 0.00 COMMENT 'IGST percentage for this product';

-- Add indexes for tax fields
CREATE INDEX idx_products_cgst ON products(cgst_percentage);
CREATE INDEX idx_products_sgst ON products(sgst_percentage);
CREATE INDEX idx_products_igst ON products(igst_percentage);

-- ==================== CUSTOMER LEDGER TABLE ====================
-- Add address and tax-related fields to customer_ledger table
ALTER TABLE customer_ledger 
ADD COLUMN address_line1 VARCHAR(255) COMMENT 'Customer address line 1',
ADD COLUMN address_line2 VARCHAR(255) COMMENT 'Customer address line 2',
ADD COLUMN city VARCHAR(100) COMMENT 'Customer city',
ADD COLUMN state VARCHAR(100) COMMENT 'Customer state',
ADD COLUMN state_code VARCHAR(10) COMMENT 'Customer state code for tax determination',
ADD COLUMN zip_code VARCHAR(20) COMMENT 'Customer ZIP/PIN code',
ADD COLUMN gstin VARCHAR(15) COMMENT 'Customer GSTIN number';

-- Add indexes for address and tax fields
CREATE INDEX idx_customer_ledger_state ON customer_ledger(state);
CREATE INDEX idx_customer_ledger_state_code ON customer_ledger(state_code);
CREATE INDEX idx_customer_ledger_city ON customer_ledger(city);
CREATE INDEX idx_customer_ledger_gstin ON customer_ledger(gstin);

-- ==================== COMMENTS ====================
-- This migration adds:
-- 1. Product Tax Configuration: Admin can define CGST, SGST, and IGST percentages for each product
-- 2. Customer Address Information: Required for automatic tax type determination
-- 
-- Tax Logic:
-- - Products have predefined tax rates (CGST, SGST, IGST percentages)
-- - Customer's state determines whether to use CGST+SGST (same state) or IGST (different state)
-- - Business state code is configured in the application (default: Maharashtra - 27)
-- 
-- Example Usage:
-- - Product: T-Shirt with CGST=9%, SGST=9%, IGST=18%
-- - Customer in Maharashtra (same state as business): Apply CGST=9% + SGST=9%
-- - Customer in Gujarat (different state): Apply IGST=18%
