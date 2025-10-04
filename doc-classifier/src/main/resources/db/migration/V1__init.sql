-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

-- Password reset tokens
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_password_reset_token ON password_reset_tokens(token);

-- Classifications
CREATE TABLE classifications (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    priority INT NOT NULL DEFAULT 0,
    threshold DOUBLE PRECISION NOT NULL DEFAULT 0.5
);

-- Classification patterns
CREATE TABLE classification_patterns (
    id BIGSERIAL PRIMARY KEY,
    classification_id BIGINT NOT NULL REFERENCES classifications(id) ON DELETE CASCADE,
    pattern TEXT NOT NULL,
    flags VARCHAR(50)
);

CREATE INDEX idx_pattern_classification ON classification_patterns(classification_id);

-- Documents
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    filename TEXT NOT NULL,
    mime_type VARCHAR(200) NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_key TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    classification_id BIGINT REFERENCES classifications(id) ON DELETE SET NULL,
    summary TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_document_owner ON documents(owner_id);
CREATE INDEX idx_document_status ON documents(status);
CREATE INDEX idx_document_classification ON documents(classification_id);

-- Document texts (separate table to avoid loading huge blobs)
CREATE TABLE document_texts (
    document_id BIGINT PRIMARY KEY REFERENCES documents(id) ON DELETE CASCADE,
    text TEXT NOT NULL
);

-- Data point definitions
CREATE TABLE data_point_definitions (
    id BIGSERIAL PRIMARY KEY,
    classification_id BIGINT NOT NULL REFERENCES classifications(id) ON DELETE CASCADE,
    key VARCHAR(100) NOT NULL,
    label VARCHAR(200),
    type VARCHAR(20) NOT NULL,
    rule_type VARCHAR(20) NOT NULL,
    expression TEXT NOT NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX uq_dp_def_per_class_key ON data_point_definitions(classification_id, key);
CREATE INDEX idx_dp_def_classification ON data_point_definitions(classification_id);

-- Extracted data points
CREATE TABLE extracted_data_points (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    classification_id BIGINT NOT NULL REFERENCES classifications(id) ON DELETE CASCADE,
    definition_id BIGINT NOT NULL REFERENCES data_point_definitions(id) ON DELETE CASCADE,
    key VARCHAR(100) NOT NULL,
    value_string TEXT,
    value_number NUMERIC(19, 4),
    value_date DATE,
    confidence DOUBLE PRECISION,
    page INT,
    span_start INT,
    span_end INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_extracted_doc ON extracted_data_points(document_id);
CREATE INDEX idx_extracted_key ON extracted_data_points(key);
CREATE INDEX idx_extracted_classification ON extracted_data_points(classification_id);

-- Insert the reserved "undefined" classification
INSERT INTO classifications (name, description, priority, threshold)
VALUES ('undefined', 'Fallback classification for documents that do not match any defined patterns', -1, 0.0);
