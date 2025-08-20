-- Add Price Columns to Order Item Table
-- This script adds columns to store historical pricing information

-- 1. Add new columns to order_item table
ALTER TABLE order_item 
ADD COLUMN unit_price DECIMAL(10,2) DEFAULT NULL COMMENT 'Price per unit at order time',
ADD COLUMN total_price DECIMAL(10,2) DEFAULT NULL COMMENT 'Total price for this item (unit_price * quantity)',
ADD COLUMN user_type VARCHAR(20) DEFAULT NULL COMMENT 'User type at order time (Retail, B2B, Admin)',
ADD COLUMN price_type VARCHAR(20) DEFAULT NULL COMMENT 'Price type used (retail, b2b, admin)';

-- 2. Update existing order items with calculated prices (if you have existing orders)
-- This will populate the new columns for existing orders
UPDATE order_item oi
JOIN orders o ON oi.order_id = o.id
JOIN products p ON oi.product_id = p.id
SET 
    oi.unit_price = CASE 
        WHEN oi.user_type = 'B2B' AND p.b2b_price IS NOT NULL THEN p.b2b_price
        ELSE p.retail_price
    END,
    oi.total_price = CASE 
        WHEN oi.user_type = 'B2B' AND p.b2b_price IS NOT NULL THEN p.b2b_price * oi.quantity
        ELSE p.retail_price * oi.quantity
    END,
    oi.price_type = CASE 
        WHEN oi.user_type = 'B2B' AND p.b2b_price IS NOT NULL THEN 'b2b'
        ELSE 'retail'
    END
WHERE oi.unit_price IS NULL;

-- 3. Add indexes for better performance
CREATE INDEX idx_order_item_unit_price ON order_item(unit_price);
CREATE INDEX idx_order_item_user_type ON order_item(user_type);
CREATE INDEX idx_order_item_price_type ON order_item(price_type);

-- 4. Verify the changes
SELECT 
    'order_item' as table_name,
    COUNT(*) as total_rows,
    COUNT(unit_price) as rows_with_unit_price,
    COUNT(total_price) as rows_with_total_price,
    COUNT(user_type) as rows_with_user_type,
    COUNT(price_type) as rows_with_price_type
FROM order_item;

-- 5. Show sample data
SELECT 
    oi.id,
    oi.product_id,
    oi.quantity,
    oi.unit_price,
    oi.total_price,
    oi.user_type,
    oi.price_type
FROM order_item oi
LIMIT 10;
