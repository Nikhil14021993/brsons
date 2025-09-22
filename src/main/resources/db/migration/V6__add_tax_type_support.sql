-- Add Tax Type Support Migration
-- Version: 6.0
-- Description: Adds tax_type field to suppliers and CGST/SGST/IGST fields to purchase_order_items and grn_items

-- ==================== SUPPLIERS TABLE ====================
-- Add tax_type column to suppliers table
ALTER TABLE suppliers 
ADD COLUMN tax_type ENUM('CGST_SGST', 'IGST') DEFAULT 'CGST_SGST' 
COMMENT 'Tax type: CGST_SGST for intra-state, IGST for inter-state';

-- Add index for tax_type
CREATE INDEX idx_suppliers_tax_type ON suppliers(tax_type);

-- ==================== PURCHASE ORDER ITEMS TABLE ====================
-- Add CGST, SGST, IGST fields to purchase_order_items table
ALTER TABLE purchase_order_items 
ADD COLUMN cgst_percentage DECIMAL(5,2) DEFAULT 0.00 COMMENT 'CGST percentage',
ADD COLUMN sgst_percentage DECIMAL(5,2) DEFAULT 0.00 COMMENT 'SGST percentage',
ADD COLUMN igst_percentage DECIMAL(5,2) DEFAULT 0.00 COMMENT 'IGST percentage',
ADD COLUMN cgst_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT 'CGST amount',
ADD COLUMN sgst_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT 'SGST amount',
ADD COLUMN igst_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT 'IGST amount';

-- Add indexes for tax fields
CREATE INDEX idx_po_items_cgst ON purchase_order_items(cgst_percentage);
CREATE INDEX idx_po_items_sgst ON purchase_order_items(sgst_percentage);
CREATE INDEX idx_po_items_igst ON purchase_order_items(igst_percentage);

-- ==================== GRN ITEMS TABLE ====================
-- Add CGST, SGST, IGST fields to grn_items table
ALTER TABLE grn_items 
ADD COLUMN cgst_percentage DECIMAL(5,2) DEFAULT 0.00 COMMENT 'CGST percentage',
ADD COLUMN sgst_percentage DECIMAL(5,2) DEFAULT 0.00 COMMENT 'SGST percentage',
ADD COLUMN igst_percentage DECIMAL(5,2) DEFAULT 0.00 COMMENT 'IGST percentage',
ADD COLUMN cgst_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT 'CGST amount',
ADD COLUMN sgst_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT 'SGST amount',
ADD COLUMN igst_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT 'IGST amount';

-- Add indexes for tax fields
CREATE INDEX idx_grn_items_cgst ON grn_items(cgst_percentage);
CREATE INDEX idx_grn_items_sgst ON grn_items(sgst_percentage);
CREATE INDEX idx_grn_items_igst ON grn_items(igst_percentage);

-- ==================== COMMENTS ====================
-- This migration adds support for GST tax types:
-- 1. CGST_SGST: For intra-state transactions (same state supplier and buyer)
-- 2. IGST: For inter-state transactions (different state supplier and buyer)
-- 
-- The tax calculation logic will be:
-- - For CGST_SGST: Calculate CGST and SGST separately (usually half of total GST rate each)
-- - For IGST: Calculate IGST (usually same as total GST rate)
-- 
-- Example:
-- - If GST rate is 18% and supplier is CGST_SGST type:
--   - CGST = 9%, SGST = 9%
-- - If GST rate is 18% and supplier is IGST type:
--   - IGST = 18%
