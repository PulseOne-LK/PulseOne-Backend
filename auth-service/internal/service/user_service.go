package service

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"time"

	"auth-service/internal/model"
	"auth-service/pkg/auth"

	_ "github.com/lib/pq" // PostgreSQL driver
)

// Define specific errors for API handlers to interpret
var ErrUserExists = errors.New("user already exists")
var ErrInvalidCredentials = errors.New("invalid email or password")
var ErrAccountInactive = errors.New("account inactive or suspended")

// UserService holds the database connection pool.
type UserService struct {
	DB *sql.DB
}

func NewUserService(db *sql.DB) *UserService {
	return &UserService{
		DB: db,
	}
}

// RegisterUser handles user registration, hashing, and initial status setting using PostgreSQL.
func (s *UserService) RegisterUser(ctx context.Context, req model.AuthRequest) (*model.User, string, error) {
	// 1. Check if user already exists (QueryRowContext.Scan will return nil if found)
	// 1. Check if user already exists
	var userID int // Using int since your DB uses SERIAL
	err := s.DB.QueryRowContext(ctx, "SELECT id FROM users WHERE email = $1", req.Email).Scan(&userID)

	switch {
	case err == nil:
		// Case 1: Row found and scanned successfully. User already exists.
		return nil, "", ErrUserExists

	case errors.Is(err, sql.ErrNoRows):
		// Case 2: No row found. This is the expected path to proceed with registration.
		// The logic continues below (no 'return' here).

	default:
		// Case 3: Any other unexpected database error (e.g., network, syntax, permissions).
		return nil, "", err
	}

	// 2. Hash Password
	hashedPassword, err := auth.HashPassword(req.Password)
	if err != nil {
		return nil, "", err
	}

	// 3. Determine Verification Status based on Role
	isVerified := req.Role == model.RolePatient || req.Role == model.RoleSysAdmin || req.Role == model.RoleService
	verificationStatus := "APPROVED"
	if req.Role == model.RoleDoctor || req.Role == model.RolePharmacist || req.Role == model.RoleClinicAdmin {
		verificationStatus = "PENDING"
	}

	// 4. Create and Insert User
	newUser := model.User{
		Email:              req.Email,
		PasswordHash:       hashedPassword,
		Role:               req.Role,
		FirstName:          sql.NullString{},
		LastName:           sql.NullString{},
		IsActive:           true,
		CreatedAt:          time.Now().Unix(),
		IsVerified:         isVerified,
		VerificationStatus: verificationStatus,
	}

	// SQL query to insert and return the generated ID
	query := `
        INSERT INTO users (
            email, password_hash, role, first_name, last_name, is_active, created_at,
            is_verified, verification_status, license_number, clinic_id
        ) VALUES (
            $1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11
        ) RETURNING id
    `
	var insertedID int // PostgreSQL SERIAL returns an integer
	err = s.DB.QueryRowContext(ctx, query,
		newUser.Email, newUser.PasswordHash, newUser.Role, newUser.FirstName, newUser.LastName, newUser.IsActive, newUser.CreatedAt,
		newUser.IsVerified, newUser.VerificationStatus, newUser.LicenseNumber, newUser.ClinicID,
	).Scan(&insertedID)

	if err != nil {
		return nil, "", err
	}

	// Convert ID to string for consistent use
	newUser.ID = fmt.Sprintf("%d", insertedID)

	// 5. Generate Token
	token, err := auth.GenerateJWT(newUser.ID, string(newUser.Role))
	if err != nil {
		return nil, "", err
	}

	return &newUser, token, nil
}

// LoginUser handles authentication and verification checks.
func (s *UserService) LoginUser(ctx context.Context, email, password string) (*model.User, string, error) {
	var user model.User
	var passwordHash string

	// NOTE: We MUST select all fields necessary for the login check
	query := `
        SELECT id, password_hash, role, is_active, is_verified, verification_status, first_name, last_name
        FROM users
        WHERE email = $1
    `
	err := s.DB.QueryRowContext(ctx, query, email).Scan(
		&user.ID,
		&passwordHash,
		&user.Role,
		&user.IsActive,
		&user.IsVerified,
		&user.VerificationStatus,
		&user.FirstName,
		&user.LastName,
	)

	if err == sql.ErrNoRows {
		return nil, "", ErrInvalidCredentials
	}
	if err != nil {
		return nil, "", err
	}

	// 1. Check password hash
	if !auth.CheckPasswordHash(password, passwordHash) {
		return nil, "", ErrInvalidCredentials
	}

	// 2. Check general status
	if !user.IsActive {
		return nil, "", ErrAccountInactive
	}

	// 3. Check professional verification status (if unverified, return user but no token)
	if (user.Role == model.RoleDoctor || user.Role == model.RolePharmacist || user.Role == model.RoleClinicAdmin) && !user.IsVerified {
		return &user, "", nil
	}

	// 4. Generate Token
	token, err := auth.GenerateJWT(user.ID, string(user.Role))
	if err != nil {
		return nil, "", err
	}

	return &user, token, nil
}
