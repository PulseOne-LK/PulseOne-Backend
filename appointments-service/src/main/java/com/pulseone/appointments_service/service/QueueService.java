package com.pulseone.appointments_service.service;

import com.pulseone.appointments_service.dto.request.CheckInRequest;
import com.pulseone.appointments_service.dto.request.CallNextPatientRequest;
import com.pulseone.appointments_service.dto.response.QueueStatusResponse;
import com.pulseone.appointments_service.dto.response.DoctorQueueResponse;
import com.pulseone.appointments_service.entity.Appointment;
import com.pulseone.appointments_service.entity.WaitingRoom;
import com.pulseone.appointments_service.entity.Doctor;
import com.pulseone.appointments_service.entity.Session;
import com.pulseone.appointments_service.enums.AppointmentStatus;
import com.pulseone.appointments_service.repository.AppointmentRepository;
import com.pulseone.appointments_service.repository.WaitingRoomRepository;
import com.pulseone.appointments_service.repository.DoctorRepository;
import com.pulseone.appointments_service.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for queue management operations
 * Handles patient check-in, calling patients, and queue status tracking
 */
@Service
@Transactional
public class QueueService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private WaitingRoomRepository waitingRoomRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private SessionRepository sessionRepository;

    /**
     * Check in a patient for their appointment
     */
    public QueueStatusResponse checkInPatient(CheckInRequest request) {
        // Find the appointment
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new RuntimeException("Appointment not found: " + request.getAppointmentId()));

        // Validate appointment status
        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new RuntimeException("Patient can only check in for BOOKED appointments. Current status: " + appointment.getStatus());
        }

        // Validate appointment date
        if (!appointment.getAppointmentDate().equals(LocalDate.now())) {
            throw new RuntimeException("Patient can only check in on the appointment date");
        }

        // Update appointment status
        appointment.setStatus(AppointmentStatus.CHECKED_IN);
        appointmentRepository.save(appointment);

        // Create waiting room entry
        WaitingRoom waitingRoom = new WaitingRoom(appointment);
        waitingRoomRepository.save(waitingRoom);

        // Calculate queue position and wait time
        return buildQueueStatusResponse(appointment, waitingRoom);
    }

    /**
     * Call the next patient for consultation
     */
    public QueueStatusResponse callNextPatient(CallNextPatientRequest request) {
        // Find the appointment
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new RuntimeException("Appointment not found: " + request.getAppointmentId()));

        // Validate appointment status
        if (appointment.getStatus() != AppointmentStatus.CHECKED_IN) {
            throw new RuntimeException("Only checked-in patients can be called. Current status: " + appointment.getStatus());
        }

        // Find waiting room entry
        WaitingRoom waitingRoom = waitingRoomRepository.findByAppointment_AppointmentId(request.getAppointmentId())
                .orElseThrow(() -> new RuntimeException("Patient not found in waiting room"));

        // Update appointment status
        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        appointmentRepository.save(appointment);

        // Update waiting room entry
        waitingRoom.callPatient(request.getCalledBy());
        waitingRoomRepository.save(waitingRoom);

        return buildQueueStatusResponse(appointment, waitingRoom);
    }

    /**
     * Mark consultation as started
     */
    public QueueStatusResponse startConsultation(UUID appointmentId) {
        // Find the appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found: " + appointmentId));

        // Validate appointment status
        if (appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
            throw new RuntimeException("Consultation can only be started for IN_PROGRESS appointments. Current status: " + appointment.getStatus());
        }

        // Find waiting room entry
        WaitingRoom waitingRoom = waitingRoomRepository.findByAppointment_AppointmentId(appointmentId)
                .orElseThrow(() -> new RuntimeException("Patient not found in waiting room"));

        // Start consultation
        waitingRoom.startConsultation();
        waitingRoomRepository.save(waitingRoom);

        // Update appointment times
        appointment.setActualStartTime(LocalDateTime.now());
        appointmentRepository.save(appointment);

        return buildQueueStatusResponse(appointment, waitingRoom);
    }

    /**
     * Complete consultation and remove from waiting room
     */
    public QueueStatusResponse completeConsultation(UUID appointmentId) {
        // Find the appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found: " + appointmentId));

        // Validate appointment status
        if (appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
            throw new RuntimeException("Only IN_PROGRESS appointments can be completed. Current status: " + appointment.getStatus());
        }

        // Update appointment status and end time
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setActualEndTime(LocalDateTime.now());
        appointmentRepository.save(appointment);

        // Remove from waiting room (consultation is done)
        waitingRoomRepository.deleteByAppointment_AppointmentId(appointmentId);

        return buildQueueStatusResponse(appointment, null);
    }

    /**
     * Mark patient as no-show
     */
    public QueueStatusResponse markNoShow(UUID appointmentId) {
        // Find the appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found: " + appointmentId));

        // Validate appointment status (can mark no-show for BOOKED or CHECKED_IN)
        if (appointment.getStatus() != AppointmentStatus.BOOKED && 
            appointment.getStatus() != AppointmentStatus.CHECKED_IN) {
            throw new RuntimeException("Can only mark BOOKED or CHECKED_IN appointments as no-show. Current status: " + appointment.getStatus());
        }

        // Update appointment status
        appointment.setStatus(AppointmentStatus.NO_SHOW);
        appointmentRepository.save(appointment);

        // Remove from waiting room if they were checked in
        waitingRoomRepository.deleteByAppointment_AppointmentId(appointmentId);

        return buildQueueStatusResponse(appointment, null);
    }

    /**
     * Get doctor's queue for today
     */
    public DoctorQueueResponse getDoctorQueueToday(String doctorId) {
        return getDoctorQueue(doctorId, LocalDate.now());
    }

    /**
     * Get doctor's queue for a specific date
     */
    public DoctorQueueResponse getDoctorQueue(String doctorId, LocalDate date) {
        // Get doctor information
        Doctor doctor = doctorRepository.findByUserId(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found: " + doctorId));

        // Get all appointments for the doctor on the specified date
        List<Appointment> appointments = appointmentRepository.findByDoctorIdAndAppointmentDateOrderByQueueNumberAsc(doctorId, date);

        // Build queue statistics
        DoctorQueueResponse response = new DoctorQueueResponse(doctorId, doctor.getName(), date);
        response.setSpecialization(doctor.getSpecialization());
        response.setTotalAppointments(appointments.size());

        // Calculate status counts
        long pendingCheckIn = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.BOOKED).count();
        long waitingPatients = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CHECKED_IN).count();
        long inConsultation = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.IN_PROGRESS).count();
        long completed = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();
        long noShows = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.NO_SHOW).count();
        long cancelled = appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count();

        response.setPendingCheckIn((int) pendingCheckIn);
        response.setWaitingPatients((int) waitingPatients);
        response.setInConsultation((int) inConsultation);
        response.setCompleted((int) completed);
        response.setNoShows((int) noShows);
        response.setCancelled((int) cancelled);

        // Get current and next queue numbers
        Optional<Integer> currentQueueNumber = waitingRoomRepository.getCurrentQueueNumberBeingServed(doctorId, date);
        response.setCurrentQueueNumber(currentQueueNumber.orElse(0));

        Optional<WaitingRoom> nextPatient = waitingRoomRepository.findNextPatientToCall(doctorId, date);
        response.setNextQueueNumber(nextPatient.map(wr -> wr.getAppointment().getQueueNumber()).orElse(null));

        // Get average wait time
        Optional<Double> avgWaitTime = waitingRoomRepository.getAverageWaitTimeMinutes(doctorId, date);
        response.setAverageWaitMinutes(avgWaitTime.orElse(0.0));

        // Build detailed queue list
        List<QueueStatusResponse> queueList = appointments.stream()
                .map(this::buildQueueStatusResponseFromAppointment)
                .collect(Collectors.toList());
        response.setQueueList(queueList);

        return response;
    }

    /**
     * Get current queue status for a doctor
     */
    public List<QueueStatusResponse> getCurrentQueueStatus(String doctorId) {
        LocalDate today = LocalDate.now();
        List<WaitingRoom> waitingPatients = waitingRoomRepository.findCurrentWaitingPatients(doctorId, today);
        
        return waitingPatients.stream()
                .map(wr -> buildQueueStatusResponse(wr.getAppointment(), wr))
                .collect(Collectors.toList());
    }

    /**
     * Get patient's position in queue
     */
    public QueueStatusResponse getPatientQueuePosition(UUID appointmentId) {
        // Find the appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found: " + appointmentId));

        // Find waiting room entry (if exists)
        Optional<WaitingRoom> waitingRoom = waitingRoomRepository.findByAppointment_AppointmentId(appointmentId);

        return buildQueueStatusResponse(appointment, waitingRoom.orElse(null));
    }

    /**
     * Get next patient to be called
     */
    public Optional<QueueStatusResponse> getNextPatientToCall(String doctorId) {
        LocalDate today = LocalDate.now();
        Optional<WaitingRoom> nextPatient = waitingRoomRepository.findNextPatientToCall(doctorId, today);
        
        if (nextPatient.isPresent()) {
            return Optional.of(buildQueueStatusResponse(nextPatient.get().getAppointment(), nextPatient.get()));
        }
        return Optional.empty();
    }

    /**
     * Get patients waiting longer than expected
     */
    public List<QueueStatusResponse> getPatientsWaitingTooLong(int thresholdMinutes) {
        LocalDateTime thresholdTime = LocalDateTime.now().minusMinutes(thresholdMinutes);
        List<WaitingRoom> longWaitingPatients = waitingRoomRepository.findPatientsWaitingLongerThan(thresholdTime);
        
        return longWaitingPatients.stream()
                .map(wr -> buildQueueStatusResponse(wr.getAppointment(), wr))
                .collect(Collectors.toList());
    }

    // Helper Methods

    /**
     * Build QueueStatusResponse from appointment and waiting room data
     */
    private QueueStatusResponse buildQueueStatusResponse(Appointment appointment, WaitingRoom waitingRoom) {
        QueueStatusResponse response = new QueueStatusResponse();
        
        // Basic appointment information
        response.setAppointmentId(appointment.getAppointmentId());
        response.setPatientId(appointment.getPatientId());
        response.setDoctorId(appointment.getDoctorId());
        response.setAppointmentDate(appointment.getAppointmentDate());
        response.setQueueNumber(appointment.getQueueNumber());
        response.setStatus(appointment.getStatus());
        response.setChiefComplaint(appointment.getChiefComplaint());

        // Waiting room information
        if (waitingRoom != null) {
            response.setCheckedInAt(waitingRoom.getCheckedInAt());
            response.setCalledAt(waitingRoom.getCalledAt());
            response.setCalledBy(waitingRoom.getCalledBy());
            response.setConsultationStartedAt(waitingRoom.getConsultationStartedAt());
            response.setActualWaitMinutes(waitingRoom.getWaitTimeMinutes());
        }

        // Calculate queue position information
        calculateQueuePosition(response, appointment);

        // Get doctor and session information
        populateDoctorAndSessionInfo(response, appointment);

        return response;
    }

    /**
     * Build QueueStatusResponse from appointment only
     */
    private QueueStatusResponse buildQueueStatusResponseFromAppointment(Appointment appointment) {
        Optional<WaitingRoom> waitingRoom = waitingRoomRepository.findByAppointment_AppointmentId(appointment.getAppointmentId());
        return buildQueueStatusResponse(appointment, waitingRoom.orElse(null));
    }

    /**
     * Calculate queue position and wait time estimates
     */
    private void calculateQueuePosition(QueueStatusResponse response, Appointment appointment) {
        String doctorId = appointment.getDoctorId();
        LocalDate date = appointment.getAppointmentDate();
        Integer queueNumber = appointment.getQueueNumber();

        // Get current queue number being served
        Optional<Integer> currentQueueNumber = waitingRoomRepository.getCurrentQueueNumberBeingServed(doctorId, date);
        response.setCurrentQueueNumber(currentQueueNumber.orElse(0));

        // Count patients ahead
        Long patientsAhead = waitingRoomRepository.countPatientsAhead(doctorId, date, queueNumber);
        response.setPatientsAhead(patientsAhead.intValue());

        // Estimate wait time
        Session session = sessionRepository.findByDoctorIdAndDayOfWeek(doctorId, date.getDayOfWeek()).stream().findFirst().orElse(null);
        if (session != null) {
            response.setEstimatedConsultationMinutes(session.getEstimatedConsultationMinutes());
            int estimatedWaitMinutes = patientsAhead.intValue() * session.getEstimatedConsultationMinutes();
            response.setEstimatedWaitMinutes(estimatedWaitMinutes);
        }
    }

    /**
     * Populate doctor and session information
     */
    private void populateDoctorAndSessionInfo(QueueStatusResponse response, Appointment appointment) {
        // Get doctor information
        Optional<Doctor> doctor = doctorRepository.findByUserId(appointment.getDoctorId());
        if (doctor.isPresent()) {
            response.setDoctorName(doctor.get().getName());
            if (appointment.getClinicId() != null) {
                response.setClinicName(doctor.get().getClinic() != null ? doctor.get().getClinic().getName() : null);
            }
        }

        // Get session information
        Optional<Session> session = sessionRepository.findById(appointment.getSessionId());
        if (session.isPresent()) {
            Session s = session.get();
            response.setEstimatedConsultationMinutes(s.getEstimatedConsultationMinutes());
            // Convert session time to LocalDateTime for today
            LocalDateTime sessionStart = appointment.getAppointmentDate().atTime(s.getSessionStartTime());
            response.setSessionStartTime(sessionStart);
        }
    }
}