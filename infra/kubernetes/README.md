# Verse Store on local Kubernetes

This directory provides a learning-focused Helm deployment for a dedicated kind cluster. Docker Compose remains the fastest local development path and is not replaced.

## Prerequisites

- Docker Desktop with Linux containers
- `kubectl`
- Helm 3
- kind
- PowerShell 7 or Windows PowerShell 5.1
- a public immutable application image in `ghcr.io/munteanooo/verse-store`

Recommended Windows installation commands (run them yourself after reviewing the packages):

```powershell
winget install --id Helm.Helm
winget install --id Kubernetes.kind
```

Verify with `docker version`, `kubectl version --client`, `helm version`, and `kind version`.

## Local deployment

The scripts refuse to operate on any context other than `kind-verse-local`.

```powershell
.\infra\kubernetes\scripts\create-kind-cluster.ps1
Copy-Item .env.example .env # replace every change-me value locally; never commit .env
.\infra\kubernetes\scripts\create-local-secret.ps1
.\infra\kubernetes\scripts\deploy-local.ps1 -ImageTag sha-<published-commit-sha>
.\infra\kubernetes\scripts\verify-local.ps1
```

`create-local-secret.ps1` reads `.env`, validates every required credential, and pipes a client-side Secret directly to `kubectl apply`. It neither prints credential values nor writes Secret YAML. The chart requires `existingSecret`; it never renders credentials.

Before deployment, validate the chart:

```powershell
helm lint .\infra\kubernetes\helm\verse-store -f .\infra\kubernetes\helm\verse-store\values-local.yaml.example
helm template verse-store .\infra\kubernetes\helm\verse-store -n verse-dev -f .\infra\kubernetes\helm\verse-store\values-local.yaml.example --set-string image.tag=sha-<published-commit-sha>
```

If `kubeconform` is installed, pipe rendered manifests to `kubeconform -strict -summary`; it is optional and is not installed by these scripts.

## Access and OIDC

Run these in separate terminals:

```powershell
kubectl -n verse-dev port-forward svc/verse 8080:8080
kubectl -n verse-dev port-forward svc/verse-keycloak 8180:8080
kubectl -n verse-dev port-forward svc/verse-mailpit 8025:8025
```

The browser-facing OIDC endpoints use `http://localhost:8180`; token, UserInfo, and JWK calls use the internal Keycloak Service. `--hostname-backchannel-dynamic=true` permits that split without disabling issuer validation. App callbacks and logout return to `http://localhost:8080`.

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

Recreating pods preserves all three PVC-backed data stores. Deleting the kind cluster destroys its local Kubernetes volumes and all contained data.

## Security

The app runs as UID/GID 10001, without a service-account token, with `RuntimeDefault` seccomp, all capabilities dropped, privilege escalation disabled, a read-only root filesystem, and an `emptyDir` for `/tmp`. Stateful services run non-root with compatible image UIDs and dedicated volumes. Databases and the MinIO console are never exposed externally by default.

NetworkPolicy templates implement default deny, DNS, app-to-database/Keycloak/MinIO, and Keycloak-to-database/Mailpit paths. They are disabled in local values because kind's default CNI does not enforce NetworkPolicy. Enable only after installing and validating a capable CNI.

## Operations

```powershell
helm upgrade verse-store .\infra\kubernetes\helm\verse-store -n verse-dev -f .\infra\kubernetes\helm\verse-store\values-local.yaml.example --set-string image.tag=sha-<new-sha> --wait
helm history verse-store -n verse-dev
helm rollback verse-store <revision> -n verse-dev --wait
kubectl logs -n verse-dev deployment/verse
kubectl get events -n verse-dev --sort-by=.lastTimestamp
kubectl describe pod -n verse-dev <pod>
```

For safe cleanup, the exact cluster name is mandatory:

```powershell
.\infra\kubernetes\scripts\destroy-local.ps1 -ConfirmClusterName verse-local
```

This deletes only `verse-local`, but also irreversibly deletes its local Kubernetes volumes. It does not touch Docker Compose volumes.

## Manual verification checklist

After automated readiness checks, verify landing, register, CUSTOMER login, Profile, ADMIN login, Control Plane, product create/edit/publish, image upload and serving, logout, and switching users. Delete only pods (not PVCs), wait for recreation, then confirm products, users, and uploaded images remain.

## Production direction

This single-node stateful layout is not a production database recommendation. Prefer managed PostgreSQL for both databases and managed S3/R2/Spaces-compatible object storage. Configure the S3 endpoint and credentials through values/Secrets without changing Product domain code. A later checkpoint should add a real ingress controller, DNS, TLS certificates, external secret management, backups, multi-replica readiness, autoscaling, and a CNI-enforced NetworkPolicy model.

