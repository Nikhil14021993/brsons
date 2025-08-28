# Credit Note Quantity Logic - Business Rules

## Overview
This document explains how quantities are handled when creating Credit Notes from GRN (Goods Received Note) items.

## Key Principles

### 1. **Stock Management vs. Credit Claims**
- **Stock Updates**: Based on `receivedQuantity` (what physically arrived)
- **Credit Notes**: Based on `acceptedQuantity` (what you will pay for)

### 2. **Quantity Types in GRN**
- **`orderedQuantity`**: What was ordered in Purchase Order
- **`receivedQuantity`**: What physically arrived from supplier
- **`acceptedQuantity`**: What passed quality inspection
- **`rejectedQuantity`**: What failed quality inspection

## Business Scenarios

### Scenario 1: Perfect Delivery
```
Ordered: 4 items
Received: 4 items  
Accepted: 4 items
Rejected: 0 items
```
- **Credit Note Quantity**: 4 (acceptedQuantity)
- **Stock Impact**: +4 (receivedQuantity)
- **Payment**: Pay for 4 items

### Scenario 2: Partial Receipt
```
Ordered: 4 items
Received: 3 items (1 missing)
Accepted: 3 items
Rejected: 0 items
```
- **Credit Note Quantity**: 3 (acceptedQuantity)
- **Stock Impact**: +3 (receivedQuantity)
- **Payment**: Pay for 3 items, claim credit for missing 1

### Scenario 3: Quality Issues
```
Ordered: 4 items
Received: 4 items
Accepted: 3 items
Rejected: 1 item (quality issue)
```
- **Credit Note Quantity**: 3 (acceptedQuantity)
- **Stock Impact**: +4 (receivedQuantity)
- **Payment**: Pay for 3 items, return rejected 1 to supplier

### Scenario 4: Over-Delivery
```
Ordered: 4 items
Received: 5 items (1 extra)
Accepted: 4 items
Rejected: 1 item (extra item)
```
- **Credit Note Quantity**: 4 (acceptedQuantity)
- **Stock Impact**: +5 (receivedQuantity)
- **Payment**: Pay for 4 items, return extra 1 to supplier

## Implementation Details

### Credit Note Creation
```java
// Use ACCEPTED quantity for Credit Note
creditItem.setQuantity(grnItem.getAcceptedQuantity());
creditItem.setQuantityType("ACCEPTED");
```

### Stock Management
```java
// Note: Stock changes are now handled manually by the user
// No automatic stock operations when Credit Notes are created or status changes
// Users must manually update stock levels based on their business requirements
```

## Why This Approach?

1. **Fair Billing**: You only pay for what you accept
2. **Accurate Stock**: Stock reflects what physically exists
3. **Supplier Relations**: Clear communication about quality issues
4. **Financial Accuracy**: Credit notes match actual payment obligations
5. **Manual Control**: Stock changes are handled manually, giving users full control over inventory management

## Best Practices

1. **Always use `acceptedQuantity` for Credit Notes**
2. **Document rejection reasons clearly**
3. **Return rejected items to supplier promptly**
4. **Reconcile quantities before payment processing**
5. **Manually manage stock levels** based on your business requirements
6. **Track stock movements separately** from Credit Note operations

## Manual Stock Management

Since Credit Notes no longer automatically change stock levels, you need to:

1. **Create Credit Notes** for the quantities you want to claim credit for
2. **Manually adjust stock** using your inventory management system
3. **Document stock changes** with appropriate reasons and references
4. **Reconcile stock levels** with physical inventory counts

This approach gives you:
- **Full control** over when and how stock changes occur
- **Flexibility** to handle complex business scenarios
- **Audit trail** for all stock movements
- **Separation of concerns** between financial documents and inventory management

## Common Mistakes to Avoid

❌ **Wrong**: Using `receivedQuantity` for Credit Notes
✅ **Correct**: Using `acceptedQuantity` for Credit Notes

❌ **Wrong**: Ignoring rejected quantities
✅ **Correct**: Process rejected items separately

❌ **Wrong**: Mixing stock updates with credit calculations
✅ **Correct**: Keep stock and credit logic separate
