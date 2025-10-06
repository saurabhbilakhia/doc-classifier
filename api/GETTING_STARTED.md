# Getting Started with Document AI

This guide will help you set up and run the Document AI Classification & Extraction System.

## Prerequisites

1. **JDK 21** - Required for building and running the application
2. **Docker & Docker Compose** - For running PostgreSQL and MailHog
3. **Gradle** - Included via Gradle wrapper (no separate installation needed)

## Step-by-Step Setup

### 1. Install Gradle Wrapper (First Time Only)

If you don't have the Gradle wrapper files yet, run:

```bash
gradle wrapper --gradle-version 8.5
```

### 2. Start Infrastructure Services

Start PostgreSQL and MailHog using Docker Compose:

```bash
docker compose up -d
```

Verify services are running:
```bash
docker compose ps
```

You should see:
- `docai-postgres` running on port 5432
- `docai-mailhog` running on ports 1025 (SMTP) and 8025 (Web UI)

### 3. Build the Application

```bash
./gradlew build
```

On Windows:
```bash
gradlew.bat build
```

This will:
- Download dependencies
- Compile Kotlin code
- Run tests
- Create executable JAR

### 4. Run the Application

```bash
./gradlew bootRun
```

On Windows:
```bash
gradlew.bat bootRun
```

The application will:
- Start on `http://localhost:8080`
- Run Flyway migrations (create database schema)
- Seed the "undefined" classification

### 5. Verify Installation

Open your browser to:
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **MailHog UI**: http://localhost:8025

## First Steps

### Create an Admin User

Since the first user needs to be an admin, you'll need to manually update the database or modify the code.

**Option 1: Modify the database directly**

1. Connect to PostgreSQL:
```bash
docker exec -it docai-postgres psql -U docai -d docai
```

2. Register a user via API first, then update their role:
```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'admin@example.com';
```

**Option 2: Modify AuthService temporarily**

In `AuthService.kt`, temporarily change the default role:
```kotlin
val user = User(
    email = request.email,
    passwordHash = passwordEncoder.encode(request.password),
    role = Role.ADMIN  // Change from USER to ADMIN
)
```

Then register your admin user and change it back.

### Test the API

#### 1. Register a user

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "saurabh.bilakhia@gmail.com",
    "password": "admin123"
  }'
```

#### 2. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "saurabh.bilakhia@gmail.com",
    "password": "admin123"
  }'
```

Save the `accessToken` from the response. You'll use it in subsequent requests.

#### 3. Create a Classification

```bash
TOKEN="your_access_token_here"

curl -X POST http://localhost:8080/api/admin/classifications \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "invoice",
    "description": "Vendor invoices",
    "priority": 10,
    "threshold": 0.4
  }'
```

#### 4. Add Classification Patterns

```bash
curl -X POST http://localhost:8080/api/admin/classifications/2/patterns \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    {"pattern": "(?i)invoice"},
    {"pattern": "(?i)bill to"},
    {"pattern": "(?i)due date"}
  ]'
```

#### 5. Add Data Point Definitions

```bash
curl -X POST http://localhost:8080/api/admin/classifications/2/datapoints \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "key": "invoice_number",
      "label": "Invoice Number",
      "type": "STRING",
      "ruleType": "REGEX",
      "expression": "(?i)invoice\\s*(?:number|#)?\\s*[:\\-]?\\s*([A-Z0-9\\-]+)",
      "required": true
    },
    {
      "key": "total_amount",
      "label": "Total Amount",
      "type": "NUMBER",
      "ruleType": "REGEX",
      "expression": "(?i)total\\s*[:\\-]?\\s*\\$?\\s*([0-9,]+\\.\\d{2})"
    }
  ]'
```

#### 6. Upload a Test Document

Create a simple text file with invoice content:

```bash
echo "INVOICE

Invoice Number: INV-2024-001
Bill To: Acme Corp
Due Date: 2024-12-31
Total: $1,250.00" > test-invoice.txt
```

Upload it:

```bash
curl -X POST http://localhost:8080/api/documents \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@test-invoice.txt"
```

#### 7. View Extracted Data

Replace `{id}` with the document ID from the upload response:

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/documents/{id}/extracted
```

## Common Commands

### Development

```bash
# Build without tests
./gradlew build -x test

# Run tests only
./gradlew test

# Clean build artifacts
./gradlew clean

# Generate dependency report
./gradlew dependencies
```

### Database

```bash
# Access PostgreSQL
docker exec -it docai-postgres psql -U docai -d docai

# View tables
\dt

# Query users
SELECT * FROM users;

# Query classifications
SELECT * FROM classifications;

# Exit
\q
```

### Docker

```bash
# View logs
docker compose logs -f

# Stop services
docker compose down

# Stop and remove volumes (deletes data)
docker compose down -v

# Restart a service
docker compose restart db
```

## Troubleshooting

### Port Already in Use

If port 8080 is already in use, change it in `application.yml`:

```yaml
server:
  port: 8081
```

### Database Connection Issues

1. Ensure PostgreSQL is running:
```bash
docker compose ps
```

2. Check PostgreSQL logs:
```bash
docker compose logs db
```

3. Test connection:
```bash
docker exec -it docai-postgres psql -U docai -d docai -c "SELECT 1;"
```

### Build Failures

1. Ensure you're using JDK 21:
```bash
java -version
```

2. Clean and rebuild:
```bash
./gradlew clean build --refresh-dependencies
```

### Email Not Sending

1. Check MailHog is running:
```bash
docker compose ps mailhog
```

2. View MailHog web UI:
```
http://localhost:8025
```

3. Check application logs for email errors

## Next Steps

1. **Explore the API**: Use Swagger UI to test all endpoints
2. **Create More Classifications**: Add classifications for different document types
3. **Upload Real Documents**: Test with actual PDFs and DOCX files
4. **Review Extracted Data**: Check accuracy of extraction rules
5. **Refine Patterns**: Adjust regex patterns and thresholds based on results

## Additional Resources

- **API Documentation**: http://localhost:8080/swagger-ui/index.html
- **Application Logs**: Check console output
- **Database**: PostgreSQL on localhost:5432
- **Email Testing**: MailHog UI on http://localhost:8025

## Production Considerations

Before deploying to production:

1. **Change JWT Secret**: Update `jwt.secret` in `application.yml`
2. **Use Environment Variables**: Don't commit secrets to git
3. **Configure Real SMTP**: Replace MailHog with actual email service
4. **Set Up S3**: Implement S3 storage for documents
5. **Enable HTTPS**: Configure SSL/TLS
6. **Add Rate Limiting**: Protect against abuse
7. **Set Up Monitoring**: Add logging and metrics
8. **Database Backups**: Configure PostgreSQL backups
9. **Scaling**: Consider async processing for large files
10. **Security Audit**: Review security configurations

Enjoy building with Document AI! 🚀
