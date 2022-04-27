 --- Create the api_keys table.

# --- !Ups

CREATE TABLE IF NOT EXISTS api_keys (
  id BIGSERIAL PRIMARY KEY NOT NULL,
  name VARCHAR NOT NULL,
  create_time TIMESTAMP NOT NULL,
  update_time TIMESTAMP NOT NULL,
  created_by VARCHAR NOT NULL,
  retired_time TIMESTAMP,
  retired_by VARCHAR,
  key_id VARCHAR UNIQUE NOT NULL,
  salted_key_secret VARCHAR UNIQUE NOT NULL,
  subnet VARCHAR NOT NULL,
  expiration TIMESTAMP NOT NULL,
  call_count BIGINT NOT NULL DEFAULT 0,
  last_call_ip_address VARCHAR,
  grants JSONB NOT NULL
);

CREATE UNIQUE INDEX api_key_ids ON api_keys (key_id);

# --- !Downs

DROP TABLE IF EXISTS api_keys;
DROP INDEX IF EXISTS api_key_ids;
