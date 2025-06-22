# 🚀 DevSync

**DevSync** is a collaborative knowledge management platform for developers and teams.  
It allows users to create, organize, and share technical knowledge through notes, questions, discussions, and more, combining the best of Notion, Stack Overflow, and internal wikis.

This project is the **biggest personal challenge** of my software development career. It's designed to showcase my experience in full-stack development and DevOps, with a modular architecture inspired by **Domain-Driven Design (DDD)**.

---

## 🌟 Goals

- 🧠 Centralize technical knowledge in a structured and searchable way
- 📋 Enable rich note-taking with Markdown and code highlighting
- 🤝 Facilitate collaboration through comments and questions
- 🔍 Improve discoverability of content via tags and full-text search
- 📐 Apply real-world software architecture patterns using DDD principles

---

## 🧰 Tech Stack

### Backend (Java)
- Spring Boot 3.x
- Spring Data JPA
- PostgreSQL
- Spring Security + JWT / OAuth2
- Actuator, OpenAPI (Swagger)
- Flyway (DB migrations)
- Docker

### Frontend (Angular)
- Angular 17+
- Angular Material or Tailwind CSS
- RxJS, Reactive Forms
- Markdown editor
- Responsive layout

### DevOps
- Docker Compose (for local dev)
- Kubernetes (future plan)
- CI/CD pipeline (GitHub Actions)
- Monitoring (Prometheus, Grafana)

---

## 📁 Architecture

The project follows a layered architecture loosely based on **Domain-Driven Design (DDD)**:

``` text
DevSync
├── domain
├── application
├── infrastructure
├── interface (REST API)
├── shared
└── DevSyncApplication.java
```


- **Domain**: Business logic, aggregates, and models
- **Application**: Use cases and coordination
- **Infrastructure**: DB access, external systems
- **Interface**: API controllers and DTOs

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Docker & Docker Compose
- PostgreSQL (or use Docker image)
