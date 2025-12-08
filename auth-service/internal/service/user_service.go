package service

import (
	"context"
	"crypto/rand"
	"database/sql"
	"encoding/base64"
	"errors"
	"fmt"
	"net/smtp"
	"os"
	"strings"
	"time"

	"auth-service/internal/messaging"
	"auth-service/internal/model"
	"auth-service/internal/proto"
	"auth-service/pkg/auth"
)

// Define specific errors for API handlers to interpret
var ErrUserExists = errors.New("user already exists")
var ErrInvalidCredentials = errors.New("invalid email or password")
var ErrAccountInactive = errors.New("account inactive or suspended")

// UserService holds the database connection pool.
type UserService struct {
	DB                 *sql.DB
	ProfileClient      *ProfileServiceClient
	AppointmentsClient *AppointmentsServiceClient
	RabbitPublisher    *messaging.RabbitMQPublisher
}

// generateRandomToken creates a URL-safe random token string of n bytes.
func generateRandomToken(n int) (string, error) {
	b := make([]byte, n)
	if _, err := rand.Read(b); err != nil {
		return "", err
	}
	return base64.RawURLEncoding.EncodeToString(b), nil
}

func (s *UserService) saveVerificationToken(ctx context.Context, userID int, token string, ttlMinutes int) error {
	expiresAt := time.Now().Add(time.Duration(ttlMinutes) * time.Minute).Unix()
	createdAt := time.Now().Unix()
	_, err := s.DB.ExecContext(ctx, `INSERT INTO email_verification_tokens(token, user_id, expires_at, created_at) VALUES($1,$2,$3,$4)`, token, userID, expiresAt, createdAt)
	return err
}

func (s *UserService) markTokenUsedAndVerifyUser(ctx context.Context, token string) (int, error) {
	// Fetch token
	var userID int
	var expiresAt int64
	var usedAt sql.NullInt64
	err := s.DB.QueryRowContext(ctx, `SELECT user_id, expires_at, used_at FROM email_verification_tokens WHERE token=$1`, token).Scan(&userID, &expiresAt, &usedAt)
	if err == sql.ErrNoRows {
		return 0, errors.New("invalid token")
	}
	if err != nil {
		return 0, err
	}
	if usedAt.Valid {
		return 0, errors.New("token already used")
	}
	if time.Now().Unix() > expiresAt {
		return 0, errors.New("token expired")
	}

	// Verify user and mark token used in a transaction
	tx, err := s.DB.BeginTx(ctx, nil)
	if err != nil {
		return 0, err
	}
	defer func() {
		if err != nil {
			_ = tx.Rollback()
		}
	}()

	if _, err = tx.ExecContext(ctx, `UPDATE users SET is_verified=TRUE WHERE id=$1`, userID); err != nil {
		return 0, err
	}
	if _, err = tx.ExecContext(ctx, `UPDATE email_verification_tokens SET used_at=$1 WHERE token=$2`, time.Now().Unix(), token); err != nil {
		return 0, err
	}
	if err = tx.Commit(); err != nil {
		return 0, err
	}
	return userID, nil
}

func (s *UserService) sendVerificationEmail(toEmail, verifyURL string) error {
	from := os.Getenv("GMAIL_SMTP_EMAIL")
	appPass := os.Getenv("GMAIL_SMTP_APP_PASSWORD")
	fromName := os.Getenv("EMAIL_FROM_NAME")
	if from == "" || appPass == "" {
		return errors.New("smtp credentials not configured")
	}

	host := "smtp.gmail.com"
	addr := host + ":587"
	auth := smtp.PlainAuth("", from, appPass, host)
	if fromName == "" {
		fromName = "PulseOne"
	}

	subject := "Verify your email"
	body := fmt.Sprintf("Hello,\r\n\r\nPlease verify your email by clicking the link below:\r\n%s\r\n\r\nThis link will expire in 15 minutes.\r\n\r\nThanks,\r\n%s Team\r\n", verifyURL, fromName)
	// Build RFC 5322 message
	headers := []string{
		"From: " + fromName + " <" + from + ">",
		"To: " + toEmail,
		"Subject: " + subject,
		"MIME-Version: 1.0",
		"Content-Type: text/plain; charset=utf-8",
	}
	msg := strings.Join(headers, "\r\n") + "\r\n\r\n" + body
	return smtp.SendMail(addr, auth, from, []string{toEmail}, []byte(msg))
}

func NewUserService(db *sql.DB, profileClient *ProfileServiceClient, appointmentsClient *AppointmentsServiceClient, rabbitPublisher *messaging.RabbitMQPublisher) *UserService {
	return &UserService{
		DB:                 db,
		ProfileClient:      profileClient,
		AppointmentsClient: appointmentsClient,
		RabbitPublisher:    rabbitPublisher,
	}
}

// RegisterUser handles user registration, hashing, initial status, and sends email verification using PostgreSQL.
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
	// For email verification flow, default new self-registered users to not verified.
	// Professional roles still keep VerificationStatus=PENDING for manual approval flow.
	isVerified := false
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

	// 5. Generate auth token (optional for immediate use)
	jwtToken, err := auth.GenerateJWT(newUser.ID, string(newUser.Role))
	if err != nil {
		return nil, "", err
	}

	// 6. Email verification: generate token, store, and send email
	verifyToken, err := generateRandomToken(32)
	if err != nil {
		return nil, "", err
	}
	if err := s.saveVerificationToken(ctx, insertedID, verifyToken, 15); err != nil {
		return nil, "", err
	}
	baseURL := os.Getenv("AUTH_PUBLIC_BASE_URL")
	if baseURL == "" {
		baseURL = "http://localhost:8080"
	}
	verifyURL := strings.TrimRight(baseURL, "/") + "/auth/verify?token=" + verifyToken
	if err := s.sendVerificationEmail(newUser.Email, verifyURL); err != nil {
		// Log-only behavior could be added here; returning error ensures caller knows email didn't send
		return nil, "", err
	}

	// 7. Notify other services via HTTP
	event := UserRegistrationEventRequest{
		UserID:    newUser.ID,
		Email:     newUser.Email,
		Role:      string(newUser.Role),
		FirstName: newUser.FirstName.String,
		LastName:  newUser.LastName.String,
	}

	// For CLINIC_ADMIN users, add default clinic data
	// TODO: This should come from a proper clinic registration form
	if newUser.Role == model.RoleClinicAdmin {
		fullName := newUser.FirstName.String + " " + newUser.LastName.String
		if fullName == " " {
			fullName = newUser.Email
		}
		event.ClinicName = "Clinic managed by " + fullName
		event.ClinicPhysicalAddress = "Address to be provided"
		event.ClinicContactPhone = ""                           // Optional
		event.ClinicOperatingHours = "Monday-Friday 9:00-17:00" // Default hours
	}

	// Publish event to RabbitMQ (primary method)
	if s.RabbitPublisher != nil {
		go func() {
			// Convert to protobuf format
			var clinicData *proto.ClinicData
			if newUser.Role == model.RoleClinicAdmin {
				clinicData = &proto.ClinicData{
					Name:            event.ClinicName,
					PhysicalAddress: event.ClinicPhysicalAddress,
					ContactPhone:    event.ClinicContactPhone,
					OperatingHours:  event.ClinicOperatingHours,
				}
			}

			pbEvent := &proto.UserRegistrationEvent{
				UserId:      newUser.ID,
				Email:       newUser.Email,
				Role:        string(newUser.Role),
				FirstName:   newUser.FirstName.String,
				LastName:    newUser.LastName.String,
				PhoneNumber: "",
				Timestamp:   time.Now().Unix(),
				EventType:   "user.registered",
				ClinicData:  clinicData,
			}

			if err := s.RabbitPublisher.PublishUserRegistrationEvent(pbEvent); err != nil {
				fmt.Printf("⚠️  Warning: Failed to publish to RabbitMQ: %v. Falling back to HTTP.\n", err)
				// Fall back to HTTP if RabbitMQ fails
				if s.ProfileClient != nil {
					if err := s.ProfileClient.NotifyUserRegistered(context.Background(), event); err != nil {
						fmt.Printf("Warning: Failed to notify profile service of user registration: %v\n", err)
					}
				}
				if s.AppointmentsClient != nil {
					if err := s.AppointmentsClient.NotifyUserRegistered(context.Background(), event); err != nil {
						fmt.Printf("Warning: Failed to notify appointments service of user registration: %v\n", err)
					}
				}
			} else {
				fmt.Printf("✓ Published user registration event to RabbitMQ: %s (role: %s, email: %s)\n", newUser.ID, newUser.Role, newUser.Email)
			}
		}()
	} else {
		// RabbitMQ not available, use HTTP fallback
		// Send notification to profile service asynchronously
		if s.ProfileClient != nil {
			go func() {
				if err := s.ProfileClient.NotifyUserRegistered(context.Background(), event); err != nil {
					// Log error but don't fail the registration
					fmt.Printf("Warning: Failed to notify profile service of user registration: %v\n", err)
				}
			}()
		}

		// Send notification to appointments service asynchronously
		if s.AppointmentsClient != nil {
			go func() {
				if err := s.AppointmentsClient.NotifyUserRegistered(context.Background(), event); err != nil {
					// Log error but don't fail the registration
					fmt.Printf("Warning: Failed to notify appointments service of user registration: %v\n", err)
				}
			}()
		}
	}

	return &newUser, jwtToken, nil
}

// VerifyEmailToken verifies a token and marks the associated user as verified.
func (s *UserService) VerifyEmailToken(ctx context.Context, token string) error {
	if strings.TrimSpace(token) == "" {
		return errors.New("token required")
	}
	_, err := s.markTokenUsedAndVerifyUser(ctx, token)
	return err
}

// AdminRegisterUser handles user registration by SYS_ADMIN with auto-approval.
// Unlike regular registration, accounts created this way are automatically verified.
func (s *UserService) AdminRegisterUser(ctx context.Context, req model.AuthRequest) (*model.User, string, error) {
	// 1. Check if user already exists
	var userID int
	err := s.DB.QueryRowContext(ctx, "SELECT id FROM users WHERE email = $1", req.Email).Scan(&userID)

	switch {
	case err == nil:
		return nil, "", ErrUserExists
	case errors.Is(err, sql.ErrNoRows):
		// No user found, proceed with registration
	default:
		return nil, "", err
	}

	// 2. Hash Password
	hashedPassword, err := auth.HashPassword(req.Password)
	if err != nil {
		return nil, "", err
	}

	// 3. Admin-created accounts are automatically verified and approved
	newUser := model.User{
		Email:              req.Email,
		PasswordHash:       hashedPassword,
		Role:               req.Role,
		FirstName:          sql.NullString{},
		LastName:           sql.NullString{},
		IsActive:           true,
		CreatedAt:          time.Now().Unix(),
		IsVerified:         true,       // Auto-verify admin-created accounts
		VerificationStatus: "APPROVED", // Auto-approve
	}

	// 4. Insert User
	query := `
        INSERT INTO users (
            email, password_hash, role, first_name, last_name, is_active, created_at,
            is_verified, verification_status, license_number, clinic_id
        ) VALUES (
            $1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11
        ) RETURNING id
    `
	var insertedID int
	err = s.DB.QueryRowContext(ctx, query,
		newUser.Email, newUser.PasswordHash, newUser.Role, newUser.FirstName, newUser.LastName, newUser.IsActive, newUser.CreatedAt,
		newUser.IsVerified, newUser.VerificationStatus, newUser.LicenseNumber, newUser.ClinicID,
	).Scan(&insertedID)

	if err != nil {
		return nil, "", err
	}

	newUser.ID = fmt.Sprintf("%d", insertedID)

	// 5. Generate Token
	token, err := auth.GenerateJWT(newUser.ID, string(newUser.Role))
	if err != nil {
		return nil, "", err
	}

	// 6. Notify other services via HTTP
	event := UserRegistrationEventRequest{
		UserID:    newUser.ID,
		Email:     newUser.Email,
		Role:      string(newUser.Role),
		FirstName: newUser.FirstName.String,
		LastName:  newUser.LastName.String,
	}

	// For CLINIC_ADMIN users, add default clinic data
	// TODO: This should come from a proper clinic registration form
	if newUser.Role == model.RoleClinicAdmin {
		fullName := newUser.FirstName.String + " " + newUser.LastName.String
		if fullName == " " {
			fullName = newUser.Email
		}
		event.ClinicName = "Clinic managed by " + fullName
		event.ClinicPhysicalAddress = "Address to be provided"
		event.ClinicContactPhone = ""                           // Optional
		event.ClinicOperatingHours = "Monday-Friday 9:00-17:00" // Default hours
	}

	// Publish event to RabbitMQ (primary method)
	if s.RabbitPublisher != nil {
		go func() {
			// Convert to protobuf format
			var clinicData *proto.ClinicData
			if newUser.Role == model.RoleClinicAdmin {
				clinicData = &proto.ClinicData{
					Name:            event.ClinicName,
					PhysicalAddress: event.ClinicPhysicalAddress,
					ContactPhone:    event.ClinicContactPhone,
					OperatingHours:  event.ClinicOperatingHours,
				}
			}

			pbEvent := &proto.UserRegistrationEvent{
				UserId:      newUser.ID,
				Email:       newUser.Email,
				Role:        string(newUser.Role),
				FirstName:   newUser.FirstName.String,
				LastName:    newUser.LastName.String,
				PhoneNumber: "",
				Timestamp:   time.Now().Unix(),
				EventType:   "user.registered",
				ClinicData:  clinicData,
			}

			if err := s.RabbitPublisher.PublishUserRegistrationEvent(pbEvent); err != nil {
				fmt.Printf("⚠️  Warning: Failed to publish to RabbitMQ: %v. Falling back to HTTP.\n", err)
				// Fall back to HTTP if RabbitMQ fails
				if s.ProfileClient != nil {
					if err := s.ProfileClient.NotifyUserRegistered(context.Background(), event); err != nil {
						fmt.Printf("Warning: Failed to notify profile service of user registration: %v\n", err)
					}
				}
				if s.AppointmentsClient != nil {
					if err := s.AppointmentsClient.NotifyUserRegistered(context.Background(), event); err != nil {
						fmt.Printf("Warning: Failed to notify appointments service of user registration: %v\n", err)
					}
				}
			} else {
				fmt.Printf("✓ Published user registration event to RabbitMQ: %s (role: %s, email: %s)\n", newUser.ID, newUser.Role, newUser.Email)
			}
		}()
	} else {
		// RabbitMQ not available, use HTTP fallback
		// Send notification to profile service asynchronously
		if s.ProfileClient != nil {
			go func() {
				if err := s.ProfileClient.NotifyUserRegistered(context.Background(), event); err != nil {
					// Log error but don't fail the registration
					fmt.Printf("Warning: Failed to notify profile service of user registration: %v\n", err)
				}
			}()
		}

		// Send notification to appointments service asynchronously
		if s.AppointmentsClient != nil {
			go func() {
				if err := s.AppointmentsClient.NotifyUserRegistered(context.Background(), event); err != nil {
					// Log error but don't fail the registration
					fmt.Printf("Warning: Failed to notify appointments service of user registration: %v\n", err)
				}
			}()
		}
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

	// 3. Check email verification status (block login until verified for all roles)
	if !user.IsVerified {
		return &user, "", nil
	}

	// 4. Generate Token
	token, err := auth.GenerateJWT(user.ID, string(user.Role))
	if err != nil {
		return nil, "", err
	}

	return &user, token, nil
}

// ForgotPassword generates a password reset token and sends it via email
func (s *UserService) ForgotPassword(ctx context.Context, email string) error {
	// Check if user exists
	var userID int
	var userEmail string
	err := s.DB.QueryRowContext(ctx, "SELECT id, email FROM users WHERE email = $1", email).Scan(&userID, &userEmail)

	// For security, always return success even if email doesn't exist
	if err == sql.ErrNoRows {
		return nil // Don't reveal if email exists or not
	}
	if err != nil {
		return err
	}

	// Generate password reset token
	resetToken, err := generateRandomToken(32)
	if err != nil {
		return err
	}

	// Save token to database (expires in 1 hour)
	if err := s.savePasswordResetToken(ctx, userID, resetToken, 60); err != nil {
		return err
	}

	// Send password reset email
	baseURL := os.Getenv("FRONTEND_BASE_URL")
	if baseURL == "" {
		baseURL = "http://localhost:3000" // Default frontend URL
	}
	resetURL := strings.TrimRight(baseURL, "/") + "/reset-password?token=" + resetToken

	if err := s.sendPasswordResetEmail(userEmail, resetURL); err != nil {
		return err
	}

	return nil
}

// ResetPassword validates the token and updates the user's password
func (s *UserService) ResetPassword(ctx context.Context, token, newPassword string) error {
	// Validate and get user ID from token
	userID, err := s.validatePasswordResetToken(ctx, token)
	if err != nil {
		return err
	}

	// Hash the new password
	hashedPassword, err := auth.HashPassword(newPassword)
	if err != nil {
		return err
	}

	// Update password and mark token as used in a transaction
	tx, err := s.DB.BeginTx(ctx, nil)
	if err != nil {
		return err
	}
	defer func() {
		if err != nil {
			_ = tx.Rollback()
		}
	}()

	// Update user password
	if _, err = tx.ExecContext(ctx, `UPDATE users SET password_hash=$1 WHERE id=$2`, hashedPassword, userID); err != nil {
		return err
	}

	// Mark token as used
	if _, err = tx.ExecContext(ctx, `UPDATE password_reset_tokens SET used_at=$1 WHERE token=$2`, time.Now().Unix(), token); err != nil {
		return err
	}

	if err = tx.Commit(); err != nil {
		return err
	}

	return nil
}

// savePasswordResetToken stores a password reset token in the database
func (s *UserService) savePasswordResetToken(ctx context.Context, userID int, token string, ttlMinutes int) error {
	// First, invalidate any existing tokens for this user
	_, err := s.DB.ExecContext(ctx, `UPDATE password_reset_tokens SET used_at=$1 WHERE user_id=$2 AND used_at IS NULL`, time.Now().Unix(), userID)
	if err != nil {
		return err
	}

	// Insert new token
	expiresAt := time.Now().Add(time.Duration(ttlMinutes) * time.Minute).Unix()
	createdAt := time.Now().Unix()
	_, err = s.DB.ExecContext(ctx,
		`INSERT INTO password_reset_tokens(token, user_id, expires_at, created_at) VALUES($1,$2,$3,$4)`,
		token, userID, expiresAt, createdAt)
	return err
}

// validatePasswordResetToken checks if token is valid and returns user ID
func (s *UserService) validatePasswordResetToken(ctx context.Context, token string) (int, error) {
	var userID int
	var expiresAt int64
	var usedAt sql.NullInt64

	err := s.DB.QueryRowContext(ctx,
		`SELECT user_id, expires_at, used_at FROM password_reset_tokens WHERE token=$1`,
		token).Scan(&userID, &expiresAt, &usedAt)

	if err == sql.ErrNoRows {
		return 0, errors.New("invalid or expired token")
	}
	if err != nil {
		return 0, err
	}

	if usedAt.Valid {
		return 0, errors.New("token already used")
	}

	if time.Now().Unix() > expiresAt {
		return 0, errors.New("token expired")
	}

	return userID, nil
}

// sendPasswordResetEmail sends a password reset email to the user
func (s *UserService) sendPasswordResetEmail(toEmail, resetURL string) error {
	from := os.Getenv("GMAIL_SMTP_EMAIL")
	appPass := os.Getenv("GMAIL_SMTP_APP_PASSWORD")
	fromName := os.Getenv("EMAIL_FROM_NAME")

	if from == "" || appPass == "" {
		return errors.New("smtp credentials not configured")
	}

	host := "smtp.gmail.com"
	addr := host + ":587"
	auth := smtp.PlainAuth("", from, appPass, host)

	if fromName == "" {
		fromName = "PulseOne"
	}

	subject := "Reset Your Password"
	body := fmt.Sprintf(`Hello,

You have requested to reset your password. Please click the link below to reset your password:

%s

This link will expire in 60 minutes for security reasons.

If you did not request this password reset, please ignore this email.

Thanks,
%s Team
`, resetURL, fromName)

	// Build RFC 5322 message
	headers := []string{
		"From: " + fromName + " <" + from + ">",
		"To: " + toEmail,
		"Subject: " + subject,
		"MIME-Version: 1.0",
		"Content-Type: text/plain; charset=utf-8",
	}
	msg := strings.Join(headers, "\r\n") + "\r\n\r\n" + body

	return smtp.SendMail(addr, auth, from, []string{toEmail}, []byte(msg))
}
