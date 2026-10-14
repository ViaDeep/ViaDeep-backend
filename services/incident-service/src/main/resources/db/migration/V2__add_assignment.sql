-- Responsable (persona y área) que el operador asigna para atender la incidencia.
ALTER TABLE incidents ADD COLUMN assignee        VARCHAR(120);
ALTER TABLE incidents ADD COLUMN assigned_area   VARCHAR(120);
ALTER TABLE incidents ADD COLUMN assignment_note VARCHAR(500);
ALTER TABLE incidents ADD COLUMN assigned_by     UUID;
ALTER TABLE incidents ADD COLUMN assigned_at     TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_incidents_updated_at ON incidents (updated_at);
