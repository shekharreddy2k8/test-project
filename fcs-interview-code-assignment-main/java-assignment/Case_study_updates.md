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

## Local run troubleshooting

If Docker is available:
- `cd /<local workspace>/fcs-interview-code-assignment-main/java-assignment`
- `./mvnw quarkus:dev -Dquarkus.analytics.disabled=true`

If Docker is not available, run against a local PostgreSQL on `localhost:15432`:
- `./mvnw quarkus:dev -Dquarkus.datasource.devservices.enabled=false -Dquarkus.datasource.db-kind=postgresql -Dquarkus.datasource.username=quarkus_test -Dquarkus.datasource.password=quarkus_test -Dquarkus.datasource.jdbc.url=jdbc:postgresql://localhost:15432/quarkus_test -Dquarkus.analytics.disabled=true`
