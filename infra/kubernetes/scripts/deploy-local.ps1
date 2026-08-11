[CmdletBinding()]
param(
    [string]$ClusterName = "verse-local",
    [string]$Namespace = "verse-dev",
    [string]$ImageTag = "",
    [string]$ValuesFile = (Join-Path $PSScriptRoot "..\helm\verse-store\values-local.yaml.example")
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

foreach ($command in @("kubectl", "helm", "git")) {
    if (-not (Get-Command $command -ErrorAction SilentlyContinue)) { throw "$command is required." }
}
if ((kubectl config current-context) -ne "kind-$ClusterName") { throw "Refusing to deploy outside kind-$ClusterName." }
if ([string]::IsNullOrWhiteSpace($ImageTag)) { $ImageTag = "sha-$(git rev-parse HEAD)" }
if ($ImageTag -eq "latest") { throw "The latest tag is forbidden." }
$chart = Join-Path $PSScriptRoot "..\helm\verse-store"
helm upgrade --install verse-store $chart --namespace $Namespace --create-namespace `
    --values $ValuesFile --set-string "image.tag=$ImageTag" --wait --timeout 10m
kubectl -n $Namespace rollout status deployment/verse --timeout=5m
Write-Host "Verse Store deployed with immutable tag '$ImageTag'."

