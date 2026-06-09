# reliable-ci-cd-pipeline

A cross-platform CI/CD monorepo demonstrating reusable pipeline patterns across **Jenkins**, **GitHub Actions**, and **GitLab CI**  with Docker image builds, Helm chart deployments, and Kubernetes rollouts.

This repository is the application side of the pattern. The pipeline logic is in separate shared repositories. Each service here only declares what it is and passes parameters to the shared definition.

---

## Repository Structure

```
reliable-ci-cd-pipeline/
├── services/
│   ├── api-gateway/
│       ├── src/
│       ├── pom.xml
│       ├── Dockerfile
│       ├── .gitlab-ci.yml        # GitLab CI service pipeline
│       ├── Jenkinsfile           # Jenkins service pipeline
│   ├── authors-service/
│   ├── books-service/
│   └── bookstore-frontend/
├── charts/
│   └── microservice/             # Single shared Helm chart for all services
│       ├── Chart.yaml
│       ├── values.yaml
│       └── templates/
├── helm-values/
│   ├── api-gateway/
│   │   ├── values-dev.yaml
│   │   └── values-prod.yaml
│   ├── authors-service/
│   ├── books-service/
│   └── bookstore-frontend/
├── .github/
│   └── workflows/                # GitHub Actions caller workflows (one per service)
└── .gitlab-ci.yml                # GitLab CI root pipeline
```

Each service directory contains its own `Jenkinsfile` (for Jenkins) and `.gitlab-ci.yml` (for GitLab CI). GitHub Actions workflow files live at the root under `.github/workflows/` due to a platform constraint.

---

## The Four Services

| Service | Type | Build Tool |
|---|---|---|
| `api-gateway` | backend | Maven / JDK 17 |
| `authors-service` | backend | Maven / JDK 17 |
| `books-service` | backend | Maven / JDK 17 |
| `bookstore-frontend` | frontend | Node.js 24 / npm |

---

## Pipeline Stages

All three CI/CD platforms execute the same logical stages:

1. **Compute Version** : generates a unique image tag encoding branch, build number, and short commit SHA
2. **Build** : Maven (`mvn clean package`) for backend services, `npm ci && npm run build` for frontend
3. **Docker Build & Push** : builds and pushes the image to Docker Hub, tags `latest` on `main`
4. **Deploy** : Helm upgrade/install into the target Kubernetes namespace, `dev` branch → `dev` namespace, `main` branch → `prod` namespace
5. **Manual Approval Gate** : production deployments require explicit human approval before Helm runs

### Version Format

| Branch | Example Tag |
|---|---|
| `dev` | `1.0.47-dev-a3f9c12` |
| `main` | `1.0.47-a3f9c12` |
| Feature branch | `1.0.47-feature-login-a3f9c12` |

---

## Shared Pipeline Repositories

This monorepo consumes pipeline logic from three separate repositories:

| Platform | Shared Repo |
|---|---|
| Jenkins | [jenkins-shared-library](https://github.com/rouisskhawla/jenkins-shared-library) |
| GitHub Actions | [github-shared-workflow](https://github.com/rouisskhawla/github-shared-workflow) |
| GitLab CI | [gitlab-shared-template](https://github.com/rouisskhawla/gitlab-shared-template) |

---

## Jenkins Setup

Each service's `Jenkinsfile` pins to a versioned release of the shared library:

```groovy
@Library('jenkins-shared-library@v1.0.16') _

buildPipeline(
    serviceDir:  'services/api-gateway',
    serviceName: 'api-gateway',
    imageName:   'username/ci-cd-gateway'
)
```

The library is registred in Jenkins under **Manage Jenkins → System → Global Pipeline Libraries** pointing to the `jenkins-shared-library` repository.

---

## GitHub Actions Setup

Each service has a caller workflow under `.github/workflows/`. A `paths:` filter ensures only the relevant service pipeline triggers on each push:

```yaml
on:
  push:
    branches: [main, dev]
    paths:
      - 'services/api-gateway/**'

jobs:
  pipeline:
    uses: username/github-shared-workflow/.github/workflows/ci-cd-pipeline.yml@main
    with:
      service-name: api-gateway
      docker-image: username/ci-cd-gateway
      service-dir: services/api-gateway
    secrets:
      DOCKER_USERNAME: ${{ secrets.DOCKER_USERNAME }}
      DOCKER_PASSWORD: ${{ secrets.DOCKER_PASSWORD }}
      KUBECONFIG_DEV:  ${{ secrets.KUBECONFIG_DEV }}
      KUBECONFIG_PROD: ${{ secrets.KUBECONFIG_PROD }}
```

The self-hosted runner and all secrets are configured in this repository.

---

## GitLab CI Setup

The root `.gitlab-ci.yml` includes each service's pipeline file with a `rules: changes:` filter. Service pipelines include the shared template and extend its job definitions:

```yaml
# Root pipeline
include:
  - local: '/services/api-gateway/.gitlab-ci.yml'
    rules:
      - changes:
          - services/api-gateway/**/*
```

---

## Required Secrets / CI Variables

These secrets are configured in each CI/CD platform (GitHub repository secrets, Jenkins credentials or GitLab CI/CD variables):

| Variable | Description |
|---|---|
| `DOCKER_USERNAME` | Docker Hub username |
| `DOCKER_PASSWORD` | Docker Hub password or access token |
| `KUBECONFIG_DEV` | kubeconfig file content for the dev cluster |
| `KUBECONFIG_PROD` | kubeconfig file content for the prod cluster |

---

## Helm Chart

All four services share the single chart at `charts/microservice/`. Each service provides its own values files under `helm-values/<service-name>/`. The chart is a templateand the values files are the per-service configuration.

```bash
# Example: deploy api-gateway to dev
helm upgrade --install api-gateway charts/microservice \
    -f helm-values/api-gateway/values-dev.yaml \
    --set global.imageTag=1.0.47-dev-a3f9c12 \
    --namespace dev \
    --create-namespace
```

---

## Related Repositories

- [jenkins-shared-library](https://github.com/rouisskhawla/jenkins-shared-library) — Groovy shared library for Jenkins
- [github-shared-workflow](https://github.com/rouisskhawla/github-shared-workflow) — reusable workflow for GitHub Actions
- [gitlab-shared-template](https://github.com/rouisskhawla/gitlab-shared-template) — job templates for GitLab CI