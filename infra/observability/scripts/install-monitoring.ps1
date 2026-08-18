[CmdletBinding()]
param(
    [string]$ClusterName = "verse-local",
    [string]$Namespace = "monitoring",
    [string]$ReleaseName = "monitoring"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$ChartVersion = "88.1.3"
$Chart = "oci://ghcr.io/prometheus-community/charts/kube-prometheus-stack"

foreach ($command in @("kubectl", "helm")) {
    if (-not (Get-Command $command -CommandType Application -ErrorAction SilentlyContinue)) { throw "$command is required." }
}
if ((kubectl config current-context) -ne "kind-$ClusterName") { throw "Refusing to install monitoring outside kind-$ClusterName." }

kubectl create namespace $Namespace --dry-run=client -o yaml | kubectl apply -f - *> $null
if ($LASTEXITCODE -ne 0) { throw "Failed to ensure namespace '$Namespace'." }

$existingGrafanaSecret = kubectl -n $Namespace get secret monitoring-grafana-admin --ignore-not-found -o name
if ($LASTEXITCODE -ne 0) { throw "Failed to inspect the Grafana admin Secret." }
if ([string]::IsNullOrWhiteSpace($existingGrafanaSecret)) {
    $bytes = New-Object byte[] 32
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($bytes) } finally { $generator.Dispose() }
    $password = [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
    $userBase64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("admin"))
    $passwordBase64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($password))
    @"
apiVersion: v1
kind: Secret
metadata:
  name: monitoring-grafana-admin
  namespace: $Namespace
type: Opaque
data:
  admin-user: $userBase64
  admin-password: $passwordBase64
"@ | kubectl apply -f - *> $null
    if ($LASTEXITCODE -ne 0) { throw "Failed to create Grafana admin Secret." }
    Remove-Variable password,passwordBase64 -ErrorAction SilentlyContinue
}

$values = Join-Path $PSScriptRoot "..\kubernetes\kube-prometheus-stack-values.yaml"
helm upgrade --install $ReleaseName $Chart --version $ChartVersion --namespace $Namespace `
    --values $values --wait --timeout 15m
if ($LASTEXITCODE -ne 0) { throw "Monitoring Helm installation failed." }

$dashboards = Join-Path $PSScriptRoot "..\grafana\dashboards"
Get-ChildItem -LiteralPath $dashboards -Filter "*.json" | ForEach-Object {
    $name = "grafana-dashboard-$($_.BaseName)"
    kubectl -n $Namespace create configmap $name --from-file=$($_.FullName) --dry-run=client -o yaml |
        kubectl apply -f - *> $null
    if ($LASTEXITCODE -ne 0) { throw "Failed to apply dashboard '$($_.Name)'." }
    kubectl -n $Namespace label configmap $name grafana_dashboard=1 --overwrite *> $null
}
kubectl -n $Namespace rollout status deployment/monitoring-grafana --timeout=5m
Write-Host "kube-prometheus-stack $ChartVersion and versioned Verse Store dashboards are installed."
