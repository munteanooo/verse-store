[CmdletBinding()]
param([string]$ClusterName = "verse-local")

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$ControllerVersion = "controller-v1.15.1"
$ManifestUri = "https://raw.githubusercontent.com/kubernetes/ingress-nginx/$ControllerVersion/deploy/static/provider/kind/deploy.yaml"

foreach ($command in @("kubectl")) {
    if (-not (Get-Command $command -ErrorAction SilentlyContinue)) { throw "$command is required." }
}
if ((kubectl config current-context) -ne "kind-$ClusterName") { throw "Refusing to modify a context other than kind-$ClusterName." }
kubectl apply --server-side -f $ManifestUri
kubectl -n ingress-nginx wait --for=condition=Available deployment/ingress-nginx-controller --timeout=10m
Write-Host "ingress-nginx $ControllerVersion is Ready."
