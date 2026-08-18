[CmdletBinding()]
param([string]$ClusterName = "verse-local", [string]$Namespace = "verse-dev")

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ((kubectl config current-context) -ne "kind-$ClusterName") { throw "Refusing to verify a context other than kind-$ClusterName." }
kubectl -n ingress-nginx wait --for=condition=Available deployment/ingress-nginx-controller --timeout=5m
kubectl -n $Namespace wait --for=condition=Ready pod --all --timeout=10m
kubectl -n $Namespace get pods,deployments,statefulsets,services,ingress,pvc

$request = [System.Net.HttpWebRequest]::Create("http://verse.local")
$request.AllowAutoRedirect = $false
try { $httpResponse = $request.GetResponse() } catch [System.Net.WebException] { $httpResponse = $_.Exception.Response }
try {
    if ([int]$httpResponse.StatusCode -notin @(301, 302, 307, 308)) { throw "HTTP did not redirect to HTTPS." }
    if (-not $httpResponse.Headers["Location"].StartsWith("https://verse.local")) { throw "HTTP redirect target is unexpected." }
} finally { if ($null -ne $httpResponse) { $httpResponse.Dispose() } }

foreach ($uri in @(
    "https://verse.local/actuator/health/liveness",
    "https://verse.local/actuator/health/readiness",
    "https://verse.local/",
    "https://auth.verse.local/realms/verse/.well-known/openid-configuration"
)) {
    $response = Invoke-WebRequest -UseBasicParsing -Uri $uri
    if ($response.StatusCode -ne 200) { throw "HTTPS smoke test failed for $uri." }
}
Write-Host "HTTPS, redirect, application health, and public OIDC metadata checks passed."
