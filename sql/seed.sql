-- CourseCraft sample data
-- CS157A-02 Fall 2026, Team 1

-- Sample data for the Fall 2026 catalog page.
-- AE 107 from the SJSU schedule. The rest invented.
-- TODO: replace with scraped data.

USE coursecraft;

-- Clear for re-runs. WHERE is required by safe update mode.

DELETE FROM section WHERE class_number > 0;
DELETE FROM course  WHERE course_id > 0;
DELETE FROM student WHERE student_id > 0;

INSERT INTO course (course_id, subject, course_number, title, units) VALUES
  (1, 'AE',   '107',  'Programming and AI for Aerospace Applications', 2.0),
  (2, 'AAS',  '1',    'Introduction to Asian American Studies',        3.0),
  (3, 'CS',   '146',  'Data Structures and Algorithms',                3.0),
  (4, 'CS',   '151',  'Object-Oriented Design',                        3.0),
  (5, 'CS',   '157A', 'Introduction to Database Management Systems',   3.0),
  (6, 'CS',   '166',  'Information Security',                          3.0),
  (7, 'MATH', '42',   'Discrete Mathematics',                          3.0);

INSERT INTO section
  (class_number, course_id, section_code, days, start_time, end_time, instructor, open_seats)
VALUES
  -- AE 107, from the published SJSU schedule
  (47817, 1, '02', 'M',  '15:00:00', '17:50:00', 'Ang Li',          0),
  (47818, 1, '03', 'W',  '15:00:00', '17:50:00', 'Ang Li',          0),
  (47819, 1, '04', 'F',  '09:00:00', '11:50:00', 'Ang Li',          0),

  -- AAS 1
  (47822, 2, '01', 'MW', '09:00:00', '10:15:00', 'Joanne Rondilla', 0),
  (47824, 2, '03', 'MW', '12:00:00', '13:15:00', 'Trung Nguyen',    0),
  (47825, 2, '04', 'MW', '13:30:00', '14:45:00', 'Joanne Rondilla', 0),
  (47826, 2, '05', 'MW', '15:00:00', '16:15:00', 'Lan Nguyen',      4),

  -- CS
  (48113, 3, '02', 'TR', '09:00:00', '10:15:00', 'Kelly Zhao',      0),
  (48120, 4, '01', 'TR', '13:30:00', '14:45:00', 'Ahmed Patel',    12),
  (48201, 5, '01', 'MW', '15:00:00', '16:15:00', 'Mike Wu',         3),
  (48202, 5, '02', 'MW', '16:30:00', '17:45:00', 'Mike Wu',         7),
  (48244, 6, '02', 'MW', '12:00:00', '13:15:00', 'Sayma Akther',    0),

  -- MATH
  (46510, 7, '03', 'TR', '10:30:00', '11:45:00', 'Wasin So',       18);
