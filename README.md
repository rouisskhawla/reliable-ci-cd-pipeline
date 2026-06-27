# reliable-ci-cd-pipeline

![CI CD Pipeline Api Gateway](https://github.com/rouisskhawla/reliable-ci-cd-pipeline/actions/workflows/api-gateway-ci-cd.yml/badge.svg)
![CI CD Pipeline Authors](https://github.com/rouisskhawla/reliable-ci-cd-pipeline/actions/workflows/authors-ci-cd.yml/badge.svg)
![CI CD Pipeline Books](https://github.com/rouisskhawla/reliable-ci-cd-pipeline/actions/workflows/books-ci-cd.yml/badge.svg)
![CI CD Pipeline Bookstore Frontend](https://github.com/rouisskhawla/reliable-ci-cd-pipeline/actions/workflows/bookstore-frontend-ci-cd.yml/badge.svg)

A Kubernetes-based monorepo containing four microservices with a fully automated CI/CD pipeline powered by GitHub Actions, Docker, and Helm. Infrastructure is provisioned and managed separately via Terraform. This repository contains only application code and pipeline configuration.

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
| `api-gateway` | Backend | Maven / JDK 17 |
| `authors-service` | Backend | Maven / JDK 17 |
| `books-service` | Backend | Maven / JDK 17 |
| `bookstore-frontend` | Frontend | Node.js 24 / npm |

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

## GitHub Actions Setup

Each service uses a reusable workflow:

```yaml
# .github/workflows/api-gateway-ci-cd.yml
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

A push that only touches `services/books-service/` will not trigger any other service's pipeline.

### Pipeline Stages

```
Build & Test
     ↓
Docker Build & Push
     ↓
Deploy via Helm
     ↓
Manual Approval (prod only)
     ↓
Slack Notification
```

### Branch Environment Mapping

| Branch | Namespace | Approval required |
|---|---|---|
| `dev` | `dev` | No |
| `main` | `prod` | Yes — GitHub Environment gate |

### Image Version Format

| Branch | Example tag |
|---|---|
| `dev` | `1.0.47-dev-a3f9c12` |
| `main` | `1.0.47-a3f9c12` |

---

## Helm Chart

All services share one chart located at `charts/microservice/`. Each service provides its own values per environment under `helm-values/<service-name>/`.

Example deploy command (run by the shared workflow):

```bash
helm upgrade --install api-gateway charts/microservice \
  -f helm-values/api-gateway/values-dev.yaml \
  --set global.imageTag=1.0.47-dev-a3f9c12 \
  --namespace dev \
  --create-namespace
```

---

## Infrastructure

Cluster infrastructure: namespaces, RBAC, NGINX Ingress Controller, TLS certificates, and the GitHub Actions service account is managed by Terraform in a separate repository:

**[devops-infrastructure-terraform](https://github.com/rouisskhawla/devops-infrastructure-terraform)**

Terraform provisions:
- The `dev` and `prod` namespaces on the Kubernetes clusters
- A scoped `github-actions` ServiceAccount with the minimum permissions needed for Helm deploys
- NGINX Ingress Controller (via Helm) with hostNetwork binding on each cluster node
- Self-signed TLS certificates for `*.bookstore.com` domains, stored as Kubernetes TLS secrets
- `KUBECONFIG_DEV` and `KUBECONFIG_PROD` secrets in this repository: pushed automatically after each Terraform apply, no manual copy-paste

The `KUBECONFIG_DEV` and `KUBECONFIG_PROD` secrets used by this pipeline are generated and rotated by Terraform. They contain a scoped kubeconfig for the `github-actions` ServiceAccount, not admin credentials.

---

## Required Secrets

These must exist as repository secrets in this repo. `KUBECONFIG_DEV` and `KUBECONFIG_PROD` are managed automatically by Terraform, the rest are set manually once:

| Secret | Description | Managed by |
|---|---|---|
| `DOCKER_USERNAME` | Docker Hub username | Manual |
| `DOCKER_PASSWORD` | Docker Hub token | Manual |
| `KUBECONFIG_DEV` | Scoped kubeconfig for dev cluster | Terraform (automatic) |
| `KUBECONFIG_PROD` | Scoped kubeconfig for prod cluster | Terraform (automatic) |
| `SLACK_BOT_TOKEN` | Slack bot token | Manual |
| `SLACK_CHANNEL_ID` | Slack channel ID | Manual |

---

## Related Repositories

| Repository | Purpose |
|---|---|
| [github-shared-workflow](https://github.com/rouisskhawla/github-shared-workflow) | Reusable GitHub Actions pipeline called by all services |
| [devops-infrastructure-terraform](https://github.com/rouisskhawla/devops-infrastructure-terraform) | Terraform infrastructure for dev and prod clusters |
| [jenkins-shared-library](https://github.com/rouisskhawla/jenkins-shared-library) | Equivalent shared pipeline for Jenkins |
| [gitlab-shared-template](https://github.com/rouisskhawla/gitlab-shared-template) | Equivalent shared pipeline for GitLab CI |