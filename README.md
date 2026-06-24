# Patient Management — Spring Boot + PostgreSQL + GKE

Multi-tier patient CRUD microservice for the Kubernetes assignment.

## Architecture

```
                    Internet
                        |
                    Ingress
                        |
                Spring Boot API
                  (2-4 replicas)
                        |
              ClusterIP Service
              (postgres-service)
                        |
                    PostgreSQL
                     (1 pod)
                   StatefulSet
                        |
              PVC (volumeClaimTemplate)
```

| Kubernetes concept | How it is demonstrated                                     |
|--------------------|------------------------------------------------------------|
| DB inside cluster | PostgreSQL StatefulSet in `patient-management` namespace   |
| Persistence | `volumeClaimTemplates` → PVC `postgres-storage-postgres-0` |
| ConfigMap | `api-config`, `postgres-config`                            |
| Secret | `db-secret` (password never in YAML)                       |
| Self-healing | Deployment + StatefulSet controllers recreate deleted pods |
| Rolling updates | API Deployment `RollingUpdate` strategy                    |
| HPA | `patient-api-hpa` (2–4 replicas)                           |
| Internal DB only | `postgres-service` is ClusterIP (not exposed via Ingress)  |
| External API | Ingress → `patient-api-service`                            |

## Tech Stack

| Layer | Technology |
|-------|------------|
| API | Java 21, Spring Boot 3.5, Spring Data JPA, HikariCP, Actuator, OpenAPI/Swagger |
| Database | PostgreSQL 16 (in-cluster) |
| Container | Docker multi-stage build |
| Orchestration | GKE — Deployment, **StatefulSet**, PVC, ConfigMap, Secret, HPA, Ingress |

## Links

| Item | URL                                                           |
|------|---------------------------------------------------------------|
| GitHub Repository | `https://github.com/vikas-jagga/patient-management`           |
| Docker Hub Image | `https://hub.docker.com/r/vikaskumarjagga/patient-management` |
| Service API (Ingress) | `http://34.45.149.97//api/patients`                        |

## Folder Structure

```
patient-management/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── README.md
├── DOCUMENTATION.md
├── src/main/java/com/assignment/patient/
│   ├── PatientManagementApplication.java
│   ├── config/
│   ├── controller/
│   ├── model/Patient.java
│   ├── repository/
│   └── service/
├── src/main/resources/
│   ├── application.yml          # shared config, no passwords
│   ├── application-local.yml    # local Docker Postgres (profile: local)
│   └── application-gcp.yml      # Cloud SQL via Auth Proxy (profile: gcp)
├── k8s/
│   ├── namespace.yaml
│   ├── configmap-api.yaml
│   ├── configmap-db.yaml
│   ├── secret-db.example.yaml
│   ├── statefulset-db.yaml
│   ├── service-db-headless.yaml
│   ├── service-db.yaml
│   ├── deployment-api.yaml
│   ├── service-api.yaml
│   ├── ingress-api.yaml
│   ├── hpa-api.yaml
│   └── finops-optimized-api-resources.yaml
└── scripts/
    ├── setup-gke.ps1
    ├── build-and-push.ps1
    └── deploy-k8s.ps1
```

## Patient Entity (5 attributes)

| Field | Type | Example |
|-------|------|---------|
| firstName | String | Rahul |
| lastName | String | Sharma |
| age | Integer | 34 |
| gender | String | Male |
| diagnosis | String | Hypertension |

8 seed patients are inserted on first startup.

## REST API

| Method | Endpoint |
|--------|----------|
| GET | `/api/patients` |
| GET | `/api/patients/{id}` |
| POST | `/api/patients` |
| PUT | `/api/patients/{id}` |
| DELETE | `/api/patients/{id}` |
| GET | `/actuator/health` |
| GET | `/swagger-ui.html` | OpenAPI UI |
| GET | `/api-docs` | OpenAPI JSON |

Example POST body:

```json
{
  "firstName": "Arjun",
  "lastName": "Verma",
  "age": 41,
  "gender": "Male",
  "diagnosis": "Back Pain"
}
```

---

## Configuration profiles

| File | When it is used | How to activate |
|------|-----------------|-----------------|
| `application.yml` | Always (base config) | Loaded automatically; **no passwords** |
| `application-local.yml` | Local dev with Docker Postgres | `-Dspring-boot.run.profiles=local` |
| `application-gcp.yml` | Optional: Cloud SQL from laptop | `-Dspring-boot.run.profiles=gcp` + `DB_PASSWORD` env |
| *(none — env vars only)* | **GKE / Kubernetes** | ConfigMap + Secret inject `DB_*` env vars |

`application-gcp.yml` is **not** used on GKE. The assignment DB runs **inside the cluster** (StatefulSet); Kubernetes supplies connection settings via ConfigMap/Secret, which `application.yml` reads.

---

## Step 1: Run Locally

```powershell
cd C:\Users\vikas\patient-management
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=local
curl http://localhost:8080/api/patients
```

`application-local.yml` connects to `localhost:5432` with the same credentials as `docker-compose.yml`.

---

## Step 2: Google Cloud SQL (optional — not used for K8s assignment)

Use this only if you want to test against **Cloud SQL from your laptop** (Auth Proxy). It does **not** deploy to GKE.

1. Create PostgreSQL instance in GCP Console (free tier: `db-f1-micro`).
2. Create database `patientdb` and user `patientuser`.
3. Run Cloud SQL Auth Proxy:

```powershell
cloud-sql-proxy patient-management-500306:YOUR_REGION:YOUR_INSTANCE --port 5432
```

4. Activate the **`gcp`** profile (loads `application-gcp.yml`):

```powershell
$env:DB_PASSWORD="your-cloud-sql-password"
mvn spring-boot:run -Dspring-boot.run.profiles=gcp
```

---

## Step 3: Build & Push to Docker Hub

Docker Hub username: **vikaskumarjagga**

```powershell
.\scripts\build-and-push.ps1 -ImageTag 1.0.0
```

Image: `vikaskumarjagga/patient-management:1.0.0`

---

## Step 4: Deploy on GKE

### 4.1 Create GKE cluster (one time)

Edit `scripts/setup-gke.ps1` if needed (project ID is already set to `patient-management-500306`), then:

```powershell
.\scripts\setup-gke.ps1
```

Or manually:

```powershell
gcloud config set project patient-management-500306

gcloud container clusters create patient-management-cluster `
  --zone us-central1-a `
  --num-nodes 2 `
  --machine-type e2-medium

gcloud container clusters get-credentials patient-management-cluster --zone us-central1-a

kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.11.3/deploy/static/provider/cloud/deploy.yaml
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

### 4.2 Deploy application

```powershell
.\scripts\deploy-k8s.ps1
```

### 4.3 Get external URL

```powershell
kubectl get ingress -n patient-management
```

Use the **ADDRESS** (GKE LoadBalancer IP):

Then: `http://34.45.149.97//api/patients`

---

## Step 5: Assignment Demo (screen recording)

```
kubectl get all,pvc,ingress,hpa,configmap -n patient-management
curl http://34.45.149.97/api/patients

# Self-healing API pod
kubectl delete pod -n patient-management -l app=patient-api
kubectl get pods -n patient-management -w

# Self-healing DB + persistence (StatefulSet)
kubectl get statefulset,pvc -n patient-management
kubectl delete pod postgres-0 -n patient-management
kubectl wait --for=condition=ready pod/postgres-0 -n patient-management --timeout=120s
curl http://34.45.149.97/api/patients

# Rolling update
kubectl set image deployment/patient-api-deployment patient-api=vikaskumarjagga/patient-management:1.0.1 -n patient-management
kubectl rollout status deployment/patient-api-deployment -n patient-management

# FinOps
kubectl top pods -n patient-management
kubectl get hpa -n patient-management
kubectl apply -f k8s/finops-optimized-api-resources.yaml
```

---

## Kubernetes Summary

| Requirement | Implementation                                      |
|-------------|-----------------------------------------------------|
| Docker image | `vikaskumarjagga/patient-management:1.0.0`          |
| GKE storage | `standard-rwo` PVC                                  |
| API exposed | Ingress (nginx)                                     |
| DB internal | ClusterIP `postgres-service`                        |
| API pods | 2 replicas + HPA (2–4)                              |
| DB pod | 1 StatefulSet replica + PVC via volumeClaimTemplate |
| ConfigMap | `api-config`, `postgres-config`                     |
| Secret | `db-secret` (kubectl, not plain YAML)               |
| FinOps | `finops-optimized-api-resources.yaml`               |

See `DOCUMENTATION.md` for full assignment documentation.

**Delete cluster after submission:**

```powershell
gcloud container clusters delete patient-management-cluster --zone us-central1-a
```
