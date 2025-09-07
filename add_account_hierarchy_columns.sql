-- Add hierarchy support to Account table
-- This script adds parent-child relationship support for sub-accounts

-- Add new columns to Account table
ALTER TABLE account ADD COLUMN description VARCHAR(500);
ALTER TABLE account ADD COLUMN is_active BOOLEAN DEFAULT TRUE;
ALTER TABLE account ADD COLUMN parent_id BIGINT;

-- Add foreign key constraint for parent-child relationship
ALTER TABLE account ADD CONSTRAINT fk_account_parent 
    FOREIGN KEY (parent_id) REFERENCES account(id);

-- Add index for better performance
CREATE INDEX idx_account_parent_id ON account(parent_id);
CREATE INDEX idx_account_is_active ON account(is_active);

-- Update existing accounts to be active by default
UPDATE account SET is_active = TRUE WHERE is_active IS NULL;

-- Add some sample accounts with hierarchy for testing
INSERT INTO account (code, name, type, description, is_active) VALUES 
('1000', 'ASSETS', 'ASSET', 'All Assets', TRUE),
('2000', 'LIABILITIES', 'LIABILITY', 'All Liabilities', TRUE),
('3000', 'INCOME', 'INCOME', 'All Income', TRUE),
('4000', 'EXPENSES', 'EXPENSE', 'All Expenses', TRUE),
('5000', 'EQUITY', 'EQUITY', 'All Equity', TRUE);

-- Add sub-accounts under ASSETS
INSERT INTO account (code, name, type, description, is_active, parent_id) VALUES 
('1001', 'Current Assets', 'ASSET', 'Current Assets', TRUE, (SELECT id FROM account WHERE code = '1000')),
('1002', 'Fixed Assets', 'ASSET', 'Fixed Assets', TRUE, (SELECT id FROM account WHERE code = '1000'));

-- Add specific accounts under Current Assets
INSERT INTO account (code, name, type, description, is_active, parent_id) VALUES 
('1001.01', 'Cash', 'ASSET', 'Cash in Hand', TRUE, (SELECT id FROM account WHERE code = '1001')),
('1001.02', 'Bank Account', 'ASSET', 'Bank Account', TRUE, (SELECT id FROM account WHERE code = '1001')),
('1001.03', 'Accounts Receivable', 'ASSET', 'Money owed by customers', TRUE, (SELECT id FROM account WHERE code = '1001'));

-- Add specific accounts under Fixed Assets
INSERT INTO account (code, name, type, description, is_active, parent_id) VALUES 
('1002.01', 'Equipment', 'ASSET', 'Office Equipment', TRUE, (SELECT id FROM account WHERE code = '1002')),
('1002.02', 'Furniture', 'ASSET', 'Office Furniture', TRUE, (SELECT id FROM account WHERE code = '1002'));

-- Add sub-accounts under LIABILITIES
INSERT INTO account (code, name, type, description, is_active, parent_id) VALUES 
('2001', 'Current Liabilities', 'LIABILITY', 'Current Liabilities', TRUE, (SELECT id FROM account WHERE code = '2000')),
('2002', 'Long-term Liabilities', 'LIABILITY', 'Long-term Liabilities', TRUE, (SELECT id FROM account WHERE code = '2000'));

-- Add specific accounts under Current Liabilities
INSERT INTO account (code, name, type, description, is_active, parent_id) VALUES 
('2001.01', 'Accounts Payable', 'LIABILITY', 'Money owed to suppliers', TRUE, (SELECT id FROM account WHERE code = '2001')),
('2001.02', 'Accrued Expenses', 'LIABILITY', 'Accrued Expenses', TRUE, (SELECT id FROM account WHERE code = '2001'));

-- Add sub-accounts under INCOME
INSERT INTO account (code, name, type, description, is_active, parent_id) VALUES 
('3001', 'Sales Revenue', 'INCOME', 'Sales Revenue', TRUE, (SELECT id FROM account WHERE code = '3000')),
('3002', 'Other Income', 'INCOME', 'Other Income', TRUE, (SELECT id FROM account WHERE code = '3000'));

-- Add specific accounts under Sales Revenue
INSERT INTO account (code, name, type, description, is_active, parent_id) VALUES 
('3001.01', 'Retail Sales', 'INCOME', 'Retail Sales Revenue', TRUE, (SELECT id FROM account WHERE code = '3001')),
('3001.02', 'B2B Sales', 'INCOME', 'B2B Sales Revenue', TRUE, (SELECT id FROM account WHERE code = '3001'));

-- Add sub-accounts under EXPENSES
INSERT INTO account (code, name, type, description, is_active, parent_id) VALUES 
('4001', 'Operating Expenses', 'EXPENSE', 'Operating Expenses', TRUE, (SELECT id FROM account WHERE code = '4000')),
('4002', 'Cost of Goods Sold', 'EXPENSE', 'Cost of Goods Sold', TRUE, (SELECT id FROM account WHERE code = '4000'));

-- Add specific accounts under Operating Expenses
INSERT INTO account (code, name, type, description, is_active, parent_id) VALUES 
('4001.01', 'Rent', 'EXPENSE', 'Office Rent', TRUE, (SELECT id FROM account WHERE code = '4001')),
('4001.02', 'Utilities', 'EXPENSE', 'Electricity, Water, etc.', TRUE, (SELECT id FROM account WHERE code = '4001')),
('4001.03', 'Salaries', 'EXPENSE', 'Employee Salaries', TRUE, (SELECT id FROM account WHERE code = '4001'));

-- Add specific accounts under Cost of Goods Sold
INSERT INTO account (code, name, type, description, is_active, parent_id) VALUES 
('4002.01', 'Purchase of Goods', 'EXPENSE', 'Cost of Purchased Goods', TRUE, (SELECT id FROM account WHERE code = '4002')),
('4002.02', 'Freight Charges', 'EXPENSE', 'Shipping and Freight', TRUE, (SELECT id FROM account WHERE code = '4002'));

-- Add sub-accounts under EQUITY
INSERT INTO account (code, name, type, description, is_active, parent_id) VALUES 
('5001', 'Owner Equity', 'EQUITY', 'Owner Equity', TRUE, (SELECT id FROM account WHERE code = '5000')),
('5002', 'Retained Earnings', 'EQUITY', 'Retained Earnings', TRUE, (SELECT id FROM account WHERE code = '5000'));

-- Add specific accounts under Owner Equity
INSERT INTO account (code, name, type, description, is_active, parent_id) VALUES 
('5001.01', 'Capital', 'EQUITY', 'Owner Capital', TRUE, (SELECT id FROM account WHERE code = '5001')),
('5001.02', 'Drawings', 'EQUITY', 'Owner Drawings', TRUE, (SELECT id FROM account WHERE code = '5001'));

COMMIT;
