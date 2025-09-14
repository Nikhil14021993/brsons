-- Add custom product fields to order_item table
ALTER TABLE order_item 
ADD COLUMN is_custom_product BOOLEAN DEFAULT FALSE,
ADD COLUMN custom_product_name VARCHAR(255),
ADD COLUMN custom_product_sku VARCHAR(100),
ADD COLUMN custom_product_description VARCHAR(500);
