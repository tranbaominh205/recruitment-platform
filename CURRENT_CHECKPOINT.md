# CURRENT CHECKPOINT

Last updated:

2026-09-06

Project:

Recruitment Platform Capstone

Current phase:

DAY 4 DONE / DAY 5 READY

---

# 1. Critical Rule for the Next Chat

Before generating any Day 5 implementation code:

1. Read:
    - `MASTER_PROMPT.md`
    - `PROJECT_CONTEXT.md`
    - `ARCHITECTURE.md`
    - `DEVELOPMENT_GUIDE.md`
    - `CURRENT_CHECKPOINT.md`
2. Inspect the actual GitHub `main` branch.
3. Do NOT assume repository state only from this checkpoint.
4. Source code on `main` is implementation truth.
5. If this checkpoint differs from actual source:
    - use actual source to determine implementation state;
    - explicitly report the inconsistency;
    - do not silently rewrite frozen architecture/domain rules.
6. Confirm Day 4 source state before starting Day 5.
7. Do not claim compile/test/runtime/Postman success without actual evidence.

Known Day 3 final implementation merge:

`87677f173867c61e0cbc601ca0d9b274b9143040`

Commit message:

`feat(recruitment): add application tracking and recruiter reads`

Known Day 3 closeout/checkpoint merge:

`7ab08d2abd39d613f02041bcdba1fa00bc8e3e61`

Known Day 4 final implementation merge:

`ccfd5bf1cad389000f8ea08fc64ff21bdfe2f61f`

Commit message:

`feat(notification): add interview scheduled notifications`

Day 4 implementation history:

- `5d595893fe77d06d44e5d28d4f53a1ce30a66e30`
    - recruiter-owned Application status transitions;
    - strict frozen transition validation.

- `3fd66d02f741d6e4c6b2b9f90288ee4ebc81f191`
    - Candidate Application withdrawal;
    - frozen withdraw transitions.

- `f3442661019709d66643faf41579f4d16bba8b84`
    - Notification Service foundation;
    - MongoDB notification persistence;
    - own-notification read API.

- `2fead79eb38c367c573cfe377104567ce71d6aa9`
    - `application.status.changed`;
    - Recruitment Kafka producer;
    - Notification Kafka consumer;
    - CandidateProfile UUID -> Identity Account UUID resolution.

- `019d2a85ee935f4ca6a2e0d2bfdff8999a0068b6`
    - Interview scheduling domain/API;
    - recruiter ownership;
    - one Interview schedule per Application for P0.

- `ccfd5bf1cad389000f8ea08fc64ff21bdfe2f61f`
    - `interview.scheduled`;
    - Kafka producer/consumer;
    - Candidate Interview notification.

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
- `notification-service`

Not implemented yet:

- `matching-service`

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

Do not choose Spring/Kafka/AI dependencies from tutorials without checking compatibility with the current locked stack.

---

# 4. Current Ports

- API Gateway: `8888`
- Identity Service: `8081`
- Candidate Service: `8082`
- Employer Service: `8083`
- Job Service: `8084`
- Resume Service: `8085`
- Recruitment Service: `8086`
- Notification Service: `8088`

External business API prefix:

`/api/v1/**`

Downstream services do not duplicate `/api/v1`.

`matching-service` is not implemented yet, so do not treat a Matching port as an existing runtime port.

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

Notification Service:

`notification_db`

## MinIO

Resume Service owns Resume/CV binary storage.

Default bucket:

`resumes`

## Kafka

Current business topics:

- `application.status.changed`
- `interview.scheduled`

Recruitment Service is the current producer owner for these Day 4 events.

Notification Service is the current consumer.

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

## Identity

Implemented:

- Candidate/Recruiter registration;
- public ADMIN registration blocked;
- login;
- BCrypt;
- JWT;
- introspection;
- current account;
- admin account listing;
- role authorization;
- MapStruct;
- password hash not exposed.

## Gateway

Implemented:

- Identity routing;
- JWT authentication;
- Identity introspection;
- trusted identity headers;
- public/protected endpoint handling;
- spoofed trusted headers overwritten.

Day 1 is closed.

---

# 8. DAY 2 — COMPLETE

## Candidate Service

Implemented:

- `CandidateProfile`;
- separate CandidateProfile UUID;
- Identity `accountId` reference;
- create/get/update own profile;
- Candidate-only ownership;
- candidate preferences:
    - `desiredJobTitles`
    - `preferredLocations`
    - `employmentTypes`
    - `workplaceTypes`

CandidateProfile does NOT contain Resume/CV contents.

## Employer Service

Implemented:

- `Company`;
- recruiter ownership;
- create/get/update own Company;
- recruiter cannot manage another recruiter's Company.

## Job Service

Implemented:

- Job creation;
- initial `DRAFT`;
- update `DRAFT`;
- `DRAFT -> PUBLISHED`;
- `PUBLISHED -> CLOSED`;
- recruiter own-job list;
- public PUBLISHED-job search;
- filtering;
- pagination;
- ownership through Company;
- Job Service does not directly query Employer DB.

Gateway public search rule:

Only method-aware:

`GET /job/search`

is public.

Known Day 2 completion merge:

`6d0c63ea024a6570c906583bb212d3873202a319`

Day 2 is closed.

---

# 9. DAY 3 — COMPLETE

Day 3 goal:

Candidate can apply to a Job using one explicitly selected Resume.

Implemented at source level:

## Resume Service

- MongoDB Resume metadata;
- MinIO binary storage;
- multiple Resume uploads;
- unique Resume UUID per upload;
- unique immutable storage key;
- Resume list;
- Resume metadata detail;
- secure Resume download;
- Candidate ownership enforcement.

## Recruitment Service / Application

- Application persistence;
- exact selected `resumeId`;
- initial `SUBMITTED` status;
- Candidate Application tracking;
- Recruiter Application listing;
- Candidate/Recruiter Application detail authorization;
- recruiter Job ownership verification through Job Service.

Day 3 contains no Kafka business workflow.

Day 3 final implementation history:

- `3028f3f0e18e320e9ec5ae650c1d63317f4fec0b`
    - Resume Service foundation;
    - MongoDB Resume metadata;
    - MinIO upload.

- `0adf2dbb4ca6249006b737ad75e2ec15045d863d`
    - Resume list/detail;
    - secure Resume download;
    - ownership enforcement.

- `1232bd9fd510f77c3a3e62b931dce207c5be2d63`
    - Recruitment Service foundation;
    - Application submission;
    - exact selected `resumeId`.

- `87677f173867c61e0cbc601ca0d9b274b9143040`
    - Candidate Application tracking;
    - recruiter Application reads;
    - recruiter Job ownership verification.

Day 3 is closed.

---

# 10. DAY 4 — COMPLETE

Day 4 goal:

Asynchronous recruitment workflow functions end-to-end.

Implemented at source level:

## ATS Status Workflow

Frozen Application statuses:

- `SUBMITTED`
- `SCREENING`
- `INTERVIEW`
- `OFFER`
- `HIRED`
- `REJECTED`
- `WITHDRAWN`

Recruiter-controlled transitions:

- `SUBMITTED -> SCREENING`
- `SUBMITTED -> REJECTED`
- `SCREENING -> INTERVIEW`
- `SCREENING -> REJECTED`
- `INTERVIEW -> OFFER`
- `INTERVIEW -> REJECTED`
- `OFFER -> HIRED`
- `OFFER -> REJECTED`

Candidate-controlled withdrawal:

- `SUBMITTED -> WITHDRAWN`
- `SCREENING -> WITHDRAWN`
- `INTERVIEW -> WITHDRAWN`
- `OFFER -> WITHDRAWN`

Terminal states:

- `HIRED`
- `REJECTED`
- `WITHDRAWN`

No arbitrary status jump is allowed.

Recruiter status mutation must verify Job ownership through Job Service.

Candidate withdraw must verify Candidate ownership through Candidate Service/Application ownership.

## Interview Scheduling

Recruitment Service owns Interview scheduling.

Current P0 model:

- one Interview schedule per Application;
- Recruiter must own the related Job;
- Application must already be `INTERVIEW`;
- Candidate owner can read the Interview;
- Recruiter owner can read the Interview;
- scheduling does not automatically mutate Application status;
- no reschedule/cancel;
- no multi-round interview workflow.

## Kafka

Implemented Day 4 business topics:

- `application.status.changed`
- `interview.scheduled`

Recruitment Service is the producer owner.

Notification Service is the consumer.

Events use minimal payloads and do not contain whole domain objects.

Kafka publishing is triggered after the owning MySQL transaction commits.

Current implementation is NOT claimed to provide:

- transactional outbox;
- exactly-once delivery;
- production-grade distributed transaction guarantees.

## Notification Service

Implemented:

- `notification-service`;
- port `8088`;
- MongoDB `notification_db`;
- in-app Notification persistence;
- Candidate/Recruiter own-notification read API;
- Kafka consumer for Application status changes;
- Kafka consumer for Interview scheduling;
- `sourceEventId` duplicate protection;
- CandidateProfile UUID -> Identity Account UUID resolution through Candidate Service.

Notification ownership uses Identity Account UUID:

`recipientAccountId`

Notification Service does not directly query another service database.

## Day 4 Scope Exclusions

Day 4 does NOT implement:

- Matching Service;
- Resume parsing;
- AI matching;
- Elasticsearch;
- interview reschedule/cancel;
- multi-round interview workflow;
- transactional outbox;
- email delivery.

Known Day 4 final implementation merge:

`ccfd5bf1cad389000f8ea08fc64ff21bdfe2f61f`

Day 4 source-level scope is complete.

Do not claim fresh runtime/quality-gate success in a new chat unless supported by actual evidence from the user or current execution.

---

# 11. Resume Domain — Current State

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

- new Resume UUID;
- new storage key;
- new immutable binary object.

Uploading a newer Resume must NOT overwrite a Resume referenced by an older Application.

Current Candidate Resume APIs:

`POST /api/v1/resume`

`GET /api/v1/resume`

`GET /api/v1/resume/{resumeId}`

`GET /api/v1/resume/{resumeId}/download`

These are Candidate-owned APIs.

Recruiters must NOT receive unrestricted Resume browsing access.

Recruiter-facing matching later must use Resume data versus Job requirements and must preserve the exact selected Resume relationship where Application context is involved.

---

# 12. Application Domain — Current State

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

# 13. Current Recruitment APIs

## Candidate submission

`POST /api/v1/recruitment/application`

Body:

```json
{
  "jobId": "UUID",
  "resumeId": "UUID"
}
```

Client must NOT send arbitrary `candidateId`.

Submission validation:

1. authenticated role must be Candidate;
2. `CandidateProfile` is resolved through Candidate Service;
3. exact Resume is verified through Resume Service using authenticated ownership;
4. Job is verified through Job Service;
5. only `PUBLISHED` Job can receive a new Application;
6. Recruitment Service persists exact CandidateProfile ID, Job ID, and selected Resume ID;
7. initial status is `SUBMITTED`.

## Candidate tracking

`GET /api/v1/recruitment/application/mine`

## Candidate/Recruiter authorized Application detail

`GET /api/v1/recruitment/application/{applicationId}`

## Recruiter own-Job Applications

`GET /api/v1/recruitment/application/job/{jobId}`

Recruiter must own the related Job.

Recruitment Service verifies ownership through Job Service.

Recruitment Service does NOT query `job_db`.

## Recruiter status transition

`PATCH /api/v1/recruitment/application/{applicationId}/status`

Recruiter can only perform frozen valid transitions for Applications belonging to Jobs they own.

Recruiter cannot set `WITHDRAWN`.

## Candidate withdraw

`PATCH /api/v1/recruitment/application/{applicationId}/withdraw`

Candidate can only withdraw their own Application and only from frozen active states.

## Interview scheduling

`POST /api/v1/recruitment/application/{applicationId}/interview`

Recruiter must own the related Job.

Application must already be `INTERVIEW`.

## Interview detail

`GET /api/v1/recruitment/application/{applicationId}/interview`

Authorized Candidate owner or Recruiter owner can read.

---

# 14. Job APIs Used by Recruitment

Published Job validation:

`GET /job/{jobId}`

Only returns a Job when status is `PUBLISHED`.

Used by Recruitment Service before accepting a new Application.

Recruiter Job ownership validation:

`GET /job/{jobId}/ownership`

Requires authenticated recruiter ownership.

This ownership check is NOT restricted to `PUBLISHED`.

A recruiter may still inspect/process Applications belonging to an owned Job after it becomes `CLOSED`.

---

# 15. Candidate Internal Contract Used by Notification

Internal endpoint:

`GET /internal/candidate/{candidateId}/account`

Purpose:

Resolve:

`CandidateProfile UUID -> Identity Account UUID`

Used by Notification Service after consuming recruitment events.

This is a service-to-service/internal contract.

It is not a normal frontend business API.

Notification must NOT treat Application `candidateId` as Identity `accountId`.

---

# 16. Notification Domain — Current State

Notification belongs to Notification Service.

MongoDB:

`notification_db`

Notification ownership:

`recipientAccountId`

Current notification types:

- `APPLICATION_STATUS_CHANGED`
- `INTERVIEW_SCHEDULED`

Notification data includes fields such as:

- `id`
- `sourceEventId`
- `recipientAccountId`
- `type`
- `title`
- `message`
- `referenceId`
- `read`
- `createdAt`

Current public API:

`GET /api/v1/notification`

The authenticated Candidate/Recruiter can only read notifications owned by their Identity Account UUID.

There is no public client endpoint for arbitrary notification creation.

---

# 17. Gateway Routes — Current State

Current external routes include:

- `/api/v1/identity/**`
- `/api/v1/candidate/**`
- `/api/v1/employer/**`
- `/api/v1/job/**`
- `/api/v1/resume/**`
- `/api/v1/recruitment/**`
- `/api/v1/notification/**`

Resume, Recruitment, and Notification routes are protected by Gateway authentication by default.

Do not make them public.

Public endpoint handling remains method-aware.

---

# 18. Error Code Ranges

Maintain service-specific ranges:

- Identity: `1xxx`
- Candidate: `2xxx`
- Employer: `3xxx`
- Job: `4xxx`
- Resume: `5xxx`
- Recruitment: `6xxx`
- Matching: `7xxx`
- Notification: `8xxx`
- Gateway: `9xxx`

Do not casually change existing codes.

---

# 19. Important Frozen Domain Rules

## Candidate != Resume

`CandidateProfile` contains stable profile/preferences.

Resume contains Resume-specific data.

Candidate may own multiple Resumes.

## Application Freezes Selected Resume

Application must persist exact `resumeId`.

Do NOT implement:

`latest resume`

replacement behavior.

## Matching Direction

Recruiter-facing matching is:

`Resume <-> Job`

not:

`CandidateProfile <-> Job`

Candidate preferences must NOT be mixed into recruiter matching score.

## Service Database Ownership

No direct cross-schema/database queries.

## Authentication

Gateway authenticates.

Owning business service authorizes.

## Kafka

Do not add Kafka events without an actual asynchronous use case and real consumer/use case.

## Search

No Elasticsearch during P0.

## AI

AI is decision support only.

AI must never automatically reject/hire a Candidate.

---

# 20. Quality/Test Rule

The user primarily tests APIs with Postman.

For each API test:

1. HTTP method + Gateway URL;
2. headers;
3. body when needed;
4. Expected Result immediately below that request.

Normal business API:

use Gateway.

Internal service-to-service endpoint:

test directly only when needed to verify/debug the internal contract, and explicitly state why.

Do not claim runtime/build/test pass unless supported by actual evidence.

Before completing an implementation step, run as applicable:

```bash
mvn spotless:apply
mvn spotless:check
mvn clean test
mvn clean compile
```

Also verify as applicable:

- service startup;
- Gateway routing;
- success cases;
- failure/security cases;
- persistence;
- Kafka topics/messages;
- Notification consumption;
- MapStruct generation;
- `git status`;
- no `.env`;
- no `target/`;
- no secrets committed.

---

# 21. Git Workflow

Normal workflow:

```text
main
-> short-lived feature branch
-> implementation
-> Spotless
-> compile/test
-> Postman/runtime tests
-> commit
-> push
-> pull request
-> merge to main
```

Merged branches are historical/read-only.

Do not continue new work on merged Day 1-4 branches.

Before every new Day 5 STEP:

- checkout/pull latest `main`;
- inspect actual source;
- create a fresh short-lived branch.

---

# 22. DAY 5 — FROZEN DIRECTION

Day 5 direction:

`Resume <-> Job Matching + AI`

Do not implement Day 5 from memory.

Before writing code, re-read `PROJECT_CONTEXT.md`, `ARCHITECTURE.md`, and actual source on `main`.

Recruiter-facing deterministic matching weights are frozen as:

- skills: 55%
- experience: 25%
- education: 10%
- title/domain: 10%

Candidate preferences must NOT be mixed into recruiter matching score.

Matching must be based on Resume data versus Job requirements.

AI may assist with explanation/extraction/ranking support only within frozen architecture.

AI must NOT automatically:

- reject a Candidate;
- hire a Candidate;
- mutate Application status.

No Elasticsearch during Day 5 P0 unless frozen architecture is explicitly changed first.

Do not implement the entire Matching Service in one step.

---

# 23. Next Action

Before writing any Day 5 implementation code:

1. inspect actual GitHub `main`;
2. read all five mandatory project files;
3. verify Day 4 final merge:
    - `ccfd5bf1cad389000f8ea08fc64ff21bdfe2f61f`;
4. verify this checkpoint is on/consistent with current `main`;
5. audit current Resume Service:
    - entity/document;
    - metadata;
    - MinIO storage;
    - secure ownership;
    - whether any parsed Resume representation already exists;
6. audit current Job Service:
    - Job fields;
    - requirements/skills/experience/education/title/domain data actually available;
7. confirm `matching-service` still does not exist;
8. inspect root `pom.xml` Spring AI version;
9. verify current Spring AI / Google GenAI integration compatible with the locked stack before selecting dependencies;
10. read frozen matching rules exactly from `PROJECT_CONTEXT.md`;
11. identify any missing prerequisite data model before scoring;
12. choose the smallest correct `DAY 5 — STEP 5.1`.

Do NOT immediately create:

- full Matching Service;
- Resume parser;
- Gemini integration;
- scoring engine;
- ranking API;
- Kafka workflow

all in one step.

The first Day 5 response in a new chat must begin with:

- source audit;
- prerequisite/data-contract audit;
- proposed small Day 5 STEP plan;
- exact scope of STEP 5.1;

and must NOT write STEP 5.1 code until the user confirms.

---

# 24. Current Known Limitations / Deferred P1 Work

Known intentionally deferred items include:

- Matching Service not implemented;
- Resume parsing not implemented;
- AI matching not implemented;
- Elasticsearch not implemented;
- interview reschedule/cancel not implemented;
- multi-round interview workflow not implemented;
- transactional outbox not implemented;
- Kafka exactly-once guarantees not claimed;
- Notification email delivery not implemented;
- current Notification duplicate protection is P0 idempotency support, not a full exactly-once guarantee.

These are not automatically bugs unless they violate the frozen scope for the current day.

---

# 25. Handoff Rule

When starting a new chat for Day 5, provide:

- GitHub `main`:
  `https://github.com/tranbaominh205/recruitment-platform`
- the latest `main` merge/checkpoint commit;
- instruction to read the five mandatory project files;
- instruction that source code on `main` is implementation truth;
- instruction to audit before coding;
- instruction to do only one STEP at a time;
- instruction to follow all 16 sections required by `MASTER_PROMPT.md`;
- instruction that Postman Expected Result must be immediately below each request;
- instruction not to claim build/runtime success without evidence.

The new chat must not rely on memory from previous chats.
