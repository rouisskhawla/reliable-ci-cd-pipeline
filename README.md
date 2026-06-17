# reliable-ci-cd-pipeline

![CI CD Pipeline Api Gateway](https://github.com/rouisskhawla/reliable-ci-cd-pipeline/actions/workflows/api-gateway-ci-cd.yml/badge.svg)
![CI CD Pipeline Authors](https://github.com/rouisskhawla/reliable-ci-cd-pipeline/actions/workflows/authors-ci-cd.yml/badge.svg)
![CI CD Pipeline Books](https://github.com/rouisskhawla/reliable-ci-cd-pipeline/actions/workflows/books-ci-cd.yml/badge.svg)
![CI CD Pipeline Bookstore Frontend](https://github.com/rouisskhawla/reliable-ci-cd-pipeline/actions/workflows/bookstore-frontend-ci-cd.yml/badge.svg)

A cross-platform CI/CD monorepo demonstrating reusable pipeline patterns across **Jenkins**, **GitHub Actions**, and **GitLab CI**, with Docker image builds, Helm chart deployments, Kubernetes rollouts, automated testing, and Slack notifications.

This repository is the application side of the pattern. The pipeline logic is defined in separate shared repositories. Each service only declares what it is and passes parameters to the shared definition.

---

## Repository Structure

```

reliable-ci-cd-pipeline/
├── services/
│   ├── api-gateway/
│       ├── src/
│       ├── pom.xml
│       ├── Dockerfile
│       ├── .gitlab-ci.yml
│       ├── Jenkinsfile
│   ├── authors-service/
│   ├── books-service/
│   └── bookstore-frontend/
├── charts/
│   └── microservice/
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
│   └── workflows/
└── .gitlab-ci.yml

````

Each service directory contains its own `Jenkinsfile` and `.gitlab-ci.yml`. GitHub Actions workflows live under `.github/workflows/`.

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

All CI/CD platforms execute the same logical stages:

1. **Compute Version**
   - Generates a unique image tag using branch, build number, and commit SHA

2. **Build**
   - Backend: `mvn clean package`
   - Frontend: `npm ci && npm run build`

3. **Test**
   - Backend: `mvn test`
   - Frontend: `npm ci && npm run test`

4. **Docker Build & Push**
   - Builds Docker image
   - Pushes to Docker Hub
   - Tags `latest` on `main`

5. **Deploy (Helm)**
   - `dev` branch → `dev` namespace
   - `main` branch → `prod` namespace

6. **Manual Approval Gate**
   - Required for production deployments

7. **Slack Notification**
   - Sends pipeline result to Slack channel
   - Runs on success or failure

---

## Version Format

| Branch | Example Tag |
|---|---|
| `dev` | `1.0.47-dev-a3f9c12` |
| `main` | `1.0.47-a3f9c12` |
| Feature branch | `1.0.47-feature-login-a3f9c12` |

---

## Shared Pipeline Repositories

Pipeline logic is centralized in shared repositories:

| Platform | Shared Repo |
|---|---|
| Jenkins | https://github.com/rouisskhawla/jenkins-shared-library |
| GitHub Actions | https://github.com/rouisskhawla/github-shared-workflow |
| GitLab CI | https://github.com/rouisskhawla/gitlab-shared-template |

---

## Jenkins Setup

Each service uses a shared Jenkins library:

```groovy
@Library('jenkins-shared-library@v1.0.16') _

buildPipeline(
    serviceDir:  'services/api-gateway',
    serviceName: 'api-gateway',
    imageName:   'username/ci-cd-gateway'
)
```

The library is registered under:
**Manage Jenkins → System → Global Pipeline Libraries**

---

## GitHub Actions Setup

Each service uses a reusable workflow:

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
      SLACK_BOT_TOKEN: ${{ secrets.SLACK_BOT_TOKEN }}
      SLACK_CHANNEL_ID: ${{ secrets.SLACK_CHANNEL_ID }}
```

The self-hosted runner and all secrets are configured in this repository.

---

## GitLab CI Setup

Root pipeline includes service pipelines:

```yaml
include:
  - local: '/services/api-gateway/.gitlab-ci.yml'
    rules:
      - changes:
          - services/api-gateway/**/*
```

Each service pipeline extends shared templates.

---

## Required Secrets / CI Variables

All CI/CD platforms require:

| Variable           | Description                  |
| ------------------ | ---------------------------- |
| `DOCKER_USERNAME`  | Docker Hub username          |
| `DOCKER_PASSWORD`  | Docker Hub password or token |
| `KUBECONFIG_DEV`   | kubeconfig for dev cluster   |
| `KUBECONFIG_PROD`  | kubeconfig for prod cluster  |
| `SLACK_BOT_TOKEN`  | Slack bot token              |
| `SLACK_CHANNEL_ID` | Slack channel ID             |

---

## Slack Notifications

Slack notifications are sent after pipeline execution.

### Behavior

* Runs after all stages complete
* Triggered on success or failure
* Includes service, branch, status, env and version

* Uses Slack Web API (`chat.postMessage`)

---

## Helm Chart

All services share a single Helm chart:

```
charts/microservice/
```

Each service provides its own values:

```
helm-values/<service-name>/values-dev.yaml
helm-values/<service-name>/values-prod.yaml
```

Example deployment:

```bash
helm upgrade --install api-gateway charts/microservice \
  -f helm-values/api-gateway/values-dev.yaml \
  --set global.imageTag=1.0.47-dev-a3f9c12 \
  --namespace dev \
  --create-namespace
```

---

## Pipeline Flow

```
Compute Version
      ↓
Build
      ↓
Test
      ↓
Docker Build & Push
      ↓
Deploy (Helm)
      ↓
Manual Approval (prod)
      ↓
Slack Notification
```

---

## Related Repositories

* [https://github.com/rouisskhawla/jenkins-shared-library](https://github.com/rouisskhawla/jenkins-shared-library)
* [https://github.com/rouisskhawla/github-shared-workflow](https://github.com/rouisskhawla/github-shared-workflow)
* [https://github.com/rouisskhawla/gitlab-shared-template](https://github.com/rouisskhawla/gitlab-shared-template)
