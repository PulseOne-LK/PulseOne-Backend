package com.pulseone.appointments_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for successful appointment booking
 */
public class BookingResponse {

    private UUID appointmentId;
    private Integer queueNumber;
    private LocalDateTime estimatedStartTime;
    private Integer estimatedWaitTimeMinutes;
    private BigDecimal consultationFee;
    private String message;

    // Constructors
    public BookingResponse() {
    }

    public BookingResponse(UUID appointmentId, Integer queueNumber, LocalDateTime estimatedStartTime, 
                          Integer estimatedWaitTimeMinutes, BigDecimal consultationFee, String message) {
        this.appointmentId = appointmentId;
        this.queueNumber = queueNumber;
        this.estimatedStartTime = estimatedStartTime;
        this.estimatedWaitTimeMinutes = estimatedWaitTimeMinutes;
        this.consultationFee = consultationFee;
        this.message = message;
    }

    // Getters and Setters
    public UUID getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(UUID appointmentId) {
        this.appointmentId = appointmentId;
    }

    public Integer getQueueNumber() {
        return queueNumber;
    }

    public void setQueueNumber(Integer queueNumber) {
        this.queueNumber = queueNumber;
    }

    public LocalDateTime getEstimatedStartTime() {
        return estimatedStartTime;
    }

    public void setEstimatedStartTime(LocalDateTime estimatedStartTime) {
        this.estimatedStartTime = estimatedStartTime;
    }

    public Integer getEstimatedWaitTimeMinutes() {
        return estimatedWaitTimeMinutes;
    }

    public void setEstimatedWaitTimeMinutes(Integer estimatedWaitTimeMinutes) {
        this.estimatedWaitTimeMinutes = estimatedWaitTimeMinutes;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(BigDecimal consultationFee) {
        this.consultationFee = consultationFee;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}