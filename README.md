# Enterprise Resource Orchestration Platform (EROP)

Automates the full lifecycle of an enterprise resource request (laptops, software licenses, peripherals, equipment):

**Submit → Policy validation → Manager approval → Inventory check → IT assignment → Asset tracking → Audit trail**

Stack: Java 25 (LTS), Spring Boot 3.5, Spring Data JPA / Hibernate, Spring Security, H2 (default) or MySQL, OpenAPI/Swagger, JUnit 5.

## Run it

```bash
mvn spring-boot:run              # H2 in-memory, zero setup
mvn test                         # runs the lifecycle tests
mvn spring-boot:run -Dspring-boot.run.profiles=mysql   # MySQL on localhost:3306 (see application-mysql.yml)
```

* Swagger UI: http://localhost:8080/swagger-ui.html  (click **Authorize**, enter a user below)
* H2 console: http://localhost:8080/h2-console  (JDBC URL `jdbc:h2:mem:orchestrator`, user `sa`, empty password)

| User  | Password   | Role      | Notes                         |
|-------|------------|-----------|-------------------------------|
| bob   | `password` | EMPLOYEE  | manager = alice               |
| carol | `password` | EMPLOYEE  | manager = alice               |
| alice | `password` | MANAGER   | manager = admin               |
| ian   | `password` | IT_ADMIN  | assigns assets, manages stock |
| admin | `password` | ADMIN     | can do everything             |

## Architecture

```
 HTTP ──> web (controllers, DTOs, exception handler)      thin: validation + role check (@PreAuthorize)
            └─> service (RequestService, FulfillmentService, AssetService, AuditService)
                      │      business logic + @Transactional boundaries
                      ├─> policy (PolicyEngine + PolicyRule strategies)
                      └─> repository (Spring Data JPA) ──> domain entities ──> SQL DB
```

## Request lifecycle (state machine in `RequestStatus`)

```
 submit ──policy fails──> REJECTED_BY_POLICY
   │
   v
 PENDING_APPROVAL ──> REJECTED / CANCELLED
   │ manager approves
   v
 APPROVED ──IT assigns, stock exists──> FULFILLED
   │ no stock
   v
 BACKORDERED ──stock arrives (auto, FIFO)──> FULFILLED
```

## What does what

| Package / class | Responsibility |
|---|---|
| `domain/*` | JPA entities: `AppUser`, `ResourceType` (catalog), `Asset` (physical unit / license seat), `ResourceRequest`, `AuditLog`. `RequestStatus` owns the legal transitions. |
| `repository/*` | Data access. `AssetRepository.findAvailableForUpdate` uses `SELECT … FOR UPDATE` (pessimistic lock). |
| `policy/*` | **Strategy pattern.** `PolicyRule` implementations: `ApprovalChainRule`, `BudgetCeilingRule`, `EntitlementLimitRule`. `PolicyEngine` runs all and collects every violation. Add a rule = add one `@Component`. |
| `service/RequestService` | Submit (+policy), approve/reject/cancel, record-level authorization, queries. |
| `service/FulfillmentService` | Inventory check, asset assignment, backorder, FIFO backorder fulfilment. |
| `service/AssetService` | Register stock, return assets, catalog and inventory summary. |
| `service/AuditService` | Writes audit rows with `Propagation.MANDATORY` so they share the business transaction. |
| `security/*`, `config/SecurityConfig` | HTTP Basic auth against the users table, BCrypt, role-based method security. |
| `web/*` | REST controllers, DTO records, `GlobalExceptionHandler` (RFC 7807 problem JSON). |
| `config/DataSeeder` | Demo users, catalog and stock. |

## Key design decisions (good viva answers)

1. **Atomic transactions** – approving/assigning changes request, asset and audit rows in one `@Transactional` unit; all or nothing.
2. **No double assignment** – pessimistic row lock on the chosen asset + `@Version` optimistic locking on `Asset` and `ResourceRequest`.
3. **State machine** – illegal moves (e.g. approving a rejected request) throw `BusinessRuleException` → HTTP 409.
4. **Three layers of access control** – login (URL), role (`@PreAuthorize`), ownership (service: only *your* manager approves; nobody approves their own request).
5. **Audit trail** – append-only, written in the same transaction, so history can never disagree with the data.
6. **Policy as pluggable strategies** – Open/Closed principle.
7. **`open-in-view: false`** – services map entities to DTO records inside the transaction; no lazy-loading surprises.
8. **Backorders** – out-of-stock requests wait in a FIFO queue and are fulfilled automatically when assets are registered or returned.

## Demo script (curl)

```bash
# 1. Bob views the catalog and requests a laptop (id 1)
curl -u bob:password localhost:8080/api/catalog
curl -u bob:password -H 'Content-Type: application/json' \
     -d '{"resourceTypeId":1,"justification":"New joiner"}' localhost:8080/api/requests

# 2. Bob asks for a second laptop -> REJECTED_BY_POLICY (entitlement limit)
curl -u bob:password -H 'Content-Type: application/json' \
     -d '{"resourceTypeId":1,"justification":"Spare"}' localhost:8080/api/requests

# 3. Alice sees her inbox and approves
curl -u alice:password localhost:8080/api/requests/pending-approval
curl -u alice:password -H 'Content-Type: application/json' -d '{"comment":"Approved"}' -X POST localhost:8080/api/requests/1/approve

# 4. Bob cannot approve (403); Ian assigns the laptop
curl -u bob:password -X POST localhost:8080/api/requests/1/approve
curl -u ian:password localhost:8080/api/requests/fulfillment-queue
curl -u ian:password -X POST localhost:8080/api/requests/1/assign

# 5. Inventory and full audit trail
curl -u ian:password localhost:8080/api/inventory
curl -u ian:password localhost:8080/api/audit/requests/1
```

## REST API summary

| Method & path | Who | Purpose |
|---|---|---|
| `GET /api/catalog` | any user | List requestable items |
| `POST /api/requests` | any user | Submit request |
| `GET /api/requests/mine`, `GET /api/requests/{id}` | owner / manager / IT / admin | View |
| `POST /api/requests/{id}/cancel` | requester | Withdraw |
| `GET /api/requests/pending-approval` | MANAGER, ADMIN | Approval inbox |
| `POST /api/requests/{id}/approve` or `/reject` | MANAGER (direct manager only), ADMIN | Decide |
| `GET /api/requests/fulfillment-queue` | IT_ADMIN, ADMIN | Approved + backordered |
| `POST /api/requests/{id}/assign` | IT_ADMIN, ADMIN | Inventory check + assign |
| `GET /api/inventory`, `GET/POST /api/assets`, `POST /api/assets/{id}/return` | IT_ADMIN, ADMIN | Stock & asset tracking |
| `GET /api/audit`, `/api/audit/requests/{id}`, `/api/audit/assets/{id}` | IT_ADMIN, ADMIN | Audit trail |

## Ideas to extend
JWT authentication, multi-level approval chains, email notifications via Spring events, Flyway migrations, a React front end.
