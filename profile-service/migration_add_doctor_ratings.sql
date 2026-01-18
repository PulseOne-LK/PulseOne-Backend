-- Migration: Add doctor_rating table for patient reviews
-- Purpose: Allows patients to rate and review doctors

create table if not exists doctor_rating (
   id              serial primary key,
   doctor_user_id  varchar(255) not null,
   patient_user_id varchar(255) not null,
   rating          integer not null check ( rating >= 1
      and rating <= 5 ),
   review          text,
   created_at      timestamp default current_timestamp,
   updated_at      timestamp default current_timestamp,
    -- Ensure each patient can only rate a doctor once (soft constraint via unique index)
   unique ( doctor_user_id,
            patient_user_id )
);

-- Create indexes for efficient queries
create index if not exists idx_doctor_rating_doctor_user_id on
   doctor_rating (
      doctor_user_id
   );
create index if not exists idx_doctor_rating_patient_user_id on
   doctor_rating (
      patient_user_id
   );
create index if not exists idx_doctor_rating_created_at on
   doctor_rating (
      created_at
   );

-- Add this migration to be executed against the profilesdb database
-- Usage: psql -U postgres -d profilesdb -f migration_add_doctor_ratings.sql