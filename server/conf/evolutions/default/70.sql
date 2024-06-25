# --- !Ups
INSERT INTO application_statuses (program_name,status_definitions,status_definitions_lifecycle_stage,create_time)
SELECT
  p.name,
  p.status_definitions,
  'active' AS status_definitions_lifecycle_stage,
  p.create_time
FROM programs p
       INNER JOIN versions_programs vp ON vp.programs_id = p.id
       INNER JOIN versions v ON vp.versions_id = v.id
WHERE v.lifecycle_stage IN ('active');

INSERT INTO application_statuses (program_name,status_definitions,status_definitions_lifecycle_stage)
SELECT
  p.name,
  p.status_definitions,
  'obsolete' AS status_definitions_lifecycle_stage
FROM programs p
       INNER JOIN versions_programs vp ON vp.programs_id = p.id
       INNER JOIN versions v ON vp.versions_id = v.id
WHERE v.lifecycle_stage IN ('obsolete');

# --- !Downs
