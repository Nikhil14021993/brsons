# Accounting Workflow Update

## Overview
Updated the accounting workflow to follow proper accrual accounting principles by creating accounting entries only when goods are officially received and approved.

## Previous Workflow (Incorrect)
```
PO Created → ✅ CREATE SUPPLIER LEDGER ENTRY + OUTSTANDING PAYABLE
     ↓
PO Approved → No Change
     ↓
GRN Created → No Change
     ↓
GRN Approved → No Change
```

## New Workflow (Correct)
```
PO Created (DRAFT) → No Accounting Entry
     ↓
PO Approved → No Accounting Entry  
     ↓
GRN Created (RECEIVED) → No Accounting Entry
     ↓
GRN Inspected → No Accounting Entry
     ↓
GRN Approved → ✅ CREATE SUPPLIER LEDGER ENTRY + OUTSTANDING PAYABLE
     ↓
Payment Made → ✅ UPDATE BOTH SYSTEMS
```

## Changes Made

### 1. OutstandingService.java
- **Removed** accounting entries from `createPurchaseOrderOutstanding()` method
- **Added** comment explaining the new workflow
- **Kept** the method for creating outstanding items (used by GRN approval)
- **Updated** `createOutstandingForExistingItems()` to only create outstanding for POs with approved GRNs
- **Updated** `createB2BOutstandingForExistingItems()` to only create outstanding for POs with approved GRNs
- **Added** `hasApprovedGRN()` helper method to check if PO has approved GRNs
- **Added** GRNRepository dependency

### 2. GRNService.java
- **Added** imports for `SupplierLedgerService`, `OutstandingService`, and `AccountingService`
- **Added** `createAccountingEntriesForApprovedGRN()` method
- **Added** `createVoucherForGRNApproval()` method for double-entry bookkeeping
- **Added** logic to trigger accounting entries when GRN status changes to `APPROVED`

## Benefits

1. **Accurate Financial Reporting**: Only shows actual liabilities (goods received)
2. **Prevents Overstatement**: Won't show debts for POs that might be cancelled
3. **Matches Physical Reality**: Accounting matches when goods actually arrive
4. **Better Cash Flow Management**: Outstanding payables reflect real obligations
5. **Compliance**: Follows standard accounting practices

## How It Works Now

1. **PO Creation**: No accounting entries are created
2. **PO Approval**: No accounting entries are created
3. **GRN Creation**: No accounting entries are created
4. **GRN Inspection**: No accounting entries are created
5. **GRN Approval**: 
   - Creates supplier ledger entry
   - Creates outstanding payable
   - Creates voucher entry with double-entry bookkeeping:
     - **Debit**: EXPENSES - Purchase / Cost of Goods Sold (Account ID: 35)
     - **Credit**: Current Liabilities - Accounts Payable / Creditors (Account ID: 22)
   - All systems are now in sync
6. **Payment Processing**: Works as before, deducting from outstanding payables
7. **Existing Data Sync**: When running "Create Outstanding for Existing Items", only POs with approved GRNs will get outstanding payables

## Testing

To test the new workflow:

1. Create a PO (should not create accounting entries)
2. Approve the PO (should not create accounting entries)
3. Create a GRN (should not create accounting entries)
4. Inspect the GRN (should not create accounting entries)
5. Approve the GRN (should create accounting entries)
6. Check supplier ledger and outstanding payables - both should show the PO
7. Check voucher entries - should see a voucher with:
   - Debit: EXPENSES - Purchase / Cost of Goods Sold (Account ID: 35)
   - Credit: Current Liabilities - Accounts Payable / Creditors (Account ID: 22)

## Rollback

If you need to rollback to the old behavior, simply:
1. Remove the accounting logic from GRN approval
2. Add back the accounting logic to PO creation
3. The system will work as before

## Notes

- The change is backward compatible
- Existing POs and GRNs will continue to work
- Only new GRN approvals will follow the new workflow
- All payment processing remains unchanged
