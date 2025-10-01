-- Fix GRN Accounts Script
-- This script creates the missing accounts needed for GRN voucher entries
-- Run this script to fix the Direct GRN voucher creation issue

-- First, check current account count
SELECT 'Current account count:' as info, COUNT(*) as count FROM account;

-- Show existing accounts
SELECT 'Existing accounts:' as info;
SELECT id, code, name, type, is_active FROM account ORDER BY id;

-- Create parent LIABILITIES account if it doesn't exist
INSERT INTO account (code, name, type, description, is_active) 
SELECT '2000', 'LIABILITIES', 'LIABILITY', 'All Liabilities', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '2000');

-- Create Current Liabilities parent account
INSERT INTO account (code, name, type, description, is_active, parent_id) 
SELECT '2001', 'Current Liabilities', 'LIABILITY', 'Current Liabilities', TRUE, 
       (SELECT id FROM account WHERE code = '2000')
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '2001');

-- Create Accounts Payable account (ID should be 22)
-- First, check if we need to create this account with a specific ID
-- We'll create it and then update the ID if needed
INSERT INTO account (code, name, type, description, is_active, parent_id) 
SELECT '2001.01', 'Accounts Payable', 'LIABILITY', 'Money owed to suppliers', TRUE,
       (SELECT id FROM account WHERE code = '2001')
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '2001.01');

-- Create parent EXPENSES account if it doesn't exist
INSERT INTO account (code, name, type, description, is_active) 
SELECT '4000', 'EXPENSES', 'EXPENSE', 'All Expenses', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '4000');

-- Create Purchase / Cost of Goods Sold account (ID should be 35)
INSERT INTO account (code, name, type, description, is_active, parent_id) 
SELECT '4001', 'Purchase / Cost of Goods Sold', 'EXPENSE', 'Purchase of Goods and Services', TRUE,
       (SELECT id FROM account WHERE code = '4000')
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '4001');

-- If we need to ensure specific IDs, we can update them
-- But first let's see what IDs we actually have

-- Show the accounts we just created
SELECT 'Newly created accounts:' as info;
SELECT id, code, name, type, is_active FROM account 
WHERE code IN ('2001.01', '4001') ORDER BY id;

-- If the IDs are not 22 and 35, we need to update the code
-- Let's check what IDs we actually got
SELECT 'Accounts needed for GRN:' as info;
SELECT 
    CASE 
        WHEN code = '2001.01' THEN 'Accounts Payable (should be ID 22)'
        WHEN code = '4001' THEN 'Purchase / Cost of Goods Sold (should be ID 35)'
    END as account_info,
    id, code, name
FROM account 
WHERE code IN ('2001.01', '4001') 
ORDER BY code;

-- Show final account structure
SELECT 'Final account structure:' as info;
SELECT id, code, name, type, is_active FROM account ORDER BY id;
