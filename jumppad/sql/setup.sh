#!/bin/sh
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
CREATE TABLE counter (
  count INT NOT NULL
);

INSERT INTO counter (count) VALUES (0);
EOSQL