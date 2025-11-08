package model

import "database/sql"

// UserRole defines the explicit user roles in the system.
type UserRole string

const (
	RolePatient     UserRole = "PATIENT"
	RoleDoctor      UserRole = "DOCTOR"       // Virtual Doctor
	RolePharmacist  UserRole = "PHARMACIST"   // Fulfillment Agent
	RoleClinicAdmin UserRole = "CLINIC_ADMIN" // Practice Management
	RoleSysAdmin    UserRole = "SYS_ADMIN"    // Platform Admin
	RoleService     UserRole = "SERVICE"      // Non-Human Account
)

// User represents the core identity entity for PostgreSQL.
type User struct {
	ID           string         `json:"id,omitempty"` // SERIAL will be int, but we'll use string here for simplicity/UUID future
	Email        string         `json:"email" validate:"required,email"`
	PasswordHash string         `json:"-"` // Omitted in API response
	Role         UserRole       `json:"role" validate:"required"`
	FirstName    sql.NullString `json:"first_name"`
	LastName     sql.NullString `json:"last_name"`
	IsActive     bool           `json:"is_active"`
	CreatedAt    int64          `json:"created_at"`

	// Professional Fields
	IsVerified         bool           `json:"is_verified"`
	LicenseNumber      sql.NullString `json:"license_number,omitempty"`
	VerificationStatus string         `json:"verification_status"` // PENDING, APPROVED, REJECTED
	ClinicID           sql.NullString `json:"clinic_id,omitempty"`
}

// AuthRequest is the payload for login/register API calls.
type AuthRequest struct {
	Email    string   `json:"email"`
	Password string   `json:"password"`
	Role     UserRole `json:"role"`
}

// LoginResponse is the data returned upon successful login.
type LoginResponse struct {
	Message string   `json:"message"`
	Token   string   `json:"token"`
	UserID  string   `json:"user_id"`
	Role    UserRole `json:"role"`
}

// ForgotPasswordRequest is the payload for forgot password API call.
type ForgotPasswordRequest struct {
	Email string `json:"email" validate:"required,email"`
}

// ResetPasswordRequest is the payload for reset password API call.
type ResetPasswordRequest struct {
	Token       string `json:"token" validate:"required"`
	NewPassword string `json:"new_password" validate:"required,min=8"`
}

// PasswordResetToken represents a password reset token in the database.
type PasswordResetToken struct {
	Token     string        `json:"token"`
	UserID    string        `json:"user_id"`
	ExpiresAt int64         `json:"expires_at"`
	UsedAt    sql.NullInt64 `json:"used_at,omitempty"`
	CreatedAt int64         `json:"created_at"`
}
