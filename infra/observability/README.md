# Verse Store observability

The application exposes only `health` and Prometheus metrics. In Docker and Kubernetes these endpoints use the dedicated management port `9090`; the public application service remains on `8080`. Kubernetes permits management traffic from the `monitoring` namespace, and the `ServiceMonitor` selects the application metrics port. The stack is pinned to `kube-prometheus-stack` `88.1.3`.

## Local Kubernetes

Prerequisites are the existing `kind-verse-local` cluster, Helm, kubectl, and a deployed Verse Store release. Install or reconcile the monitoring stack with Windows PowerShell 5.1:

```powershell
.\infra\observability\scripts\install-monitoring.ps1
.\infra\observability\scripts\verify-monitoring.ps1 -TestAlertLifecycle
```

The installer creates the `monitoring` namespace and a random Grafana administrator password in the `monitoring-grafana-admin` Secret if it does not already exist. It never prints that password. To open Grafana on `http://127.0.0.1:3000`:

```powershell
.\infra\observability\scripts\access-grafana.ps1
```

Add `-CopyPasswordToClipboard` only when you deliberately want the password placed on the local clipboard. Prometheus targets can be inspected with a temporary port-forward:

```powershell
kubectl -n monitoring port-forward service/monitoring-prometheus 9090:9090
```

Open `http://127.0.0.1:9090/targets`; the Verse Store target in `verse-dev` must be `UP`. Five versioned dashboards are provisioned for overview, JVM, HTTP, database/HikariCP, and Kubernetes workloads.

Alerts cover missing or unready application instances, sustained HTTP 5xx ratio and p95 latency, JVM heap pressure, and HikariCP pool saturation. The verification script's optional lifecycle test creates a temporary, harmless `PrometheusRule`, observes Pending then Firing, and removes it in `finally`.

Prometheus and Grafana use PVCs. A pod or StatefulSet/Deployment restart must retain time series and dashboard state. Deleting the namespace or PVCs destroys that local data.

## Docker Compose

Copy `.env.example` to an untracked `.env`, replace the Grafana password, then start the optional profile:

```powershell
docker compose --profile observability up -d --build
```

Grafana is available on `${GRAFANA_PORT:-3000}`. Prometheus is intentionally not published to the host; Grafana reaches it on the private Compose network. The application's management port is also not published.

## Troubleshooting

- No Verse target: confirm the application chart has `monitoring.enabled=true`, its pod is Ready, and `kubectl -n monitoring get servicemonitor` shows the Verse monitor.
- Target is down: inspect `kubectl -n monitoring describe servicemonitor verse` and the app Service endpoints; metrics must use port `9090` and path `/actuator/prometheus`.
- Missing dashboards: check ConfigMaps labeled `grafana_dashboard=1` and Grafana sidecar logs.
- Missing Hikari metrics: confirm the application has initialized its datasource and generated at least one request.
- Pending pods: inspect resource pressure and PVC binding before changing requests or limits.

Cleanup is deliberately guarded and affects only the monitoring release/namespace:

```powershell
.\infra\observability\scripts\cleanup-monitoring.ps1 -ConfirmNamespace monitoring
```

## Production limitations

This setup is for local development. It has no durable external storage, remote-write, long-term retention, Alertmanager receivers, SSO for Grafana, ingress/TLS, backup automation, multi-replica Prometheus, or production-sized resources. Production must supply those controls, rotate credentials, restrict network access further, and define alert ownership/runbooks.
