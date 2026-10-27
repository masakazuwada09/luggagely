# Security Specification (`security_spec.md`)

## 1. Data Invariants
1. **Strict Ownership Isolation**: Every document in `/users/{userId}/baggage_items/{itemId}` belongs exclusively to `userId`. Only an authenticated user where `request.auth.uid == userId` and `incoming().userId == userId` can read, create, update, or delete their own baggage items.
2. **Schema & Boundary Integrity**:
   - `userId`: string matching `request.auth.uid` and `userId` path wildcard (`size <= 128`).
   - `name`: non-empty string (`1 <= size <= 120`).
   - `weightGrams`: integer (`50 <= weightGrams <= 50000`).
   - `source`: enum string in `['AI_SCANNED', 'MANUAL_ENTRY']`.
   - `category`: enum string in `['COMPUTE', 'POWER', 'APPAREL', 'LIQUIDS', 'ACCESSORY']`.
   - `isShiftedToPersonal`: boolean.
   - `createdAt` and `updatedAt`: Firestore `timestamp` bounded by `<= request.time`. On update, `createdAt` and `userId` are strictly immutable.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read/Write**: `auth = null` attempting `get`, `list`, `create`, `update`, or `delete` on `/users/alice_123/baggage_items/item_1`.
2. **Cross-User Read/List**: `bob_456` attempting `get` or `list` on `/users/alice_123/baggage_items`.
3. **Identity Spoofing on Create**: `alice_123` creating a doc in `/users/alice_123/baggage_items/item_1` with `userId: "bob_456"`.
4. **Shadow Update / Ghost Field Injection**: `alice_123` sending `{ ..., "isAdmin": true }` on create or update.
5. **Immutable Field Mutation**: `alice_123` attempting to modify `createdAt` or `userId` during an update.
6. **Negative or Excessive Mass Poisoning**: `weightGrams: -500` or `weightGrams: 99999999`.
7. **Oversized Name String (Denial of Wallet)**: `name` with 5,000 characters.
8. **Invalid Enum Value**: `source: "HACKED_SOURCE"` or `category: "UNKNOWN"`.
9. **Future Timestamp Spoofing**: `createdAt` set to a timestamp in the future (`> request.time`).
10. **Type Confusion Attack**: `weightGrams: "2150"` (string instead of int) or `isShiftedToPersonal: "true"`.
11. **Path ID Poisoning**: Document ID containing invalid characters or exceeding 128 chars.
12. **Partial Update Deleting Required Key**: Attempting to delete `name` or `weightGrams` on update.

## 3. Red Team Audit & Conflict Report
- **Identity Spoofing**: Blocked by `request.auth.uid == userId && incoming().userId == userId` on create and `incoming().userId == existing().userId` on update.
- **State Shortcutting**: All updates are gated by `isValidBaggageItem(incoming())` and `affectedKeys().hasOnly(['name', 'weightGrams', 'source', 'category', 'isShiftedToPersonal', 'updatedAt'])`.
- **Resource Poisoning**: `isValidId(userId)` and `isValidId(itemId)` enforce `<= 128` chars and regex `^[a-zA-Z0-9_\-]+$`.
- **Value Poisoning**: `isValidBaggageItem(incoming())` wraps the entire `allow update` block and validates types, enums, and numeric/string bounds.
