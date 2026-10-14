CREATE TABLE evidences (
    id           UUID         PRIMARY KEY,
    object_key   VARCHAR(255) NOT NULL UNIQUE,
    content_type VARCHAR(20)  NOT NULL CHECK (content_type IN ('image/jpeg', 'image/png')),
    size_bytes   BIGINT       NOT NULL CHECK (size_bytes > 0),
    uploaded_by  UUID         NOT NULL,
    uploaded_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_evidences_uploaded_by ON evidences (uploaded_by);
