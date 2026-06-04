CREATE TABLE IF NOT EXISTS department (
  id VARCHAR(40) PRIMARY KEY,
  name VARCHAR(80) NOT NULL UNIQUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS position_table (
  id VARCHAR(40) PRIMARY KEY,
  department_id VARCHAR(40),
  name VARCHAR(80) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE position_table ADD COLUMN IF NOT EXISTS department_id VARCHAR(40);
ALTER TABLE position_table DROP CONSTRAINT IF EXISTS position_table_name_key;
CREATE UNIQUE INDEX IF NOT EXISTS uk_position_department_name ON position_table(department_id, name);

CREATE TABLE IF NOT EXISTS employee (
  id VARCHAR(40) PRIMARY KEY,
  employee_no VARCHAR(40) NOT NULL UNIQUE,
  name VARCHAR(80) NOT NULL,
  gender VARCHAR(10) NOT NULL,
  age INT NOT NULL DEFAULT 0,
  department_id VARCHAR(40) NOT NULL,
  position_id VARCHAR(40) NOT NULL,
  hire_date DATE NOT NULL,
  status VARCHAR(20) NOT NULL,
  login_account VARCHAR(80) UNIQUE,
  login_password_hash VARCHAR(255),
  role_code VARCHAR(40) NOT NULL DEFAULT 'employee',
  recycle_receiver BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE employee ADD COLUMN IF NOT EXISTS recycle_receiver BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX IF NOT EXISTS idx_employee_department ON employee(department_id);
CREATE INDEX IF NOT EXISTS idx_employee_no ON employee(employee_no);
CREATE INDEX IF NOT EXISTS idx_employee_status ON employee(status);
CREATE INDEX IF NOT EXISTS idx_employee_recycle_receiver ON employee(recycle_receiver);

CREATE TABLE IF NOT EXISTS supervisor_data_scope (
  id VARCHAR(40) PRIMARY KEY,
  supervisor_id VARCHAR(40) NOT NULL,
  department_id VARCHAR(40) NOT NULL,
  position_id VARCHAR(40),
  all_positions BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_supervisor_scope_supervisor ON supervisor_data_scope(supervisor_id);
CREATE INDEX IF NOT EXISTS idx_supervisor_scope_department ON supervisor_data_scope(department_id);
CREATE INDEX IF NOT EXISTS idx_supervisor_scope_position ON supervisor_data_scope(position_id);

UPDATE position_table p
SET department_id = (
  SELECT e.department_id FROM employee e WHERE e.position_id = p.id LIMIT 1
)
WHERE p.department_id IS NULL AND EXISTS (
  SELECT 1 FROM employee e WHERE e.position_id = p.id
);

CREATE TABLE IF NOT EXISTS phone_number (
  id VARCHAR(40) PRIMARY KEY,
  employee_id VARCHAR(40) NOT NULL,
  phone_number VARCHAR(30) NOT NULL,
  operator VARCHAR(20) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT '在用',
  registered_at DATE NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_employee_phone ON phone_number(employee_id, phone_number);
CREATE INDEX IF NOT EXISTS idx_phone_number ON phone_number(phone_number);

CREATE TABLE IF NOT EXISTS device_asset (
  id VARCHAR(40) PRIMARY KEY,
  employee_id VARCHAR(40),
  department_id VARCHAR(40) NOT NULL,
  phone_id VARCHAR(40) NOT NULL,
  device_type VARCHAR(40) NOT NULL,
  brand VARCHAR(80) NOT NULL,
  model VARCHAR(120) NOT NULL,
  status VARCHAR(30) NOT NULL,
  registered_at DATE NOT NULL,
  recycle_initiator_employee_id VARCHAR(40),
  recycle_initiator_name VARCHAR(80),
  recycle_source VARCHAR(80),
  recycle_reason VARCHAR(255),
  recycle_created_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_device_owner ON device_asset(employee_id);
CREATE INDEX IF NOT EXISTS idx_device_department ON device_asset(department_id);
CREATE INDEX IF NOT EXISTS idx_device_status ON device_asset(status);
CREATE INDEX IF NOT EXISTS idx_device_type_brand_model ON device_asset(device_type, brand, model);

CREATE TABLE IF NOT EXISTS channel_account (
  id VARCHAR(40) PRIMARY KEY,
  employee_id VARCHAR(40) NOT NULL,
  phone_id VARCHAR(40) NOT NULL,
  channel VARCHAR(80) NOT NULL,
  account_name VARCHAR(160) NOT NULL,
  account_password_cipher VARCHAR(512) NOT NULL,
  real_name_status VARCHAR(20) NOT NULL,
  real_name VARCHAR(80) NOT NULL,
  id_card_cipher VARCHAR(512) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT '在用',
  registered_at DATE NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_channel_account_owner ON channel_account(employee_id);
CREATE INDEX IF NOT EXISTS idx_channel_account_phone ON channel_account(phone_id);
CREATE INDEX IF NOT EXISTS idx_channel_account_channel ON channel_account(channel);

CREATE TABLE IF NOT EXISTS handover_task (
  id VARCHAR(40) PRIMARY KEY,
  applicant_id VARCHAR(40) NOT NULL,
  source_department_id VARCHAR(40) NOT NULL,
  device_id VARCHAR(40) NOT NULL,
  asset_type VARCHAR(20) NOT NULL DEFAULT '设备',
  asset_id VARCHAR(40) NOT NULL,
  target_type VARCHAR(30) NOT NULL,
  receiver_employee_id VARCHAR(40),
  receiver_department_id VARCHAR(40),
  approved_by_id VARCHAR(40),
  approved_by_name VARCHAR(80),
  status VARCHAR(30) NOT NULL,
  applicant_note VARCHAR(255),
  reject_reason VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_handover_applicant ON handover_task(applicant_id);
CREATE INDEX IF NOT EXISTS idx_handover_source_department ON handover_task(source_department_id);
CREATE INDEX IF NOT EXISTS idx_handover_receiver ON handover_task(receiver_employee_id);
CREATE INDEX IF NOT EXISTS idx_handover_device ON handover_task(device_id);
CREATE INDEX IF NOT EXISTS idx_handover_status ON handover_task(status);

CREATE TABLE IF NOT EXISTS recycle_record (
  id VARCHAR(40) PRIMARY KEY,
  device_id VARCHAR(40) NOT NULL,
  former_employee_id VARCHAR(40),
  target_employee_id VARCHAR(40),
  department_id VARCHAR(40) NOT NULL,
  action_type VARCHAR(30) NOT NULL,
  action_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  operator_employee_id VARCHAR(40) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_recycle_device ON recycle_record(device_id);
CREATE INDEX IF NOT EXISTS idx_recycle_department ON recycle_record(department_id);
CREATE INDEX IF NOT EXISTS idx_recycle_action ON recycle_record(action_type);
