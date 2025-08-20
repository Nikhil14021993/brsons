-- Database Update Script for Brsons E-commerce
-- This script updates the existing products table and creates the new product_variants table

-- 1. Backup existing products table (optional but recommended)
-- CREATE TABLE products_backup AS SELECT * FROM products;

-- 2. Add new columns to existing products table
ALTER TABLE products 
ADD COLUMN description TEXT,
ADD COLUMN retail_price DECIMAL(10,2),
ADD COLUMN b2b_price DECIMAL(10,2),
ADD COLUMN discount DECIMAL(5,2) DEFAULT 0.0,
ADD COLUMN stock_quantity INT DEFAULT 0,
ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 3. Migrate existing data
-- Set retail_price to existing price
UPDATE products SET retail_price = price WHERE retail_price IS NULL;

-- Set b2b_price to retail_price * 0.8 (20% discount for B2B) - adjust as needed
UPDATE products SET b2b_price = retail_price * 0.8 WHERE b2b_price IS NULL;

-- Set default stock quantity
UPDATE products SET stock_quantity = 10 WHERE stock_quantity IS NULL;

-- Set default description
UPDATE products SET description = CONCAT('Product: ', product_name) WHERE description IS NULL OR description = '';

-- 4. Create product_variants table
CREATE TABLE product_variants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    size VARCHAR(50),
    color VARCHAR(50),
    stock_quantity INT DEFAULT 0,
    retail_price DECIMAL(10,2),
    b2b_price DECIMAL(10,2),
    variant_discount DECIMAL(5,2),
    sku VARCHAR(100) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'Active',
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- 5. Create indexes for better performance
CREATE INDEX idx_product_variants_product_id ON product_variants(product_id);
CREATE INDEX idx_product_variants_sku ON product_variants(sku);
CREATE INDEX idx_product_variants_status ON product_variants(status);
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_status ON products(status);
CREATE INDEX idx_products_retail_price ON products(retail_price);
CREATE INDEX idx_products_b2b_price ON products(b2b_price);

-- 6. Insert sample variants for existing products (optional)
-- This creates basic variants for existing products based on their current size and color
INSERT INTO product_variants (product_id, size, color, stock_quantity, retail_price, b2b_price, variant_discount, sku, status)
SELECT 
    p.id,
    p.size,
    p.colour,
    p.stock_quantity,
    p.retail_price,
    p.b2b_price,
    p.discount,
    CONCAT(UPPER(SUBSTRING(p.product_name, 1, 3)), '-', 
           UPPER(SUBSTRING(COALESCE(p.size, 'NA'), 1, 2)), '-',
           UPPER(SUBSTRING(COALESCE(p.colour, 'NA'), 1, 2)), '-',
           LPAD(p.id, 4, '0')) as sku,
    'Active'
FROM products p
WHERE p.size IS NOT NULL OR p.colour IS NOT NULL;

-- 7. Update existing products to remove old fields (optional - uncomment if you want to clean up)
-- ALTER TABLE products DROP COLUMN price;
-- ALTER TABLE products DROP COLUMN size;
-- ALTER TABLE products DROP COLUMN colour;

-- 8. Add constraints
ALTER TABLE products 
MODIFY COLUMN retail_price DECIMAL(10,2) NOT NULL,
MODIFY COLUMN b2b_price DECIMAL(10,2) NOT NULL,
MODIFY COLUMN stock_quantity INT NOT NULL DEFAULT 0,
MODIFY COLUMN description TEXT NOT NULL;

-- 9. Add check constraints for valid values
ALTER TABLE products 
ADD CONSTRAINT chk_retail_price CHECK (retail_price >= 0),
ADD CONSTRAINT chk_b2b_price CHECK (b2b_price >= 0),
ADD CONSTRAINT chk_discount CHECK (discount >= 0 AND discount <= 100),
ADD CONSTRAINT chk_stock_quantity CHECK (stock_quantity >= 0);

ALTER TABLE product_variants 
ADD CONSTRAINT chk_variant_stock_quantity CHECK (stock_quantity >= 0),
ADD CONSTRAINT chk_variant_retail_price CHECK (retail_price >= 0),
ADD CONSTRAINT chk_variant_b2b_price CHECK (b2b_price >= 0),
ADD CONSTRAINT chk_variant_discount CHECK (variant_discount >= 0 AND variant_discount <= 100);

-- 10. Create view for easy product-variant access (optional)
CREATE VIEW product_with_variants AS
SELECT 
    p.*,
    pv.id as variant_id,
    pv.size as variant_size,
    pv.color as variant_color,
    pv.stock_quantity as variant_stock,
    pv.retail_price as variant_retail_price,
    pv.b2b_price as variant_b2b_price,
    pv.variant_discount,
    pv.sku,
    pv.status as variant_status
FROM products p
LEFT JOIN product_variants pv ON p.id = pv.product_id;

-- 11. Create view for stock summary (optional)
CREATE VIEW product_stock_summary AS
SELECT 
    p.id,
    p.product_name,
    p.retail_price,
    p.b2b_price,
    p.stock_quantity as total_stock,
    COUNT(pv.id) as variant_count,
    SUM(COALESCE(pv.stock_quantity, 0)) as total_variant_stock,
    CASE 
        WHEN p.stock_quantity <= 0 THEN 'Out of Stock'
        WHEN p.stock_quantity <= 5 THEN 'Low Stock'
        ELSE 'In Stock'
    END as stock_status
FROM products p
LEFT JOIN product_variants pv ON p.id = pv.product_id
GROUP BY p.id, p.product_name, p.retail_price, p.b2b_price, p.stock_quantity;

-- 12. Insert sample data for testing (optional)
-- INSERT INTO product_variants (product_id, size, color, stock_quantity, retail_price, b2b_price, sku, status)
-- VALUES 
-- (1, 'S', 'Red', 5, 25.99, 20.79, 'SAMPLE-S-RD-0001', 'Active'),
-- (1, 'M', 'Red', 8, 25.99, 20.79, 'SAMPLE-M-RD-0002', 'Active'),
-- (1, 'L', 'Red', 3, 25.99, 20.79, 'SAMPLE-L-RD-0003', 'Active');

-- 13. Update existing products with better descriptions (optional)
UPDATE products 
SET description = CONCAT('High-quality ', LOWER(product_name), ' available in various sizes and colors. Perfect for everyday wear.')
WHERE description LIKE 'Product: %';

-- 14. Set default values for any remaining NULL fields
UPDATE products 
SET discount = 0.0 WHERE discount IS NULL,
    stock_quantity = 10 WHERE stock_quantity IS NULL,
    status = 'Active' WHERE status IS NULL;

-- 15. Create trigger to update updated_at timestamp
DELIMITER //
CREATE TRIGGER update_products_updated_at 
BEFORE UPDATE ON products
FOR EACH ROW
BEGIN
    SET NEW.updated_at = CURRENT_TIMESTAMP;
END//

CREATE TRIGGER update_product_variants_updated_at 
BEFORE UPDATE ON product_variants
FOR EACH ROW
BEGIN
    SET NEW.updated_at = CURRENT_TIMESTAMP;
END//
DELIMITER ;

-- Success message
SELECT 'Database schema updated successfully!' as message;
SELECT 'Products table updated with new fields' as detail;
SELECT 'Product variants table created' as detail;
SELECT 'Indexes and constraints added' as detail;
SELECT 'Sample data migrated' as detail;
