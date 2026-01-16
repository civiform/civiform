# --- Fix indexes made in evolution 42 to not be unique

# --- !Ups

DROP INDEX IF EXISTS index_application_events_by_application;
DROP INDEX IF EXISTS index_application_events_by_creator;
CREATE INDEX index_application_events_by_application ON application_events (application_id);
CREATE INDEX index_application_events_by_creator ON application_events (creator_id);

# --- !Downs

--- Note we're not recreating the previous indexes as evolution 42 didn't remove
--- them as it should have, so we're doing the next best thing.
DROP INDEX IF EXISTS index_application_events_by_application;
DROP INDEX IF EXISTS index_application_events_by_creator;
