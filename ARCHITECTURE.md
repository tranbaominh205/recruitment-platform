# ARCHITECTURE — Recruitment Platform

## 1. High-Level Architecture

External client:

React Web Application

communicates with:

Spring Cloud API Gateway

which routes to:

1. Identity Service
2. Candidate Service
3. Employer Service
4. Job Service
5. Resume Service
6. Recruitment Service
7. Matching Service
8. Notification Service

Do not add extra microservices during P0.

---

# 2. Technology Baseline — LOCKED

Backend:

- Java 25 LTS
- Spring Boot 4.0.8
- Spring Cloud 2025.1.3
- Spring AI 2.0.1
- Maven >= 3.9.5

Current local JDK:

Microsoft OpenJDK 25.0.4.1

Frontend:

React

Use Vite when frontend initialization begins.

---

# 3. Infrastructure

Docker Compose infrastructure:

- MySQL 8.4.11
- MongoDB 8.0.29
- Apache Kafka 4.2.1
- MinIO
- Redis: deferred until P1/actual use

Kafka operates in single-node KRaft mode.

No ZooKeeper.

---

# 4. Database Ownership

## MySQL

Separate schemas on the MySQL server:

### Identity Service

`identity_db`

### Candidate Service

`candidate_db`

### Employer Service

`employer_db`

### Job Service

`job_db`

### Recruitment Service

`recruitment_db`

Services must not directly manipulate another service's schema.

---

# 5. MongoDB Ownership

## Resume Service

`resume_db`

Stores resume metadata and parsed resume structures as appropriate.

CV binary is NOT stored directly in Mongo.

Binary is stored in MinIO.

## Matching Service

`matching_db`

Stores matching results / structured matching-related documents.

## Notification Service

`notification_db`

Stores notification documents.

---

# 6. MinIO

MinIO stores CV binary files.

Resume Service owns MinIO access.

A CV upload must create a unique object/storage key.

Do not allow a later upload to overwrite the binary referenced by an old Application.

---

# 7. Service Communication

## Synchronous

Use REST.

Spring OpenFeign may be used for synchronous service-to-service business communication where appropriate.

Exception:

API Gateway is reactive WebFlux.

Do NOT introduce blocking OpenFeign calls into the WebFlux event loop for Gateway authentication.

Gateway currently uses reactive WebClient-style communication to Identity.

## Asynchronous

Use Apache Kafka.

Do NOT introduce RabbitMQ.

---

# 8. Important Kafka Events

Important event topics/concepts:

- `resume.uploaded`
- `resume.analyzed`
- `job.published`
- `job.updated`
- `job.closed`
- `application.submitted`
- `application.status.changed`
- `interview.scheduled`

Do not create Kafka events without a real asynchronous use case.

---

# 9. API Gateway

Current gateway port:

8888

Public API prefix:

`/api/v1`

Example public request:

`POST /api/v1/identity/auth/login`

Gateway removes public version prefix before forwarding to downstream Identity.

Example:

Client:

`/api/v1/identity/auth/login`

Gateway:

StripPrefix = 2

Identity receives:

`/identity/auth/login`

API versioning is managed at the public Gateway boundary.

Do not duplicate `/api/v1` inside every service controller.

---

# 10. Identity Service

Current service port:

8081

Current responsibilities:

- account persistence;
- registration;
- password hashing;
- login;
- JWT issuance;
- token introspection;
- refresh rotation + replay-safe single-use refresh;
- logout/revocation via invalidated JTI registry;
- roles;
- account-level authorization;
- authoritative current-account endpoint (`/identity/me`);
- self password change with account-wide token invalidation via `tokenVersion`;
- environment-driven initial ADMIN bootstrap capability;
- Admin account management APIs for paginated/filterable account reads,
  account detail, enable/disable mutation with tokenVersion bump and self-disable
  protection, and account statistics.

Roles:

- CANDIDATE
- RECRUITER
- ADMIN

Public self-registration must NOT permit ADMIN.

---

# 11. JWT Contract

JWT access token uses HS256 in V1.

Claims:

- `sub` = accountId
- `email`
- `role`
- `tokenVersion`
- `iat`
- `exp`

Issuer:

`identity-service`

Current access-token lifetime:

3600 seconds

JWT signing secret comes from environment configuration.

Never hardcode a real signing secret.

Never log raw JWT tokens.

---

# 12. Authentication Architecture

Gateway is the primary authentication boundary.

Typical flow:

Client
-> Gateway
-> extract Bearer token
-> call Identity introspection
-> valid?
-> forward request

Public routes bypass authentication.

Protected routes:

missing token
-> 401

invalid token
-> 401

Identity unavailable during authentication:
-> 503

Gateway may propagate trusted identity context such as:

- X-Account-Id
- X-Account-Email
- X-Account-Role

Gateway MUST overwrite externally supplied values of trusted headers.

Never trust client-supplied identity headers.

---

# 13. Authorization Architecture

Business services own authorization.

Examples:

Identity Service:

`/identity/admin/**`
-> ADMIN

Employer/Job/Recruitment/Resume/Matching/Notification Services:

`/service/admin/**`
-> ADMIN via trusted gateway headers and service-level role checks

Candidate Service:

candidate profile modification
-> authenticated owner / CANDIDATE

Job Service:

job creation/management
-> appropriate recruiter/company authorization

Recruitment Service:

application status changes
-> recruiter must be authorized for related employer/job

Detailed ownership/business authorization must NOT be centralized in Gateway.

POST-P0 Phase B Admin backend:

- Gateway remains the authentication boundary and propagates trusted
  `X-Account-Id`/`X-Account-Role` headers.
- Owning business services enforce Admin authorization for `/admin/**` entry
  points by requiring a valid UUID `X-Account-Id` and `X-Account-Role=ADMIN`.
- Admin APIs are read-only across business services; only Identity Admin may
  mutate account enabled state.
- Admin list APIs use 0-based pagination with default `size=20` and max
  `size=100`.
- Services must not query other services' databases for Admin features.

---

# 14. Error Response Convention

Use meaningful HTTP statuses.

Do NOT return HTTP 200 for every error.

Application response shape:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {}
}
```
```markdown
Error code ranges:

- Identity: `1xxx`
- Candidate: `2xxx`
- Employer: `3xxx`
- Job: `4xxx`
- Resume: `5xxx`
- Recruitment: `6xxx`
- Matching: `7xxx`
- Notification: `8xxx`
- Gateway: `9xxx`

Success code:

`1000`

Existing service conventions take precedence over inventing new response shapes.

---

# 15. Introspection Contract

Identity endpoint:

`POST /identity/auth/introspect`

Valid token:

HTTP `200`

with:

`result.valid = true`

Invalid, expired, malformed token:

HTTP `200`

with:

`result.valid = false`

Identity introspection is authoritative against the account in `identity_db`:

- token JTI must not be revoked;
- account identified by `sub` must exist and be enabled;
- JWT `tokenVersion` claim must exist, be numeric, and match current `Account.tokenVersion`.

Reason:

The introspection request itself succeeded even if the provided token is invalid.

Gateway converts invalid authentication into the appropriate `401` response.

---

# 16. Candidate Domain Boundary

Candidate Service owns `CandidateProfile` and stable candidate preferences.

Candidate Service must NOT become CV storage.

Resume Service owns resumes.

---

# 17. Employer Domain Boundary

Employer Service owns employer/company-related data.

Do not put employer/company data directly inside Identity account entities.

Identity owns account identity only.

Employer Admin supports read-only company list/detail/statistics.

---

# 18. Job Domain Boundary

Job Service owns `Job` entities and job lifecycle.

Initial job search uses MySQL.

Do not introduce Elasticsearch before P0 completion.

Job Admin supports read-only job list/detail/statistics across
`DRAFT/PUBLISHED/CLOSED` without changing public published-job behavior.

---

# 19. Resume Domain Boundary

Resume Service owns:

- resume metadata;
- ownership;
- binary storage coordination;
- resume upload lifecycle.

Candidate may own multiple resumes.

Each upload is immutable in V1.

Resume Admin is metadata-only (no unrestricted Admin binary/CV download).

---

# 20. Recruitment Domain Boundary

Recruitment Service owns:

- `Application`;
- application lifecycle/status;
- selected `resumeId`;
- interview-related recruitment workflow.

Application must retain exact:

- `candidateId`;
- `jobId`;
- `resumeId`;

from submission.

Recruitment Admin provides read-only application/interview list/detail/statistics
and does not mutate Application status.

---

# 21. Matching Domain Boundary

Matching Service owns Resume–Job matching.

Stack:

- Spring Boot;
- Spring AI;
- PDFBox;
- MongoDB;
- Kafka.

AI extracts structured resume information.

Java deterministic scoring produces recruiter score.

Locked weights:

- Skills: `55%`;
- Experience: `25%`;
- Education: `10%`;
- Title/domain: `10%`.

Candidate preference scoring is separate and not included.

Matching Admin reads stored match results only and does not trigger parsing,
Gemini calls, or new match generation.

---

# 22. Notification Domain Boundary

Notification Service:

- Kafka consumer(s);
- in-app notification persistence;
- MongoDB.

P0 priority:

in-app notifications.

Do not turn Notification into an email-only service.

Notification Admin supports read-only notification list/detail/statistics and
does not create arbitrary notifications.

---

# 23. AI Provider Plan

Planned Day 5 provider:

Google GenAI Gemini Developer API.

Spring AI dependency family:

`spring-ai-starter-model-google-genai`

Planned model:

Gemini 2.5 Flash

Use structured output where appropriate.

Do not implement AI before Resume/Application core is working.

---

# 24. Service Discovery

No Eureka.

During the 7-day P0 project, service URLs are configured explicitly through environment/application configuration.

Do not introduce service discovery unless architecture is deliberately revised after P0.

---
```
