# Price Storage in Order Items - Historical Pricing System

## 🎯 **Overview**

This system stores the actual product prices at the time of order placement in the `order_item` table. This ensures historical pricing accuracy even if product prices change in the future.

## 🏗️ **Database Schema Changes**

### New Columns in `order_item` Table:

| Column | Type | Description |
|--------|------|-------------|
| `unit_price` | DECIMAL(10,2) | Price per unit at order time |
| `total_price` | DECIMAL(10,2) | Total price for this item (unit_price × quantity) |
| `user_type` | VARCHAR(20) | User type at order time (Retail, B2B, Admin) |
| `price_type` | VARCHAR(20) | Price type used (retail, b2b, admin) |

## 💰 **Pricing Logic**

### **User Type → Price Used:**

| User Type | Price Used | Price Type Stored |
|-----------|------------|-------------------|
| **Retail** | `product.retailPrice` | `"retail"` |
| **B2B** | `product.b2bPrice` | `"b2b"` |
| **Admin** | `product.retailPrice` | `"retail"` |

### **Example:**
- **Product Retail Price:** ₹1000
- **Product B2B Price:** ₹800 (80% of retail)
- **B2B User Order:** Unit Price = ₹800, Price Type = "b2b"
- **Retail User Order:** Unit Price = ₹1000, Price Type = "retail"

## 🔄 **How It Works**

### **1. Order Creation (CheckoutController):**
```java
// Get product to determine pricing
Product product = productRepository.findById(cartItem.getProductId()).orElse(null);

// Store the actual price at order time based on user type
BigDecimal unitPrice;
String priceType;

if ("B2B".equalsIgnoreCase(user.getType()) && product.getB2bPrice() != null) {
    unitPrice = BigDecimal.valueOf(product.getB2bPrice());
    priceType = "b2b";
} else {
    unitPrice = BigDecimal.valueOf(product.getRetailPrice());
    priceType = "retail";
}

item.setUnitPrice(unitPrice);
item.setUserType(user.getType());
item.setPriceType(priceType);
item.calculateTotalPrice(); // Calculates total_price
```

### **2. Order Processing (OrderAccountingService):**
```java
// Use stored prices from OrderItems instead of recalculating
for (OrderItem it : items) {
    if (it.getTotalPrice() != null) {
        sub = sub.add(it.getTotalPrice());
    }
}
```

### **3. PDF Generation:**
```java
// Use stored prices instead of recalculating from product table
BigDecimal unitPrice = item.getUnitPrice();
BigDecimal total = item.getTotalPrice();
```

## 📊 **Benefits**

### **✅ Historical Accuracy:**
- **Order placed in January:** Product price was ₹1000
- **Product price changed in February:** Now ₹1200
- **Invoice generated in March:** Still shows ₹1000 (original order price)

### **✅ Performance:**
- No need to fetch products during order processing
- No need to recalculate prices during PDF generation
- Faster order processing and invoice generation

### **✅ Data Integrity:**
- Price information is permanently stored with the order
- No dependency on current product prices
- Accurate financial records for accounting

### **✅ Future-Proof:**
- Product prices can change without affecting historical orders
- Easy to implement price increase/decrease features
- Maintains audit trail of pricing decisions

## 🚀 **Implementation Steps**

### **Step 1: Run Database Migration**
```sql
source add_price_columns_to_order_item.sql;
```

### **Step 2: Restart Application**
The updated code will automatically start storing prices in new orders.

### **Step 3: Verify Implementation**
- Place a test order
- Check `order_item` table for new columns
- Generate invoice to verify stored prices are used

## 🔍 **Verification Queries**

### **Check New Columns:**
```sql
DESCRIBE order_item;
```

### **View Sample Data:**
```sql
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
```

### **Verify Price Calculations:**
```sql
SELECT 
    oi.id,
    oi.unit_price,
    oi.quantity,
    oi.total_price,
    (oi.unit_price * oi.quantity) as calculated_total,
    oi.user_type,
    oi.price_type
FROM order_item oi
WHERE oi.unit_price IS NOT NULL
LIMIT 10;
```

## ⚠️ **Important Notes**

### **Data Migration:**
- Existing orders will have NULL values in new columns
- Run the migration script to populate historical data
- New orders will automatically populate these fields

### **Backward Compatibility:**
- System falls back to product table prices if stored prices are NULL
- No breaking changes to existing functionality
- Gradual migration to new system

### **Performance Impact:**
- Slightly larger `order_item` table
- Better performance for order processing and invoice generation
- Reduced database queries during order operations

## 🎉 **Result**

After implementation:
- ✅ **Historical pricing accuracy** maintained
- ✅ **Performance improved** (fewer database queries)
- ✅ **Data integrity** enhanced
- ✅ **Future price changes** won't affect historical orders
- ✅ **Audit trail** of pricing decisions maintained

This system ensures that your customers always see the exact price they were charged at the time of order placement, regardless of any future price changes in your product catalog.
