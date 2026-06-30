🚀 Freelance Marketplace Platform

A cloud-native freelance marketplace built using a Microservices Architecture with Spring Boot. The platform is inspired by modern freelance platforms and demonstrates scalable backend development using distributed services, event-driven communication, containerization, orchestration, and observability.

The system follows microservices best practices where each service owns its own database and communicates using both synchronous REST APIs and asynchronous messaging.

⸻

✨ Features

* User authentication and authorization
* User profile management
* Job posting and management
* Proposal submission and lifecycle
* Contract management
* Wallet and payout management
* RESTful APIs
* Service-to-service communication with OpenFeign
* Event-driven communication using RabbitMQ
* Saga-based workflows
* Kubernetes deployment
* Centralized monitoring and logging

⸻

🛠️ Tech Stack

Backend

* Java
* Spring Boot
* Spring Security
* Spring Data JPA
* Hibernate
* OpenFeign
* Maven

Messaging

* RabbitMQ
* Topic Exchanges
* Routing Keys
* Publishers
* Consumers
* Dead Letter Queues (DLQ)

Databases

* PostgreSQL
* MongoDB
* Redis
* Cassandra
* Neo4j
* Elasticsearch

DevOps & Infrastructure

* Docker
* Docker Compose
* Kubernetes
* ConfigMaps
* Secrets

Monitoring & Observability

* Spring Boot Actuator
* Prometheus
* Grafana
* Loki

⸻

🏗️ Architecture

The platform consists of multiple independent microservices:

* User Service
* Job Service
* Proposal Service
* Contract Service
* Wallet Service

Each service:

* Owns its own database
* Can be developed independently
* Can be deployed independently
* Communicates with other services using REST APIs or RabbitMQ events

⸻

🔄 Communication

The system uses two communication approaches.

1. Synchronous Communication

Services communicate directly using OpenFeign.

Examples include:

* User Service → Contract Service
* User Service → Wallet Service
* Proposal Service → Contract Service

This approach is used when an immediate response is required.

⸻

2. Asynchronous Communication

Services communicate through RabbitMQ events.

Typical flow:

Service publishes event

↓

RabbitMQ Topic Exchange

↓

Routing Key

↓

Matching Queue

↓

Consumer receives event

↓

Business logic is executed

This allows services to remain loosely coupled and improves scalability.

⸻

📡 Event-Driven Architecture

Published events include:

* user.registered
* user.deactivated
* proposal.completed
* proposal.cancelled
* contract.created
* contract.status-changed
* payment.initiated
* payment.completed
* payment.failed
* payment.refunded
* job.closed

RabbitMQ routes each event to the appropriate queues based on its routing key.

Consumers process the event asynchronously without blocking the publisher.

⸻

☠️ Dead Letter Queues (DLQ)

Each consumer queue is configured with a corresponding Dead Letter Queue.

If a consumer cannot process a message:

* RabbitMQ retries the message
* If all retry attempts fail
* The message is moved automatically to its DLQ

This prevents message loss while allowing failed events to be inspected later.

⸻

📦 Microservices

User Service

Responsible for:

* User registration
* Authentication
* User management
* Freelancer statistics
* User reporting

⸻

Job Service

Responsible for:

* Job creation
* Job updates
* Job lifecycle

⸻

Proposal Service

Responsible for:

* Proposal creation
* Proposal lifecycle
* Saga orchestration

⸻

Contract Service

Responsible for:

* Contract creation
* Contract management
* Contract reporting

⸻

Wallet Service

Responsible for:

* Payments
* Wallet balances
* Payouts
* Earnings

⸻

☸️ Kubernetes Deployment

Each microservice is deployed using Kubernetes.

Deployment resources include:

* Deployment
* Service
* ConfigMap
* Secrets

Health monitoring is provided through:

* Readiness Probes
* Liveness Probes

Configuration is externalized using ConfigMaps and Secrets, allowing services to be configured without rebuilding the application.

⸻

📊 Monitoring & Observability

Application metrics are exposed using Spring Boot Actuator.

Prometheus periodically collects metrics from each service.

Grafana visualizes those metrics through dashboards.

Application logs are pushed to Loki and viewed through Grafana.

Collected information includes:

* HTTP request count
* Request latency
* JVM memory usage
* CPU usage
* Error rates
* Service health
* Application logs

⸻

🗄️ Databases

The platform follows the Database per Service pattern.

Technologies used include:

* PostgreSQL
* MongoDB
* Redis
* Cassandra
* Neo4j
* Elasticsearch

This architecture eliminates direct cross-service database access and improves service independence.

⸻

# 🚀 Getting Started

## Clone the repository

```bash
git clone https://github.com/Youssefeslamm/35-TheFourthPyramid-Freelance.git
```

## Navigate to the project

```bash
cd 35-TheFourthPyramid-Freelance
```

## Start the infrastructure

```bash
docker compose up --build
```

## Deploy the services to Kubernetes

```bash
kubectl apply -f k8s/
```

## Verify the deployment

```bash
kubectl get pods -n freelance
```

---

# 📁 Project Structure

```text
.
├── api-gateway
├── user-service
├── job-service
├── proposal-service
├── contract-service
├── wallet-service
├── contracts
├── k8s
├── docker-compose.yml
└── README.md
```

⸻

📚 Technologies Demonstrated

* Microservices Architecture
* Distributed Systems
* REST APIs
* OpenFeign
* RabbitMQ
* Event-Driven Architecture
* Saga Pattern
* Docker
* Kubernetes
* Spring Boot Actuator
* Prometheus
* Grafana
* Loki
* PostgreSQL
* MongoDB
* Redis
* Cassandra
* Neo4j
* Elasticsearch

⸻

📄 License

This repository was developed as an academic software engineering project for educational purposes.
