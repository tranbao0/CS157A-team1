-- CourseCraft schema
-- CS157A-02 Fall 2026, Team 1
-- TODO 9/29: cut-down schema for the demo. Full design to follow.

DROP DATABASE IF EXISTS coursecraft;
CREATE DATABASE coursecraft;
USE coursecraft;

-- App account. Rows only, no schema changes.
CREATE USER IF NOT EXISTS 'coursecraft'@'localhost' IDENTIFIED BY 'coursecraft_dev';
GRANT SELECT, INSERT, UPDATE, DELETE ON coursecraft.* TO 'coursecraft'@'localhost';

-- One row per course. CS 157A is one row, many sections.
CREATE TABLE course (
  course_id     INT,
  subject       VARCHAR(8),
  course_number VARCHAR(8),
  title         VARCHAR(160),
  units         REAL,
  PRIMARY KEY (course_id),
  UNIQUE (subject, course_number)
);

-- One offering of a course.
-- class_number is SJSU's registration number. Unique within a term.
CREATE TABLE section (
  class_number INT,
  course_id    INT,          -- TODO: add FOREIGN KEY
  section_code VARCHAR(6),
  days         VARCHAR(10),  -- TODO: split days
  start_time   TIME,
  end_time     TIME,
  instructor   VARCHAR(120), -- TODO: allow two
  open_seats   INT,
  PRIMARY KEY (class_number) -- TODO: term-scoped
);

-- Registered user. Password never stored.
-- TODO: unused
CREATE TABLE student (
  student_id    INT,
  email         VARCHAR(160),
  password_hash VARCHAR(255),
  full_name     VARCHAR(120),
  PRIMARY KEY (student_id),
  UNIQUE (email)
);
