# Vyapar Backend

A generic, multi-business POS and operations backend built with Java 21, Spring Boot 4.1, PostgreSQL, and MinIO.

The React and TypeScript application is maintained as a separate Git repository. During local development it can be
checked out at `frontend/`; that complete directory is ignored by this backend repository. Authentication, business,
outlet, menu management, real cart editing, customer capture, simple payment recording, and order management use the
backend. Dashboard and report screens remain future integration work.

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

`V1__initial_schema.sql` is the immutable baseline. Menu images and paid add-ons are introduced by the forward-only
`V4__menu_images_and_addons.sql` migration; later migrations normalize category positions, allow safe media reuse
between copied outlet catalogs, and add immutable order-item add-on snapshots in V12. The local development database's
earlier V1/V2/V3 history was backed up
and normalized once after those unreleased migrations had previously been squashed; migrations are strictly validated
again and must remain immutable from this point forward. V9 retains an earlier nullable preparation-state column for
checksum compatibility; the active order flow does not expose a separate Ready state. V10 adds outlet-level preparing
order cancellation policies.

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
environment-specific frontend configuration is required. The signed-in menu starts empty and shows only records saved
for the selected outlet; it never falls back to representative products or categories.

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
5. Create categories, reusable paid add-on groups, products, images, ingredients, and recipes under
   `/api/v1/outlets/{outletId}`.
6. Create and post a purchase to receive ingredient stock.
7. Create an order, optionally hold it, then call `/checkout` with zero or more payment lines.
8. Read `/receipt` or `/receipt.pdf`; use `/reports/dashboard` for the home screen.

Each order receives an outlet-scoped sequential number when its draft is created, displayed as `Order #000001` in the
frontend. Allocation locks only that outlet row, so orders submitted concurrently by multiple tills remain unique and
chronologically distinguishable. The opaque `order_...` primary key remains the API identifier and is not used as the
human-facing queue number. The current frontend supports adding and removing active menu items, changing quantities,
per-item kitchen notes, an optional table/token reference, and optional customer details. A customer name is required
only when saving a customer; the phone number can be left blank when the customer does not want to provide it. The
frontend creates the customer record and attaches it to the order. Staff can apply one fixed-amount or percentage
order-level discount for an offer or campaign. The deliberately simple payment UI records the discounted order total
as Cash, UPI, or Card; split payments, partial payments, custom tenders, payment references, and customer-balance
screens are not exposed in the current frontend.

The current frontend uses a deliberately simple order flow. `POST /api/v1/outlets/{outletId}/orders/{id}/prepare` moves
a draft or legacy held order to `PREPARING`. An empty payment list means “prepare now, pay later”; a full payment records
cash, UPI, or card and means “pay and prepare.” The new-order screen does not create held orders. Existing held records
remain visible in All orders and can be resumed so no local data is lost.

Preparing orders expose one final action based on payment status: an unpaid order uses “Collect payment & deliver,”
while a paid order uses “Complete & deliver.” Both close the order. Preparation consumes recipe inventory exactly once,
and final completion never consumes it again or records a duplicate payment.

Completed orders expose receipt preview, browser printing, and authenticated PDF download from order history. The
frontend loads a fresh signed business-logo URL when a receipt is opened and displays invoice details, item add-ons,
payments, paid amount, and remaining due. Reopening the receipt action supports later reprints without changing the
historical order snapshot.

The Orders screen opens on the outlet's current local date and lets staff move backward or forward by day or choose a
date directly. `GET /api/v1/outlets/{outletId}/orders?date=YYYY-MM-DD` returns that outlet-local calendar day as a
paginated list, while `GET /api/v1/outlets/{outletId}/orders/summary?date=YYYY-MM-DD` returns exact counts by workflow
state, unpaid-order count, and completed sales. The backend converts the selected date to UTC boundaries using the
outlet timezone; clients must not calculate those boundaries themselves. Cancelled orders remain available through the
dated history and its Cancelled tab.

The signed-in dashboard uses outlet-specific precomputed daily summaries. Every order remains assigned to the outlet
business date derived from its `createdAt` timestamp, even when it is paid, completed, cancelled, or refunded later.
Order transactions atomically append durable PostgreSQL dashboard events. The projection worker starts when either 100
eligible events accumulate or the oldest event has waited five minutes, then drains bounded transactions of 100 events.
It recalculates each changed order's latest contribution, applies only the difference to its daily summary, and safely
ignores duplicate or obsolete revisions. Existing orders are backfilled by V13. Monthly and lifetime figures sum daily
rows, while recent orders and seven-day best sellers use bounded indexed queries. The dashboard shows its last summary
update time because the projection is intentionally eventually consistent.

Dashboard APIs are `GET /api/v1/outlets/{outletId}/reports/dashboard/summary` for today/yesterday cards and recent
orders, and `GET /api/v1/outlets/{outletId}/reports/dashboard/insights` for monthly, lifetime, and best-seller data.
The frontend loads them independently, keeps the previous result in memory while refreshing, and polls the summary
every five minutes only while the dashboard tab is visible.

## Request logging and correlation

Every HTTP request receives a server-generated UUID in the `X-Request-Id` response header. The same value remains in
the logging MDC for the complete request. Authenticated workflows also add `userId`; authorized tenant and outlet
lookups add `merchantId` (the business ID) and `outletId`. The console log pattern prints these fields on every log line
so an API completion entry and the business-operation entries produced during that request can be correlated.

The HTTP filter logs one completion entry per request: successful responses at `INFO`, client/authentication failures at
`WARN`, and server or unhandled failures at `ERROR`. It records method, path, status, duration, query-parameter names,
and bounded JSON request/response bodies. It never logs authorization or cookie headers. Passwords, PINs, secrets,
tokens, emails, phone numbers, addresses, tax identifiers, UPI IDs, customer identity fields, and payment references are
recursively redacted. Multipart bodies, PDFs, CSV exports, non-JSON content, and content beyond the configured capture
limit are not logged as raw content.

Local body logging is controlled with `HTTP_LOG_INCLUDE_BODIES` and `HTTP_LOG_MAX_BODY_BYTES`. Set
`HTTP_LOG_INCLUDE_BODIES=false` when a shared or production environment should keep metadata-only request logs.

Each outlet configures whether unpaid preparing orders can be cancelled: always (the default), only within a 1–1440
minute window measured from preparation start, or never after preparation starts. The API returns `cancellable` and an
optional `cancellationDeadline` on each order so clients can hide expired actions, while the backend rechecks the policy
when cancellation is submitted. Cancelling a preparing order creates signed `SALE_REVERSAL` inventory movements rather
than deleting its original sale movements. Preparing orders with recorded payments require the completion/refund path
and are not directly cancellable.

The authenticated frontend now performs business onboarding before opening the application. A first-time owner must
provide a business name, outlet name, currency, and timezone. Legal name, phone, address, GSTIN, FSSAI number, UPI ID,
and receipt footer remain optional and can be maintained later. Business and outlet CRUD endpoints are:

- `GET/POST /api/v1/businesses`
- `GET/PUT/DELETE /api/v1/businesses/{businessId}`
- `GET/POST /api/v1/businesses/{businessId}/outlets`
- `GET/PUT/DELETE /api/v1/outlets/{outletId}`

Delete operations archive records rather than physically deleting them. Business updates and archiving require the
`OWNER` role. Outlet updates allow `OWNER` and `MANAGER`; outlet creation and archiving require `OWNER`.

List endpoints default to 20 records and accept at most 100 records per page. Order and item CSV exports require
`from` and `to` ISO-8601 timestamps, allow at most 366 days, stream their output, and reject exports above 100,000 rows.

Business logos and product images are stored in a private bucket. Upload endpoints return `key`, `downloadUrl`, and
`expiresAt`. Call `GET /api/v1/businesses/{businessId}/logo` or
`GET /api/v1/outlets/{outletId}/products/{productId}/images` to obtain fresh signed URLs when they expire. A product
supports up to six images. Upload one or more files using the multipart field `files`; each image must be JPEG, PNG, or
WebP and no larger than 5 MB. Configure the count with `STORAGE_MAX_PRODUCT_IMAGES`.

Menu categories organize products, while add-on groups represent optional paid choices shared by multiple products.
For example, an `Ice cream` group can contain `Vanilla +₹30` and `Chocolate +₹35`, then be assigned to any brownie.
Managers can configure how many options a customer may select. Add-on selection during order entry is deliberately
validated against the product's assigned groups and each group's maximum-selection rule. Order items store immutable
group, option, and price snapshots, and the selected add-on prices are multiplied by item quantity and included in the
order subtotal. Later menu edits therefore do not change historical order details or receipts.

Category positions are assigned by the backend in creation order. Clients send only the category `name`; they do not
choose a numeric display order. Active categories have unique positions within an outlet, and renaming a category keeps
its existing position. Category allocation is serialized per outlet, so simultaneous requests from multiple tills do
not produce duplicate positions. Category names are unique, ignoring letter case, only among active categories in the
same outlet. Archiving keeps existing product and historical references intact while allowing a new active category to
reuse the archived name.

When an owner creates an additional outlet, the active categories, products, product images, and add-on definitions from
the business's oldest active outlet are copied as independent catalog records. Subsequent changes remain local to each
outlet. When creating a product, the frontend lets an authorized user select one or more accessible outlets and choose
the initial active/inactive state for each outlet. The batch endpoint is
`POST /api/v1/outlets/{sourceOutletId}/products/batch`; it maps the source category and add-ons to each selected outlet.

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
