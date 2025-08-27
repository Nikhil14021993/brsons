-- Fix Credit Notes Database Schema
-- This script will update the database to match the new CreditNote entity structure

-- Step 1: Drop existing tables (if they exist)
DROP TABLE IF EXISTS credit_note_items CASCADE;
DROP TABLE IF EXISTS supplier_credit_notes CASCADE;

-- Step 2: Create the new credit_notes table
CREATE TABLE credit_notes (
    id BIGSERIAL PRIMARY KEY,
    credit_note_number VARCHAR(255) NOT NULL UNIQUE,
    purchase_order_id BIGINT NOT NULL,
    supplier_id BIGINT,
    credit_date TIMESTAMP NOT NULL,
    credit_amount DECIMAL(10,2) NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(50) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    CONSTRAINT fk_credit_note_purchase_order FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id),
    CONSTRAINT fk_credit_note_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

-- Step 3: Create the new credit_note_items table
CREATE TABLE credit_note_items (
    id BIGSERIAL PRIMARY KEY,
    credit_note_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    discount_percentage DECIMAL(5,2) DEFAULT 0,
    tax_percentage DECIMAL(5,2) DEFAULT 0,
    total_amount DECIMAL(10,2) NOT NULL,
    reason VARCHAR(500),
    CONSTRAINT fk_credit_note_item_credit_note FOREIGN KEY (credit_note_id) REFERENCES credit_notes(id) ON DELETE CASCADE,
    CONSTRAINT fk_credit_note_item_product FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Step 4: Create indexes for better performance
CREATE INDEX idx_credit_notes_purchase_order ON credit_notes(purchase_order_id);
CREATE INDEX idx_credit_notes_supplier ON credit_notes(supplier_id);
CREATE INDEX idx_credit_notes_status ON credit_notes(status);
CREATE INDEX idx_credit_note_items_credit_note ON credit_note_items(credit_note_id);
CREATE INDEX idx_credit_note_items_product ON credit_note_items(product_id);

-- Step 5: Insert sample data (optional - for testing)
-- INSERT INTO credit_notes (credit_note_number, purchase_order_id, supplier_id, credit_date, credit_amount, status, created_at) 
-- VALUES ('CN-001', 1, 1, NOW(), 100.00, 'Draft', NOW());

COMMIT;
