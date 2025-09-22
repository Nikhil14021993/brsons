-- Add Tax Breakdown Fields to Orders Table
-- Version: 9.0
-- Description: Adds tax breakdown fields (tax_type, cgst_rate, cgst_amount, sgst_rate, sgst_amount, igst_rate, igst_amount) to the orders table for proper CGST/SGST/IGST display in invoices.

ALTER TABLE orders
ADD COLUMN tax_type VARCHAR(20),
ADD COLUMN cgst_rate DECIMAL(5,2),
ADD COLUMN cgst_amount DECIMAL(10,2),
ADD COLUMN sgst_rate DECIMAL(5,2),
ADD COLUMN sgst_amount DECIMAL(10,2),
ADD COLUMN igst_rate DECIMAL(5,2),
ADD COLUMN igst_amount DECIMAL(10,2);

-- Add indexes for tax fields for faster lookups
CREATE INDEX idx_orders_tax_type ON orders(tax_type);
CREATE INDEX idx_orders_cgst_amount ON orders(cgst_amount);
CREATE INDEX idx_orders_sgst_amount ON orders(sgst_amount);
CREATE INDEX idx_orders_igst_amount ON orders(igst_amount);
