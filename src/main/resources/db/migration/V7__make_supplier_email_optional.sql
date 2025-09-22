-- Make Supplier Email Optional Migration
-- Version: 7.0
-- Description: Makes email column nullable in suppliers table to allow suppliers without email

-- ==================== MODIFY SUPPLIERS TABLE ====================
-- Make email column nullable to allow suppliers without email
ALTER TABLE suppliers 
MODIFY COLUMN email VARCHAR(255) NULL;

-- Add a comment to document the change
ALTER TABLE suppliers 
MODIFY COLUMN email VARCHAR(255) NULL COMMENT 'Optional email address for supplier contact';
