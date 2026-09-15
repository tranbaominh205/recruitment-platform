# PROJECT CONTEXT — Recruitment Platform

## 1. Project

University capstone project:

Online Recruitment Platform using Microservices with modest AI-assisted Resume–Job matching.

Development deadline:

7 days.

Primary objective:

Deliver a demonstrable end-to-end recruitment workflow.

The project must favor working P0 functionality over infrastructure complexity.

---

# 2. Primary Actors

## Candidate

Can:

- register/login;
- maintain CandidateProfile;
- upload multiple resumes;
- view jobs;
- apply to jobs using a selected resume;
- track applications;
- withdraw eligible applications;
- receive notifications.

## Recruiter

Can:

- register/login;
- belong to/manage employer/company information;
- create and manage jobs;
- view applications;
- inspect the exact resume submitted with an application;
- move applicants through recruitment stages;
- view Resume–Job matching scores;
- schedule interviews;
- receive notifications.

## Admin

Admin is the platform oversight role for account operations, business moderation,
and operational visibility across the platform.

Admin is NOT publicly self-registered.

The system keeps exactly one primary role per `Account`:

- CANDIDATE
- RECRUITER
- ADMIN

`ADMIN` additionally owns an authoritative database-backed set of
`AdminPermission` values. These are stored in Identity Service state and are
returned by `/identity/me` and identity introspection for frontend capability
checks and backend authorization decisions.

Current `AdminPermission` values:

- ACCOUNT_DISABLE
- ACCOUNT_REVOKE_SESSIONS
- JOB_MODERATE
- COMPANY_MODERATE
- COMPANY_VERIFY

Permissions are not JWT claims. They are not trusted if they are merely sent by
clients. The Gateway strips and overwrites spoofed trusted headers, and the
owning business service re-checks the authoritative permission state before
allowing moderation or administrative actions.

Current Admin responsibilities include:

- account oversight and enable/disable flows;
- password/session invalidation and revoke-all-session workflows;
- moderation of jobs and companies;
- verification of companies;
- operational dashboards for identities, jobs, companies, applications,
  interviews, resumes, matching, and notifications.

Admin capability checks are UX only when used in the frontend. Backend
authorization remains mandatory and authoritative.

## Job and company moderation

The platform now has a separate moderation lifecycle for jobs and a separate
moderation/verification model for companies.

Job lifecycle remains:

- `DRAFT`
- `PUBLISHED`
- `CLOSED`

Job moderation is independent:

- `ACTIVE`
- `HIDDEN`
- `REMOVED`

A job is publicly visible only when:

- business status is `PUBLISHED`
- moderation status is `ACTIVE`

Company lifecycle includes moderation and verification as independent states:

- moderation: `ACTIVE` / `SUSPENDED`
- verification: `UNVERIFIED` / `VERIFIED` / `REJECTED`

A suspended company cannot create, update DRAFT, or publish DRAFT jobs. The Job
Service blocks those mutations with `COMPANY_SUSPENDED` while preserving the
company's history and allowing closed or owned historical reads.

---

# 3. Core Domain Invariants

## Candidate != Resume

CandidateProfile contains stable candidate information only.

CandidateProfile may contain:

- fullName
- phone
- school
- major
- graduationYear
- location
- desiredJobTitles
- preferredLocations
- employmentTypes
- workplaceTypes

Do NOT put parsed CV contents into CandidateProfile.

For example, do not store:

- entire resume skills;
- work-history extracted from every CV;
- resume summary;
- CV sections;

as CandidateProfile fields.

---

# 4. Multiple Resumes

One candidate may own multiple resumes.

Example:

Candidate A:

- Resume Backend Java
- Resume Full-stack
- Resume Internship

Each upload creates a distinct Resume identifier.

---

# 5. Application Resume Immutability

When applying for a job, Candidate MUST explicitly choose one Resume.

Application must store at least:

- id
- candidateId
- jobId
- resumeId
- status
- submittedAt

Critical invariant:

An existing application always references the exact resume selected at submission time.

Uploading a newer resume MUST NOT change old applications.

---

# 6. Resume Immutability V1

Each resume upload creates:

- new UUID
- new storageKey
- immutable binary

Rename only changes display metadata.

Archive changes resume status.

Do NOT implement a complex versioning framework for V1.

---

# 7. Recruitment Statuses — LOCKED

Use:

- SUBMITTED
- SCREENING
- INTERVIEW
- OFFER
- HIRED
- REJECTED
- WITHDRAWN

Do NOT introduce a separate `NEW` persisted status.

UI may display SUBMITTED as "New applicant".

Minimal transitions:

SUBMITTED
-> SCREENING
-> REJECTED
-> WITHDRAWN

SCREENING
-> INTERVIEW
-> REJECTED
-> WITHDRAWN

INTERVIEW
-> OFFER
-> REJECTED
-> WITHDRAWN

OFFER
-> HIRED
-> REJECTED
-> WITHDRAWN

---

# 8. Matching — LOCKED

Recruiter-facing matching compares:

Resume <-> Job

It does NOT compare:

CandidateProfile <-> Job

Recruiter-facing deterministic score weights:

- skills: 55%
- experience: 25%
- education: 10%
- title/domain: 10%

Candidate preferences are NOT included in recruiter matching score.

Candidate preferences belong to a separate future RecommendationEngine/P1 concern.

---

# 9. AI Role

AI is decision support only.

AI may:

- parse resume content;
- normalize extracted structured data;
- generate explanations.

Deterministic Java code should calculate recruiter-facing match score.

AI must NOT:

- automatically reject an applicant;
- automatically hire an applicant;
- make irreversible recruitment decisions.

---

# 10. Search Priority

Job search implementation order:

P0:
MySQL search/filtering.

P1:
Redis where actually useful.

P2:
Elasticsearch only after the core workflow is stable.

Do NOT introduce Elasticsearch during early core development.

---

# 11. Priority Scope

## P0

Must work end-to-end:

Candidate:

register
-> login
-> profile
-> upload resume
-> apply with selected resume
-> application tracking

Recruiter:

register
-> login
-> employer/company
-> create/publish job
-> receive application
-> inspect submitted resume
-> move recruitment status

System:

Gateway authentication
-> database persistence
-> Kafka events
-> notification
-> AI resume parsing/matching

## P1

Examples:

- candidate recommendations;
- Redis optimizations;
- better notification flows;
- richer filtering;
- convenience features.

## P2

Examples:

- Elasticsearch;
- advanced observability;
- additional AI improvements.

## Post-P0 roadmap — current remaining work

The platform is now past P0 and into the next operational stabilization wave:

- Step 4/7 — D1 Candidate Job Recommendations
  - candidate-facing job recommendations using CandidateProfile preferences;
  - completely separate from recruiter Resume–Job matching.
- Step 5/7 — D2 Notification Usability
  - read/unread state and more useful navigation/reference behavior.
- Step 6/7 — D3 Recruiter Applicant Filtering/Sorting
  - recruiter-side filtering and sorting improvements without weakening
    frozen application status rules.
- Step 7/7 — Phase E Final Regression + Documentation
  - backend/frontend regression, security negative cases, startup/runtime checks,
    and final documentation freeze.

---

# 12. 7-Day Plan

## Day 1 — Foundation

- parent Maven project
- Docker infrastructure
- Identity Service
- register
- login
- JWT
- introspection
- API Gateway
- authentication
- role authorization
- OpenAPI/Swagger
- MapStruct convention

## Day 2 — Core Business Profiles + Jobs

- Candidate Service
- CandidateProfile
- candidate job preferences
- Employer Service
- employer/company foundation
- Job Service
- job CRUD/publish foundation

Goal:

Candidate/Recruiter/Job data model is working.

## Day 3 — Resume + Application

- Resume Service
- MinIO storage
- resume metadata
- upload/download
- Recruitment Service
- application submission
- selected resumeId persistence
- initial recruitment workflow

Goal:

Candidate can apply to a job with a specific resume.

## Day 4 — Kafka + ATS Workflow + Notification

- Kafka integration
- recruitment events
- status transitions
- interview scheduling
- Notification Service
- Mongo notification persistence

Goal:

Asynchronous workflow functions end-to-end.

## Day 5 — AI Matching

Matching Service:

- Spring AI
- Google GenAI / Gemini
- PDFBox
- resume structured extraction
- Mongo persistence
- deterministic Resume–Job scoring
- AI explanation

Goal:

Recruiter can see explainable Resume–Job match output.

## Day 6 — React Frontend

Implement core candidate and recruiter UI.

Focus only on P0 workflow.

## Day 7 — Stabilization

- end-to-end regression
- bug fixing
- README
- architecture diagrams
- demo preparation
- report evidence
- optional P1 only if time remains

---

# 13. Explicit Non-Goals During P0

Do NOT introduce:

- Kubernetes
- Eureka
- Config Server
- RabbitMQ
- Saga framework
- distributed tracing stack
- Prometheus/Grafana
- ELK stack
- Neo4j
- vector database
- ML model training
- complex OAuth providers
- chat/WebSocket features
- mobile application
- Elasticsearch before core workflow works
