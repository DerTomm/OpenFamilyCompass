-- TaskDefinitions: Klare Trennung zwischen Serien-Enddatum und Deadline
--   end_date → series_end_date  (Bis wann eine Wiederholungsserie Instanzen erzeugt)
--   end_at   → deadline         (Exakter Frist-Zeitpunkt für ONCE-Aufgaben)
ALTER TABLE task_definitions RENAME COLUMN end_date TO series_end_date;
ALTER TABLE task_definitions RENAME COLUMN end_at TO deadline;

-- TaskInstances: due_date (LocalDate) + due_at (LocalDateTime) → deadline (LocalDateTime)
--   deadline = due_at wenn gesetzt, sonst due_date als Tagesende (23:59:59)
ALTER TABLE task_instances ADD COLUMN deadline timestamp;
UPDATE task_instances
SET deadline = COALESCE(due_at, (due_date + INTERVAL '23 hours 59 minutes 59 seconds')::timestamp)
WHERE due_date IS NOT NULL OR due_at IS NOT NULL;
ALTER TABLE task_instances DROP COLUMN due_date;
ALTER TABLE task_instances DROP COLUMN due_at;
