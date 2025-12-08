package auth

import (
	"errors"
	"os"
	"sync"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"golang.org/x/crypto/bcrypt"
)

var (
	jwtKey []byte
	once   sync.Once
)

// getJWTKey returns the JWT secret key, loading it lazily from environment
func getJWTKey() []byte {
	once.Do(func() {
		secret := os.Getenv("JWT_SECRET")
		if secret == "" {
			panic("JWT_SECRET environment variable is required")
		}
		jwtKey = []byte(secret)
	})
	return jwtKey
}

// AuthClaims holds the custom claims included in the JWT payload.
type AuthClaims struct {
	UserID string `json:"user_id"`
	Role   string `json:"role"`
	jwt.RegisteredClaims
}

// HashPassword securely hashes the user's password using bcrypt.
func HashPassword(password string) (string, error) {
	// Cost factor of 14 is a strong modern default.
	bytes, err := bcrypt.GenerateFromPassword([]byte(password), 14)
	return string(bytes), err
}

// CheckPasswordHash compares a plaintext password with a hashed password.
func CheckPasswordHash(password, hash string) bool {
	// Compare hashed password with plaintext password
	err := bcrypt.CompareHashAndPassword([]byte(hash), []byte(password))
	return err == nil
}

// GenerateJWT creates a signed JSON Web Token for the authenticated user.
func GenerateJWT(userID string, role string) (string, error) {
	expirationTime := time.Now().Add(24 * time.Hour)
	claims := &AuthClaims{
		UserID: userID,
		Role:   role,
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(expirationTime),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
		},
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString(getJWTKey())
}

// ValidateJWT parses and validates the JWT.
func ValidateJWT(tokenString string) (*AuthClaims, error) {
	claims := &AuthClaims{}
	token, err := jwt.ParseWithClaims(tokenString, claims, func(token *jwt.Token) (interface{}, error) {
		return getJWTKey(), nil
	})

	if err != nil {
		return nil, err
	}
	if token == nil || !token.Valid {
		return nil, errors.New("invalid token")
	}
	return claims, nil
}
