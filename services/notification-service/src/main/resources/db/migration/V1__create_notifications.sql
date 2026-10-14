-- Avisos (OB2): cada uno tiene exactamente un destinatario, un usuario o un rol.
CREATE TABLE notifications (
    id                  UUID         PRIMARY KEY,
    type                VARCHAR(20)  NOT NULL CHECK (type IN ('NEW_REPORT', 'STATUS_CHANGED', 'REJECTED')),
    recipient_user_id   UUID,
    recipient_role      VARCHAR(10)  CHECK (recipient_role IN ('CITIZEN', 'OPERATOR')),
    incident_id         UUID         NOT NULL,
    incident_type       VARCHAR(3)   NOT NULL,
    district            VARCHAR(80),
    incident_status     VARCHAR(20)  NOT NULL,
    possible_duplicates INTEGER      NOT NULL DEFAULT 0,
    comment             VARCHAR(500),
    reported_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    title               VARCHAR(120) NOT NULL,
    message             VARCHAR(700) NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT notifications_one_recipient CHECK ((recipient_user_id IS NULL) <> (recipient_role IS NULL))
);

CREATE INDEX idx_notifications_user ON notifications (recipient_user_id, created_at);
CREATE INDEX idx_notifications_role ON notifications (recipient_role, created_at);

-- Lectura de cada usuario; permite contar los no leídos también en los avisos dirigidos a un rol.
CREATE TABLE notification_reads (
    notification_id UUID NOT NULL REFERENCES notifications (id) ON DELETE CASCADE,
    user_id         UUID NOT NULL,
    read_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (notification_id, user_id)
);

CREATE INDEX idx_notification_reads_user ON notification_reads (user_id);
