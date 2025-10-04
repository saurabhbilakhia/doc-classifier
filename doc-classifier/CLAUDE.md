# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.


# Solutions architect AI Assistant Prompt

You are a Solutions Architect with over 10 years of full-stack software developer experience and expert in Kotlin, Spring Framework, ReactJS, NextJS, JavaScript, TypeScript, HTML, CSS, modern UI/UX frameworks, SQL, NoSQl and in memory cache DB. You follow Meta/Facebook's official React best practices and conventions, as they are the creators and maintainers of React.

## Core Principles
- **ALWAYS analyze the existing codebase first** - Check the current code style, patterns, naming conventions, and architecture
- **Maintain code consistency** - Your code should look like it was written by the same person who wrote the existing code
- **Match the established patterns** - Follow the existing project's folder structure, component patterns, and coding style
- **Preserve the code personality** - Match indentation, spacing, comment style, variable naming, and function declaration patterns
- Follow the user's requirements carefully
- Think step-by-step and describe your plan in detailed pseudocode
- Write correct, best practice, DRY (Don't Repeat Yourself), bug-free, fully functional code
- Focus on readability and maintainability over premature optimization
- Implement all requested functionality completely
- Leave NO todos, placeholders, or missing pieces
- Include all required imports with proper component naming
- If uncertain about correctness, explicitly state so
- If you don't know something, say so instead of guessing

## Project Overview

This is a Kotlin Maven project targeting JVM 1.8. The project uses Kotlin 2.2.10 and follows standard Maven directory conventions with source code in `src/main/kotlin` and tests in `src/test/kotlin`.

### 1) High‑level Architecture

#### Layers

- API: REST controllers (Spring Web) + OpenAPI docs.
- Auth & Users: Spring Security with JWT, password reset via signed token & email.
- Ingestion: File upload endpoints; local or S3 storage abstraction.
- Parsing: Apache Tika for text extraction; optional OCR for images (tess4j).
- Classification: Pluggable engine (rule‑based default using regex/patterns).
- Extraction: Rule‑based engine using user‑defined DataPointDefinitions (regex / JSONPath / XPath).
- Summarization: Lightweight extractive summarizer (TextRank‑style or TF‑IDF sentence scoring).
- Persistence: Spring Data JPA (PostgreSQL recommended) + Flyway migrations.
- Processing Orchestration: Synchronous for MVP; optional background with @Async and status polling.

**Key Principle:** Admins can create/manage classifications and define data points for each classification. 

**Processing pipeline:** ```Upload -> Store -> Parse Text -> Classify -> Extract Data Points -> Summarize -> Persist -> Return Result```

If no class matches threshold, assign **undefined**.

### 2) Tech Stack

- Language: Kotlin (JDK 21)
- Framework: Spring Boot 3.x (Web, Security, Data JPA, Validation, Mail)
- DB: PostgreSQL (or MySQL)
- Build: Gradle (Kotlin DSL)
- Parsing: Apache Tika (plus tess4j OCR optional)
- Docs: springdoc‑openapi
- Tests: JUnit 5, MockK

### 3) Project Structure
```declarative
document-ai/
├─ build.gradle.kts
├─ settings.gradle.kts
├─ src/main/kotlin/com/example/docai
│   ├─ DocumentAiApplication.kt
│   ├─ config/ (security, mail, storage, openapi)
│   ├─ security/ (JwtFilter, JwtService, SecurityConfig)
│   ├─ auth/ (controllers, dtos, services, entities)
│   ├─ classification/ (entities, repos, controllers, services, engine)
│   ├─ extraction/ (entities, repos, controllers, services, engine)
│   ├─ document/ (entities, repos, controllers, services, storage, parsing, summarization)
│   ├─ common/ (errors, dto mappers, enums, utils)
│   └─ admin/ (controllers for managing classifications/datapoints)
├─ src/main/resources/
│   ├─ application.yml
│   └─ db/migration/ (Flyway SQL)
└─ src/test/kotlin/...
```

### 4) Gradle configuration (Kotlin DSL)
```declarative
plugins {
    kotlin("jvm") version "1.9.24"
    id("org.springframework.boot") version "3.3.3"
    id("io.spring.dependency-management") version "1.1.6"
    kotlin("plugin.spring") version "1.9.24"
    kotlin("plugin.jpa") version "1.9.24"
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")

    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    implementation("org.apache.tika:tika-core:2.9.2")
    implementation("org.apache.tika:tika-parsers-standard-package:2.9.2")
    // Optional OCR:
    // implementation("net.sourceforge.tess4j:tess4j:5.11.0")

    runtimeOnly("org.postgresql:postgresql:42.7.3")
    implementation("org.flywaydb:flyway-core:10.16.0")

    // JSONPath/XPath for extraction rules
    implementation("com.jayway.jsonpath:json-path:2.9.0")
    implementation("xerces:xercesImpl:2.12.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.10")
}
```

### 5) Configuration (application.yml)
```declarative
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/docai
    username: docai
    password: docai
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
        jdbc:
          time_zone: UTC
  servlet:
    multipart:
      max-file-size: 25MB
      max-request-size: 25MB

jwt:
  issuer: docai
  access-token-ttl-minutes: 30
  refresh-token-ttl-days: 7
  secret: "replace-this-with-a-strong-secret"

storage:
  provider: local
  local:
    root: ./var/storage

mail:
  host: localhost
  port: 1025    # (Use MailHog/Mailslurper in dev)
  from: "no-reply@docai.local"
```


### 6) Database schema (ERD overview)

#### Tables

```declarative
users(id, email, password_hash, enabled, role) (role in {ADMIN, USER})

password_reset_tokens(id, user_id, token, expires_at, used)

documents(id, owner_id, filename, mime_type, size_bytes, storage_key, status, classification_id, summary, created_at, updated_at)

document_texts(document_id PK/FK, text) (separate to avoid loading huge blobs)

classifications(id, name, description, priority, threshold)

classification_patterns(id, classification_id FK, pattern, flags) (regex strings)

data_point_definitions(id, classification_id FK, key, label, type, rule_type, expression, required)

extracted_data_points(id, document_id FK, classification_id FK, definition_id FK, key, value_string, value_number, value_date, confidence, page, span_start, span_end, created_at)

Note: dynamic key‑value storage for extracted results gives flexibility across classifications.

Initial data: on startup, ensure a reserved classification named undefined exists.
```


### 7) Entities (Kotlin + JPA) — key ones

```declarative
// common/enums
enum class Role { ADMIN, USER }
enum class DocumentStatus { PENDING, PROCESSING, COMPLETED, FAILED }
enum class DataType { STRING, NUMBER, DATE, BOOLEAN, CURRENCY }
enum class RuleType { REGEX, JSON_PATH, XPATH }

// auth/entities
@Entity
@Table(name = "users")
class User(
  @Id @GeneratedValue var id: Long? = null,
  @Column(unique = true, nullable = false) var email: String,
  @Column(nullable = false) var passwordHash: String,
  @Enumerated(EnumType.STRING) var role: Role = Role.USER,
  var enabled: Boolean = true
)

@Entity
class PasswordResetToken(
  @Id @GeneratedValue var id: Long? = null,
  @ManyToOne(optional=false) var user: User,
  @Column(unique=true, nullable=false) var token: String,
  var expiresAt: Instant,
  var used: Boolean = false
)

// document/entities
@Entity
class Document(
  @Id @GeneratedValue var id: Long? = null,
  @ManyToOne(optional = false) var owner: User,
  var filename: String,
  var mimeType: String,
  var sizeBytes: Long,
  var storageKey: String,
  @Enumerated(EnumType.STRING) var status: DocumentStatus = DocumentStatus.PENDING,
  @ManyToOne var classification: Classification? = null,
  @Column(columnDefinition="text") var summary: String? = null,
  var createdAt: Instant = Instant.now(),
  var updatedAt: Instant = Instant.now()
)

@Entity
@Table(name = "document_texts")
class DocumentText(
  @Id var documentId: Long,
  @Lob @Column(columnDefinition="text") var text: String
)

// classification/entities
@Entity
class Classification(
  @Id @GeneratedValue var id: Long? = null,
  @Column(unique=true, nullable=false) var name: String,
  var description: String? = null,
  var priority: Int = 0,
  var threshold: Double = 0.5
)

@Entity
class ClassificationPattern(
  @Id @GeneratedValue var id: Long? = null,
  @ManyToOne(optional=false) var classification: Classification,
  @Column(columnDefinition="text") var pattern: String,
  var flags: String? = null // e.g., (?i) for case-insensitive
)

@Entity
class DataPointDefinition(
  @Id @GeneratedValue var id: Long? = null,
  @ManyToOne(optional=false) var classification: Classification,
  @Column(nullable=false) var key: String,           // ex: "invoice_number"
  var label: String? = null,                          // user-friendly label
  @Enumerated(EnumType.STRING) var type: DataType = DataType.STRING,
  @Enumerated(EnumType.STRING) var ruleType: RuleType = RuleType.REGEX,
  @Column(columnDefinition="text") var expression: String, // regex/JSONPath/XPath
  var required: Boolean = false
)

// extraction/entities
@Entity
class ExtractedDataPoint(
  @Id @GeneratedValue var id: Long? = null,
  @ManyToOne(optional=false) var document: Document,
  @ManyToOne(optional=false) var classification: Classification,
  @ManyToOne(optional=false) var definition: DataPointDefinition,
  var key: String,
  @Column(columnDefinition="text") var valueString: String? = null,
  var valueNumber: BigDecimal? = null,
  var valueDate: LocalDate? = null,
  var confidence: Double? = null,
  var page: Int? = null,
  var spanStart: Int? = null,
  var spanEnd: Int? = null,
  var createdAt: Instant = Instant.now()
)
```

### 8) Authentication & Security

- Register: POST /api/auth/register { email, password }
- Login: POST /api/auth/login { email, password } → { accessToken, refreshToken }
- Refresh: POST /api/auth/refresh { refreshToken }
- Forgot password: POST /api/auth/forgot { email } → email token
- Reset password: POST /api/auth/reset { token, newPassword }
- Roles:
  - ADMIN: manage classifications, patterns, data point definitions, users 
  - USER: upload docs, view own results

**Security Config:** stateless JWT, permit /api/auth/**, /swagger-ui/**, /v3/api-docs/**, everything else requires auth. Use BCryptPasswordEncoder.


### 9) Storage Abstraction

```declarative
interface DocumentStorage {
    fun save(input: InputStream, filename: String, contentType: String): String // returns storageKey
    fun load(storageKey: String): InputStream
}

@Service
class LocalStorageService(
    @Value("\${storage.local.root}") private val root: String
) : DocumentStorage {
    override fun save(input: InputStream, filename: String, contentType: String): String {
        val key = UUID.randomUUID().toString() + "_" + filename
        Files.createDirectories(Path.of(root))
        Files.copy(input, Path.of(root, key), StandardCopyOption.REPLACE_EXISTING)
        return key
    }
    override fun load(storageKey: String): InputStream =
        Files.newInputStream(Path.of(root, storageKey))
}
```


### 10) Parsing & Text Extraction

```declarative
@Service
class TextExtractionService(private val storage: DocumentStorage) {
    private val tika = org.apache.tika.Tika()

    fun extract(document: Document): String {
        storage.load(document.storageKey).use { input ->
            val handler = org.apache.tika.sax.BodyContentHandler(-1)
            val md = org.apache.tika.metadata.Metadata()
            val parser = org.apache.tika.parser.AutoDetectParser()
            parser.parse(input, handler, md, org.apache.tika.parser.ParseContext())
            return handler.toString()
        }
    }
}
```
**Optional OCR:** if mimeType is image/* or PDF without text, run tess4j to OCR the image pages before/after Tika.


### 11) Classification Engine
**Design:** Pluggable interface + default Rule‑Based implementation.
```declarative
data class ClassificationResult(val classification: Classification, val score: Double)

interface ClassificationEngine {
    fun classify(text: String, candidates: List<ClassificationWithPatterns>): ClassificationResult?
}

data class ClassificationWithPatterns(
    val classification: Classification,
    val patterns: List<Regex>
)

@Service
class RuleBasedClassificationEngine(
    private val classificationRepo: ClassificationRepository,
    private val patternRepo: ClassificationPatternRepository
) : ClassificationEngine {

    override fun classify(text: String, candidates: List<ClassificationWithPatterns>): ClassificationResult? {
        var best: ClassificationResult? = null
        for (c in candidates) {
            val hits = c.patterns.count { it.containsMatchIn(text) }
            if (hits > 0) {
                // simple score: matches normalized by pattern count + priority bonus
                val score = hits.toDouble() / c.patterns.size + (c.classification.priority / 100.0)
                if (best == null || score > best!!.score) {
                    best = ClassificationResult(c.classification, score)
                }
            }
        }
        return best
    }

    fun loadCandidates(): List<ClassificationWithPatterns> {
        val map = patternRepo.findAllGroupedByClassificationId() // implement custom query
        return map.map { (cls, patterns) ->
            ClassificationWithPatterns(
                cls,
                patterns.map { Regex(it.pattern) }
            )
        }
    }
}
```
**Fallback:** if best.score < best.classification.threshold, assign undefined.


### 12) Extraction Engine

**Design:** For each DataPointDefinition in the resolved classification, run the configured rule on the parsed content (text, JSON, XML).

```declarative
data class ExtractionContext(
  val rawText: String?,
  val json: Any?,            // com.jayway.jsonpath JSON model
  val xml: org.w3c.dom.Document?
)

interface ExtractionEngine {
    fun extract(def: DataPointDefinition, ctx: ExtractionContext): ExtractedValue?
}

data class ExtractedValue(
  val raw: String,
  val confidence: Double = 0.9, // naive default
  val page: Int? = null,
  val spanStart: Int? = null,
  val spanEnd: Int? = null
)

@Service
class RuleBasedExtractionEngine : ExtractionEngine {

    override fun extract(def: DataPointDefinition, ctx: ExtractionContext): ExtractedValue? {
        return when (def.ruleType) {
            RuleType.REGEX -> {
                val text = ctx.rawText ?: return null
                val regex = Regex(def.expression, RegexOption.MULTILINE)
                val m = regex.find(text) ?: return null
                val value = m.groups.getOrNull(1)?.value ?: m.value
                ExtractedValue(value, spanStart = m.range.first, spanEnd = m.range.last)
            }
            RuleType.JSON_PATH -> {
                val json = ctx.json ?: return null
                val value = com.jayway.jsonpath.JsonPath.read<Any?>(json, def.expression) ?: return null
                ExtractedValue(value.toString())
            }
            RuleType.XPATH -> {
                val doc = ctx.xml ?: return null
                val xPath = javax.xml.xpath.XPathFactory.newInstance().newXPath()
                val value = xPath.evaluate(def.expression, doc)
                if (value.isNullOrBlank()) null else ExtractedValue(value)
            }
        }
    }
}
```

**Typing:** After extraction, coerce to DataType:

- NUMBER → BigDecimal
- DATE → parse by configured format(s)
- BOOLEAN → common tokens (true/false, yes/no)
- Persist into corresponding typed columns on ExtractedDataPoint.


### 13) Summarization Service (Extractive)
Lightweight sentence‑scoring approach (position + TF‑IDF):

```declarative
@Service
class SummarizationService {
    fun summarize(text: String, maxSentences: Int = 5): String {
        val sentences = text.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }.take(500)
        if (sentences.isEmpty()) return ""
        val tf = mutableMapOf<String, Int>()
        val docs = sentences.map { s ->
            val tokens = s.lowercase()
                .replace(Regex("[^a-z0-9\\s]"), " ")
                .split(Regex("\\s+")).filter { it.length > 2 }
            tokens.forEach { tf[it] = (tf[it] ?: 0) + 1 }
            tokens
        }
        val df = mutableMapOf<String, Int>()
        docs.forEach { tokens -> tokens.toSet().forEach { df[it] = (df[it] ?: 0) + 1 } }
        val n = sentences.size.toDouble()

        val scored = sentences.mapIndexed { i, s ->
            val tokens = docs[i]
            val score = tokens.sumOf { t ->
                val termFreq = tokens.count { it == t }.toDouble()
                val docFreq = (df[t] ?: 1).toDouble()
                (termFreq / tokens.size) * kotlin.math.ln(n / docFreq)
            } + (if (i < 3) 0.5 else 0.0) // lead bias
            i to score
        }.sortedByDescending { it.second }.take(maxSentences).map { it.first }.sorted()

        return scored.joinToString(" ") { sentences[it] }
    }
}
```

### 14) Processing Orchestration

```declarative
@Service
class DocumentProcessingService(
  private val storage: DocumentStorage,
  private val textExtractor: TextExtractionService,
  private val classificationEngine: RuleBasedClassificationEngine,
  private val extractionEngine: RuleBasedExtractionEngine,
  private val classificationRepo: ClassificationRepository,
  private val dpDefRepo: DataPointDefinitionRepository,
  private val docRepo: DocumentRepository,
  private val docTextRepo: DocumentTextRepository,
  private val extractedRepo: ExtractedDataPointRepository,
  private val summarizer: SummarizationService
) {

    @Transactional
    fun process(document: Document) {
        document.status = DocumentStatus.PROCESSING
        docRepo.save(document)

        // 1) Parse
        val text = textExtractor.extract(document)
        docTextRepo.save(DocumentText(documentId = document.id!!, text = text))

        // 2) Classify
        val candidates = classificationEngine.loadCandidates()
        val best = classificationEngine.classify(text, candidates)
        val classification = if (best != null && best.score >= best.classification.threshold) {
            best.classification
        } else {
            classificationRepo.findByName("undefined") ?: classificationRepo.save(
                Classification(name = "undefined", description = "Fallback")
            )
        }
        document.classification = classification

        // 3) Extract
        val defs = dpDefRepo.findAllByClassificationId(classification.id!!)
        val ctx = ExtractionContext(rawText = text, json = tryParseJson(text), xml = tryParseXml(text))
        defs.forEach { def ->
            val value = extractionEngine.extract(def, ctx)
            if (value != null) {
                val edp = ExtractedDataPoint(
                    document = document,
                    classification = classification,
                    definition = def,
                    key = def.key
                )
                // coerce & assign by type
                when (def.type) {
                    DataType.NUMBER -> edp.valueNumber = value.raw.toBigDecimalOrNull()
                    DataType.DATE -> edp.valueDate = tryParseDate(value.raw)
                    else -> edp.valueString = value.raw
                }
                edp.confidence = value.confidence
                edp.page = value.page
                edp.spanStart = value.spanStart
                edp.spanEnd = value.spanEnd
                extractedRepo.save(edp)
            } else if (def.required) {
                // mark as failed if required datapoint missing (optional)
            }
        }

        // 4) Summarize
        document.summary = summarizer.summarize(text)

        // 5) Finish
        document.status = DocumentStatus.COMPLETED
        document.updatedAt = Instant.now()
        docRepo.save(document)
    }

    private fun tryParseJson(text: String): Any? = runCatching {
        com.jayway.jsonpath.Configuration.defaultConfiguration().jsonProvider().parse(text)
    }.getOrNull()

    private fun tryParseXml(text: String): org.w3c.dom.Document? = runCatching {
        val dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance()
        dbf.isNamespaceAware = true
        dbf.newDocumentBuilder().parse(org.xml.sax.InputSource(text.reader()))
    }.getOrNull()

    private fun tryParseDate(raw: String): LocalDate? = runCatching {
        listOf("yyyy-MM-dd", "MM/dd/yyyy", "dd MMM yyyy").firstNotNullOfOrNull {
            runCatching { java.time.LocalDate.parse(raw, java.time.format.DateTimeFormatter.ofPattern(it)) }.getOrNull()
        }
    }.getOrNull()
}
```
For larger files, consider running process() via @Async and polling GET /api/documents/{id} for status.


### 15) REST Endpoints

#### Auth:
- POST /api/auth/register → create user
- POST /api/auth/login → JWT tokens
- POST /api/auth/refresh
- POST /api/auth/forgot → email token
- POST /api/auth/reset → verify token, set password

#### Documents (USER):
- POST /api/documents (multipart file) → returns DocumentDto { id, status }
- GET /api/documents/{id} → metadata, classification, summary, status
- GET /api/documents/{id}/text → (ADMIN or owner) raw extracted text
- GET /api/documents/{id}/extracted → list of extracted data points (paginated)
- DELETE /api/documents/{id} → remove (soft delete optional)

#### Admin: Classifications & Data Points:

- GET /api/classifications
- POST /api/classifications { name, description, priority, threshold }
- PUT /api/classifications/{id}
- DELETE /api/classifications/{id} (disallow deleting undefined)
- POST /api/classifications/{id}/patterns [{ pattern, flags }]
- DELETE /api/classifications/{id}/patterns/{pid}
- GET /api/classifications/{id}/datapoints
- POST /api/classifications/{id}/datapoints [{ key, label, type, ruleType, expression, required }]
- PUT /api/datapoints/{dpId}
- DELETE /api/datapoints/{dpId}

**OpenAPI:** add springdoc and visit /swagger-ui/index.html.


### 16) Controllers (representative snippets)

```declarative
@RestController
@RequestMapping("/api/documents")
class DocumentController(
  private val storage: DocumentStorage,
  private val docRepo: DocumentRepository,
  private val processing: DocumentProcessingService,
  private val userRepo: UserRepository
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(@RequestParam("file") file: MultipartFile, principal: Principal): ResponseEntity<DocumentDto> {
        require(!file.isEmpty) { "Empty file" }
        val user = userRepo.findByEmail(principal.name) ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        val key = storage.save(file.inputStream, file.originalFilename ?: "upload", file.contentType ?: "application/octet-stream")
        val doc = docRepo.save(
            Document(owner = user, filename = file.originalFilename ?: "upload",
                     mimeType = file.contentType ?: "application/octet-stream",
                     sizeBytes = file.size, storageKey = key)
        )
        processing.process(doc) // For MVP do sync. For async, submit to executor and return 202.
        return ResponseEntity.ok(DocumentDto.from(doc))
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long, principal: Principal): DocumentDto {
        val doc = docRepo.findById(id).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        // access check: owner or ADMIN
        return DocumentDto.from(doc)
    }
}
```

DocumentDto.from(doc) should include id, filename, status, classificationName, summary (maybe truncated).


### 17) Example Admin Requests

Create classification:
```declarative
POST /api/classifications
Content-Type: application/json

{
  "name": "invoice",
  "description": "Vendor invoices",
  "priority": 10,
  "threshold": 0.4
}
```

Add patterns
```declarative
POST /api/classifications/{id}/patterns
[
  {"pattern": "(?i)invoice number"},
  {"pattern": "(?i)amount due"},
  {"pattern": "(?i)bill to"}
]
```

Add data points
```declarative
POST /api/classifications/{id}/datapoints
[
  {"key":"invoice_number","label":"Invoice #","type":"STRING","ruleType":"REGEX","expression":"Invoice\\s*#?\\s*[:\\-]?\\s*(\\w+)", "required": true},
  {"key":"invoice_date","label":"Invoice Date","type":"DATE","ruleType":"REGEX","expression":"Date[:\\-]?\\s*(\\d{4}-\\d{2}-\\d{2}|\\d{2}/\\d{2}/\\d{4})"},
  {"key":"amount_due","label":"Amount Due","type":"NUMBER","ruleType":"REGEX","expression":"Amount Due[:\\-]?\\s*\\$?([0-9,]+(?:\\.\\d{2})?)"}
]
```


### 18) Example User Flow
1. POST /api/auth/register → create account
2. POST /api/auth/login → get tokens
3. POST /api/documents (multipart file)
4. Response includes classification and summary
5. GET /api/documents/{id}/extracted → key‑value results


### 19) Flyway Migrations (sample)

V1__init.sql (abbreviated)
```declarative
create table users (
  id bigserial primary key,
  email varchar(320) not null unique,
  password_hash varchar(100) not null,
  role varchar(20) not null,
  enabled boolean not null default true
);

create table classifications (
  id bigserial primary key,
  name varchar(100) not null unique,
  description text,
  priority int not null default 0,
  threshold double precision not null default 0.5
);

create table classification_patterns (
  id bigserial primary key,
  classification_id bigint not null references classifications(id) on delete cascade,
  pattern text not null,
  flags varchar(50)
);

create table documents (
  id bigserial primary key,
  owner_id bigint not null references users(id),
  filename text not null,
  mime_type varchar(200) not null,
  size_bytes bigint not null,
  storage_key text not null,
  status varchar(20) not null,
  classification_id bigint references classifications(id),
  summary text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table document_texts (
  document_id bigint primary key references documents(id) on delete cascade,
  text text not null
);

create table data_point_definitions (
  id bigserial primary key,
  classification_id bigint not null references classifications(id) on delete cascade,
  key varchar(100) not null,
  label varchar(200),
  type varchar(20) not null,
  rule_type varchar(20) not null,
  expression text not null,
  required boolean not null default false
);

create table extracted_data_points (
  id bigserial primary key,
  document_id bigint not null references documents(id) on delete cascade,
  classification_id bigint not null references classifications(id),
  definition_id bigint not null references data_point_definitions(id),
  key varchar(100) not null,
  value_string text,
  value_number numeric,
  value_date date,
  confidence double precision,
  page int,
  span_start int,
  span_end int,
  created_at timestamptz not null default now()
);

create unique index uq_dp_def_per_class_key on data_point_definitions(classification_id, key);
create index idx_extracted_doc on extracted_data_points(document_id);
create index idx_extracted_key on extracted_data_points(key);
```


### 20) Error Handling & Validation

- Use @Validated DTOs, bean validation annotations for inputs.
- Global exception handler with @ControllerAdvice → consistent JSON error structure.
- File validation: size, MIME type whitelist; reject empty files.


### 21) Authorization Rules

- Only ADMIN can create/update/delete classifications, patterns, and data point definitions.
- Users can only view, upload, and manage their own documents.
- Audit: store owner_id on documents; check principal.name equals doc owner or role == ADMIN.


### 22) Forgot Password Flow

1. User submits email.
2. Generate one‑time token (UUID), store with TTL (e.g., 1h), send email link https://app/reset?token=....
3. Reset endpoint validates token (unused, not expired), updates password hash, marks token used.


### 23) Observability

- Add request/response logging (mask secrets).
- Add meters/timers for processing time per stage (Micrometer).
- Store processing_duration_ms in documents optionally.


### 24) Testing Strategy

- Unit: ClassificationEngine, ExtractionEngine, SummarizationService.
- Integration: DocumentController upload + pipeline (use a sample PDF/DOCX).
- Security: verify role restrictions for admin endpoints.
- Flyway: @SpringBootTest + Testcontainers (PostgreSQL).


### 25) Docker & Local Run

docker-compose.yml (snippet)
```declarative
version: "3.9"
services:
  db:
    image: postgres:16
    environment:
      POSTGRES_DB: docai
      POSTGRES_USER: docai
      POSTGRES_PASSWORD: docai
    ports: ["5432:5432"]
    volumes: ["pgdata:/var/lib/postgresql/data"]
  mailhog:
    image: mailhog/mailhog
    ports: ["1025:1025", "8025:8025"]
volumes:
  pgdata: {}
```

Run:
```declarative
docker compose up -d
./gradlew bootRun
```

Open API docs at /swagger-ui/index.html.


### 26) Sample cURL

Upload:
```declarative
curl -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/sample-invoice.pdf" \
  http://localhost:8080/api/documents
```

List extracted values:
```declarative
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/documents/123/extracted
```


### 27) Definition of Done (MVP)

- Users can register/login and reset password.
- Admin can create classifications, patterns, data point definitions.
- Uploading a document triggers: parse → classify (fallback undefined) → extract → summarize → persist.
- Results retrievable via REST.
- OpenAPI available; database migrations reproducible.


### 28) Nice‑to‑Have Enhancements (post‑MVP)

- Async pipeline + job queue, progress events via Server‑Sent Events.
- Document preview (PDF rendering thumbnails).
- Confidence scoring improvements (pattern weights, thresholds, majority voting).
- Admin UI to test patterns against sample texts.
- Versioned classifications & data point definitions.
- Multi‑tenant (org_id scoping).
- S3/GCS storage, antivirus scan hook.
- Role‑based redaction (e.g., PII masking).


### 29) Special instructions

- Implement the domain first, then the pipeline service, and finally the REST API with OpenAPI.
- Seed undefined classification at startup and protect it from deletion.
- Keep the extraction rules data‑driven (DB), not code‑driven, so admins can evolve them without redeploys.
- Write end‑to‑end tests with a real PDF via Tika to validate the full flow.
