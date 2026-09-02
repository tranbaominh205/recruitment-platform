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

Can perform basic administrative account operations.

Admin is NOT publicly self-registered.

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
