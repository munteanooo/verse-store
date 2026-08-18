[CmdletBinding()]
param(
    [string]$ClusterName = "verse-local",
    [string]$Namespace = "monitoring",
    [int]$LocalPort = 3000,
    [switch]$CopyPasswordToClipboard
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
if ((kubectl config current-context) -ne "kind-$ClusterName") { throw "Refusing access outside kind-$ClusterName." }
$grafanaService = kubectl -n $Namespace get service monitoring-grafana --ignore-not-found -o name
if ($LASTEXITCODE -ne 0) { throw "Failed to inspect the Grafana Service." }
if ([string]::IsNullOrWhiteSpace($grafanaService)) { throw "Grafana Service is missing. Run install-monitoring.ps1 first." }
if ($CopyPasswordToClipboard) {
    $encoded = kubectl -n $Namespace get secret monitoring-grafana-admin -o jsonpath='{.data.admin-password}'
    $password = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($encoded))
    Set-Clipboard -Value $password
    Remove-Variable encoded,password -ErrorAction SilentlyContinue
    Write-Host "Grafana password copied to the clipboard; it was not printed."
}
Write-Host "Grafana: http://127.0.0.1:$LocalPort (user: admin). Press Ctrl+C to stop port-forward."
kubectl -n $Namespace port-forward service/monitoring-grafana "${LocalPort}:80"
