# --- !Ups

ALTER TABLE applications
ADD COLUMN submission_duration interval
GENERATED ALWAYS AS (submit_time - create_time) STORED;

CREATE MATERIALIZED VIEW monthly_submissions_reporting_view AS
SELECT
  programs.name AS program_name,
  date_trunc('month', applications.submit_time) AS submit_month,
  count(*),
  percentile_cont(0.25) WITHIN GROUP (
    ORDER BY applications.submission_duration) AS p25,
  percentile_cont(0.5) WITHIN GROUP (
    ORDER BY applications.submission_duration) AS p50,
  percentile_cont(0.75) WITHIN GROUP (
    ORDER BY applications.submission_duration) AS p75,
  percentile_cont(0.99) WITHIN GROUP (
    ORDER BY applications.submission_duration) AS p99
FROM applications
INNER JOIN programs ON applications.program_id = programs.id
WHERE applications.lifecycle_stage IN ('active', 'obsolete')
GROUP BY programs.name, DATE_TRUNC('month', applications.submit_time)
ORDER BY programs.name, DATE_TRUNC('month', applications.submit_time) DESC;

CREATE INDEX index_applications_by_submit_time ON applications(submit_time);

# --- !Downs

DROP INDEX IF EXISTS index_applications_by_submit_time;

DROP MATERIALIZED VIEW monthly_submissions_reporting_view;

ALTER TABLE applications DROP COLUMN IF EXISTS submission_duration;
