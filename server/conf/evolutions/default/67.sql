# --- !Ups
CREATE TABLE IF NOT EXISTS statuses AS
SELECT
  p.name,
  p.status_definitions,
  TRUE AS is_archived
FROM programs p
       INNER JOIN versions_programs vp ON vp.programs_id = p.id
       INNER JOIN versions v ON vp.versions_id = v.id
      WHERE v.lifecycle_stage IN ('active');
# --- !Downs
drop table if exists statuses cascade;
