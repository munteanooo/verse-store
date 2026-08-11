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
