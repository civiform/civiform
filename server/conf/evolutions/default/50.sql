# --- !Ups

CREATE INDEX index_applications_by_submit_time ON applications(submit_time);

# --- !Downs

DROP INDEX IF EXISTS index_applications_by_submit_time;
