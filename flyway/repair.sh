#!/bin/bash
source ../../.env
flyway repair -driver=org.postgresql.Driver -url=${POSTGRES_DB} -user=${POSTGRES_USER} -password=${POSTGRES_PASSWORD} -table=flyway_schema_history -locations="classpath:./db/migration"
