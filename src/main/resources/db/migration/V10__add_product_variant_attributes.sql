-- Adds descriptive attribute columns for product variants
ALTER TABLE product_variants
    ADD COLUMN fabric VARCHAR(100),
    ADD COLUMN style VARCHAR(100),
    ADD COLUMN pattern VARCHAR(100),
    ADD COLUMN care_instruction VARCHAR(500),
    ADD COLUMN occasion VARCHAR(100);

