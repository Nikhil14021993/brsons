# Retail Order Voucher Split Implementation

## Overview
Implemented split voucher creation for retail orders in the `/cart` functionality where the Subtotal and Tax Amount are posted to separate accounts as requested.

## Changes Made

### Modified AdminOrderService.java
Updated `createVoucherEntryForRetailOrder()` method to create split vouchers for retail orders.

## New Voucher Structure

### Previous Structure (Single Entry):
```
DEBIT ENTRY:
- Cash/Bank Account ← Total Amount

CREDIT ENTRY:
- Sales Account (3001) ← Total Amount
```

### New Structure (Split Entries):
```
DEBIT ENTRY:
- Cash/Bank Account ← Total Amount

CREDIT ENTRIES:
- Sales Account (3001) ← Subtotal Amount
- Duty and Taxes (7001) ← Tax Amount (Total - Subtotal)
```

## Implementation Details

### Amount Calculation:
```java
// From Order model:
BigDecimal voucherAmount = order.getTotal();        // Total amount (including tax)
BigDecimal subtotalAmount = order.getSubTotal();    // Subtotal (before tax)
BigDecimal taxAmount = order.getGstAmount();        // Tax amount
// Fallback calculation if gstAmount is null:
if (taxAmount == null) {
    taxAmount = voucherAmount.subtract(subtotalAmount);
}
```

### Voucher Entry Creation:
```java
// 1. Debit Entry - Cash/Bank Account (Full Amount)
VoucherEntry debitEntry = new VoucherEntry();
debitEntry.setAccount(debitAccount);        // Cash or Bank
debitEntry.setDebit(voucherAmount);         // Total amount
debitEntry.setCredit(BigDecimal.ZERO);

// 2. Credit Entry 1 - Sales Account (Subtotal)
VoucherEntry salesCreditEntry = new VoucherEntry();
salesCreditEntry.setAccount(salesAccount);  // Account 3001
salesCreditEntry.setDebit(BigDecimal.ZERO);
salesCreditEntry.setCredit(subtotalAmount); // Subtotal amount

// 3. Credit Entry 2 - Tax Account (Tax Amount)
VoucherEntry taxCreditEntry = new VoucherEntry();
taxCreditEntry.setAccount(taxAccount);      // Account 7001
taxCreditEntry.setDebit(BigDecimal.ZERO);
taxCreditEntry.setCredit(taxAmount);        // Tax amount
```

## Expected Results

### Console Output:
```
=== Retail Order Voucher Split Debug ===
Order ID: 123
Voucher Amount (Total): 1180.00
Subtotal Amount: 1000.00
Tax Amount: 180.00
Calculated Total (Subtotal + Tax): 1180.00
Successfully created split voucher entry for Retail order ID: 123
- Payment Method: Cash
- Debit: Cash Account (1180.00)
- Credit Sales: Sales (1000.00)
- Credit Tax: Duty and Taxes (180.00)
```

### Database Entries:
| Account | Code  | Debit  | Credit | Description |
|---------|-------|--------|--------|-------------|
| Cash Account | 1001 | 1180.00 | 0.00 | Cash Account - Order #123 - Customer Name - Cash |
| Sales | 3001 | 0.00 | 1000.00 | Sales - Order #123 - Customer Name |
| Duty and Taxes | 7001 | 0.00 | 180.00 | Duty and Taxes - Order #123 - Customer Name |

## Accounting Logic

The new voucher structure follows proper double-entry bookkeeping:

1. **Total Amount (1180.00)** → Debit Cash/Bank Account
   - Records cash received from customer

2. **Subtotal Amount (1000.00)** → Credit Sales Account (3001)
   - Records sales revenue (excluding tax)

3. **Tax Amount (180.00)** → Credit Duty and Taxes Account (7001)
   - Records tax collected from customer

**Balancing:**
- Total Debits: 1180.00
- Total Credits: 1000.00 + 180.00 = 1180.00
- Balance: 0.00 ✅

## Key Features

1. **Automatic Account Creation**: Creates account 7001 if it doesn't exist
2. **Flexible Tax Calculation**: Uses `gstAmount` if available, otherwise calculates as `Total - Subtotal`
3. **Comprehensive Logging**: Detailed debug output for troubleshooting
4. **Error Handling**: Proper exception handling with detailed error messages
5. **Backward Compatibility**: Works with existing retail order flow

## Testing Instructions

### 1. Test Retail Order Creation
1. Go to `/cart` or create a retail order
2. Add items with prices that include tax
3. Complete the order with payment method (Cash/Card)

### 2. Verify Voucher Creation
1. Check console logs for split voucher creation
2. Verify voucher entries in database:
   ```sql
   SELECT v.id, v.narration, v.type,
          ve.account_id, a.name, a.code,
          ve.debit, ve.credit, ve.description
   FROM voucher v
   JOIN voucher_entry ve ON v.id = ve.voucher_id
   JOIN account a ON ve.account_id = a.id
   WHERE v.type = 'SALES'
   ORDER BY v.id, ve.id;
   ```

### 3. Expected Results
- **Debit Entry**: Cash/Bank account with total amount
- **Credit Entry 1**: Sales account (3001) with subtotal amount
- **Credit Entry 2**: Duty and Taxes account (7001) with tax amount
- **Balanced Voucher**: Total debits = Total credits

## Benefits

1. **Proper Tax Separation**: Tax amounts are now tracked separately from sales revenue
2. **Accurate Financial Reporting**: Clear distinction between sales and tax collection
3. **Compliance**: Better alignment with accounting standards for tax handling
4. **Detailed Tracking**: Each component (sales vs tax) is tracked separately
5. **Audit Trail**: Clear voucher entries showing exact amounts for each component

## Files Modified
- `src/main/java/com/brsons/service/AdminOrderService.java` - Split retail voucher logic
- `RETAIL_ORDER_VOUCHER_SPLIT_IMPLEMENTATION.md` - This documentation

## Dependencies
- Account 7001 (Duty and Taxes) must exist (created by `create_grn_split_accounts.sql`)
- Account 3001 (Sales) must exist
- Cash/Bank accounts must exist for payment processing

The retail order voucher creation now properly splits amounts between Sales and Tax accounts as requested! 🎯
