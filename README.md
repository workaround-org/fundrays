<p align="center">
  <img src="logo-readme.svg" alt="fundrays" width="340">
</p>

<p align="center">
  <a href="https://github.com/workaround-org/fundrays/actions/workflows/native-image.yml">
    <img src="https://github.com/workaround-org/fundrays/actions/workflows/native-image.yml/badge.svg" alt="Native Image CI">
  </a>
  <a href="https://github.com/workaround-org/fundrays/pkgs/container/fundrays">
    <img src="https://img.shields.io/badge/container-ghcr.io-ed2a91?logo=docker&logoColor=white" alt="Container">
  </a>
  <img src="https://img.shields.io/badge/Quarkus-3.36.1-4695EB?logo=quarkus&logoColor=white" alt="Quarkus">
  <img src="https://img.shields.io/badge/Java-25-ed2a91?logo=openjdk&logoColor=white" alt="Java 25">
  <img src="https://img.shields.io/badge/native-GraalVM-ed2a91" alt="GraalVM native">
  <img src="https://img.shields.io/github/license/workaround-org/fundrays?color=ed2a91" alt="MIT License">
</p>

<p align="center">A lightweight donation management tool — create campaigns with goal amounts, share them via QR code and link, and track contributions in real time.</p>

---

## Tech stack

- **Quarkus 3.35.4** (Java 25, native image via GraalVM)
- **quarkus-renarde** — server-side MVC admin UI (Qute templates)
- **quarkus-hibernate-orm-panache** + **quarkus-jdbc-postgresql** — persistence
- **quarkus-security-jpa** — form-based admin authentication (BCrypt)
- **quarkus-rest** + **quarkus-rest-jackson** — typesafe REST API
- **quarkus-smallrye-openapi** — OpenAPI / Swagger UI
- **quarkus-mailer** — donor confirmation + admin notification mails (SMTP, Qute mail templates)

## Local setup

Copy the example env file and fill in your credentials:

```bash
cp .env.example .env
```

Edit `.env` and paste your [Mollie test API key](https://my.mollie.com/dashboard/developers/api-keys). The file is gitignored.

## Running in dev mode

Requires Docker (Quarkus Dev Services starts a PostgreSQL container automatically).

```bash
./mvnw quarkus:dev
```

- Admin UI: http://localhost:8080/admin  
- Public donation page: http://localhost:8080/donate/{slug}  
- Swagger UI: http://localhost:8080/q/swagger-ui  
- Dev credentials: `admin` / `admin123`

Mollie is enabled automatically in dev mode when `FUNDRAYS_MOLLIE_API_KEY` is present in `.env`.

## Running tests

```bash
./mvnw test
```

Unit tests mock the Mollie gateway — no API key needed. Three additional live tests
(`MolliePaymentLiveTest`) hit the real Mollie test API and are skipped unless a real key is configured in `.env`.

## Mollie payment gateway

Payments are processed via [Mollie](https://mollie.com). Mollie's hosted checkout supports all major payment methods.

| Variable | Description |
|----------|-------------|
| `FUNDRAYS_MOLLIE_API_KEY` | Mollie API key (`test_...` for test mode, `live_...` for production) |
| `FUNDRAYS_MOLLIE_ENABLED` | Set to `true` to enable payments (dev mode sets this automatically) |
| `FUNDRAYS_BASE_URL` | Public base URL of the app, must end with `/` (defaults to `http://localhost:8080/`) |

Mollie sends payment status updates to `POST /webhooks/mollie` as a form-encoded body containing the payment ID. The application then fetches the payment status from the Mollie API to confirm or fail the donation.

## Building a native image

```bash
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

The resulting binary is at `target/*-runner`. The Docker image is built from `src/main/docker/Dockerfile.native`.

## Deployment

On every push to `main`, CI builds the native image and publishes it to the GitHub Container Registry as `ghcr.io/workaround-org/fundrays:latest` (release tags `fundrays-X.Y.Z` also produce `X`, `X.Y`, and `X.Y.Z` image tags).

### Configuration via environment variables

Any Quarkus property can be set with an environment variable using the standard
[MicroProfile Config mapping](https://quarkus.io/guides/config-reference#environment-variables):
uppercase the property name and replace every non-alphanumeric character with `_`.
For example `quarkus.datasource.jdbc.url` → `QUARKUS_DATASOURCE_JDBC_URL`. This is how
the database connection is provided in production (there is no datasource baked into
`application.properties` — dev/test use Dev Services, prod is configured entirely via env).

| Variable | Property | Description |
|----------|----------|-------------|
| `QUARKUS_DATASOURCE_JDBC_URL` | `quarkus.datasource.jdbc.url` | **Required.** e.g. `jdbc:postgresql://db:5432/fundrays` |
| `QUARKUS_DATASOURCE_USERNAME` | `quarkus.datasource.username` | **Required.** Database user |
| `QUARKUS_DATASOURCE_PASSWORD` | `quarkus.datasource.password` | **Required.** Database password |
| `FUNDRAYS_ADMIN_USERNAME` | `fundrays.admin.username` | **Required.** Admin login created/updated on startup |
| `FUNDRAYS_ADMIN_PASSWORD` | `fundrays.admin.password` | **Required.** Admin password (BCrypt-hashed at boot; change it here to reset) |
| `FUNDRAYS_BASE_URL` | `fundrays.base-url` | Public base URL, must end with `/` (used for og:image and Mollie webhooks) |
| `FUNDRAYS_MOLLIE_ENABLED` | `fundrays.mollie.enabled` | Set to `true` to enable payments |
| `FUNDRAYS_MOLLIE_API_KEY` | `fundrays.mollie.api-key` | Mollie `live_...` key (leading/trailing whitespace is trimmed) |
| `FUNDRAYS_FORM_ENCRYPTION_KEY` | `quarkus.http.auth.form.encryption-key` | **Recommended in prod.** Stable secret (≥16 chars) for FORM auth sessions; without it admins are logged out on every restart and sessions break across instances |

On startup Flyway migrates the schema automatically (`db/migration`); the demo seed
data is **dev-only** and is never loaded in production. The admin account is created
from `FUNDRAYS_ADMIN_USERNAME`/`FUNDRAYS_ADMIN_PASSWORD` the first time the app boots
against a fresh database — there are no default production credentials.

### Example

```bash
docker run -p 8080:8080 \
  -e QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://db:5432/fundrays \
  -e QUARKUS_DATASOURCE_USERNAME=fundrays \
  -e QUARKUS_DATASOURCE_PASSWORD=... \
  -e FUNDRAYS_ADMIN_USERNAME=admin \
  -e FUNDRAYS_ADMIN_PASSWORD=... \
  -e FUNDRAYS_BASE_URL=https://fundrays.example.org/ \
  -e FUNDRAYS_MOLLIE_ENABLED=true \
  -e FUNDRAYS_MOLLIE_API_KEY=live_... \
  -e FUNDRAYS_FORM_ENCRYPTION_KEY=... \
  ghcr.io/workaround-org/fundrays:latest
```

## REST API

**Public (unauthenticated)**

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/public/campaigns/{slug}` | Campaign data for the donation page; 404 unless status is ACTIVE |
| `GET` | `/api/public/campaigns/{slug}/progress` | Polling-friendly aggregate: raised amount (cents), goal, percentage, donation count, recent public donor messages |
| `POST` | `/api/public/campaigns/{slug}/donate` | Initiate a donation; minimum 500 cents (5,00 €); returns `paymentUrl` |
| `GET` | `/api/campaigns` | List all active campaigns |
| `GET` | `/api/campaigns/{slug}` | Get a single campaign |
| `GET` | `/d/{slug}` | Short alias — 302 redirect to `/donate/{slug}` |

**Admin (authentication required)**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/campaigns` | Create a campaign |
| `PATCH` | `/api/campaigns/{slug}` | Update a campaign |
| `GET` | `/api/campaigns/{slug}/qrcode` | QR code for the donation URL; `?format=png` (default) or `?format=svg`; optional `utm_*` params appended to encoded URL |
| `GET` | `/api/donations` | List donations |

## CI / CD

Every push to `main` builds a native image and pushes it to `ghcr.io` as `:latest`. Release tags matching `fundrays-X.Y.Z` additionally push versioned tags. Dependabot keeps Maven dependencies, Docker base images, and GitHub Actions up to date.
