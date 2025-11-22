# Bookstore Platform (Microservices)

Microservices-based backend for an online bookstore, implemented in **Java / Spring Boot**.  
The system is split into several independent services that communicate over HTTP through an API Gateway and are registered in a Eureka discovery server.

The project is intended as a learning / portfolio application to demonstrate:

- Designing a small microservice architecture
- Working with Spring Cloud (Eureka, API Gateway)
- Implementing REST APIs for typical e‑commerce flows (users, products, cart, orders, payments)
- Containerised local development with Docker Compose and MySQL

---

## Features (high level)

- **User management**
  - Registration and login
  - JWT‑based authentication (user‑service)
- **Product catalog**
  - Managing books (title, author, price, etc.) in the `product-service`
- **Shopping cart**
  - Storing items added by a user in the `cart-service`
  - Cart holds product IDs and prices at the time of adding
- **Orders**
  - Creating orders in the `order-service` based on the cart contents
  - Marking orders as ready for shipment after successful payment
- **Payments**
  - Dedicated `payment-service` designed to integrate with **Stripe** (sandbox)
  - Webhook handling so that a payment is treated as successful only after it is persisted correctly
- **API Gateway & Service discovery**
  - Single entry point for the client through `api-gateway`
  - Service registration and discovery via `eureka-server`

> **Note:** The project is under active development – individual services may be at different levels of completeness.

---

## Tech stack

- **Language & Framework**
  - Java (17+)
  - Spring Boot (Web, Data JPA, Security in selected services)
  - Spring Cloud Netflix (Eureka Server, Eureka Client, API Gateway)

- **Persistence**
  - MySQL
  - SQL schema / seed data in `init.sql`

- **Build & tooling**
  - Maven (multi‑module project, root `pom.xml`)
  - Docker & Docker Compose

---

## Repository structure

Root of the project:

```text
pawoom-microservices/
  api-gateway/        # Spring Cloud API Gateway (single entry point)
  cart-service/       # Shopping cart service
  eureka-server/      # Service discovery (Eureka)
  order-service/      # Order management
  payment-service/    # Payment processing (Stripe integration)
  product-service/    # Book catalog
  user-service/       # User registration, login, JWT auth
  docker-compose.yml  # Local development environment
  init.sql            # Initial database schema / data
  pom.xml             # Parent Maven configuration
```

Each service is a separate Spring Boot application with its own `pom.xml` and configuration.

---

## Running the project locally

You can run the system either with **Docker Compose** (recommended for a quick demo) or by starting services manually from your IDE / terminal.

### Option 1 – Docker Compose (recommended)

#### Prerequisites

- Docker
- Docker Compose (or Docker Desktop with Compose support)

#### Steps

From the root folder of the project:

```bash
docker-compose up --build
```

This should:

- start a MySQL instance with schema/data from `init.sql`
- start the Eureka server
- start all microservices
- start the API Gateway

The API Gateway is configured to expose the backend on a single port (e.g. `http://localhost:8001` – see `api-gateway` configuration for the exact value).

Once everything is up, all client traffic should go through the gateway.

To stop the environment:

```bash
docker-compose down
```

---

### Option 2 – Running services manually

#### Prerequisites

- Java 17+
- Maven
- Local MySQL instance (configured according to the properties in each service)
- Optional: Docker only for the database

#### Steps

1. Start MySQL locally and create the databases/schema as required  
   (optionally using `init.sql` as a base).

2. Build the project:

   ```bash
   mvn clean install
   ```

3. Start the Eureka server:

   ```bash
   cd eureka-server
   mvn spring-boot:run
   ```

4. Start the API Gateway:

   ```bash
   cd api-gateway
   mvn spring-boot:run
   ```

5. Start the remaining services (each in its own terminal):

   ```bash
   cd user-service
   mvn spring-boot:run

   cd product-service
   mvn spring-boot:run

   cd cart-service
   mvn spring-boot:run

   cd order-service
   mvn spring-boot:run

   cd payment-service
   mvn spring-boot:run
   ```

Services will register with Eureka and become available behind the API Gateway.

---

## Example responsibilities of individual services

> **Important:** exact endpoint paths may change during development – treat this section as a conceptual overview rather than strict API documentation.

- **user-service**
  - Registration and login endpoints  
  - Issues JWT tokens used by the client and (optionally) other services

- **product-service**
  - CRUD operations for books  
  - Searching and listing products

- **cart-service**
  - Storing cart items for a given user  
  - Returning the current contents of the cart with prices used later by the `order-service`

- **order-service**
  - Creating orders once the cart is paid  
  - Changing order status (e.g. NEW → PAID → TO_SHIP)

- **payment-service**
  - Initiating payments via Stripe (sandbox mode)  
  - Handling Stripe webhooks and confirming payments only after the payment record is persisted

- **api-gateway**
  - Routing external requests to the appropriate microservice  
  - Central place for cross‑cutting concerns (logging, security, rate limiting – to be extended)

- **eureka-server**
  - Service registry that keeps track of available microservices

---

