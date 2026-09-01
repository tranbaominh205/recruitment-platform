# CURRENT CHECKPOINT

Last updated:

2026-08-31

Project:

Recruitment Platform Capstone

Current phase:

DAY 1 CLOSEOUT / PRE-DAY-2 VERIFICATION

---

# 1. Important Rule for Next Chat

Before starting Day 2, inspect the actual GitHub `main` branch.

Do NOT assume Day 1 is complete only from this checkpoint.

Verify all Day 1 completion criteria first.

If any Day 1 issue remains, fix it before producing Day 2 Step 2.1.

---

# 2. Completed Foundation Work

The following work was implemented during Day 1.

## Root Maven Project

Multi-module Maven project.

Root group/package convention:

`com.tbm.recruitment`

Current relevant modules include:

- identity-service
- api-gateway

Future modules will be added only when their implementation begins.

---

# 3. Technology Baseline

Locked:

- Java 25 LTS
- Microsoft OpenJDK 25.0.4.1 locally
- Spring Boot 4.0.8
- Spring Cloud 2025.1.3
- Spring AI 2.0.1
- Maven >= 3.9.5
- Lombok 1.18.46
- MapStruct 1.6.3
- Spotless Maven Plugin 3.10.0

Do not downgrade to Java 21 or Boot 3 based on old tutorials.

---

# 4. Infrastructure

Docker infrastructure established:

## MySQL

Image:

`mysql:8.4.11`

Container:

`recruitment-mysql`

Port:

3306

Current schema:

`identity_db`

Future schemas planned:

- candidate_db
- employer_db
- job_db
- recruitment_db

## MongoDB

Image:

`mongo:8.0.29`

Container:

`recruitment-mongodb`

Port:

27017

Future DBs:

- resume_db
- matching_db
- notification_db

## Kafka

Image:

`apache/kafka:4.2.1`

Container:

`recruitment-kafka`

KRaft single node.

External:

9094

Internal:

9092

No ZooKeeper.

## MinIO

Container:

`recruitment-minio`

Ports:

9000 / 9001

Used later by Resume Service.

## Redis

Not running.

Redis is P1 and must not be introduced until code has a real use for it.

---

# 5. Local Environment Convention

Root:

`.env`

contains real local secrets.

Never commit it.

Committed:

`.env.example`

Root run scripts include:

- `scripts/run-identity.ps1`
- `scripts/run-gateway.ps1`

Scripts load environment variables from root `.env`.

---

# 6. Identity Service

Port:

8081

Database:

`identity_db`

Implemented concepts:

- Identity service bootstrap
- health endpoint
- Account entity
- UUID account IDs
- unique email
- BCrypt password hashing
- roles
- account enabled flag
- registration
- login
- JWT generation
- JWT validation/resource server
- token introspection
- current account endpoint
- role authorization
- admin account listing
- API response convention
- error handling
- OpenAPI/Swagger work
- MapStruct standardization work

Roles:

- CANDIDATE
- RECRUITER
- ADMIN

Public registration:

CANDIDATE
RECRUITER

Public ADMIN registration is forbidden.

---

# 7. JWT Contract

JWT:

HS256

Claims:

- `sub` = accountId
- `email`
- `role`
- `iat`
- `exp`

Issuer:

`identity-service`

Expiration:

3600 seconds

JWT signing key:

environment variable `JWT_SIGNER_KEY`

Do not add fallback secrets.

Do not log JWT values.

---

# 8. Identity API Concepts

Known/current intended endpoints:

Health:

`GET /identity/health`

Register:

`POST /identity/auth/register`

Login:

`POST /identity/auth/login`

Introspection:

`POST /identity/auth/introspect`

Current user:

`GET /identity/me`

Admin accounts:

`GET /identity/admin/accounts`

The next chat MUST inspect current source to verify exact endpoint/controller definitions.

---

# 9. Introspection Behavior

Valid JWT:

HTTP 200

`result.valid = true`

Invalid/malformed/expired JWT:

HTTP 200

`result.valid = false`

Gateway converts invalid authentication state to 401.

---

# 10. Identity Error/Response Convention

ApiResponse pattern:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {}
}
````
Identity code range:

`1xxx`

Use meaningful HTTP status.

Do not convert all errors to HTTP `200`.

---

# 11. Role Authorization

JWT role claim is intended to map into Spring Security authorities.

Concept:

```text
ADMIN
-> ROLE_ADMIN
```

Admin endpoint:

`/identity/admin/**`

must only allow `ADMIN`.

Candidate and Recruiter must receive `403` for Admin APIs.

Business authorization belongs in owning services.

---

# 12. API Gateway

Port:

`8888`

Spring Cloud Gateway WebFlux.

Public API prefix:

`/api/v1`

Example:

```http
POST http://localhost:8888/api/v1/identity/auth/login
```

Gateway routes to Identity.

Gateway uses:

`StripPrefix=2`

so downstream receives:

`/identity/auth/login`

---

# 13. Gateway Authentication

Gateway `GlobalFilter`:

- allows configured public endpoints;
- allows `OPTIONS` requests;
- extracts Bearer token;
- calls Identity introspection;
- returns `401` for missing/invalid authentication;
- returns `503` if Identity introspection service is unavailable;
- does not log the raw token.

Trusted headers may be forwarded:

- `X-Account-Id`;
- `X-Account-Email`;
- `X-Account-Role`.

Gateway must overwrite spoofed client values.

Authorization header is still forwarded so services may validate JWT/security context.

---

# 14. Authentication vs Authorization Decision

LOCKED:

Authentication:

primarily Gateway boundary.

Authorization:

business service.

Detailed ownership/business permissions:

owning service.

Do NOT implement all authorization in Gateway.

---

# 15. MapStruct Convention Added

MapStruct version:

`1.6.3`

Lombok:

`1.18.46`

Binding:

`lombok-mapstruct-binding`

Identity mapper:

`AccountMapper`

Desired pattern:

```
@Mapper(componentModel = "spring")
```

Service should inject mapper as a Spring bean.

Mapping logic belongs in mapper.

Business/security logic remains in Service.

Example:

password encoding stays in `AuthenticationService`.

---

# 16. LAST OBSERVED UNRESOLVED ISSUE

The last runtime error observed in the previous chat was:

Spring could not inject:

`com.tbm.recruitment.identity.mapper.AccountMapper`

Error concept:

```text
No qualifying bean of type AccountMapper available
```

Likely area:

MapStruct annotation processing / generated Spring component.

Before Day 2, inspect current `main` and verify whether this has already been fixed.

Verification:

Run:

```bash
mvn clean compile
```

Check generated file:

`identity-service/target/generated-sources/annotations/com/tbm/recruitment/identity/mapper/AccountMapperImpl.java`

It should exist.

Generated implementation should be registered as a Spring component.

Then run Identity Service and verify startup succeeds.

If current `main` already contains the fix, mark this issue resolved.

If not, fix this before Day 2.

---

# 17. Day 1 Regression Gate

Before declaring Day 1 DONE, verify:

## Build

- `mvn spotless:check` passes;
- `mvn clean test` passes.

## Identity

- service starts on `8081`;
- Swagger/OpenAPI loads if committed;
- register Candidate works;
- register Recruiter works;
- public `ADMIN` registration is blocked;
- login returns JWT;
- introspection valid token -> `true`;
- introspection invalid token -> `false`.

## Gateway

- starts on `8888`;
- Identity health routes through Gateway;
- login routes through Gateway.

## Authentication

Without token:

```http
GET /api/v1/identity/me
```

-> `401`

Invalid token:

-> `401`

Valid token:

-> `200`

## Authorization

Candidate JWT:

Admin accounts endpoint

-> `403`

Recruiter JWT:

Admin accounts endpoint

-> `403`

Admin JWT:

Admin accounts endpoint

-> `200`

Response must not expose `passwordHash`.

## Security

- no raw JWT logging;
- no committed real secrets.

Only when all items above pass:

```text
DAY 1 = DONE
```

---

# 18. Next Planned Work

Only after Day 1 regression gate passes:

## DAY 2

### STEP 2.1

Candidate Service foundation + `CandidateProfile`.

Initial Day 2 direction:

Candidate Service:

- add Maven module;
- bootstrap Spring Boot service;
- connect `candidate_db`;
- `CandidateProfile` entity;
- candidate ownership;
- DTOs;
- MapStruct mapper;
- repository;
- service;
- controller;
- validation;
- Gateway route;
- authentication integration;
- persistence tests.

Stable `CandidateProfile` fields:

- `fullName`;
- `phone`;
- `school`;
- `major`;
- `graduationYear`;
- `location`;
- `desiredJobTitles`;
- `preferredLocations`;
- `employmentTypes`;
- `workplaceTypes`.

`CandidateProfile` must NOT contain CV/resume contents.

Candidate identity must be linked to the authenticated Identity account.

Do not accept arbitrary account ownership from the client.

---

# 19. Planned Day 2 Sequence

Do not implement all of Day 2 at once.

Likely sequence:

### STEP 2.1

Candidate Service bootstrap + `CandidateProfile`.

### STEP 2.2

Candidate profile read/update + preferences/ownership refinement as required.

### STEP 2.3

Employer Service foundation/company domain.

### STEP 2.4

Job Service foundation + job creation.

### STEP 2.5

Job lifecycle/list/search foundation.

Exact split must be determined from actual repository state and time remaining.

One step at a time.

---

# 20. Do Not Start Yet

Before writing Day 2 implementation code:

- inspect GitHub `main`;
- read the five project context files;
- verify Day 1 gate;
- summarize current architecture;
- identify unresolved issues.

Only then proceed.
````
