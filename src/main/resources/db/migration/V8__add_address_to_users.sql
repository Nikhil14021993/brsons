-- Add Address Fields to Users Migration
-- Version: 8.0
-- Description: Adds address fields to users table for tax calculation

-- ==================== USERS TABLE ====================
-- Add address fields to users table
ALTER TABLE users 
ADD COLUMN address_line1 VARCHAR(255) NULL COMMENT 'User address line 1',
ADD COLUMN address_line2 VARCHAR(255) NULL COMMENT 'User address line 2',
ADD COLUMN city VARCHAR(100) NULL COMMENT 'User city',
ADD COLUMN state VARCHAR(100) NULL COMMENT 'User state',
ADD COLUMN zip_code VARCHAR(20) NULL COMMENT 'User zip code',
ADD COLUMN gstin VARCHAR(15) NULL COMMENT 'User GSTIN';

-- Add indexes for address fields
CREATE INDEX idx_users_state ON users(state);
CREATE INDEX idx_users_city ON users(city);
CREATE INDEX idx_users_gstin ON users(gstin);

-- ==================== COMMENTS ====================
-- This migration adds address fields to the users table to support:
-- 1. Tax type determination (CGST+SGST vs IGST)
-- 2. User profile management
-- 3. Address-based business logic
-- 
-- The fields are nullable to maintain backward compatibility with existing users.
-- Users can update their address information through profile management.
