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

	profileServiceURL := os.Getenv("PROFILE_SERVICE_URL")
	if profileServiceURL == "" {
		profileServiceURL = "http://profile-service:8082"
	}

	port := os.Getenv("PORT")
	if port == "" {
		port = "8081"
	}

	authURL, _ := url.Parse(authServiceURL)
	authProxy := httputil.NewSingleHostReverseProxy(authURL)

	profileURL, _ := url.Parse(profileServiceURL)
	profileProxy := httputil.NewSingleHostReverseProxy(profileURL)

	http.HandleFunc("/auth/", func(w http.ResponseWriter, r *http.Request) {
		// Do NOT strip "/auth"
		authProxy.ServeHTTP(w, r)
	})

	http.HandleFunc("/profile/", func(w http.ResponseWriter, r *http.Request) {
		// Do NOT strip "/profile"
		profileProxy.ServeHTTP(w, r)
	})

	log.Printf("API Gateway running on :%s\n", port)
	log.Printf("Routing /auth/* to %s\n", authServiceURL)
	log.Printf("Routing /profile/* to %s\n", profileServiceURL)
	log.Fatal(http.ListenAndServe(":"+port, nil))
}
