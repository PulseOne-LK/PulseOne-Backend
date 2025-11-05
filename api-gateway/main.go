package main

import (
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"
	"os"
	"strings"

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
		profileServiceURL = "http://profile-service:8080"
	}

	port := os.Getenv("PORT")
	if port == "" {
		port = "8081"
	}

	// Parse URLs
	authURL, err := url.Parse(authServiceURL)
	if err != nil {
		log.Fatalf("Invalid AUTH_SERVICE_URL: %v", err)
	}

	profileURL, err := url.Parse(profileServiceURL)
	if err != nil {
		log.Fatalf("Invalid PROFILE_SERVICE_URL: %v", err)
	}

	// Create proxies with prefix stripping
	authProxy := createReverseProxy(authURL, "/auth")
	profileProxy := createReverseProxy(profileURL, "/profile")

	// Route handlers
	http.HandleFunc("/auth/", func(w http.ResponseWriter, r *http.Request) {
		log.Printf("Request: %s %s -> %s", r.Method, r.URL.Path, authServiceURL)
		authProxy.ServeHTTP(w, r)
	})

	http.HandleFunc("/profile/", func(w http.ResponseWriter, r *http.Request) {
		log.Printf("Request: %s %s -> %s", r.Method, r.URL.Path, profileServiceURL)
		profileProxy.ServeHTTP(w, r)
	})

	// Health check for the gateway itself
	http.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("API Gateway OK"))
	})

	log.Printf("API Gateway running on :%s\n", port)
	log.Printf("Routing /auth/* to %s (prefix stripped)\n", authServiceURL)
	log.Printf("Routing /profile/* to %s (prefix stripped)\n", profileServiceURL)
	log.Fatal(http.ListenAndServe(":"+port, nil))
}

// createReverseProxy creates a reverse proxy that strips the given prefix
func createReverseProxy(target *url.URL, prefix string) *httputil.ReverseProxy {
	proxy := httputil.NewSingleHostReverseProxy(target)

	// Customize the Director to strip the prefix
	originalDirector := proxy.Director
	proxy.Director = func(req *http.Request) {
		// Call the original director first
		originalDirector(req)

		// Strip the prefix from the path
		req.URL.Path = strings.TrimPrefix(req.URL.Path, prefix)

		// Ensure path starts with /
		if !strings.HasPrefix(req.URL.Path, "/") {
			req.URL.Path = "/" + req.URL.Path
		}

		// Set the Host header to match the target
		req.Host = target.Host

		log.Printf("Proxying to: %s%s", target.String(), req.URL.Path)
	}

	// Custom error handler
	proxy.ErrorHandler = func(w http.ResponseWriter, r *http.Request, err error) {
		log.Printf("Proxy error for %s %s: %v", r.Method, r.URL.Path, err)
		http.Error(w, "Bad Gateway", http.StatusBadGateway)
	}

	return proxy
}
