FROM golang:1.25-alpine AS builder
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN go build -o /api-gateway

FROM alpine:latest
WORKDIR /root/
COPY --from=builder /api-gateway .
EXPOSE 8081
CMD ["./api-gateway"]