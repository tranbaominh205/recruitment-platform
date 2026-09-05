# CURRENT CHECKPOINT

Last updated:

2026-09-03

Project:

Recruitment Platform Capstone

Current phase:

DAY 3 DONE / DAY 4 READY

---

# 1. Critical Rule for the Next Chat

Before generating any Day 4 implementation code:

1. Read:
    - `MASTER_PROMPT.md`
    - `PROJECT_CONTEXT.md`
    - `ARCHITECTURE.md`
    - `DEVELOPMENT_GUIDE.md`
    - `CURRENT_CHECKPOINT.md`
2. Inspect the actual GitHub `main` branch.
3. Do NOT assume repository state only from this checkpoint.
4. Source code on `main` is implementation truth.
5. If this checkpoint differs from actual source, use actual source to determine implementation state and explicitly report the inconsistency.
6. Do not silently change frozen architecture/domain constraints.
7. Confirm Day 3 source and quality/regression state before starting Day 4.

Known Day 3 final implementation merge:

`87677f173867c61e0cbc601ca0d9b274b9143040`

Commit message:

`feat(recruitment): add application tracking and recruiter reads`

Day 3 implementation history also includes:

- `3028f3f0e18e320e9ec5ae650c1d63317f4fec0b`
    - Resume Service foundation
    - MongoDB Resume metadata
    - MinIO upload

- `0adf2dbb4ca6249006b737ad75e2ec15045d863d`
    - Resume list/detail
    - secure Resume download
    - ownership enforcement

- `1232bd9fd510f77c3a3e62b931dce207c5be2d63`
    - Recruitment Service foundation
    - Application submission
    - exact selected `resumeId`

- `87677f173867c61e0cbc601ca0d9b274b9143040`
    - Candidate Application tracking
    - recruiter Application reads
    - recruiter Job ownership verification

---

# 2. Current Maven Modules

Implemented modules:

- `identity-service`
- `api-gateway`
- `candidate-service`
- `employer-service`
- `job-service`
- `resume-service`
- `recruitment-service`

Not implemented yet:

- `matching-service`
- `notification-service`

Do not create additional microservices outside the frozen architecture.

---

# 3. Locked Technology Baseline

Keep the versions in the current root `pom.xml`.

Known baseline:

- Java 25
- Spring Boot 4.0.8
- Spring Cloud 2025.1.3
- Spring AI 2.0.1
- Lombok 1.18.46
- MapStruct 1.6.3
- MinIO Java SDK 9.0.3
- Spotless Maven Plugin 3.10.0
- MySQL 8.4.11
- MongoDB 8.0.29
- Kafka 4.2.1
- MinIO

Do not downgrade versions based on older tutorials.

---

# 4. Current Ports

- API Gateway: `8888`
- Identity Service: `8081`
- Candidate Service: `8082`
- Employer Service: `8083`
- Job Service: `8084`
- Resume Service: `8085`
- Recruitment Service: `8086`

External business API:

`/api/v1/**`

Downstream services do not duplicate `/api/v1`.

---

# 5. Current Persistence Ownership

## MySQL

Identity Service:

`identity_db`

Candidate Service:

`candidate_db`

Employer Service:

`employer_db`

Job Service:

`job_db`

Recruitment Service:

`recruitment_db`

## MongoDB

Resume Service:

`resume_db`

## MinIO

Resume Service owns Resume/CV binary storage.

Default bucket:

`resumes`

Important:

No service may directly query/manipulate another service's database/schema.

Cross-service business data must be obtained through supported service APIs or an architecture-approved asynchronous mechanism.

---

# 6. Authentication and Authorization

Authentication boundary:

API Gateway.

Gateway:

- validates/introspects JWT;
- resolves authenticated account;
- overwrites trusted identity headers;
- forwards:
    - `X-Account-Id`
    - `X-Account-Email`
    - `X-Account-Role`

Business authorization/ownership remains inside the owning business service.

Do not trust ownership IDs supplied arbitrarily by the frontend.

Normal business API tests use Gateway.

Direct downstream calls are internal/debug only.

---

# 7. DAY 1 — COMPLETE

Implemented:

## Identity

- Candidate/Recruiter registration
- public ADMIN registration blocked
- login
- BCrypt
- JWT
- introspection
- current account
- admin account listing
- role authorization
- MapStruct
- password hash not exposed

## Gateway

- Identity routing
- JWT authentication
- Identity introspection
- trusted identity headers
- public/protected endpoint handling
- spoofed trusted headers overwritten

Day 1 is closed.

---

# 8. DAY 2 — COMPLETE

## Candidate Service

Implemented:

- `CandidateProfile`
- separate CandidateProfile UUID
- Identity `accountId` reference
- create/get/update own profile
- Candidate-only ownership
- candidate preferences:
    - `desiredJobTitles`
    - `preferredLocations`
    - `employmentTypes`
    - `workplaceTypes`

CandidateProfile does NOT contain Resume/CV contents.

## Employer Service

Implemented:

- `Company`
- recruiter ownership
- create/get/update own Company
- recruiter cannot manage another recruiter's Company

## Job Service

Implemented:

- Job creation
- initial `DRAFT`
- update DRAFT
- `DRAFT -> PUBLISHED`
- `PUBLISHED -> CLOSED`
- recruiter own-job list
- public PUBLISHED-job search
- filtering
- pagination
- ownership through Company
- Job Service does not directly query Employer DB

Gateway public search rule:

Only method-aware:

`GET /job/search`

is public.

Day 2 is closed.

Known Day 2 completion merge:

`6d0c63ea024a6570c906583bb212d3873202a319`

---

# 9. DAY 3 — COMPLETE

Day 3 goal:

Candidate can apply to a Job using one explicitly selected Resume.

Implemented successfully at source level:

- Resume Service
- MongoDB Resume metadata
- MinIO binary storage
- multiple Resume uploads
- unique Resume UUID per upload
- unique immutable storage key
- Resume list
- Resume metadata detail
- secure Resume download
- Resume Candidate ownership
- Recruitment Service
- Application persistence
- selected `resumeId`
- initial `SUBMITTED` status
- Candidate Application tracking
- recruiter Application listing
- Candidate/recruiter Application detail authorization

Day 3 contains no Kafka implementation.

---

# 10. Resume Domain — Current State

Resume belongs to Resume Service.

MongoDB metadata contains fields such as:

- `id`
- `ownerAccountId`
- `displayName`
- `originalFileName`
- `contentType`
- `size`
- `storageKey`
- `status`
- `createdAt`

Binary is stored in MinIO, not MongoDB.

Each upload creates:

- new Resume UUID
- new storage key
- new immutable binary object

Uploading a newer Resume must NOT overwrite a Resume referenced by an older Application.

Current Candidate Resume APIs:

`POST /api/v1/resume`

`GET /api/v1/resume`

`GET /api/v1/resume/{resumeId}`

`GET /api/v1/resume/{resumeId}/download`

These are Candidate-owned APIs.

Recruiters must NOT receive unrestricted Resume browsing access.

---

# 11. Application Domain — Current State

Application belongs to Recruitment Service.

Application persists:

- `id`
- `candidateId`
- `jobId`
- `resumeId`
- `status`
- `submittedAt`

Important:

`candidateId` is CandidateProfile domain UUID.

It is NOT the Identity account UUID.

Critical invariant:

`resumeId` is the exact Resume selected during submission.

An old Application must NEVER automatically switch to a newer Resume.

Application `resumeId` is immutable after creation.

Initial persisted status:

`SUBMITTED`

Locked complete status set:

- `SUBMITTED`
- `SCREENING`
- `INTERVIEW`
- `OFFER`
- `HIRED`
- `REJECTED`
- `WITHDRAWN`

Do not add persisted `NEW`.

---

# 12. Current Application APIs

Candidate submission:

`POST /api/v1/recruitment/application`

Body:

```json
{
  "jobId": "UUID",
  "resumeId": "UUID"
}
```
Client must NOT send arbitrary `candidateId`. Submission validation: 1. authenticated role must be Candidate; 2. `CandidateProfile` is resolved through Candidate Service; 3. exact Resume is verified through Resume Service using authenticated ownership; 4. Job is verified through Job Service; 5. only `PUBLISHED` Job can receive a new Application; 6. Recruitment Service persists exact CandidateProfile ID, Job ID, and selected Resume ID; 7. initial status is `SUBMITTED`. Candidate tracking: ```http GET /api/v1/recruitment/application/mine ``` Candidate/recruiter authorized detail: ```http GET /api/v1/recruitment/application/{applicationId} ``` Recruiter own-Job applications: ```http GET /api/v1/recruitment/application/job/{jobId} ``` Recruiter must own the related Job. Recruitment Service verifies ownership through Job Service. Recruitment Service does NOT query `job_db`. --- # 13. Job APIs Added for Recruitment Integration Published Job validation: ```http GET /job/{jobId} ``` Only returns a Job when status is `PUBLISHED`. Used by Recruitment Service before accepting an Application. Recruiter Job ownership validation: ```http GET /job/{jobId}/ownership ``` Requires authenticated recruiter ownership. This ownership check is NOT restricted to `PUBLISHED`. A recruiter may still inspect applications belonging to an owned Job after it becomes `CLOSED`. --- # 14. Gateway Routes Current external routes include: - `/api/v1/identity/**`; - `/api/v1/candidate/**`; - `/api/v1/employer/**`; - `/api/v1/job/**`; - `/api/v1/resume/**`; - `/api/v1/recruitment/**`. Resume and Recruitment routes are protected by Gateway authentication by default. Do not make them public. --- # 15. Error Code Ranges Maintain service-specific ranges: - Identity: `1xxx`; - Candidate: `2xxx`; - Employer: `3xxx`; - Job: `4xxx`; - Resume: `5xxx`; - Recruitment: `6xxx`; - Matching: `7xxx`; - Notification: `8xxx`; - Gateway: `9xxx`. Do not casually change existing codes. --- # 16. Important Frozen Domain Rules ## Candidate != Resume `CandidateProfile` contains stable profile/preferences. Resume contains Resume-specific data. Candidate may own multiple Resumes. ## Application Freezes Selected Resume Application must persist exact `resumeId`. Do NOT implement: ```text latest resume ``` replacement behavior. ## Service Database Ownership No direct cross-schema queries. ## Authentication Gateway authenticates. Owning business service authorizes. ## Kafka Do not add Kafka events without an actual asynchronous use case. ## Search No Elasticsearch during P0. ## Matching Recruiter-facing matching is: ```text Resume <-> Job ``` not: ```text CandidateProfile <-> Job ``` ## AI AI is decision support only. AI must never automatically reject/hire a Candidate. --- # 17. Quality/Test Rule The user primarily tests APIs with Postman. For each API test: 1. HTTP method + Gateway URL; 2. headers; 3. body when needed; 4. Expected Result immediately below that request. Normal business API: use Gateway. Do not claim runtime/build/test pass unless supported by actual evidence. Before completing a step run as applicable: ```bash mvn spotless:apply mvn spotless:check mvn clean test mvn clean compile ``` Also verify: - service startup; - Gateway routing; - success cases; - failure/security cases; - persistence; - MapStruct generation where applicable; - no `.env`; - no `target/`; - no secrets committed. --- # 18. DAY 4 — Frozen Scope According to `PROJECT_CONTEXT.md`: ```text DAY 4 — Kafka + ATS Workflow + Notification ``` Planned scope: - Kafka integration; - recruitment events; - recruitment status transitions; - interview scheduling; - Notification Service; - Mongo notification persistence. Goal: Asynchronous recruitment workflow functions end-to-end. Do NOT automatically implement all these items together. Day 4 must still be divided into small implementation STEPs. Do not invent the Day 4 STEP split until current `main` and all mandatory project files have been inspected. --- # 19. Day 4 Important Constraints Recruitment statuses remain: - `SUBMITTED`; - `SCREENING`; - `INTERVIEW`; - `OFFER`; - `HIRED`; - `REJECTED`; - `WITHDRAWN`. Minimal transitions are defined in `PROJECT_CONTEXT.md`. Do not allow arbitrary status jumps. Candidate withdraw behavior must respect the frozen transition plan. Recruiter must only mutate Applications belonging to Jobs they own/manage. Kafka should be introduced only where an asynchronous use case now exists. Notification Service is one of the frozen architecture services. Do not create another workflow/event microservice. No Matching Service implementation until the appropriate Day 5 step. --- # 20. Git Workflow Normal workflow: ```text main -> short-lived feature branch -> implementation -> Spotless -> compile/test -> Postman/runtime tests -> commit -> push -> pull request -> merge to main ``` Merged branches are historical/read-only. Do not continue new work on merged Day 3 branches. --- # 21. Next Action Before writing Day 4 code: 1. inspect actual GitHub `main`; 2. read all five mandatory project files; 3. verify final Day 3 merge/checkpoint; 4. inspect current Kafka infrastructure/config; 5. inspect current Recruitment Service/Application status model; 6. inspect Gateway/routes; 7. inspect Notification/Matching modules to confirm they do not exist yet; 8. identify unresolved issues; 9. determine the smallest correct Day 4 STEP. Do NOT immediately dump Kafka + status workflow + Notification Service together. Produce only ONE Day 4 implementation STEP. After that STEP: STOP and wait for implementation/testing.
