-- Business Management Tables Migration Script
-- This script creates tables for Purchase Orders, GRN, and Supplier Credit Notes functionality
-- Run this script in your MySQL database

-- Drop tables if they exist (in reverse order due to foreign key constraints)
DROP TABLE IF EXISTS credit_note_items;
DROP TABLE IF EXISTS supplier_credit_notes;
DROP TABLE IF EXISTS grn_items;
DROP TABLE IF EXISTS goods_received_notes;
DROP TABLE IF EXISTS purchase_order_items;
DROP TABLE IF EXISTS purchase_orders;
DROP TABLE IF EXISTS suppliers;

-- Create Suppliers Table
CREATE TABLE suppliers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_code VARCHAR(50) UNIQUE NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    contact_person VARCHAR(100),
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    zip_code VARCHAR(20),
    country VARCHAR(100),
    gstin VARCHAR(15),
    pan VARCHAR(10),
    payment_terms VARCHAR(100),
    credit_limit DECIMAL(15,2),
    current_balance DECIMAL(15,2) DEFAULT 0.00,
    status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED', 'BLACKLISTED') DEFAULT 'ACTIVE',
    rating INT CHECK (rating >= 1 AND rating <= 5),
    notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_supplier_code (supplier_code),
    INDEX idx_company_name (company_name),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

-- Create Purchase Orders Table
CREATE TABLE purchase_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    po_number VARCHAR(50) UNIQUE NOT NULL,
    supplier_id BIGINT NOT NULL,
    order_date DATE NOT NULL,
    expected_delivery_date DATE,
    delivery_address TEXT,
    payment_terms VARCHAR(100),
    subtotal DECIMAL(15,2) DEFAULT 0.00,
    tax_amount DECIMAL(15,2) DEFAULT 0.00,
    shipping_cost DECIMAL(15,2) DEFAULT 0.00,
    discount_amount DECIMAL(15,2) DEFAULT 0.00,
    total_amount DECIMAL(15,2) DEFAULT 0.00,
    status ENUM('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'ORDERED', 'PARTIALLY_RECEIVED', 'FULLY_RECEIVED', 'CANCELLED') DEFAULT 'DRAFT',
    notes TEXT,
    created_by VARCHAR(100),
    approved_by VARCHAR(100),
    approved_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE RESTRICT,
    INDEX idx_po_number (po_number),
    INDEX idx_supplier_id (supplier_id),
    INDEX idx_status (status),
    INDEX idx_order_date (order_date),
    INDEX idx_created_at (created_at)
);

-- Create Purchase Order Items Table
CREATE TABLE purchase_order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    ordered_quantity INT NOT NULL,
    received_quantity INT DEFAULT 0,
    unit_price DECIMAL(10,2) NOT NULL,
    discount_percentage DECIMAL(5,2) DEFAULT 0.00,
    discount_amount DECIMAL(10,2) DEFAULT 0.00,
    tax_percentage DECIMAL(5,2) DEFAULT 0.00,
    tax_amount DECIMAL(10,2) DEFAULT 0.00,
    total_amount DECIMAL(12,2) DEFAULT 0.00,
    notes TEXT,
    
    FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    INDEX idx_purchase_order_id (purchase_order_id),
    INDEX idx_product_id (product_id)
);

-- Create Goods Received Notes Table
CREATE TABLE goods_received_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    grn_number VARCHAR(50) UNIQUE NOT NULL,
    purchase_order_id BIGINT NOT NULL,
    supplier_id BIGINT NOT NULL,
    received_date DATE NOT NULL,
    delivery_note_number VARCHAR(100),
    vehicle_number VARCHAR(20),
    received_by VARCHAR(100),
    inspected_by VARCHAR(100),
    status ENUM('RECEIVED', 'INSPECTED', 'APPROVED', 'REJECTED', 'PARTIALLY_ACCEPTED') DEFAULT 'RECEIVED',
    subtotal DECIMAL(15,2) DEFAULT 0.00,
    tax_amount DECIMAL(15,2) DEFAULT 0.00,
    total_amount DECIMAL(15,2) DEFAULT 0.00,
    quality_remarks TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id) ON DELETE RESTRICT,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE RESTRICT,
    INDEX idx_grn_number (grn_number),
    INDEX idx_purchase_order_id (purchase_order_id),
    INDEX idx_supplier_id (supplier_id),
    INDEX idx_status (status),
    INDEX idx_received_date (received_date),
    INDEX idx_created_at (created_at)
);

-- Create GRN Items Table
CREATE TABLE grn_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    grn_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    ordered_quantity INT NOT NULL,
    received_quantity INT NOT NULL,
    accepted_quantity INT DEFAULT 0,
    rejected_quantity INT DEFAULT 0,
    unit_price DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(12,2) DEFAULT 0.00,
    quality_status ENUM('PENDING', 'ACCEPTED', 'REJECTED', 'PARTIALLY_ACCEPTED') DEFAULT 'PENDING',
    rejection_reason VARCHAR(255),
    notes TEXT,
    
    FOREIGN KEY (grn_id) REFERENCES goods_received_notes(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    INDEX idx_grn_id (grn_id),
    INDEX idx_product_id (product_id),
    INDEX idx_quality_status (quality_status)
);

-- Create Supplier Credit Notes Table
CREATE TABLE supplier_credit_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    credit_note_number VARCHAR(50) UNIQUE NOT NULL,
    supplier_id BIGINT NOT NULL,
    purchase_order_id BIGINT NULL,
    grn_id BIGINT NULL,
    credit_note_date DATE NOT NULL,
    type ENUM('RETURN', 'DISCOUNT', 'DEFECTIVE', 'PRICE_ADJUSTMENT', 'OTHER') NOT NULL,
    status ENUM('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'APPLIED') DEFAULT 'DRAFT',
    subtotal DECIMAL(15,2) DEFAULT 0.00,
    tax_amount DECIMAL(15,2) DEFAULT 0.00,
    total_amount DECIMAL(15,2) DEFAULT 0.00,
    reason TEXT,
    reference_document VARCHAR(100),
    notes TEXT,
    created_by VARCHAR(100),
    approved_by VARCHAR(100),
    approved_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE RESTRICT,
    FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id) ON DELETE SET NULL,
    FOREIGN KEY (grn_id) REFERENCES goods_received_notes(id) ON DELETE SET NULL,
    INDEX idx_credit_note_number (credit_note_number),
    INDEX idx_supplier_id (supplier_id),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_credit_note_date (credit_note_date),
    INDEX idx_created_at (created_at)
);

-- Create Credit Note Items Table
CREATE TABLE credit_note_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    credit_note_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    discount_percentage DECIMAL(5,2) DEFAULT 0.00,
    discount_amount DECIMAL(10,2) DEFAULT 0.00,
    total_amount DECIMAL(12,2) DEFAULT 0.00,
    reason VARCHAR(255),
    notes TEXT,
    
    FOREIGN KEY (credit_note_id) REFERENCES supplier_credit_notes(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    INDEX idx_credit_note_id (credit_note_id),
    INDEX idx_product_id (product_id)
);

-- Create triggers to update parent document totals when items are updated
DELIMITER //

CREATE TRIGGER update_purchase_order_total_after_item_update
    AFTER UPDATE ON purchase_order_items
    FOR EACH ROW
BEGIN
    UPDATE purchase_orders 
    SET updated_at = CURRENT_TIMESTAMP 
    WHERE id = NEW.purchase_order_id;
END//

CREATE TRIGGER update_grn_total_after_item_update
    AFTER UPDATE ON grn_items
    FOR EACH ROW
BEGIN
    UPDATE goods_received_notes 
    SET updated_at = CURRENT_TIMESTAMP 
    WHERE id = NEW.grn_id;
END//

CREATE TRIGGER update_credit_note_total_after_item_update
    AFTER UPDATE ON credit_note_items
    FOR EACH ROW
BEGIN
    UPDATE supplier_credit_notes 
    SET updated_at = CURRENT_TIMESTAMP 
    WHERE id = NEW.credit_note_id;
END//

DELIMITER ;

-- Insert sample data

-- Sample Suppliers
INSERT INTO suppliers (supplier_code, company_name, contact_person, email, phone, address_line1, city, state, country, gstin, pan, payment_terms, credit_limit, status, rating, created_by) VALUES
('SUP001', 'ABC Electronics Ltd', 'John Smith', 'john@abcelectronics.com', '+91-9876543210', '123 Industrial Area', 'Mumbai', 'Maharashtra', 'India', '27ABCDE1234F1Z5', 'ABCDE1234F', '30 days', 500000.00, 'ACTIVE', 4, 'admin'),
('SUP002', 'XYZ Textiles Pvt Ltd', 'Sarah Johnson', 'sarah@xyztextiles.com', '+91-9876543211', '456 Textile Hub', 'Surat', 'Gujarat', 'India', '24XYZAB5678G2H6', 'XYZAB5678G', '45 days', 750000.00, 'ACTIVE', 5, 'admin'),
('SUP003', 'Global Fashion House', 'Mike Wilson', 'mike@globalfashion.com', '+91-9876543212', '789 Fashion Street', 'Delhi', 'Delhi', 'India', '07GLOBA9012I3J7', 'GLOBA9012I', '60 days', 1000000.00, 'ACTIVE', 3, 'admin');

-- Sample Purchase Order
INSERT INTO purchase_orders (po_number, supplier_id, order_date, expected_delivery_date, delivery_address, payment_terms, subtotal, tax_amount, total_amount, status, created_by) VALUES
('PO2024001', 1, '2024-01-15', '2024-01-30', '123 Main Street, Mumbai, Maharashtra', '30 days', 25000.00, 4500.00, 29500.00, 'APPROVED', 'admin');

-- Sample Purchase Order Items
INSERT INTO purchase_order_items (purchase_order_id, product_id, ordered_quantity, unit_price, tax_percentage, discount_percentage) VALUES
(1, 1, 50, 500.00, 18.00, 0.00);

-- Update purchase order item totals
UPDATE purchase_order_items 
SET discount_amount = (ordered_quantity * unit_price * discount_percentage / 100),
    tax_amount = (ordered_quantity * unit_price * (1 - discount_percentage/100) * tax_percentage / 100),
    total_amount = (ordered_quantity * unit_price * (1 - discount_percentage/100) * (1 + tax_percentage/100));

-- Sample GRN
INSERT INTO goods_received_notes (grn_number, purchase_order_id, supplier_id, received_date, delivery_note_number, vehicle_number, received_by, inspected_by, status, subtotal, tax_amount, total_amount) VALUES
('GRN2024001', 1, 1, '2024-01-28', 'DN001', 'MH01AB1234', 'Warehouse Manager', 'Quality Inspector', 'APPROVED', 25000.00, 4500.00, 29500.00);

-- Sample GRN Items
INSERT INTO grn_items (grn_id, product_id, ordered_quantity, received_quantity, accepted_quantity, rejected_quantity, unit_price, total_amount, quality_status) VALUES
(1, 1, 50, 50, 48, 2, 500.00, 24000.00, 'PARTIALLY_ACCEPTED');

-- Sample Supplier Credit Note
INSERT INTO supplier_credit_notes (credit_note_number, supplier_id, purchase_order_id, grn_id, credit_note_date, type, status, subtotal, tax_amount, total_amount, reason, created_by) VALUES
('SCN2024001', 1, 1, 1, '2024-02-01', 'DEFECTIVE', 'APPROVED', 1000.00, 180.00, 1180.00, 'Defective items returned', 'admin');

-- Sample Credit Note Items
INSERT INTO credit_note_items (credit_note_id, product_id, quantity, unit_price, total_amount, reason) VALUES
(1, 1, 2, 500.00, 1000.00, 'Defective products');

-- Update totals for sample data
UPDATE purchase_orders po 
SET subtotal = (SELECT IFNULL(SUM(poi.total_amount / (1 + poi.tax_percentage/100)), 0) FROM purchase_order_items poi WHERE poi.purchase_order_id = po.id),
    tax_amount = (SELECT IFNULL(SUM(poi.tax_amount), 0) FROM purchase_order_items poi WHERE poi.purchase_order_id = po.id),
    total_amount = (SELECT IFNULL(SUM(poi.total_amount), 0) FROM purchase_order_items poi WHERE poi.purchase_order_id = po.id);

UPDATE goods_received_notes grn 
SET subtotal = (SELECT IFNULL(SUM(gi.total_amount), 0) FROM grn_items gi WHERE gi.grn_id = grn.id),
    total_amount = subtotal + tax_amount;

UPDATE supplier_credit_notes scn 
SET subtotal = (SELECT IFNULL(SUM(cni.total_amount), 0) FROM credit_note_items cni WHERE cni.credit_note_id = scn.id),
    total_amount = subtotal + tax_amount;

-- Display success message
SELECT 'Business Management tables created successfully!' as message;
