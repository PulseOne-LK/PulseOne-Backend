package main

import (
	"fmt"
	"log"
	"net/http"
	"os"

	"auth-service/internal/api"
	"auth-service/internal/db"
	"auth-service/internal/service"

	"github.com/go-chi/chi/v5"
	"github.com/joho/godotenv"
)

func getEnv(key, fallback string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return fallback
}

func main() {
	// Load .env file if present
	_ = godotenv.Load()

	// Read config from environment
	postgresDSN := getEnv("POSTGRES_DSN", "user=postgres password=postgres host=auth-postgres-db port=5432 dbname=authdb sslmode=disable")
	port := getEnv("PORT", "8080")

	// Initialize Database and Services
	dbConn := db.ConnectAndMigrate(postgresDSN)
	defer dbConn.Close()

	userService := service.NewUserService(dbConn)
	authHandlers := api.NewAuthHandlers(userService)

	r := chi.NewRouter()
	r.Post("/register", authHandlers.RegisterHandler)
	r.Post("/login", authHandlers.LoginHandler)
	r.Get("/validate", authHandlers.ValidateTokenHandler)

	fmt.Printf("Auth Service (Go + PostgreSQL + Chi) running on port %s...\n", port)
	log.Fatal(http.ListenAndServe(":"+port, r))
}
