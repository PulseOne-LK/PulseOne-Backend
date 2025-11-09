# ----------------------------------------------------
# STAGE 1: THE BUILDER
# Goal: Compile the Go source code into a static binary.
# We use a full Go image (golang:1.23-alpine) for the compiler tools.
# ----------------------------------------------------
FROM golang:1.24-alpine AS builder

# Install C dependencies for building. The PostgreSQL driver (github.com/lib/pq)
# requires CGO, so we need gcc and other tools for static linking.
RUN apk add --no-cache git gcc libc-dev

WORKDIR /app

# Copy go.mod and go.sum to cache dependencies
COPY go.mod go.sum ./
RUN go mod download

# Copy all source code
COPY . .

# Build the final binary. 
# -ldflags "-s -w" strips debugging symbols, making the binary smaller.
# The output binary is named 'go-auth-service' and placed in the root of the app directory.
RUN go build -ldflags "-s -w" -o /go-auth-service ./cmd/main.go


# ----------------------------------------------------
# STAGE 2: THE RUNNER (Deployment Image)
# Goal: Create the smallest possible production image.
# We use 'alpine:latest' as a tiny base (~5MB) that includes only necessary certificates.
# ----------------------------------------------------
FROM alpine:latest

# Install CA certificates required for HTTPS/SSL connections (e.g., if you talk to
# other services over HTTPS, or connect to a remote PostgreSQL with SSL enabled).
RUN apk --no-cache add ca-certificates

WORKDIR /root/

# Copy the static binary from the builder stage (/go-auth-service)
COPY --from=builder /go-auth-service .

# Set the environment variable for PostgreSQL DSN 
# IMPORTANT: This placeholder is overwritten by docker-compose.yml
ENV POSTGRES_DSN="user=postgres password=root2004 host=postgres-db port=5432 dbname=authdb sslmode=disable"
ENV PORT="8081"

# Expose the service port
EXPOSE 8080

# Command to run the service
CMD ["./go-auth-service"]