#!/bin/bash
source ../../.env
flyway repair -driver=org.postgresql.Driver -url="jdbc:postgresql://postgresql.woods.local:5432/postgres" -user=${SPRING_DATASOURCE_USERNAME} -password=${SPRING_DATASOURCE_PASSWORD} -table=flyway_schema_history -locations="classpath:./db/migration"
