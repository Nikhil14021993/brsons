-- Make GRN Purchase Order Optional Migration
-- Version: 5.0
-- Description: Makes purchase_order_id nullable in goods_received_notes table to allow direct GRN creation

-- ==================== MODIFY GRN TABLE ====================
-- Make purchase_order_id nullable to allow direct GRN creation without PO
ALTER TABLE goods_received_notes 
MODIFY COLUMN purchase_order_id BIGINT NULL;

-- Update the foreign key constraint to allow NULL values
-- First drop the existing foreign key constraint
ALTER TABLE goods_received_notes 
DROP FOREIGN KEY goods_received_notes_ibfk_1;

-- Add the foreign key constraint back with ON DELETE SET NULL to handle PO deletion
ALTER TABLE goods_received_notes 
ADD CONSTRAINT fk_grn_purchase_order 
FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id) ON DELETE SET NULL;

-- Add a comment to document the change
ALTER TABLE goods_received_notes 
MODIFY COLUMN purchase_order_id BIGINT NULL COMMENT 'Optional reference to purchase order - NULL for direct GRN creation';
