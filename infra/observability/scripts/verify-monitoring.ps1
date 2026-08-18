[CmdletBinding()]
param(
    [string]$ClusterName = "verse-local",
    [string]$Namespace = "monitoring",
    [switch]$TestAlertLifecycle
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
if ((kubectl config current-context) -ne "kind-$ClusterName") { throw "Refusing verification outside kind-$ClusterName." }
kubectl -n $Namespace wait --for=condition=Ready pod --all --timeout=10m
kubectl -n $Namespace get pods,pvc

$forward = Start-Process kubectl -ArgumentList "-n", $Namespace, "port-forward", "service/monitoring-prometheus", "19090:9090" -PassThru -WindowStyle Hidden
try {
    Start-Sleep -Seconds 4
    $targetReady = $false
    for ($attempt = 1; $attempt -le 18 -and -not $targetReady; $attempt++) {
        $targets = Invoke-RestMethod -Uri "http://127.0.0.1:19090/api/v1/targets"
        $verseTarget = @($targets.data.activeTargets | Where-Object {
            $_.labels.namespace -eq "verse-dev" -and $_.labels.service -eq "verse"
        })
        $targetReady = $verseTarget.Count -ge 1 -and @($verseTarget | Where-Object health -eq "up").Count -ge 1
        if (-not $targetReady) { Start-Sleep -Seconds 5 }
    }
    if (-not $targetReady) { throw "Verse Store Prometheus target did not become UP within 90 seconds." }

    # A fresh deployment has no HTTP timer series until its first application request.
    kubectl -n verse-dev exec deployment/verse -- wget -q -O /dev/null http://127.0.0.1:8080/login
    if ($LASTEXITCODE -ne 0) { throw "Failed to generate the local HTTP metrics verification request." }
    foreach ($metric in @("jvm_memory_used_bytes", "http_server_requests_seconds_count", "hikaricp_connections_active")) {
        $metricFound = $false
        for ($attempt = 1; $attempt -le 18 -and -not $metricFound; $attempt++) {
            $query = Invoke-RestMethod -Uri "http://127.0.0.1:19090/api/v1/query?query=$metric"
            $metricFound = @($query.data.result).Count -ge 1
            if (-not $metricFound) { Start-Sleep -Seconds 5 }
        }
        if (-not $metricFound) { throw "Required metric '$metric' was not scraped within 90 seconds." }
    }

    if ($TestAlertLifecycle) {
        @"
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: verse-observability-lifecycle-test
  namespace: $Namespace
  labels:
    release: monitoring
spec:
  groups:
    - name: verse.lifecycle.test
      interval: 5s
      rules:
        - alert: VerseObservabilityLifecycleTest
          expr: vector(1)
          for: 20s
          labels: {severity: none}
          annotations: {summary: Controlled local alert lifecycle test}
"@ | kubectl apply -f - *> $null
        try {
            Start-Sleep -Seconds 10
            $pending = Invoke-RestMethod -Uri "http://127.0.0.1:19090/api/v1/alerts"
            if (-not (@($pending.data.alerts).state -contains "pending")) { throw "Controlled alert did not become Pending." }
            Start-Sleep -Seconds 25
            $firing = Invoke-RestMethod -Uri "http://127.0.0.1:19090/api/v1/alerts"
            if (-not (@($firing.data.alerts).state -contains "firing")) { throw "Controlled alert did not become Firing." }
        } finally {
            kubectl -n $Namespace delete prometheusrule verse-observability-lifecycle-test --ignore-not-found *> $null
        }
    }
} finally {
    if (-not $forward.HasExited) { Stop-Process -Id $forward.Id }
}

$encodedUser = kubectl -n $Namespace get secret monitoring-grafana-admin -o jsonpath='{.data.admin-user}'
$encodedPassword = kubectl -n $Namespace get secret monitoring-grafana-admin -o jsonpath='{.data.admin-password}'
$username = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($encodedUser))
$password = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($encodedPassword))
$credentialBytes = [Text.Encoding]::ASCII.GetBytes("${username}:${password}")
$headers = @{Authorization = "Basic $([Convert]::ToBase64String($credentialBytes))"}
$grafanaForward = Start-Process kubectl -ArgumentList "-n", $Namespace, "port-forward", "service/monitoring-grafana", "13000:80" -PassThru -WindowStyle Hidden
try {
    Start-Sleep -Seconds 4
    $dashboards = Invoke-RestMethod -Uri "http://127.0.0.1:13000/api/search?tag=verse-store" -Headers $headers
    if (@($dashboards).Count -lt 5) { throw "Expected five versioned Verse Store dashboards in Grafana." }
} finally {
    if (-not $grafanaForward.HasExited) { Stop-Process -Id $grafanaForward.Id }
    Remove-Variable encodedUser,encodedPassword,username,password,credentialBytes,headers -ErrorAction SilentlyContinue
}
Write-Host "Prometheus target, JVM/HTTP/HikariCP metrics, Grafana dashboards, and optional alert lifecycle are verified."
