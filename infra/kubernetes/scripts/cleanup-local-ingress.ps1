[CmdletBinding(SupportsShouldProcess = $true, ConfirmImpact = "High")]
param([string]$ClusterName = "verse-local", [string]$Namespace = "verse-dev")

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ((kubectl config current-context) -ne "kind-$ClusterName") { throw "Refusing cleanup outside kind-$ClusterName." }
if ($PSCmdlet.ShouldProcess("Ingress resources and TLS Secret in $Namespace, plus namespace ingress-nginx", "Delete only local ingress/TLS resources")) {
    kubectl -n $Namespace delete ingress verse-app verse-keycloak verse-mailpit --ignore-not-found
    kubectl -n $Namespace delete secret verse-local-tls --ignore-not-found
    kubectl delete namespace ingress-nginx --ignore-not-found
}
