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
    $configPath = Join-Path ([System.IO.Path]::GetTempPath()) "verse-kind-$([guid]::NewGuid().ToString('N')).yaml"
    try {
        @"
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
  - role: control-plane
    extraPortMappings:
      - containerPort: 80
        hostPort: 80
        protocol: TCP
      - containerPort: 443
        hostPort: 443
        protocol: TCP
    labels:
      ingress-ready: "true"
"@ | Set-Content -LiteralPath $configPath -Encoding utf8
        kind create cluster --name $ClusterName --config $configPath --wait 120s
    } finally {
        Remove-Item -LiteralPath $configPath -Force -ErrorAction SilentlyContinue
    }
}
kubectl config use-context "kind-$ClusterName" *> $null
if ((kubectl config current-context) -ne "kind-$ClusterName") { throw "Unexpected Kubernetes context." }
Write-Host "Cluster '$ClusterName' is ready."
