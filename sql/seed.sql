-- CourseCraft sample data
-- CS157A-02 Fall 2026, Team 1
-- Run after schema.sql.

USE coursecraft;

-- ============================================================
-- Sample data
-- GE areas, and the titles and GE areas of GE courses, follow
-- the SJSU 2026-2027 catalog. Other data is sample data, not
-- verified against the live SJSU catalog.
-- Computer Science BS and Software Engineering BS 2025 roadmaps,
-- their courses and 2025 prerequisites follow SJSU 2025-2026 catalog.
-- Math and Writing Enrollment Category and PPA score rules live in
-- placement_categories, group_accepts_category and student_placements.
-- Students are invented. Every sample student signs in with password123.
-- ============================================================

INSERT INTO terms (term_id, year, season) VALUES
  (1,  2024, 'Fall'),
  (2,  2025, 'Spring'),
  (3,  2025, 'Summer'),
  (4,  2025, 'Fall'),
  (5,  2026, 'Spring'),
  (6,  2026, 'Summer'),
  (7,  2026, 'Fall'),
  (8,  2027, 'Winter'),
  (9,  2027, 'Spring'),
  (10, 2027, 'Summer'),
  (11, 2027, 'Fall'),
  (12, 2028, 'Spring'),
  -- later terms let 4-year demo plans reach graduation
  (13, 2028, 'Fall'),
  (14, 2029, 'Spring'),
  (15, 2029, 'Fall'),
  (16, 2030, 'Spring');

INSERT INTO sessions (session_id, term_id, name, start_date, end_date) VALUES
  (1,  1,  'Regular',          '2024-08-21', '2024-12-16'),
  (2,  2,  'Regular',          '2025-01-23', '2025-05-19'),
  (3,  3,  'Summer Session 1', '2025-06-02', '2025-07-11'),
  (4,  3,  'Summer Session 2', '2025-07-14', '2025-08-15'),
  (5,  4,  'Regular',          '2025-08-20', '2025-12-15'),
  (6,  5,  'Regular',          '2026-01-22', '2026-05-18'),
  (7,  6,  'Summer Session 1', '2026-06-01', '2026-07-10'),
  (8,  6,  'Summer Session 2', '2026-07-13', '2026-08-14'),
  (9,  7,  'Regular',          '2026-08-19', '2026-12-14'),
  (10, 8,  'Winter Session',   '2027-01-04', '2027-01-22'),
  (11, 9,  'Regular',          '2027-01-25', '2027-05-17'),
  (12, 11, 'Regular',          '2027-08-18', '2027-12-13'),
  (13, 12, 'Regular',          '2028-01-24', '2028-05-15'),
  (14, 10, 'Summer Session 1', '2027-06-01', '2027-07-09'),
  (15, 13, 'Regular',          '2028-08-23', '2028-12-18'),
  (16, 14, 'Regular',          '2029-01-24', '2029-05-21'),
  (17, 15, 'Regular',          '2029-08-22', '2029-12-17'),
  (18, 16, 'Regular',          '2030-01-23', '2030-05-20');

-- Core GE areas, then upper division (UD) SJSU Studies areas.
INSERT INTO ge_areas (code, name) VALUES
  ('1A',    'Written Communication I'),
  ('1B',    'Critical Thinking and Writing'),
  ('1C',    'Oral Communication'),
  ('2',     'Mathematical Concepts and Quantitative Reasoning'),
  ('3A',    'Arts'),
  ('3B',    'Humanities'),
  ('4',     'Social and Behavioral Sciences'),
  ('5A',    'Physical Science'),
  ('5B',    'Life Science'),
  ('5C',    'Laboratory'),
  ('6',     'Ethnic Studies'),
  ('UD2/5', 'Earth, Environment, and Sustainability'),
  ('UD3',   'Cultures and Global Understanding'),
  ('UD4',   'Self, Society, and Equality in the U.S.');

INSERT INTO courses (course_id, subject, course_number, title, units, description, ge_area_code) VALUES
  (1,  'CS',   '46A',  'Introduction to Programming',                     4.0, 'Basic programming in Java. Variables, control flow, methods, arrays and simple classes.', NULL),
  (2,  'CS',   '46B',  'Introduction to Data Structures',                 4.0, 'Lists, stacks, queues, trees and hashing. Recursion and basic algorithm analysis.', NULL),
  (3,  'CS',   '47',   'Introduction to Computer Systems',                3.0, 'Data representation, assembly language and how programs run on hardware.', NULL),
  (4,  'CS',   '49J',  'Programming in Java',                             3.0, 'Java for students who already program in another language.', NULL),
  (5,  'CS',   '100W', 'Technical Writing Workshop',                      3.0, 'Writing for computing professionals.', NULL),
  (6,  'CS',   '146',  'Data Structures and Algorithms',                  3.0, 'Sorting, searching, graphs and algorithm design techniques.', NULL),
  (7,  'CS',   '149',  'Operating Systems',                               3.0, 'Processes, threads, scheduling, memory management and file systems.', NULL),
  (8,  'CS',   '151',  'Object-Oriented Design',                          3.0, 'Object-oriented analysis and design, UML and design patterns.', NULL),
  (9,  'CS',   '152',  'Programming Paradigms',                           3.0, 'Functional, logic and object-oriented languages.', NULL),
  (10, 'CS',   '154',  'Formal Languages and Computability',              3.0, 'Automata, grammars, Turing machines and decidability.', NULL),
  (11, 'CS',   '157A', 'Introduction to Database Management Systems',     3.0, 'Relational model, SQL, ER design and normalization.', NULL),
  (12, 'CS',   '160',  'Software Engineering',                            3.0, 'Team software projects, requirements, testing and process.', NULL),
  (13, 'CS',   '166',  'Information Security',                            3.0, 'Cryptography, access control and software security.', NULL),
  (14, 'MATH', '19',   'Precalculus',                                     5.0, NULL, '2'),
  (15, 'MATH', '30',   'Calculus I',                                      3.0, NULL, '2'),
  (16, 'MATH', '31',   'Calculus II',                                     4.0, NULL, '2'),
  (17, 'MATH', '32',   'Calculus III',                                    3.0, NULL, '2'),
  (18, 'MATH', '39',   'Linear Algebra I',                                3.0, NULL, NULL),
  (19, 'MATH', '42',   'Discrete Mathematics',                            3.0, NULL, NULL),
  (20, 'PHYS', '50',   'General Physics I: Mechanics',                    4.0, NULL, '5A'),
  (21, 'PHYS', '51',   'General Physics II: Electricity and Magnetism',   4.0, NULL, '5A'),
  (22, 'ENGL', '1A',   'First Year Writing',                              3.0, NULL, '1A'),
  (23, 'COMM', '20',   'Public Speaking',                                 3.0, NULL, '1C'),
  (24, 'PHIL', '57',   'Logic and Critical Reasoning',                    3.0, NULL, '1B'),
  (25, 'BIOL', '10',   'The Living World',                                3.0, NULL, '5B'),
  (26, 'HIST', '15',   'Essentials of U.S. History',                      3.0, NULL, '4'),
  (27, 'AAS',  '1',    'Introduction to Asian American Studies',          3.0, NULL, '6'),
  (28, 'CMPE', '30',   'Programming Concepts and Methodology',            3.0, NULL, NULL),
  (29, 'MATH', '161A', 'Applied Probability and Statistics I',            3.0, NULL, NULL),
  (30, 'CS',   '158A', 'Computer Networks',                               3.0, NULL, NULL),
  (31, 'MUSC', '10A',  'Music Appreciation',                              3.0, NULL, '3A'),
  (32, 'PHIL', '10',   'Introduction to Philosophy',                      3.0, NULL, '3B'),
  -- rows below fill full CS and SE 2025 roadmaps, units and GE areas come from 2025-2026 catalog
  (33, 'CS',   '147',  'Computer Architecture',                          3.0, 'How processors, memory and input/output work together, and how that affects program speed.', NULL),
  (34, 'PHIL', '134',  'Computers, Ethics and Society',                  3.0, 'Ethical and social questions raised by computing technology.', 'UD3'),
  (35, 'BIOL', '30',   'Principles of Biology I',                        4.0, 'Cells, genetics and evolution, with lab.', '5B'),
  (36, 'MATH', '142',  'Introduction to Combinatorics',                  3.0, 'Counting methods, recurrences and graph basics.', NULL),
  (37, 'ENGL', '1B',   'Argument and Analysis',                          3.0, 'Second semester writing focused on argument and reading.', '3B'),
  (38, 'ENGR', '10',   'Introduction to Engineering',                    3.0, 'Engineering fields, design process and team projects.', NULL),
  (39, 'CMPE', '131',  'Software Engineering I',                         3.0, 'Requirements, design and team development of a software project.', NULL),
  (40, 'CMPE', '102',  'Assembly Language Programming',                  3.0, 'Machine level programming and how high level code maps to it.', NULL),
  (41, 'CMPE', '120',  'Computer Organization and Architecture',         3.0, 'Data paths, memory hierarchy and instruction sets.', NULL),
  (42, 'CMPE', '133',  'Software Engineering II',                        3.0, 'Architecture, testing and maintenance of larger software systems.', NULL),
  (43, 'ISE',  '130',  'Engineering Probability and Statistics',         3.0, 'Probability and statistics for engineering problems.', NULL),
  (44, 'CMPE', '148',  'Computer Networks I',                            3.0, 'Network layers, protocols and socket programming.', NULL),
  (45, 'ENGR', '100W', 'Engineering Reports',                            3.0, 'Technical writing and reports for engineers.', 'UD2/5'),
  (46, 'MATH', '33LA', 'Differential Equations and Linear Algebra',      3.0, 'Linear algebra and ordinary differential equations for engineers.', NULL),
  (47, 'CMPE', '165',  'Software Engineering Process Management',        3.0, 'Planning, estimating and managing software projects.', NULL),
  (48, 'CMPE', '172',  'Enterprise Software Platforms',                  3.0, 'Building and running large scale web and enterprise systems.', NULL),
  (49, 'CMPE', '187',  'Software Quality Engineering',                   3.0, 'Software testing methods and quality assurance.', NULL),
  (50, 'CMPE', '195A', 'Senior Design Project I',                        2.0, 'First half of year-long team capstone project.', 'UD4'),
  (51, 'ENGR', '195A', 'Global and Social Issues in Engineering',        1.0, 'Social and global side of engineering, taken with capstone.', 'UD4'),
  (52, 'CMPE', '195B', 'Senior Design Project II',                       3.0, 'Second half of year-long team capstone project.', 'UD3'),
  (53, 'ENGR', '195B', 'Global and Social Issues in Engineering',        1.0, 'Second part of engineering and society seminar.', 'UD3'),
  (54, 'ISE',  '164',  'Computer and Human Interaction',                 3.0, 'Designing and testing user interfaces around human needs.', NULL),
  (55, 'CS',   '156',  'Introduction to Artificial Intelligence',        3.0, 'Search, reasoning and learning methods in AI.', NULL),
  (56, 'CS',   '122',  'Advanced Programming with Python',               3.0, 'Python for data handling, scripting and larger programs.', NULL),
  (57, 'CS',   '171',  'Introduction to Machine Learning',               3.0, 'Supervised and unsupervised learning algorithms.', NULL),
  (58, 'CS',   '174',  'Server-side Web Programming',                    3.0, 'Building web applications on server side.', NULL),
  (59, 'POLS', '15',   'Essentials of U.S. & California Government',     3.0, NULL, '4'),
  (60, 'ENVS', '152',  'Globalization and the Environment',              3.0, NULL, 'UD2/5'),
  (61, 'COMM', '174',  'Intercultural Comm and Structural Inequalities', 3.0, NULL, 'UD4'),
  (62, 'KIN',  '2A',   'Beginning Swimming',                             1.0, NULL, NULL),
  (63, 'KIN',  '2B',   'Intermediate Swimming',                          1.0, NULL, NULL),
  (64, 'CS',   '155',  'Introduction to the Design and Analysis of Algorithms', 3.0, 'Dynamic programming, greedy methods, NP-completeness and other algorithm design topics.', NULL);

-- Session 6 = Spring 2026, 7 = Summer 2026 S1, 9 = Fall 2026,
-- 10 = Winter 2027, 11 = Spring 2027.
INSERT INTO sections
  (section_id, course_id, session_id, class_number, section_code, mode_of_instruction, type, status)
VALUES
  -- Fall 2026
  (1,  1,  9,  48001, '01', 'In Person', 'LEC', 'Active'),
  (2,  1,  9,  48002, '02', 'In Person', 'LEC', 'Active'),
  (3,  1,  9,  48003, '11', 'In Person', 'LAB', 'Active'),
  (4,  2,  9,  48010, '01', 'In Person', 'LEC', 'Active'),
  (5,  2,  9,  48011, '02', 'Hybrid',    'LEC', 'Active'),
  (6,  3,  9,  48020, '01', 'In Person', 'LEC', 'Active'),
  (7,  6,  9,  48113, '01', 'In Person', 'LEC', 'Active'),
  (8,  6,  9,  48114, '02', 'In Person', 'LEC', 'Active'),
  (9,  6,  9,  48115, '03', 'Online',    'LEC', 'Cancelled'),
  (10, 8,  9,  48120, '01', 'In Person', 'LEC', 'Active'),
  (11, 7,  9,  48130, '01', 'In Person', 'LEC', 'Active'),
  (12, 11, 9,  48201, '01', 'In Person', 'LEC', 'Active'),
  (13, 11, 9,  48202, '02', 'In Person', 'LEC', 'Active'),
  (14, 12, 9,  48210, '01', 'In Person', 'LEC', 'Active'),
  (15, 13, 9,  48244, '02', 'Hybrid',    'LEC', 'Active'),
  (16, 19, 9,  46510, '03', 'In Person', 'LEC', 'Active'),
  (17, 15, 9,  46400, '01', 'In Person', 'LEC', 'Active'),
  (18, 16, 9,  46420, '01', 'In Person', 'LEC', 'Active'),
  (19, 20, 9,  46700, '01', 'In Person', 'LEC', 'Active'),
  (20, 20, 9,  46701, '11', 'In Person', 'LAB', 'Active'),
  (21, 5,  9,  48050, '01', 'Online',    'SEM', 'Active'),
  (22, 9,  9,  48140, '01', 'In Person', 'LEC', 'Active'),
  (23, 10, 9,  48150, '01', 'In Person', 'LEC', 'Active'),
  (24, 18, 9,  46440, '01', 'In Person', 'LEC', 'Active'),
  -- Spring 2026
  (25, 1,  6,  22001, '01', 'In Person', 'LEC', 'Active'),
  (26, 6,  6,  22113, '01', 'In Person', 'LEC', 'Active'),
  (27, 11, 6,  22201, '01', 'In Person', 'LEC', 'Active'),
  -- Summer 2026, Session 1
  (28, 15, 7,  70010, '01', 'Online',    'LEC', 'Active'),
  (29, 2,  7,  70020, '01', 'Online',    'LEC', 'Cancelled'),
  -- Spring 2027, posted early
  (30, 6,  11, 23113, '01', 'In Person', 'LEC', 'Active'),
  (31, 11, 11, 23201, '01', 'In Person', 'LEC', 'Active'),
  (32, 12, 11, 23210, '01', 'In Person', 'LEC', 'Active'),
  (33, 8,  11, 23120, '01', 'In Person', 'LEC', 'Active'),
  -- Winter 2027
  (34, 23, 10, 90010, '01', 'Online',    'LEC', 'Active');

-- open_seats = seat_capacity - enrolled_count. Waitlist only when full.
INSERT INTO enrollment_snapshots
  (section_id, captured_at, seat_capacity, enrolled_count, open_seats, waitlist_count)
VALUES
  -- CS 146-01, fills up before the term
  (7,  '2026-08-01 08:00:00', 40, 32,  8,  0),
  (7,  '2026-08-15 08:00:00', 40, 40,  0,  6),
  (7,  '2026-08-26 08:00:00', 40, 40,  0, 11),
  (7,  '2026-09-28 08:00:00', 40, 40,  0,  9),
  -- CS 146-02
  (8,  '2026-08-01 08:00:00', 40, 28, 12,  0),
  (8,  '2026-08-15 08:00:00', 40, 38,  2,  0),
  (8,  '2026-08-26 08:00:00', 40, 40,  0,  4),
  (8,  '2026-09-28 08:00:00', 40, 40,  0,  3),
  -- CS 146-03, cancelled mid-August
  (9,  '2026-08-01 08:00:00', 35,  6, 29,  0),
  (9,  '2026-08-15 08:00:00',  0,  0,  0,  0),
  -- CS 157A-01 and -02
  (12, '2026-08-01 08:00:00', 45, 45,  0,  3),
  (12, '2026-08-26 08:00:00', 45, 45,  0,  7),
  (12, '2026-09-28 08:00:00', 45, 45,  0,  5),
  (13, '2026-08-01 08:00:00', 45, 40,  5,  0),
  (13, '2026-08-26 08:00:00', 45, 45,  0,  2),
  (13, '2026-09-28 08:00:00', 45, 44,  1,  0),
  -- Other Fall 2026 lectures, latest count only
  (1,  '2026-09-28 08:00:00', 60, 55,  5,  0),
  (2,  '2026-09-28 08:00:00', 60, 60,  0,  2),
  (4,  '2026-09-28 08:00:00', 50, 50,  0,  4),
  (5,  '2026-09-28 08:00:00', 50, 47,  3,  0),
  (10, '2026-09-28 08:00:00', 40, 28, 12,  0),
  (11, '2026-09-28 08:00:00', 40, 40,  0,  8),
  (14, '2026-09-28 08:00:00', 35, 35,  0,  2),
  (15, '2026-09-28 08:00:00', 40, 40,  0,  6),
  (16, '2026-09-28 08:00:00', 40, 22, 18,  0),
  (17, '2026-09-28 08:00:00', 45, 36,  9,  0),
  -- Spring 2026
  (25, '2026-01-20 08:00:00', 60, 58,  2,  0),
  (26, '2026-01-20 08:00:00', 40, 40,  0, 12),
  (27, '2026-01-20 08:00:00', 45, 45,  0,  4),
  -- Spring 2027, before registration opens
  (30, '2026-09-28 08:00:00', 40,  0, 40,  0);

INSERT INTO majors (major_id, code, name, department) VALUES
  (1,  'CS-BS',   'Computer Science, BS',     'Computer Science'),
  (2,  'SE-BS',   'Software Engineering, BS', 'Computer Engineering'),
  (3,  'CMPE-BS', 'Computer Engineering, BS', 'Computer Engineering'),
  (4,  'DS-BS',   'Data Science, BS',         'Mathematics and Statistics'),
  (5,  'MATH-BS', 'Applied Mathematics, BS',  'Mathematics and Statistics'),
  (6,  'MATH-BA', 'Mathematics, BA',          'Mathematics and Statistics'),
  (7,  'PHYS-BS', 'Physics, BS',              'Physics and Astronomy'),
  (8,  'BIOL-BS', 'Biological Science, BS',   'Biological Sciences'),
  (9,  'HIST-BA', 'History, BA',              'History'),
  (10, 'AE-BS',   'Aerospace Engineering, BS', 'Aerospace Engineering');

INSERT INTO roadmaps (roadmap_id, major_id, catalog_year) VALUES
  (1,  1,  2024),
  (2,  1,  2025),
  (3,  1,  2026),
  (4,  2,  2025),
  (5,  3,  2025),
  (6,  4,  2025),
  (7,  5,  2025),
  (8,  6,  2025),
  (9,  7,  2025),
  (10, 8,  2025),
  (11, 9,  2025),
  (12, 10, 2025);

INSERT INTO requires_course (roadmap_id, course_id, suggested_term_position, requirement_type) VALUES
  -- Computer Science, BS, 2025 (SJSU 2025-2026 roadmap, position 1 = first fall)
  -- Elective rows come from approved lists, one of MATH 32, 142 or 161A is required
  (2, 1,  1, 'Major'),
  (2, 15, 1, 'Support'),
  (2, 19, 1, 'Support'),
  (2, 2,  2, 'Major'),
  (2, 16, 2, 'Support'),
  (2, 3,  3, 'Major'),
  (2, 6,  3, 'Major'),
  (2, 20, 3, 'Support'),
  (2, 33, 4, 'Major'),
  (2, 8,  4, 'Major'),
  (2, 18, 4, 'Support'),
  (2, 58, 4, 'Elective'),
  (2, 7,  5, 'Major'),
  (2, 35, 5, 'Support'),
  (2, 56, 5, 'Elective'),
  (2, 9,  6, 'Major'),
  (2, 34, 6, 'Support'),
  (2, 5,  6, 'Major'),
  (2, 36, 6, 'Support'),
  (2, 17, 6, 'Elective'),
  (2, 29, 6, 'Elective'),
  (2, 55, 6, 'Elective'),
  (2, 11, 7, 'Major'),
  (2, 12, 7, 'Major'),
  (2, 13, 7, 'Major'),
  (2, 30, 7, 'Elective'),
  (2, 10, 8, 'Major'),
  (2, 57, 8, 'Elective'),
  (2, 64, 8, 'Elective'),
  -- Computer Science, BS, 2024
  (1, 1,  1, 'Major'),
  (1, 2,  2, 'Major'),
  (1, 6,  4, 'Major'),
  (1, 8,  4, 'Major'),
  (1, 11, 7, 'Elective'),
  -- Computer Science, BS, 2026
  (3, 1,  1, 'Major'),
  (3, 2,  2, 'Major'),
  (3, 6,  4, 'Major'),
  -- Software Engineering, BS, 2025 (SJSU 2025-2026 roadmap)
  -- Elective rows are technical upper division electives, two are required
  (4, 1,  1, 'Major'),
  (4, 38, 1, 'Major'),
  (4, 15, 1, 'Support'),
  (4, 2,  2, 'Major'),
  (4, 37, 2, 'Support'),
  (4, 19, 2, 'Support'),
  (4, 20, 2, 'Support'),
  (4, 25, 3, 'Support'),
  (4, 39, 3, 'Major'),
  (4, 16, 3, 'Support'),
  (4, 6,  4, 'Major'),
  (4, 17, 4, 'Support'),
  (4, 21, 4, 'Support'),
  (4, 40, 5, 'Major'),
  (4, 41, 5, 'Major'),
  (4, 42, 5, 'Major'),
  (4, 11, 5, 'Major'),
  (4, 29, 5, 'Support'),
  (4, 44, 6, 'Major'),
  (4, 7,  6, 'Major'),
  (4, 8,  6, 'Major'),
  (4, 45, 6, 'Major'),
  (4, 46, 6, 'Support'),
  (4, 47, 7, 'Major'),
  (4, 48, 7, 'Major'),
  (4, 49, 7, 'Major'),
  (4, 50, 7, 'Major'),
  (4, 13, 7, 'Major'),
  (4, 51, 7, 'Major'),
  (4, 52, 8, 'Major'),
  (4, 53, 8, 'Major'),
  (4, 54, 8, 'Major'),
  (4, 55, 8, 'Elective'),
  (4, 57, 8, 'Elective'),
  -- Data Science, BS, 2025
  (6, 1,  1, 'Major'),
  (6, 15, 1, 'Support'),
  (6, 16, 2, 'Support'),
  (6, 18, 3, 'Support'),
  (6, 29, 4, 'Major'),
  (6, 11, 5, 'Major'),
  -- Applied Mathematics, BS, 2025
  (7, 15, 1, 'Major'),
  (7, 16, 2, 'Major'),
  (7, 17, 3, 'Major'),
  (7, 18, 3, 'Major'),
  (7, 19, 2, 'Major'),
  -- Physics, BS, 2025
  (9, 15, 1, 'Support'),
  (9, 16, 2, 'Support'),
  (9, 20, 2, 'Major'),
  (9, 21, 3, 'Major');

INSERT INTO requires_ge_area (roadmap_id, ge_area_code) VALUES
  (2, '1A'), (2, '1B'), (2, '1C'), (2, '2'), (2, '3A'), (2, '3B'), (2, '4'),
  (2, '5A'), (2, '5B'), (2, '5C'), (2, '6'), (2, 'UD2/5'), (2, 'UD3'), (2, 'UD4'),
  (4, '1A'), (4, '1C'), (4, '2'), (4, '3A'), (4, '3B'), (4, '4'), (4, '5A'),
  (4, '5B'), (4, '5C'), (4, '6'), (4, 'UD2/5'), (4, 'UD3'), (4, 'UD4'),
  (8, '1A'), (8, '1B'), (8, '1C'), (8, '5A'), (8, '5B'), (8, '5C'),
  (11, '1A'), (11, '1C'), (11, '2'), (11, '4');

-- SJSU gives each new student one math and one writing category, both use same four levels.
INSERT INTO placement_categories (code, kind, name) VALUES
  ('M-I',   'Math',    'GE Area 2 already met'),
  ('M-II',  'Math',    'Ready for GE math without support'),
  ('M-III', 'Math',    'Ready for supported GE math, Early Start recommended'),
  ('M-IV',  'Math',    'Ready for supported GE math, Early Start strongly recommended'),
  ('W-I',   'Writing', 'GE Area 1A already met'),
  ('W-II',  'Writing', 'Ready for GE writing without support'),
  ('W-III', 'Writing', 'Ready for supported GE writing, Early Start recommended'),
  ('W-IV',  'Writing', 'Ready for supported GE writing, Early Start strongly recommended');

-- bcrypt hashes (cost 10) of password123, each with its own salt.
INSERT INTO students (student_id, email, password_hash, roadmap_id, expected_grad_term_id, ppa_score) VALUES
  (1,  'jordan.lee@example.com',     '$2a$10$h9lsr0tAogrc2FQFM4ApEOE2aMJxVa9FUhzKQ.SQ1turxU/vxGi7i', 2,    12, NULL),
  (2,  'maya.patel@example.com',     '$2a$10$bMEZsfPBfRq7g2.OubnhPOtw6dv/GLijIlYb5Zj.k.xbT6DYHXkfK', 2,    11, NULL),
  (3,  'sam.nguyen@example.com',     '$2a$10$YFoFhHE1yPESvHd7g0RtmeTwMG0V5/cCNxJ69LzRmRLt1mmLtIW36', NULL, 12, 50),
  (4,  'alex.garcia@example.com',    '$2a$10$0Z.qbnHOz4MvnxcVp7rp/eVwjm6S7PFcHtMDsgEQ6wOFSDu5ac.oy', 4,    12, NULL),
  (5,  'riley.chen@example.com',     '$2a$10$mlXmg8IPNECkKIK/kRWKoOXNEKSj6slGrSQCuHq4rNp/buvXEsG8.', 6,    NULL, NULL),
  (6,  'taylor.kim@example.com',     '$2a$10$s9C4bYccbn1ZqxXOPAXL9uSYYICFvSV9dm5gUvuIT/pmhISgPsvk2', 2,    9, NULL),
  (7,  'casey.brown@example.com',    '$2a$10$jC2oj37vr/yMStOyH6EmNexEAv9Q.A.uVfRFgdSpE00QL7N77q7dW', 7,    11, NULL),
  (8,  'morgan.davis@example.com',   '$2a$10$sd06pWppkeFkilr/mmZUZuBl5bTK5gsd8kCpFjy5llzl9EhBkdrly', 9,    12, NULL),
  (9,  'jamie.wilson@example.com',   '$2a$10$7kFpX5lYE76LpnE7RtngAe7l.GbI83SnxwegIRlqhwckXJHLryDKu', 2,    12, NULL),
  (10, 'drew.martinez@example.com',  '$2a$10$NtvoQdP/tObQkeYs.bEw0u68y1lwVwPA2XvGlDcbarQ6M/0eKI2X2', 1,    9, NULL),
  (11, 'avery.lopez@example.com',    '$2a$10$PYZnMF.6gv./lzXgS4kMpOO2l9Ng42/hgFbENpzwHhfxoPITrMYUq', 3,    12, NULL),
  (12, 'quinn.harris@example.com',   '$2a$10$.rsb7EMjdTL5wqByCRvRTOUqYMZP.PRdySQSkw4/fkfOzfi1ziM8e', 10,   11, NULL),
  -- demo freshmen have full 4-year plans (Fall 2026 to Spring 2030), see plan_entries
  (13, 'cs.roadmap@example.com',     '$2a$10$/9nStATbvuVAWddGy9tF8uoCs/6amPY47XAD8so8nDZ5W93SSCpH6', 2,    16, 85),
  (14, 'se.roadmap@example.com',     '$2a$10$wozJrwLJBU2rTPU78RZtj.u2lW84oVgLwZa/yIjRymO0AngkSImMa', 4,    16, 90);

-- Demo freshmen got placement from high school record. Sam placed low and took MATH 19 first.
-- Other sample students have no placement on file yet.
INSERT INTO student_placements (student_id, category_code) VALUES
  (3,  'M-IV'), (3,  'W-III'),
  (13, 'M-II'), (13, 'W-II'),
  (14, 'M-I'),  (14, 'W-I');

-- Catalog year 2025 unless noted.
INSERT INTO prerequisite_groups (group_id, course_id, catalog_year) VALUES
  (1,  2,  2025),  -- CS 46B: CS 46A
  (2,  2,  2025),  -- CS 46B: MATH 19, or M-I or M-II with PPA 70+
  (3,  3,  2025),  -- CS 47: CS 46B
  (4,  6,  2025),  -- CS 146: CS 46B
  (5,  6,  2025),  -- CS 146: MATH 42
  (6,  6,  2025),  -- CS 146: MATH 30
  (7,  8,  2025),  -- CS 151: CS 46B
  (8,  7,  2025),  -- CS 149: CS 146
  (9,  7,  2025),  -- CS 149: CS 47 or CMPE 102
  (10, 11, 2025),  -- CS 157A: CS 146
  (12, 12, 2025),  -- CS 160: CS 146
  (13, 12, 2025),  -- CS 160: CS 151
  (14, 9,  2025),  -- CS 152: CS 151
  (15, 10, 2025),  -- CS 154: CS 46B
  (16, 13, 2025),  -- CS 166: CS 146
  (17, 16, 2025),  -- MATH 31: MATH 30
  (18, 17, 2025),  -- MATH 32: MATH 31
  (19, 18, 2025),  -- MATH 39: MATH 31
  (20, 20, 2025),  -- PHYS 50: MATH 30
  (21, 21, 2025),  -- PHYS 51: PHYS 50
  (22, 21, 2025),  -- PHYS 51: MATH 31
  (24, 30, 2025),  -- CS 158A: CS 146
  (25, 5,  2025),  -- CS 100W: PHIL 57 or ENGL 1B (GE 1B or second writing course)
  (26, 29, 2025),  -- MATH 161A: MATH 31
  (27, 11, 2024),  -- CS 157A, 2024 catalog: CS 146 only
  -- rows below follow 2025-2026 catalog rules for CS and SE roadmap courses
  (28, 3,  2025),  -- CS 47: MATH 42
  (29, 8,  2025),  -- CS 151: MATH 42
  (30, 10, 2025),  -- CS 154: MATH 42
  (31, 12, 2025),  -- CS 160: CS 100W
  (32, 13, 2025),  -- CS 166: CS 47 or CMPE 102 or CMPE 120
  (33, 30, 2025),  -- CS 158A: CS 47
  (34, 33, 2025),  -- CS 147: CS 47 or CMPE 102
  (35, 36, 2025),  -- MATH 142: MATH 31
  (36, 36, 2025),  -- MATH 142: MATH 42
  (37, 37, 2025),  -- ENGL 1B: ENGL 1A
  (38, 39, 2025),  -- CMPE 131: CS 46B (SE major rule)
  (39, 40, 2025),  -- CMPE 102: CS 46B
  (40, 41, 2025),  -- CMPE 120: CS 46B
  (41, 42, 2025),  -- CMPE 133: CMPE 131
  (42, 43, 2025),  -- ISE 130: MATH 32
  (43, 44, 2025),  -- CMPE 148: CMPE 120 (SE major rule)
  (44, 44, 2025),  -- CMPE 148: CS 146 (SE major rule)
  (45, 45, 2025),  -- ENGR 100W: ENGL 1B
  (46, 46, 2025),  -- MATH 33LA: MATH 31
  (47, 47, 2025),  -- CMPE 165: CMPE 131
  (48, 48, 2025),  -- CMPE 172: CS 149
  (49, 49, 2025),  -- CMPE 187: CMPE 131
  (50, 51, 2025),  -- ENGR 195A: ENGR 100W
  (51, 52, 2025),  -- CMPE 195B: CMPE 195A
  (52, 53, 2025),  -- ENGR 195B: ENGR 195A
  (53, 55, 2025),  -- CS 156: CS 146
  (54, 56, 2025),  -- CS 122: CS 146
  (55, 57, 2025),  -- CS 171: CS 146
  (56, 58, 2025),  -- CS 174: CS 46B
  (57, 63, 2025),  -- KIN 2B: KIN 2A
  (58, 64, 2025),  -- CS 155: CS 146
  -- groups below also accept placement category, see group_accepts_category
  (59, 1,  2025),  -- CS 46A: M-I, M-II or M-III (MATH 1 not in sample data)
  (60, 15, 2025),  -- MATH 30: MATH 19, or M-I to M-III with PPA 70+, or M-IV with PPA 80+
  (61, 19, 2025),  -- MATH 42: MATH 19, or M-I to M-III with PPA 70+, or M-IV with PPA 80+
  (62, 20, 2025),  -- PHYS 50: ENGL 1A, or W-I, W-II or W-III
  (63, 35, 2025),  -- BIOL 30: ENGL 1A, or W-I or W-II
  (64, 35, 2025),  -- BIOL 30: GE Area 2 math course, or M-I or M-II
  (65, 38, 2025);  -- ENGR 10: ENGL 1A, or W-I or W-II

INSERT INTO counts_towards (group_id, course_id) VALUES
  (1, 1),
  (2, 14),
  (3, 2),
  (4, 2),
  (5, 19),
  (6, 15),
  (7, 2),
  (8, 6),
  (9, 3), (9, 40),
  (10, 6),
  (12, 6),
  (13, 8),
  (14, 8),
  (15, 2),
  (16, 6),
  (17, 15),
  (18, 16),
  (19, 16),
  (20, 15),
  (21, 20),
  (22, 16),
  (24, 6),
  (25, 24), (25, 37),
  (26, 16),
  (27, 6),
  (28, 19),
  (29, 19),
  (30, 19),
  (31, 5),
  (32, 3), (32, 40), (32, 41),
  (33, 3),
  (34, 3), (34, 40),
  (35, 16),
  (36, 19),
  (37, 22),
  (38, 2),
  (39, 2),
  (40, 2),
  (41, 39),
  (42, 17),
  (43, 41),
  (44, 6),
  (45, 37),
  (46, 16),
  (47, 39),
  (48, 7),
  (49, 39),
  (50, 45),
  (51, 50),
  (52, 51),
  (53, 6),
  (54, 6),
  (55, 6),
  (56, 2),
  (57, 62),
  (58, 6),
  (60, 14),
  (61, 14),
  (62, 22),
  (63, 22),
  (64, 14), (64, 15), (64, 16), (64, 17),
  (65, 22);

-- PPA cutoffs for MATH 30 and MATH 42 come from SJSU Math department table, CS 46B cutoff comes from 2025-2026 catalog.
INSERT INTO group_accepts_category (group_id, category_code, min_ppa_score) VALUES
  (2,  'M-I',   70), (2,  'M-II',  70),
  (59, 'M-I',   NULL), (59, 'M-II',  NULL), (59, 'M-III', NULL),
  (60, 'M-I',   70), (60, 'M-II',  70), (60, 'M-III', 70), (60, 'M-IV', 80),
  (61, 'M-I',   70), (61, 'M-II',  70), (61, 'M-III', 70), (61, 'M-IV', 80),
  (62, 'W-I',   NULL), (62, 'W-II',  NULL), (62, 'W-III', NULL),
  (63, 'W-I',   NULL), (63, 'W-II',  NULL),
  (64, 'M-I',   NULL), (64, 'M-II',  NULL),
  (65, 'W-I',   NULL), (65, 'W-II',  NULL);

-- Fall 2026 courses show as 1 (in progress).
INSERT INTO completed (student_id, course_id, taken_and_passed) VALUES
  -- Jordan: second year, in CS 146 and CS 151 now
  (1, 1, 1), (1, 15, 1), (1, 22, 1), (1, 23, 1), (1, 2, 1), (1, 16, 1), (1, 19, 1),
  (1, 6, 1), (1, 8, 1),
  -- Maya: in CS 157A now
  (2, 1, 1), (2, 2, 1), (2, 3, 1), (2, 6, 1), (2, 8, 1), (2, 15, 1), (2, 16, 1),
  (2, 19, 1), (2, 20, 1), (2, 11, 1),
  -- Sam: did not pass CS 46A
  (3, 1, 0), (3, 14, 1),
  -- Alex: did not pass MATH 42
  (4, 28, 1), (4, 15, 1), (4, 19, 0),
  -- Riley
  (5, 1, 1), (5, 15, 1), (5, 16, 1),
  -- Taylor: senior, in CS 160 and CS 166 now
  (6, 1, 1), (6, 2, 1), (6, 3, 1), (6, 5, 1), (6, 6, 1), (6, 7, 1), (6, 8, 1),
  (6, 9, 1), (6, 10, 1), (6, 15, 1), (6, 16, 1), (6, 19, 1), (6, 20, 1), (6, 22, 1),
  (6, 12, 1), (6, 13, 1),
  -- Casey: in MATH 39 now
  (7, 15, 1), (7, 16, 1), (7, 17, 1), (7, 18, 1),
  -- Morgan: in PHYS 50 now
  (8, 15, 1), (8, 16, 1), (8, 20, 1),
  -- Jamie: did not pass MATH 30
  (9, 1, 1), (9, 15, 0),
  -- Drew: in CS 157A now
  (10, 1, 1), (10, 2, 1), (10, 3, 1), (10, 6, 1), (10, 8, 1), (10, 15, 1), (10, 19, 1),
  (10, 11, 1),
  -- Avery
  (11, 22, 1),
  -- Quinn
  (12, 25, 1), (12, 15, 1);

-- Session 10 = Winter 2027, 11 = Spring 2027, 12 = Fall 2027,
-- 13 = Spring 2028, 14 = Summer 2027 S1.
INSERT INTO plan_entries (plan_entry_id, student_id, course_id, session_id, added_at) VALUES
  (1,  1,  11, 11, '2026-09-20 10:15:00'),
  (2,  1,  12, 11, '2026-09-20 10:16:00'),
  (3,  1,  3,  11, '2026-09-20 10:18:00'),
  (4,  1,  7,  12, '2026-09-20 10:20:00'),
  (5,  1,  9,  13, '2026-09-20 10:21:00'),
  (6,  2,  12, 11, '2026-09-22 14:02:00'),
  (7,  2,  13, 12, '2026-09-22 14:05:00'),
  (8,  4,  19, 11, '2026-09-23 09:30:00'),
  (9,  5,  18, 11, '2026-09-24 16:45:00'),
  (10, 6,  30, 11, '2026-09-25 11:00:00'),
  (11, 9,  15, 14, '2026-09-25 19:10:00'),
  (12, 10, 12, 11, '2026-09-26 08:40:00'),
  (13, 11, 1,  11, '2026-09-26 13:25:00'),
  (14, 3,  23, 10, '2026-09-27 12:00:00'),
  (15, 7,  29, 11, '2026-09-27 15:30:00'),
  (16, 8,  21, 11, '2026-09-28 10:05:00'),
  -- demo CS student 13 and SE student 14 follow full 2025 roadmap, every row passes POST /plan/add rules
  -- sessions 9 = Fall 2026, 11 = Spring 2027, 12 = Fall 2027, 13 = Spring 2028,
  -- 15 = Fall 2028, 16 = Spring 2029, 17 = Fall 2029, 18 = Spring 2030
  (17, 13, 1,  9,  '2026-10-25 09:00:00'),
  (18, 13, 15, 9,  '2026-10-25 09:01:00'),
  (19, 13, 19, 9,  '2026-10-25 09:02:00'),
  (20, 13, 22, 9,  '2026-10-25 09:03:00'),
  (21, 13, 26, 9,  '2026-10-25 09:04:00'),
  (22, 13, 2,  11, '2026-10-25 09:05:00'),
  (23, 13, 16, 11, '2026-10-25 09:06:00'),
  (24, 13, 59, 11, '2026-10-25 09:07:00'),
  (25, 13, 24, 11, '2026-10-25 09:08:00'),
  (26, 13, 62, 11, '2026-10-25 09:09:00'),
  (27, 13, 3,  12, '2026-10-25 09:10:00'),
  (28, 13, 6,  12, '2026-10-25 09:11:00'),
  (29, 13, 20, 12, '2026-10-25 09:12:00'),
  (30, 13, 23, 12, '2026-10-25 09:13:00'),
  (31, 13, 31, 12, '2026-10-25 09:14:00'),
  (32, 13, 33, 13, '2026-10-25 09:15:00'),
  (33, 13, 8,  13, '2026-10-25 09:16:00'),
  (34, 13, 18, 13, '2026-10-25 09:17:00'),
  (35, 13, 58, 13, '2026-10-25 09:18:00'),
  (36, 13, 32, 13, '2026-10-25 09:19:00'),
  (37, 13, 7,  15, '2026-10-25 09:20:00'),
  (38, 13, 35, 15, '2026-10-25 09:21:00'),
  (39, 13, 56, 15, '2026-10-25 09:22:00'),
  (40, 13, 27, 15, '2026-10-25 09:23:00'),
  (41, 13, 63, 15, '2026-10-25 09:24:00'),
  (42, 13, 9,  16, '2026-10-25 09:25:00'),
  (43, 13, 34, 16, '2026-10-25 09:26:00'),
  (44, 13, 5,  16, '2026-10-25 09:27:00'),
  (45, 13, 36, 16, '2026-10-25 09:28:00'),
  (46, 13, 55, 16, '2026-10-25 09:29:00'),
  (47, 13, 11, 17, '2026-10-25 09:30:00'),
  (48, 13, 12, 17, '2026-10-25 09:31:00'),
  (49, 13, 13, 17, '2026-10-25 09:32:00'),
  (50, 13, 30, 17, '2026-10-25 09:33:00'),
  (51, 13, 60, 17, '2026-10-25 09:34:00'),
  (52, 13, 10, 18, '2026-10-25 09:35:00'),
  (53, 13, 57, 18, '2026-10-25 09:36:00'),
  (54, 13, 61, 18, '2026-10-25 09:37:00'),
  (55, 13, 29, 18, '2026-10-25 09:38:00'),
  (56, 14, 1,  9,  '2026-10-25 10:00:00'),
  (57, 14, 38, 9,  '2026-10-25 10:01:00'),
  (58, 14, 22, 9,  '2026-10-25 10:02:00'),
  (59, 14, 23, 9,  '2026-10-25 10:03:00'),
  (60, 14, 15, 9,  '2026-10-25 10:04:00'),
  (61, 14, 2,  11, '2026-10-25 10:05:00'),
  (62, 14, 37, 11, '2026-10-25 10:06:00'),
  (63, 14, 19, 11, '2026-10-25 10:07:00'),
  (64, 14, 20, 11, '2026-10-25 10:08:00'),
  (65, 14, 25, 12, '2026-10-25 10:09:00'),
  (66, 14, 39, 12, '2026-10-25 10:10:00'),
  (67, 14, 31, 12, '2026-10-25 10:11:00'),
  (68, 14, 26, 12, '2026-10-25 10:12:00'),
  (69, 14, 16, 12, '2026-10-25 10:13:00'),
  (70, 14, 6,  13, '2026-10-25 10:14:00'),
  (71, 14, 27, 13, '2026-10-25 10:15:00'),
  (72, 14, 59, 13, '2026-10-25 10:16:00'),
  (73, 14, 17, 13, '2026-10-25 10:17:00'),
  (74, 14, 21, 13, '2026-10-25 10:18:00'),
  (75, 14, 40, 15, '2026-10-25 10:19:00'),
  (76, 14, 41, 15, '2026-10-25 10:20:00'),
  (77, 14, 42, 15, '2026-10-25 10:21:00'),
  (78, 14, 11, 15, '2026-10-25 10:22:00'),
  (79, 14, 29, 15, '2026-10-25 10:23:00'),
  (80, 14, 44, 16, '2026-10-25 10:24:00'),
  (81, 14, 7,  16, '2026-10-25 10:25:00'),
  (82, 14, 8,  16, '2026-10-25 10:26:00'),
  (83, 14, 45, 16, '2026-10-25 10:27:00'),
  (84, 14, 46, 16, '2026-10-25 10:28:00'),
  (85, 14, 47, 17, '2026-10-25 10:29:00'),
  (86, 14, 48, 17, '2026-10-25 10:30:00'),
  (87, 14, 49, 17, '2026-10-25 10:31:00'),
  (88, 14, 50, 17, '2026-10-25 10:32:00'),
  (89, 14, 13, 17, '2026-10-25 10:33:00'),
  (90, 14, 51, 17, '2026-10-25 10:34:00'),
  (91, 14, 52, 18, '2026-10-25 10:35:00'),
  (92, 14, 53, 18, '2026-10-25 10:36:00'),
  (93, 14, 54, 18, '2026-10-25 10:37:00'),
  (94, 14, 55, 18, '2026-10-25 10:38:00'),
  (95, 14, 57, 18, '2026-10-25 10:39:00'),
  -- CS 155 fills last 3 of 17 major elective units, so CS demo plan reaches 120 units
  (96, 13, 64, 18, '2026-10-25 09:39:00');

-- Sample scores from 0 to 1. Higher score means higher risk.
INSERT INTO bottleneck_scores
  (score_id, course_id, term_id, computed_at, waitlist_count, total_capacity,
   section_count, dependent_course_count, risk_level, score)
VALUES
  -- Fall 2026
  (1,  1,  7, '2026-09-28 09:00:00',  2, 120, 2, 1, 'Low',    0.35),
  (2,  2,  7, '2026-09-28 09:00:00',  4, 100, 2, 3, 'Medium', 0.58),
  (3,  6,  7, '2026-09-28 09:00:00', 12,  80, 2, 6, 'High',   0.92),
  (4,  8,  7, '2026-09-28 09:00:00',  0,  40, 1, 3, 'Low',    0.30),
  (5,  7,  7, '2026-09-28 09:00:00',  8,  40, 1, 0, 'Medium', 0.61),
  (6,  11, 7, '2026-09-28 09:00:00',  5,  90, 2, 0, 'Low',    0.38),
  (7,  12, 7, '2026-09-28 09:00:00',  2,  35, 1, 0, 'Low',    0.33),
  (8,  13, 7, '2026-09-28 09:00:00',  6,  40, 1, 0, 'Medium', 0.52),
  (9,  19, 7, '2026-09-28 09:00:00',  0,  40, 1, 1, 'Low',    0.12),
  (10, 15, 7, '2026-09-28 09:00:00',  3,  45, 1, 5, 'Medium', 0.64),
  -- Spring 2026
  (11, 6,  5, '2026-01-20 09:00:00', 12,  40, 1, 6, 'High',   0.95),
  (12, 11, 5, '2026-01-20 09:00:00',  4,  45, 1, 0, 'Low',    0.36),
  (13, 1,  5, '2026-01-20 09:00:00',  0,  60, 1, 1, 'Low',    0.18);
