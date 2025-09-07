-- Check voucher data in the database
-- Run this in your PostgreSQL database to see what data exists

-- 1. Check if tables exist
SELECT 'Tables check:' as info;
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name IN ('account', 'voucher', 'voucher_entry')
ORDER BY table_name;

-- 2. Check account data
SELECT 'Account count:' as info, COUNT(*) as count FROM account;
SELECT 'Active accounts:' as info, COUNT(*) as count FROM account WHERE is_active = true;

-- 3. Check voucher data
SELECT 'Voucher count:' as info, COUNT(*) as count FROM voucher;
SELECT 'Voucher entries count:' as info, COUNT(*) as count FROM voucher_entry;

-- 4. Show sample data
SELECT 'Sample accounts:' as info;
SELECT id, code, name, type, is_active FROM account LIMIT 5;

SELECT 'Sample vouchers:' as info;
SELECT id, date, narration, type FROM voucher LIMIT 5;

SELECT 'Sample voucher entries:' as info;
SELECT ve.id, ve.debit, ve.credit, ve.description, 
       a.name as account_name, v.date as voucher_date
FROM voucher_entry ve
LEFT JOIN account a ON ve.account_id = a.id
LEFT JOIN voucher v ON ve.voucher_id = v.id
LIMIT 5;

-- 5. Test the trial balance query
SELECT 'Trial balance test:' as info;
SELECT a.id, a.name, a.code, a.type, a.parent_id, 
       COALESCE(SUM(e.debit), 0) as total_debit,
       COALESCE(SUM(e.credit), 0) as total_credit
FROM account a
LEFT JOIN voucher_entry e ON e.account_id = a.id
LEFT JOIN voucher v ON e.voucher_id = v.id
WHERE a.is_active = true
AND (v.date IS NULL OR v.date BETWEEN '2024-01-01' AND '2024-12-31')
GROUP BY a.id, a.name, a.code, a.type, a.parent_id
ORDER BY a.code;
