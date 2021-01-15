CREATE TABLE IF NOT EXISTS users(
	user_id serial PRIMARY KEY,
	idcs_id VARCHAR(50) UNIQUE
);
