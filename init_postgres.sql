-- Initialize databases used for unit tests and browser tests.
-- Those databases are separated from the default database as they are wiped out
-- by tests while the default database used by the developer.
CREATE DATABASE unittests;
GRANT ALL PRIVILEGES ON DATABASE unittests TO postgres;

