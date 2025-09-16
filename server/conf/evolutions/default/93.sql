# --- Allow programs to specify their display mode
# --- !Ups
ALTER TABLE programs
ADD COLUMN login_only boolean DEFAULT FALSE NOT NULL;

# --- !Downs
ALTER TABLE programs
DROP COLUMN login_only;
