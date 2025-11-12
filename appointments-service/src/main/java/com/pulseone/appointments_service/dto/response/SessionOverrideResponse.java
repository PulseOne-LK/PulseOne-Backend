package com.pulseone.appointments_service.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Response DTO for session override information
 */
public class SessionOverrideResponse {

    private Long id;
    private Long sessionId;
    private LocalDate overrideDate;
    private Boolean isCancelled;
    private LocalTime overrideStartTime;
    private LocalTime overrideEndTime;
    private Integer overrideMaxQueueSize;
    private String reason;

    // Constructors
    public SessionOverrideResponse() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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