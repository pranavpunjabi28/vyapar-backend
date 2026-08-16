# AGENTS.md

## Project purpose

This repository contains a generic, multi-tenant POS and small-business operations backend. The first product is used by
a brownie cart, but domain behavior must remain reusable for other food and retail businesses.

The application is a Java 21 and Spring Boot 4.1 modular monolith. PostgreSQL is the production database, Flyway owns
the schema, MinIO provides local S3-compatible media storage, and Gradle is the build tool.

## Read before changing code

- Read `README.md` for local setup, supported workflows, roles, and deferred features.
- Treat `src/main/resources/db/migration` as the database source of truth.
- Preserve existing API behavior unless the requested change explicitly alters the contract.
- Do not copy branding, text, or image assets from Simply Vyapar. Screenshots are product-flow references only.

## Specification authority and requirement changes

- This file defines the engineering rules and durable product invariants for the repository. `README.md` explains how
  to run and use the current implementation; OpenAPI defines the current HTTP contract; Flyway migrations define the
  current persistent schema.
- Requirements added by the user to this file are intentional. Do not delete or weaken them merely because the current
  code does not comply yet. Bring affected code into compliance when working in that area and clearly report any larger
  migration that remains.
- When requirements conflict, protect correctness, tenant isolation, security, auditability, and financial data first.
  Explain the conflict and choose the safest design that still satisfies the product intent.
- Distinguish current behavior, an approved requirement, and a future idea. Do not present planned functionality as
  already implemented.
- A task-specific user instruction may change product behavior. When it does, update code, tests, OpenAPI, migrations,
  and documentation together so these sources do not drift.

## Current product scope

The current v1 backend covers:

- email/password authentication, JWT access tokens, refresh-token rotation, and staff invitations;
- multi-business and multi-outlet access with role and outlet assignment checks;
- categories, products, ingredients, recipes, customers, suppliers, purchases, and inventory movements;
- draft, held, closed, and cancelled orders, order-level discounts, recorded payments, customer dues, refunds, invoices,
  and receipts;
- receipt PDF generation, optional UPI QR codes, dashboard data, operational reports, and CSV exports; and
- local PostgreSQL and MinIO infrastructure through Docker Compose.

The following are intentionally deferred unless a task explicitly brings them into scope: phone OTP, AWS Cognito,
payment-gateway money movement, KOT and kitchen stations, item-level GST and advanced accounting, supplier payables,
expenses, XLSX exports, offline synchronization, and frontend implementation.

## General Principles

- Prefer simple and maintainable solutions over clever abstractions.
- Do not introduce new libraries unless there is a strong reason.
- Reuse existing utilities, services, validators, exception handlers, and patterns.
- Before creating a new class or abstraction, search for an existing equivalent.
- Keep changes focused on the requested task.
- Do not refactor unrelated code unless necessary.
- Do not modify public API behavior unless explicitly requested.
- Preserve backward compatibility where practical.
- Avoid speculative changes.
- Prefer root-cause fixes over patches that hide incorrect state.
- Make invalid states difficult to represent, and validate invariants again at the service boundary even when request
  DTO validation exists.
- Keep production behavior deterministic. Do not depend on unordered collections, default JVM timezone, floating-point
  arithmetic, or database row order.
- Fail explicitly when required infrastructure or configuration is unavailable. Do not silently downgrade security,
  persistence, or audit behavior.
- Avoid premature optimization, but do not introduce known unbounded queries, N+1 query patterns, or blocking work in
  request paths without documenting the reason.

## Design Principles

- Follow SOLID and established design principles where they improve the code.
- Prefer simple, readable, and maintainable solutions.
- Do not introduce design patterns or abstractions without a clear benefit.
- Keep classes and methods focused on a single responsibility.
- Prefer composition over inheritance where appropriate.
- Avoid tight coupling between modules.
- Design code so future requirements can be added without large changes to existing code.
- Introduce interfaces only when they provide meaningful abstraction or testability.
- Keep business logic outside controllers.
- Code should be easy to understand, test, maintain, and extend.
- Avoid overengineering.
- Preserve the modular-monolith boundary. A new deployable service requires an explicit product or operational reason,
  not only code organization preference.
- Keep transport DTOs, persistence entities, and domain operations separate. Never use a JPA entity as a public API
  request or response type.
- Model important workflows as explicit state transitions. Do not allow arbitrary setters or generic update endpoints
  to bypass order, purchase, payment, refund, invitation, or inventory rules.

## Agent Decision Making

- Search the existing project before creating new classes, utilities, constants, exceptions, or abstractions.
- Reuse existing implementations and project conventions where appropriate.
- Understand the existing architecture before making structural changes.
- Keep changes focused on the requested task.
- Do not refactor unrelated code.
- Prefer the smallest clean solution that solves the root problem.
- Do not blindly follow a proposed implementation if it introduces correctness, security, concurrency, performance, or architectural issues.
- Point out important problems and recommend a better approach before implementing.
- Clearly distinguish between verified behavior and assumptions.
- Do not invent framework behavior, APIs, configuration properties, or library functionality.
- Verify uncertain framework behavior against official documentation when practical.
- If an instruction in this file is not yet reflected in the code, preserve the instruction and update the affected
  implementation when it is within task scope. If compliance requires a broad migration, document the gap and a safe
  migration path instead of silently ignoring the rule.
- Inspect the working tree and staged changes before editing. Treat existing changes as user-owned and never overwrite,
  revert, reformat, or stage unrelated work.

## Java Guidelines

- Use Java 21 features only when they improve readability.
- Prefer constructor injection over field injection.
- Use `final` for dependencies and values that should not change.
- Avoid unnecessary mutable state.
- Avoid deeply nested conditionals.
- Prefer early returns when they improve readability.
- Use meaningful variable and method names.
- Avoid abbreviations unless they are already established in the project.
- Do not catch `Exception` unless there is a specific reason.
- Never silently swallow exceptions.
- Preserve the original cause when wrapping exceptions.
- Do not use `System.out.println`.
- Use the project logging framework.
- Use records for immutable request/response projections when they remain readable; use regular classes when lifecycle,
  validation, or framework behavior makes records unsuitable.
- Use Lombok only to remove mechanical boilerplate. Do not use `@Data` on JPA entities, and do not generate entity
  `equals`, `hashCode`, or `toString` methods that traverse lazy relationships or expose sensitive values.
- Keep transaction boundaries on service methods. Controllers and repositories must not coordinate multi-step business
  transactions.
- Avoid returning `null` collections. Prefer empty immutable collections for response DTOs and service results.

## Constants

- Avoid magic strings and duplicated hardcoded values.
- Maintain separate constants classes where values are genuinely shared or externally significant:
  - `JsonKeys` for manually referenced request/response JSON keys.
  - `DbFields` for database/column names referenced as strings.
  - `ErrorMessages` for reusable error messages.
  - `LogMessages` for reusable or standardized log messages.
- Prefer type-safe references such as QueryDSL fields over string-based database fields when available.
- Do not create constants unnecessarily when they reduce readability.
- Enum values, configuration properties, HTTP header names supplied by Spring, and one-use local literals do not need a
  constants class solely to satisfy this section.

## API Exception Handling

- Use centralized `@RestControllerAdvice` for REST API exception handling.
- Do not duplicate exception handling across controllers.
- Use one common `ApiError` contract represented as RFC 9457 Problem Details. The current implementation uses
  `ProblemDetail`; if a dedicated `ApiError` Java type is introduced, it must preserve the same external media type and
  fields rather than creating a second error format.
- API errors should contain appropriate information such as:
  - error code
  - message
  - reason
  - HTTP status
  - timestamp when useful
- Always return appropriate HTTP status codes.
- Never return `200 OK` for failed operations.
- Handle request validation errors centrally.
- Prefer meaningful domain-specific exceptions over generic `RuntimeException`.
- Never expose stack traces, SQL details, internal implementation details, or sensitive information in API responses.
- Keep stable machine-readable error codes separate from human-readable messages. Clients must be able to branch on the
  code without parsing message text.
- Map malformed JSON, validation failures, authentication/authorization failures, optimistic-lock conflicts, database
  constraints, missing resources, and unavailable external storage consistently.

## Architecture

Keep code grouped by business feature under `com.bbu.vyaparbackend`:

- `auth`: identity, JWTs, refresh tokens, and password operations
- `business`: businesses, outlets, memberships, roles, and invitations
- `catalog`: categories, products, ingredients, and recipes
- `customer`: customer records
- `inventory`: suppliers, purchases, adjustments, and inventory movements
- `order`: POS order lifecycle, invoices, checkout, and receipts
- `payment`: payments and refunds
- `report`: dashboards and exports
- `file`: S3-compatible media storage
- `shared`: cross-cutting persistence and API infrastructure

Prefer feature services over direct cross-package repository access. Keep controllers limited to HTTP mapping,
validation, authorization entry points, and DTO conversion. Transactional business rules belong in services.

Dependencies should flow through public feature services. Do not make repositories public only to let another feature
modify an aggregate directly. Shared code belongs in `shared` only when it is genuinely cross-cutting; `shared` must not
become a miscellaneous package.

## Non-negotiable domain rules

- All business-owned records must be tenant-scoped. Outlet-owned records must also be outlet-scoped.
- Never trust a business or outlet ID merely because it came from an authenticated client. Validate membership and
  outlet assignment through `TenantAccess`.
- Respect the `OWNER`, `MANAGER`, `CASHIER`, and `INVENTORY` permission boundaries.
- Closed orders are immutable. Corrections use cancellation before closing or auditable refunds after closing.
- Preserve order item name and price snapshots so historical receipts do not change with the catalog.
- A customer is required when checkout leaves an unpaid balance.
- Inventory is an append-only signed movement ledger. Purchases and stock-restoring refunds add stock; sales and wastage
  subtract stock; cancelling a posted purchase creates reversing movements.
- Inventory may become negative and must be reported rather than silently preventing checkout.
- Invoice numbers are sequential per outlet and must remain safe under concurrent checkout.
- Use `BigDecimal` for money and quantities. Never use `double` or `float` for domain calculations.
- Persist timestamps as Unix epoch milliseconds in `bigint` columns. Represent them as UTC `Instant` in Java and apply
  the configured outlet timezone only at reporting or presentation boundaries.
- Archive business records instead of physically deleting them unless the record is an internal replaceable join or
  child row and deletion is explicitly safe.
- Draft purchases do not affect stock. Posting creates purchase movements; cancelling a posted purchase creates
  reversing movements rather than deleting history.
- Closing an order deducts recipe ingredients exactly once in the same transaction as invoice allocation and payment
  recording.
- Payment state is separate from order state. Recorded payments must never exceed the supported order balance rules,
  and refunds must never exceed collected funds or sold quantities.
- Staff invitation outlet access is selected by an authorized owner. Invitees must not choose or expand their own outlet
  assignments.
- Monetary rounding must be explicit and consistent at calculation boundaries. Persist currency with the owning outlet
  and never combine monetary values from different currencies.

## Transactions and concurrency

- Apply `@Transactional` at the service operation that owns the complete use case, not piecemeal across controller calls.
- Invoice allocation, checkout, purchase posting/cancellation, inventory adjustment, payment recording, refund creation,
  refresh-token rotation, and invitation acceptance must be atomic.
- Retain optimistic locking for concurrent edits. Translate stale-write failures into a conflict response that tells the
  client to reload rather than silently overwriting newer data.
- Use pessimistic locking only for narrow serialization requirements such as per-outlet invoice allocation. Keep locked
  transactions short and never call remote services while holding database locks.
- Design retried operations so they cannot duplicate invoices, payments, refunds, stock movements, or token rotation.
  Add an idempotency mechanism when a client or integration can legitimately retry a financial mutation.

## API conventions

- Keep public endpoints below `/api/v1`.
- Validate request DTOs with Jakarta Validation.
- Return DTOs rather than JPA entities.
- Paginate searchable collections and support deterministic sorting.
- Use the shared `ApiException` and RFC Problem Details handler for expected failures.
- Keep OpenAPI output accurate whenever an endpoint, request, response, or enum changes.
- Media responses expose stable object keys. Storage-provider-specific URLs or identifiers must not leak into domain
  records.
- Never log passwords, bearer tokens, refresh tokens, invitation codes, database credentials, or customer-sensitive
  data.
- Use appropriate response codes: `201` for newly created resources when practical, `204` for successful operations with
  no body, `400` for malformed input, `401` for missing/invalid authentication, `403` for denied tenant/role access,
  `404` for inaccessible or absent scoped resources, and `409` for state or concurrency conflicts.
- New list endpoints must define pagination limits, allowed sort fields, default ordering, and search behavior. Never
  expose arbitrary entity-property sorting without validation.
- Use ISO-8601 timestamps and explicit currency/unit fields. Do not return locale-formatted money or dates from JSON APIs.
- Keep endpoint naming resource-oriented. Use explicit action subresources only for domain transitions such as `hold`,
  `checkout`, `post`, `cancel`, `refund`, `refresh`, and `accept`.
- Update Swagger security requirements and examples when authentication or request formats change.

## Persistence and migrations

- Do not use Hibernate schema generation outside tests. Production uses `spring.jpa.hibernate.ddl-auto=validate`.
- Every persistent schema change requires a new versioned Flyway migration. Never edit a migration that may already have
  been applied outside a disposable local database.
- Before the first deployment, migrations may be squashed into the initial V1 baseline only when the product owner
  explicitly confirms that no shared or production database depends on the earlier migration history.
- Add indexes for tenant filters, outlet filters, report date ranges, invoice lookups, and other demonstrated query
  patterns.
- Retain optimistic locking on mutable aggregate roots and pessimistic locking where invoice allocation requires
  serialization.
- Confirm that entity relationships cannot cross business or outlet boundaries in service validation.
- Use opaque, custom-generated string primary keys. Every entity/table has a stable lowercase prefix followed by an
  underscore and a 20-character cryptographically random base-62 value (for example `user_a8K2...`). Foreign keys use
  the same string value. Retain audit timestamps, archive state, and optimistic-lock versioning on persistent business
  entities.
- Use `numeric(19,4)`/`BigDecimal` for stored money and quantities unless a documented domain requirement needs another
  scale. Apply business rounding before persistence rather than relying on database truncation.
- Avoid cascade settings that can delete audit or financial history. Load only the relationships required by a use case
  and keep `spring.jpa.open-in-view=false`.
- Never expose sequential invoice numbers as globally unique; uniqueness and allocation are scoped to an outlet.
- After adding a migration, validate it by starting the application against PostgreSQL. Restore automated migration
  verification only when the user explicitly authorizes test work.

## Security

- Public endpoints are limited to registration, login, refresh, invitation acceptance, health, and API documentation.
- Store passwords with the configured `PasswordEncoder` and refresh/invitation tokens only as hashes.
- Keep access tokens short-lived and rotate refresh tokens.
- Add explicit authorization checks to every new business or outlet operation.
- When automated testing is restored, changes to authentication, tenancy, payments, refunds, or invoice allocation
  require negative-path and cross-tenant coverage.
- Normalize emails consistently before lookup and uniqueness checks.
- Validate uploaded file size and content type; generate server-owned object keys and never accept arbitrary bucket keys
  or filesystem paths from clients.
- CORS origins, secrets, database credentials, storage credentials, and token lifetimes must come from configuration.
  Development defaults must never be treated as production-safe values.
- Prefer returning `404` for a tenant-scoped identifier when revealing that the resource exists would leak another
  tenant's data; use `403` when the caller's known membership lacks an allowed role or outlet assignment.
- Security-sensitive logs may include a correlation identifier and internal record ID, but not tokens, raw invitation
  codes, passwords, payment references, or unnecessary personal data.

## Data privacy, audit, and logging

- Collect only customer and staff data needed for an approved workflow. Avoid adding sensitive fields speculatively.
- Never place secrets or personal data in exception messages, metrics labels, URLs, object keys, or source-controlled
  fixtures.
- Business actions that change money, stock, permissions, or immutable sales history must remain attributable and
  auditable.
- Use structured, parameterized logging. Choose log levels deliberately and avoid logging routine validation failures as
  server errors.
- Preserve audit and ledger records according to product/legal requirements. Archiving a parent must not make required
  financial history unreadable.

## Performance and reporting

- Scope every operational and report query by authorized business/outlet before applying filters.
- Bound report date ranges and export sizes when data volume can grow; use streaming or background generation before an
  export becomes too large for a normal request.
- Avoid N+1 queries in list, dashboard, receipt, and export paths. Add projections or purpose-built aggregate queries
  where measurement shows repeated lazy loading.
- Add database indexes based on demonstrated query predicates and ordering. Verify query plans for high-volume reports
  rather than adding indexes speculatively.
- Dashboard and report calculations must use the outlet timezone for date boundaries and UTC instants for persistence.

## Testing and verification

Automated tests are temporarily deferred by explicit product-owner decision while the initial backend structure is being
established. Do not create or restore test classes or test-only dependencies unless the user explicitly requests test
implementation. Until that decision changes, verify production compilation and packaging with:

```bash
./gradlew clean build
```

Also validate configuration, Flyway migrations through application startup, Docker Compose configuration, and affected
API workflows manually when the required local services are available. Report every check that could not be run.

When the user explicitly restores automated testing, the intended coverage is:

- Unit-test calculations and state transitions.
- Add integration coverage for repositories, migrations, transactions, and authorization.
- Maintain an end-to-end acceptance scenario for the main business workflow.
- Add a Flyway schema smoke test for every schema change.
- Use PostgreSQL Testcontainers as the migration and database-behavior authority when Docker is available.
- Verify receipt PDFs and CSV output when changing their layouts or fields.
- Test success, validation failure, forbidden role, cross-tenant access, invalid state transition, and concurrency/retry
  behavior for security- or finance-sensitive operations.
- Prefer behavior assertions over implementation-detail assertions. Tests must remain deterministic and independent of
  execution order, machine timezone, and existing local data.
- Do not replace PostgreSQL-specific integration coverage with H2-only coverage. H2 may be used for fast tests, while
  Testcontainers remains the compatibility authority for migrations and database behavior.

Once automated tests are restored, do not weaken or remove one merely to make a build pass. Fix the underlying behavior
or update the test only when the intended contract has deliberately changed.

## Local services and configuration

Use `compose.yaml` for PostgreSQL, MinIO, and the API:

```bash
docker compose up --build
```

Use environment variables for secrets and deployment-specific values. Keep `.env.example` safe and current, but never
commit real `.env` files, credentials, tokens, local object-storage data, database volumes, or generated receipts.

- Keep `.env.example`, `application.properties`, `compose.yaml`, and `README.md` synchronized when adding configuration.
- Use MinIO only as the local S3-compatible implementation. Preserve stable object keys and storage abstractions so AWS
  S3 migration does not change public API contracts or persisted references.
- Do not make tests depend on a developer's existing PostgreSQL database, MinIO bucket, clock, locale, or credentials.

## Documentation and compatibility

- Treat generated OpenAPI as part of the frontend contract. Breaking request/response changes require explicit approval,
  migration notes, and coordinated frontend updates.
- Document new environment variables, setup steps, role permissions, workflows, and operational limitations in
  `README.md`.
- Use comments to explain non-obvious reasons, invariants, locking, or compatibility constraints. Do not narrate obvious
  code line by line.
- Mark deprecations before removal when practical. Do not silently rename JSON fields, enum values, routes, invoice
  formats, or persisted meanings.

## Git and collaboration

- Inspect `git status` and both staged and unstaged diffs before editing.
- Do not amend, commit, push, rebase, reset, or discard changes unless the user explicitly requests that Git operation.
- Stage only files requested by the user or files changed for the active task. Never stage unrelated existing changes.
- Keep generated outputs, secrets, IDE metadata, local runtime data, and reference screenshots out of Git.
- Prefer small, cohesive commits with messages that describe user-visible behavior or a clear internal objective.

## Change discipline

- Keep changes focused on the requested behavior.
- Preserve unrelated user edits and untracked files.
- Do not commit generated `build/`, IDE metadata, secrets, logs, screenshots, or local runtime data.
- Update `README.md` when setup, environment variables, workflows, or public endpoints change.
- Before handoff, report what changed, which verification ran, and any checks that could not run because an external
  service was unavailable.

## Definition of done

A change is complete only when all applicable items are true:

- the implementation satisfies the approved behavior and preserves the domain invariants in this file;
- tenant, role, validation, state-transition, error, and concurrency paths are handled;
- database changes include forward-only Flyway migrations and required indexes;
- the relevant build, configuration, migration, and manual verification commands pass; automated tests apply only after
  the user explicitly restores test implementation;
- OpenAPI, configuration examples, and `README.md` are updated when their contracts change;
- no secrets, generated files, unrelated edits, debugging output, or sensitive logs were introduced; and
- the handoff states what changed, what was verified, and any remaining limitation or external blocker.
