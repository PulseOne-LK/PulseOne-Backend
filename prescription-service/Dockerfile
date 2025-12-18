# ----------------------------------------------------
# STAGE 1: THE BUILDER
# Goal: Compile the Go source code into a static binary.
# We use a full Go image (golang:1.24-alpine) for the compiler tools.
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

# Build the application
RUN CGO_ENABLED=1 GOOS=linux go build -o main ./cmd/main.go

# Verify the binary was created
RUN ls -la /app/main

# -------------------------------------------------------
# STAGE 2: THE RUNTIME
# Goal: Run the compiled binary in a minimal runtime image.
# We use Alpine Linux as the base for a smaller final image.
# -------------------------------------------------------
FROM alpine:latest

# Install PostgreSQL client (optional but useful for debugging)
RUN apk add --no-cache postgresql-client ca-certificates

WORKDIR /app

# Copy the compiled binary from the builder stage
COPY --from=builder /app/main .

# Copy the .env file if it exists (optional)
COPY .env* ./

# Expose the port (change as needed)
EXPOSE 8085

# Run the application
CMD ["./main"]
