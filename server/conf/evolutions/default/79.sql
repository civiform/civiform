# --- Create an index on Applications over (submitTime, id) to use during sort ordering/pagination on
# --- the Admin application view.
# ---
# --- Indices must match the query column order and sort order.  The query in this case is a DESC over
# --- both the submitTime and the id.

# --- !Ups
CREATE INDEX IF NOT EXISTS index_applications_by_submit_time_and_id ON applications(submit_time DESC, id DESC);

# --- !Downs
DROP INDEX IF EXISTS index_applications_by_submit_time_and_id;
