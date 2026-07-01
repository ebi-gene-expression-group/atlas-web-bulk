# Jenkins CI caches on Kubernetes

Jenkins CI pods for **atlas-web-bulk** can reuse shared dependency caches so builds do not re-download artifacts on every run.

## Gradle read-only dependency cache

Uses Gradle’s [`GRADLE_RO_DEP_CACHE`](https://docs.gradle.org/current/userguide/dependency_cache.html#sub:shared-read-only-cache) feature.

### Layout

| Component | File | Purpose |
|-----------|------|---------|
| ConfigMap | `ebi-proxy-configmap.yaml` | Shared `HTTP(S)_PROXY` / `NO_PROXY` for CI pods |
| PVC | `gradle-ro-dep-cache-pvc.yaml` | NFS-backed volume holding `modules-2/` |
| Seed job | `gradle-ro-dep-cache-seed-job.yaml` | One-off job that populates the PVC |
| Pod template | `../jenkins-k8s-pod.yaml` | Mounts the PVC read-only at `/gradle-ro-dep-cache` |
| Pipeline | `../Jenkinsfile` | Sets `GRADLE_RO_DEP_CACHE` when the cache is present |

The seed job writes to `/home/gradle/.gradle/caches` (Gradle’s default cache location). Jenkins pods mount the same PVC at `/gradle-ro-dep-cache` and point `GRADLE_RO_DEP_CACHE` there.

## Gradle wrapper distribution cache

Gradle 7.0 zip is stored on a shared PVC. **Provision Gradle** rewrites `gradle-wrapper.properties` to `distributionUrl=file:///gradle-wrapper-cache/.../gradle-7.0-bin.zip` so `./gradlew` reads the zip from NFS but **extracts and runs Gradle on local disk** (`GRADLE_USER_HOME=/tmp/gradle`) — avoiding `libnative-platform.so` failures on NFS.

### Layout

| Component | File | Purpose |
|-----------|------|---------|
| PVC | `gradle-wrapper-cache-pvc.yaml` | NFS-backed volume holding `dists/gradle-7.0-bin/.../gradle-7.0-bin.zip` |
| Seed job | `gradle-wrapper-cache-seed-job.yaml` | Optional one-off pre-populate (same path as init container) |
| Pod template | `../jenkins-k8s-pod.yaml` | `init-gradle-wrapper` ensures zip on PVC; read-only mount at `/gradle-wrapper-cache` |
| Pipeline | `../Jenkinsfile` | *Provision Gradle* patches `distributionUrl` to `file://` then runs `./gradlew` |

The init container skips download when the zip is already present. `GRADLE_USER_HOME=/tmp/gradle` is pod-local (not the PVC); only the zip lives on NFS.

### One-time setup (optional seed before first build)

```bash
kubectl apply -f jenkins/gradle-wrapper-cache-pvc.yaml
kubectl delete job gradle-wrapper-cache-seed -n gxa-jenkins --ignore-not-found
kubectl apply -f jenkins/gradle-wrapper-cache-seed-job.yaml
kubectl logs -n gxa-jenkins -f job/gradle-wrapper-cache-seed
```

If the seed job is not run, the first CI pod’s `init-gradle-wrapper` downloads the zip instead.

## npm download cache

Uses npm’s global download cache (`_cacache`) via `npm_config_cache=/npm-cache`. Each package still gets a local `node_modules` in the workspace; only registry tarballs are shared.

### Layout

| Component | File | Purpose |
|-----------|------|---------|
| PVC | `npm-ci-cache-pvc.yaml` | NFS-backed volume holding npm `_cacache/` |
| Seed job | `npm-ci-cache-seed-job.yaml` | One-off job that runs all `npm install`s |
| Pod template | `../jenkins-k8s-pod.yaml` | Mounts the PVC at `/npm-cache`, sets `npm_config_cache` |
| Pipeline | `../Jenkinsfile` | Logs cache status in **Provision Node.js** |
| Build script | `../compile-front-end-packages.sh` | Uses `npm_config_cache` when set |

**Not cached on the PVC:** `node_modules` trees (per build, in the workspace), nvm Node install, global `npm-check-updates`.

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

### 2. Create the Gradle PVC

```bash
kubectl apply -f jenkins/gradle-ro-dep-cache-pvc.yaml
kubectl get pvc gradle-7.0-ro-dep-cache-rox -n gxa-jenkins
```

Wait until `STATUS` is `Bound`.

### 3. Seed the Gradle cache

The seed job clones `atlas-web-bulk` from GitHub and resolves compile/test/runtime/JaCoCo classpaths so Gradle stores dependencies under `modules-2/` (no compilation).

```bash
kubectl delete job gradle-ro-dep-cache-seed -n gxa-jenkins --ignore-not-found
kubectl apply -f jenkins/gradle-ro-dep-cache-seed-job.yaml
kubectl logs -n gxa-jenkins -f job/gradle-ro-dep-cache-seed
```

Expect `BUILD SUCCESSFUL` and a line like:

```text
Seeded 1422 files under modules-2
```

### 4. Create the npm PVC

```bash
kubectl apply -f jenkins/npm-ci-cache-pvc.yaml
kubectl get pvc npm-ci-cache-rwx -n gxa-jenkins
```

### 5. Seed the npm cache

Runs all package `npm install`s (skips webpack) so registry tarballs land in `_cacache`:

```bash
kubectl delete job npm-ci-cache-seed -n gxa-jenkins --ignore-not-found
kubectl apply -f jenkins/npm-ci-cache-seed-job.yaml
kubectl logs -n gxa-jenkins -f job/npm-ci-cache-seed
```

Expect `Seeded N files under _cacache`.

### 6. Confirm Jenkins can use the caches

Ensure `jenkins-k8s-pod.yaml` is configured in Jenkins (Kubernetes cloud pod template) with the `gradle-ro-dep-cache` and `npm-ci-cache` volumes. On the next build, **Provision Gradle** should log nothing about a missing Gradle cache, and **Provision Node.js** should log `npm CI cache: reusing /npm-cache` after seeding.

To verify from a running build pod:

```bash
kubectl exec -n gxa-jenkins <pod> -c openjdk -- ls /gradle-ro-dep-cache/modules-2
kubectl exec -n gxa-jenkins <pod> -c openjdk -- ls /npm-cache/_cacache
```

## Re-seeding

Re-run the Gradle seed job when dependencies change significantly (e.g. after upgrading Gradle or adding many new libraries):

```bash
kubectl delete job gradle-ro-dep-cache-seed -n gxa-jenkins --ignore-not-found
kubectl apply -f jenkins/gradle-ro-dep-cache-seed-job.yaml
```

Re-run the npm seed job after `package-lock.json` changes across front-end packages:

```bash
kubectl delete job npm-ci-cache-seed -n gxa-jenkins --ignore-not-found
kubectl apply -f jenkins/npm-ci-cache-seed-job.yaml
```

Seed jobs use the `develop` branch by default. Edit the clone command in the seed job YAML if you need a different branch.

## Troubleshooting

| Symptom | Likely cause |
|---------|----------------|
| `Gradle RO dep cache not seeded yet` in Jenkins logs | Gradle seed job not run, failed, or `modules-2` missing on PVC |
| `npm CI cache: empty or not seeded` in Jenkins logs | npm seed job not run yet; first build still works but downloads more |
| Seed job fails on unknown configuration | Add the missing Gradle configuration to the seed job loop in `gradle-ro-dep-cache-seed-job.yaml` |
| Seed job stuck downloading | Proxy env vars missing or cluster has no outbound access |
| `Insufficient cpu` scheduling seed pod | Reduce seed job resource requests or free worker capacity |
| Builds still slow on first Gradle stage | Wrapper zip missing on PVC — check init container logs or run `gradle-wrapper-cache-seed` |
| `Gradle wrapper zip missing` in Provision Gradle | `init-gradle-wrapper` failed; check pod events and proxy access to `services.gradle.org` |
| npm installs still slow after seed | `node_modules` is rebuilt each run; only tarball download is cached |

Check seed job status:

```bash
kubectl get job,pods -n gxa-jenkins -l job-name=gradle-ro-dep-cache-seed
kubectl get job,pods -n gxa-jenkins -l job-name=npm-ci-cache-seed
```

## Local development

The same pattern exists for Docker Compose under `docker/prepare-dev-environment/gradle-cache/`. See `docker/prepare-dev-environment/gradle-cache/run.sh`.
