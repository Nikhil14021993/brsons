-- Insert basic accounts if they don't exist
-- This script ensures we have some accounts for testing

-- Check if we have any accounts
SELECT COUNT(*) as account_count FROM account;

-- Insert basic accounts if none exist
INSERT INTO account (code, name, type, description, is_active) 
SELECT '1000', 'ASSETS', 'ASSET', 'All Assets', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '1000');

INSERT INTO account (code, name, type, description, is_active) 
SELECT '2000', 'LIABILITIES', 'LIABILITY', 'All Liabilities', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '2000');

INSERT INTO account (code, name, type, description, is_active) 
SELECT '3000', 'INCOME', 'INCOME', 'All Income', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '3000');

INSERT INTO account (code, name, type, description, is_active) 
SELECT '4000', 'EXPENSES', 'EXPENSE', 'All Expenses', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '4000');

INSERT INTO account (code, name, type, description, is_active) 
SELECT '5000', 'EQUITY', 'EQUITY', 'All Equity', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '5000');

-- Insert Cash account
INSERT INTO account (code, name, type, description, is_active) 
SELECT '1001', 'Cash', 'ASSET', 'Cash in Hand', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '1001');

-- Insert Bank account
INSERT INTO account (code, name, type, description, is_active) 
SELECT '1002', 'Bank Account', 'ASSET', 'Bank Account', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '1002');

-- Insert Sales account
INSERT INTO account (code, name, type, description, is_active) 
SELECT '3001', 'Sales', 'INCOME', 'Sales Revenue', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '3001');

-- Insert Purchase account
INSERT INTO account (code, name, type, description, is_active) 
SELECT '4001', 'Purchase', 'EXPENSE', 'Purchase of Goods', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '4001');

-- Show all accounts
SELECT id, code, name, type, is_active FROM account ORDER BY code;
