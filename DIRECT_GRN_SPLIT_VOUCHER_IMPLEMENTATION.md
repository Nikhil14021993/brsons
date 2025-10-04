# Direct GRN Split Voucher Implementation

## Overview
Implemented split voucher creation for Direct GRN approval where the Net Amount and Tax Amount are posted to separate accounts as requested.

## Changes Made

### 1. SQL Script: `create_grn_split_accounts.sql`
Created a comprehensive SQL script to ensure all required accounts exist:

**Required Accounts:**
- **6001** - Purchase / Cost of Goods Sold (EXPENSE) - For Net Amount debit
- **2001.04** - Duty and Taxes (LIABILITY) - For Tax Amount debit  
- **2001.01** - Accounts Payable (LIABILITY) - For Grand Total credit
- **1001** - Cash (ASSET) - For payment vouchers
- **1002** - Bank Account (ASSET) - For payment vouchers

### 2. Modified GRNService.java
Updated `createVoucherForGRNApproval()` method to create split vouchers:

**New Voucher Structure:**
```
DEBIT ENTRIES:
- Purchase/Cost of Goods Sold (6001) ← Net Amount (Subtotal - Discount)
- Duty and Taxes (2001.04) ← Tax Amount (Grand Total - Net Amount)

CREDIT ENTRY:
- Accounts Payable (2001.01) ← Grand Total (Net Amount + Tax Amount)
```

**Key Features:**
- Uses `createVoucherWithEntries()` for multiple debit entries
- Validates amount calculations (Net + Tax = Grand Total)
- Automatic tax amount adjustment if calculation mismatch
- Comprehensive logging for debugging
- Error handling without failing GRN approval

### 3. Amount Calculation Logic
```java
// From GRN model:
BigDecimal netAmount = grn.getSubtotal();           // Subtotal - Discount
BigDecimal taxAmount = grn.getTaxAmount();          // Tax on Net Amount  
BigDecimal grandTotal = grn.getTotalAmount();       // Net + Tax

// Validation:
if (netAmount.add(taxAmount).compareTo(grandTotal) != 0) {
    // Auto-adjust tax amount to match grand total
    taxAmount = grandTotal.subtract(netAmount);
}
```

## How It Works

### 1. GRN Creation Flow
1. User creates Direct GRN at `/admin/business/grn/new`
2. System calculates:
   - **Subtotal** = Accepted Quantity × Unit Price
   - **Discount Amount** = Subtotal × Discount Percentage  
   - **Net Amount** = Subtotal - Discount Amount
   - **Tax Amount** = Net Amount × Tax Percentage
   - **Grand Total** = Net Amount + Tax Amount

### 2. GRN Approval Flow
1. When GRN status changes to `APPROVED`
2. `createAccountingEntriesForApprovedGRN()` is called
3. `createVoucherForGRNApproval()` creates split voucher:
   - **Net Amount** → Debit Purchase/Cost of Goods Sold (6001)
   - **Tax Amount** → Debit Duty and Taxes (2001.04)
   - **Grand Total** → Credit Accounts Payable (2001.01)

### 3. Voucher Entry Structure
```java
List<VoucherEntryDto> entries = new ArrayList<>();

// Debit Entry 1: Net Amount
entries.add(new VoucherEntryDto(
    purchaseAccountId,    // 6001
    netAmount,           // Debit amount
    null,                // No credit
    "Purchase/Cost of Goods Sold - GRN-123"
));

// Debit Entry 2: Tax Amount  
entries.add(new VoucherEntryDto(
    taxAccountId,        // 2001.04
    taxAmount,          // Debit amount
    null,               // No credit
    "Duty and Taxes - GRN-123"
));

// Credit Entry: Grand Total
entries.add(new VoucherEntryDto(
    payableAccountId,    // 2001.01
    null,               // No debit
    grandTotal,         // Credit amount
    "Accounts Payable - GRN-123"
));
```

## Testing Instructions

### 1. Setup Accounts
```sql
-- Run the SQL script to create required accounts
source create_grn_split_accounts.sql;
```

### 2. Test Direct GRN Creation
1. Go to `/admin/business/grn/new`
2. Create a Direct GRN (no PO) with:
   - Items with unit prices
   - Discount percentages  
   - Tax percentages
3. Verify amounts are calculated correctly:
   - Net Amount = Subtotal - Discount
   - Tax Amount = Net Amount × Tax %
   - Grand Total = Net Amount + Tax Amount

### 3. Test Voucher Creation
1. Approve the Direct GRN
2. Check console logs for split voucher creation
3. Verify voucher entries in database:
   ```sql
   SELECT v.id, v.narration, v.type, 
          ve.account_id, a.name, a.code,
          ve.debit, ve.credit, ve.description
   FROM voucher v
   JOIN voucher_entry ve ON v.id = ve.voucher_id  
   JOIN account a ON ve.account_id = a.id
   WHERE v.type = 'PURCHASE' 
   ORDER BY v.id, ve.id;
   ```

### 4. Expected Results
**Console Output:**
```
=== Creating split voucher for GRN approval ===
GRN: GRN-001
PO: Direct GRN (no PO)
Subtotal: 1000.00
Tax Amount: 180.00  
Total Amount: 1180.00
Added debit entry: Purchase Account (ID: X) - Amount: 1000.00
Added debit entry: Tax Account (ID: Y) - Amount: 180.00
Added credit entry: Payable Account (ID: Z) - Amount: 1180.00
Split voucher created successfully for GRN approval
```

**Database Entries:**
| Account | Code  | Debit  | Credit | Description |
|---------|-------|--------|--------|-------------|
| Purchase/Cost of Goods Sold | 6001 | 1000.00 | 0.00 | Purchase/Cost of Goods Sold - GRN-001 |
| Duty and Taxes | 2001.04 | 180.00 | 0.00 | Duty and Taxes - GRN-001 |
| Accounts Payable | 2001.01 | 0.00 | 1180.00 | Accounts Payable - GRN-001 |

## Benefits

1. **Proper Tax Separation**: Tax amounts are now tracked separately from purchase costs
2. **Accurate Financial Reporting**: Clear distinction between goods cost and tax liability
3. **Compliance**: Better alignment with accounting standards for tax handling
4. **Flexibility**: Supports different tax rates and calculations per GRN
5. **Audit Trail**: Clear voucher entries showing exact amounts for each component

## Troubleshooting

### Issue: "Cannot create voucher - missing required accounts"
**Solution:** Run the `create_grn_split_accounts.sql` script to create all required accounts.

### Issue: "Amount calculation mismatch" warning
**Cause:** Subtotal + Tax Amount ≠ Total Amount in GRN
**Solution:** System auto-adjusts tax amount to match grand total. Check GRN calculation logic if this happens frequently.

### Issue: Voucher not created
**Check:**
1. GRN status is actually `APPROVED`
2. Required accounts exist (6001, 2001.04, 2001.01)
3. GRN has valid subtotal, taxAmount, and totalAmount
4. Check console logs for specific error messages

## Files Modified
- `src/main/java/com/brsons/service/GRNService.java` - Split voucher logic
- `create_grn_split_accounts.sql` - Account setup script
- `DIRECT_GRN_SPLIT_VOUCHER_IMPLEMENTATION.md` - This documentation
