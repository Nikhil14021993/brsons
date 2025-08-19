-- Database Migration Script for Invoice System
-- Run this script to add the missing columns to the orders table

-- Add invoice_pdf_content column (for storing PDF binary data)
ALTER TABLE orders ADD COLUMN invoice_pdf_content LONGBLOB;

-- Add invoice_generated_at column (for storing invoice generation timestamp)
ALTER TABLE orders ADD COLUMN invoice_generated_at DATETIME;

-- Verify the changes
DESCRIBE orders;
