[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$ConfirmClusterName,
    [string]$ClusterName = "verse-local"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ($ClusterName -ne "verse-local" -or $ConfirmClusterName -ne $ClusterName) {
    throw "Refusing cleanup: confirmation must exactly equal verse-local."
}
if (-not (@(kind get clusters) -contains $ClusterName)) {
    Write-Host "Cluster '$ClusterName' does not exist."
    exit 0
}
kind delete cluster --name $ClusterName
Write-Host "Deleted only kind cluster '$ClusterName'. Its local Kubernetes volumes are not recoverable."

