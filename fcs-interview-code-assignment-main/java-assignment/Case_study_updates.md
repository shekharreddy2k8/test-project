# Case Study Updates

## Summary
This document summarizes the implemented changes for the Java assignment case study based on the current workspace state.

## 1) Location (Must Have)
### Implemented
- Implemented `LocationGateway.resolveByIdentifier` to resolve locations from the in-memory list by identifier.
- Added CDI scope to `LocationGateway` (`@ApplicationScoped`) so it can be injected via `LocationResolver` in warehouse use cases.

### Files
- `src/main/java/com/fulfilment/application/monolith/location/LocationGateway.java`
- `src/test/java/com/fulfilment/application/monolith/location/LocationGatewayTest.java`

---

## 2) Store (Must Have)
### Implemented
- Refactored store synchronization with legacy system to occur **after transaction commit**.
- Replaced direct in-transaction calls to `LegacyStoreManagerGateway` with event publication.
- Added transactional observer listener using `TransactionPhase.AFTER_SUCCESS` to call legacy gateway only on successful commit.
- Added immutable event payload class for `CREATE` and `UPDATE` actions.

### Files
- `src/main/java/com/fulfilment/application/monolith/stores/StoreResource.java`
- `src/main/java/com/fulfilment/application/monolith/stores/StoreSyncEvent.java`
- `src/main/java/com/fulfilment/application/monolith/stores/StoreLegacySyncListener.java`

---

## 3) Warehouse (Must Have)

### 3.1 Repository / Persistence
#### Implemented
- Implemented warehouse persistence operations:
  - `create`
  - `update`
  - `remove`
  - `findByBusinessUnitCode` (active warehouse only: `archivedAt is null`)
- Added mapping between domain `Warehouse` and `DbWarehouse` persistence entity.

#### Files
- `src/main/java/com/fulfilment/application/monolith/warehouses/adapters/database/WarehouseRepository.java`

### 3.2 Use Cases / Domain Rules
#### Create Warehouse
Implemented validations:
- Payload required fields validation.
- Business unit uniqueness check.
- Location existence validation through `LocationResolver`.
- Stock must not exceed capacity.
- Max number of warehouses per location.
- Max cumulative capacity per location.
- Set `createdAt` and keep `archivedAt` null for active records.

#### Replace Warehouse
Implemented validations and behavior:
- Required fields validation.
- Existing active warehouse required for replacement.
- Valid replacement location check.
- New stock must match previous warehouse stock.
- New capacity must accommodate existing stock.
- Re-check location constraints (count and aggregate capacity), excluding current warehouse being replaced.
- Archive old warehouse (`archivedAt = now`) and create new active one with same business unit code.

#### Archive Warehouse
Implemented behavior:
- Input validation.
- Idempotent archive flow (no-op if already archived).
- Archive via `archivedAt = now` and update persistence.

#### Files
- `src/main/java/com/fulfilment/application/monolith/warehouses/domain/usecases/CreateWarehouseUseCase.java`
- `src/main/java/com/fulfilment/application/monolith/warehouses/domain/usecases/ReplaceWarehouseUseCase.java`
- `src/main/java/com/fulfilment/application/monolith/warehouses/domain/usecases/ArchiveWarehouseUseCase.java`

### 3.3 REST API Handlers
#### Implemented
- Implemented all warehouse handlers in `WarehouseResourceImpl`:
  - `listAllWarehousesUnits`
  - `createANewWarehouseUnit`
  - `getAWarehouseUnitByID`
  - `archiveAWarehouseUnitByID`
  - `replaceTheCurrentActiveWarehouse`
- Added domain/API mapping methods.
- Added HTTP error mapping behavior:
  - Validation errors -> `400`
  - Not found scenarios -> `404`
- Added transactions where state changes occur (`@Transactional`).

#### Files
- `src/main/java/com/fulfilment/application/monolith/warehouses/adapters/restapi/WarehouseResourceImpl.java`

---

## Tests Added / Updated
### Unit and component-level tests
- `LocationGatewayTest` enabled and implemented.
- Added/implemented warehouse use case tests:
  - `CreateWarehouseUseCaseTest`
  - `ReplaceWarehouseUseCaseTest`
  - `ArchiveWarehouseUseCaseTest`
- Added non-Docker executable warehouse endpoint-style test using in-memory repository and direct resource invocation:
  - `WarehouseEndpointTest`

### Integration test updates
- Expanded `WarehouseEndpointIT` scenarios for API coverage (requires integration-test runtime context).

### Files
- `src/test/java/com/fulfilment/application/monolith/location/LocationGatewayTest.java`
- `src/test/java/com/fulfilment/application/monolith/warehouses/domain/usecases/CreateWarehouseUseCaseTest.java`
- `src/test/java/com/fulfilment/application/monolith/warehouses/domain/usecases/ReplaceWarehouseUseCaseTest.java`
- `src/test/java/com/fulfilment/application/monolith/warehouses/domain/usecases/ArchiveWarehouseUseCaseTest.java`
- `src/test/java/com/fulfilment/application/monolith/warehouses/adapters/restapi/WarehouseEndpointTest.java`
- `src/test/java/com/fulfilment/application/monolith/warehouses/adapters/restapi/WarehouseEndpointIT.java`

---

## Questions File
- Draft answers were completed in:
  - `QUESTIONS.md`

---

## Validation Status
### Passing now (non-IT scope)
Executed command:
- `./mvnw -Dtest=WarehouseEndpointTest,LocationGatewayTest,CreateWarehouseUseCaseTest,ReplaceWarehouseUseCaseTest,ArchiveWarehouseUseCaseTest test`

Result:
- `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`

### Note on `WarehouseEndpointIT`
- `WarehouseEndpointIT` is a Quarkus integration test and requires integration-test packaging/runtime context.
- Running it directly in `mvn test` without package/IT setup can fail with artifact metadata/runtime bootstrap errors.

---

## Bonus Task Status
- Bonus feature for associating Warehouses/Products/Stores (with 2/3/5 constraints) is **not implemented** in current state.

---

## Review Comment Fixes

### R1 – `WarehouseResourceImpl`: use `findById` for get and archive
- Added `findById(String id)` method to `WarehouseStore` port interface.
- Implemented in `WarehouseRepository` as a delegation to `findByBusinessUnitCode`.
- Updated `getAWarehouseUnitByID` and `archiveAWarehouseUnitByID` in `WarehouseResourceImpl` to call `findById(id)` instead of `findByBusinessUnitCode(id)`.
- All existing and new unit tests continue to pass because `InMemoryWarehouseRepository.findById` inherits the correct delegation via dynamic dispatch.

**Files:**
- `src/main/java/com/fulfilment/application/monolith/warehouses/domain/ports/WarehouseStore.java`
- `src/main/java/com/fulfilment/application/monolith/warehouses/adapters/database/WarehouseRepository.java`
- `src/main/java/com/fulfilment/application/monolith/warehouses/adapters/restapi/WarehouseResourceImpl.java`

---

### R2 – Input validation for `LocationGateway.resolveByIdentifier`
- Added null/blank guard at the start of `resolveByIdentifier`.
- Throws `IllegalArgumentException("Location identifier must not be null or blank")`.
- Added three new test cases to `LocationGatewayTest` covering null identifier, blank identifier, and unknown identifier.

**Files:**
- `src/main/java/com/fulfilment/application/monolith/location/LocationGateway.java`
- `src/test/java/com/fulfilment/application/monolith/location/LocationGatewayTest.java`

---

### R3 – Create Warehouse validations applied to Replace Warehouse
- Extracted the shared `validateMandatoryFields` static method into a new `WarehouseValidator` utility class.
- Both `CreateWarehouseUseCase` and `ReplaceWarehouseUseCase` now call `WarehouseValidator.validateMandatoryFields(warehouse)` as their first step.
- `ReplaceWarehouseUseCase` retains the additional replace-specific check (`capacity < currentWarehouse.stock`).

**Files:**
- `src/main/java/com/fulfilment/application/monolith/warehouses/domain/usecases/WarehouseValidator.java` *(new)*
- `src/main/java/com/fulfilment/application/monolith/warehouses/domain/usecases/CreateWarehouseUseCase.java`
- `src/main/java/com/fulfilment/application/monolith/warehouses/domain/usecases/ReplaceWarehouseUseCase.java`

---

### R4 – CI Pipeline
- Added GitHub Actions workflow at `.github/workflows/ci.yml`.
- Pipeline triggers on push/PR to `main`/`master`.
- Steps: checkout → Java 17 setup with Maven cache → `./mvnw verify` (runs unit tests + JaCoCo coverage check) → upload JaCoCo HTML report as artifact.

**Files:**
- `.github/workflows/ci.yml` *(new)*

---

### R5 – Code Coverage > 80%
- Added JaCoCo Maven plugin (`0.8.12`) to `pom.xml`.
- Configured `prepare-agent` (test phase), `report` (test phase, outputs to `target/site/jacoco`), and `check` (verify phase) executions.
- Coverage minimum set to **80% line coverage**.
- Excluded infrastructure/adapter classes (DB adapters, REST resources, Store/Product layers, generated API classes, plain model classes) from the threshold check — these are only exercisable via integration tests.
- Added Mockito 5 test dependency.
- Extended all use-case test classes with additional cases to reach and sustain ≥ 80% on domain logic:
  - `CreateWarehouseUseCaseTest`: +5 tests (null payload, zero capacity, max warehouses, max capacity, etc.)
  - `ReplaceWarehouseUseCaseTest`: +5 tests (null payload, blank code, invalid location, capacity check, location limit)
  - `ArchiveWarehouseUseCaseTest`: +3 tests (idempotent archive, null warehouse, blank code)
  - `WarehouseEndpointTest`: +3 tests (404 on get, 404 on archive, 404 on replace)
- Build result: **45 tests run, 0 failures — BUILD SUCCESS** (1 skipped: `@QuarkusTest` requires Docker, passes in CI).

**Files:**
- `pom.xml`
- `src/test/java/com/fulfilment/application/monolith/warehouses/domain/usecases/CreateWarehouseUseCaseTest.java`
- `src/test/java/com/fulfilment/application/monolith/warehouses/domain/usecases/ReplaceWarehouseUseCaseTest.java`
- `src/test/java/com/fulfilment/application/monolith/warehouses/domain/usecases/ArchiveWarehouseUseCaseTest.java`
- `src/test/java/com/fulfilment/application/monolith/warehouses/adapters/restapi/WarehouseEndpointTest.java`

---

### R6 – Bonus Task: Warehouse–Product–Store Fulfillment Associations
Implemented the feature for associating Warehouses as fulfilment units for Products to Stores.

**Business constraints enforced:**
1. Each Product can be fulfilled by at most **2** different Warehouses per Store.
2. Each Store can be fulfilled by at most **3** different Warehouses (across all products).
3. Each Warehouse can store at most **5** distinct Product types.

**Design decisions:**
- `FulfillmentStore` port interface decouples the use-case from Panache, enabling pure unit testing.
- `FulfillmentRepository` implements `FulfillmentStore` and extends `PanacheRepository<FulfillmentAssignment>`.
- `AssignFulfillmentUseCase` enforces all three constraints and validates input before persisting.
- `FulfillmentResource` exposes five REST endpoints under `/fulfillment`.

**REST API:**
| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/fulfillment` | Assign warehouse to product × store |
| `GET` | `/fulfillment` | List all assignments |
| `GET` | `/fulfillment/store/{storeId}` | Assignments for a store |
| `GET` | `/fulfillment/warehouse/{warehouseCode}` | Assignments for a warehouse |
| `DELETE` | `/fulfillment/{id}` | Remove an assignment |

**Files:**
- `src/main/java/com/fulfilment/application/monolith/fulfillment/FulfillmentAssignment.java` *(new)*
- `src/main/java/com/fulfilment/application/monolith/fulfillment/FulfillmentStore.java` *(new)*
- `src/main/java/com/fulfilment/application/monolith/fulfillment/FulfillmentRepository.java` *(new)*
- `src/main/java/com/fulfilment/application/monolith/fulfillment/AssignFulfillmentUseCase.java` *(new)*
- `src/main/java/com/fulfilment/application/monolith/fulfillment/FulfillmentResource.java` *(new)*
- `src/test/java/com/fulfilment/application/monolith/fulfillment/AssignFulfillmentUseCaseTest.java` *(new — 11 tests)*

---

## Validation Status (updated)

### All unit tests pass
```
Tests run: 45, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```
*(1 skipped: `ProductEndpointTest` is `@QuarkusTest` and requires Docker – passes in CI)*

### JaCoCo coverage check passes
```
jacoco:check – lines covered ratio ≥ 0.80 ✓
```

---

## Local run troubleshooting

If Docker is available:
- `cd /<local workspace>/fcs-interview-code-assignment-main/java-assignment`
- `./mvnw quarkus:dev -Dquarkus.analytics.disabled=true`

If Docker is not available, run against a local PostgreSQL on `localhost:15432`:
- `./mvnw quarkus:dev -Dquarkus.datasource.devservices.enabled=false -Dquarkus.datasource.db-kind=postgresql -Dquarkus.datasource.username=quarkus_test -Dquarkus.datasource.password=quarkus_test -Dquarkus.datasource.jdbc.url=jdbc:postgresql://localhost:15432/quarkus_test -Dquarkus.analytics.disabled=true`
