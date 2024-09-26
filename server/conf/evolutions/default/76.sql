# --- !Ups

ALTER TABLE applications ADD COLUMN IF NOT EXISTS latest_note varchar;

# -- Update historical data from application events table
UPDATE applications
SET latest_note = match_applications.latest_note
  FROM (
SELECT
    latest_note_event.application_id,
    latest_note_event.latest_note,
    latest_note_event.row_number
FROM
    (
        SELECT
            application_id,
            details->'note_event'->>'note' AS latest_note,
            ROW_NUMBER() OVER (PARTITION BY application_id ORDER BY create_time DESC) AS row_number
        FROM
            application_events
        WHERE
            details->>'event_type' = 'NOTE_CHANGE'
    ) latest_note_event
WHERE
    latest_note_event.row_number = 1
) AS match_applications WHERE applications.id = match_applications.application_id;

# --- !Downs

ALTER TABLE applications DROP COLUMN IF EXISTS latest_note;

