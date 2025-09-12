-- Setup P&L Accounts Script
-- This script creates the necessary REVENUE and EXPENSE accounts for P&L reporting

-- First, check current accounts
SELECT 'Current Account Count:' as info, COUNT(*) as count FROM account;
SELECT 'Account Types:' as info, type, COUNT(*) as count FROM account GROUP BY type;

-- Create Revenue/Income accounts if they don't exist
INSERT INTO account (code, name, type, description, is_active) 
SELECT '3001', 'Sales Revenue', 'INCOME', 'Sales Revenue from Products', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '3001');

INSERT INTO account (code, name, type, description, is_active) 
SELECT '3002', 'Service Revenue', 'INCOME', 'Service Revenue', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '3002');

INSERT INTO account (code, name, type, description, is_active) 
SELECT '3003', 'Other Income', 'INCOME', 'Other Income Sources', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '3003');

-- Create Expense accounts if they don't exist
INSERT INTO account (code, name, type, description, is_active) 
SELECT '4001', 'Purchase Expense', 'EXPENSE', 'Cost of Goods Sold', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '4001');

INSERT INTO account (code, name, type, description, is_active) 
SELECT '4002', 'Operating Expenses', 'EXPENSE', 'General Operating Expenses', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '4002');

INSERT INTO account (code, name, type, description, is_active) 
SELECT '4003', 'Rent Expense', 'EXPENSE', 'Rent and Utilities', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '4003');

INSERT INTO account (code, name, type, description, is_active) 
SELECT '4004', 'Salary Expense', 'EXPENSE', 'Employee Salaries', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '4004');

INSERT INTO account (code, name, type, description, is_active) 
SELECT '4005', 'Marketing Expense', 'EXPENSE', 'Marketing and Advertising', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '4005');

-- Show the created accounts
SELECT 'Created Accounts:' as info;
SELECT code, name, type, description FROM account WHERE type IN ('INCOME', 'EXPENSE') ORDER BY type, code;

-- Show sample voucher entries for testing
SELECT 'Sample Voucher Entries for Testing:' as info;
SELECT 'To create a sales transaction:' as instruction;
SELECT 'Debit: Cash (Asset) - 1000' as debit_entry;
SELECT 'Credit: Sales Revenue (Income) - 1000' as credit_entry;
SELECT '' as separator;
SELECT 'To create a purchase transaction:' as instruction;
SELECT 'Debit: Purchase Expense (Expense) - 500' as debit_entry;
SELECT 'Credit: Cash (Asset) - 500' as credit_entry;
