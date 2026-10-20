# WP-R5 — RMRS & PMRD (Treasury) service (`services/rmrs-service`) — Implementation Report

Register: GAP-REGISTER-5, WP-R5 (Catalog #24, SEV Medium) — NEW service, package
`com.hims.rmrs`, port **8104**, schema **ihms_rmrs**, Flyway `V1__init_ihms_rmrs.sql`,
money columns `NUMERIC(19,2)`.

## Implementation summary

- **Schema (V1, `ihms_rmrs`)**: `t_head_master` (head_code UNIQUE, is_active default true),
  `t_authority_master` (authority_code UNIQUE, head_id FK **NULL** allowed per register),
  `t_ledger_master` (ledger_code UNIQUE, head_id FK **NOT NULL**),
  `t_rmrs_collection` (receipt_no UNIQUE, amount CHECK > 0, head/ledger FK NOT NULL,
  authority NULL, `payment_ref VARCHAR(40)` for the E-08 PaymentReceived linkage),
  `t_accounting_entry` (debit/credit NUMERIC(19,2), DB CHECK
  `ck_t_accounting_entry_one_sided`), and `outbox` (IMP-DEC-004 DDL parity incl.
  `uq_outbox_event_id`; **negative-assertion table only**). Explicit per-table
  sequences (`ihms_rmrs.t_*_seq`, IMP-DEC-003) assigned in the service via
  `nextval` before persist — same convention as sibling services.
- **Masters CRUD-minimal** (`MasterController`/`MasterService`): POST/GET heads,
  authorities, ledgers under `/api/v1/masters`. Guards: code+name required
  (400 BEXA003 pattern), code uniqueness → 409 `RMRS_DUPLICATE_CODE`;
  ledger creation requires an existing headId (FK NOT NULL per register).
- **Collections** (`CollectionController`/`CollectionService`): `POST /api/v1/collections`
  validates receiptNo presence, amount > 0, headCode/ledgerCode/authorityCode
  resolution and **head-ledger consistency** (400 otherwise); receipt duplicate →
  409 `RMRS_DUPLICATE_RECEIPT`. Writes the collection **and both balanced postings
  in the SAME transaction**; post-save balance verification reads SUM(debit)/SUM(credit)
  back from the DB inside the same tx and throws if unbalanced.
- **Pagination**: legacy ROWNUM list (POSTGRESQL-MIGRATION :1361-1366) replaced by
  limit-offset pagination on the collection search (`Page<RmrsCollection>` via Spring
  Data `Pageable`; page/size request params, size capped in the controller).
- **Events**: `CollectionPosted` is PROPOSED with no consumers — the service never
  writes to the outbox; integration tests pin its emptiness after successful creates,
  rejected guards AND rejected duplicates (explicit negative assertion).
- **OpenAPI**: `openapi/rmrs-service.yaml` verified 1:1 against the controllers —
  11 operations / 7 paths (masters heads·authorities·ledgers GET+POST;
  collections POST+GET, `{id}` GET, summary/service-wise GET, summary/user-wise GET).

## Balanced-postings invariant coverage

Posting rule: DR the treasury-head ledger, CR the collection ledger; exactly one side
of each entry > 0 with the other = 0; SUM(debit) = SUM(credit) per collection.

| Layer | Enforcement | Test |
|---|---|---|
| Build time | `assertBalanced(...)` over the two constructed entries before save, same tx | unit + integration |
| Post-save | DB read-back `SUM(debit)=SUM(credit)` inside the SAME tx (`sumsByCollectionId`) throws on violation | `fullCollectionFlow_balancedPostings_andNegativeOutboxAssertion` (+ `assertBalancedInDb`) |
| Exactly-one-sided entry | Service sets debit=0/credit=amount and vice-versa; DB CHECK `ck_t_accounting_entry_one_sided` backstop | `dbCheck_enforcesExactlyOneSidedEntries`, `dbCheck_enforcesAmountPositive` |
| Receipt uniqueness | API pre-check 409 + DB `uq_t_rmrs_collection_receipt` backstop | integration duplicate-receipt step |
| amount > 0 | Service guard + DB CHECK `ck_t_rmrs_collection_amount` | unit + `dbCheck_enforcesAmountPositive` |

## Deviations

- **ROWNUM replacement note**: legacy ROWNUM pagination is replaced by standard
  limit-offset pagination (Spring Data `Pageable`, `fetch first ? rows only` +
  count query) instead of keyset pagination; the register allows "keyset/limit-offset"
  — limit-offset chosen to match sibling-service conventions and PageRequest semantics.
- Nullable search filters use `COALESCE(:param, col)` instead of `:param IS NULL OR ...`
  because PostgreSQL cannot infer the data type of an untyped null JDBC parameter
  ("could not determine data type of parameter") under Hibernate 6; semantics identical
  (null param ⇒ predicate always true).
- E-08 PaymentReceived consumption (auto-receipt generation) is comment-only
  (`payment_ref` column ready) — Wave-5b read-model concern per register.

## Repairs made this session (service inherited mid-task)

1. Integration-test fixture: hardcoded generated ids (`createLedger(..., 1L)`,
   native INSERT with `head_id=1, ledger_id=11`). PG sequences do NOT roll back with
   the `@Transactional` test tx, so only whichever creator-test ran first saw id 1 —
   every later test failed with `Unknown headId for ledger: 1`. Tests now capture the
   returned generated ids. No validation logic weakened.
2. Flyway schema test: excluded Flyway's own `flyway_schema_history` bookkeeping table
   from the information_schema listing so the assertion pins exactly the six domain tables.
3. `AccountingEntryRepository.sumsByCollectionId`: a multi-select JPQL query yields
   `Object[]` rows; declared return type corrected from `Object[]` to `List<Object[]>`
   (was causing ClassCastException at runtime).
4. Repository nullable-filter queries rewritten to COALESCE form (see Deviations).

## Test table (final: 33/33 green)

| Test class | Type | Tests | Covers |
|---|---|---|---|
| `api.CollectionControllerTest` | MockMvc (default-deny security) | 7 | POST/GET/{id}/summaries, validation errors, 404/409 mappings |
| `api.MasterControllerTest` | MockMvc (default-deny security) | 6 | masters heads/authorities/ledgers POST+GET |
| `application.CollectionServiceTest` | Unit (Mockito) | 14 | balanced DR/CR build, guard order, duplicates, outbox absence |
| `infrastructure.persistence.RmrsServiceIntegrationTest` | PG16 Testcontainers | 6 | V1 schema + DB checks, full flow, exactly-one-sided CHECK, amount>0 CHECK, summary math, limit-offset pagination, negative outbox assertion |

Command: `mvn -f services/rmrs-service/pom.xml test` → **BUILD SUCCESS, exit 0,
Tests run: 33, Failures: 0, Errors: 0, Skipped: 0**.
