-- Business Management Tables Migration
-- Version: 2.0
-- Description: Creates tables for Purchase Orders, GRN, Supplier Credit Notes, and Suppliers

-- ==================== SUPPLIERS TABLE ====================
CREATE TABLE suppliers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_code VARCHAR(50) UNIQUE NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    contact_person VARCHAR(255),
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    zip_code VARCHAR(20),
    country VARCHAR(100),
    gstin VARCHAR(50),
    pan VARCHAR(50),
    payment_terms VARCHAR(255),
    credit_limit DECIMAL(15,2),
    current_balance DECIMAL(15,2) DEFAULT 0.00,
    status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED', 'BLACKLISTED') DEFAULT 'ACTIVE',
    rating INT CHECK (rating >= 1 AND rating <= 5),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_supplier_code (supplier_code),
    INDEX idx_company_name (company_name),
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_city (city),
    INDEX idx_state (state),
    INDEX idx_country (country),
    INDEX idx_rating (rating),
    INDEX idx_created_at (created_at)
);

-- ==================== PURCHASE ORDERS TABLE ====================
CREATE TABLE purchase_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    po_number VARCHAR(50) UNIQUE NOT NULL,
    supplier_id BIGINT NOT NULL,
    order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expected_delivery_date TIMESTAMP,
    delivery_address TEXT,
    payment_terms VARCHAR(255),
    subtotal DECIMAL(15,2) DEFAULT 0.00,
    tax_amount DECIMAL(15,2) DEFAULT 0.00,
    shipping_cost DECIMAL(15,2) DEFAULT 0.00,
    discount_amount DECIMAL(15,2) DEFAULT 0.00,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status ENUM('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'ORDERED', 'PARTIALLY_RECEIVED', 'FULLY_RECEIVED', 'CANCELLED', 'CLOSED') DEFAULT 'DRAFT',
    notes TEXT,
    created_by VARCHAR(255),
    approved_by VARCHAR(255),
    approved_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE RESTRICT,
    
    INDEX idx_po_number (po_number),
    INDEX idx_supplier_id (supplier_id),
    INDEX idx_status (status),
    INDEX idx_order_date (order_date),
    INDEX idx_expected_delivery_date (expected_delivery_date),
    INDEX idx_created_by (created_by),
    INDEX idx_approved_by (approved_by),
    INDEX idx_created_at (created_at)
);

-- ==================== PURCHASE ORDER ITEMS TABLE ====================
CREATE TABLE purchase_order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    ordered_quantity INT NOT NULL DEFAULT 0,
    received_quantity INT NOT NULL DEFAULT 0,
    unit_price DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    discount_percentage DECIMAL(5,2) DEFAULT 0.00,
    discount_amount DECIMAL(15,2) DEFAULT 0.00,
    tax_percentage DECIMAL(5,2) DEFAULT 0.00,
    tax_amount DECIMAL(15,2) DEFAULT 0.00,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    notes TEXT,
    
    FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    
    INDEX idx_purchase_order_id (purchase_order_id),
    INDEX idx_product_id (product_id),
    INDEX idx_ordered_quantity (ordered_quantity),
    INDEX idx_received_quantity (received_quantity)
);

-- ==================== GOODS RECEIVED NOTES TABLE ====================
CREATE TABLE goods_received_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    grn_number VARCHAR(50) UNIQUE NOT NULL,
    purchase_order_id BIGINT NOT NULL,
    supplier_id BIGINT NOT NULL,
    received_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivery_note_number VARCHAR(100),
    vehicle_number VARCHAR(50),
    received_by VARCHAR(255),
    inspected_by VARCHAR(255),
    status ENUM('DRAFT', 'RECEIVED', 'INSPECTED', 'APPROVED', 'REJECTED', 'CANCELLED') DEFAULT 'DRAFT',
    subtotal DECIMAL(15,2) DEFAULT 0.00,
    tax_amount DECIMAL(15,2) DEFAULT 0.00,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
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
    INDEX idx_received_by (received_by),
    INDEX idx_inspected_by (inspected_by),
    INDEX idx_created_at (created_at)
);

-- ==================== GRN ITEMS TABLE ====================
CREATE TABLE grn_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    grn_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    ordered_quantity INT NOT NULL DEFAULT 0,
    received_quantity INT NOT NULL DEFAULT 0,
    accepted_quantity INT NOT NULL DEFAULT 0,
    rejected_quantity INT NOT NULL DEFAULT 0,
    unit_price DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    quality_status VARCHAR(100),
    rejection_reason TEXT,
    notes TEXT,
    
    FOREIGN KEY (grn_id) REFERENCES goods_received_notes(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    
    INDEX idx_grn_id (grn_id),
    INDEX idx_product_id (product_id),
    INDEX idx_ordered_quantity (ordered_quantity),
    INDEX idx_received_quantity (received_quantity),
    INDEX idx_accepted_quantity (accepted_quantity),
    INDEX idx_rejected_quantity (rejected_quantity),
    INDEX idx_quality_status (quality_status)
);

-- ==================== SUPPLIER CREDIT NOTES TABLE ====================
CREATE TABLE supplier_credit_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    credit_note_number VARCHAR(50) UNIQUE NOT NULL,
    supplier_id BIGINT NOT NULL,
    purchase_order_id BIGINT,
    grn_id BIGINT,
    credit_note_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    type ENUM('RETURN', 'DISCOUNT', 'CORRECTION', 'DAMAGED_GOODS', 'QUALITY_ISSUE', 'PRICE_ADJUSTMENT') NOT NULL,
    status ENUM('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'CANCELLED', 'APPLIED') DEFAULT 'DRAFT',
    subtotal DECIMAL(15,2) DEFAULT 0.00,
    tax_amount DECIMAL(15,2) DEFAULT 0.00,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    reason TEXT,
    reference_document VARCHAR(255),
    notes TEXT,
    created_by VARCHAR(255),
    approved_by VARCHAR(255),
    approved_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE RESTRICT,
    FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id) ON DELETE SET NULL,
    FOREIGN KEY (grn_id) REFERENCES goods_received_notes(id) ON DELETE SET NULL,
    
    INDEX idx_credit_note_number (credit_note_number),
    INDEX idx_supplier_id (supplier_id),
    INDEX idx_purchase_order_id (purchase_order_id),
    INDEX idx_grn_id (grn_id),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_credit_note_date (credit_note_date),
    INDEX idx_created_by (created_by),
    INDEX idx_approved_by (approved_by),
    INDEX idx_created_at (created_at)
);

-- ==================== CREDIT NOTE ITEMS TABLE ====================
CREATE TABLE credit_note_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    credit_note_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    unit_price DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    discount_percentage DECIMAL(5,2) DEFAULT 0.00,
    discount_amount DECIMAL(15,2) DEFAULT 0.00,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    reason TEXT,
    notes TEXT,
    
    FOREIGN KEY (credit_note_id) REFERENCES supplier_credit_notes(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    
    INDEX idx_credit_note_id (credit_note_id),
    INDEX idx_product_id (product_id),
    INDEX idx_quantity (quantity)
);

-- ==================== TRIGGERS FOR AUTOMATIC UPDATES ====================

-- Trigger to update PO totals when items change
DELIMITER //
CREATE TRIGGER update_po_totals_after_item_change
AFTER UPDATE ON purchase_order_items
FOR EACH ROW
BEGIN
    UPDATE purchase_orders 
    SET updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.purchase_order_id;
END//

-- Trigger to update GRN totals when items change
CREATE TRIGGER update_grn_totals_after_item_change
AFTER UPDATE ON grn_items
FOR EACH ROW
BEGIN
    UPDATE goods_received_notes 
    SET updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.grn_id;
END//

-- Trigger to update credit note totals when items change
CREATE TRIGGER update_credit_note_totals_after_item_change
AFTER UPDATE ON credit_note_items
FOR EACH ROW
BEGIN
    UPDATE supplier_credit_notes 
    SET updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.credit_note_id;
END//

DELIMITER ;

-- ==================== SAMPLE DATA ====================

-- Insert sample suppliers
INSERT INTO suppliers (supplier_code, company_name, contact_person, email, phone, address_line1, city, state, country, gstin, payment_terms, credit_limit, rating, notes) VALUES
('SUP001', 'ABC Textiles Ltd.', 'John Smith', 'john@abctextiles.com', '+91-9876543210', '123 Industrial Area', 'Mumbai', 'Maharashtra', 'India', '27AABC1234567890', 'Net 30', 100000.00, 5, 'Premium textile supplier'),
('SUP002', 'XYZ Fabrics Co.', 'Jane Doe', 'jane@xyzfabrics.com', '+91-9876543211', '456 Textile Street', 'Delhi', 'Delhi', 'India', '07BXYZ1234567890', 'Net 45', 75000.00, 4, 'Quality fabric supplier'),
('SUP003', 'Fashion Materials Inc.', 'Mike Johnson', 'mike@fashionmaterials.com', '+91-9876543212', '789 Fashion District', 'Bangalore', 'Karnataka', 'India', '29CFAS1234567890', 'Net 60', 50000.00, 4, 'Trendy materials supplier');

-- Insert sample purchase order
INSERT INTO purchase_orders (po_number, supplier_id, order_date, expected_delivery_date, delivery_address, payment_terms, subtotal, tax_amount, total_amount, status, notes, created_by) VALUES
('PO-2024-001', 1, CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 7 DAY), 'BRSons Warehouse, Mumbai', 'Net 30', 50000.00, 9000.00, 59000.00, 'APPROVED', 'Sample purchase order for testing', 'Admin');

-- Insert sample purchase order items
INSERT INTO purchase_order_items (purchase_order_id, product_id, ordered_quantity, unit_price, total_amount) VALUES
(1, 1, 100, 500.00, 50000.00);

-- Update purchase order totals
UPDATE purchase_orders SET subtotal = 50000.00, total_amount = 59000.00 WHERE id = 1;

-- Insert sample GRN
INSERT INTO goods_received_notes (grn_number, purchase_order_id, supplier_id, received_date, delivery_note_number, received_by, status, subtotal, total_amount, notes) VALUES
('GRN-2024-001', 1, 1, CURRENT_TIMESTAMP, 'DN-001', 'Warehouse Manager', 'RECEIVED', 50000.00, 50000.00, 'Sample GRN for testing');

-- Insert sample GRN items
INSERT INTO grn_items (grn_id, product_id, ordered_quantity, received_quantity, accepted_quantity, unit_price, total_amount, quality_status) VALUES
(1, 1, 100, 100, 95, 500.00, 47500.00, 'Good');

-- Update GRN totals
UPDATE goods_received_notes SET subtotal = 47500.00, total_amount = 47500.00 WHERE id = 1;

-- Insert sample credit note
INSERT INTO supplier_credit_notes (credit_note_number, supplier_id, purchase_order_id, grn_id, credit_note_date, type, status, subtotal, total_amount, reason, created_by, notes) VALUES
('CN-2024-001', 1, 1, 1, CURRENT_TIMESTAMP, 'RETURN', 'APPROVED', 2500.00, 2500.00, 'Damaged goods returned', 'Admin', 'Sample credit note for testing');

-- Insert sample credit note items
INSERT INTO credit_note_items (credit_note_id, product_id, quantity, unit_price, total_amount, reason) VALUES
(1, 1, 5, 500.00, 2500.00, 'Damaged during transit');

-- Update credit note totals
UPDATE supplier_credit_notes SET subtotal = 2500.00, total_amount = 2500.00 WHERE id = 1;

-- ==================== COMMIT TRANSACTION ====================
COMMIT;
