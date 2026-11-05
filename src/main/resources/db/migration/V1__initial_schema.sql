CREATE TABLE users (
 id CHAR(36) NOT NULL PRIMARY KEY,
 email VARCHAR(254) NULL UNIQUE,
 phone VARCHAR(24) NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL,
 full_name VARCHAR(100) NOT NULL,
 role VARCHAR(16) NOT NULL DEFAULT 'FARMER',
 village VARCHAR(100) NOT NULL,
 district VARCHAR(100) NOT NULL,
 state VARCHAR(100) NOT NULL DEFAULT 'Maharashtra',
 latitude DECIMAL(9,6) NULL,
 longitude DECIMAL(9,6) NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 CONSTRAINT users_role_check CHECK(role IN ('FARMER','OWNER','OPERATOR','ADMIN')),
 CONSTRAINT users_location_pair_check CHECK((latitude IS NULL)=(longitude IS NULL)),
 CONSTRAINT users_location_lat_check CHECK(latitude IS NULL OR latitude BETWEEN -90 AND 90),
 CONSTRAINT users_location_lng_check CHECK(longitude IS NULL OR longitude BETWEEN -180 AND 180),
 CONSTRAINT users_contact_check CHECK(email IS NOT NULL OR phone IS NOT NULL),
 INDEX users_role_district_idx(role,district,village)
) ENGINE=InnoDB;

CREATE TABLE login_attempts (
 attempt_key CHAR(64) NOT NULL PRIMARY KEY,
 attempts SMALLINT NOT NULL DEFAULT 0,
 window_started TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 INDEX login_attempt_window_idx(window_started)
) ENGINE=InnoDB;

CREATE TABLE refresh_sessions (
 id CHAR(36) NOT NULL PRIMARY KEY,
 user_id CHAR(36) NOT NULL,
 family_id CHAR(36) NOT NULL,
 token_hash CHAR(64) NOT NULL UNIQUE,
 expires_at TIMESTAMP(6) NOT NULL,
 revoked_at TIMESTAMP(6) NULL,
 replaced_by_hash CHAR(64) NULL,
 user_agent VARCHAR(500) NULL,
 ip_address VARCHAR(45) NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT refresh_user_fk FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
 INDEX refresh_family_idx(family_id), INDEX refresh_user_idx(user_id), INDEX refresh_expiry_idx(expires_at)
) ENGINE=InnoDB;

CREATE TABLE equipment (
 id CHAR(36) NOT NULL PRIMARY KEY,
 owner_id CHAR(36) NOT NULL,
 name VARCHAR(120) NOT NULL,
 type VARCHAR(24) NOT NULL,
 brand VARCHAR(100) NULL,
 model VARCHAR(100) NULL,
 manufacturing_year SMALLINT NULL,
 description VARCHAR(3000) NOT NULL DEFAULT '',
 specifications JSON NOT NULL,
 photos JSON NOT NULL,
 village VARCHAR(100) NOT NULL,
 district VARCHAR(100) NOT NULL,
 state VARCHAR(100) NOT NULL DEFAULT 'Maharashtra',
 latitude DECIMAL(9,6) NULL,
 longitude DECIMAL(9,6) NULL,
 price_per_day_paise BIGINT NOT NULL,
 price_per_acre_paise BIGINT NULL,
 transport_fee_paise BIGINT NOT NULL DEFAULT 0,
 operator_available BOOLEAN NOT NULL DEFAULT FALSE,
 status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
 average_rating DECIMAL(3,2) NOT NULL DEFAULT 0,
 rating_count INT NOT NULL DEFAULT 0,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 CONSTRAINT equipment_owner_fk FOREIGN KEY(owner_id) REFERENCES users(id) ON DELETE RESTRICT,
 CONSTRAINT equipment_type_check CHECK(type IN ('TRACTOR','HARVESTER','ROTAVATOR','SPRAYER','SEED_DRILL','CULTIVATOR','OTHER')),
 CONSTRAINT equipment_status_check CHECK(status IN ('AVAILABLE','UNAVAILABLE','MAINTENANCE','INACTIVE')),
 CONSTRAINT equipment_rates_check CHECK(price_per_day_paise>=0 AND transport_fee_paise>=0 AND (price_per_acre_paise IS NULL OR price_per_acre_paise>=0)),
 CONSTRAINT equipment_location_check CHECK((latitude IS NULL)=(longitude IS NULL)),
 INDEX equipment_search_idx(type,status,district,price_per_day_paise),
 INDEX equipment_owner_idx(owner_id,created_at), INDEX equipment_name_idx(name)
) ENGINE=InnoDB;

CREATE TABLE operator_profiles (
 user_id CHAR(36) NOT NULL PRIMARY KEY,
 skills JSON NOT NULL,
 daily_rate_paise BIGINT NOT NULL DEFAULT 0,
 service_area_km INT NOT NULL DEFAULT 25,
 experience_years INT NOT NULL DEFAULT 0,
 available BOOLEAN NOT NULL DEFAULT TRUE,
 verified_at TIMESTAMP(6) NULL,
 rating DECIMAL(3,2) NOT NULL DEFAULT 0,
 rating_count INT NOT NULL DEFAULT 0,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT operator_user_fk FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
 CONSTRAINT operator_rate_check CHECK(daily_rate_paise>=0 AND experience_years>=0 AND service_area_km BETWEEN 1 AND 500)
) ENGINE=InnoDB;

CREATE TABLE bookings (
 id CHAR(36) NOT NULL PRIMARY KEY,
 equipment_id CHAR(36) NOT NULL,
 renter_id CHAR(36) NOT NULL,
 operator_id CHAR(36) NULL,
 start_date DATE NOT NULL,
 end_date DATE NOT NULL,
 acres DECIMAL(8,2) NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 daily_rate_paise BIGINT NOT NULL,
 rental_amount_paise BIGINT NOT NULL,
 operator_fee_paise BIGINT NOT NULL DEFAULT 0,
 transport_fee_paise BIGINT NOT NULL DEFAULT 0,
 total_paise BIGINT NOT NULL,
 renter_note VARCHAR(1000) NOT NULL DEFAULT '',
 farm_village VARCHAR(100) NULL,
 farm_district VARCHAR(100) NULL,
 farm_latitude DECIMAL(9,6) NULL,
 farm_longitude DECIMAL(9,6) NULL,
 owner_note VARCHAR(1000) NOT NULL DEFAULT '',
 idempotency_key VARCHAR(100) NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 CONSTRAINT booking_equipment_fk FOREIGN KEY(equipment_id) REFERENCES equipment(id) ON DELETE RESTRICT,
 CONSTRAINT booking_renter_fk FOREIGN KEY(renter_id) REFERENCES users(id) ON DELETE RESTRICT,
 CONSTRAINT booking_operator_fk FOREIGN KEY(operator_id) REFERENCES users(id) ON DELETE SET NULL,
 CONSTRAINT booking_dates_check CHECK(end_date>start_date),
 CONSTRAINT booking_amount_check CHECK(daily_rate_paise>=0 AND rental_amount_paise>=0 AND operator_fee_paise>=0 AND transport_fee_paise>=0 AND total_paise>=0),
 CONSTRAINT booking_status_check CHECK(status IN ('PENDING','CONFIRMED','REJECTED','CANCELLED','IN_PROGRESS','COMPLETED')),
 UNIQUE KEY booking_idempotency_uk(renter_id,idempotency_key),
 INDEX booking_renter_idx(renter_id,created_at), INDEX booking_equipment_dates_idx(equipment_id,status,start_date,end_date), INDEX booking_operator_idx(operator_id,status,start_date,end_date)
) ENGINE=InnoDB;

CREATE TABLE payments (
 id CHAR(36) NOT NULL PRIMARY KEY,
 booking_id CHAR(36) NOT NULL UNIQUE,
 payer_id CHAR(36) NOT NULL,
 payee_id CHAR(36) NOT NULL,
 rental_amount_paise BIGINT NOT NULL,
 transport_paise BIGINT NOT NULL DEFAULT 0,
 operator_paise BIGINT NOT NULL DEFAULT 0,
 status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
 method VARCHAR(30) NOT NULL DEFAULT 'OFFLINE',
 reference VARCHAR(100) NULL,
 recorded_by CHAR(36) NULL,
 paid_at TIMESTAMP(6) NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT payment_booking_fk FOREIGN KEY(booking_id) REFERENCES bookings(id) ON DELETE RESTRICT,
 CONSTRAINT payment_payer_fk FOREIGN KEY(payer_id) REFERENCES users(id) ON DELETE RESTRICT,
 CONSTRAINT payment_payee_fk FOREIGN KEY(payee_id) REFERENCES users(id) ON DELETE RESTRICT,
 CONSTRAINT payment_recorder_fk FOREIGN KEY(recorded_by) REFERENCES users(id) ON DELETE SET NULL,
 CONSTRAINT payment_status_check CHECK(status IN ('PENDING','PAID','REFUNDED','FAILED')),
 CONSTRAINT payment_amount_check CHECK(rental_amount_paise>=0 AND transport_paise>=0 AND operator_paise>=0),
 INDEX payment_payer_idx(payer_id,created_at), INDEX payment_payee_idx(payee_id,created_at)
) ENGINE=InnoDB;

CREATE TABLE maintenance_logs (
 id CHAR(36) NOT NULL PRIMARY KEY,
 equipment_id CHAR(36) NOT NULL,
 created_by CHAR(36) NOT NULL,
 service_date DATE NOT NULL,
 operating_hours DECIMAL(10,1) NULL,
 summary VARCHAR(2000) NOT NULL,
 cost_paise BIGINT NOT NULL DEFAULT 0,
 makes_unavailable BOOLEAN NOT NULL DEFAULT FALSE,
 next_service_date DATE NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT maintenance_equipment_fk FOREIGN KEY(equipment_id) REFERENCES equipment(id) ON DELETE CASCADE,
 CONSTRAINT maintenance_creator_fk FOREIGN KEY(created_by) REFERENCES users(id) ON DELETE RESTRICT,
 CONSTRAINT maintenance_cost_check CHECK(cost_paise>=0 AND (operating_hours IS NULL OR operating_hours>=0)),
 INDEX maintenance_due_idx(equipment_id,next_service_date)
) ENGINE=InnoDB;

CREATE TABLE feedback (
 id CHAR(36) NOT NULL PRIMARY KEY,
 user_id CHAR(36) NOT NULL,
 rating TINYINT NOT NULL,
 category VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
 message VARCHAR(3000) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT feedback_user_fk FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
 CONSTRAINT feedback_rating_check CHECK(rating BETWEEN 1 AND 5), INDEX feedback_created_idx(created_at)
) ENGINE=InnoDB;

CREATE TABLE community_posts (
 id CHAR(36) NOT NULL PRIMARY KEY,
 author_id CHAR(36) NOT NULL,
 body VARCHAR(2000) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 deleted_at TIMESTAMP(6) NULL,
 CONSTRAINT community_author_fk FOREIGN KEY(author_id) REFERENCES users(id) ON DELETE CASCADE,
 INDEX community_feed_idx(deleted_at,created_at)
) ENGINE=InnoDB;

CREATE TABLE audit_events (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 actor_id CHAR(36) NULL,
 action VARCHAR(80) NOT NULL,
 entity_type VARCHAR(40) NOT NULL,
 entity_id VARCHAR(80) NULL,
 metadata JSON NOT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT audit_actor_fk FOREIGN KEY(actor_id) REFERENCES users(id) ON DELETE SET NULL,
 INDEX audit_actor_time_idx(actor_id,created_at), INDEX audit_entity_idx(entity_type,entity_id,created_at)
) ENGINE=InnoDB;
