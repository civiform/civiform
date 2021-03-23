#!/usr/bin/env bash

set -euo pipefail

cat <<EOF | PGPASSWORD=example psql -h db -p 5432 -U postgres 
TRUNCATE TABLE accounts CASCADE;
TRUNCATE TABLE applicants CASCADE;
TRUNCATE TABLE applications CASCADE;
TRUNCATE TABLE files CASCADE;
TRUNCATE TABLE programs CASCADE;
TRUNCATE TABLE questions CASCADE;
EOF
