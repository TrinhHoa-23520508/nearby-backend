# Identity API contract (Accounts and Authorization)

This contract covers SRS section 3.1 (FR-ID-01 to FR-ID-08) and the account part of UC20. The live OpenAPI document is at `/v3/api-docs`, and Swagger UI is at `/swagger-ui.html`. A Postman collection is in [`docs/postman/`](../postman/).

## Conventions

| Topic | Rule |
|---|---|
| Base path | `/api/v1` |
| Body | JSON (`application/json`); timestamps are ISO-8601 in UTC |
| Auth | `Authorization: Bearer <accessToken>` on every endpoint except `/api/v1/auth/**` |
| Errors | RFC 9457 `application/problem+json` with a stable `code` field. Validation errors add `errors: [{field, message}]` |
| Rate limits | `429 RATE_LIMITED` with a `Retry-After` header in seconds |
| Pagination | Cursor based: `{items, nextCursor, hasMore}`. Pass `nextCursor` back as `cursor` |

Error example:

```json
{
  "type": "https://nearby.uit.edu.vn/problems/weak-password",
  "title": "Bad Request",
  "status": 400,
  "detail": "Password must be at least 8 characters. Password must contain both letters and digits.",
  "code": "WEAK_PASSWORD",
  "violations": [
    { "rule": "MIN_LENGTH", "message": "Password must be at least 8 characters." },
    { "rule": "LETTER_AND_DIGIT", "message": "Password must contain both letters and digits." }
  ]
}
```

## Endpoint summary

| # | Method and path | Auth | Success | Requirement |
|---|---|---|---|---|
| 1 | `POST /auth/register` | public | 201 | FR-ID-01 |
| 2 | `POST /auth/email-verification` | public | 204 | FR-ID-01 |
| 3 | `POST /auth/email-verification/resend` | public | 202 | FR-ID-01 |
| 4 | `POST /auth/login` | public | 200 | FR-ID-02 |
| 5 | `POST /auth/refresh` | public | 200 | FR-ID-02 |
| 6 | `POST /auth/logout` | public | 204 | FR-ID-07 |
| 7 | `POST /auth/password/forgot` | public | 202 | FR-ID-08 |
| 8 | `POST /auth/password/reset` | public | 204 | FR-ID-08 |
| 9 | `GET /users/me` | USER | 200 | FR-ID-03 |
| 10 | `PUT /users/me` | USER | 200 | FR-ID-03 |
| 11 | `POST /users/me/avatar/upload-url` | USER | 200 | FR-ID-03 |
| 12 | `PUT /users/me/avatar` | USER | 200 | FR-ID-03 |
| 13 | `DELETE /users/me/avatar` | USER | 204 | FR-ID-03 |
| 14 | `GET /users/me/location-consent` | USER | 200 | FR-ID-06 |
| 15 | `PUT /users/me/location-consent` | USER | 200 | FR-ID-06 |
| 16 | `DELETE /users/me/location-consent` | USER | 204 | FR-ID-06 |
| 17 | `GET /admin/users` | ADMIN | 200 | FR-ID-04, UC20 |
| 18 | `GET /admin/users/{userId}` | ADMIN | 200 | FR-ID-04, UC20 |
| 19 | `POST /admin/users/{userId}/lock` | ADMIN | 200 | FR-ID-05 |
| 20 | `POST /admin/users/{userId}/unlock` | ADMIN | 200 | FR-ID-05 |

Every path in the table is relative to `/api/v1`. Every authenticated endpoint can also return `401 UNAUTHENTICATED`, and every endpoint can return `400 VALIDATION_FAILED`. The per-endpoint tables below list only the other errors.

## Account lifecycle

```
register --> PENDING --verify email--> ACTIVE
PENDING / ACTIVE --admin lock--> BANNED --admin unlock--> ACTIVE (or PENDING if the email was never verified)
```

Only `ACTIVE` accounts can log in. A password reset also verifies the email, because opening the reset link proves that the user owns the address.

---

## 1. Authentication

### 1.1 `POST /auth/register`

Creates a `PENDING` account and emails a verification link. Tokens are issued only after the email is verified.

Request:

```json
{ "email": "an.nguyen@example.com", "password": "Secret123", "displayName": "An Nguyen" }
```

| Field | Rules |
|---|---|
| `email` | Required, valid email, max 255. Stored trimmed and lowercased |
| `password` | Required, max 200. Password policy: at least 8 characters, at most 72 UTF-8 bytes (BCrypt limit), letters and digits, must not contain the email's local part |
| `displayName` | Required, 2-60 characters, trimmed |

Response `201`:

```json
{ "userId": "1b0c...", "email": "an.nguyen@example.com", "status": "PENDING" }
```

| Status | Code | When |
|---|---|---|
| 400 | `WEAK_PASSWORD` | The password breaks the policy. `violations` lists every rule that failed |
| 409 | `REGISTRATION_REJECTED` | The email is taken. The message is generic and does not say that the account exists |
| 429 | `RATE_LIMITED` | More than `auth.registrations_per_hour_per_ip` registrations from the same IP |

### 1.2 `POST /auth/email-verification`

Request: `{ "token": "<token from the link>" }`. Response `204`. The token is single use and valid for `auth.email_verification_ttl_hours` (24 hours).

| Status | Code | When |
|---|---|---|
| 400 | `INVALID_VERIFICATION_TOKEN` | The token is unknown, already used or expired |

The link has the form `${EMAIL_VERIFICATION_URL}?token=...`, for example `nearby://verify-email?token=...`. The app reads the token from the link and posts it back to this endpoint.

### 1.3 `POST /auth/email-verification/resend`

Request: `{ "email": "an.nguyen@example.com" }`. The response is always `202`, so a caller cannot learn whether an email is registered. A new link is sent only to a `PENDING` account. Each address gets at most `auth.account_emails_per_hour` emails per hour; extra requests are dropped silently.

### 1.4 `POST /auth/login`

Request: `{ "email": "...", "password": "..." }`

Response `200`:

```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "accessTokenExpiresAt": "2026-10-18T03:15:00Z",
  "refreshToken": "q3nV...",
  "refreshTokenExpiresAt": "2026-11-17T03:00:00Z"
}
```

The access token is an HS256 JWT. `sub` is the user id and `roles` holds `USER` or `ADMIN`. It lives 15 minutes (`JWT_ACCESS_TTL`), and the refresh token lives 30 days (`JWT_REFRESH_TTL`). Only the SHA-256 hash of the refresh token is stored.

| Status | Code | When |
|---|---|---|
| 401 | `INVALID_CREDENTIALS` | Unknown email or wrong password. Both cases get the same message and take the same time |
| 403 | `EMAIL_NOT_VERIFIED` | Correct password, but the account is still `PENDING` |
| 403 | `ACCOUNT_LOCKED` | Correct password, but the account is `BANNED` |
| 429 | `RATE_LIMITED` | `auth.login_max_failed_attempts` (5) failures within `auth.login_lockout_minutes` (15). The block holds even for the right password until the window ends. A successful login resets the counter |

### 1.5 `POST /auth/refresh`

Request: `{ "refreshToken": "..." }`. Response `200` with the same body as login. The presented token is revoked, and the new one belongs to the same token chain (family).

| Status | Code | When |
|---|---|---|
| 401 | `INVALID_REFRESH_TOKEN` | Unknown or expired token. The client must log in again |
| 401 | `REFRESH_TOKEN_REUSED` | The token was already rotated or revoked. **The whole chain is revoked**, so every token from that login stops working |
| 403 | `ACCOUNT_LOCKED` | The account was locked |

### 1.6 `POST /auth/logout`

Request: `{ "refreshToken": "..." }`. The response is always `204`. An unknown, expired or already revoked token counts as already logged out.

### 1.7 `POST /auth/password/forgot`

Request: `{ "email": "..." }`. The response is always `202`, even for an unknown email. A reset link (`${PASSWORD_RESET_URL}?token=...`, valid for `auth.password_reset_ttl_minutes`, 30 minutes) is sent only to an account that is not locked. The per-address hourly email quota applies.

### 1.8 `POST /auth/password/reset`

Request: `{ "token": "...", "newPassword": "NewSecret456" }`. Response `204`. The password is changed, every refresh token of the account is revoked, and the email is marked as verified.

| Status | Code | When |
|---|---|---|
| 400 | `INVALID_PASSWORD_RESET_TOKEN` | The token is unknown, already used or expired |
| 400 | `WEAK_PASSWORD` | The new password breaks the policy. The token stays valid, so the user can try another password |

---

## 2. Profile (`/users/me`)

### 2.1 `GET /users/me`

Response `200`:

```json
{
  "id": "1b0c...",
  "email": "an.nguyen@example.com",
  "displayName": "An Nguyen",
  "avatarUrl": "https://storage.example.com/nearby-images/avatars/1b0c.../7f3e....jpg?X-Amz-...",
  "role": "USER",
  "status": "ACTIVE",
  "homeArea": { "latitude": 10.87, "longitude": 106.803 },
  "emailVerified": true,
  "createdAt": "2026-10-18T03:00:00Z"
}
```

`avatarUrl` is a presigned GET URL that is valid for `MINIO_PRESIGN_TTL`. When a field has no value (`avatarUrl`, `homeArea`), it is left out of the response.

### 2.2 `PUT /users/me`

Request:

```json
{ "displayName": "An Nguyen", "homeArea": { "latitude": 10.870149, "longitude": 106.803151 } }
```

Response `200` with the profile. `homeArea` is the default area. It is optional: leave it out or send `null` to clear it. Latitude must be in [-90, 90] and longitude in [-180, 180]. The point is **rounded to 3 decimals** (about 110 m) before it is stored, so a precise home location is never saved.

### 2.3 Avatar upload (two steps, the image never passes through the backend)

**Step 1:** `POST /users/me/avatar/upload-url`

```json
{ "contentType": "image/jpeg", "sizeBytes": 204800 }
```

Response `200`:

```json
{
  "objectKey": "avatars/1b0c.../7f3e....jpg",
  "uploadUrl": "https://storage.example.com/nearby-images/avatars/...?X-Amz-...",
  "method": "PUT",
  "expiresAt": "2026-10-18T03:10:00Z",
  "requiredHeaders": { "Content-Type": "image/jpeg" }
}
```

The client then sends `PUT uploadUrl` with the raw image bytes and every header listed in `requiredHeaders`.

**Step 2:** `PUT /users/me/avatar` with `{ "objectKey": "avatars/1b0c.../7f3e....jpg" }`. Response `200` with the profile. The backend reads the stored object's real size and type, and any previous avatar is deleted after the change is committed.

| Status | Code | When |
|---|---|---|
| 400 | `INVALID_IMAGE` | Step 1 or 2: the type is not JPEG, PNG or WebP, or the size is over `image.max_bytes` (5 MB). In step 2 the uploaded object is deleted |
| 400 | `INVALID_AVATAR_KEY` | Step 2: the key was not issued for the caller's avatar |
| 400 | `AVATAR_NOT_UPLOADED` | Step 2: nothing was uploaded under the key |

### 2.4 `DELETE /users/me/avatar`

Response `204`. The image is deleted from storage.

---

## 3. Location consent (`/users/me/location-consent`)

Consent must be given before the first precise location is collected. Area verification and listings with a precise location check it through the `identity.LocationConsents` facade, which throws `403 LOCATION_CONSENT_REQUIRED` when there is no consent.

### 3.1 `GET`

Response `200`:

```json
{
  "granted": true,
  "termsVersion": "1.0",
  "consentedAt": "2026-10-18T03:00:00Z",
  "currentTermsVersion": "1.0"
}
```

`granted` is true only while the consent is active **and** was given to `currentTermsVersion` (`consent.location_terms_version`). When the terms version is raised, every user must consent again. A withdrawn consent also has `withdrawnAt`.

### 3.2 `PUT`

Request: `{ "termsVersion": "1.0" }`. Response `200` with the consent. The backend records the time and the terms version.

| Status | Code | When |
|---|---|---|
| 400 | `OUTDATED_TERMS_VERSION` | `termsVersion` is not the current version |

### 3.3 `DELETE`

Response `204`. This publishes `LocationConsentWithdrawn`, after which modules stop collecting precise locations. Data already collected is kept and then disposed of per SRS section 4.4. Withdrawing when there is no active consent does nothing.

---

## 4. Admin: users (`/admin/users`, ADMIN only)

A `USER` token gets `403 ACCESS_DENIED`, and the attempt is logged with the user id and path (FR-ID-04). The first admin account is created on startup from `ADMIN_EMAIL` / `ADMIN_PASSWORD`.

### 4.1 `GET /admin/users?query=&status=&cursor=&limit=`

| Param | Meaning |
|---|---|
| `query` | Optional. Case-insensitive substring match on email or display name. `%` and `_` are matched literally |
| `status` | Optional. `PENDING`, `ACTIVE` or `BANNED` |
| `cursor` | Optional. The `nextCursor` value from the previous page |
| `limit` | Optional. Default 20, max 100 |

Results are sorted newest first. Response `200`:

```json
{
  "items": [
    {
      "id": "1b0c...",
      "email": "an.nguyen@example.com",
      "displayName": "An Nguyen",
      "role": "USER",
      "status": "ACTIVE",
      "emailVerified": true,
      "createdAt": "2026-10-18T03:00:00Z",
      "updatedAt": "2026-10-18T03:00:00Z"
    }
  ],
  "nextCursor": "MTc2MDc1NjQwMDAwMHwxYjBj...",
  "hasMore": true
}
```

| Status | Code | When |
|---|---|---|
| 400 | `INVALID_CURSOR` | The cursor is malformed |

### 4.2 `GET /admin/users/{userId}`

Response `200` with one item in the format above.

| Status | Code | When |
|---|---|---|
| 404 | `USER_NOT_FOUND` | No account has that id |

### 4.3 `POST /admin/users/{userId}/lock`

Request: `{ "reason": "Repeated scam reports" }`. The reason is required, max 200 characters. Response `200` with the user, whose `status` is now `BANNED`.

In one transaction, the backend:

1. sets the account to `BANNED`, so it cannot log in or refresh;
2. revokes every refresh token of the account;
3. writes `audit_log` (`USER_LOCKED`, actor, target, reason). If that write fails, the lock is rejected (FR-MD-04);
4. publishes `UserBanned` through the outbox.

After the commit, access tokens issued before the lock are rejected right away (`401`), not only when they expire.

| Status | Code | When |
|---|---|---|
| 403 | `CANNOT_LOCK_OWN_ACCOUNT` | The admin tries to lock their own account |
| 403 | `CANNOT_LOCK_ADMIN` | The target is another admin |
| 404 | `USER_NOT_FOUND` | No account has that id |
| 409 | `ACCOUNT_ALREADY_LOCKED` | The account is already locked |
| 409 | `CONCURRENT_MODIFICATION` | Another admin changed the same account at the same time |

### 4.4 `POST /admin/users/{userId}/unlock`

Request: `{ "reason": "Appeal accepted" }`. Response `200`. The status goes back to `ACTIVE`, or to `PENDING` if the email was never verified. The backend writes `audit_log` (`USER_UNLOCKED`) and publishes `UserUnbanned`.

| Status | Code | When |
|---|---|---|
| 404 | `USER_NOT_FOUND` | No account has that id |
| 409 | `ACCOUNT_NOT_LOCKED` | The account is not locked |

---

## Domain events (module public API)

| Event | Published when | Expected consumers |
|---|---|---|
| `identity.UserBanned(userId, lockedAt)` | An admin locks an account | Listing hides the user's listings. Transaction cancels open transactions without penalizing the other party (FR-ID-05) |
| `identity.UserUnbanned(userId, unlockedAt)` | An admin unlocks an account | Listing may show the hidden listings again |
| `identity.LocationConsentWithdrawn(userId, withdrawnAt)` | The user withdraws location consent | Area Verification stops collecting locations |

These events go through the `event_publication` outbox, so consumers must be idempotent. The Listing and Transaction consumers belong to those modules and are not part of this change.

## Runtime parameters (`app_parameters`)

| Key | Default | Used by |
|---|---|---|
| `auth.login_max_failed_attempts` | 5 | Login lockout |
| `auth.login_lockout_minutes` | 15 | Login lockout window |
| `auth.registrations_per_hour_per_ip` | 10 | Registration rate limit |
| `auth.account_emails_per_hour` | 5 | Verification and reset emails per address |
| `auth.email_verification_ttl_hours` | 24 | Verification link lifetime |
| `auth.password_reset_ttl_minutes` | 30 | Reset link lifetime |
| `consent.location_terms_version` | 1.0 | Current consent terms |
| `image.max_bytes` | 5242880 | Avatar size limit |
