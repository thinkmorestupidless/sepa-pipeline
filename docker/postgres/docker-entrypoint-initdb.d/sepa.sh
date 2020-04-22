#!/bin/sh
set -e

psql -U postgres <<DDL
CREATE DATABASE sepa;
REVOKE CONNECT ON DATABASE sepa FROM PUBLIC;
CREATE USER sepa WITH PASSWORD 'sepa';
GRANT CONNECT ON DATABASE sepa TO sepa;
DDL

psql sepa < /docker-entrypoint-initdb.d/sepa.sql.dump
