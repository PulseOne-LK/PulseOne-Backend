-- Prescription Service Database Schema
-- Note: GORM will auto-create these tables via AutoMigration in main.go

-- Create prescriptions table
create table if not exists prescriptions (
   id             uuid primary key default gen_random_uuid(),
   appointment_id varchar(255),
   doctor_id      varchar(255) not null,
   patient_id     varchar(255) not null,
   clinic_id      varchar(255) not null,
   issued_at      timestamp default current_timestamp,
   status         varchar(50) default 'ACTIVE', -- ACTIVE, FILLED, CANCELLED
   created_at     timestamp default current_timestamp,
   updated_at     timestamp default current_timestamp
);

-- Create prescription items table
create table if not exists prescription_items (
   id              uuid primary key default gen_random_uuid(),
   prescription_id uuid not null
      references prescriptions ( id )
         on delete cascade,
   drug_name       varchar(255) not null,
   dosage          varchar(255) not null,
   duration        varchar(255) not null,
   quantity        int not null,
   created_at      timestamp default current_timestamp,
   updated_at      timestamp default current_timestamp
);

-- Create indexes for performance
create index if not exists idx_prescriptions_appointment_id on
   prescriptions (
      appointment_id
   );
create index if not exists idx_prescriptions_patient_id on
   prescriptions (
      patient_id
   );
create index if not exists idx_prescriptions_clinic_id on
   prescriptions (
      clinic_id
   );
create index if not exists idx_prescriptions_status on
   prescriptions (
      status
   );
create index if not exists idx_prescriptions_issued_at on
   prescriptions (
      issued_at
   );
create index if not exists idx_prescription_items_prescription_id on
   prescription_items (
      prescription_id
   );

create index if not exists idx_prescriptions_patient_id on
   prescriptions (
      patient_id
   );
create index if not exists idx_prescriptions_status on
   prescriptions (
      status
   );
create index if not exists idx_prescriptions_created_at on
   prescriptions (
      created_at
   );
create index if not exists idx_prescription_items_prescription_id on
   prescription_items (
      prescription_id
   );
create index if not exists idx_prescription_events_prescription_id on
   prescription_events (
      prescription_id
   );
create index if not exists idx_prescription_events_created_at on
   prescription_events (
      created_at
   );