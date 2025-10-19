package main

import (
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"
	"os"

	"github.com/joho/godotenv"
)

func main() {
	// Load .env file if present
	_ = godotenv.Load()

	authServiceURL := os.Getenv("AUTH_SERVICE_URL")
	if authServiceURL == "" {
		authServiceURL = "http://auth-service:8080"
	}

	port := os.Getenv("PORT")
	if port == "" {
		port = "8081"
	}

	authURL, _ := url.Parse(authServiceURL)
	authProxy := httputil.NewSingleHostReverseProxy(authURL)

	http.HandleFunc("/auth/", func(w http.ResponseWriter, r *http.Request) {
		// Do NOT strip "/auth"
		authProxy.ServeHTTP(w, r)
	})

	log.Printf("API Gateway running on :%s\n", port)
	log.Fatal(http.ListenAndServe(":"+port, nil))
}
