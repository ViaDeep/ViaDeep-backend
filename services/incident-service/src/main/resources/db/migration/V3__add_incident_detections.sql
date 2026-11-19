-- Todas las detecciones de LMD-YOLO en la foto del reporte. accepted indica si superó el umbral de confianza de la
-- app; las que no lo superaron se guardan igual para que el operador vea lo que reconoció el modelo.
-- El tipo y la confianza de la tabla incidents siguen siendo los de la detección principal (la de mayor confianza).
CREATE TABLE incident_detections (
    incident_id      UUID     NOT NULL REFERENCES incidents (id),
    position         INTEGER  NOT NULL,
    incident_type_id SMALLINT NOT NULL REFERENCES incident_types (id),
    confidence       REAL     NOT NULL CHECK (confidence BETWEEN 0 AND 1),
    bbox_x           REAL     NOT NULL CHECK (bbox_x BETWEEN 0 AND 1),
    bbox_y           REAL     NOT NULL CHECK (bbox_y BETWEEN 0 AND 1),
    bbox_width       REAL     NOT NULL CHECK (bbox_width BETWEEN 0 AND 1),
    bbox_height      REAL     NOT NULL CHECK (bbox_height BETWEEN 0 AND 1),
    accepted         BOOLEAN  NOT NULL,
    PRIMARY KEY (incident_id, position)
);

-- Los reportes anteriores tenían una sola detección: la principal.
INSERT INTO incident_detections (incident_id, position, incident_type_id, confidence, bbox_x, bbox_y, bbox_width,
                                 bbox_height, accepted)
SELECT id, 0, incident_type_id, confidence, bbox_x, bbox_y, bbox_width, bbox_height, TRUE
FROM incidents
WHERE ai_validated;
