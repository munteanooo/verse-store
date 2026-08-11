[CmdletBinding()]
param([string]$ClusterName = "verse-local", [string]$Namespace = "verse-dev")

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ((kubectl config current-context) -ne "kind-$ClusterName") { throw "Refusing to verify a context other than kind-$ClusterName." }
kubectl -n $Namespace wait --for=condition=Ready pod --all --timeout=10m
kubectl -n $Namespace get pods
kubectl -n $Namespace get deployments,statefulsets,services,pvc
kubectl -n $Namespace get events --sort-by=.lastTimestamp
kubectl -n $Namespace get deployment verse -o jsonpath='{.spec.template.spec.securityContext}'
kubectl -n $Namespace get deployment verse -o jsonpath='{.spec.template.spec.containers[0].startupProbe}{.spec.template.spec.containers[0].readinessProbe}{.spec.template.spec.containers[0].livenessProbe}'
$forward = Start-Process kubectl -ArgumentList "-n", $Namespace, "port-forward", "svc/verse", "18080:8080" -PassThru -WindowStyle Hidden
try {
    Start-Sleep -Seconds 3
    foreach ($path in @("/actuator/health/liveness", "/actuator/health/readiness", "/")) {
        $response = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:18080$path"
        if ($response.StatusCode -ne 200) { throw "Smoke test failed for $path." }
    }
} finally {
    if (-not $forward.HasExited) { Stop-Process -Id $forward.Id }
}
Write-Host "Automated Kubernetes smoke checks passed. Complete authenticated flows manually."

