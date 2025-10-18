# Bank Microservices Project

This project is composed of multiple microservices using the Netflix stack (~~Eureka~~, Feign, Resilience4j) with Spring Boot and Maven.
## Jenkins + Docker + Kubernetes + Helm
The project automates the CI/CD process for banking microservices. It performs:
- code build and testing;
- Docker image building;
- publishing images to GitHub Container Registry (GHCR);
- deployment to a Kubernetes cluster using Helm.

All stages are executed through Jenkins Pipeline.

## Prerequisites
To run the project, you will need add such entries to your `/etc/hosts` file
```/etc/hosts
127.0.0.1 customer.prod.local
127.0.0.1 fraud.prod.local
127.0.0.1 notification.prod.local
127.0.0.1 front-ui.prod.local
127.0.0.1 keycloak.prod.local
127.0.0.1 apigw.prod.local
127.0.0.1 exchange.prod.local
127.0.0.1 exchange-generator.prod.local
127.0.0.1 cash.prod.local
127.0.0.1 transfer.prod.local
```

## Spring Profiles
The project supports two Spring profiles:

- **default**
  used for local development, where Keycloak and each DB can be run as Docker containers (Docker Compose) and all the core microservices directly via Maven.
- **kube**
  Used for Docker Desktop Kubernetes cluster. This profile contains Kubernetes-specific configuration parameters for service endpoints, Keycloak issuer URLs, database connections, etc.
  Pods in Kubernetes do not automatically resolve *.prod.local domains to the ingress controller unless you explicitly configure /etc/hosts mapping inside pods using hostAliases. You must set hostAliases in deployment.yaml >> Replace 10.1.0.88 with the current IP of your ingress controller (check via kubectl get pod -n ingress-nginx -o wide).

You can switch profiles by setting the SPRING_PROFILES_ACTIVE environment variable.

## Modules

- **Customer**  
  Handles customer registration and account management. It uses Resilience4j circuit breaker and retry functionalities. The service communicates with the Fraud service using Feign clients.

- **Fraud**  
  Provides endpoints to check if a customer is fraudulent. It stores fraud check history and uses a service to determine fraud status.

- **Exchange**  
  Manages currency exchange rates for the banking system. Provides real-time currency rates relative to RUB (base currency) and handles rate updates. Supports RUB, USD, and CNY currencies. The service stores exchange rates in PostgreSQL and integrates with the Exchange Generator service for automatic rate updates.

- **Exchange Generator**  
  A separate service that provides exchange rates for RUB, USD, and CNY. Updates the Exchange service with new rates every 5 seconds via Feign Client. It uses JSON file with exchange rates as a data source (using Round Robin strategy for rate selection).

- **Clients**  
  Contains shared Feign clients and response object definitions used for inter-service communication.

- **Notification**  
  Handles sending notifications to customers. It uses a PostgreSQL database for persistence. The service receives notification requests from other microservices via Feign clients and processes them asynchronously.

- **API Gateway**  
  Routes incoming requests to the appropriate microservices. It is built using Spring Cloud Gateway and provides load balancing, routing, and fallback capabilities. The gateway uses filters to handle cross-cutting concerns like error handling and circuit breaking.

- **FRONT UI**  
  Provides a web user interface using Thymeleaf. The service runs on port `8085` and integrates with other microservices via Feign clients. It exposes endpoints such as `/` for the landing page (redirecting to `/main`) and `/main?login={login}` to render the main page with dynamic user data. The UI automatically updates exchange rates every second using JavaScript.
 
- **Cash**
  Handles cash-in (deposit) and cash-out (withdraw) operations on user accounts.

- **Transfer**
  Handles money transfers between customers accounts.


## Technologies

- Spring Boot
- Spring Cloud OpenFeign, Load Balancer
- Resilience4j (Circuit breaker and Retry)
- Docker Desktop (with k8s), Docker Compose
- Jenkins
- Helm
- PostgreSQL (database per service)
- Maven
- Keycloak (Authorization & Authentication)

## Security & Authentication

- **Keycloak** (OIDC, port 8090 (if not k8s), realm: `bank-realm`) provides authentication and JWT tokens.
- **Front-UI** uses Authorization Code Flow with Keycloak.
- **API Gateway** and all backend microservices (Customer, Exchange, Notification, Fraud, Cash, Transfer) are set as resource servers and validate Keycloak JWT tokens.
- Service-to-service calls use confidential Keycloak clients (service accounts) for authentication.
- On login, user is redirected to Keycloak, receives JWT (proxied by Gateway), and all service calls are authorized. On logout, SSO session is ended via Keycloak.
- **Just-In-Time Provisioning:** on first login, Front-UI syncs profile with Customer service.

## Service Endpoints

### API Gateway

- All requests pass through the API Gateway on apigw.prod.local (k8s) / localhost:8080 (local)`.
- **Example:**  
  - **GET** `/api/v1/customers/public`  
    Routes the request to the Customer service.
  - **GET** `/api/rates`  
    Routes the request to the Exchange service for currency rates.

### Customer Microservice

- **POST** `/api/v1/customers/signup`  
  Registers a new customer using a JSON body with fields: `login`, `password`, `confirmPassword`, `name`, `email`, and `birthdate`.

- **GET** `/api/v1/customers/public`  
  Public endpoint to verify service availability.

- **GET** `/api/v1/customers/main?login={login}`  
  Retrieves main page data for a specific user including account information, available currencies, and user lists.

- **POST** `/api/v1/customers/user/{login}/editPassword`  
  Changes user password. Accepts JSON body with `password` and `confirmPassword` fields.

- **POST** `/api/v1/customers/user/{login}/editUserAccounts`  
  Updates user profile and account settings. Accepts JSON body with `name`, `birthdate`, and `accounts` fields.

### Exchange Microservice

- **GET** `/api/rates`  
  Retrieves current currency exchange rates. Returns a JSON array with exchange rate information for all supported currencies (RUB, USD, CNY).

- **PUT** `/api/rates/update`  
  Updates currency exchange rates. Accepts a JSON array of exchange rate objects with `title`, `name`, and `value` fields.

### Fraud Microservice

- **GET** `/api/v1/fraud-check/{customerId}`  
  Checks if the customer with the provided \`customerId\` is fraudulent. Stores the fraud check history.

### Notification Microservice

- **POST** `/api/v1/notifications`  
  Sends a notification to a customer.
- The service is configured to run on port \`8083\` and uses PostgreSQL for data storage.

### Clients Module

- Provides Feign clients for inter-service communication such as contacting the Fraud, Notification, and Exchange services.

### FRONT UI Microservice

- **GET** `/`  
  Redirects to the main page.
  
- **GET** `/main?login={login}`  
  Renders the main page using Thymeleaf and loads dynamic user data for the specified user. Automatically fetches and displays current exchange rates.

- **GET** `/signup`  
  Displays the customer registration form.

- **POST** `/signup`  
  Registers a new customer. Accepts form data with `login`, `password`, `confirm_password`, `name`, `email`, and `birthdate` fields.

- **POST** `/user/{login}/editPassword`  
  Processes password change form submission. Accepts form data with `password` and `confirm_password` fields.

- **POST** `/user/{login}/editUserAccounts`  
  Processes user account update form submission. Accepts form data with `name`, `birthdate`, and `accounts` fields.

- **POST** `/user/{login}/cash` — send cash-in/cash-out request (form invokes Cash service via Backend). Form accepts 'PUT'/'GET' as an action
- **POST** `/user/{login}/cash`
  - **JSON body:**
    ```json
    {
      "login": "john",
      "currency": "RUB",
      "value": 1000.00,
      "action": "PUT"
    }
    ```
  - **Valid values for `action`:** `"PUT"` or `"GET"`. (only form FRONT-UI)
  
### Cash Microservice

- **POST** `/api/v1/cash/operation`
  - **JSON body:**
    ```json
    {
      "login": "john",
      "currency": "RUB",
      "value": 1000.00,
      "action": "DEPOSIT"
    }
    ```
  - **Valid values for `action`:** `"DEPOSIT"` or `"WITHDRAW"`.
  - **Response:**
    ```json
    {
      "success": true,
      "errors": []
    }
    ```
    - **Example of insufficient funds response:**
    ```json
    {
      "success": false,
      "errors": ["Insufficient funds. Available: 20.00 RUB"]
    }
    ```
### Transfer Microservice

- **POST** `/user/{login}/transfer`  
  Transfers money between accounts. Supports transfers within same user accounts or to different users with automatic currency conversion.
  - **JSON body:**
    ```
    {
      "fromCurrency": "USD",
      "toCurrency": "RUB", 
      "value": 100.00,
      "toLogin": "recipient_user"
    }
    ```
  - **Response:**
    ```
    {
      "success": true,
      "transferErrors": [],
      "transferOtherErrors": []
    }
    ```
  - **Currency conversion examples:**
    - Same currency (RUB → RUB): No conversion, direct transfer
    - Different currencies (USD → RUB): Automatic conversion using Exchange service rates
    - Cross-currency (USD → CNY): Conversion through RUB as base currency

- **Example transfer scenarios:**
  - **Same currency transfer:** 100 RUB → 100 RUB (no Exchange service call)
  - **Currency conversion:** 10 USD → 950 RUB (using rate USD=95.0)
  - **Cross-currency:** 10 USD → 70.37 CNY (USD→RUB→CNY conversion)


## Architecture Overview

The system consists of the following microservices running on different ports (database per service is PostgreSQL):

- **Customer**: - User management and accounts
- **Fraud**: - Fraud detection
- **Notification**: - Notification handling  
- **API Gateway**: - Request routing and load balancing
- **Front UI**: - Web user interface
- **Exchange**: - Currency exchange rates management
- **Exchange Generator**: - Automatic exchange rate generation
- **Cash**: withdrawal and deposit operations
- **Transfer**: - Money transfers with currency conversion

## Running the Project locally
1. .env >> прописать свои значения для postgres (в докере если локально запускать)
2. ```docker-compose up -d```.
   - **Note:** Ensure that the Keycloak server is running on port `8090` and the realm is named `bank-realm`.
   - The Keycloak server will be used for authentication and authorization.

3. Build the project:
   ```bash
   mvn clean install
    ```
4. Run the services (using 'default' Spring Profile):
   ```bash
   mvn spring-boot:run
    ```
5. Register new customer `http://localhost:8080/signup`.
6. Access the application (api-gateway) at `http://localhost:8080`. After successful login redirect to http://localhost:8080/main 
7. Do logout (via `http://localhost:8080/logout` and create one more customer (to test transferring money between them).

## Running the Project in Kubernetes
1. Ensure you have a running Kubernetes cluster (e.g., Docker Desktop with Kubernetes enabled).
2. Install Helm if you haven't already.
3. Run jenkins (via docker-compose up -d --build in jenkins folder).
4. Open Jenkins at `http://localhost:8080` and trigger the `YandexHelmApp` job to deploy the application to your Kubernetes cluster.
5. Wait for the job to complete successfully. There is a manual approval step before deploying to production.
6. Make sure that keycloak is running in the cluster and accessible at `http://keycloak.prod.local`.
5. Access the application via the configured domain names (`http://apigw.prod.local/signup`).
6. After successful login redirect to http://apigw.prod.local/main

## Usage Examples

### Register a new customer (for k8s)
```bash
POST http://apigw.prod.local/signup
Content-Type: application/json

{
  "login": "john",
  "password": "securePassword123",
  "confirmPassword": "securePassword123", 
  "name": "John Doe",
  "email": "jdoe2@gmail.com",
  "birthdate": "1995-03-15"
}
```

### Change user password
```bash
POST http://apigw.prod.local/api/v1/customers/user/john/editPassword
Content-Type: application/json

{
  "password": "newPassword123",
  "confirmPassword": "newPassword123"
}
```

### Change user profile and accounts
```bash
POST http://apigw.prod.local/api/v1/customers/user/john/editUserAccounts
Content-Type: application/json

{
  "name" : "John Doe",
  "birthdate": "1995-03-15",
  "accounts": [
    "RUB",
    "USD"
  ]
}
```

### Get current exchange rates
```bash
GET http://apigw.prod.local/api/rates
Accept: application/json
```

**Response:**
```json
[
  {
    "title": "Рубль",
    "name": "RUB", 
    "value": 1.0
  },
  {
    "title": "Доллар",
    "name": "USD",
    "value": 93.50
  },
  {
    "title": "Юань", 
    "name": "CNY",
    "value": 13.20
  }
]
```

### Update exchange rates
```bash
PUT http://localhost:8086/api/rates/update
Content-Type: application/json

[
  {
    "title": "Рубль",
    "name": "RUB",
    "value": 1.0
  },
  {
    "title": "Доллар",
    "name": "USD",
    "value": 94.00
  },
  {
    "title": "Юань",
    "name": "CNY",
    "value": 13.50
  }
]
```

### DEPOSIT cash operation:
```http
POST http://localhost:8088/api/v1/cash/operation
Content-Type: application/json

{
  "login": "john",
  "currency": "RUB",
  "value": 500.00,
  "action": "DEPOSIT"
}
```

### WITHDRAW cash:
```http
POST http://localhost:8088/api/v1/cash/operation
Content-Type: application/json

{
  "login": "john",
  "currency": "USD",
  "value": 20.00,
  "action": "WITHDRAW"
}
```
**Insufficient funds:**
```json
{
  "success": false,
  "errors": ["Insufficient funds. Available: 15.00 USD"]
}
```

### using Front-UI:
В браузере — форма отправляет POST `/user/{login}/cash`, параметры: currency, value, action ("DEPOSIT"/"WITHDRAW"). Front-UI вызывает Cash сервис, результат и ошибки отображаются на /main.
