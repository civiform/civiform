# --- !Ups

ALTER TABLE questions ADD COLUMN IF NOT EXISTS concurrency_token UUID NOT NULL DEFAULT gen_random_uuid();

# --- !Downs
ALTER TABLE questions DROP COLUMN IF EXISTS concurrency_token;
