# DEVELOPMENT GUIDE — Recruitment Platform

## 1. Java Package Convention

Root Java namespace:

`com.tbm.recruitment`

Examples:

- `com.tbm.recruitment.identity`
- `com.tbm.recruitment.gateway`
- `com.tbm.recruitment.candidate`
- `com.tbm.recruitment.employer`
- `com.tbm.recruitment.job`
- `com.tbm.recruitment.resume`
- `com.tbm.recruitment.recruitment`
- `com.tbm.recruitment.matching`
- `com.tbm.recruitment.notification`

Do not switch package namespace without an explicit migration decision.

---

# 2. Technology Versions

Current locked baseline:

- Java 25 LTS
- Spring Boot 4.0.8
- Spring Cloud 2025.1.3
- Spring AI 2.0.1
- Lombok 1.18.46
- MapStruct 1.6.3
- Spotless Maven Plugin 3.10.0

When adding a dependency:

- check compatibility with Spring Boot 4 / Spring Framework 7;
- do not copy dependency versions from older Devteria projects blindly.

---

# 3. Spring Boot 4 Compatibility

Spring Boot 4 belongs to the newer Spring Framework generation.

Be careful when copying code written for:

- Spring Boot 2;
- Spring Boot 3;
- older Spring Security;
- older Spring Cloud Gateway.

Jackson also changed in the Boot 4 generation.

Do not casually paste old Jackson 2 imports/configuration.

Prefer avoiding custom JSON manipulation unless necessary.

---

# 4. Lombok Convention

For Controllers and Services, prefer the Devteria-style pattern when suitable:

```
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) 
````
Example:

```java
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExampleService {

    ExampleRepository exampleRepository;
    ExampleMapper exampleMapper;
}
```

Do NOT blindly use:

`makeFinal = true`

on:

- JPA entities;
- mutable configuration fields;
- `@Value` injected fields unless handled intentionally.

---

# 5. MapStruct Convention

MapStruct is the standard DTO/entity mapping approach for business services.

Current version:

`1.6.3`

Lombok + MapStruct must use annotation processor configuration including:

- Lombok processor;
- `lombok-mapstruct-binding`;
- MapStruct processor.

Use:

```
@Mapper(componentModel = "spring")
```

or the project-wide default Spring component model.

Mapper examples:

- `AccountMapper`;
- `CandidateMapper`;
- `EmployerMapper`;
- `JobMapper`;
- `ResumeMapper`;
- `ApplicationMapper`.

Do NOT create empty `mapper` packages just for folder structure.

Create a mapper when actual mapping exists.

Do NOT put business logic into mapper expressions.

Examples of logic that must stay in Service:

- password hashing;
- authorization;
- entity ownership checks;
- status transitions;
- scoring;
- database lookups;
- Kafka publishing decisions.

MapStruct handles representation mapping.

---

# 6. Generated MapStruct Code

MapStruct generated implementations appear under:

`target/generated-sources/annotations`

Example:

`AccountMapperImpl.java`

Never manually edit generated implementations.

Never commit `target/`.

If a mapper cannot be injected:

1. verify generated implementation exists;
2. verify it is a Spring component;
3. inspect Maven annotation processor configuration;
4. inspect effective POM.

Do not manually implement a generated mapper just to silence the error.

---

# 7. DTO Convention

Separate request and response DTOs.

Typical layout:

```text
dto/
├── request/
└── response/
```

Do not return JPA entities directly from controllers.

Especially never expose security-sensitive entity fields such as:

- `passwordHash`.

Use explicit DTOs.

Java records may be used for immutable/simple request-response DTOs where appropriate.

Follow existing service style for consistency.

---

# 8. Layering

Typical business service structure:

```text
configuration/
controller/
dto/
    request/
    response/
entity/
enums/
exception/
mapper/
repository/
service/
```

Add packages only when they contain actual code.

Do not create decorative empty packages.

Typical call flow:

```text
Controller
-> Service
-> Repository
```

DTO mapping:

```text
Mapper
```

Cross-service calls:

```text
Client
```

Async integration:

```text
Kafka producer/consumer
```

---

# 9. Controller Rule

Controller responsibilities:

- receive HTTP request;
- validation boundary;
- authenticated principal/context extraction where appropriate;
- call Service;
- return API DTO.

Controller should NOT contain:

- database logic;
- complicated business rules;
- status machine logic;
- password encoding;
- Kafka orchestration logic.

---

# 10. Service Rule

Service owns business logic.

Examples:

- account registration rules;
- candidate ownership;
- company authorization;
- job lifecycle;
- resume ownership;
- application state transitions.

Use transactions deliberately where persistence consistency is required.

---

# 11. Repository Rule

Repositories provide persistence access.

Do not implement business authorization inside repository interfaces.

Repository query methods should answer persistence questions.

Business decisions belong in Service.

---

# 12. Entity Rule

Do not expose entities directly through REST.

JPA entities may use Lombok carefully.

Avoid generated equality/toString behavior that can cause issues with lazy JPA relations.

Do not use `@Data` blindly on complex JPA entities.

---

# 13. API Response Convention

Follow existing `ApiResponse<T>` style.

Typical success:

```json
{
  "code": 1000,
  "message": "Success",
  "result": {}
}
```

Use:

- meaningful HTTP status;
- service-specific `ErrorCode`;
- `GlobalExceptionHandler` where appropriate.

Do not invent a different response convention for each service.

---

# 14. Error Code Convention

Ranges:

- Identity: `1xxx`
- Candidate: `2xxx`
- Employer: `3xxx`
- Job: `4xxx`
- Resume: `5xxx`
- Recruitment: `6xxx`
- Matching: `7xxx`
- Notification: `8xxx`
- Gateway: `9xxx`

Do not duplicate unrelated error codes across services when avoidable.

---

# 15. Security

Never trust:

- `accountId` from request body;
- `candidateId` from request body;
- `recruiterId` from request body;

when authenticated identity should determine ownership.

For self-service operations derive identity from authenticated context.

Example:

Bad:

```json
{
  "candidateId": "some-user-selected-id"
}
```

for updating one's own profile.

Better:

```text
authenticated account
-> resolve candidate ownership
-> update own profile
```

Detailed implementation may differ by service, but ownership must not rely on arbitrary client identifiers.

---

# 16. Gateway

Gateway uses Spring Cloud Gateway WebFlux.

Current Gateway dependency family:

`spring-cloud-starter-gateway-server-webflux`

Do not add:

`spring-boot-starter-web`

to the reactive Gateway.

Do not use blocking Feign directly inside Gateway authentication flow.

Do not log raw authorization tokens.

---

# 17. API Versioning

External API:

`/api/v1/...`

Internal downstream service controllers do NOT need `/api/v1`.

Example:

External:

`/api/v1/identity/auth/login`

Internal Identity:

`/identity/auth/login`

---

# 18. Environment Configuration

Real local secrets:

`.env`

Never commit `.env`.

Committed template:

`.env.example`

Examples of secret/environment values:

- `JWT_SIGNER_KEY`;
- DB passwords;
- Mongo credentials;
- MinIO credentials;
- AI API key.

Never hardcode real secrets into Java/YAML committed to Git.

---

# 19. Run Scripts

Local PowerShell helper scripts belong in root:

`scripts/`

Examples already established:

- `scripts/run-identity.ps1`;
- `scripts/run-gateway.ps1`.

Run scripts may load root `.env` into process environment.

Do not scatter environment scripts into individual service folders without reason.

---

# 20. Spotless

Run after implementation:

```bash
mvn spotless:apply
mvn spotless:check
```

Do not manually spend excessive time fighting formatter layout.

Existing JDK 25 warnings from tools such as Lombok/Spotless should not block progress when builds pass, unless they become actual errors.

---

# 21. Maven Quality Gate

Before merge:

```bash
mvn spotless:apply
mvn spotless:check
mvn clean test
```

At minimum:

```bash
mvn clean compile
```

must succeed.

Never treat runtime startup as a substitute for compile/test.

---

# 22. Git Convention

Workflow:

```text
main
-> feature branch
-> implementation
-> formatting
-> testing
-> commit
-> push
-> PR
-> merge
```

Examples:

- `feature/candidate-profile`
- `feature/employer-company`
- `feature/job-management`
- `fix/identity-mapstruct`

Commit examples:

- `feat(candidate): implement candidate profile`
- `feat(job): add job creation and publishing`
- `fix(identity): configure MapStruct annotation processing`
- `docs: update Day 2 checkpoint`

Do not mix unrelated features into one large commit if avoidable.

---

# 23. Devteria / Bookteria Usage

Bookteria reference repository:

<https://github.com/devteria/bookteria>

Approximate reuse guidance already established:

Gateway:

reuse routing/filter/introspection concepts.

Identity:

reuse authentication/JWT/service structure concepts.

Profile:

reuse layering/DTO/mapper shape only.

Do NOT reuse Neo4j/social graph.

Post:

reuse CRUD/pagination concepts for Job.

Do NOT copy social-post domain.

File:

reuse upload/storage abstraction ideas for Resume.

Use MinIO and recruitment ownership rules.

Notification:

reuse Kafka/Mongo conventions.

Build recruitment notifications, not social notifications.

Frontend:

reuse Axios/auth/API organization ideas only.

Build recruitment UI from new Vite React app.

Docker:

reuse Kafka KRaft ideas.

No Neo4j.

Never log JWT like the Bookteria Gateway example did.

---

# 24. Avoid Overengineering

Before adding a new framework/infrastructure dependency, ask:

> Does P0 require this in the next 7 days?

If no, defer it.

Simple correct code is preferred over architecture theater.

---

# 25. Definition of Done Discipline

A step is not complete because:

> "the code was written".

A step is complete when:

- application compiles;
- service starts if applicable;
- request works;
- expected response matches;
- persistence is verified when applicable;
- failure/security cases are checked;
- formatting passes;
- tests/build pass.

Only after that proceed to the next step.
````
