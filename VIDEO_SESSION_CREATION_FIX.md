# VIDEO SESSION CREATION ISSUE - DIAGNOSTIC REPORT

## Problem Summary

When booking a VIRTUAL appointment, the appointment is created successfully but NO video session is being created.

## Root Cause Analysis

### Issue 1: Video Consultation Service Not Running ❌

**STATUS**: The video-consultation-service (Python/FastAPI) is NOT running.

**Evidence**:

- Appointments-service logs show: "Published video session creation request for appointment: 30e622b0-405f-4f69-8ae3-4ba7ddec07ae via RabbitMQ"
- RabbitMQ logs only show heartbeat messages - no actual message consumption
- No logs from video-consultation-service processing the message

**Impact**: RabbitMQ messages are being published but there's no consumer to process them.

### Issue 2: RabbitMQ Exchange Mismatch ✅ FIXED

**STATUS**: Fixed in this session

**Original Problem**:

- Appointments-service publishes to: `appointments-exchange`
- Video-service was listening to: `video-consultation-events`

**Fix Applied**:

- Updated `video-consultation-service/app/config.py` to use `appointments-exchange`

### Issue 3: Missing RabbitMQ Consumer Disconnect ✅ FIXED

**STATUS**: Fixed in this session

**Original Problem**:

- The shutdown handler in `main.py` didn't disconnect the RabbitMQ consumer

**Fix Applied**:

- Added `await rabbitmq_consumer.disconnect()` to the shutdown handler

---

## How the Video Session Flow Should Work

```
┌─────────────────────────────────────────────────────────────────┐
│  VIRTUAL Appointment Booking → Video Session Creation Flow      │
└─────────────────────────────────────────────────────────────────┘

1. Patient books VIRTUAL appointment via mobile app
   └─> POST /appointments/appointments/book

2. Appointments-Service (Java)
   └─> Creates appointment with status: BOOKED
   └─> Publishes RabbitMQ message:
       - Exchange: appointments-exchange
       - Routing Key: appointment.video.create
       - Event Type: appointment.video.create
       - Data: { appointment_id, doctor_id, patient_id, scheduled_time }

3. RabbitMQ
   └─> Routes message to queue: video-session-requests

4. Video-Consultation-Service (Python) 🚫 NOT RUNNING
   └─> SHOULD consume message from queue
   └─> SHOULD create AWS Chime meeting
   └─> SHOULD save session to videodb
   └─> SHOULD publish response:
       - Event Type: video.session.created
       - Routing Key: video.session.created
       - Data: { appointment_id, session_id, meeting_id, attendee_url }

5. Appointments-Service
   └─> SHOULD receive response
   └─> SHOULD update appointment with:
       - meeting_id
       - meeting_link
       - attendee_url
```

---

## Solution Steps

### Step 1: Install and Configure RabbitMQ

```bash
# Check if RabbitMQ is running
# Default port: 5672
# Default credentials: guest/guest

# Windows: Check services
# services.msc → RabbitMQ

# Or download RabbitMQ:
https://www.rabbitmq.com/install-windows.html
```

### Step 2: Configure AWS Credentials

The video-consultation-service requires AWS credentials for AWS Chime.

**Option A: Use AWS Free Tier (Recommended for Development)**

1. Create AWS account: https://aws.amazon.com/free/
2. Navigate to IAM → Users → Security Credentials
3. Create Access Key
4. Copy AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY

**Option B: Use Mock/Testing Mode (Temporary)**

- For local testing, you can temporarily disable AWS Chime integration

### Step 3: Create .env File

```bash
cd video-consultation-service
copy .env.example .env
```

Edit `.env` and set:

```ini
AWS_ACCESS_KEY_ID=your-actual-key
AWS_SECRET_ACCESS_KEY=your-actual-secret
JWT_SECRET_KEY=your-jwt-secret  # Same as auth-service
```

### Step 4: Install Python Dependencies

```bash
cd video-consultation-service
python -m pip install -r requirements.txt
```

### Step 5: Start Video Consultation Service

**Option A: Use the batch file (Recommended)**

```bash
cd PulseOne-Backend
RUN_VIDEO_SERVICE.bat
```

**Option B: Manual start**

```bash
cd video-consultation-service
python main.py
```

**Expected Output**:

```
2026-01-25 12:00:00 - INFO - Database initialized successfully
2026-01-25 12:00:01 - INFO - RabbitMQ Publisher connected successfully
2026-01-25 12:00:02 - INFO - Consumer connected to RabbitMQ: localhost
2026-01-25 12:00:02 - INFO - Queues and bindings set up successfully
2026-01-25 12:00:02 - INFO - RabbitMQ Consumer started successfully
2026-01-25 12:00:03 - INFO - 🎥 Video Consultation Service started on port 8086
INFO:     Uvicorn running on http://0.0.0.0:8086
```

### Step 6: Verify Service is Running

```bash
# Test health check
curl http://localhost:8086/health

# Expected response:
{
  "status": "healthy",
  "service": "Video Consultation Service",
  "version": "1.0.0",
  "database": "healthy",
  "rabbitmq": "healthy",
  "aws_chime": "configured"
}
```

### Step 7: Test Video Session Creation

1. Book a VIRTUAL appointment via mobile app
2. Check appointments-service logs for:
   ```
   INFO: Published video session creation request for appointment: <uuid>
   ```
3. Check video-consultation-service logs for:
   ```
   INFO: Received event: appointment.video.create
   INFO: Creating video session for appointment: <uuid>
   INFO: Video session created: <session_id> for appointment: <uuid>
   INFO: Published event: video.session.created
   ```
4. Check appointments-service logs for:
   ```
   INFO: Received video session event from Video Service
   INFO: Updated appointment with video session
   ```

---

## Quick Verification Checklist

- [ ] PostgreSQL running (port 5432)
- [ ] RabbitMQ running (port 5672)
- [ ] videodb database exists
- [ ] video-consultation-service/.env file configured
- [ ] AWS credentials set in .env
- [ ] JWT_SECRET_KEY matches auth-service
- [ ] video-consultation-service is running
- [ ] Health check passes: http://localhost:8086/health
- [ ] Can see "RabbitMQ Consumer started successfully" in logs

---

## Debugging Commands

### Check RabbitMQ Status

```bash
# Windows
netstat -ano | findstr :5672

# Check RabbitMQ Management UI
http://localhost:15672/
# Default: guest/guest
```

### Check PostgreSQL Databases

```sql
psql -U postgres -c "\l"
# Should see: videodb
```

### View Video Service Logs

```bash
# Logs appear in terminal where service is running
# Look for errors related to:
- Database connection
- RabbitMQ connection
- AWS credentials
```

### Test RabbitMQ Message Flow

```bash
# 1. Start video-consultation-service
# 2. Book VIRTUAL appointment
# 3. Watch both service logs in real-time
```

---

## Files Modified in This Fix

1. `video-consultation-service/app/config.py`
   - Changed RABBITMQ_EXCHANGE to "appointments-exchange"
   - Fixed database port from 5438 to 5432

2. `video-consultation-service/main.py`
   - Added rabbitmq_consumer.disconnect() to shutdown handler

3. **NEW:** `video-consultation-service/.env.example`
   - Created template for environment configuration

4. **NEW:** `RUN_VIDEO_SERVICE.bat`
   - Created standalone run script for video service

---

## Common Errors and Solutions

### Error: "Failed to connect to RabbitMQ"

**Solution**: Install and start RabbitMQ service

### Error: "AWS credentials not found"

**Solution**: Set AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY in .env

### Error: "Database connection failed"

**Solution**:

- Ensure PostgreSQL is running
- Run SETUP_DATABASES.bat to create videodb
- Check DB_PASSWORD in .env matches your PostgreSQL password

### Error: "ModuleNotFoundError"

**Solution**: Install dependencies: `pip install -r requirements.txt`

---

## Additional Resources

- AWS Chime SDK: https://aws.amazon.com/chime/chime-sdk/
- RabbitMQ Downloads: https://www.rabbitmq.com/download.html
- Video Integration Docs: See VIDEO_INTEGRATION_RABBITMQ.md
- API Documentation: http://localhost:8086/docs (when service is running)

---

## Next Steps After Fix

1. Start video-consultation-service
2. Book a new VIRTUAL appointment
3. Verify video session is created in videodb
4. Verify appointment is updated with meeting_link
5. Test joining the video consultation

---

**Note**: The core issue is simply that the video-consultation-service needs to be running to consume and process the RabbitMQ messages. Once started with proper configuration, video sessions will be created automatically when VIRTUAL appointments are booked.
