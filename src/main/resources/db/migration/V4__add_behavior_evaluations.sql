-- Migration: Hinzufügen der behavior_evaluations Tabelle
-- Ermöglicht wöchentliche Verhaltensbewertungen mit Bemerkungen

CREATE TABLE IF NOT EXISTS behavior_evaluations (
    id BIGSERIAL PRIMARY KEY,
    behavior_id BIGINT NOT NULL,
    child_id BIGINT NOT NULL,
    current_points INT NOT NULL DEFAULT 0,
    remarks TEXT,
    week_start_date TIMESTAMP NOT NULL,
    committed BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_behavior_evaluation_behavior FOREIGN KEY (behavior_id) REFERENCES behaviors(id) ON DELETE CASCADE,
    CONSTRAINT fk_behavior_evaluation_child FOREIGN KEY (child_id) REFERENCES children(id) ON DELETE CASCADE,
    CONSTRAINT fk_behavior_evaluation_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_behavior_evaluations_child ON behavior_evaluations(child_id);
CREATE INDEX idx_behavior_evaluations_week ON behavior_evaluations(week_start_date);
CREATE INDEX idx_behavior_evaluations_committed ON behavior_evaluations(committed);
