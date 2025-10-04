# Document AI Classification & Extraction System

A comprehensive document processing system built with Kotlin and Spring Boot that automatically classifies documents and extracts structured data from them.

## Features

- **Document Upload & Storage**: Upload documents (PDF, DOCX, etc.) with local or S3 storage options
- **Text Extraction**: Apache Tika-based text extraction supporting multiple formats
- **Automatic Classification**: Rule-based classification using regex patterns with configurable thresholds
- **Data Extraction**: Extract structured data points using regex, JSONPath, or XPath rules
- **Summarization**: Extractive summarization using TF-IDF sentence scoring
- **User Authentication**: JWT-based authentication with role-based access control (ADMIN/USER)
- **Password Reset**: Email-based password reset flow
- **REST API**: Complete REST API with OpenAPI/Swagger documentation
- **Admin Interface**: Manage classifications, patterns, and data point definitions

## Tech Stack

- **Language**: Kotlin 1.9.24
- **Framework**: Spring Boot 3.3.3
- **Build Tool**: Gradle (Kotlin DSL)
- **JDK**: 21
- **Database**: PostgreSQL 16
- **Migrations**: Flyway
- **Document Parsing**: Apache Tika 2.9.2
- **Security**: Spring Security + JWT
- **Documentation**: SpringDoc OpenAPI
- **Testing**: JUnit 5, MockK

## Quick Start

### Prerequisites

- JDK 21
- Docker & Docker Compose

### 1. Start Infrastructure

```bash
docker compose up -d
```

This starts:
- PostgreSQL database (port 5432)
- MailHog for email testing (SMTP: 1025, Web UI: 8025)

### 2. Build the Project

```bash
./gradlew build
```

### 3. Run the Application

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

### 4. Access API Documentation

Open your browser to:
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### 5. View Emails (MailHog)

Open: `http://localhost:8025` to see password reset emails

## Build Commands

```bash
# Compile
./gradlew compileKotlin

# Run tests
./gradlew test

# Run a single test
./gradlew test --tests "TestClassName"

# Clean build
./gradlew clean build

# Run application
./gradlew bootRun
```

## API Overview

### Authentication Endpoints

- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login and get JWT tokens
- `POST /api/auth/refresh` - Refresh access token
- `POST /api/auth/forgot` - Request password reset
- `POST /api/auth/reset` - Reset password with token

### Document Endpoints (Authenticated)

- `POST /api/documents` - Upload document (multipart/form-data)
- `GET /api/documents` - List user's documents (paginated)
- `GET /api/documents/{id}` - Get document details
- `GET /api/documents/{id}/text` - Get extracted text (ADMIN or owner)
- `GET /api/documents/{id}/extracted` - Get extracted data points (paginated)
- `DELETE /api/documents/{id}` - Delete document

### Admin Endpoints (ADMIN role required)

- `GET /api/admin/classifications` - List all classifications
- `POST /api/admin/classifications` - Create classification
- `PUT /api/admin/classifications/{id}` - Update classification
- `DELETE /api/admin/classifications/{id}` - Delete classification
- `POST /api/admin/classifications/{id}/patterns` - Add patterns
- `DELETE /api/admin/classifications/{id}/patterns/{pid}` - Delete pattern
- `POST /api/admin/classifications/{id}/datapoints` - Add data point definitions
- `PUT /api/admin/datapoints/{dpId}` - Update data point definition
- `DELETE /api/admin/datapoints/{dpId}` - Delete data point definition

## Example Usage

### 1. Register a User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'
```

### 2. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'
```

Save the `accessToken` from the response.

### 3. Create a Classification (as ADMIN)

First, manually create an admin user in the database or modify the register endpoint to create admin.

```bash
curl -X POST http://localhost:8080/api/admin/classifications \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name":"invoice",
    "description":"Vendor invoices",
    "priority":10,
    "threshold":0.4
  }'
```

### 4. Add Patterns

```bash
curl -X POST http://localhost:8080/api/admin/classifications/1/patterns \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    {"pattern":"(?i)invoice number"},
    {"pattern":"(?i)amount due"},
    {"pattern":"(?i)bill to"}
  ]'
```

### 5. Add Data Point Definitions

```bash
curl -X POST http://localhost:8080/api/admin/classifications/1/datapoints \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "key":"invoice_number",
      "label":"Invoice #",
      "type":"STRING",
      "ruleType":"REGEX",
      "expression":"Invoice\\s*#?\\s*[:\\-]?\\s*(\\w+)",
      "required":true
    },
    {
      "key":"amount_due",
      "label":"Amount Due",
      "type":"NUMBER",
      "ruleType":"REGEX",
      "expression":"Amount Due[:\\-]?\\s*\\$?([0-9,]+(?:\\.\\d{2})?)"
    }
  ]'
```

### 6. Upload a Document

```bash
curl -X POST http://localhost:8080/api/documents \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -F "file=@/path/to/invoice.pdf"
```

### 7. Get Extracted Data

```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  http://localhost:8080/api/documents/1/extracted
```

## Project Structure

```
src/main/kotlin/com/example/docai/
├── DocumentAiApplication.kt
├── config/                    # Configuration classes
│   ├── SecurityConfig.kt
│   ├── OpenApiConfig.kt
│   └── MailConfig.kt
├── security/                  # JWT authentication
│   ├── JwtService.kt
│   └── JwtAuthenticationFilter.kt
├── auth/                      # User authentication
│   ├── entities/
│   ├── repositories/
│   ├── service/
│   ├── controller/
│   └── dto/
├── classification/            # Document classification
│   ├── entities/
│   ├── repositories/
│   └── engine/
├── extraction/                # Data extraction
│   ├── entities/
│   ├── repositories/
│   └── engine/
├── document/                  # Document management
│   ├── entities/
│   ├── repositories/
│   ├── controller/
│   ├── service/
│   ├── dto/
│   ├── storage/              # Storage abstraction
│   ├── parsing/              # Text extraction
│   └── summarization/        # Summarization
├── admin/                     # Admin endpoints
│   ├── controller/
│   └── dto/
└── common/                    # Common utilities
    ├── enums/
    └── error/

src/main/resources/
├── application.yml
└── db/migration/
    └── V1__init.sql
```

## Configuration

Key configuration properties in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/docai
  servlet:
    multipart:
      max-file-size: 25MB

jwt:
  secret: "change-this-in-production"
  access-token-ttl-minutes: 30
  refresh-token-ttl-days: 7

storage:
  provider: local
  local:
    root: ./var/storage
```

## Database Schema

The system uses PostgreSQL with Flyway migrations. Key tables:

- `users` - User accounts with roles
- `classifications` - Document classification types
- `classification_patterns` - Regex patterns for classification
- `documents` - Uploaded documents metadata
- `document_texts` - Extracted text (separate for performance)
- `data_point_definitions` - Rules for extracting data points
- `extracted_data_points` - Extracted structured data
- `password_reset_tokens` - Password reset tokens

## Architecture

### Processing Pipeline

```
Upload → Store → Parse Text → Classify → Extract Data Points → Summarize → Persist → Return Result
```

1. **Upload**: Document stored via storage abstraction
2. **Parse**: Apache Tika extracts text
3. **Classify**: Rule-based engine matches patterns, fallback to "undefined"
4. **Extract**: Apply data point rules (REGEX/JSONPath/XPath)
5. **Summarize**: TF-IDF extractive summarization
6. **Persist**: Save results to database

### Security

- Stateless JWT authentication
- Role-based access control (ADMIN, USER)
- BCrypt password hashing
- Users can only access their own documents
- Admins can manage classifications and view all documents

## Development

### Adding a New Classification

1. Create classification via admin endpoint
2. Add regex patterns that identify this document type
3. Define data points to extract with rules
4. Upload test documents to verify

### Extending Storage

Implement the `DocumentStorage` interface to add S3 or other storage backends.

### Adding OCR

Uncomment tess4j dependency in `build.gradle.kts` and enhance `TextExtractionService`.

## License

This is an example/demo project for educational purposes.
