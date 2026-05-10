Note: Since two separate services (frontend and backend) were created in Render, it may take some time for the website to work without 5xx error. The backend service is available at https://loan-application-d9vu.onrender.com/swagger-ui/index.html

---

# Loan Application

A web application for submitting and processing loan applications. The system validates the applicant, generates a payment schedule, and allows a manager to approve or reject the application.

## Tech Stack

- **Backend:** Java 21, Spring Boot 3.5, PostgreSQL, Liquibase, Swagger
- **Frontend:** React, Vite, nginx
- **Infrastructure:** Docker, Docker Compose

## Getting Started

The only requirement is Docker. Run the entire stack with a single command:

```
docker compose up --build
```

Once running:

- Frontend: http://localhost:3000
- Swagger UI: http://localhost:8080/swagger-ui/index.html

## How It Works

1. A customer submits a loan application with their name, Estonian personal ID code, loan amount, period, and interest margin.
2. The system validates the applicant's age. If the customer is older than 70, the application is automatically rejected.
3. If validation passes, an annuity payment schedule is generated and the application moves to the review stage.
4. A manager reviews the application and payment schedule, then either approves it or rejects it with a reason.

## Project Structure

```
loanapp/
├── backend/       Java + Spring Boot
├── frontend/      React + Vite
└── docker-compose.yaml
```

## Features

- Estonian personal ID code validation with checksum
- Automatic age check with configurable age limit
- Annuity payment schedule generation
- Schedule regeneration with updated parameters
- Dynamic configuration (base interest rate, max age) managed via database
- Error handling with separate business and technical exceptions
- Unit tests with Mockito, integration tests with Testcontainers
