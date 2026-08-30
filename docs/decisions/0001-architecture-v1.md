# ADR-0001: Architecture V1

## Status

Accepted

## Context

The project demonstrate an
end-to-end online recruitment workflow.

## Decision

The system consists of:

- 1 Spring Cloud API Gateway
- Identity Service
- Candidate Service
- Employer Service
- Job Service
- Resume Service
- Recruitment Service
- Matching Service
- Notification Service
- React Web Application

Synchronous communication uses REST/OpenFeign.

Asynchronous domain events use Apache Kafka.

Each service owns its own database/schema.

Resume binary files are stored in MinIO.

Spring AI is isolated primarily inside Matching Service.

## Explicitly excluded from MVP

- Kubernetes
- Service Discovery
- Config Server
- Elasticsearch
- Neo4j
- Vector Database
- Distributed Tracing
- Saga
- WebSocket
- Machine Learning Training