[CmdletBinding(SupportsShouldProcess = $true, ConfirmImpact = "High")]
param(
    [Parameter(Mandatory = $true)][string]$ConfirmNamespace,
    [string]$ClusterName = "verse-local",
    [string]$Namespace = "monitoring"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
if ((kubectl config current-context) -ne "kind-$ClusterName") { throw "Refusing cleanup outside kind-$ClusterName." }
if ($ConfirmNamespace -ne $Namespace -or $Namespace -ne "monitoring") { throw "Confirmation must exactly equal 'monitoring'." }
if ($PSCmdlet.ShouldProcess("namespace monitoring and its monitoring-only PVCs", "Uninstall monitoring and delete its namespace")) {
    helm uninstall monitoring --namespace $Namespace --ignore-not-found
    kubectl delete namespace $Namespace --ignore-not-found
}
