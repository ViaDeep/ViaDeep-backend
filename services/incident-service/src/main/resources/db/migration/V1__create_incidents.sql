-- Catálogo de tipos: corresponde a las clases que detecta LMD-YOLO.
CREATE TABLE incident_types (
    id   SMALLINT     PRIMARY KEY,
    code VARCHAR(3)   NOT NULL UNIQUE,
    name VARCHAR(60)  NOT NULL UNIQUE
);

INSERT INTO incident_types (id, code, name) VALUES
    (1, 'D00', 'Grieta longitudinal'),
    (2, 'D10', 'Grieta transversal'),
    (3, 'D20', 'Piel de cocodrilo'),
    (4, 'D40', 'Bache');

CREATE TABLE incidents (
    id               UUID         PRIMARY KEY,
    reporter_id      UUID         NOT NULL,
    evidence_id      UUID         NOT NULL UNIQUE,
    incident_type_id SMALLINT     NOT NULL REFERENCES incident_types (id),
    location         GEOGRAPHY(POINT, 4326) NOT NULL,
    district         VARCHAR(80),
    description      VARCHAR(500),
    status           VARCHAR(20)  NOT NULL CHECK (status IN ('PENDING', 'IN_PROGRESS', 'RESOLVED', 'REJECTED')),
    severity         VARCHAR(10)  CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    ai_validated     BOOLEAN      NOT NULL,
    confidence       REAL         CHECK (confidence BETWEEN 0 AND 1),
    bbox_x           REAL         CHECK (bbox_x BETWEEN 0 AND 1),
    bbox_y           REAL         CHECK (bbox_y BETWEEN 0 AND 1),
    bbox_width       REAL         CHECK (bbox_width BETWEEN 0 AND 1),
    bbox_height      REAL         CHECK (bbox_height BETWEEN 0 AND 1),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    -- Un reporte validado por la IA trae la confianza y el bounding box; uno manual no trae ninguno.
    CHECK ((ai_validated AND confidence IS NOT NULL AND bbox_x IS NOT NULL AND bbox_y IS NOT NULL
                AND bbox_width IS NOT NULL AND bbox_height IS NOT NULL)
        OR (NOT ai_validated AND confidence IS NULL AND bbox_x IS NULL AND bbox_y IS NULL
                AND bbox_width IS NULL AND bbox_height IS NULL))
);

CREATE INDEX idx_incidents_location ON incidents USING GIST (location);
CREATE INDEX idx_incidents_status ON incidents (status);
CREATE INDEX idx_incidents_reporter_id ON incidents (reporter_id);
CREATE INDEX idx_incidents_created_at ON incidents (created_at);

CREATE TABLE status_histories (
    id          UUID         PRIMARY KEY,
    incident_id UUID         NOT NULL REFERENCES incidents (id),
    from_status VARCHAR(20)  CHECK (from_status IN ('PENDING', 'IN_PROGRESS', 'RESOLVED', 'REJECTED')),
    to_status   VARCHAR(20)  NOT NULL CHECK (to_status IN ('PENDING', 'IN_PROGRESS', 'RESOLVED', 'REJECTED')),
    changed_by  UUID         NOT NULL,
    comment     VARCHAR(500),
    changed_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_status_histories_incident_id ON status_histories (incident_id);
CREATE INDEX idx_status_histories_changed_at ON status_histories (changed_at);
