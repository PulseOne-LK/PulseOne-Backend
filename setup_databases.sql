-- =======================================================
-- PulseOne Backend - PostgreSQL Local Setup Script
-- =======================================================
-- This script creates all required databases for local development

-- Connect as superuser (postgres) and run this script:
-- psql -U postgres -f setup_databases.sql

-- =======================================================
-- 1. CREATE DATABASES
-- =======================================================

CREATE DATABASE authdb
    WITH 
    ENCODING 'UTF8'
    LOCALE 'en_US.UTF-8'
    TEMPLATE template0;

CREATE DATABASE profilesdb
    WITH 
    ENCODING 'UTF8'
    LOCALE 'en_US.UTF-8'
    TEMPLATE template0;

CREATE DATABASE appointmentsdb
    WITH 
    ENCODING 'UTF8'
    LOCALE 'en_US.UTF-8'
    TEMPLATE template0;

-- =======================================================
-- 2. VERIFY CREATION
-- =======================================================
\l

-- =======================================================
-- 3. SET UP USERS (optional, if not using default postgres user)
-- =======================================================
-- If you want separate users per service, uncomment below:
-- CREATE ROLE auth_user WITH LOGIN PASSWORD 'auth_password';
-- CREATE ROLE profile_user WITH LOGIN PASSWORD 'profile_password';
-- CREATE ROLE appointments_user WITH LOGIN PASSWORD 'appointments_password';

-- GRANT ALL PRIVILEGES ON DATABASE authdb TO auth_user;
-- GRANT ALL PRIVILEGES ON DATABASE profilesdb TO profile_user;
-- GRANT ALL PRIVILEGES ON DATABASE appointmentsdb TO appointments_user;

-- =======================================================
-- SUCCESS: All databases created!
-- =======================================================
-- 
-- Next steps:
-- 1. Start Auth Service - it will auto-migrate schema via Go
-- 2. Start Profile Service - Hibernate will auto-create schema
-- 3. Start Appointments Service - Hibernate will auto-create schema
--
-- Database URLs for services:
-- - Auth Service:         authdb on localhost:5432
-- - Profile Service:      profilesdb on localhost:5432
-- - Appointments Service: appointmentsdb on localhost:5432
--
-- All services use credentials: postgres / postgres
-- =======================================================
