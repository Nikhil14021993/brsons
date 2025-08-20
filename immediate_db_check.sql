-- Immediate Database Check - Run this first!
-- This will show you exactly what's in your products table

-- 1. Check table structure
DESCRIBE products;

-- 2. Check if you have any products at all
SELECT COUNT(*) as total_products FROM products;

-- 3. Check if you have products in category 9
SELECT COUNT(*) as products_in_category_9 
FROM products p 
JOIN categories c ON p.category_id = c.id 
WHERE c.id = 9;

-- 4. Show sample products with their current structure
SELECT 
    id,
    product_name,
    COALESCE(price, 'NULL') as old_price,
    COALESCE(retail_price, 'NULL') as new_retail_price,
    COALESCE(b2b_price, 'NULL') as new_b2b_price,
    COALESCE(status, 'NULL') as status,
    COALESCE(category_id, 'NULL') as category_id
FROM products 
LIMIT 5;

-- 5. Check what categories exist
SELECT id, category_name, status FROM categories;
