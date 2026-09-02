# CURRENT CHECKPOINT

Last updated:

2026-09-02

Project:

Recruitment Platform Capstone

Current phase:

DAY 2 DONE / DAY 3 READY

---

# 1. Critical Rule for the Next Chat

Before generating any Day 3 implementation code:

1. Read:

    * `MASTER_PROMPT.md`
    * `PROJECT_CONTEXT.md`
    * `ARCHITECTURE.md`
    * `DEVELOPMENT_GUIDE.md`
    * `CURRENT_CHECKPOINT.md`

2. Inspect the actual GitHub `main` branch.

3. Do NOT assume repository state only from this checkpoint.

4. Source code on `main` is the implementation truth.

5. If this checkpoint differs from actual source, use actual source and explicitly report the difference.

6. Do not start Day 3 if Day 2 source on `main` is incomplete or does not compile.

Known Day 2 completion merge:

`6d0c63ea024a6570c906583bb212d3873202a319`

This merged:

`feature/job-lifecycle-search`

Commit message:

`feat(job): add lifecycle and search`

---

# 2. Current Maven Modules

Current implemented modules:

* `identity-service`
* `api-gateway`
* `candidate-service`
* `employer-service`
* `job-service`

Modules not implemented yet:

* `resume-service`
* `recruitment-service`
* `notification-service`
* `matching-service`
* interview-related service/module if required later by the frozen plan

Do not create future modules before their implementation step begins.

---

# 3. Locked Technology Baseline

Keep the existing repository versions.

Important known baseline:

* Java 25
* Spring Boot 4.0.8
* Spring Cloud 2025.1.3
* Spring AI 2.0.1
* Lombok 1.18.46
* MapStruct 1.6.3
* Spotless Maven Plugin 3.10.0
* MySQL 8.4.11
* MongoDB 8.0.29
* Kafka 4.2.1
* MinIO infrastructure already planned/available

Do not downgrade Java or Spring Boot based on older tutorials.

Always inspect the actual root `pom.xml` before adding dependencies.

---

# 4. Current Ports

Current service ports:

* API Gateway: `8888`
* Identity Service: `8081`
* Candidate Service: `8082`
* Employer Service: `8083`
* Job Service: `8084`

Future service ports must be chosen consistently and must not collide with existing services.

External business API versioning remains at the Gateway:

`/api/v1/**`

Downstream controllers must not duplicate `/api/v1`.

---

# 5. Current MySQL Schemas

Implemented schemas:

* `identity_db`
* `candidate_db`
* `employer_db`
* `job_db`

Planned next relational schema:

* `recruitment_db`

Each business service owns its own schema.

A service must NOT directly query or manipulate another service's database/schema.

Cross-service data must be resolved through supported service APIs or the architecture-approved asynchronous mechanism.

---

# 6. Authentication and Authorization Model

Authentication boundary:

API Gateway.

Gateway:

* validates/introspects JWT;
* resolves authenticated identity;
* overwrites trusted identity headers;
* forwards:

    * `X-Account-Id`
    * `X-Account-Email`
    * `X-Account-Role`

Business authorization and ownership remain inside the owning business service.

Do not trust account/recruiter/candidate ownership IDs supplied arbitrarily by the frontend.

Do not move detailed domain authorization into the Gateway.

Direct calls to downstream service ports are internal/debug calls and do not represent the normal public API flow.

---

# 7. DAY 1 — COMPLETE

Implemented and verified:

## Identity Service

* account registration
* Candidate registration
* Recruiter registration
* public ADMIN registration blocked
* login
* BCrypt password hashing
* JWT generation
* JWT validation/introspection
* current account endpoint
* admin account listing
* role authorization
* password hash not exposed
* MapStruct AccountMapper
* Spring mapper injection verified
* OpenAPI/Swagger integration

## API Gateway

* Identity routing
* authentication filter
* Identity introspection
* trusted identity headers
* `401` handling
* public Identity endpoints
* protected endpoints
* spoofed trusted headers overwritten

Day 1 is closed.

---

# 8. DAY 2 — COMPLETE

## STEP 2.1 — Candidate Service + CandidateProfile

Implemented:

* `candidate-service`
* `candidate_db`
* CandidateProfile
* separate Candidate UUID
* Identity `accountId` reference
* one profile per Candidate account
* create own profile
* get own profile
* update own profile
* Candidate-only authorization
* Gateway Candidate route
* MapStruct
* validation
* MySQL persistence

Stable core CandidateProfile data includes:

* `fullName`
* `phone`
* `school`
* `major`
* `graduationYear`
* `location`

CandidateProfile does NOT contain Resume/CV contents.

---

# 9. STEP 2.2 — Candidate Job Preferences

Implemented:

* `desiredJobTitles`
* `preferredLocations`
* `employmentTypes`
* `workplaceTypes`
* preference update endpoint
* preference persistence using collections
* Candidate ownership
* GET CandidateProfile returns preferences
* core profile update does not erase preferences

Merge known as part of Day 2 history:

`9118aaa1b5b21695ac9ed2b7c79d5e37a707b494`

---

# 10. STEP 2.3 — Employer Service + Company

Implemented:

* `employer-service`
* `employer_db`
* Company entity/domain
* separate Company UUID
* authenticated Recruiter ownership
* create own Company
* get own Company
* update own Company
* Candidate forbidden from company management
* Gateway Employer route
* MapStruct
* MySQL persistence

Company ownership is derived from authenticated identity.

Client does not choose arbitrary `ownerAccountId`.

Known merge:

`463959fcf208d4dc0fbea0abeab81a9281832804`

---

# 11. STEP 2.4 — Job Service + Job Creation

Implemented:

* `job-service`
* `job_db`
* Job entity
* Job UUID
* `companyId`
* `createdByAccountId`
* Employer Service REST integration
* recruiter company resolution
* create Job
* newly created Job starts as `DRAFT`
* salary validation
* Candidate cannot create Job
* recruiter without Company cannot create Job
* Gateway Job route
* MapStruct
* MySQL persistence

Job Service does NOT query `employer_db` directly.

Known merge:

`6768a8d6e0c11251ffc1f4e634c6f355f30933d2`

---

# 12. STEP 2.5 — Job Lifecycle + List + Public Search

Implemented and merged:

`6d0c63ea024a6570c906583bb212d3873202a319`

Implemented lifecycle:

* `DRAFT`
* `PUBLISHED`
* `CLOSED`

Allowed core transitions:

* `DRAFT -> PUBLISHED`
* `PUBLISHED -> CLOSED`

Current behavior includes:

* update DRAFT Job
* prevent modification after publish/close
* publish Job
* prevent invalid lifecycle transitions
* close published Job
* recruiter list own Company Jobs
* company ownership checks
* public Job search
* search without JWT
* only `PUBLISHED` Jobs appear in public search
* keyword filter
* location filter
* employment type filter
* workplace type filter
* pagination
* pagination validation

API Gateway has method-aware public handling for:

`GET /api/v1/job/search`

Recruiter management APIs remain protected.

No Kafka Job events were introduced on Day 2.

No Elasticsearch was introduced.

P0 search currently uses MySQL.

---

# 13. Day 2 Architecture State

Core synchronous business flow currently available:

Candidate:

`register -> login -> CandidateProfile -> preferences`

Recruiter:

`register -> login -> Company -> create Job -> publish Job -> close Job`

Public/Candidate:

`search PUBLISHED Jobs`

Current cross-service synchronous relationship:

`Job Service -> Employer Service`

for recruiter Company resolution.

No service directly accesses another service's database.

---

# 14. Day 2 Completion Gate

User confirmed STEP 2.5 completed before creating this checkpoint.

Before Day 3 implementation, the next chat must still inspect actual GitHub `main`.

At minimum verify:

* commit `6d0c63ea024a6570c906583bb212d3873202a319` is present on `main`;
* root Maven contains all current modules;
* Candidate Service source exists;
* Employer Service source exists;
* Job Service source exists;
* Job lifecycle source exists;
* public Job search source exists;
* Gateway current routing/security configuration matches the source;
* no unresolved merge conflict is present.

Do not falsely claim build/test/runtime results unless supported by repository state or user-provided results.

---

# 15. DAY 3 Goal

DAY 3 — Resume + Application.

Frozen Day 3 scope from `PROJECT_CONTEXT.md`:

## Resume

* Resume Service
* MinIO storage
* resume metadata
* upload
* download

## Recruitment

* Recruitment Service
* application submission
* persist selected `resumeId`
* initial recruitment workflow

Day 3 business goal:

Candidate can apply to a Job using a specific Resume.

---

# 16. Important Resume Domain Rules

Do not mix Resume contents into CandidateProfile.

CandidateProfile contains stable candidate information/preferences.

Resume is a separate domain.

A Candidate may have multiple Resumes.

An Application must preserve the exact Resume selected at application time using `resumeId`.

Do not silently replace this with "latest resume".

Resume binary/file storage belongs in MinIO.

Resume metadata/domain persistence must follow the frozen architecture and actual repository conventions.

Before implementing, inspect `ARCHITECTURE.md` for the approved Resume database/storage ownership.

---

# 17. Important Recruitment/Application Rules

Application belongs to Recruitment Service.

Do not put Application directly inside Job Service.

Do not put Application directly inside Candidate Service.

An Application must reference the relevant IDs according to frozen architecture.

At minimum Day 3 must preserve:

* candidate identity/domain reference;
* job reference;
* selected `resumeId`;
* initial workflow/status.

Recruiter/Application authorization belongs in Recruitment Service.

Do not add Kafka workflow before the Day 4 step unless the actual frozen architecture or updated plan explicitly requires it.

---

# 18. Day 3 Implementation Strategy

Do not implement all Day 3 features at once.

Likely sequence must be determined after inspecting actual `main`.

Expected direction:

## STEP 3.1

Resume Service foundation + Resume metadata + MinIO upload.

## STEP 3.2

Resume list/get/download/ownership refinement as required.

## STEP 3.3

Recruitment Service foundation + Application submission using explicit `resumeId`.

## STEP 3.4

Initial Application read/workflow foundation as required to complete Day 3 P0.

The exact split must be based on:

* actual source;
* frozen architecture;
* P0 priority;
* remaining time.

Only ONE implementation STEP at a time.

After every STEP:

STOP and wait for user implementation/testing.

---

# 19. Permanent Instruction for Tests

The user tests APIs primarily with Postman.

For every Postman test case:

1. show HTTP method;
2. show full Gateway URL or Postman variable URL;
3. show required Authorization header;
4. show request body if applicable;
5. immediately below that same test, show its Expected Result.

Do NOT put all expected responses into a separate distant section that forces the user to cross-reference tests.

Public endpoints must explicitly say when Authorization must be omitted.

Business API tests should normally use the Gateway URL, not direct internal service ports.

---

# 20. Permanent Instruction for Code Output

When a STEP requires creating or modifying a source file:

* provide the actual code required;
* provide complete code when replacement of the file is required;
* do not say only:

    * "copy the Candidate version";
    * "copy this file from Employer Service";
    * "same as the previous service";
    * "reuse the existing handler and change the package";
* do not force the user to reconstruct required code from earlier chat messages.

Existing reusable source should still be inspected to preserve conventions, but the answer must show the concrete implementation needed for the current STEP.

Do not dump unrelated services.

Only show files required by the current STEP.

---

# 21. Quality Gate

Before a STEP is DONE, run as applicable:

```bash
mvn spotless:apply
mvn spotless:check
mvn clean test
mvn clean compile
```

Also verify:

* service startup;
* Gateway routing where applicable;
* Postman success cases;
* Postman failure/security cases;
* persistence;
* mapper generation where MapStruct is used;
* no committed `.env`;
* no committed `target/`;
* no real secrets.

---

# 22. Git Workflow

Normal workflow:

`main`
-> new short-lived feature branch
-> implementation
-> Spotless
-> compile/test
-> Postman/runtime tests
-> commit
-> push
-> pull request
-> merge to `main`

Do not continue new work on already merged feature branches.

At the end of Day 3:

* regression test completed Day 3 flow;
* merge completed work into `main`;
* update this checkpoint;
* commit checkpoint.

---

# 23. Next Action

The next chat must NOT immediately invent STEP 3.1 code.

First:

1. inspect actual GitHub `main`;
2. read all five mandatory project files;
3. inspect current modules and relevant source;
4. verify Day 2 state against commit `6d0c63ea024a6570c906583bb212d3873202a319`;
5. inspect MinIO configuration already present;
6. inspect architecture constraints for Resume Service and Recruitment Service;
7. identify any inconsistency/blocker.

If Day 2 is clean:

NEXT:

`DAY 3 — STEP 3.1 — Resume Service foundation + Resume metadata + MinIO upload`

Then follow the mandatory 16-section STEP format from `MASTER_PROMPT.md`.

After STEP 3.1:

STOP.
