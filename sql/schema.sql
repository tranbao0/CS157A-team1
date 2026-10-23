-- CourseCraft schema
-- CS157A-02 Fall 2026, Team 1
-- Run as an admin user. Drops and rebuilds the coursecraft database.
-- Then run seed.sql.

DROP DATABASE IF EXISTS coursecraft;
CREATE DATABASE coursecraft CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE coursecraft;

-- App account. Rows only, no schema changes.
CREATE USER IF NOT EXISTS 'coursecraft'@'localhost' IDENTIFIED BY 'coursecraft_dev';
GRANT SELECT, INSERT, UPDATE, DELETE ON coursecraft.* TO 'coursecraft'@'localhost';


-- ============================================================
-- Tables, in dependency order
-- ============================================================

-- One academic term, e.g. Fall 2026.
CREATE TABLE terms (
  term_id INT,
  year    INT         NOT NULL,
  season  VARCHAR(10) NOT NULL,
  PRIMARY KEY (term_id),
  UNIQUE (year, season),
  CHECK (season IN ('Fall', 'Spring', 'Summer', 'Winter'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- One session inside a term, e.g. Regular or Summer Session 1.
CREATE TABLE sessions (
  session_id INT,
  term_id    INT NOT NULL,
  name       VARCHAR(40),
  start_date DATE,
  end_date   DATE,
  PRIMARY KEY (session_id),
  UNIQUE (term_id, name),
  FOREIGN KEY (term_id) REFERENCES terms (term_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- One GE area, e.g. 1A Written Communication I.
CREATE TABLE ge_areas (
  code VARCHAR(8),
  name VARCHAR(120) NOT NULL,
  PRIMARY KEY (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- One catalog course. CS 157A is one row, many sections.
-- ge_area_code: the one GE area this course satisfies, if any.
-- A course that satisfies two areas stores the main one.
CREATE TABLE courses (
  course_id     INT,
  subject       VARCHAR(8)   NOT NULL,
  course_number VARCHAR(8)   NOT NULL,
  title         VARCHAR(160) NOT NULL,
  units         REAL         NOT NULL,
  description   VARCHAR(1000),
  ge_area_code  VARCHAR(8),
  PRIMARY KEY (course_id),
  UNIQUE (subject, course_number),
  FOREIGN KEY (ge_area_code) REFERENCES ge_areas (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- One offering of a course in a session. Never deleted, only cancelled.
-- class_number is SJSU's registration number. Unique within a session.
CREATE TABLE sections (
  section_id          INT,
  course_id           INT         NOT NULL,
  session_id          INT         NOT NULL,
  class_number        INT         NOT NULL,
  section_code        VARCHAR(6)  NOT NULL,
  mode_of_instruction VARCHAR(20),
  type                VARCHAR(10) NOT NULL,
  status              VARCHAR(10) NOT NULL DEFAULT 'Active',
  PRIMARY KEY (section_id),
  UNIQUE (session_id, class_number),
  FOREIGN KEY (course_id)  REFERENCES courses (course_id),
  FOREIGN KEY (session_id) REFERENCES sessions (session_id),
  CHECK (status IN ('Active', 'Cancelled'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- One seat count for a section at one point in time.
CREATE TABLE enrollment_snapshots (
  section_id     INT      NOT NULL,
  captured_at    DATETIME NOT NULL,
  seat_capacity  INT,
  enrolled_count INT,
  open_seats     INT,
  waitlist_count INT,
  PRIMARY KEY (section_id, captured_at),
  FOREIGN KEY (section_id) REFERENCES sections (section_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- One degree program, e.g. Computer Science BS.
CREATE TABLE majors (
  major_id   INT,
  code       VARCHAR(10)  NOT NULL,
  name       VARCHAR(120) NOT NULL,
  department VARCHAR(80),
  PRIMARY KEY (major_id),
  UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- One major's requirements for one catalog year.
CREATE TABLE roadmaps (
  roadmap_id   INT,
  major_id     INT NOT NULL,
  catalog_year INT NOT NULL,
  PRIMARY KEY (roadmap_id),
  UNIQUE (major_id, catalog_year),
  FOREIGN KEY (major_id) REFERENCES majors (major_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- One course a roadmap requires.
-- suggested_term_position: 1 = first term, 8 = last term.
CREATE TABLE requires_course (
  roadmap_id              INT NOT NULL,
  course_id               INT NOT NULL,
  suggested_term_position INT,
  requirement_type        VARCHAR(20),
  PRIMARY KEY (roadmap_id, course_id),
  FOREIGN KEY (roadmap_id) REFERENCES roadmaps (roadmap_id),
  FOREIGN KEY (course_id)  REFERENCES courses (course_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- One GE area a roadmap requires.
CREATE TABLE requires_ge_area (
  roadmap_id   INT        NOT NULL,
  ge_area_code VARCHAR(8) NOT NULL,
  PRIMARY KEY (roadmap_id, ge_area_code),
  FOREIGN KEY (roadmap_id)   REFERENCES roadmaps (roadmap_id),
  FOREIGN KEY (ge_area_code) REFERENCES ge_areas (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Registered user. Password never stored, only its hash.
-- student_id is AUTO_INCREMENT so sign-up gets new ID from MySQL.
CREATE TABLE students (
  student_id            INT AUTO_INCREMENT,
  email                 VARCHAR(160) NOT NULL,
  password_hash         VARCHAR(255) NOT NULL,
  roadmap_id            INT,
  expected_grad_term_id INT,
  PRIMARY KEY (student_id),
  UNIQUE (email),
  FOREIGN KEY (roadmap_id)            REFERENCES roadmaps (roadmap_id),
  FOREIGN KEY (expected_grad_term_id) REFERENCES terms (term_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- One prerequisite group of a course for one catalog year.
-- The course needs every group. Any one course in a group is enough.
CREATE TABLE prerequisite_groups (
  group_id     INT,
  course_id    INT NOT NULL,
  catalog_year INT NOT NULL,
  PRIMARY KEY (group_id),
  FOREIGN KEY (course_id) REFERENCES courses (course_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- One course that satisfies a prerequisite group.
CREATE TABLE counts_towards (
  group_id  INT NOT NULL,
  course_id INT NOT NULL,
  PRIMARY KEY (group_id, course_id),
  FOREIGN KEY (group_id)  REFERENCES prerequisite_groups (group_id),
  FOREIGN KEY (course_id) REFERENCES courses (course_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- One course a student has taken.
-- taken_and_passed: 1 = C- or better, or in progress. 0 = not passed.
CREATE TABLE completed (
  student_id       INT     NOT NULL,
  course_id        INT     NOT NULL,
  taken_and_passed TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (student_id, course_id),
  FOREIGN KEY (student_id) REFERENCES students (student_id),
  FOREIGN KEY (course_id)  REFERENCES courses (course_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- One course a student plans to take in a session.
CREATE TABLE plan_entries (
  plan_entry_id INT,
  student_id    INT      NOT NULL,
  course_id     INT      NOT NULL,
  session_id    INT      NOT NULL,
  added_at      DATETIME NOT NULL,
  PRIMARY KEY (plan_entry_id),
  FOREIGN KEY (student_id) REFERENCES students (student_id),
  FOREIGN KEY (course_id)  REFERENCES courses (course_id),
  FOREIGN KEY (session_id) REFERENCES sessions (session_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- One bottleneck risk result for a course in a term.
CREATE TABLE bottleneck_scores (
  score_id               INT,
  course_id              INT      NOT NULL,
  term_id                INT      NOT NULL,
  computed_at            DATETIME NOT NULL,
  waitlist_count         INT,
  total_capacity         INT,
  section_count          INT,
  dependent_course_count INT,
  risk_level             VARCHAR(10),
  score                  REAL,
  PRIMARY KEY (score_id),
  FOREIGN KEY (course_id) REFERENCES courses (course_id),
  FOREIGN KEY (term_id)   REFERENCES terms (term_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
