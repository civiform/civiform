# --- !Ups

ALTER TABLE applications ADD COLUMN IF NOT EXISTS latest_note varchar;
-- Update historical data from application events table
UPDATE applications
SET latest_note = c.latest_note
  FROM (
SELECT
    b.application_id,
    b.latest_note,
    b.row_number
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
    ) b
WHERE
    b.row_number = 1
) AS c WHERE applications.id = c.application_id;

# --- !Downs

alter table applications drop column if exists latest_note;

