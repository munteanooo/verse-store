[CmdletBinding()]
param([string]$ClusterName = "verse-local")

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ($ClusterName -ne "verse-local") { throw "Only the dedicated verse-local cluster is permitted." }
foreach ($command in @("docker", "kubectl", "kind")) {
    if (-not (Get-Command $command -ErrorAction SilentlyContinue)) { throw "$command is required." }
}
docker version *> $null
$clusters = @(kind get clusters)
if ($clusters -contains $ClusterName) {
    Write-Host "kind cluster '$ClusterName' already exists."
} else {
    kind create cluster --name $ClusterName --wait 120s
}
kubectl config use-context "kind-$ClusterName" *> $null
if ((kubectl config current-context) -ne "kind-$ClusterName") { throw "Unexpected Kubernetes context." }
Write-Host "Cluster '$ClusterName' is ready."

