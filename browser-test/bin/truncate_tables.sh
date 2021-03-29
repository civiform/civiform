#!/usr/bin/env bash

set -euo pipefail

if ! which psql; then
    docker run --rm -it \
        --network browser-test_default \
        -e BASE_URL=$BASE_URL \
        civiform-browser-test:latest \
        /usr/src/civiform-browser-tests/bin/truncate_tables.sh

    exit $?
fi

if [[ ${BASE_URL-not_set} == "http://localhost:9999" ]]; then
    host=localhost
    port=2345
else
    host=db
    port=5432
fi

cat <<EOF | PGPASSWORD=example psql -h $host -p $port -U postgres
TRUNCATE TABLE accounts CASCADE;
TRUNCATE TABLE applicants CASCADE;
TRUNCATE TABLE applications CASCADE;
TRUNCATE TABLE files CASCADE;
TRUNCATE TABLE programs CASCADE;
TRUNCATE TABLE questions CASCADE;
EOF
