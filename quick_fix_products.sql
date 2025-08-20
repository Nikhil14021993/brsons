-- Quick Fix Script for Products Table
-- Run this if your products table still has the old structure

-- 1. Add new columns if they don't exist
ALTER TABLE products 
ADD COLUMN IF NOT EXISTS description TEXT,
ADD COLUMN IF NOT EXISTS retail_price DECIMAL(10,2),
ADD COLUMN IF NOT EXISTS b2b_price DECIMAL(10,2),
ADD COLUMN IF NOT EXISTS discount DECIMAL(5,2) DEFAULT 0.0,
ADD COLUMN IF NOT EXISTS stock_quantity INT DEFAULT 10,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 2. Migrate existing data
UPDATE products SET retail_price = price WHERE retail_price IS NULL AND price IS NOT NULL;
UPDATE products SET b2b_price = retail_price * 0.8 WHERE b2b_price IS NULL AND retail_price IS NOT NULL;
UPDATE products SET stock_quantity = 10 WHERE stock_quantity IS NULL;
UPDATE products SET description = CONCAT('Product: ', product_name) WHERE description IS NULL OR description = '';
UPDATE products SET status = 'Active' WHERE status IS NULL OR status = '';

-- 3. Check the results
SELECT 
    id,
    product_name,
    COALESCE(description, 'NULL') as description,
    COALESCE(retail_price, 'NULL') as retail_price,
    COALESCE(b2b_price, 'NULL') as b2b_price,
    COALESCE(status, 'NULL') as status
FROM products 
LIMIT 10;

-- 4. Verify products exist for category 9
SELECT COUNT(*) as products_in_category_9
FROM products p
JOIN categories c ON p.category_id = c.id
WHERE c.id = 9;
