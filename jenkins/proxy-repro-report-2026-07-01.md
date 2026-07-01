# HTTPS proxy CONNECT failure report — `hh-webadmin-35` → `services.gradle.org`

**Date (UTC):** 2026-07-01  
**Reporter:** Gene Expression Atlas / atlas-web-bulk Jenkins CI migration  
**Cluster:** `hh-webadmin-35` (RKE), namespace `gxa-jenkins`  
**Worker node (test pod):** `hh-rke-wp-webadmin-35-worker-5.caas.ebi.ac.uk` (pod IP `10.251.200.133`)  
**Proxy:** `http://hx-wwwcache.ebi.ac.uk:3128`  
**Destination:** `https://services.gradle.org/distributions/gradle-7.0-bin.zip` (~110 MB)  
**Method:** HTTPS via HTTP `CONNECT` through EBI webcache (Squid)

---

## Executive summary

Outbound HTTPS from Jenkins CI pods on **hh-webadmin-35** to **services.gradle.org** through **hx-wwwcache.ebi.ac.uk:3128** is **unreliable**. Failures occur during **CONNECT tunnel setup** or shortly after, before a complete download.

This blocks Gradle wrapper downloads in Java (`./gradlew`), causing Jenkins build failures:

```text
java.io.IOException: Unable to tunnel through proxy.
Proxy returns "HTTP/1.1 503 Service Unavailable"
```

Controlled reproduction (10 iterations per client, same URL, same proxy, no retries) shows **~50% failure rate for Java** and **40–100% failure rate for curl** depending on whether redirects are followed and CONNECT timeout behaviour.

**Request:** Please investigate Squid/webcache path from **hh-webadmin-35 worker subnets** to **services.gradle.org:443**, including CONNECT ACLs, parent cache health, timeouts, and rate/connection limits for large (~110 MB) downloads.

---

## Business impact

| Item | Detail |
|------|--------|
| **Service** | Jenkins — *Bulk Expression Atlas – Develop* |
| **URL** | `https://wwwint.ebi.ac.uk/fg/jenkins/` (job on cloud `hh-webadmin-35`) |
| **Failure stage** | *Provision Gradle* — `./gradlew` downloads Gradle 7.0 distribution |
| **User-visible error** | Proxy `503` on CONNECT, or connection timeout |
| **Workaround** | Manual cache seeding / init containers (undesirable operational burden) |

---

## Test environment

| Setting | Value |
|---------|--------|
| Pod image (Java tests) | `eclipse-temurin:11` (OpenJDK 11.0.31) — same family as Jenkins `openjdk` container |
| Pod image (curl tests) | `curlimages/curl:8.5.0` / curl 8.18.0 |
| Proxy env | `HTTP_PROXY` / `HTTPS_PROXY` = `http://hx-wwwcache.ebi.ac.uk:3128` (ConfigMap `ebi-proxy`) |
| Java proxy props | `-Dhttps.proxyHost=hx-wwwcache.ebi.ac.uk -Dhttps.proxyPort=3128` |
| Target file | `gradle-7.0-bin.zip` (112,554,948 bytes when download succeeds) |
| K8s repro Job | `proxy-repro-report` in `gxa-jenkins` |

---

## Test 1 — curl HEAD (short CONNECT, 5 runs)

All succeeded quickly with HTTP **307** (redirect to CDN). This is why quick `curl -I` checks can look healthy even when full downloads fail.

| Run | HTTP code | Error |
|-----|-----------|-------|
| 1–5 | 307 | none |

---

## Test 2 — Java `HttpURLConnection` full download (10 runs, no retry)

Simulates Gradle wrapper behaviour: single attempt, follows redirects, 30 s connect timeout, 600 s read timeout.

| Run | Result | HTTP | Bytes downloaded | Time (s) | Error |
|-----|--------|------|------------------|----------|-------|
| 1 | **FAIL** | — | 0 | 59 | `503 Service Unavailable` on CONNECT |
| 2 | **FAIL** | — | 0 | 59 | `503 Service Unavailable` on CONNECT |
| 3 | OK | 200 | 112,554,948 | 61 | — |
| 4 | OK | 200 | 112,554,948 | 0 | — |
| 5 | **FAIL** | — | 0 | 59 | `503 Service Unavailable` on CONNECT |
| 6 | OK | 200 | 112,554,948 | 0 | — |
| 7 | **FAIL** | — | 0 | 119 | `503 Service Unavailable` on CONNECT |
| 8 | **FAIL** | — | 0 | 59 | `503 Service Unavailable` on CONNECT |
| 9 | OK | 200 | 112,554,948 | 60 | — |
| 10 | OK | 200 | 112,554,948 | 0 | — |

**Java summary: 5 OK / 5 FAIL (50%)**

All Java failures share the same message:

```text
IOException: Unable to tunnel through proxy.
Proxy returns "HTTP/1.1 503 Service Unavailable"
```

When Java succeeds, the **full 110 MB** file is downloaded.

---

## Test 3 — curl full download without `-L` (10 runs, no retry)

**Note:** Without `-L`, curl records HTTP **307** and saves only the redirect body (~169 bytes), not the zip. Runs marked “OK” here did **not** download the distribution.

| Run | Result | HTTP | Bytes | Time (s) | Error |
|-----|--------|------|-------|----------|-------|
| 1 | FAIL | ERR | 0 | 30 | Connection timed out (30 s) |
| 2 | OK* | 307 | 169 | 0 | — |
| 3–5 | FAIL | ERR | 0 | 30 | Connection timed out |
| 6–10 | OK* | 307 | 169 | 0 | — |

**curl summary: 6 “OK” / 4 FAIL** — but *OK runs did not fetch the zip*.

---

## Test 4 — curl `-L` full download (10 runs, follow redirects, no retry)

Follow-up job `proxy-repro-curl-follow` — same proxy, same URL, `-L` enabled (fair comparison to Java).

| Run | Result | Time (s) | Error |
|-----|--------|----------|-------|
| 1–10 | **FAIL** | 30 each | `curl: (28) Proxy CONNECT aborted due to timeout` |

**curl -L summary: 0 OK / 10 FAIL (100%)** at time of test (2026-07-01 ~09:15 UTC)

---

## Why curl can look fine in casual checks

| Observation | Explanation |
|-------------|-------------|
| `curl -I` always works | Short CONNECT + immediate response; no large transfer |
| curl without `-L` “succeeds” | Returns **307** redirect page (~169 B), not the 110 MB file |
| curl with `--retry` in scripts | Masks intermittent failures (Gradle wrapper does **not** retry) |
| Java fails on first bad CONNECT | Single attempt → Jenkins build fails |

---

## What we are asking the network/webcache team to check

1. **Squid ACL / policy** — Is `CONNECT` to `services.gradle.org:443` (and CDN targets after redirect) allowed from **hh-webadmin-35** worker IP ranges?
2. **503 source** — Which tier returns `503` (Squid itself, load balancer, parent cache)? Please correlate with Squid/access logs for test window **2026-07-01 08:59–09:15 UTC**, source pod on worker-5 (`10.251.200.133`).
3. **Parent/upstream cache health** — Intermittent 503 often indicates parent selection or origin reachability issues.
4. **CONNECT / tunnel timeouts** — Java failures occur at ~59–119 s; curl `-L` fails at 30 s with “CONNECT aborted due to timeout”. Are CONNECT or tunnel idle timeouts too low for large downloads?
5. **Connection / rate limits** — Per-source limits on concurrent CONNECT tunnels from K8s worker nodes.
6. **CDN redirects** — Origin responds **307** to `downloads.gradle.org` (or similar). Ensure CONNECT is permitted to **redirect targets**, not only `services.gradle.org`.

---

## Suggested fix / acceptable outcomes

| Option | Description |
|--------|-------------|
| **Preferred** | Reliable CONNECT from hh-webadmin-35 → `services.gradle.org` (+ CDN) through hx-wwwcache |
| **Alternative** | Document a stable internal mirror URL on `.ebi.ac.uk` for Gradle distributions (bypasses external CONNECT) |
| **Alternative** | Confirm direct egress (no proxy) from hh-webadmin-35 to `services.gradle.org` if policy allows |

---

## Reproduction on cluster

```bash
# Re-run main report (Java + curl, 10 iterations)
kubectl delete job proxy-repro-report -n gxa-jenkins --ignore-not-found
kubectl apply -f jenkins/proxy-repro-job.yaml   # or ConfigMap + Job from repo

kubectl logs -n gxa-jenkins -f job/proxy-repro-report

# curl -L follow-up
kubectl delete job proxy-repro-curl-follow -n gxa-jenkins --ignore-not-found
kubectl apply -f jenkins/proxy-repro-curl-follow-job.yaml
kubectl logs -n gxa-jenkins job/proxy-repro-curl-follow
```

ConfigMap `proxy-repro-script` and Job manifests were applied in `gxa-jenkins` on 2026-07-01.

---

## Appendix — Jenkins CI proxy configuration (for reference)

From `jenkins/ebi-proxy-configmap.yaml` and `jenkins-k8s-pod.yaml`:

```yaml
HTTP_PROXY: http://hx-wwwcache.ebi.ac.uk:3128
HTTPS_PROXY: http://hx-wwwcache.ebi.ac.uk:3128
NO_PROXY: localhost,127.0.0.1,dockerhub.ebi.ac.uk,.ebi.ac.uk,*.cluster.local,...
```

Java (Gradle):

```properties
-Dhttps.proxyHost=hx-wwwcache.ebi.ac.uk
-Dhttps.proxyPort=3128
```

`services.gradle.org` is **not** in `NO_PROXY` (expected — external host).
