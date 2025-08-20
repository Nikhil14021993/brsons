-- Database Check Script for Brsons E-commerce
-- Run this to check your current database structure and data

-- 1. Check if the new columns exist in products table
DESCRIBE products;

-- 2. Check if product_variants table exists
SHOW TABLES LIKE 'product_variants';

-- 3. Check current product data structure
SELECT 
    id,
    product_name,
    COALESCE(description, 'NULL') as description,
    COALESCE(retail_price, 'NULL') as retail_price,
    COALESCE(b2b_price, 'NULL') as b2b_price,
    COALESCE(discount, 'NULL') as discount,
    COALESCE(stock_quantity, 'NULL') as stock_quantity,
    COALESCE(status, 'NULL') as status,
    COALESCE(created_at, 'NULL') as created_at,
    COALESCE(updated_at, 'NULL') as updated_at,
    COALESCE(category_id, 'NULL') as category_id
FROM products 
LIMIT 5;

-- 4. Check if old columns still exist
SELECT 
    id,
    product_name,
    COALESCE(price, 'NULL') as price,
    COALESCE(colour, 'NULL') as colour,
    COALESCE(size, 'NULL') as size
FROM products 
LIMIT 5;

-- 5. Check category data
SELECT 
    id,
    category_name,
    status
FROM categories 
LIMIT 10;

-- 6. Check products by category
SELECT 
    p.id,
    p.product_name,
    p.status,
    c.category_name,
    COALESCE(p.retail_price, 'NULL') as retail_price
FROM products p
LEFT JOIN categories c ON p.category_id = c.id
WHERE c.id = 9
LIMIT 10;

-- 7. Count products by status
SELECT 
    COALESCE(status, 'NULL') as status,
    COUNT(*) as count
FROM products 
GROUP BY status;

-- 8. Check if any products exist for category 9
SELECT COUNT(*) as total_products_for_category_9
FROM products p
JOIN categories c ON p.category_id = c.id
WHERE c.id = 9;
