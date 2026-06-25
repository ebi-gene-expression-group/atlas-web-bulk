# Gradle read-only dependency cache (Jenkins on Kubernetes)

Jenkins CI pods for **atlas-web-bulk** can reuse a shared Gradle dependency cache so builds do not re-download Maven artifacts on every run. This uses Gradle’s [`GRADLE_RO_DEP_CACHE`](https://docs.gradle.org/current/userguide/dependency_cache.html#sub:shared-read-only-cache) feature.

## Layout

| Component | File | Purpose |
|-----------|------|---------|
| ConfigMap | `ebi-proxy-configmap.yaml` | Shared `HTTP(S)_PROXY` / `NO_PROXY` for CI pods |
| PVC | `gradle-ro-dep-cache-pvc.yaml` | NFS-backed volume holding `modules-2/` |
| Seed job | `gradle-ro-dep-cache-seed-job.yaml` | One-off job that populates the PVC |
| Pod template | `../jenkins-k8s-pod.yaml` | Mounts the PVC read-only at `/gradle-ro-dep-cache` |
| Pipeline | `../Jenkinsfile` | Sets `GRADLE_RO_DEP_CACHE` when the cache is present |

The seed job writes to `/home/gradle/.gradle/caches` (Gradle’s default cache location). Jenkins pods mount the same PVC at `/gradle-ro-dep-cache` and point `GRADLE_RO_DEP_CACHE` there.

**Not cached on the PVC:** the Gradle wrapper distribution still downloads per pod into `GRADLE_USER_HOME` (`/tmp/gradle` in `jenkins-k8s-pod.yaml`).

## Prerequisites

- Cluster context for the target Jenkins cluster (e.g. `hh-webadmin-35`)
- Namespace `gxa-jenkins` exists
- Storage class `standard-nfs-production` is available
- Outbound HTTP(S) via EBI proxy (`hx-wwwcache.ebi.ac.uk:3128`) from worker nodes

## One-time setup

### 1. Create the proxy ConfigMap

```bash
kubectl apply -f jenkins/ebi-proxy-configmap.yaml
```

Pods reference this via `envFrom.configMapRef` (`ebi-proxy` in `gxa-jenkins`).

### 2. Create the PVC

```bash
kubectl apply -f jenkins/gradle-ro-dep-cache-pvc.yaml
kubectl get pvc gradle-7.0-ro-dep-cache-rox -n gxa-jenkins
```

Wait until `STATUS` is `Bound`.

### 3. Seed the cache

The seed job clones `atlas-web-bulk` from GitHub and runs compile tasks so Gradle resolves and stores dependencies under `modules-2/`.

```bash
kubectl delete job gradle-ro-dep-cache-seed -n gxa-jenkins --ignore-not-found
kubectl apply -f jenkins/gradle-ro-dep-cache-seed-job.yaml
kubectl logs -n gxa-jenkins -f job/gradle-ro-dep-cache-seed
```

Expect `BUILD SUCCESSFUL` and a line like:

```text
Seeded 1422 files under modules-2
```

### 4. Confirm Jenkins can use the cache

Ensure `jenkins-k8s-pod.yaml` is configured in Jenkins (Kubernetes cloud pod template) with the `gradle-ro-dep-cache` volume. On the next build, the **Provision Gradle** stage should log nothing about a missing cache, and dependency downloads should be minimal.

To verify from a running build pod:

```bash
kubectl exec -n gxa-jenkins <pod> -c openjdk -- ls /gradle-ro-dep-cache/modules-2
```

## Re-seeding

Re-run the seed job when dependencies change significantly (e.g. after upgrading Gradle or adding many new libraries):

```bash
kubectl delete job gradle-ro-dep-cache-seed -n gxa-jenkins --ignore-not-found
kubectl apply -f jenkins/gradle-ro-dep-cache-seed-job.yaml
```

The job uses the `develop` branch by default. Edit the clone command in `gradle-ro-dep-cache-seed-job.yaml` if you need a different branch or tag.

## Troubleshooting

| Symptom | Likely cause |
|---------|----------------|
| `Gradle RO dep cache not seeded yet` in Jenkins logs | Seed job not run, failed, or `modules-2` missing on PVC |
| `Task 'testCompileJava' not found` in seed job | Gradle 7+ uses `compileTestJava`; ensure seed job YAML is up to date |
| Seed job stuck downloading | Proxy env vars missing or cluster has no outbound access |
| `Insufficient cpu` scheduling seed pod | Reduce seed job resource requests or free worker capacity |
| Builds still slow on first stage | Wrapper zip download is expected; only Maven deps use the shared cache |

Check seed job status:

```bash
kubectl get job,pods -n gxa-jenkins -l job-name=gradle-ro-dep-cache-seed
```

## Local development

The same pattern exists for Docker Compose under `docker/prepare-dev-environment/gradle-cache/`. See `docker/prepare-dev-environment/gradle-cache/run.sh`.
