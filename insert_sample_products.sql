-- Insert sample products for testing
-- First, create a category if it doesn't exist
INSERT INTO categories (category_name, status, created_at, updated_at) 
VALUES ('Clothing', 'Active', NOW(), NOW())
ON DUPLICATE KEY UPDATE category_name = category_name;

-- Get the category ID
SET @category_id = LAST_INSERT_ID();
-- If the category already exists, get its ID
SELECT @category_id := id FROM categories WHERE category_name = 'Clothing' LIMIT 1;

-- Insert sample products
INSERT INTO products (
    product_name, 
    description, 
    retail_price, 
    b2b_price, 
    purchase_price,
    b2b_min_quantity,
    discount, 
    stock_quantity, 
    reserved_quantity,
    status, 
    category_id,
    sku,
    created_at, 
    updated_at
) VALUES 
('Cotton T-Shirt', 'Comfortable cotton t-shirt in various sizes', 299.00, 250.00, 150.00, 10, 0.00, 50, 0, 'Active', @category_id, 'TSH-001', NOW(), NOW()),
('Denim Jeans', 'Classic blue denim jeans', 1299.00, 1100.00, 650.00, 5, 10.00, 25, 0, 'Active', @category_id, 'JNS-001', NOW(), NOW()),
('Polo Shirt', 'Premium polo shirt for casual wear', 599.00, 500.00, 300.00, 8, 5.00, 30, 0, 'Active', @category_id, 'POL-001', NOW(), NOW()),
('Hoodie', 'Warm and comfortable hoodie', 899.00, 750.00, 450.00, 6, 0.00, 20, 0, 'Active', @category_id, 'HOD-001', NOW(), NOW()),
('Cargo Pants', 'Utility cargo pants with multiple pockets', 1099.00, 900.00, 550.00, 4, 15.00, 15, 0, 'Active', @category_id, 'CAR-001', NOW(), NOW()),
('Sweater', 'Soft woolen sweater for winter', 799.00, 650.00, 400.00, 5, 0.00, 18, 0, 'Active', @category_id, 'SWT-001', NOW(), NOW()),
('Shorts', 'Comfortable summer shorts', 399.00, 320.00, 200.00, 12, 0.00, 35, 0, 'Active', @category_id, 'SHT-001', NOW(), NOW()),
('Jacket', 'Stylish denim jacket', 1499.00, 1200.00, 750.00, 3, 20.00, 10, 0, 'Active', @category_id, 'JCK-001', NOW(), NOW());

-- Verify the products were inserted
SELECT 
    id, 
    product_name, 
    retail_price, 
    b2b_price, 
    stock_quantity, 
    status,
    sku
FROM products 
WHERE status = 'Active'
ORDER BY created_at DESC;
