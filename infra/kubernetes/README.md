# Verse Store on local Kubernetes

Metrics, dashboards, alerts, and the separate monitoring namespace are documented in [the observability runbook](../observability/README.md).

This directory provides a learning-focused Helm deployment for a dedicated kind cluster. Docker Compose remains the fastest local development path and is not replaced.

## Prerequisites

- Docker Desktop with Linux containers
- `kubectl`
- Helm 3
- kind
- PowerShell 7 or Windows PowerShell 5.1
- `mkcert` with its local CA installed
- a public immutable application image in `ghcr.io/munteanooo/verse-store`

Recommended Windows installation commands (run them yourself after reviewing the packages):

```powershell
winget install --id Helm.Helm
winget install --id Kubernetes.kind
winget install --id FiloSottile.mkcert
mkcert -install
```

Verify with `docker version`, `kubectl version --client`, `helm version`, and `kind version`.

`mkcert -install` changes the Windows trust store and may request Administrator approval. No private key or generated certificate is stored in this repository.

## Local DNS

Add these entries to `C:\Windows\System32\drivers\etc\hosts` from an elevated editor. The scripts deliberately do not modify this system file:

```text
127.0.0.1 verse.local
127.0.0.1 auth.verse.local
127.0.0.1 mail.verse.local
```

Verify without changing hosts:

```powershell
.\infra\kubernetes\scripts\verify-local-dns.ps1
```

## Local deployment

The scripts refuse to operate on any context other than `kind-verse-local`.

```powershell
.\infra\kubernetes\scripts\create-kind-cluster.ps1
Copy-Item .env.example .env # replace every change-me value locally; never commit .env
.\infra\kubernetes\scripts\create-local-secret.ps1
.\infra\kubernetes\scripts\install-ingress-controller.ps1
.\infra\kubernetes\scripts\create-local-tls.ps1
.\infra\kubernetes\scripts\deploy-local.ps1 -ImageTag sha-<published-commit-sha>
.\infra\kubernetes\scripts\verify-local.ps1
```

New clusters map host ports 80/443 to the control-plane node and label it `ingress-ready=true`. An existing cluster created without those mappings must be backed up and recreated; never delete it merely to apply this checkpoint. Back up both PostgreSQL databases with `pg_dump` and mirror the MinIO bucket before requesting destructive approval. Inventory PVCs with `kubectl get pvc -n verse-dev` and verify each backup before recreation.

The controller is pinned to official ingress-nginx `controller-v1.15.1`. The upstream project was retired in March 2026, so this pinned controller is limited to the isolated local learning cluster and should not be selected for a new production platform. The official artifacts remain available, but no future security maintenance is expected.

`create-local-secret.ps1` reads `.env`, validates every required credential, and pipes a client-side Secret directly to `kubectl apply`. It neither prints credential values nor writes Secret YAML. The chart requires `existingSecret`; it never renders credentials.

Before deployment, validate the chart:

```powershell
helm lint .\infra\kubernetes\helm\verse-store -f .\infra\kubernetes\helm\verse-store\values-local.yaml.example
helm template verse-store .\infra\kubernetes\helm\verse-store -n verse-dev -f .\infra\kubernetes\helm\verse-store\values-local.yaml.example --set-string image.tag=sha-<published-commit-sha>
```

If `kubeconform` is installed, pipe rendered manifests to `kubeconform -strict -summary`; it is optional and is not installed by these scripts.

## Access and OIDC

- Storefront: `https://verse.local`
- Keycloak and Account Console: `https://auth.verse.local`
- Mailpit (local values only): `https://mail.verse.local`

HTTP redirects to HTTPS. ingress-nginx sends forwarded headers and Spring uses the framework forwarding strategy, so the callback is `https://verse.local/login/oauth2/code/verse-store`. Browser authorization, registration, account management and logout use the public Keycloak hostname. Token, UserInfo and JWK requests remain internal over the ClusterIP Service; JWT and ID-token validation still require the exact public issuer `https://auth.verse.local/realms/verse`. State, nonce, PKCE, CSRF and TLS validation remain enabled.

The realm JSON bootstraps new databases. For a persistent realm, `deploy-local.ps1` runs `reconcile-keycloak-client.ps1`. The idempotent reconciliation enforces registration, email, password-recovery and password-policy settings; ensures the `CUSTOMER` and `ADMIN` roles and `/customers` default group; keeps `VERIFY_EMAIL` non-default; and reconciles the existing `verse-store` HTTPS URLs. Missing roles, group or client are created, but existing users, passwords, role assignments and unrelated default groups are never removed.

Registering creates a user in `/customers`, which grants `CUSTOMER`. Assign `ADMIN` manually in the Keycloak admin console; never make it a default role. The standard `roles` client scope emits `realm_access.roles` to access tokens, ID tokens, and UserInfo. Email verification is disabled for local learning only.

## Architecture and persistence

- Spring Boot: Deployment and ClusterIP Service, one replica initially
- Keycloak: Deployment and ClusterIP Service
- Mailpit: optional Deployment and ClusterIP Service (`mailpit.enabled=true` locally)
- application PostgreSQL: single-replica StatefulSet, headless Service, dedicated PVC
- Keycloak PostgreSQL: separate single-replica StatefulSet, headless Service, dedicated PVC
- MinIO: single-replica StatefulSet, ClusterIP Service, dedicated PVC

Flyway remains the sole schema owner and applies V1/V2; Hibernate stays on `ddl-auto=validate`. Restarting an app pod does not recreate migrations. The S3 adapter creates the configured bucket idempotently on first use, and stable `/media/products/{key}` URLs remain application-owned.

Keycloak realm import is bootstrap-only. On an empty Keycloak database, it creates realm `verse`, roles, `/customers`, the OIDC client, and local SMTP configuration. Once the realm exists in its persistent database, restarting Keycloak does not replace it or its users. Updating the ConfigMap is not a persistent-realm migration; use Keycloak administration/export procedures for later changes.

### Keycloak PostgreSQL backup and restore

Store backups outside the repository. The following creates a custom-format dump without printing the database password:

```powershell
$backup = Join-Path $env:TEMP "verse-keycloak-$(Get-Date -Format yyyyMMdd-HHmmss).dump"
kubectl -n verse-dev exec verse-keycloak-postgres-0 -- sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc -f /tmp/keycloak.dump'
kubectl -n verse-dev cp verse-keycloak-postgres-0:/tmp/keycloak.dump $backup
kubectl -n verse-dev exec verse-keycloak-postgres-0 -- rm -f /tmp/keycloak.dump
Get-Item -LiteralPath $backup
```

Verify the dump before relying on it:

```powershell
kubectl -n verse-dev cp $backup verse-keycloak-postgres-0:/tmp/keycloak-verify.dump
kubectl -n verse-dev exec verse-keycloak-postgres-0 -- pg_restore --list /tmp/keycloak-verify.dump
kubectl -n verse-dev exec verse-keycloak-postgres-0 -- rm -f /tmp/keycloak-verify.dump
```

Restore is a maintenance operation that replaces database state and therefore requires separate explicit approval. After taking and verifying a fresh backup, run the following with `$backup` pointing to the selected dump:

```powershell
kubectl -n verse-dev scale deployment/verse-keycloak --replicas=0
kubectl -n verse-dev rollout status deployment/verse-keycloak --timeout=5m
kubectl -n verse-dev cp $backup verse-keycloak-postgres-0:/tmp/keycloak-restore.dump
kubectl -n verse-dev exec verse-keycloak-postgres-0 -- sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists /tmp/keycloak-restore.dump'
kubectl -n verse-dev exec verse-keycloak-postgres-0 -- rm -f /tmp/keycloak-restore.dump
kubectl -n verse-dev scale deployment/verse-keycloak --replicas=1
kubectl -n verse-dev rollout status deployment/verse-keycloak --timeout=5m
.\infra\kubernetes\scripts\reconcile-keycloak-client.ps1
```

Never restore while Keycloak is writing and never delete the PVC as part of restore. These commands are documentation only; deployment and reconciliation scripts never perform a restore.

Recreating pods preserves all three PVC-backed data stores. Deleting the kind cluster destroys its local Kubernetes volumes and all contained data.

## Security

The app runs as UID/GID 10001, without a service-account token, with `RuntimeDefault` seccomp, all capabilities dropped, privilege escalation disabled, a read-only root filesystem, and an `emptyDir` for `/tmp`. Stateful services run non-root with compatible image UIDs and dedicated volumes. Databases and the MinIO console are never exposed externally by default.

NetworkPolicy templates implement default deny, DNS, ingress-controller access, app-to-database/Keycloak/MinIO, and Keycloak-to-database/Mailpit paths. They are disabled in local values because kind's default CNI does not enforce NetworkPolicy. Enable only after installing and validating a capable CNI. PostgreSQL, Keycloak PostgreSQL and MinIO remain ClusterIP-only and have no Ingress.

Ingress accepts 6 MB requests to accommodate the validated 5 MB upload limit. It does not add wildcard CORS or long-lived HSTS, avoiding sticky browser behavior on local domains.

## Operations

```powershell
helm upgrade verse-store .\infra\kubernetes\helm\verse-store -n verse-dev -f .\infra\kubernetes\helm\verse-store\values-local.yaml.example --set-string image.tag=sha-<new-sha> --wait
helm history verse-store -n verse-dev
helm rollback verse-store <revision> -n verse-dev --wait
kubectl logs -n verse-dev deployment/verse
kubectl get events -n verse-dev --sort-by=.lastTimestamp
kubectl describe pod -n verse-dev <pod>
```

To remove only Ingress/TLS resources while preserving workloads and PVCs:

```powershell
.\infra\kubernetes\scripts\cleanup-local-ingress.ps1
```

For destructive whole-cluster cleanup, the exact cluster name is mandatory:

```powershell
.\infra\kubernetes\scripts\destroy-local.ps1 -ConfirmClusterName verse-local
```

This deletes only `verse-local`, but also irreversibly deletes its local Kubernetes volumes. It does not touch Docker Compose volumes.

## Manual verification checklist

After automated readiness checks, verify the trusted certificate, HTTP redirect, register, CUSTOMER login, Profile and Account Console, ADMIN login, Control Plane, product create/edit/publish, 5 MB image upload and Catalog rendering, logout at Spring and Keycloak, Mailpit HTTPS, and switching users. Confirm PostgreSQL and MinIO have no external Service or Ingress. Delete only pods (not PVCs), wait for recreation, then confirm products, users, and uploaded images remain.

Troubleshooting: certificate warnings mean the mkcert CA is not trusted for the current Windows/browser profile; connection refusal on 80/443 usually means the kind cluster lacks the required port mappings; an OIDC issuer mismatch means public and internal URLs were incorrectly conflated; a stale redirect URI on an existing realm should be repaired by rerunning the idempotent reconciliation script.

## Production direction

This single-node stateful layout is not a production database recommendation. Prefer managed PostgreSQL for both databases and managed S3/R2/Spaces-compatible object storage. Configure the S3 endpoint and credentials through values/Secrets without changing Product domain code. A later checkpoint should add a real ingress controller, DNS, TLS certificates, external secret management, backups, multi-replica readiness, autoscaling, and a CNI-enforced NetworkPolicy model.
