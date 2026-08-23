# Vyapar Backend

A generic, multi-business POS and operations backend built with Java 21, Spring Boot 4.1, PostgreSQL, and MinIO.

The React and TypeScript application is maintained as a separate Git repository. During local development it can be
checked out at `frontend/`; that complete directory is ignored by this backend repository. It includes a reviewable
demo experience for the dashboard, catalog, POS cart, checkout, orders, reports, and settings while the remaining
forms are connected to the backend incrementally.

## What is implemented

- Email/password registration, login, in-memory JWT access tokens, rotating HttpOnly refresh cookies, logout, and
  password changes
- Businesses, separate outlets, owner/staff roles, outlet assignments, and expiring invitation codes
- Categories, products, private product images, ingredients, recipes, customers, and suppliers
- Draft/post/cancel purchase workflow and an auditable inventory ledger
- Draft, held, closed, and cancelled orders with immutable price snapshots
- Fixed/percentage order discounts, split payments, partial payments, customer dues, and per-outlet invoice sequences
- Recipe-based stock deduction, audited refunds, and optional stock restoration
- Receipt JSON/PDF with optional UPI QR, database-aggregated dashboards, paginated reports, and streaming CSV exports
- Private S3-compatible media with short-lived signed URLs
- Swagger/OpenAPI, Problem Details errors, Flyway migrations, and Docker Compose

The unreleased project currently uses one consolidated Flyway baseline: `V1__initial_schema.sql`.

## Run locally

Install Docker Desktop and ensure it is running, then execute:

```bash
docker compose up --build
```

Services:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- PostgreSQL: `localhost:5432`
- MinIO API: `http://localhost:9000`
- MinIO console: `http://localhost:9001`

The Compose credentials are development-only. Change `JWT_SECRET`, database credentials, and MinIO credentials before
using a shared environment.

To run the API from IntelliJ, start only its dependencies:

```bash
docker compose up postgres minio
./gradlew bootRun
```

### Frontend preview

Install the frontend dependencies and start Vite in a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173` to register, sign in, or restore an existing backend session. Open
`http://localhost:5173/?demo=1` for the isolated interactive demo. The Vite development server proxies `/api` requests
to the Spring Boot API at `http://localhost:8080`. Copy `frontend/.env.example` to `frontend/.env` when
environment-specific frontend configuration is required. Screens after authentication still use representative data
and will be connected to their feature APIs incrementally.

Create a production frontend bundle with:

```bash
cd frontend
npm run build
```

## First API workflow

1. `POST /api/v1/auth/register` creates an owner account, returns an access token, and sets the rotating refresh token
   as an HttpOnly SameSite cookie.
2. Send `Authorization: Bearer <accessToken>` on protected requests.
3. `POST /api/v1/businesses` creates a business.
4. `POST /api/v1/businesses/{businessId}/outlets` creates an outlet.
5. Create categories, products, ingredients, and product recipes under `/api/v1/outlets/{outletId}`.
6. Create and post a purchase to receive ingredient stock.
7. Create an order, optionally hold it, then call `/checkout` with zero or more payment lines.
8. Read `/receipt` or `/receipt.pdf`; use `/reports/dashboard` for the home screen.

List endpoints default to 20 records and accept at most 100 records per page. Order and item CSV exports require
`from` and `to` ISO-8601 timestamps, allow at most 366 days, stream their output, and reject exports above 100,000 rows.

Business logos and product images are stored in a private bucket. Upload endpoints return `key`, `downloadUrl`, and
`expiresAt`. Call `GET /api/v1/businesses/{businessId}/logo` or
`GET /api/v1/outlets/{outletId}/products/{productId}/image` to obtain a fresh signed URL when one expires. Images must
be JPEG, PNG, or WebP and no larger than 5 MB.

Database primary keys are opaque strings with a table-specific prefix and a 20-character random base-62 suffix, for
example `user_aOsQVrbouDyheUIyD3yN` or `product_...`. Clients must treat IDs as complete strings and must not parse or
generate them. All application timestamp columns are stored as Unix epoch milliseconds in PostgreSQL `bigint` columns.
Java and the JSON API continue to use UTC `Instant`/ISO-8601 values so timezone conversion remains explicit and safe.

Registration example:

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"owner@example.com","password":"change-me-now","displayName":"Owner"}'
```

Browser authentication keeps the short-lived access token in memory. `POST /api/v1/auth/refresh` uses the cookie,
rotates it, and returns a new access token; it accepts no request body. `POST /api/v1/auth/logout` revokes and clears the
cookie. See [the authentication contract](docs/AUTHENTICATION.md) for the request/response shapes and session flow.

## Important business rules

- All business/outlet access is checked on the server. Supplying another tenant's ID does not grant access.
- `OWNER` has full business access. `MANAGER`, `CASHIER`, and `INVENTORY` are limited by both role and outlet
  assignment.
- A closed order cannot be edited. Use refunds for corrections.
- A customer is required when checkout leaves an unpaid balance.
- Inventory uses signed ledger movements. Sales may make stock negative so an inaccurate count never blocks checkout.
- Product name and price are copied onto order items, keeping old receipts stable when the catalog changes.
- Staff invitations record the allowed outlets when created; invitees cannot choose their own outlet access.

## Development verification

```bash
./gradlew clean build
```

Automated tests are temporarily deferred during the initial backend restructuring and must only be restored when
explicitly requested. Until then, verify Flyway and Hibernate validation by starting the application against PostgreSQL,
and manually smoke-check affected API workflows through Swagger or `curl`.

Package layout follows business features:

```text
auth       identity, JWT, refresh tokens
business   tenants, outlets, roles, invitations
catalog    categories, products, ingredients, recipes
customer   customer records
inventory  suppliers, purchases, movements
order      POS lifecycle, receipts, invoice allocation
payment    payments and refunds
report     dashboard and exports
file       S3-compatible media storage
shared     audit entities, pagination, API errors
```

## Production notes

- Use a managed PostgreSQL database and a private S3-compatible bucket. The media API stores stable object keys and
  exposes expiring signed URLs, so MinIO can be replaced by AWS S3 without changing persisted references.
- Serve the API behind HTTPS and use a high-entropy JWT secret from a secret manager.
- Set `REFRESH_COOKIE_SECURE=true` outside local HTTP development. Use exact trusted CORS origins; credentialed browser
  requests are enabled for those configured origins.
- Add email delivery around the returned staff invitation code before public launch.
- AWS Cognito, payment gateways, KOT, item-level GST, accounting, expenses, and offline synchronization are
  intentionally deferred.
