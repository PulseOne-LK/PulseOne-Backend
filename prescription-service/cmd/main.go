package main

import (
	"fmt"
	"log"
	"os"

	_ "prescription-service/docs"
	"prescription-service/internal/database"
	"prescription-service/internal/handlers"

	"github.com/gofiber/fiber/v2"
	"github.com/gofiber/swagger"
	"github.com/joho/godotenv"
)

// @title           Prescription Service API
// @version         1.0
// @description     API for managing patient prescriptions, including creation, retrieval, and status management
// @termsOfService  http://swagger.io/terms/
// @contact.name    PulseOne Support
// @contact.url     http://www.pulseone.com/support
// @license.name    Apache 2.0
// @license.url     http://www.apache.org/licenses/LICENSE-2.0.html
// @host            localhost:8085
// @BasePath        /api
// @schemes         http https
func getEnv(key, fallback string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return fallback
}

func init() {
	// Load .env file before anything else
	paths := []string{
		".env",
		"../.env",
		"./prescription-service/.env",
		"../../.env",
	}

	var loaded bool
	for _, path := range paths {
		if err := godotenv.Load(path); err == nil {
			loaded = true
			break
		}
	}

	if !loaded {
		log.Println("⚠️  Warning: .env file not found in common locations, using system environment variables")
	}
}

func main() {
	// Read config from environment
	port := getEnv("PORT", "8085")
	dbHost := getEnv("DB_HOST", "localhost")
	dbPort := getEnv("DB_PORT", "5436")
	dbUser := getEnv("DB_USER", "postgres")
	dbPassword := getEnv("DB_PASSWORD", "postgres")
	dbName := getEnv("DB_NAME", "prescriptiondb")

	// Initialize Database
	db, err := database.InitDB(dbHost, dbPort, dbUser, dbPassword, dbName)
	if err != nil {
		log.Fatalf("❌ Failed to initialize database: %v\n", err)
	}

	log.Println("✓ Database connected and migrated successfully")

	// Initialize Fiber app
	app := fiber.New(fiber.Config{
		AppName: "Prescription Service v1.0",
	})

	// Health check endpoint
	app.Get("/health", func(c *fiber.Ctx) error {
		return c.JSON(fiber.Map{"status": "healthy"})
	})

	// Swagger documentation
	app.Get("/swagger/*", swagger.HandlerDefault)
	// Swagger UI index route
	app.Get("/swagger-ui/index.html", func(c *fiber.Ctx) error {
		return c.Redirect("/swagger/index.html", fiber.StatusMovedPermanently)
	})

	// Initialize handlers
	prescriptionHandlers := handlers.NewPrescriptionHandler(db)

	// API routes
	api := app.Group("/api")

	// Prescription routes
	api.Post("/prescriptions", prescriptionHandlers.CreatePrescription)
	api.Get("/prescriptions", prescriptionHandlers.GetPrescriptionsForDashboard)
	api.Get("/prescriptions/patient/:patient_id", prescriptionHandlers.GetPatientHistory)
	api.Patch("/prescriptions/:id/status", prescriptionHandlers.UpdateStatus)

	// Start server
	addr := fmt.Sprintf(":%s", port)
	log.Printf("🚀 Prescription Service starting on %s\n", addr)
	if err := app.Listen(addr); err != nil {
		log.Fatalf("❌ Server failed to start: %v\n", err)
	}
}
