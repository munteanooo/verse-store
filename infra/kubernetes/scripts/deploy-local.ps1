[CmdletBinding()]
param(
    [string]$ClusterName = "verse-local",
    [string]$Namespace = "verse-dev",
    [string]$ExistingSecret = "verse-store-secrets",
    [string]$ImageRepository = "ghcr.io/munteanooo/verse-store",
    [string]$ImageTag = "",
    [ValidateSet("Always", "IfNotPresent", "Never")][string]$ImagePullPolicy = "IfNotPresent",
    [string]$ValuesFile = (Join-Path $PSScriptRoot "..\helm\verse-store\values-local.yaml.example")
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

foreach ($command in @("kubectl", "helm", "git")) {
    if (-not (Get-Command $command -ErrorAction SilentlyContinue)) { throw "$command is required." }
}
if ((kubectl config current-context) -ne "kind-$ClusterName") { throw "Refusing to deploy outside kind-$ClusterName." }
kubectl -n ingress-nginx wait --for=condition=Available deployment/ingress-nginx-controller --timeout=5m *> $null
if ($LASTEXITCODE -ne 0) { throw "ingress-nginx is not Ready. Run install-ingress-controller.ps1 first." }
kubectl -n $Namespace get secret $ExistingSecret *> $null
if ($LASTEXITCODE -ne 0) {
    throw "Required Secret '$ExistingSecret' does not exist in namespace '$Namespace'. Run create-local-secret.ps1 first."
}
kubectl -n $Namespace get secret verse-local-tls *> $null
if ($LASTEXITCODE -ne 0) { throw "TLS Secret 'verse-local-tls' is missing. Run create-local-tls.ps1 first." }
if ([string]::IsNullOrWhiteSpace($ImageTag)) { $ImageTag = "sha-$(git rev-parse HEAD)" }
if ($ImageTag -eq "latest") { throw "The latest tag is forbidden." }
$chart = Join-Path $PSScriptRoot "..\helm\verse-store"
helm upgrade --install verse-store $chart --namespace $Namespace --create-namespace `
    --values $ValuesFile --set-string "existingSecret=$ExistingSecret" `
    --set-string "image.repository=$ImageRepository" --set-string "image.tag=$ImageTag" `
    --set-string "image.pullPolicy=$ImagePullPolicy" --wait --timeout 10m
kubectl -n $Namespace rollout status deployment/verse --timeout=5m
& (Join-Path $PSScriptRoot "reconcile-keycloak-client.ps1") -ClusterName $ClusterName -Namespace $Namespace
Write-Host "Verse Store deployed with immutable tag '$ImageTag'."
