-- Optional: Hibernate (ddl-auto=update) creates these automatically. Use this file to create them manually.
CREATE DATABASE IF NOT EXISTS smart_attendance CHARACTER SET utf8mb4;
USE smart_attendance;

CREATE TABLE IF NOT EXISTS employees (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  emp_code VARCHAR(30) NOT NULL UNIQUE,
  full_name VARCHAR(255) NOT NULL,
  company_email VARCHAR(255) NOT NULL UNIQUE,
  personal_email VARCHAR(255), phone VARCHAR(255), address VARCHAR(500), gender VARCHAR(255),
  emergency_contact VARCHAR(255), department VARCHAR(255), designation VARCHAR(255),
  date_of_birth DATE, joining_date DATE,
  role VARCHAR(20) NOT NULL, shift_type VARCHAR(20) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  must_change_password BIT NOT NULL, active BIT NOT NULL,
  qr_token VARCHAR(64) NOT NULL UNIQUE,
  manager_id BIGINT NULL,
  FOREIGN KEY (manager_id) REFERENCES employees(id)
);
CREATE TABLE IF NOT EXISTS projects (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255) NOT NULL, description VARCHAR(2000),
  status VARCHAR(20), start_date DATE, end_date DATE, manager_id BIGINT NULL,
  FOREIGN KEY (manager_id) REFERENCES employees(id)
);
CREATE TABLE IF NOT EXISTS project_members (
  project_id BIGINT NOT NULL, employee_id BIGINT NOT NULL, PRIMARY KEY (project_id, employee_id),
  FOREIGN KEY (project_id) REFERENCES projects(id), FOREIGN KEY (employee_id) REFERENCES employees(id)
);
CREATE TABLE IF NOT EXISTS attendance (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, employee_id BIGINT NOT NULL, work_date DATE NOT NULL,
  check_in DATETIME(6), check_out DATETIME(6), worked_hours DOUBLE, status VARCHAR(20),
  UNIQUE KEY uq_att (employee_id, work_date), FOREIGN KEY (employee_id) REFERENCES employees(id)
);
CREATE TABLE IF NOT EXISTS leave_requests (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, employee_id BIGINT NOT NULL, leave_type VARCHAR(20) NOT NULL,
  from_date DATE NOT NULL, to_date DATE NOT NULL, reason VARCHAR(1000), status VARCHAR(20),
  manager_remark VARCHAR(1000), created_at DATETIME(6), decided_at DATETIME(6),
  FOREIGN KEY (employee_id) REFERENCES employees(id)
);
CREATE TABLE IF NOT EXISTS wfh_requests (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, employee_id BIGINT NOT NULL, wfh_date DATE NOT NULL,
  reason VARCHAR(1000), status VARCHAR(20), manager_remark VARCHAR(1000),
  created_at DATETIME(6), decided_at DATETIME(6), FOREIGN KEY (employee_id) REFERENCES employees(id)
);
CREATE TABLE IF NOT EXISTS reminder_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, employee_id BIGINT NOT NULL, reminder_date DATE NOT NULL,
  reminder_type VARCHAR(20) NOT NULL, UNIQUE KEY uq_rem (employee_id, reminder_date, reminder_type),
  FOREIGN KEY (employee_id) REFERENCES employees(id)
);
