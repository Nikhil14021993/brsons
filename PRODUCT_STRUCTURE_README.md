# 🛍️ Brsons E-commerce Product Structure Update

## 📋 Overview

This update enhances the product management system with:
- **Dual Pricing**: Separate prices for Retail and B2B customers
- **Product Variants**: Support for multiple sizes, colors, and stock levels
- **Enhanced Fields**: Description, discount, stock quantity, timestamps
- **Better Organization**: Structured variant management

## 🏗️ New Database Structure

### 📦 Products Table (Updated)

| Field | Type | Description |
|-------|------|-------------|
| `id` | BIGINT | Primary key |
| `product_name` | VARCHAR | Product name |
| `description` | TEXT | Product description |
| `retail_price` | DECIMAL(10,2) | Price for retail customers |
| `b2b_price` | DECIMAL(10,2) | Price for B2B customers |
| `discount` | DECIMAL(5,2) | Discount percentage (0-100) |
| `stock_quantity` | INT | Total stock quantity |
| `created_at` | TIMESTAMP | Creation timestamp |
| `updated_at` | TIMESTAMP | Last update timestamp |
| `status` | VARCHAR | Product status |
| `main_photo` | VARCHAR | Main product image |
| `image1` to `image5` | VARCHAR | Additional images |
| `category_id` | BIGINT | Category reference |

### 🎨 Product Variants Table (New)

| Field | Type | Description |
|-------|------|-------------|
| `id` | BIGINT | Primary key |
| `product_id` | BIGINT | Reference to products table |
| `size` | VARCHAR | Size (S, M, L, XL, etc.) |
| `color` | VARCHAR | Color name |
| `stock_quantity` | INT | Variant-specific stock |
| `retail_price` | DECIMAL(10,2) | Variant-specific retail price |
| `b2b_price` | DECIMAL(10,2) | Variant-specific B2B price |
| `variant_discount` | DECIMAL(5,2) | Variant-specific discount |
| `sku` | VARCHAR | Stock Keeping Unit |
| `created_at` | TIMESTAMP | Creation timestamp |
| `updated_at` | TIMESTAMP | Last update timestamp |
| `status` | VARCHAR | Variant status |

## 🔄 Migration Process

### 1. **Backup Your Database**
```sql
-- Create backup of existing products table
CREATE TABLE products_backup AS SELECT * FROM products;
```

### 2. **Run the Update Script**
```bash
# Execute the database_update_script.sql
mysql -u your_username -p your_database < database_update_script.sql
```

### 3. **Verify Migration**
```sql
-- Check new structure
DESCRIBE products;
DESCRIBE product_variants;

-- Verify data migration
SELECT * FROM products LIMIT 5;
SELECT * FROM product_variants LIMIT 5;
```

## 🚀 New Features

### 💰 **Dual Pricing System**
- **Retail Price**: Standard price for individual customers
- **B2B Price**: Discounted price for business customers
- **Automatic Calculation**: B2B price defaults to 80% of retail price

### 🏷️ **Discount Management**
- **Product Level**: Overall product discount
- **Variant Level**: Specific variant discounts
- **Percentage Based**: 0-100% discount range
- **Automatic Calculation**: Discounted prices calculated automatically

### 📦 **Stock Management**
- **Product Level**: Total stock across all variants
- **Variant Level**: Individual variant stock levels
- **Stock Status**: In Stock, Low Stock, Out of Stock
- **Real-time Updates**: Stock updates automatically

### 🎨 **Variant System**
- **Multiple Sizes**: S, M, L, XL, XXL, etc.
- **Multiple Colors**: Red, Blue, Green, etc.
- **Individual Pricing**: Variant-specific prices
- **Individual Stock**: Variant-specific stock levels
- **SKU Generation**: Automatic SKU creation

## 📝 Usage Examples

### **Adding a Product with Variants**

```java
// Create product
Product product = new Product();
product.setProductName("Classic T-Shirt");
product.setDescription("Premium cotton t-shirt");
product.setRetailPrice(29.99);
product.setB2bPrice(23.99);
product.setDiscount(10.0);
product.setStockQuantity(100);

// Create variants
ProductVariant variant1 = new ProductVariant();
variant1.setSize("S");
variant1.setColor("Red");
variant1.setStockQuantity(25);
variant1.setSku("CTS-S-RD-0001");

ProductVariant variant2 = new ProductVariant();
variant2.setSize("M");
variant2.setColor("Red");
variant2.setStockQuantity(30);
variant2.setSku("CTS-M-RD-0002");
```

### **Price Calculations**

```java
// Get discounted prices
Double discountedRetail = product.getDiscountedRetailPrice();
Double discountedB2B = product.getDiscountedB2bPrice();

// Get variant-specific prices
Double variantRetail = variant.getEffectiveRetailPrice();
Double variantB2B = variant.getEffectiveB2bPrice();
```

### **Stock Management**

```java
// Check stock status
String stockStatus = variant.getStockStatus(); // "In Stock", "Low Stock", "Out of Stock"
boolean inStock = variant.isInStock();

// Get total stock
Integer totalStock = product.getStockQuantity();
Integer variantStock = variant.getStockQuantity();
```

## 🔧 Admin Panel Updates

### **Add Product Form**
- **Basic Info**: Name, description, category
- **Pricing**: Retail price, B2B price, discount
- **Stock**: Total quantity
- **Images**: Main photo + 5 additional images
- **Variants**: Size, color, stock, pricing arrays

### **Product Management**
- **List View**: Shows products with variant counts
- **Edit View**: Update product and variant information
- **Stock View**: Monitor stock levels across variants
- **Price View**: Manage pricing for different customer types

## 📊 Database Views

### **product_with_variants**
Combines product and variant information for easy querying.

### **product_stock_summary**
Provides stock overview with status indicators.

## 🔒 Constraints & Validation

### **Data Integrity**
- **Price Validation**: All prices must be >= 0
- **Discount Validation**: Discounts must be 0-100%
- **Stock Validation**: Stock quantities must be >= 0
- **Foreign Keys**: Proper referential integrity

### **Business Rules**
- **B2B Discount**: B2B price typically lower than retail
- **Stock Tracking**: Variant stock <= product total stock
- **Status Management**: Active/Inactive status control

## 🚨 Important Notes

### **Before Migration**
1. **Backup your database**
2. **Test in development environment**
3. **Review existing product data**
4. **Plan for downtime if needed**

### **After Migration**
1. **Verify data integrity**
2. **Update frontend forms**
3. **Test new functionality**
4. **Train admin users**

### **Breaking Changes**
- Old `price` field replaced with `retail_price` and `b2b_price`
- Old `size` and `colour` fields moved to variants
- New required fields: `description`, `stock_quantity`

## 🔮 Future Enhancements

### **Planned Features**
- **Bulk Variant Import**: CSV/Excel import for variants
- **Advanced Pricing**: Dynamic pricing based on quantity
- **Inventory Alerts**: Low stock notifications
- **Variant Images**: Individual images for variants
- **Price History**: Track price changes over time

### **API Endpoints**
- **Variant Management**: CRUD operations for variants
- **Stock Updates**: Real-time stock modifications
- **Price Calculations**: Dynamic pricing calculations
- **Inventory Reports**: Stock level reports

## 📞 Support

If you encounter any issues during migration:
1. Check the database logs
2. Verify constraint violations
3. Review data types and formats
4. Contact the development team

---

**Happy Selling! 🎉**

*This update provides a robust foundation for managing complex product catalogs with multiple variants and pricing strategies.*
