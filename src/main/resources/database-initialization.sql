-- database initialization
-- create the database (if it has not been created yet)
CREATE DATABASE IF NOT EXISTS online_meeting_booking_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE online_meeting_booking_system;

-- set security mode
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE='NO_AUTO_VALUE_ON_ZERO';
SET NAMES utf8mb4;

-- delete the existing table (if any exists)
DROP TABLE IF EXISTS booking_reject_log;
DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS meeting_rooms;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS users;

-- create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100),
    avatar VARCHAR(255),
    role ENUM('Admin', 'User') DEFAULT 'User',
    is_locked BOOLEAN DEFAULT FALSE,
    email_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL,
    last_password_change_at TIMESTAMP NULL,
    lock_until DATETIME NULL,
    failed_attempts INT DEFAULT 0,
    employee_id VARCHAR(20)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- create user_roles table (if needed)
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role_name),
    CONSTRAINT fk_user_roles FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- create meeting_rooms table
CREATE TABLE IF NOT EXISTS meeting_rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_name VARCHAR(100) NOT NULL UNIQUE,
    type ENUM('COMPUTER_LAB', 'LARGE_CLASSROOM', 'SMALL_CLASSROOM') NOT NULL DEFAULT 'LARGE_CLASSROOM',
    capacity INT NOT NULL CHECK (capacity > 0),
    location VARCHAR(255),
    floor INT,
    has_projector BOOLEAN DEFAULT FALSE,
    has_computer BOOLEAN DEFAULT FALSE,
    has_whiteboard BOOLEAN DEFAULT FALSE,
    has_screen BOOLEAN DEFAULT FALSE,
    has_speaker BOOLEAN DEFAULT FALSE,
    image_url VARCHAR(255),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    available BOOLEAN DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- create bookings table
CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    reason VARCHAR(255),
    status ENUM('PENDING','APPROVED','REJECTED','CANCELLED') DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_booking_room_time (room_id, start_time, end_time),
    INDEX idx_booking_user (user_id),
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_room FOREIGN KEY (room_id) REFERENCES meeting_rooms(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- create booking_reject_log table
CREATE TABLE IF NOT EXISTS booking_reject_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    admin_id BIGINT NOT NULL,
    rejection_reason VARCHAR(255) DEFAULT NULL,
    rejected_at DATETIME DEFAULT NULL,
    CONSTRAINT fk_reject_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT fk_reject_admin FOREIGN KEY (admin_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- create notification table
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    message VARCHAR(255) NOT NULL,
    is_read BIT(1) DEFAULT b'0',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notify_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id BIGINT,
    admin_username VARCHAR(255) NOT NULL,
    target_user_id BIGINT,
    target_username VARCHAR(255),
    action VARCHAR(50) NOT NULL,
    details VARCHAR(1000),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_admin_id (admin_id),
    INDEX idx_target_user_id (target_user_id),
    INDEX idx_action (action),
    INDEX idx_timestamp (timestamp)
);
-- add user data
INSERT INTO users (id, username, email, name, password, role, is_locked, email_verified, created_at, employee_id) VALUES
(1, 'admin', 'admin@university.edu', 'Administrator', '$2a$10$GVEyDhD3A09Cd6Dt7dbONuEeK35a5YzMGW4qDtnN2tBaRItlSHIYO', 'Admin', 0, 1, NOW(), 'A00001'),
(2, 'student1', 'student1@university.edu', 'Student One', '$2a$10$GVEyDhD3A09Cd6Dt7dbONuEeK35a5YzMGW4qDtnN2tBaRItlSHIYO', 'User', 0, 1, NOW(), 'S00001'),
(3, 'student2', 'student2@university.edu', 'Student Two', '$2a$10$elqCZA.FTqC7d.5KoTiCpeLdYvvw0oIxU0xxIWrOpUbw.z0jqZhXO', 'User', 0, 1, NOW(), 'S00002'),
(4, 'teacher1', 'teacher1@university.edu', 'Teacher One', '$2a$10$elqCZA.FTqC7d.5KoTiCpeLdYvvw0oIxU0xxIWrOpUbw.z0jqZhXO', 'User', 0, 1, NOW(), 'T00001'),
(5, 'teacher2', 'teacher2@university.edu', 'Teacher Two', '$2a$10$elqCZA.FTqC7d.5KoTiCpeLdYvvw0oIxU0xxIWrOpUbw.z0jqZhXO', 'User', 0, 1, NOW(), 'T00002');

-- add user roles
INSERT INTO user_roles (user_id, role_name) VALUES
(1, 'ROLE_ADMIN'),
(1, 'ROLE_USER'),
(2, 'ROLE_USER'),
(3, 'ROLE_USER'),
(4, 'ROLE_USER'),
(5, 'ROLE_USER');

-- add meeting rooms data
INSERT INTO meeting_rooms (id, room_name, type, capacity, location, floor, has_projector, 
                          has_screen, has_speaker, has_computer, has_whiteboard, 
                          description, created_at, updated_at, available) VALUES
(1, 'A101', 'LARGE_CLASSROOM', 50, 'Building A', 1, 1, 1, 1, 0, 1, 'Large lecture hall, suitable for course demonstrations and lectures', NOW(), NOW(), 1),
(2, 'A102', 'LARGE_CLASSROOM', 20, 'Building A', 1, 1, 1, 1, 0, 0, 'Large meeting room, suitable for team discussions and presentations', NOW(), NOW(), 1),
(3, 'A201', 'LARGE_CLASSROOM', 30, 'Building A', 2, 1, 1, 1, 0, 0, 'Large lecture hall, suitable for course demonstrations and lectures', NOW(), NOW(), 1),
(4, 'A202', 'SMALL_CLASSROOM', 10, 'Building A', 2, 1, 1, 0, 0, 1, 'Medium-sized meeting room, suitable for small group discussions', NOW(), NOW(), 1),
(5, 'A301', 'SMALL_CLASSROOM', 10, 'Building A', 3, 1, 1, 0, 0, 1, 'Medium-sized meeting room, suitable for small group discussions', NOW(), NOW(), 1),
(6, 'B101', 'LARGE_CLASSROOM', 50, 'Building B', 1, 1, 1, 1, 0, 1, 'Large lecture hall, suitable for course demonstrations and lectures', NOW(), NOW(), 1),
(7, 'B201', 'COMPUTER_LAB', 15, 'Building B', 2, 1, 1, 1, 1, 0, 'Multimedia classroom, suitable for teaching demonstrations', NOW(), NOW(), 1),
(8, 'B202', 'COMPUTER_LAB', 20, 'Building B', 2, 1, 1, 1, 1, 0, 'Multimedia classroom, suitable for teaching demonstrations', NOW(), NOW(), 1),
(9, 'B301', 'SMALL_CLASSROOM', 6, 'Building B', 3, 0, 0, 0, 0, 1, 'Small meeting room, suitable for brief discussions', NOW(), NOW(), 1),
(10, 'B302', 'SMALL_CLASSROOM', 4, 'Building B', 3, 0, 0, 0, 0, 0, 'Small meeting room, suitable for brief discussions', NOW(), NOW(), 1);

-- add appointment record (using relative time to ensure the data is always valid)
-- appointment of Student 1
INSERT INTO bookings (room_id, user_id, start_time, end_time, reason, status, created_at) VALUES
(1, 2, NOW() - INTERVAL 7 DAY + INTERVAL 10 HOUR, NOW() - INTERVAL 7 DAY + INTERVAL 12 HOUR, 'Course Discussion', 'APPROVED', NOW() - INTERVAL 10 DAY),
(3, 2, NOW() + INTERVAL 1 DAY + INTERVAL 13 HOUR, NOW() + INTERVAL 1 DAY + INTERVAL 15 HOUR, 'Group Meeting', 'APPROVED', NOW() - INTERVAL 2 DAY),
(5, 2, NOW() - INTERVAL 3 DAY + INTERVAL 9 HOUR, NOW() - INTERVAL 3 DAY + INTERVAL 10 HOUR, 'Brief Discussion', 'CANCELLED', NOW() - INTERVAL 5 DAY);

-- appointment of Student 2
INSERT INTO bookings (room_id, user_id, start_time, end_time, reason, status, created_at) VALUES
(2, 3, NOW() - INTERVAL 5 DAY + INTERVAL 14 HOUR, NOW() - INTERVAL 5 DAY + INTERVAL 16 HOUR, 'Course Demonstration', 'APPROVED', NOW() - INTERVAL 7 DAY),
(4, 3, NOW() + INTERVAL 2 DAY + INTERVAL 10 HOUR, NOW() + INTERVAL 2 DAY + INTERVAL 12 HOUR, 'Group Discussion', 'REJECTED', NOW() - INTERVAL 3 DAY),
(6, 3, NOW() + INTERVAL 3 DAY + INTERVAL 15 HOUR, NOW() + INTERVAL 3 DAY + INTERVAL 17 HOUR, 'Project Presentation', 'APPROVED', NOW() - INTERVAL 1 DAY);

-- appointment of Teacher 1
INSERT INTO bookings (room_id, user_id, start_time, end_time, reason, status, created_at) VALUES
(1, 4, NOW() - INTERVAL 2 DAY + INTERVAL 13 HOUR, NOW() - INTERVAL 2 DAY + INTERVAL 15 HOUR, 'Teaching Research Meeting', 'APPROVED', NOW() - INTERVAL 4 DAY),
(2, 4, NOW() + INTERVAL 4 DAY + INTERVAL 9 HOUR, NOW() + INTERVAL 4 DAY + INTERVAL 12 HOUR, 'Faculty Lecture', 'APPROVED', NOW() - INTERVAL 2 DAY),
(3, 4, NOW() - INTERVAL 1 DAY + INTERVAL 10 HOUR, NOW() - INTERVAL 1 DAY + INTERVAL 12 HOUR, 'Course Tutoring', 'APPROVED', NOW() - INTERVAL 3 DAY);

-- appointment of Teacher 2
INSERT INTO bookings (room_id, user_id, start_time, end_time, reason, status, created_at) VALUES
(1, 5, NOW() + INTERVAL 5 DAY + INTERVAL 13 HOUR, NOW() + INTERVAL 5 DAY + INTERVAL 15 HOUR, 'Department Meeting', 'APPROVED', NOW() - INTERVAL 1 DAY),
(6, 5, NOW() - INTERVAL 4 DAY + INTERVAL 14 HOUR, NOW() - INTERVAL 4 DAY + INTERVAL 16 HOUR, 'Multimedia Course', 'APPROVED', NOW() - INTERVAL 6 DAY),
(4, 5, NOW() + INTERVAL 7 DAY + INTERVAL 10 HOUR, NOW() + INTERVAL 7 DAY + INTERVAL 12 HOUR, 'Teaching Research Group', 'REJECTED', NOW() - INTERVAL 2 DAY);

-- add more reservations to demonstrate the statistical effect
-- today appointments
INSERT INTO bookings (room_id, user_id, start_time, end_time, reason, status, created_at) VALUES
(1, 2, NOW() + INTERVAL 8 HOUR, NOW() + INTERVAL 10 HOUR, 'Today Meeting 1', 'APPROVED', NOW() - INTERVAL 1 DAY),
(2, 3, NOW() + INTERVAL 10 HOUR, NOW() + INTERVAL 12 HOUR, 'Today Meeting 2', 'APPROVED', NOW() - INTERVAL 1 DAY),
(3, 4, NOW() + INTERVAL 14 HOUR, NOW() + INTERVAL 16 HOUR, 'Today Meeting 3', 'APPROVED', NOW() - INTERVAL 1 DAY);

-- tomorrow appointments
INSERT INTO bookings (room_id, user_id, start_time, end_time, reason, status, created_at) VALUES
(4, 2, NOW() + INTERVAL 1 DAY + INTERVAL 9 HOUR, NOW() + INTERVAL 1 DAY + INTERVAL 11 HOUR, 'Tomorrow Meeting 1', 'APPROVED', NOW() - INTERVAL 2 DAY),
(5, 3, NOW() + INTERVAL 1 DAY + INTERVAL 13 HOUR, NOW() + INTERVAL 1 DAY + INTERVAL 15 HOUR, 'Tomorrow Meeting 2', 'APPROVED', NOW() - INTERVAL 2 DAY);

-- yesterday appointments
INSERT INTO bookings (room_id, user_id, start_time, end_time, reason, status, created_at) VALUES
(6, 4, NOW() - INTERVAL 1 DAY + INTERVAL 10 HOUR, NOW() - INTERVAL 1 DAY + INTERVAL 12 HOUR, 'Yesterday Meeting 1', 'APPROVED', NOW() - INTERVAL 3 DAY),
(1, 5, NOW() - INTERVAL 1 DAY + INTERVAL 14 HOUR, NOW() - INTERVAL 1 DAY + INTERVAL 16 HOUR, 'Yesterday Meeting 2', 'APPROVED', NOW() - INTERVAL 3 DAY);

-- recover security setting
SET FOREIGN_KEY_CHECKS = 1;

-- confirm that the data has been inserted
SELECT 'DATA INITIALIZATION COMPLETE' AS message;
