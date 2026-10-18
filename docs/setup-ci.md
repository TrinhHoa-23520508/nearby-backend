# CI setup (GitHub Actions)

The project currently has **CI only**: every push and Pull Request is built and checked automatically. There is no CD (automated deployment) yet.

## 1. What CI does

Workflow file: [`.github/workflows/ci.yml`](../.github/workflows/ci.yml)

| Trigger | When |
|---|---|
| Push to `master` | Every merged or directly pushed commit |
| Pull Request into `master` opened or updated | Every push to the PR branch |
| Manual run | **Actions** tab → **CI** → **Run workflow** |

A single job, **Build & test** (Ubuntu, JDK 25), runs `./gradlew build`. The build **fails** on any of the following:

| Check | Tool | How to fix |
|---|---|---|
| Unit and integration tests | JUnit 5 + Testcontainers (real PostGIS, Redis) | Read the report in the `test-reports` artifact |
| Module boundaries | `ModularityTests` (`ApplicationModules.verify()`) | Do not call another module's internals |
| Code format | spring-javaformat (`checkFormat`) | Run `./gradlew format` and commit |
| Coverage | JaCoCo: ≥ 60% overall, ≥ 80% for `domain` / `application` packages | Add tests; **never** lower the threshold |

Each run keeps these artifacts, downloadable at the bottom of the run page:

- `jacoco-report`: coverage report; open `html/index.html`
- `test-reports`: only on failure; details of the failing tests
- `spring-modulith-docs`: module diagrams

## 2. Enable CI for the first time

No secrets or extra settings are needed. GitHub picks up the workflow as soon as `.github/workflows/ci.yml` exists on GitHub.

1. Commit and push:

   ```sh
   git add .
   git commit -m "Set up project infrastructure and CI"
   git push -u origin feature/init-infra
   ```

2. On GitHub, open a Pull Request from `feature/init-infra` into `master`. CI runs automatically and its status appears on the PR.
3. See details in the repository's **Actions** tab: https://github.com/TrinhHoa-23520508/nearby-backend/actions

If the Actions tab shows no workflow, go to **Settings → Actions → General**, select **Allow all actions and reusable workflows**, and click **Save**.

> `gradlew` must be executable in Git, otherwise the Linux runner fails with `Permission denied`. This is already set. If the error comes back, run `git update-index --chmod=+x gradlew` and commit.

## 3. Require green CI before merging (recommended)

This stops code that breaks the build from being merged into `master`.

1. **Settings → Branches → Add branch ruleset** (or *Add classic branch protection rule*).
2. Branch: `master`.
3. Enable these options:
   - **Require a pull request before merging**
   - **Require status checks to pass**, and add the check **`Build & test`**. The check only appears in the list after CI has run at least once.
   - **Require branches to be up to date before merging**
   - **Block force pushes**
4. Click **Save / Create**.

From then on, every change follows the same flow: create a branch → push → open a PR → CI green → review → merge.

## 4. Run the same checks locally

Before pushing, run the exact command CI uses to catch problems early. Docker Desktop must be running.

```sh
./gradlew format     # fix formatting
./gradlew build      # exactly what CI runs
```

If the build passes locally, it will almost certainly pass on CI.

## 5. When CI fails

| Log message | Cause | Fix |
|---|---|---|
| `./gradlew: Permission denied` | `gradlew` lost its executable bit | `git update-index --chmod=+x gradlew`, then commit |
| `checkFormatMain FAILED` / `checkFormatTest FAILED` | Code is not formatted | `./gradlew format`, then commit |
| `Rule violated for bundle ... lines covered ratio is 0.5x, but expected minimum is 0.60` | Coverage below the threshold | Add tests for the new code |
| `ModularityTests > verifiesModuleStructure() FAILED` | A module uses another module's internal classes, or there is a dependency cycle | Go through the root-package interface or an event; the log names the offending types |
| `XxxTests > ... FAILED` | A test failed | Download the `test-reports` artifact and open `index.html` |
| `Could not find a valid Docker environment` | The runner has no Docker (rare) | Keep `runs-on: ubuntu-latest`; Windows and macOS runners do not ship Docker |
| Job `cancelled` | A newer commit was pushed to the same branch and the older run was cancelled (`concurrency` setting) | Expected; check the latest run |

## 6. Limits and cost

- **Public** repository: GitHub Actions is free with unlimited minutes.
- **Private** repository: the Free plan includes 2,000 minutes per month. One CI run takes about 3–5 minutes. The first run takes longer because it downloads dependencies and Docker images; later runs use the cache.
- The job has `timeout-minutes: 20` so a hung run cannot burn minutes.

## 7. Adding CD later

The deployment setup (Docker Compose, Nginx, Prometheus, Grafana) already exists in `deploy/`; see [deploy/README.md](../deploy/README.md). Once the VM provider is chosen (Architecture Design, section 2.2), add two jobs to `ci.yml`:

1. **Package image**: runs after `build`, only on `master`. Builds the `Dockerfile` and pushes it to GitHub Container Registry (`ghcr.io`). Needs `permissions: packages: write`.
2. **Deploy**: connects to the VM over SSH and runs `docker compose pull app && docker compose up -d`. Needs the secrets `DEPLOY_HOST`, `DEPLOY_USER` and `DEPLOY_SSH_KEY` under **Settings → Environments → production**.
