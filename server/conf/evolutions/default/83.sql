# --- !Ups

ALTER TABLE applications ADD COLUMN IF NOT EXISTS status_create_time TIMESTAMP;

-- Update historical data from application events table
UPDATE applications
SET status_create_time = match_applications.create_time
  FROM (
SELECT
    latest_status_event.application_id,
    latest_status_event.create_time,
    latest_status_event.row_number
FROM
    (
        SELECT
            application_id,
            create_time,
            ROW_NUMBER() OVER (PARTITION BY application_id ORDER BY create_time DESC) AS row_number
        FROM
            application_events
        WHERE
            details->>'event_type' = 'STATUS_CHANGE'
    ) latest_status_event
WHERE
    latest_status_event.row_number = 1
) AS match_applications WHERE applications.id = match_applications.application_id;

# --- !Downs

ALTER TABLE applications DROP COLUMN IF EXISTS status_create_time;

