[CmdletBinding()]
param(
    [string]$ClusterName = "verse-local",
    [string]$Namespace = "verse-dev",
    [string]$SecretName = "verse-store-secrets",
    [string]$EnvFile = (Join-Path $PSScriptRoot "..\..\..\.env")
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ((kubectl config current-context) -ne "kind-$ClusterName") { throw "Refusing to modify a context other than kind-$ClusterName." }
if (-not (Test-Path -LiteralPath $EnvFile)) { throw "Local .env file not found." }
$values = @{}
foreach ($line in Get-Content -LiteralPath $EnvFile) {
    if ($line -match '^\s*#' -or [string]::IsNullOrWhiteSpace($line)) { continue }
    $parts = $line -split '=', 2
    if ($parts.Count -eq 2) { $values[$parts[0].Trim()] = $parts[1] }
}
$required = @("POSTGRES_PASSWORD", "KEYCLOAK_DB_PASSWORD", "KEYCLOAK_BOOTSTRAP_ADMIN_PASSWORD", "KEYCLOAK_CLIENT_SECRET", "MINIO_ROOT_USER", "MINIO_ROOT_PASSWORD")
foreach ($name in $required) {
    if (-not $values.ContainsKey($name) -or [string]::IsNullOrWhiteSpace($values[$name])) { throw "Required value $name is missing from .env." }
}
kubectl get namespace $Namespace *> $null
if ($LASTEXITCODE -ne 0) { kubectl create namespace $Namespace *> $null }
kubectl -n $Namespace create secret generic $SecretName `
    "--from-literal=postgres-password=$($values.POSTGRES_PASSWORD)" `
    "--from-literal=keycloak-db-password=$($values.KEYCLOAK_DB_PASSWORD)" `
    "--from-literal=keycloak-bootstrap-admin-password=$($values.KEYCLOAK_BOOTSTRAP_ADMIN_PASSWORD)" `
    "--from-literal=keycloak-client-secret=$($values.KEYCLOAK_CLIENT_SECRET)" `
    "--from-literal=minio-root-user=$($values.MINIO_ROOT_USER)" `
    "--from-literal=minio-root-password=$($values.MINIO_ROOT_PASSWORD)" `
    --dry-run=client -o yaml | kubectl apply -f - *> $null
Write-Host "Secret '$SecretName' was created or updated in '$Namespace' (values hidden)."

