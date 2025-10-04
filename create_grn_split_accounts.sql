-- Create GRN Split Accounts Script
-- This script creates the required accounts for split GRN voucher creation

-- First, check current account count
SELECT 'Current account count:' as info, COUNT(*) as count FROM account;

-- Create parent LIABILITIES account if it doesn't exist
INSERT INTO account (code, name, type, description, is_active) 
SELECT '2000', 'LIABILITIES', 'LIABILITY', 'All Liabilities', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '2000');

-- Create Current Liabilities parent account
INSERT INTO account (code, name, type, description, is_active, parent_id) 
SELECT '2001', 'Current Liabilities', 'LIABILITY', 'Current Liabilities', TRUE, 
       (SELECT id FROM account WHERE code = '2000')
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '2001');

-- Create Accounts Payable account (Code: 2001.01) - CREDIT ACCOUNT
INSERT INTO account (code, name, type, description, is_active, parent_id) 
SELECT '2001.01', 'Accounts Payable', 'LIABILITY', 'Money owed to suppliers', TRUE,
       (SELECT id FROM account WHERE code = '2001')
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '2001.01');

-- Create Duty and Taxes account (Code: 7001) - DEBIT ACCOUNT FOR TAX
INSERT INTO account (code, name, type, description, is_active) 
SELECT '7001', 'Duty and Taxes', 'LIABILITY', 'Duty and Taxes', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '7001');

-- Create parent EXPENSES account if it doesn't exist
INSERT INTO account (code, name, type, description, is_active) 
SELECT '4000', 'EXPENSES', 'EXPENSE', 'All Expenses', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '4000');

-- Create Purchase / Cost of Goods Sold account (Code: 6001) - DEBIT ACCOUNT FOR NET AMOUNT
INSERT INTO account (code, name, type, description, is_active, parent_id) 
SELECT '6001', 'Purchase / Cost of Goods Sold', 'EXPENSE', 'Purchase of Goods and Services', TRUE,
       (SELECT id FROM account WHERE code = '4000')
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '6001');

-- Create parent ASSETS account if it doesn't exist
INSERT INTO account (code, name, type, description, is_active) 
SELECT '1000', 'ASSETS', 'ASSET', 'All Assets', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '1000');

-- Create Current Assets parent account
INSERT INTO account (code, name, type, description, is_active, parent_id) 
SELECT '1001', 'Current Assets', 'ASSET', 'Current Assets', TRUE, 
       (SELECT id FROM account WHERE code = '1000')
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '1001');

-- Create Cash account (Code: 1001) - FOR PAYMENT VOUCHERS
INSERT INTO account (code, name, type, description, is_active, parent_id) 
SELECT '1001', 'Cash', 'ASSET', 'Cash in Hand', TRUE,
       (SELECT id FROM account WHERE code = '1001')
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '1001');

-- Create Bank Account (Code: 1002) - FOR PAYMENT VOUCHERS
INSERT INTO account (code, name, type, description, is_active, parent_id) 
SELECT '1002', 'Bank Account', 'ASSET', 'Bank Account', TRUE,
       (SELECT id FROM account WHERE code = '1001')
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '1002');

-- Show the accounts we just created/verified
SELECT 'Required accounts for split GRN voucher creation:' as info;
SELECT 
    CASE 
        WHEN code = '2001.01' THEN 'Accounts Payable (Credit - Grand Total)'
        WHEN code = '7001' THEN 'Duty and Taxes (Debit - Tax Amount)'
        WHEN code = '6001' THEN 'Purchase/Cost of Goods Sold (Debit - Net Amount)'
        WHEN code = '1001' THEN 'Cash (for payment vouchers)'
        WHEN code = '1002' THEN 'Bank Account (for payment vouchers)'
    END as account_purpose,
    id, code, name
FROM account 
WHERE code IN ('2001.01', '7001', '6001', '1001', '1002') 
ORDER BY code;

-- Show final account structure
SELECT 'Final account structure:' as info;
SELECT id, code, name, type, is_active FROM account ORDER BY id;

-- Test account lookups that the code will use
SELECT 'Testing account lookups:' as info;
SELECT 'Accounts Payable (2001.01):' as test, id, code, name FROM account WHERE code = '2001.01';
SELECT 'Duty and Taxes (7001):' as test, id, code, name FROM account WHERE code = '7001';
SELECT 'Purchase/Cost of Goods Sold (6001):' as test, id, code, name FROM account WHERE code = '6001';
SELECT 'Cash (1001):' as test, id, code, name FROM account WHERE code = '1001';
SELECT 'Bank Account (1002):' as test, id, code, name FROM account WHERE code = '1002';
