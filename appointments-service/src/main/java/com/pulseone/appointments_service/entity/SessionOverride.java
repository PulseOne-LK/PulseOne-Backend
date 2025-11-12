package com.pulseone.appointments_service.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represents exceptions to regular session schedules.
 * Used for holidays, special hours, or temporary changes to doctor availability.
 */
@Entity
@Table(name = "session_overrides")
public class SessionOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the session that this override affects
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    /**
     * Specific date for which this override applies
     */
    @Column(name = "override_date", nullable = false)
    private LocalDate overrideDate;

    /**
     * Whether the session is cancelled on this date
     * If true, the session will not be available on this date
     */
    @Column(name = "is_cancelled", nullable = false)
    private Boolean isCancelled = Boolean.FALSE;

    /**
     * Override start time for this specific date
     * If null and not cancelled, uses the regular session start time
     */
    @Column(name = "override_start_time")
    private LocalTime overrideStartTime;

    /**
     * Override end time for this specific date
     * If null and not cancelled, uses the regular session end time
     */
    @Column(name = "override_end_time")
    private LocalTime overrideEndTime;

    /**
     * Override max queue size for this specific date
     * If null and not cancelled, uses the regular session max queue size
     */
    @Column(name = "override_max_queue_size")
    private Integer overrideMaxQueueSize;

    /**
     * Reason for the override (e.g., "Holiday", "Emergency", "Extended Hours")
     */
    @Column(name = "reason")
    private String reason;

    // Constructors
    public SessionOverride() {
    }

    public SessionOverride(Session session, LocalDate overrideDate, Boolean isCancelled, String reason) {
        this.session = session;
        this.overrideDate = overrideDate;
        this.isCancelled = isCancelled;
        this.reason = reason;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
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