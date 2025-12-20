# Doctor Dashboard Requirements

## Based on PulseOne Backend Services Analysis

---

## 📊 DASHBOARD OVERVIEW

The Doctor Dashboard is a personalized workspace for doctors to manage their appointments, consultations, patient records, and schedule. It integrates with three main backend services: **Appointments Service**, **Auth Service**, and **Profile Service**.

---

## 🎯 CORE FEATURES & SECTIONS

### 1. **DASHBOARD HOME** (Landing/Overview)

Main dashboard showing key metrics and quick actions.

#### Key Metrics Cards:

- **Total Appointments Today**
  - Count of all appointments scheduled for today
  - Filter by status (BOOKED, CHECKED_IN, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW)
- **Upcoming Sessions**
  - Count of active sessions for the day
  - Display session times and locations (clinic or virtual)
- **Patients in Queue**
  - Real-time count of patients waiting in waiting room
  - Patients who have CHECKED_IN but not started IN_PROGRESS
- **Consultation Notes Pending**
  - Count of completed appointments without consultation notes yet
  - Quick access to complete medical records
- **New Patient Registrations**
  - Count of new patients assigned to doctor's clinic/practice
  - Recent registrations from profile service

#### Quick Actions Panel:

- [ ] View Today's Schedule
- [ ] View Queue/Waiting Room
- [ ] Add Consultation Notes
- [ ] Manage Schedule/Sessions
- [ ] View Patient History

---

### 2. **APPOINTMENT MANAGEMENT** (Main Scheduling Section)

#### 2.1 Appointments List View

Display appointments in multiple views:

**View Options:**

- **Daily View** (Default) - All appointments for selected day
- **Weekly View** - Overview of all sessions for the week
- **Monthly View** - Calendar overview
- **List View** - Scrollable list with filters

**Appointment Card/Row Information:**

- **Patient Name** - From appointment record
- **Time Slot** - Appointment date and time (from session start time)
- **Appointment Type** - VIRTUAL or IN_PERSON (appointment_type enum)
- **Status Badge** - Current appointment status:
  - 🔵 BOOKED (Blue)
  - 🟢 CHECKED_IN (Green)
  - 🟡 IN_PROGRESS (Yellow/Orange)
  - ✅ COMPLETED (Green)
  - ❌ CANCELLED (Red)
  - ⚠️ NO_SHOW (Gray)
- **Clinic/Location** - Where appointment takes place (if in-person)
- **Chief Complaint** - Patient's reason for visit (from appointment.chief_complaint)
- **Consultation Fee** - Payment amount for appointment
- **Queue Number** - Position in queue for the session

**Filters:**

- By Date Range
- By Status (BOOKED, CHECKED_IN, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW)
- By Appointment Type (VIRTUAL, IN_PERSON)
- By Clinic/Location
- By Patient Name (search)

**Actions per Appointment:**

- 👁️ View Details
- ✏️ Edit Notes
- ⏱️ Start Consultation
- ✅ Mark as Complete
- ❌ Cancel
- 📋 View History

#### 2.2 Appointment Details Panel

When clicking on an appointment:

**Patient Information:**

- Full Name
- Contact Information
- Age/DOB
- Medical History (from consultation notes)
- Previous Appointments (from appointment history)

**Appointment Details:**

- Appointment ID (UUID)
- Date and Time
- Duration
- Type (Virtual/In-person)
- Session Information
- Clinic Name & Address (if applicable)
- Queue Number
- Chief Complaint

**Payment Information:**

- Consultation Fee (BigDecimal)
- Payment Status (from PaymentStatus enum)
- Payment Method

**Actions Available:**

- ✏️ Edit appointment
- 🔴 Mark as IN_PROGRESS
- ✅ Mark as COMPLETED
- ❌ Mark as CANCELLED
- 📞 Start Virtual Consultation (if VIRTUAL type)
- 📋 View/Add Consultation Notes

---

### 3. **WAITING ROOM / QUEUE MANAGEMENT**

#### 3.1 Waiting Room Queue Display

Real-time queue management for in-person appointments.

**Queue Information Display:**

- **Total Waiting** - Count of patients in waiting_room with status CHECKED_IN
- **Now Calling** - Current patient being consulted (CALLED status)
- **Next in Queue** - Next 3-5 patients in order

**Queue Card Components:**

- **Position Number** - Queue number from appointment.queue_number
- **Patient Name**
- **Appointment Time**
- **Check-in Time** (waiting_room.checked_in_at)
- **Wait Duration** - Calculated from checked_in_at
- **Chief Complaint** - Brief reason for visit

**Queue Actions:**

- 📞 **Call Next Patient** - Move patient from queue to consultation

  - Updates: waiting_room.called_at, waiting_room.called_by
  - Triggers: Appointment status → IN_PROGRESS
  - Triggers: Waiting room status → CALLED

- ⏸️ **Hold/Defer** - Defer patient to later
- 🚫 **Mark No-Show** - Patient didn't arrive
  - Updates appointment.status → NO_SHOW

#### 3.2 Waiting Room Status Tracking

Visual timeline for each patient:

- ✅ Checked In → (waiting_room.checked_in_at)
- 📞 Called for Consultation → (waiting_room.called_at)
- 🏥 Consultation Started → (waiting_room.consultation_started_at)

---

### 4. **CONSULTATION MANAGEMENT**

#### 4.1 Consultation Notes Editor

Interface for creating and updating medical records during/after consultation.

**Consultation Notes Fields:**

Medical History Section:

- **Chief Complaint** (textarea) - Patient's reason for visit
- **Vital Signs** (JSON object, displayed as form):
  - Blood Pressure
  - Temperature
  - Heart Rate (Pulse)
  - Respiratory Rate
  - Oxygen Level (SPO2)
  - Weight
  - Height

Clinical Assessment Section:

- **Diagnosis** (textarea) - Doctor's diagnosis
- **Treatment Plan** (textarea) - Recommended treatment

Follow-up Section:

- **Follow-up Required** (boolean toggle)
- **Follow-up Days** (number input) - If follow-up required
- **Follow-up Instructions** (textarea)

Additional Info:

- **Consultation Duration** (minutes) - Length of consultation
- **Notes** (free-form text)

**Automatic Fields:**

- Appointment ID (UUID)
- Doctor ID (from auth context)
- Patient ID (from appointment)
- Created Timestamp
- Note ID (UUID)

**Actions:**

- 💾 Save Consultation Notes
- ✏️ Edit Existing Notes
- 🔄 Update Existing Notes
- 📋 **Proceed to Prescriptions** (Next step after saving notes)

#### 4.2 Consultation Notes History

View previous consultation notes for the patient.

**History List Shows:**

- Previous consultation dates
- Diagnosis for each visit
- Treatment provided
- Follow-up recommendations
- Doctor who performed consultation
- Consultation duration

---

### 4.3 **PRESCRIPTION GENERATION & MANAGEMENT** ⭐ NEW

**Prescription Creation Workflow** (Integrated into Consultation Notes):

When saving consultation notes, doctor proceeds to prescription section:

#### 4.3.1 Prescription Type Selection

Before prescribing, system determines available options based on appointment type:

**FOR IN-PERSON (CLINIC) APPOINTMENTS:**

- ✅ **Dispense from Clinic Inventory** (Primary option)
- ✅ **External Pharmacy** (For items not in stock)
- ✅ **Mixed** (Some from inventory, some from external)

**FOR VIRTUAL APPOINTMENTS:**

- ✅ **External Pharmacy Only** (Inventory not applicable)

#### 4.3.2 Add Medication Interface

**CLINIC APPOINTMENTS - Dual Mode:**

**Mode 1: Search & Dispense from Clinic Inventory**

- 🔍 **Drug Search Bar** (with auto-complete)
  - Search by: Drug Name, Generic Name, Catalog Item ID
  - Real-time suggestions from clinic's catalog
  - Shows available quantity and expiry dates
- **Medication Selection Card:**

  - Drug Name (from CatalogItem.drugName)
  - Generic Name (from CatalogItem.genericName)
  - **Current Stock Status:**
    - ✅ Available Quantity (from InventoryBatch.availableQuantity)
    - ⏰ Batch Expiry Date (from InventoryBatch.expiryDate)
    - 📦 Batch Number (from InventoryBatch.batchNumber)
    - 💰 Cost Price (from InventoryBatch.costPrice) - For clinic records
  - **Availability Indicator:**
    - 🟢 In Stock - Quantity available
    - 🟡 Low Stock - Below reorder_level
    - 🔴 Out of Stock - Quantity unavailable
    - ⚠️ Expiring Soon - Within 30 days of expiry

- **Prescription Details Form:**
  - Dosage (e.g., "500mg", "2 tablets")
  - Duration (e.g., "7 days", "14 days", "30 days")
  - Quantity to Dispense (number input)
  - ✅ Automatically calculates remaining inventory
  - ✅ Automatically selects FIFO batch (oldest batch first)
  - 💾 Add to Prescription List

**Mode 2: Add External Pharmacy Prescription**

- 📋 **Manual Drug Entry:**
  - Drug Name (free text)
  - Dosage (e.g., "500mg", "2 tablets")
  - Duration (e.g., "7 days", "14 days")
  - Quantity (number input)
  - Reason for External (dropdown):
    - "Not in inventory"
    - "Out of stock"
    - "Patient preference"
    - "Special formulation"
  - 💾 Add to Prescription List

**VIRTUAL APPOINTMENTS - External Only:**

- Similar to Mode 2 above (no inventory access)

#### 4.3.3 Prescription Items List

Table/List showing all items added to prescription:

| Drug Name   | Dosage | Duration | Quantity | Source   | Status       |
| ----------- | ------ | -------- | -------- | -------- | ------------ |
| Paracetamol | 500mg  | 7 days   | 14       | Clinic   | ✅ Dispensed |
| Ibuprofen   | 200mg  | 7 days   | 7        | External | 📋 To Buy    |
| Amoxicillin | 250mg  | 10 days  | 30       | Clinic   | ✅ Dispensed |

**Actions per Item:**

- ✏️ Edit
- 🗑️ Remove

#### 4.3.4 Prescription Summary

Before saving:

- **Total Items:** Count of medications
- **Clinic Dispensed:** Count of medications from clinic inventory
- **External Pharmacy:** Count of medications to buy externally
- **Estimated Cost:** Sum of costs (if clinic tracks pricing)

#### 4.3.5 Save & Generate Prescription

- 💾 **Save Prescription** Button
  - Creates Prescription record (prescription_service)
  - For clinic items: Triggers Dispense API (inventory_service)
  - For external items: Marks as pending external pharmacy
  - Generates Prescription ID (UUID)
  - Status: "ACTIVE"

**Data Flow on Save:**

1. ✅ Create Prescription record in prescription_service

   - prescription.appointment_id = Appointment UUID
   - prescription.doctor_id = Doctor User ID
   - prescription.patient_id = Patient User ID
   - prescription.clinic_id = Clinic ID (if in-person)
   - prescription.status = "ACTIVE"

2. ✅ For each clinic item:

   - Call Dispense API (inventory_service/api/inventory/dispense)
   - Reduces InventoryBatch.available_quantity
   - Uses FIFO logic (oldest batch first)
   - Tracks StockTransaction

3. ✅ Create Prescription Items
   - prescription_item.drug_name
   - prescription_item.dosage
   - prescription_item.duration
   - prescription_item.quantity
   - prescription_item.source (CLINIC / EXTERNAL)

#### 4.3.6 Prescription Actions

After prescription is saved:

- 📄 **View Prescription** - Display full prescription details
- 📋 **Print Prescription** - Generate printable format
- 📧 **Send to Patient** - Email prescription details
- 🏥 **Send External Items to Pharmacy** - Generate list for external pharmacy (with patient contact info)
- ❌ **Cancel Prescription** - Mark as CANCELLED (if not yet filled)
- 🔄 **Refill Prescription** - Create similar prescription for follow-up

---

### 4.4 **INVENTORY INTEGRATION - CLINIC APPOINTMENTS ONLY**

**Inventory Visibility** (Only for In-Person Clinic Appointments):

#### 4.4.1 Stock Status Dashboard Widget

Display on Consultation Page (clinic appointments only):

- **Inventory Quick Stats:**
  - 📦 Total Items in Catalog
  - 🔴 Low Stock Items (count below reorder level)
  - ⏰ Expiring Soon (within 30 days)
  - 📉 Out of Stock (quantity = 0)

#### 4.4.2 Low Stock Warning

When adding medication:

- If selected drug has insufficient stock:
  - 🟡 **Yellow Warning:** "Only X units available"
  - Option to: Dispense available amount + request external for remainder
  - Or: Fully external prescription

#### 4.4.3 Dispensing History

Track all dispensed medications per appointment:

- Drug dispensed
- Quantity dispensed
- Batch number used
- Timestamp of dispensing
- Remaining inventory after dispensing

---

### 4.5 **PRESCRIPTION STATUS TRACKING**

Monitor all prescriptions issued:

**Status Options:**

- 🔵 **ACTIVE** - Recently issued, not yet filled
- 🟢 **FILLED** - Patient has filled prescription at pharmacy
- ❌ **CANCELLED** - Prescription cancelled by doctor
- ⏳ **PENDING_EXTERNAL** - Awaiting external pharmacy fulfillment

**Prescription History View:**

- List of all prescriptions issued by doctor
- Patient Name
- Appointment Date
- Prescription Date
- Items Count
- Source Breakdown (Clinic % vs External %)
- Status
- Actions: View, Print, Send, Cancel, Refill

---

### 5. **SCHEDULE MANAGEMENT**

#### 5.1 Doctor's Weekly Schedule

View and manage doctor's available sessions.

**Session Information Display:**

- **Day of Week** (MONDAY - SUNDAY)
- **Session Time** - From session_start_time to session_end_time
- **Clinic Location** - Where session is held (if applicable)
- **Service Type** - TELEMEDICINE or IN_PERSON or HYBRID
- **Max Queue Size** - Maximum patients per session
- **Current Queue Size** - Current number of booked appointments
- **Available Slots** - Remaining capacity

**Session Metrics:**

- 👥 Total Patients Booked
- 💺 Seats Available
- 🕐 Duration (hours)
- 📍 Location Type (Virtual/Physical/Both)

**Session Actions:**

- ✏️ Edit Session
- 🗑️ Delete Session
- ⏸️ Pause Session (prevent new bookings)
- 👁️ View All Appointments in Session

#### 5.2 Create/Edit Session Dialog

Form to create or modify sessions.

**Fields:**

- Day of Week (dropdown)
- Start Time (time picker)
- End Time (time picker)
- Clinic Location (dropdown - if IN_PERSON)
- Service Type (TELEMEDICINE / IN_PERSON / HYBRID)
- Max Queue Size (number)
- Active/Inactive toggle

---

### 6. **PRESCRIPTIONS MANAGEMENT PAGE** ⭐ NEW

Dedicated page for viewing, managing, and issuing prescriptions.

#### 6.1 Prescriptions List/Dashboard

**Overview Display:**

- **Total Prescriptions Issued** - All time & This month
- **Active Prescriptions** - Count of ACTIVE status
- **Filled Prescriptions** - Count of FILLED status
- **Pending External** - Count awaiting external pharmacy fulfillment

**Prescriptions Table:**

| Patient    | Appointment | Issued           | Items | Clinic | External | Status     | Actions             |
| ---------- | ----------- | ---------------- | ----- | ------ | -------- | ---------- | ------------------- |
| John Doe   | 2024-12-20  | 2024-12-20 10:30 | 3     | 2      | 1        | 🔵 ACTIVE  | View, Print, Cancel |
| Jane Smith | 2024-12-19  | 2024-12-19 14:00 | 2     | 0      | 2        | ⏳ PENDING | View, Print, Send   |

**Columns:**

- Patient Name (clickable for patient details)
- Appointment Date
- Issued Date/Time
- Total Items Count
- Clinic Medications Count
- External Medications Count
- Current Status (with color badge)
- Action Buttons

**Filters & Search:**

- Search by patient name/ID
- Filter by status (ACTIVE, FILLED, CANCELLED, PENDING_EXTERNAL)
- Filter by date range
- Filter by source (Clinic, External, Both)
- Sort by: Date (newest/oldest), Status, Patient

#### 6.2 Prescription Details View

When clicking on a prescription:

**Prescription Header:**

- Prescription ID (UUID)
- Patient Name
- Appointment Date & Time
- Doctor Name
- Clinic Name (if in-person)
- Issue Date/Time
- Current Status

**Medications Table:**

| Item # | Drug Name   | Dosage | Duration | Qty | Source      | Dispensed  | Status    |
| ------ | ----------- | ------ | -------- | --- | ----------- | ---------- | --------- |
| 1      | Paracetamol | 500mg  | 7 days   | 14  | 🏥 Clinic   | ✅ Yes     | Completed |
| 2      | Ibuprofen   | 200mg  | 7 days   | 7   | 🏪 External | ⏳ Pending | To Buy    |

**For Clinic Items:**

- Batch Number
- Quantity Dispensed
- Stock Remaining After Dispense
- Timestamp

**For External Items:**

- Reason (Not in stock, Patient preference, etc.)
- Note for pharmacy

**Actions:**

- 📄 View Full Details
- 📋 Print Prescription
- 📧 Email to Patient
- 🏥 Send External List to Pharmacy
- ❌ Cancel Prescription
- 🔄 Refill

#### 6.3 Prescription Statistics (For Clinic Doctors)

**Monthly/Weekly Analytics:**

- **Total Prescriptions Issued**
- **Clinic Dispensed %** - Percentage from clinic inventory
- **External Pharmacy %** - Percentage from external
- **Average Items per Prescription**
- **Top Medications Prescribed** - Most prescribed drugs
- **Stock Usage Rate** - Which inventory items are most used

**Charts:**

- Pie chart: Clinic vs External breakdown
- Bar chart: Top 10 medications prescribed
- Line chart: Prescription trends over time

#### 6.4 Create Quick Prescription (Shortcut)

From Prescriptions page, doctor can:

- ➕ **Create New Prescription** - Manual prescription creation (without appointment)
- Useful for follow-up prescriptions or repeat medications

---

### 7. **INVENTORY MANAGEMENT PAGE** (Clinic Doctors Only) ⭐ NEW

Dedicated page for managing pharmacy inventory. **Only visible for clinic-based doctors.**

#### 7.1 Inventory Overview Dashboard

**Quick Stats Cards:**

- 📦 Total Catalog Items - Drug types in inventory
- ✅ Items in Stock - Items with available quantity > 0
- 🔴 Low Stock Items - Items below reorder level
- ⏰ Expiring Soon - Items expiring within 30 days
- 📉 Out of Stock - Items with zero quantity

#### 7.2 Stock Levels View

**Inventory Table:**

| Drug Name   | Generic Name | Available | Reorder Level | Status  | Batches | Expiry     | Actions      |
| ----------- | ------------ | --------- | ------------- | ------- | ------- | ---------- | ------------ |
| Paracetamol | Paracetamol  | 250 units | 100 units     | 🟢 Good | 3       | 2025-06-15 | View Batches |
| Ibuprofen   | Ibuprofen    | 45 units  | 100 units     | 🟡 Low  | 2       | 2025-09-20 | View Batches |
| Amoxicillin | Amoxicillin  | 0 units   | 50 units      | 🔴 Out  | 0       | -          | Reorder      |

**Columns:**

- Drug Name
- Generic Name
- Available Quantity
- Reorder Level (threshold)
- Status Badge (Good/Low/Out)
- Number of Batches
- Nearest Expiry Date
- Action Buttons

**Filters:**

- By Status (In Stock, Low Stock, Out of Stock)
- By Expiry (Expiring Soon)
- By Drug Name (search)
- Sort by: Name, Quantity, Status, Expiry Date

#### 7.3 Batch Management

**Batch Details for Each Drug:**

- Batch Number (unique identifier)
- Expiry Date
- Cost Price (per unit)
- Total Quantity Received
- Current Available Quantity
- Quantity Dispensed
- **Dispensing History:**
  - Date dispensed
  - Appointment ID
  - Quantity dispensed
  - Doctor who dispensed

**Actions per Batch:**

- 📋 View Details
- ✏️ Edit Cost Price (if needed)
- 📊 View Dispensing History

#### 7.4 Add/Manage Stock

**Receive Stock (Stock-In):**

- Drug Name (select from catalog)
- Batch Number
- Expiry Date (date picker)
- Cost Price (per unit)
- Quantity Received
- Supplier (optional)
- Notes (optional)
- ✅ Add Stock Button

**Catalog Management:**

- Add New Drug to Catalog
- Edit Drug Details (Generic name, Unit type, Reorder level)
- View Drug Activity (usage history)

#### 7.5 Expiry Management

**Expiring Soon Alert:**

- List of items expiring within 30 days
- Drug name, batch number, expiry date
- Quantity available
- **Actions:**
  - 📌 Mark for Disposal
  - 📌 Mark as Used/Expired
  - 🔄 Extend Expiry (if applicable)

**Disposal/Expiry Records:**

- Track expired and disposed medications
- Maintain audit trail for compliance

#### 7.6 Inventory Reports

**Generate Reports:**

- Stock status report (PDF)
- Expiry status report
- Dispensing history (by period)
- Low stock alert report
- Usage analysis

---

### 8. **PROFILE & PREFERENCES**

#### 8.1 Doctor Profile Information

View and update personal/professional profile.

**Personal Information:**

- Full Name
- Email
- Contact Number
- Profile Photo

**Professional Information:**

- Specialty (from doctor_profile.specialty)
- Years of Experience (from doctor_profile.years_of_experience)
- Consultation Fee (from doctor_profile.consultation_fee)
- Professional Bio (from doctor_profile.bio)
- License Photo (from doctor_profile.license_photo_url)
- Verification Status (from doctor_profile.is_verified) - Badge showing if verified

**Telemedicine Settings:**

- Virtual Consultation URL (telecom_url)
- Is Virtual Available (boolean toggle)

**Clinic Affiliations:**

- List of clinics where doctor works
- Primary clinic
- Working hours at each clinic

#### 8.2 Preferences/Settings

- Notification preferences
- Default session duration
- Auto-accept appointments toggle
- Language preference
- Theme (Light/Dark mode)

---

### 9. **REPORTS & ANALYTICS**

#### 9.1 Performance Dashboard

Metrics showing doctor's performance.

**Statistics:**

- **Total Appointments** - All time & This month
- **Completed Appointments** - Percentage of completed vs total
- **Average Consultation Duration** - From consultation_notes.consultation_duration_minutes
- **No-Show Rate** - Percentage of NO_SHOW status
- **Patient Satisfaction** (if implemented)
- **Revenue Generated** - Sum of consultation_fee for completed appointments
- **New Patients** - Count of new patient registrations

#### 9.2 Charts & Graphs

- **Appointment Trends** - Line chart of appointments over time
- **Status Distribution** - Pie chart of appointment statuses
- **Hourly Distribution** - Bar chart of appointments by time slot
- **Clinic-wise Breakdown** - If multiple clinic assignments
- **Revenue Chart** - Monthly/Weekly revenue trends

---

### 10. **SEARCH & FILTERS**

Global search bar to quickly find:

- 🔍 Patient by name/ID
- 🔍 Appointment by ID/date
- 🔍 Consultation notes
- 🔍 Sessions

**Advanced Filters:**

- Date range picker
- Status multi-select
- Clinic/Location filter
- Patient name/ID search
- Appointment type filter

---

### 11. **NOTIFICATIONS**

Real-time notification system:

- 🔔 New appointment booked
- 🔔 Patient checked in (waiting room)
- 🔔 Appointment cancellation
- 🔔 Follow-up reminders
- 🔔 New patient registration
- 🔔 Messages from admin/clinic staff

---

### 12. **PATIENT HISTORY VIEW**

Quick access panel showing patient's medical history.

**Available When:**

- Viewing appointment details
- Editing consultation notes

**Shows:**

- Previous appointment dates
- Previous diagnoses
- Previous treatments
- Previous consultations
- Follow-up history
- Allergies/Medical conditions (if stored)
- Current medications

---

## 📱 LAYOUT & NAVIGATION

### Top Navigation Bar:

- 🏠 Dashboard Logo/Home
- 📅 Date/Time Display
- 🔔 Notifications Bell (with unread count)
- 👤 Doctor Profile Menu (Logout, Settings, Profile)

### Left Sidebar Navigation:

```
┌─────────────────────────┐
│ 📊 Dashboard            │
│ 📅 Appointments         │
│ 👥 Queue/Waiting        │
│ 📋 Consultations        │
│ 💊 Prescriptions ⭐ NEW │
│ 🗓️  Schedule            │
│ 👤 Profile              │
│ 📈 Reports              │
│ 📦 Inventory (Clinic)   │
│ ⚙️  Settings            │
└─────────────────────────┘
```

### Main Content Area:

- Dynamic based on selected section
- Responsive grid layout
- Cards/Panels for information display

---

## 🔄 DATA FLOW & API INTEGRATION

### Authentication:

- Login via Auth Service
- JWT token for API requests
- Doctor identity via `userId`

### Data Sources:

**From Appointments Service:**

- Appointments (GET, POST, PUT, DELETE)
- Sessions (GET, POST, PUT, DELETE)
- Consultation Notes (POST, GET, PUT)
- Waiting Room (GET, POST, PUT)
- Appointment History (GET)
- Doctors (GET, POST)

**From Profile Service:**

- Doctor Profile information (GET, PUT)
- Clinic information (GET)

**From Prescription Service:**

- Prescriptions (GET, POST, DELETE)
  - GET /prescriptions/{doctorId} - List all prescriptions by doctor
  - POST /prescriptions - Create new prescription
  - PUT /prescriptions/{prescriptionId} - Update prescription status
  - DELETE /prescriptions/{prescriptionId} - Cancel prescription
- Prescription Items (linked to prescriptions)

**From Inventory Service (Clinic Appointments Only):**

- Catalog Items (GET)
  - GET /api/inventory/catalog/{clinicId} - Get all drugs in clinic catalog
  - GET /api/inventory/catalog/search?name={drugName} - Search drugs by name
- Inventory Batches (GET, POST)
  - GET /api/inventory/batches/{catalogItemId} - Get available batches for a drug
  - Shows: batch_number, expiry_date, available_quantity, cost_price
- Stock Levels (GET)
  - GET /api/inventory/stock/{catalogItemId}/{clinicId} - Get current stock
  - GET /api/inventory/low-stock/{clinicId} - Get items below reorder level
  - GET /api/inventory/expiring/{clinicId} - Get items expiring soon
- Dispense Medication (POST)
  - POST /api/inventory/dispense - Dispense medication from inventory
  - Automatically uses FIFO (oldest batch first)
  - Reduces available_quantity in InventoryBatch
  - Creates StockTransaction record

### Conditional Logic by Appointment Type:

**IN-PERSON (CLINIC) APPOINTMENTS:**

- ✅ Show Inventory Search & Quick Stats
- ✅ Allow prescription from clinic inventory
- ✅ Allow prescription from external pharmacy (if not in stock)
- ✅ Dispense API available on save

**VIRTUAL APPOINTMENTS:**

- ❌ Hide Inventory Section
- ✅ Allow prescription from external pharmacy only
- ❌ Dispense API not called

---

## 🎨 UI/UX CONSIDERATIONS

### Color Scheme:

- **Primary:** Medical Blue (#0066CC)
- **Status Colors:**
  - BOOKED: Blue
  - CHECKED_IN: Green
  - IN_PROGRESS: Orange
  - COMPLETED: Green with checkmark
  - CANCELLED: Red
  - NO_SHOW: Gray

### Typography:

- Headers: Bold, Large font
- Body text: Regular weight
- Labels: Semi-bold
- Helper text: Smaller, lighter color

### Components Needed:

- ✅ Cards/Panels
- ✅ Buttons (Primary, Secondary, Danger)
- ✅ Forms with validation
- ✅ Modals/Dialogs
- ✅ Dropdowns/Selects
- ✅ Date/Time pickers
- ✅ Tabs
- ✅ Tables with sorting/filtering
- ✅ Charts/Graphs
- ✅ Status badges
- ✅ Timeline components
- ✅ Notifications/Toasts

### Responsive Design:

- Desktop: Full layout with sidebar
- Tablet: Collapsible sidebar
- Mobile: Bottom navigation or hamburger menu

---

## 🔒 SECURITY & PERMISSIONS

### Access Control:

- Only authenticated doctors can access
- Doctors can only see their own appointments
- Doctors cannot modify appointments of other doctors
- Admin can view all appointments (separate admin dashboard)

### Data Protection:

- Patient data privacy (HIPAA-compliant if required)
- Secure API communication (HTTPS)
- Token-based authentication

---

## 📊 ENTITY RELATIONSHIPS SUMMARY

```
Doctor (Auth Service)
├── Doctor (Appointments Service)
│   ├── Sessions
│   │   ├── Appointments
│   │   │   ├── Waiting Room
│   │   │   ├── Consultation Notes
│   │   │   └── Appointment History
│   │   └── Clinic
│   └── Clinic
└── Doctor Profile (Profile Service)
    └── Clinic Profile
```

---

## ✅ FEATURE CHECKLIST FOR FIGMA

**Core Dashboard Features:**

- [ ] Dashboard overview with metrics
- [ ] Appointment list view (multiple layouts - daily, weekly, monthly)
- [ ] Appointment details modal
- [ ] Waiting room/queue management
- [ ] Consultation notes editor
- [ ] Schedule management (create/edit/delete sessions)
- [ ] Doctor profile page
- [ ] Settings/Preferences page
- [ ] Reports & analytics dashboard
- [ ] Search and filter interface
- [ ] Notification system
- [ ] Patient history sidebar
- [ ] Navigation structure
- [ ] Status badges and icons
- [ ] Forms (create/edit dialogs)
- [ ] Charts and graphs
- [ ] Mobile responsive layouts
- [ ] Error states and empty states
- [ ] Loading states
- [ ] Confirmation dialogs

**Prescription Management Features:** ⭐ NEW

- [ ] Prescription creation wizard
- [ ] Drug search with auto-complete
- [ ] Medication selection from inventory (clinic only)
- [ ] Stock availability indicators (In Stock, Low, Out, Expiring Soon)
- [ ] External pharmacy medication entry
- [ ] Prescription items list/table
- [ ] Prescription summary view
- [ ] Prescription save with dispensing
- [ ] Prescription details page
- [ ] Prescription history/list view
- [ ] Print prescription
- [ ] Email prescription to patient
- [ ] Send external medications list to pharmacy
- [ ] Cancel prescription
- [ ] Refill prescription
- [ ] Prescription status tracking (ACTIVE, FILLED, CANCELLED, PENDING_EXTERNAL)
- [ ] Prescription statistics/analytics

**Inventory Management Features (Clinic Only):** ⭐ NEW

- [ ] Inventory dashboard with quick stats
- [ ] Stock levels table with filters and sorting
- [ ] Batch management view
- [ ] Add/receive stock form
- [ ] Catalog management (add/edit drugs)
- [ ] Expiry management alerts
- [ ] Low stock warnings
- [ ] Dispensing history tracking
- [ ] Inventory reports (PDF generation)
- [ ] Stock usage analytics
- [ ] Stock transaction history
- [ ] FIFO visual indicators

---

## 🚀 IMPLEMENTATION NOTES

### Key Data to Display:

1. **Dates & Times:** Use proper formatting (12/24 hour, timezone aware)
2. **Numbers:** Format currency for consultation fees, numbers for metrics
3. **Names:** Display patient and doctor names prominently
4. **Status:** Always show status with color-coding
5. **Duration:** Show time elapsed in waiting room and consultation duration

### Real-time Features:

- Queue position updates as patients are called
- Appointment status changes reflected immediately
- Notification system for new appointments/changes
- WebSocket recommended for real-time updates

### Performance Considerations:

- Pagination for appointment lists (especially history)
- Lazy loading for patient history
- Debounce search functionality
- Cache frequently accessed data

---

## 📝 CONSULTATION NOTES - DETAILED VIEW

The consultation notes section is critical and needs special attention:

**Pre-Consultation:**

- Display patient info
- Display appointment details
- Display chief complaint

**During Consultation:**

- Real-time vital signs entry
- Diagnosis entry (rich text)
- Treatment plan (rich text with formatting)
- Medications list (searchable/auto-complete)

**Post-Consultation:**

- Mark appointment as COMPLETED
- Save consultation notes
- Set follow-up requirements
- Generate prescription (if prescription service integrated)
- Auto-notify patient about follow-up

---

## 🎯 PRIORITY FEATURES (MVP)

**Phase 1 (Must Have):**

- Dashboard overview
- Appointments list
- Appointment details
- Consultation notes editor
- Waiting room queue
- Schedule view
- **Prescription creation** (External pharmacy for virtual, Inventory + External for clinic)
- **Dispensing integration** (for clinic doctors)

**Phase 2 (Should Have):**

- Reports/Analytics
- Patient history
- Settings/Profile
- Appointment filters
- Search functionality
- **Prescriptions management page**
- **Inventory view** (clinic doctors only)
- **Prescription history & status tracking**

**Phase 3 (Nice to Have):**

- Advanced analytics
- Custom reports
- Bulk actions
- Export functionality
- Appointment reminders
- Integration with calendar apps
- **Inventory stock reordering workflow**
- **Advanced inventory reports**
- **Batch expiry tracking & disposal**
- **Prescription refill automation**

---

Generated: December 19, 2025
Based on: PulseOne Backend Services Architecture Analysis
