package api

import (
	"encoding/json"
	"errors"
	"log"
	"net/http"
	"strings"

	"auth-service/internal/model"
	"auth-service/internal/service"
	"auth-service/pkg/auth"
)

// AuthHandlers holds dependencies for the API handlers.
type AuthHandlers struct {
	Service *service.UserService
}

func NewAuthHandlers(s *service.UserService) *AuthHandlers {
	return &AuthHandlers{Service: s}
}

// respondJSON is a utility to write JSON responses.
func respondJSON(w http.ResponseWriter, status int, payload interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	if err := json.NewEncoder(w).Encode(payload); err != nil {
		log.Printf("Error writing JSON response: %v", err)
	}
}

// RegisterHandler handles POST /auth/register
func (h *AuthHandlers) RegisterHandler(w http.ResponseWriter, r *http.Request) {
	var req model.AuthRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondJSON(w, http.StatusBadRequest, map[string]string{"error": "Invalid request payload"})
		return
	}

	// Simple role validation for self-registration
	if req.Role != model.RolePatient && req.Role != model.RoleDoctor && req.Role != model.RolePharmacist {
		respondJSON(w, http.StatusForbidden, map[string]string{"error": "Invalid role for self-registration. Use ADMIN for other roles."})
		return
	}

	user, token, err := h.Service.RegisterUser(r.Context(), req)
	if err != nil {
		if errors.Is(err, service.ErrUserExists) {
			respondJSON(w, http.StatusConflict, map[string]string{"error": "User with this email already exists"})
			return
		}
		log.Printf("Registration error: %v", err)
		respondJSON(w, http.StatusInternalServerError, map[string]string{"error": "Failed to register user due to an internal error."})
		return
	}

	// Successful registration response
	respondJSON(w, http.StatusCreated, map[string]interface{}{
		"message": "User registered successfully",
		"token":   token,
		"user_id": user.ID,
		"role":    user.Role,
	})
}

// LoginHandler handles POST /auth/login
func (h *AuthHandlers) LoginHandler(w http.ResponseWriter, r *http.Request) {
	var req model.AuthRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondJSON(w, http.StatusBadRequest, map[string]string{"error": "Invalid request payload"})
		return
	}

	user, token, err := h.Service.LoginUser(r.Context(), req.Email, req.Password)
	if err != nil {
		if errors.Is(err, service.ErrInvalidCredentials) {
			respondJSON(w, http.StatusUnauthorized, map[string]string{"error": "Invalid email or password"})
			return
		}
		if errors.Is(err, service.ErrAccountInactive) {
			respondJSON(w, http.StatusForbidden, map[string]string{"error": "Account is suspended by administrator."})
			return
		}

		// Case: User exists, credentials are valid, but verification is pending
		if token == "" && user != nil {
			respondJSON(w, http.StatusForbidden, map[string]string{
				"error":   "Account verification is pending.",
				"details": "Please wait for an Admin to approve your license/details.",
			})
			return
		}

		log.Printf("Login error: %v", err)
		respondJSON(w, http.StatusInternalServerError, map[string]string{"error": "Internal server error during login"})
		return
	}

	respondJSON(w, http.StatusOK, model.LoginResponse{
		Message: "Login successful",
		Token:   token,
		UserID:  user.ID,
		Role:    user.Role,
	})
}

// AdminRegisterHandler handles POST /auth/admin/register
// Allows SYS_ADMIN to create accounts for CLINIC_ADMIN and other roles
func (h *AuthHandlers) AdminRegisterHandler(w http.ResponseWriter, r *http.Request) {
	// 1. Extract and validate admin credentials from headers
	adminUserID := r.Header.Get("X-User-ID")
	adminRole := r.Header.Get("X-User-Role")

	if adminUserID == "" || adminRole != string(model.RoleSysAdmin) {
		respondJSON(w, http.StatusForbidden, map[string]string{"error": "Access denied: Only SYS_ADMIN can create users via this endpoint"})
		return
	}

	// 2. Parse request body
	var req model.AuthRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondJSON(w, http.StatusBadRequest, map[string]string{"error": "Invalid request payload"})
		return
	}

	// 3. Validate the role being created
	validAdminRoles := []model.UserRole{
		model.RoleClinicAdmin,
		model.RoleSysAdmin,
		model.RoleDoctor,
		model.RolePharmacist,
	}

	isValidRole := false
	for _, validRole := range validAdminRoles {
		if req.Role == validRole {
			isValidRole = true
			break
		}
	}

	if !isValidRole {
		respondJSON(w, http.StatusBadRequest, map[string]string{"error": "Invalid role for admin registration"})
		return
	}

	// 4. Register the user using the service (with admin privileges)
	user, token, err := h.Service.AdminRegisterUser(r.Context(), req)
	if err != nil {
		if errors.Is(err, service.ErrUserExists) {
			respondJSON(w, http.StatusConflict, map[string]string{"error": "User with this email already exists"})
			return
		}
		log.Printf("Admin registration error: %v", err)
		respondJSON(w, http.StatusInternalServerError, map[string]string{"error": "Failed to register user due to an internal error."})
		return
	}

	// 5. Successful registration response
	respondJSON(w, http.StatusCreated, map[string]interface{}{
		"message": "User registered successfully by admin",
		"token":   token,
		"user_id": user.ID,
		"role":    user.Role,
	})
}

// ValidateTokenHandler handles GET /auth/validate
// This is used by the API Gateway to enforce security on every request.
func (h *AuthHandlers) ValidateTokenHandler(w http.ResponseWriter, r *http.Request) {
	authHeader := r.Header.Get("Authorization")
	tokenString := strings.TrimPrefix(authHeader, "Bearer ")

	claims, err := auth.ValidateJWT(tokenString)
	if err != nil {
		respondJSON(w, http.StatusUnauthorized, map[string]string{"error": "Invalid or expired token"})
		return
	}

	respondJSON(w, http.StatusOK, map[string]string{
		"message": "Token is valid",
		"user_id": claims.UserID,
		"role":    claims.Role,
	})
}
