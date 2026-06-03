# Performance testing

Two complementary tools:

1. **k6** — HTTP load tests against the running back (scripts in this folder).
2. **Micrometer + Prometheus** — live metrics exposed by the app on `GET /metrics`.

---

## 1. k6 load tests

### Install k6

```powershell
winget install k6 --source winget   # Windows
# or: choco install k6
# macOS: brew install k6
```

### Setup: run the back in test mode (local Postgres + mocked Mistral)

This is the correct setup for load testing — it does **not** touch prod
(Supabase) and does **not** call (or pay for) the real Mistral API.

```powershell
# terminal 1 — fake Mistral conversations API on :8089
./gradlew runMistralMock

# terminal 2 — app on :8080, APP_MODE=test (local Postgres :5433), Mistral -> mock
./gradlew runTestMode      # auto-starts the postgres:16-alpine test container
```

`runTestMode` sets `APP_MODE=test` (so `Database.run(...)` uses JDBC/local
Postgres instead of Supabase) and points `MISTRAL_API_URL` at the local mock.
Override the Mistral target by exporting `MISTRAL_API_URL` before the task.

### Run the load tests

```powershell
k6 run perf/smoke.js                              # sanity check, 1 VU
k6 run perf/users.js                              # /users CRUD       (DB-bound)
k6 run perf/objectives.js                         # /objectives CRUD  (DB-bound)
k6 run perf/coach.js                              # /coach/chat       (mocked Mistral)
k6 run perf/profiling.js                          # /profiling        (mocked Mistral)
k6 run -e BASE_URL=http://localhost:8080 perf/users.js   # override target
```

DB-bound scripts ramp to 20 VUs (~1m45) with thresholds `p95<200ms / p99<500ms`;
the Mistral-backed scripts ramp to 10 VUs with looser budgets (`p95<400ms`)
since they include an HTTP hop to the mock. A failed threshold makes k6 exit
non-zero — usable as a CI gate.

### Coverage

| Script | Routes | Backend |
|--------|--------|---------|
| `smoke.js` | `/users` create+get+delete | Postgres |
| `users.js` | `/users` full CRUD | Postgres |
| `objectives.js` | `/objectives` full CRUD | Postgres |
| `coach.js` | `POST /coach/chat/{userId}` + `/{convId}` | Postgres + mocked Mistral |
| `profiling.js` | `POST /profiling`, `GET /profiling/{userId}[/history]` | Postgres + mocked Mistral |

> The mock (`src/main/kotlin/mock/MistralMock.kt`) returns canned, schema-valid
> conversation responses. To stress the back *with* real Mistral latency,
> export the real `MISTRAL_API_URL` before `runTestMode` — but mind the rate
> limits and billing.

---

## 2. Micrometer / Prometheus metrics

The app exposes Prometheus metrics on `GET /metrics` (wired in
`src/main/kotlin/Monitoring.kt`). Useful series:

| Metric | What |
|--------|------|
| `ktor_http_server_requests_seconds_*` | per-route latency histogram + count (tagged by `route`, `method`, `status`) |
| `ktor_http_server_active_requests` | in-flight requests |
| `jvm_memory_used_bytes`, `jvm_gc_pause_seconds` | heap & GC pressure |
| `jvm_threads_live_threads` | thread count |
| `system_cpu_usage`, `process_cpu_usage` | CPU |

Quick check while a k6 run is going:

```powershell
curl http://localhost:8080/metrics
```

To chart it, scrape with Prometheus and view in Grafana:

```yaml
# prometheus.yml
scrape_configs:
  - job_name: relaunch-back
    metrics_path: /metrics
    scrape_interval: 5s
    static_configs:
      - targets: ['host.docker.internal:8080']   # if Prometheus runs in Docker
```

The Grafana dashboard **ID 4701** ("JVM Micrometer") works out of the box with
these metrics.

---

## Suggested workflow

1. `./gradlew startTestDb` and run the app in `test` mode (local Postgres, Mistral mocked).
2. `k6 run perf/smoke.js` — confirm everything is wired.
3. `k6 run perf/users.js` / `perf/objectives.js` — get baseline p95/p99.
4. Watch `/metrics` (or Grafana) during the run to spot GC pauses / CPU saturation.
5. If a route is slow: profile the JVM with **async-profiler** or **JFR** and check
   Postgres `pg_stat_statements` for slow queries.
