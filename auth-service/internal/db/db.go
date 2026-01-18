package db

import (
	"database/sql"
	"fmt"
	"log"
	"time"

	_ "github.com/lib/pq"
)

// ConnectAndMigrate connects to PostgreSQL, runs migrations, and seeds initial data.
func ConnectAndMigrate(dsn string) *sql.DB {
	db, err := sql.Open("postgres", dsn)
	if err != nil {
		log.Fatalf("Failed to open connection to PostgreSQL: %v", err)
	}

	db.SetMaxOpenConns(25)
	db.SetMaxIdleConns(5)
	db.SetConnMaxLifetime(5 * time.Minute)

	if err := db.Ping(); err != nil {
		log.Fatalf("Failed to ping PostgreSQL database. Ensure your DB is accessible via DSN: %s, Error: %v", dsn, err)
	}
	fmt.Println("Successfully connected to PostgreSQL!")

	createTableQuery := `
        CREATE TABLE IF NOT EXISTS users (
            id SERIAL PRIMARY KEY,
            email VARCHAR(255) UNIQUE NOT NULL,
            password_hash VARCHAR(255) NOT NULL,
            role VARCHAR(50) NOT NULL,
            first_name VARCHAR(100),
            last_name VARCHAR(100),
            is_active BOOLEAN NOT NULL DEFAULT TRUE,
            created_at BIGINT NOT NULL,
            is_verified BOOLEAN NOT NULL DEFAULT FALSE,
            license_number VARCHAR(100) UNIQUE,
            verification_status VARCHAR(50) DEFAULT 'PENDING',
            clinic_id VARCHAR(100)
        );

        CREATE TABLE IF NOT EXISTS email_verification_tokens (
            token VARCHAR(255) PRIMARY KEY,
            user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            expires_at BIGINT NOT NULL,
            used_at BIGINT,
            created_at BIGINT NOT NULL,
            UNIQUE(user_id, token)
        );

        CREATE INDEX IF NOT EXISTS idx_evt_user_id ON email_verification_tokens(user_id);
        CREATE INDEX IF NOT EXISTS idx_evt_expires_at ON email_verification_tokens(expires_at);

        CREATE TABLE IF NOT EXISTS password_reset_tokens (
            token VARCHAR(255) PRIMARY KEY,
            user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            expires_at BIGINT NOT NULL,
            used_at BIGINT,
            created_at BIGINT NOT NULL,
            UNIQUE(user_id, token)
        );

        CREATE INDEX IF NOT EXISTS idx_prt_user_id ON password_reset_tokens(user_id);
        CREATE INDEX IF NOT EXISTS idx_prt_expires_at ON password_reset_tokens(expires_at);

        CREATE TABLE IF NOT EXISTS token_blacklist (
            token VARCHAR(1024) PRIMARY KEY,
            user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            expires_at TIMESTAMP NOT NULL,
            created_at TIMESTAMP DEFAULT NOW()
        );

        CREATE INDEX IF NOT EXISTS idx_tbl_user_id ON token_blacklist(user_id);
        CREATE INDEX IF NOT EXISTS idx_tbl_expires_at ON token_blacklist(expires_at);
    `
	if _, err := db.Exec(createTableQuery); err != nil {
		log.Fatalf("Failed to execute CREATE TABLE migration: %v", err)
	}
	fmt.Println("User table verified/created successfully.")

	sysAdminInsertQuery := `
        INSERT INTO users (email, password_hash, role, is_active, created_at, is_verified, verification_status)
        VALUES (
            'sysadmin@app.com', 
            '$2a$14$iW8lXmK1M3L2S.iV5jJ4o.kYt8Q.T2L4YtQ.g.uHq.vK.W9w.Y5t.', 
            'SYS_ADMIN', TRUE, $1, TRUE, 'APPROVED'
        ) ON CONFLICT (email) DO NOTHING;
    `
	if _, err := db.Exec(sysAdminInsertQuery, time.Now().Unix()); err != nil {
		log.Fatalf("Failed to execute SysAdmin INSERT: %v", err)
	}
	fmt.Println("Initial SysAdmin account checked/created successfully.")

	return db
}
