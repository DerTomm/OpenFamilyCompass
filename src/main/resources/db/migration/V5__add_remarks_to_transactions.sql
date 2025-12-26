-- Migration: Hinzufügen der remarks Spalte zu point_transactions
-- Ermöglicht zusätzliche Bemerkungen bei Transaktionen (z.B. für Verhaltensregeln)

ALTER TABLE point_transactions 
ADD COLUMN IF NOT EXISTS remarks TEXT;
