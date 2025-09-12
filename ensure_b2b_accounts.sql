-- Ensure required accounts exist for B2B voucher entries
-- Run this script to create the necessary accounts if they don't exist

-- Check current account count
SELECT 'Current account count:' as info, COUNT(*) as count FROM account;

-- Create parent accounts if they don't exist
INSERT INTO account (code, name, type, description, is_active) 
SELECT '1000', 'ASSETS', 'ASSET', 'All Assets', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '1000');

INSERT INTO account (code, name, type, description, is_active) 
SELECT '3000', 'INCOME', 'INCOME', 'All Income', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '3000');

-- Create Current Assets parent account
INSERT INTO account (code, name, type, description, is_active, parent_id) 
SELECT '1001', 'Current Assets', 'ASSET', 'Current Assets', TRUE, 
       (SELECT id FROM account WHERE code = '1000')
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '1001');

-- Create Accounts Receivable account (1001.01)
INSERT INTO account (code, name, type, description, is_active, parent_id) 
SELECT '1001.01', 'Accounts Receivable', 'ASSET', 'Money owed by customers', TRUE,
       (SELECT id FROM account WHERE code = '1001')
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '1001.01');

-- Create Sales account (3001)
INSERT INTO account (code, name, type, description, is_active, parent_id) 
SELECT '3001', 'Sales', 'INCOME', 'Sales Revenue', TRUE,
       (SELECT id FROM account WHERE code = '3000')
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '3001');

-- Verify the accounts exist
SELECT 'Required accounts for B2B voucher entries:' as info;
SELECT id, code, name, type, is_active, parent_id 
FROM account 
WHERE code IN ('1001.01', '3001') 
ORDER BY code;
