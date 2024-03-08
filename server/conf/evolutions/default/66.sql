# --- !Ups
--Drop the existing view
DROP MATERIALIZED VIEW IF EXISTS monthly_submissions_reporting_view;
-- create a new view
CREATE MATERIALIZED VIEW IF NOT EXISTS monthly_submissions_reporting_view AS
  SELECT
  programs.name AS program_name,
  activeprogram.localized_name AS localized_name,
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
  INNER JOIN
  (SELECT
    p.name,
    ((p.localized_name #>> '{}')::jsonb #>> '{translations,en_US}') AS localized_name
    FROM programs p
    INNER JOIN versions_programs vp ON
    vp.programs_id = p.id
    INNER JOIN versions v ON
    vp.versions_id = v.id WHERE v.lifecycle_stage IN ('active')) AS activeprogram
  ON activeprogram.name = programs.name
WHERE applications.lifecycle_stage IN ('active', 'obsolete')
GROUP BY programs.name, activeprogram.localized_name, DATE_TRUNC('month', applications.submit_time)
ORDER BY programs.name,activeprogram.localized_name, DATE_TRUNC('month', applications.submit_time) DESC;

--Reindexing
REINDEX INDEX index_applications_by_submit_time;

# --- !Downs

DROP INDEX IF EXISTS index_applications_by_submit_time;

DROP MATERIALIZED VIEW IF EXISTS monthly_submissions_reporting_view;

CREATE MATERIALIZED VIEW IF NOT EXISTS monthly_submissions_reporting_view AS
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





