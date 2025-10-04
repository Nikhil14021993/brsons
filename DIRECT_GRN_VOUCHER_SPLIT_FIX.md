# Direct GRN Voucher Split Fix

## Problem Identified
The voucher was showing the grand total in the Purchase/Cost of Goods Sold account (6001) instead of splitting correctly between Net Amount and Tax Amount.

## Root Cause
The issue was in the `GoodsReceivedNote.calculateTotals()` method. It was incorrectly setting the `subtotal` field to the sum of `item.getTotalAmount()`, but `item.getTotalAmount()` already includes tax amounts.

**Previous Logic (Incorrect):**
```java
public void calculateTotals() {
    this.subtotal = BigDecimal.ZERO;
    if (this.grnItems != null) {
        for (GRNItem item : this.grnItems) {
            this.subtotal = this.subtotal.add(item.getTotalAmount()); // ❌ This includes tax!
        }
    }
    this.totalAmount = this.subtotal.add(this.taxAmount != null ? this.taxAmount : BigDecimal.ZERO);
}
```

**Problem:** `item.getTotalAmount()` = Net Amount + Tax Amount, so `subtotal` was actually the grand total, not the net amount.

## Solution Implemented

### 1. Fixed GRN Model (`GoodsReceivedNote.java`)
Updated `calculateTotals()` method to properly calculate Net Amount and Tax Amount separately:

```java
public void calculateTotals() {
    BigDecimal totalNetAmount = BigDecimal.ZERO;
    BigDecimal totalTaxAmount = BigDecimal.ZERO;
    
    if (this.grnItems != null) {
        for (GRNItem item : this.grnItems) {
            // Calculate item net amount (subtotal - discount)
            BigDecimal itemBaseAmount = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getAcceptedQuantity()));
            BigDecimal itemDiscountAmount = BigDecimal.ZERO;
            if (item.getDiscountPercentage() != null && item.getDiscountPercentage().compareTo(BigDecimal.ZERO) > 0) {
                itemDiscountAmount = itemBaseAmount.multiply(item.getDiscountPercentage())
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            }
            BigDecimal itemNetAmount = itemBaseAmount.subtract(itemDiscountAmount);
            
            // Calculate item tax amount
            BigDecimal itemTaxAmount = BigDecimal.ZERO;
            if (item.getCgstAmount() != null) itemTaxAmount = itemTaxAmount.add(item.getCgstAmount());
            if (item.getSgstAmount() != null) itemTaxAmount = itemTaxAmount.add(item.getSgstAmount());
            if (item.getIgstAmount() != null) itemTaxAmount = itemTaxAmount.add(item.getIgstAmount());
            
            totalNetAmount = totalNetAmount.add(itemNetAmount);
            totalTaxAmount = totalTaxAmount.add(itemTaxAmount);
        }
    }
    
    // Set the calculated amounts correctly
    this.subtotal = totalNetAmount; // Net amount (after discount, before tax)
    this.taxAmount = totalTaxAmount; // Total tax amount
    this.totalAmount = totalNetAmount.add(totalTaxAmount); // Grand total
}
```

### 2. Enhanced Debugging (`GRNService.java`)
Added detailed logging to verify amounts are calculated correctly:

```java
System.out.println("=== Amount Calculation Debug ===");
System.out.println("GRN Subtotal (Net Amount): " + netAmount);
System.out.println("GRN Tax Amount: " + taxAmount);
System.out.println("GRN Total Amount (Grand Total): " + grandTotal);
System.out.println("Calculated Total (Net + Tax): " + netAmount.add(taxAmount));
```

## How It Works Now

### GRN Amount Calculation:
1. **Item Base Amount** = Accepted Quantity × Unit Price
2. **Item Discount Amount** = Base Amount × Discount Percentage
3. **Item Net Amount** = Base Amount - Discount Amount
4. **Item Tax Amount** = CGST + SGST + IGST (calculated on Net Amount)
5. **GRN Subtotal** = Sum of all Item Net Amounts
6. **GRN Tax Amount** = Sum of all Item Tax Amounts
7. **GRN Total Amount** = Subtotal + Tax Amount

### Voucher Creation:
- **Net Amount** (Subtotal) → Debit Purchase/Cost of Goods Sold (6001)
- **Tax Amount** → Debit Duty and Taxes (2001.04)
- **Grand Total** → Credit Accounts Payable (2001.01)

## Expected Results

### Console Output:
```
=== Creating split voucher for GRN approval ===
GRN: GRN-001
PO: Direct GRN (no PO)
Subtotal: 1000.00
Tax Amount: 180.00
Total Amount: 1180.00
=== Amount Calculation Debug ===
GRN Subtotal (Net Amount): 1000.00
GRN Tax Amount: 180.00
GRN Total Amount (Grand Total): 1180.00
Calculated Total (Net + Tax): 1180.00
Added debit entry: Purchase Account (ID: X) - Amount: 1000.00
Added debit entry: Tax Account (ID: Y) - Amount: 180.00
Added credit entry: Payable Account (ID: Z) - Amount: 1180.00
Split voucher created successfully for GRN approval
```

### Database Entries:
| Account | Code  | Debit  | Credit | Description |
|---------|-------|--------|--------|-------------|
| Purchase/Cost of Goods Sold | 6001 | 1000.00 | 0.00 | Purchase/Cost of Goods Sold - GRN-001 |
| Duty and Taxes | 2001.04 | 180.00 | 0.00 | Duty and Taxes - GRN-001 |
| Accounts Payable | 2001.01 | 0.00 | 1180.00 | Accounts Payable - GRN-001 |

## Testing Instructions

1. **Create a Direct GRN** with items that have:
   - Unit prices
   - Discount percentages
   - Tax percentages (CGST/SGST or IGST)

2. **Verify Amount Calculation**:
   - Check that Net Amount = Subtotal - Discount
   - Check that Tax Amount is calculated on Net Amount
   - Check that Grand Total = Net Amount + Tax Amount

3. **Approve the GRN** and check:
   - Console logs show correct amount splitting
   - Voucher entries are created with proper amounts
   - Net Amount goes to Purchase account (6001)
   - Tax Amount goes to Duty and Taxes account (2001.04)

## Files Modified
- `src/main/java/com/brsons/model/GoodsReceivedNote.java` - Fixed calculateTotals() method
- `src/main/java/com/brsons/service/GRNService.java` - Enhanced debugging
- `DIRECT_GRN_VOUCHER_SPLIT_FIX.md` - This documentation

## Key Benefits
1. **Correct Amount Separation**: Net Amount and Tax Amount are now properly calculated and separated
2. **Accurate Voucher Entries**: Each amount goes to the correct account
3. **Better Debugging**: Enhanced logging helps identify any future issues
4. **Proper Accounting**: Follows standard accounting practices for tax separation
