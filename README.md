\# Realtime Notification System



A scalable realtime notification system built with Spring Boot, SSE, Redis, PostgreSQL, Angular, Docker, and Kubernetes.



\## Architecture

Angular Frontend

&#x09;|

&#x20;REST + SSE

&#x09;|

Spring Boot Backend (horizontally scalable)

|              |

PostgreSQL      Redis

(storage)    (pub/sub + cache + rate limiting)



\### How it works

\- REST APIs handle creating, fetching, and marking notifications

\- SSE (Server-Sent Events) pushes notifications to connected clients in realtime

\- Redis Pub/Sub ensures notifications reach the correct pod in a multi-pod deployment

\- Redis caches unread counts for fast reads, falls back to DB if Redis is down

\- Rate limiting uses Redis fixed window (10 req/60s per IP)

\- Idempotency keys prevent duplicate notifications on retried requests



\## Tech Stack

\- \*\*Backend\*\*: Spring Boot 3.2.5, Java 21

\- \*\*Database\*\*: PostgreSQL 15

\- \*\*Cache/PubSub\*\*: Redis 7

\- \*\*Realtime\*\*: Server-Sent Events (SSE)

\- \*\*Frontend\*\*: Angular 17

\- \*\*Infra\*\*: Docker, Docker Compose, Kubernetes (minikube)



\## Project Structure

├── src/

│   └── main/java/com/realtime/notification/

│       ├── app/          # Main application class

│       ├── controller/   # REST + SSE controllers

│       ├── service/      # Business logic

│       ├── repository/   # JPA repositories

│       ├── entity/       # JPA entities

│       ├── dto/          # Request DTOs

│       ├── config/       # Redis configuration

│       ├── ratelimit/    # Rate limiting annotation + aspect

│       └── sse/          # SSE connection manager, publisher, subscriber

├── k8s/                  # Kubernetes deployment YAMLs

├── Dockerfile

├── docker-compose.yml

└── notification-dashboard/  # Angular frontend







\## API Documentation



\### Create Notification





POST /api/notifications

Content-Type: application/json

{

"userId": 1,

"message": "You have a new alert",

"idempotencyKey": "unique-key-001"

}



\### Get User Notifications

GET /api/notifications/user/{userId}



\### Get Unread Count

GET /api/notifications/user/{userId}/unread-count



\### Mark as Read

PATCH /api/notifications/{id}/read



\### SSE Subscribe

GET /api/sse/subscribe/{userId}



\## Running Locally



\### Prerequisites

\- Java 21

\- Maven

\- Docker Desktop



\### Start dependencies

```bash

docker-compose up -d postgres redis

```



\### Run backend

```bash

mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Duser.timezone=Asia/Kolkata"

```



\### Run frontend

```bash

cd notification-dashboard

ng serve

```



Open http://localhost:4200



\## Running with Docker Compose

```bash

docker-compose up --build

```



\## Running with Kubernetes

```bash

minikube start

minikube image load real-time-notification-app:latest

kubectl apply -f k8s/

minikube service notification-app --url

```



\## Production Constraints Addressed



| Constraint | Solution |

|---|---|

| Multiple devices per user | `ConcurrentHashMap<userId, List<SseEmitter>>` |

| No duplicate notifications | Idempotency key stored in DB, checked before save |

| Redis crash resilience | Try/catch on all Redis calls, fallback to DB |

| Horizontal scaling | Redis Pub/Sub — any pod can push to any client |



\## AI Usage Declaration

AI tools were used to assist in code generation and architectural decisions. Every line of code has been reviewed, understood, and is explainable by the Me.

