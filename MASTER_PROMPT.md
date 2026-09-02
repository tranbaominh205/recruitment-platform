# MASTER PROMPT — Recruitment Platform Capstone

## 1. Purpose

This file defines how the AI assistant must work on this project.

Before proposing, modifying, or implementing anything, the assistant MUST read:

1. `MASTER_PROMPT.md`
2. `PROJECT_CONTEXT.md`
3. `ARCHITECTURE.md`
4. `DEVELOPMENT_GUIDE.md`
5. `CURRENT_CHECKPOINT.md`

The assistant must also inspect the actual code on the current GitHub `main` branch for every file/module relevant to the next step.

Do not rely on previous chat memory when repository content is available.

---

# 2. Sources of Truth

Use the following rules when information conflicts.

## Project scope and domain rules

Source of truth:

- `PROJECT_CONTEXT.md`
- `ARCHITECTURE.md`

Do not silently change frozen architecture or domain rules just because current code differs.

If current code contradicts a frozen architecture decision, STOP and explicitly report the inconsistency.

## Current implementation state

Source of truth:

- actual GitHub `main` branch
- `CURRENT_CHECKPOINT.md`

If `CURRENT_CHECKPOINT.md` says something is complete but current `main` does not contain it, treat the code as the actual implementation state and report that the checkpoint is stale.

## Development conventions

Source of truth:

- `DEVELOPMENT_GUIDE.md`

## AI workflow and response format

Source of truth:

- this `MASTER_PROMPT.md`

---

# 3. Mandatory Pre-Step Check

Before generating the next implementation step:

1. Read all project context files.
2. Inspect current `main`.
3. Inspect the root `pom.xml`.
4. Inspect the relevant service `pom.xml`.
5. Inspect existing packages/classes related to the requested feature.
6. Inspect current configuration.
7. Check `CURRENT_CHECKPOINT.md`.
8. Identify unresolved issues.
9. Confirm the previous Definition of Done is satisfied.
10. Only then create the next step.

Do NOT generate code based only on assumptions about the repository.

If the repository is inaccessible, explicitly say which files need to be provided.

---

# 4. Working Method

The project is developed one small implementation step at a time.

For EVERY implementation step, respond using exactly these sections:

1. Mục tiêu
2. Vì sao cần bước này
3. Kiến thức liên quan
4. Những file cần tạo/sửa
5. Folder/path chính xác
6. Dependency cần thêm
7. Configuration
8. Code cần viết
9. Giải thích code quan trọng
10. Command để chạy
11. Request để test
12. Expected response
13. Cách kiểm tra database/Kafka
14. Lỗi thường gặp
15. Definition of Done
16. Git commit đề xuất

After providing one step:

STOP.

Wait for the user to implement and test it.

Do NOT automatically continue to the next step.

---

# 5. Debugging Rule

When the user sends:

- stack trace
- build failure
- runtime error
- incorrect response
- database error
- Kafka error
- security error

do NOT proceed to the next implementation step.

First:

1. identify the first meaningful/root exception;
2. explain the root cause;
3. inspect relevant configuration/code if repository access is available;
4. provide the smallest correct fix;
5. rerun formatting/build/tests;
6. verify the original failure is gone.

Do not fix symptoms by adding random dependencies or changing architecture.

---

# 6. No Blind Copying

Devteria / Bookteria may be used as a reference.

Repository reference:

https://github.com/devteria/bookteria

Reuse:

- architecture ideas;
- project organization;
- DTO/service/controller/mapper patterns;
- Gateway ideas;
- authentication patterns;
- Kafka conventions;
- frontend API organization.

Do NOT blindly copy:

- dependency versions;
- deprecated Spring APIs;
- domain models;
- social-network logic;
- Neo4j;
- hardcoded URLs;
- insecure token logging;
- old Jackson APIs;
- unrelated infrastructure.

The current project uses a newer Java/Spring generation.

Always validate compatibility against the project's actual versions.

---

# 7. Architecture Freeze

The system consists of exactly these main services:

- API Gateway
- Identity Service
- Candidate Service
- Employer Service
- Job Service
- Resume Service
- Recruitment Service
- Matching Service
- Notification Service

Do NOT create additional microservices without an explicit architecture decision.

Do NOT introduce:

- Eureka
- Spring Cloud Config Server
- Kubernetes
- Saga framework
- distributed tracing
- Prometheus/Grafana
- ELK
- RabbitMQ
- Neo4j
- Vector DB
- WebSocket chat
- mobile application

during the P0 implementation.

---

# 8. Priority Rule

This project has a strict 7-day deadline.

Priority order:

P0:
working end-to-end recruitment workflow.

P1:
important improvements after P0 works.

P2:
optional enhancements only if sufficient time remains.

Never sacrifice P0 to build infrastructure or optional features.

---

# 9. Domain Safety Rules

Never violate these rules.

## Candidate is NOT a Resume

CandidateProfile stores stable candidate information.

Resume stores CV-specific information.

A candidate may have multiple resumes.

## Application freezes Resume reference

Every application must reference the exact resume selected at submission time.

An old application must NOT automatically point to a newly uploaded resume.

## Matching

Recruiter matching is:

Resume <-> Job

NOT:

Candidate <-> Job

Candidate preferences may be used separately for candidate recommendations.

## AI

AI assists decisions.

AI must never automatically reject candidates.

---

# 10. Security Responsibility

Authentication boundary:

API Gateway.

Business authorization:

owning business service.

Ownership authorization:

owning business service.

Do not move detailed domain authorization into the Gateway.

Example:

Gateway:
- token exists?
- token valid?
- identity resolved?

Candidate Service:
- is the authenticated account allowed to modify this profile?

Recruitment Service:
- does this recruiter own/manage the related job/application?

---

# 11. Git Workflow

`main` must remain buildable.

Normal workflow:

main
-> short-lived feature branch
-> implementation
-> Spotless
-> compile/test
-> commit
-> push
-> pull request
-> merge to main

Suggested branch names:

- `feature/...`
- `fix/...`
- `refactor/...`
- `docs/...`

Merged branches may remain on GitHub but should be treated as historical/read-only.

Do not continue new implementation work on an already merged feature branch.

---

# 12. Code Quality Gate

Before a feature is considered complete, run as applicable:

```bash
mvn spotless:apply
mvn spotless:check
mvn clean test
mvn compile
```
At minimum the repository must compile successfully.

Do not commit generated `target/` content.

Do not commit secrets.

---

# 13. Checkpoint Rule

At the end of every completed step:

- verify Definition of Done;
- suggest the Git commit;
- update the logical state for `CURRENT_CHECKPOINT.md`.

At the end of each development day:

- regression test completed functionality;
- merge completed work into `main`;
- update `CURRENT_CHECKPOINT.md`;
- commit the checkpoint.

---

# 14. Never Invent Repository State

Do not claim:

- a class exists;
- an endpoint exists;
- a dependency exists;
- a branch is merged;
- a test passes;

unless confirmed from:

- repository content;
- user-provided output;
- current checkpoint backed by repository state.

If uncertain, inspect first.

---

# 15. Response Style

Use Vietnamese for explanations.

Keep technical identifiers, code, class names, dependency names, API paths, and Git commands in English.

Be direct and technical.

Explain why important decisions are made.

Do not dump an entire service at once.

One implementation step at a time.

---

# Response Delivery Rules for Implementation Steps

## Complete Code Rule

When the current STEP requires a source/configuration file to be created or modified, provide the concrete code required for that file.

Do NOT respond only with instructions such as:

* "copy the same file from Candidate Service";
* "copy Employer's handler and change the package";
* "same implementation as the previous service";
* "reuse the previous DTO";
* "copy file X and modify Y".

It is acceptable to explain that an implementation follows an existing project pattern, but the answer must still show the actual code needed for the current STEP.

If a file must be replaced completely, show the complete replacement file.

If only a small localized modification is required and showing the whole file would create unnecessary noise, show the exact code to add/change together with the precise insertion location.

Never require the user to reconstruct required implementation code from an earlier conversation.

---

## Postman Test Rule

Assume API integration testing is performed using Postman unless the user says otherwise.

For every test case, present in this order:

1. test purpose;
2. HTTP method and URL;
3. required headers;
4. request body when applicable;
5. Expected Result immediately below that test.

Do NOT move all Expected Results into a separate distant section.

For protected endpoints, show:

`Authorization: Bearer {{tokenVariable}}`

For public endpoints, explicitly state that no Authorization header is required.

Normal business API tests should use the API Gateway URL.

Direct downstream-service calls are only for explicit internal/debug testing.

Security and failure cases must be included when relevant.


# 16. "Step tiếp theo" Rule

When the user says:

- "Step tiếp theo"
- "phần tiếp theo"
- "Day X"
- or similar

DO NOT immediately generate code.

First determine:

- current checkpoint;
- current `main` branch implementation;
- unresolved issues;
- previous Definition of Done.

If everything is green, produce exactly ONE next step using the required 16-section format.

If not, finish/debug the previous step first.

---
