# Authentication contract

This document records the authentication contract that existed before the frontend integration and the minimal
transport changes approved for browser sessions. Password verification, JWT signing, refresh-token hashing, rotation,
and revocation remain server-owned behavior.

## Contract before browser integration

All endpoints are below `/api/v1/auth`.

| Method and path | Authentication | Request | Successful response |
|---|---|---|---|
| `POST /register` | Public | `{ email, password, displayName }` | `200` token JSON |
| `POST /login` | Public | `{ email, password }` | `200` token JSON |
| `POST /refresh` | Public | `{ refreshToken }` | `200` rotated token JSON |
| `POST /logout` | Bearer token | `{ refreshToken }` | `200`, empty body |
| `GET /me` | Bearer token | None | `200` current-user JSON |
| `POST /password` | Bearer token | `{ currentPassword, newPassword }` | `200`, empty body |

The old token JSON was:

```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<opaque-secret>",
  "accessTokenExpiresAt": "2026-08-23T10:15:00Z",
  "userId": "user_<20-character-random-part>",
  "email": "owner@example.com",
  "displayName": "Owner"
}
```

Expected failures use `application/problem+json`/RFC Problem Details. Validation errors can include an `errors` object
whose keys are request-field names.

## Mismatches found

- The raw refresh token crossed the JavaScript boundary in both responses and request bodies.
- The CORS policy did not allow credentialed requests, so an API cookie could not be used across configured origins.
- Logout required a valid access token, which prevented revocation after the access token expired.
- Spring Security authentication and authorization failures were not guaranteed to use the same Problem Details shape
  as application errors.
- The frontend login/register form enabled a demo session and did not call any auth endpoint.
- The frontend API client stored an access token in memory but did not restore a session, refresh on `401`, coordinate
  concurrent refresh attempts, distinguish `403`, or send cookies.

## Browser session contract

Registration and login return an access token in JSON and set the refresh token in a host-only cookie. Refresh rotates
that cookie. Logout revokes the current refresh token when present and clears the cookie. The raw refresh token is never
returned to or read by frontend JavaScript.

The refresh cookie is:

- `HttpOnly`;
- `SameSite=Strict`;
- scoped to `/api/v1/auth`;
- host-only (no `Domain` attribute);
- `Secure` in shared/production environments; local HTTP development can disable `Secure` explicitly; and
- bounded by the configured refresh-token lifetime.

| Method and path | Authentication | Request | Successful response |
|---|---|---|---|
| `POST /register` | Public | `{ email, password, displayName }` | `200` access-session JSON and refresh cookie |
| `POST /login` | Public | `{ email, password }` | `200` access-session JSON and refresh cookie |
| `POST /refresh` | Refresh cookie | No body | `200` rotated access-session JSON and refresh cookie |
| `POST /logout` | Refresh cookie when present | No body | `204` and expired refresh cookie |
| `GET /me` | Bearer access token | None | `200` current-user JSON |
| `POST /password` | Bearer access token | `{ currentPassword, newPassword }` | `204` |

The browser-visible access-session JSON is:

```json
{
  "accessToken": "<jwt>",
  "accessTokenExpiresAt": "2026-08-23T10:15:00Z",
  "user": {
    "id": "user_<20-character-random-part>",
    "email": "owner@example.com",
    "displayName": "Owner",
    "passwordChangeRequired": false
  }
}
```

The frontend keeps the access token only in memory. On startup it calls `/refresh`, stores the returned access token in
memory, and confirms identity with `/me`. An authenticated request that receives `401` performs one coordinated refresh
and retries once. A failed refresh clears the in-memory session and returns the user to sign-in. A `403` keeps the
session and presents an access-denied message because it represents insufficient permission rather than expiration.

## Security notes

SameSite cookies reduce CSRF exposure, and the refresh cookie is intentionally restricted to auth paths. Production
must set `REFRESH_COOKIE_SECURE=true`, serve the application over HTTPS, and configure exact trusted CORS origins. The
development-only `false` setting exists solely because the local Vite/API workflow uses HTTP.
