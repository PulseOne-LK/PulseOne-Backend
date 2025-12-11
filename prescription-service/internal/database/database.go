package database

import (
	"fmt"
	"log"

	"prescription-service/internal/model"

	"gorm.io/driver/postgres"
	"gorm.io/gorm"
)

// InitDB initializes the PostgreSQL database connection and runs AutoMigration
func InitDB(host, port, user, password, dbName string) (*gorm.DB, error) {
	dsn := fmt.Sprintf(
		"host=%s port=%s user=%s password=%s dbname=%s sslmode=disable",
		host, port, user, password, dbName,
	)

	db, err := gorm.Open(postgres.Open(dsn), &gorm.Config{})
	if err != nil {
		log.Printf("❌ Failed to connect to database: %v\n", err)
		return nil, err
	}

	log.Println("✓ Connected to PostgreSQL")

	// Run AutoMigration for models
	if err := db.AutoMigrate(
		&model.Prescription{},
		&model.PrescriptionItem{},
	); err != nil {
		log.Printf("❌ Failed to run AutoMigration: %v\n", err)
		return nil, err
	}

	log.Println("✓ Database migrations completed successfully")

	return db, nil
}
