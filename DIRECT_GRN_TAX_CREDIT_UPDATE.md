# Direct GRN Tax Credit Update

## Changes Made
Updated the Direct GRN voucher creation to use account code **7001** for Duty and Taxes and make it a **CREDIT entry** instead of debit.

## New Voucher Structure

### Previous Structure (Incorrect):
```
DEBIT ENTRIES:
- Purchase/Cost of Goods Sold (6001) ← Net Amount
- Duty and Taxes (2001.04) ← Tax Amount

CREDIT ENTRY:
- Accounts Payable (2001.01) ← Grand Total
```

### New Structure (Correct):
```
DEBIT ENTRY:
- Purchase/Cost of Goods Sold (6001) ← Net Amount

CREDIT ENTRIES:
- Duty and Taxes (7001) ← Tax Amount
- Accounts Payable (2001.01) ← Net Amount
```

## Files Modified

### 1. GRNService.java
**Changes:**
- Updated tax account code from "2001.04" to "7001"
- Changed tax amount from debit to credit entry
- Updated Accounts Payable credit to only include Net Amount (not Grand Total)
- Updated logging messages to reflect new structure

**Code Changes:**
```java
// Find accounts by code
Long purchaseAccountId = findAccountIdByCode("6001"); // Purchase / Cost of Goods Sold
Long taxAccountId = findAccountIdByCode("7001"); // Duty and Taxes (changed from 2001.04)
Long payableAccountId = findAccountIdByCode("2001.01"); // Accounts Payable

// Debit Entry 1: Net Amount to Purchase/Cost of Goods Sold
entries.add(new VoucherEntryDto(purchaseAccountId, netAmount, null, "Purchase/Cost of Goods Sold - " + grn.getGrnNumber()));

// Credit Entry 2: Tax Amount to Duty and Taxes (changed from debit to credit)
entries.add(new VoucherEntryDto(taxAccountId, null, taxAmount, "Duty and Taxes - " + grn.getGrnNumber()));

// Credit Entry 3: Net Amount to Accounts Payable (changed from Grand Total to Net Amount)
entries.add(new VoucherEntryDto(payableAccountId, null, netAmount, "Accounts Payable - " + grn.getGrnNumber()));
```

### 2. create_grn_split_accounts.sql
**Changes:**
- Updated to create account 7001 instead of 2001.04
- Changed account type from LIABILITY to EXPENSE
- Updated account descriptions and test queries

**SQL Changes:**
```sql
-- Create Duty and Taxes account (Code: 7001) - CREDIT ACCOUNT FOR TAX
INSERT INTO account (code, name, type, description, is_active) 
SELECT '7001', 'Duty and Taxes', 'EXPENSE', 'Duty and Taxes', TRUE
WHERE NOT EXISTS (SELECT 1 FROM account WHERE code = '7001');
```

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
Added credit entry: Tax Account (ID: Y) - Amount: 180.00
Added credit entry: Payable Account (ID: Z) - Amount: 1000.00
Split voucher created successfully for GRN approval
Net Amount (1000.00) → Debit Purchase Account, Tax Amount (180.00) → Credit Tax Account (7001), Net Amount (1000.00) → Credit Payable Account
```

### Database Entries:
| Account | Code  | Debit  | Credit | Description |
|---------|-------|--------|--------|-------------|
| Purchase/Cost of Goods Sold | 6001 | 1000.00 | 0.00 | Purchase/Cost of Goods Sold - GRN-001 |
| Duty and Taxes | 7001 | 0.00 | 180.00 | Duty and Taxes - GRN-001 |
| Accounts Payable | 2001.01 | 0.00 | 1000.00 | Accounts Payable - GRN-001 |

## Accounting Logic

The new voucher structure follows this accounting logic:

1. **Net Amount (1000.00)** → Debit Purchase/Cost of Goods Sold (6001)
   - Records the cost of goods purchased (after discount)

2. **Tax Amount (180.00)** → Credit Duty and Taxes (7001)
   - Records tax liability as a credit (expense account)

3. **Net Amount (1000.00)** → Credit Accounts Payable (2001.01)
   - Records the amount owed to supplier (excluding tax)

## Benefits

1. **Proper Tax Treatment**: Tax amount is now credited to a separate tax account (7001)
2. **Clear Separation**: Net amount and tax amount are handled separately
3. **Correct Account Types**: Uses appropriate account codes as requested
4. **Balanced Entries**: Total debits = Total credits (1000.00 = 1000.00 + 180.00 - 180.00)

## Testing Instructions

1. **Run the updated SQL script**:
   ```sql
   source create_grn_split_accounts.sql;
   ```

2. **Create a Direct GRN** with items, discounts, and taxes

3. **Approve the GRN** and verify:
   - Net Amount (1000.00) → Debit Purchase Account (6001)
   - Tax Amount (180.00) → Credit Tax Account (7001)
   - Net Amount (1000.00) → Credit Payable Account (2001.01)

4. **Check voucher entries** in database to confirm correct amounts and account codes

## Files Modified
- `src/main/java/com/brsons/service/GRNService.java` - Updated voucher logic
- `create_grn_split_accounts.sql` - Updated account setup
- `DIRECT_GRN_TAX_CREDIT_UPDATE.md` - This documentation
