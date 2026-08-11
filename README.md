# Verse Store

Minimalist Spring Boot product catalog with PostgreSQL and Flyway.

## Run with Docker Compose

Prerequisite: Docker Desktop with Docker Compose.

Create the local environment file in PowerShell and replace the example password:

```powershell
Copy-Item .env.example .env
```

Build and start the application:

```powershell
docker compose build
docker compose up -d
docker compose ps
```

The catalog is available at `http://localhost:8080/catalog`. Follow application logs with:

```powershell
docker compose logs -f app
```

Stop the stack while preserving PostgreSQL data:

```powershell
docker compose down
```

To explicitly reset all local data, including the PostgreSQL volume:

```powershell
docker compose down -v
```

Warning: `docker compose down -v` permanently deletes the local database volume.

## CI/CD and container security

Pull requests targeting `dev` or `main` run the complete Maven verification suite, including PostgreSQL Testcontainers tests. CI also validates Docker Compose, checks that the database password remains mandatory, scans the repository with Trivy, builds the runtime image, verifies its non-root user, and performs a blocking image scan.

Pushes to `dev` and `main` run the same verification before publishing to GitHub Container Registry. Version tags matching `v*` also trigger publication after semantic-version validation. Images are available at:

```text
ghcr.io/munteanooo/verse-store
```

Pull a specific published tag with:

```powershell
docker pull ghcr.io/munteanooo/verse-store:<tag>
```

The `dev` branch updates the `dev` image tag but never `latest`. The `main` branch updates `main` and `latest`; release tags publish their semantic version. The package may be private by default, depending on repository and package visibility.

GitHub Actions authenticates to GHCR with the workflow-scoped `GITHUB_TOKEN`; no manually managed publishing token is required. Trivy blocks remediable HIGH and CRITICAL image vulnerabilities. Each verified image also produces an SPDX JSON software bill of materials (SBOM), uploaded as a short-lived workflow artifact so the components in the image can be audited without committing generated files.

Recommended repository settings:

- Protect `main` and require changes through pull requests.
- Require the CI checks before merge and disable force-pushes to `main`.
- Require at least one approval when the project becomes collaborative.
- Enable GitHub secret scanning, Dependabot alerts, and dependency security updates.

## Local identity and access management

The local stack uses Keycloak 26.7.1 with a dedicated PostgreSQL database and Mailpit for email verification. Copy `.env.example` to the ignored `.env` file and replace every `change-me` value with a strong local secret before starting the stack.

- Keycloak: `http://localhost:8180`
- Mailpit: `http://localhost:8025`
- Verse Store: `http://localhost:8080`

The `verse` realm enables public registration, verified email, password recovery, brute-force protection, and the Keycloak Account Console. New registrations join the `customers` default group and receive the `CUSTOMER` realm role. `ADMIN` is never granted by registration or imported users; assign it manually from the Keycloak Admin Console to a local test user with a verified email.

Catalog pages and APIs remain public. `/profile` requires `CUSTOMER` or `ADMIN`; `/admin/**` and `/api/admin/**` require `ADMIN`. UI login uses the OIDC authorization-code flow, logout terminates the Keycloak session, and profile changes, password changes, and account deletion stay inside the Keycloak Account Console. CSRF remains enabled for session-backed UI and administration requests.

The client secret is injected into the realm import through `${KEYCLOAK_CLIENT_SECRET}`. Never replace this placeholder with a real secret in Git. Startup import skips an existing realm, so remove the dedicated Keycloak data volume only when intentionally testing a fresh import.

Mailpit is a development-only SMTP capture service and must not be used as a production mail server.
