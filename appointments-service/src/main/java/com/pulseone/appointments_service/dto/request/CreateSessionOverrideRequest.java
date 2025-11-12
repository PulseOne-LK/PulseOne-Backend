package com.pulseone.appointments_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request DTO for creating a session override
 */
public class CreateSessionOverrideRequest {

    @NotNull(message = "Session ID is required")
    private Long sessionId;

    @NotNull(message = "Override date is required")
    private LocalDate overrideDate;

    @NotNull(message = "Is cancelled flag is required")
    private Boolean isCancelled;

    private LocalTime overrideStartTime; // Optional

    private LocalTime overrideEndTime; // Optional

    private Integer overrideMaxQueueSize; // Optional

    @NotBlank(message = "Reason is required")
    private String reason;

    // Constructors
    public CreateSessionOverrideRequest() {
    }

    // Getters and Setters
    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public LocalDate getOverrideDate() {
        return overrideDate;
    }

    public void setOverrideDate(LocalDate overrideDate) {
        this.overrideDate = overrideDate;
    }

    public Boolean getIsCancelled() {
        return isCancelled;
    }

    public void setIsCancelled(Boolean isCancelled) {
        this.isCancelled = isCancelled;
    }

    public LocalTime getOverrideStartTime() {
        return overrideStartTime;
    }

    public void setOverrideStartTime(LocalTime overrideStartTime) {
        this.overrideStartTime = overrideStartTime;
    }

    public LocalTime getOverrideEndTime() {
        return overrideEndTime;
    }

    public void setOverrideEndTime(LocalTime overrideEndTime) {
        this.overrideEndTime = overrideEndTime;
    }

    public Integer getOverrideMaxQueueSize() {
        return overrideMaxQueueSize;
    }

    public void setOverrideMaxQueueSize(Integer overrideMaxQueueSize) {
        this.overrideMaxQueueSize = overrideMaxQueueSize;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}