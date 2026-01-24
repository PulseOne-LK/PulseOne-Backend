package com.pulseone.appointments_service.service;

import com.pulseone.appointments_service.dto.request.AvailabilitySearchRequest;
import com.pulseone.appointments_service.dto.response.DoctorAvailabilityResponse;
import com.pulseone.appointments_service.dto.response.DoctorCalendarResponse;
import com.pulseone.appointments_service.entity.Doctor;
import com.pulseone.appointments_service.entity.Session;
import com.pulseone.appointments_service.entity.SessionOverride;
import com.pulseone.appointments_service.repository.AppointmentRepository;
import com.pulseone.appointments_service.repository.DoctorRepository;
import com.pulseone.appointments_service.repository.SessionOverrideRepository;
import com.pulseone.appointments_service.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for handling doctor availability searches and calendar generation.
 */
@Service
@Transactional(readOnly = true)
public class AvailabilityService {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private SessionOverrideRepository sessionOverrideRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    /**
     * Search for available doctors based on criteria
     */
    public List<DoctorAvailabilityResponse> searchAvailableDoctors(AvailabilitySearchRequest request) {
        List<Doctor> doctors;
        
        // Filter doctors based on criteria
        if (request.getDoctorUserId() != null) {
            // Search for specific doctor
            Optional<Doctor> doctor = doctorRepository.findByUserId(request.getDoctorUserId());
            doctors = doctor.map(List::of).orElse(new ArrayList<>());
        } else if (request.getSpecialization() != null && !request.getSpecialization().isEmpty()) {
            // Search by specialization
            doctors = doctorRepository.findBySpecializationAndIsActiveTrue(request.getSpecialization());
        } else {
            // Get all active doctors
            doctors = doctorRepository.findByIsActiveTrue();
        }

        // Convert to availability responses
        List<DoctorAvailabilityResponse> availabilityResponses = new ArrayList<>();
        
        for (Doctor doctor : doctors) {
            DoctorAvailabilityResponse response = new DoctorAvailabilityResponse();
            response.setDoctorUserId(doctor.getUserId());
            response.setDoctorName(doctor.getName());
            response.setSpecialization(doctor.getSpecialization());
            
            // Get available slots for this doctor
            List<DoctorAvailabilityResponse.AvailableSlot> availableSlots = 
                getAvailableSlotsForDoctor(doctor, request);
            
            if (!availableSlots.isEmpty()) {
                response.setAvailableSlots(availableSlots);
                availabilityResponses.add(response);
            }
        }

        return availabilityResponses;
    }

    /**
     * Get doctor's calendar for the next 30 days
     */
    public DoctorCalendarResponse getDoctorCalendar(String doctorUserId) {
        Doctor doctor = doctorRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found with userId: " + doctorUserId));

        DoctorCalendarResponse response = new DoctorCalendarResponse();
        response.setDoctorUserId(doctor.getUserId());
        response.setDoctorName(doctor.getName());
        response.setSpecialization(doctor.getSpecialization());

        // Generate calendar for next 30 days
        List<DoctorCalendarResponse.CalendarDay> calendar = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();
        
        for (int i = 0; i < 30; i++) {
            LocalDate date = currentDate.plusDays(i);
            DoctorCalendarResponse.CalendarDay calendarDay = generateCalendarDay(doctor, date);
            calendar.add(calendarDay);
        }
        
        response.setCalendar(calendar);
        return response;
    }

    /**
     * Get available slots for a specific doctor based on search criteria
     */
    private List<DoctorAvailabilityResponse.AvailableSlot> getAvailableSlotsForDoctor(
            Doctor doctor, AvailabilitySearchRequest request) {
        
        List<DoctorAvailabilityResponse.AvailableSlot> availableSlots = new ArrayList<>();
        
        // Get doctor's sessions
        List<Session> sessions = sessionRepository.findByDoctorAndIsActiveTrueOrderByDayOfWeekAscSessionStartTimeAsc(doctor);
        
        // Filter sessions based on request criteria
        sessions = sessions.stream()
                .filter(session -> matchesServiceType(session, request.getServiceType()))
                .filter(session -> matchesClinic(session, request.getClinicId()))
                .collect(Collectors.toList());

        // Determine date range to check
        List<LocalDate> datesToCheck = getDatesToCheck(request.getDate());
        
        for (LocalDate date : datesToCheck) {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            
            // Find sessions for this day of week
            List<Session> daySessions = sessions.stream()
                    .filter(session -> session.getDayOfWeek().equals(dayOfWeek))
                    .filter(session -> isSessionEffectiveOnDate(session, date))
                    .collect(Collectors.toList());
            
            for (Session session : daySessions) {
                // Check for overrides
                Optional<SessionOverride> override = sessionOverrideRepository
                        .findBySessionAndOverrideDate(session, date);
                
                if (override.isPresent() && override.get().getIsCancelled()) {
                    continue; // Skip cancelled sessions
                }
                
                // Calculate available slots
                Long bookedCount = appointmentRepository.countActiveAppointmentsForSessionAndDate(session, date);
                Integer maxSlots = override.map(SessionOverride::getOverrideMaxQueueSize)
                        .orElse(session.getMaxQueueSize());
                
                int availableCount = maxSlots - bookedCount.intValue();
                
                if (availableCount > 0) {
                    DoctorAvailabilityResponse.AvailableSlot slot = new DoctorAvailabilityResponse.AvailableSlot();
                    slot.setSessionId(session.getId());
                    slot.setDate(date);
                    slot.setStartTime(override.map(SessionOverride::getOverrideStartTime)
                            .orElse(session.getSessionStartTime()));
                    slot.setEndTime(override.map(SessionOverride::getOverrideEndTime)
                            .orElse(session.getSessionEndTime()));
                    slot.setServiceType(session.getServiceType());
                    slot.setAvailableSlots(availableCount);
                    slot.setTotalSlots(maxSlots);
                    
                    if (session.getClinic() != null) {
                        slot.setClinicName(session.getClinic().getName());
                        slot.setClinicAddress(session.getClinic().getAddress());
                    }
                    
                    availableSlots.add(slot);
                }
            }
        }
        
        return availableSlots;
    }

    /**
     * Generate calendar day information for a specific doctor and date
     */
    private DoctorCalendarResponse.CalendarDay generateCalendarDay(Doctor doctor, LocalDate date) {
        DoctorCalendarResponse.CalendarDay calendarDay = new DoctorCalendarResponse.CalendarDay();
        calendarDay.setDate(date);
        calendarDay.setDayOfWeek(date.getDayOfWeek().toString());
        
        // Get sessions for this day
        List<Session> daySessions = sessionRepository.findByDoctorAndDayOfWeekAndIsActiveTrueOrderBySessionStartTimeAsc(
                doctor, date.getDayOfWeek());
        
        // Filter sessions that are effective on this date
        daySessions = daySessions.stream()
                .filter(session -> isSessionEffectiveOnDate(session, date))
                .collect(Collectors.toList());
        
        if (daySessions.isEmpty()) {
            calendarDay.setIsAvailable(false);
            calendarDay.setUnavailableReason("No sessions scheduled");
            calendarDay.setSessions(new ArrayList<>());
            return calendarDay;
        }
        
        List<DoctorCalendarResponse.SessionSlot> sessionSlots = new ArrayList<>();
        boolean hasAvailableSlots = false;
        
        for (Session session : daySessions) {
            // Check for overrides
            Optional<SessionOverride> override = sessionOverrideRepository
                    .findBySessionAndOverrideDate(session, date);
            
            if (override.isPresent() && override.get().getIsCancelled()) {
                continue; // Skip cancelled sessions
            }
            
            // Calculate availability
            Long bookedCount = appointmentRepository.countActiveAppointmentsForSessionAndDate(session, date);
            Integer maxSlots = override.map(SessionOverride::getOverrideMaxQueueSize)
                    .orElse(session.getMaxQueueSize());
            
            int availableCount = maxSlots - bookedCount.intValue();
            
            DoctorCalendarResponse.SessionSlot sessionSlot = new DoctorCalendarResponse.SessionSlot();
            sessionSlot.setSessionId(session.getId());
            sessionSlot.setStartTime(override.map(o -> o.getOverrideStartTime().toString())
                    .orElse(session.getSessionStartTime().toString()));
            sessionSlot.setEndTime(override.map(o -> o.getOverrideEndTime().toString())
                    .orElse(session.getSessionEndTime().toString()));
            sessionSlot.setServiceType(session.getServiceType().toString());
            sessionSlot.setAvailableSlots(availableCount);
            sessionSlot.setTotalSlots(maxSlots);
            
            if (session.getClinic() != null) {
                sessionSlot.setClinicName(session.getClinic().getName());
            }
            
            sessionSlots.add(sessionSlot);
            
            if (availableCount > 0) {
                hasAvailableSlots = true;
            }
        }
        
        calendarDay.setSessions(sessionSlots);
        calendarDay.setIsAvailable(hasAvailableSlots);
        
        if (!hasAvailableSlots && !sessionSlots.isEmpty()) {
            calendarDay.setUnavailableReason("Fully booked");
        }
        
        return calendarDay;
    }

    /**
     * Check if session matches the requested service type.
     * In the dual-mode concept, sessions are strictly either VIRTUAL or IN_PERSON.
     * 
     * @param session The session to check
     * @param serviceType The requested service type (null means no filter)
     * @return true if session matches or no filter applied
     */
    private boolean matchesServiceType(Session session, com.pulseone.appointments_service.enums.ServiceType serviceType) {
        if (serviceType == null) {
            return true; // No filter, match all
        }
        
        // Strict matching - no BOTH option anymore in dual-mode concept
        return session.getServiceType().equals(serviceType);
    }

    /**
     * Helper method to check if session matches clinic filter
     */
    private boolean matchesClinic(Session session, Long clinicId) {
        if (clinicId == null) {
            return true;
        }
        
        return session.getClinic() != null && session.getClinic().getProfileClinicId().equals(clinicId);
    }

    /**
     * Helper method to check if session is effective on a specific date
     */
    private boolean isSessionEffectiveOnDate(Session session, LocalDate date) {
        return !date.isBefore(session.getEffectiveFrom()) &&
               (session.getEffectiveUntil() == null || !date.isAfter(session.getEffectiveUntil()));
    }

    /**
     * Helper method to get dates to check based on search request
     */
    private List<LocalDate> getDatesToCheck(LocalDate requestDate) {
        List<LocalDate> dates = new ArrayList<>();
        
        if (requestDate != null) {
            // Search for specific date
            dates.add(requestDate);
        } else {
            // Search for next 7 days
            LocalDate currentDate = LocalDate.now();
            for (int i = 0; i < 7; i++) {
                dates.add(currentDate.plusDays(i));
            }
        }
        
        return dates;
    }
}