-- Check and insert basic accounts for voucher system
-- Connect to your PostgreSQL database and run this script

-- First, check if we have any accounts
SELECT 'Current account count:' as info, COUNT(*) as count FROM account;

-- Check if the account table exists and has the right structure
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'account' 
ORDER BY ordinal_position;

-- Insert basic accounts if they don't exist
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

-- Insert some specific accounts
INSERT INTO account (code, name, type, description, is_active) 
SELECT '1001', 'Cash', 'ASSET', 'Cash in Hand', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '1001');

INSERT INTO account (code, name, type, description, is_active) 
SELECT '1002', 'Bank Account', 'ASSET', 'Bank Account', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '1002');

INSERT INTO account (code, name, type, description, is_active) 
SELECT '3001', 'Sales', 'INCOME', 'Sales Revenue', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '3001');

INSERT INTO account (code, name, type, description, is_active) 
SELECT '4001', 'Purchase', 'EXPENSE', 'Purchase of Goods', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '4001');

-- Show all accounts after insertion
SELECT 'All accounts after insertion:' as info;
SELECT id, code, name, type, is_active, parent_id FROM account ORDER BY code;
