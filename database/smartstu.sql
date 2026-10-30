-- SmartStu schema v1. MySQL 8.4 / utf8mb4. Non-destructive and repeatable.
-- Aggregate snapshots use JSON to preserve the question/rubric version that was actually answered.
-- Indexed identities, ownership and relationships are SQL columns and foreign keys.
CREATE DATABASE IF NOT EXISTS smartstu CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE smartstu;
CREATE TABLE IF NOT EXISTS schema_migrations(version INT PRIMARY KEY, applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS write_lock(id INT PRIMARY KEY);
INSERT IGNORE INTO write_lock(id) VALUES(1);
CREATE TABLE IF NOT EXISTS users (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id),
  email VARCHAR(150) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(payload,'$.user.email'))) STORED,
  display_name VARCHAR(100) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(payload,'$.user.name'))) STORED,
  role VARCHAR(12) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(payload,'$.user.role'))) STORED,
  locked TINYINT GENERATED ALWAYS AS (JSON_EXTRACT(payload,'$.locked')=TRUE) STORED,
  UNIQUE KEY uq_user_email(email), KEY ix_user_role(role,locked),
  CONSTRAINT ck_user_role CHECK(role IN ('STUDENT','TEACHER','ADMIN'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_credentials (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id),
  CONSTRAINT fk_credentials_user FOREIGN KEY(row_key) REFERENCES users(row_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS catalog_entries (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id),
  kind VARCHAR(24) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(payload,'$.kind'))) STORED,
  owner_email VARCHAR(150) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(payload,'$.owner'))) STORED,
  status VARCHAR(24) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(payload,'$.status'))) STORED,
  KEY ix_catalog_scope(kind,owner_email,status),
  CONSTRAINT fk_catalog_owner FOREIGN KEY(owner_email) REFERENCES users(email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS chapters (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id),
  course_id VARCHAR(191) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(payload,'$.course'))) STORED,
  KEY ix_chapter_course(course_id), CONSTRAINT fk_chapter_course FOREIGN KEY(course_id) REFERENCES catalog_entries(row_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS content_placements (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS course_covers (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id),
  CONSTRAINT fk_cover_course FOREIGN KEY(row_key) REFERENCES catalog_entries(row_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS enrollments (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id),
  student_email VARCHAR(150) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(payload,'$.email'))) STORED,
  course_id VARCHAR(191) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(payload,'$.course'))) STORED,
  UNIQUE KEY uq_enrollment(student_email,course_id),
  CONSTRAINT fk_enrollment_user FOREIGN KEY(student_email) REFERENCES users(email),
  CONSTRAINT fk_enrollment_course FOREIGN KEY(course_id) REFERENCES catalog_entries(row_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS quizzes (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id),
  owner_email VARCHAR(150) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(payload,'$.owner'))) STORED,
  status VARCHAR(24) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(payload,'$.status'))) STORED,
  KEY ix_quiz_scope(owner_email,status), CONSTRAINT fk_quiz_owner FOREIGN KEY(owner_email) REFERENCES users(email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS quiz_runs (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id),
  student_email VARCHAR(150) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(payload,'$.email'))) STORED,
  quiz_id VARCHAR(191) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(payload,'$.quiz.id'))) STORED,
  KEY ix_run_student(student_email,quiz_id),
  CONSTRAINT fk_run_user FOREIGN KEY(student_email) REFERENCES users(email),
  CONSTRAINT fk_run_quiz FOREIGN KEY(quiz_id) REFERENCES quizzes(row_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS quiz_attempts (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id),
  CONSTRAINT fk_attempt_run FOREIGN KEY(row_key) REFERENCES quiz_runs(row_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS question_bank (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS question_owners (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id),
  CONSTRAINT fk_question_owner_question FOREIGN KEY(row_key) REFERENCES question_bank(row_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS question_versions (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id),
  CONSTRAINT fk_question_version_question FOREIGN KEY(row_key) REFERENCES question_bank(row_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS question_archived (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id),
  CONSTRAINT fk_question_archived_question FOREIGN KEY(row_key) REFERENCES question_bank(row_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS essay_assignments (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id),
  owner_email VARCHAR(150) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(payload,'$.owner'))) STORED,
  KEY ix_assignment_owner(owner_email), CONSTRAINT fk_assignment_owner FOREIGN KEY(owner_email) REFERENCES users(email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS essay_submissions (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id),
  student_email VARCHAR(150) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(payload,'$.email'))) STORED,
  assignment_id VARCHAR(191) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(payload,'$.assignment.id'))) STORED,
  KEY ix_submission_student(student_email,assignment_id),
  CONSTRAINT fk_submission_user FOREIGN KEY(student_email) REFERENCES users(email),
  CONSTRAINT fk_submission_assignment FOREIGN KEY(assignment_id) REFERENCES essay_assignments(row_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS essay_drafts (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS essay_ai_suggestions (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS lesson_progress (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id),
  CONSTRAINT fk_progress_user FOREIGN KEY(row_key) REFERENCES users(row_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS learning_activities (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id),
  CONSTRAINT fk_activity_user FOREIGN KEY(row_key) REFERENCES users(row_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS learning_scores (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id),
  CONSTRAINT fk_scores_user FOREIGN KEY(row_key) REFERENCES users(row_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS flashcard_progress (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id),
  CONSTRAINT fk_card_progress_user FOREIGN KEY(row_key) REFERENCES users(row_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS learning_time (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id),
  CONSTRAINT fk_learning_time_user FOREIGN KEY(row_key) REFERENCES users(row_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS last_lesson (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id),
  CONSTRAINT fk_last_lesson_user FOREIGN KEY(row_key) REFERENCES users(row_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS audit_events (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS system_settings (
  row_key VARCHAR(512) NOT NULL PRIMARY KEY,
  payload JSON NOT NULL,
  sequence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_sequence(sequence_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ai_conversations(
 id CHAR(36) PRIMARY KEY,
 owner_email VARCHAR(150) NOT NULL,
 title VARCHAR(150) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 deleted_at TIMESTAMP(6) NULL,
 KEY ix_conversation_owner(owner_email,deleted_at,created_at),
 CONSTRAINT fk_conversation_user FOREIGN KEY(owner_email) REFERENCES users(email)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS ai_messages(
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 conversation_id CHAR(36) NOT NULL,
 request_id CHAR(36) NOT NULL,
 role VARCHAR(12) NOT NULL,
 content TEXT NOT NULL,
 status VARCHAR(12) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE KEY uq_message_request(conversation_id,request_id,role),
 KEY ix_message_order(conversation_id,id),
 CONSTRAINT fk_message_conversation FOREIGN KEY(conversation_id) REFERENCES ai_conversations(id),
 CONSTRAINT ck_message_role CHECK(role IN ('user','assistant')),
 CONSTRAINT ck_message_status CHECK(status IN ('complete','pending','failed'))
) ENGINE=InnoDB;
-- Read-only relational projections of existing unified content. No second source of truth.
CREATE OR REPLACE VIEW courses AS SELECT row_key AS id,owner_email AS teacher_email,status,
 JSON_UNQUOTE(JSON_EXTRACT(payload,'$.title')) AS title, payload FROM catalog_entries WHERE kind='courses';
CREATE OR REPLACE VIEW lessons AS SELECT c.row_key AS id,c.owner_email,c.status,
 JSON_UNQUOTE(JSON_EXTRACT(c.payload,'$.title')) AS title,
 JSON_UNQUOTE(JSON_EXTRACT(p.payload,'$.course')) AS course_id,c.payload
 FROM catalog_entries c LEFT JOIN content_placements p ON p.row_key=c.row_key WHERE c.kind='lesson';
CREATE OR REPLACE VIEW flashcards AS SELECT row_key AS id,owner_email,status,payload FROM catalog_entries WHERE kind='flashcards';
CREATE OR REPLACE VIEW quiz_questions AS SELECT q.row_key AS quiz_id,j.ordinal,j.question
 FROM quizzes q JOIN JSON_TABLE(q.payload,'$.questions[*]' COLUMNS(ordinal FOR ORDINALITY,question JSON PATH '$')) j;
INSERT IGNORE INTO schema_migrations(version) VALUES(1);
-- Initial accounts/content are an explicit opt-in, never inserted on ordinary application startup.
-- Run com.smartstudy.persistence.SeedDatabase (documented in docs/DATABASE.md) or database/seed-local.sql.
-- SmartStu v2: additive integrity migration. No application data is deleted.
USE smartstu;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='content_placements' AND column_name='course_id'), 'SELECT 1', 'ALTER TABLE content_placements ADD COLUMN course_id VARCHAR(191) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(payload,''$.course''))) STORED');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='content_placements' AND column_name='chapter_id'), 'SELECT 1', 'ALTER TABLE content_placements ADD COLUMN chapter_id VARCHAR(191) GENERATED ALWAYS AS (NULLIF(JSON_UNQUOTE(JSON_EXTRACT(payload,''$.chapter'')),'''')) STORED');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='question_owners' AND column_name='owner_email'), 'SELECT 1', 'ALTER TABLE question_owners ADD COLUMN owner_email VARCHAR(150) GENERATED ALWAYS AS (JSON_UNQUOTE(payload)) STORED');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='question_bank' AND column_name='topic'), 'SELECT 1', 'ALTER TABLE question_bank ADD COLUMN topic VARCHAR(80) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(payload,''$.topic''))) STORED');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='question_bank' AND column_name='difficulty'), 'SELECT 1', 'ALTER TABLE question_bank ADD COLUMN difficulty VARCHAR(40) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(payload,''$.difficulty''))) STORED');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='quiz_attempts' AND column_name='student_email'), 'SELECT 1', 'ALTER TABLE quiz_attempts ADD COLUMN student_email VARCHAR(150) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(payload,''$.run.email''))) STORED');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='quiz_attempts' AND column_name='quiz_id'), 'SELECT 1', 'ALTER TABLE quiz_attempts ADD COLUMN quiz_id VARCHAR(191) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(payload,''$.run.quiz.id''))) STORED');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='quiz_attempts' AND column_name='score'), 'SELECT 1', 'ALTER TABLE quiz_attempts ADD COLUMN score INT GENERATED ALWAYS AS (JSON_EXTRACT(payload,''$.score'')) STORED');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='quiz_attempts' AND column_name='maximum'), 'SELECT 1', 'ALTER TABLE quiz_attempts ADD COLUMN maximum INT GENERATED ALWAYS AS (JSON_EXTRACT(payload,''$.run.quiz.maxScore'')) STORED');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='essay_submissions' AND column_name='public_id'), 'SELECT 1', 'ALTER TABLE essay_submissions ADD COLUMN public_id VARCHAR(191) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(payload,''$.id''))) STORED');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='essay_submissions' AND column_name='owner_email'), 'SELECT 1', 'ALTER TABLE essay_submissions ADD COLUMN owner_email VARCHAR(150) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(payload,''$.assignment.owner''))) STORED');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='enrollments' AND column_name='active'), 'SELECT 1', 'ALTER TABLE enrollments ADD COLUMN active TINYINT GENERATED ALWAYS AS (JSON_EXTRACT(payload,''$.active'')=TRUE) STORED');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='essay_drafts' AND column_name='student_email'), 'SELECT 1', 'ALTER TABLE essay_drafts ADD COLUMN student_email VARCHAR(150) GENERATED ALWAYS AS (SUBSTRING_INDEX(row_key,'':'',1)) STORED');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='essay_drafts' AND column_name='assignment_id'), 'SELECT 1', 'ALTER TABLE essay_drafts ADD COLUMN assignment_id VARCHAR(191) GENERATED ALWAYS AS (SUBSTRING_INDEX(row_key,'':'',-1)) STORED');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='essay_submissions' AND index_name='uq_submission_public'), 'SELECT 1', 'ALTER TABLE essay_submissions ADD UNIQUE INDEX uq_submission_public(public_id)');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='content_placements' AND index_name='ix_placement_course_chapter'), 'SELECT 1', 'ALTER TABLE content_placements ADD INDEX ix_placement_course_chapter(course_id,chapter_id)');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='question_bank' AND index_name='ix_question_topic_level'), 'SELECT 1', 'ALTER TABLE question_bank ADD INDEX ix_question_topic_level(topic,difficulty)');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='quiz_attempts' AND index_name='ix_attempt_student_quiz'), 'SELECT 1', 'ALTER TABLE quiz_attempts ADD INDEX ix_attempt_student_quiz(student_email,quiz_id)');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='essay_submissions' AND index_name='ix_submission_teacher'), 'SELECT 1', 'ALTER TABLE essay_submissions ADD INDEX ix_submission_teacher(owner_email)');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='enrollments' AND index_name='ix_enrollment_course_active'), 'SELECT 1', 'ALTER TABLE enrollments ADD INDEX ix_enrollment_course_active(course_id,active)');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='content_placements' AND constraint_name='fk_placement_course'), 'SELECT 1', 'ALTER TABLE content_placements ADD CONSTRAINT fk_placement_course FOREIGN KEY(course_id) REFERENCES catalog_entries(row_key)');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='content_placements' AND constraint_name='fk_placement_chapter'), 'SELECT 1', 'ALTER TABLE content_placements ADD CONSTRAINT fk_placement_chapter FOREIGN KEY(chapter_id) REFERENCES chapters(row_key)');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='question_owners' AND constraint_name='fk_question_owner_user'), 'SELECT 1', 'ALTER TABLE question_owners ADD CONSTRAINT fk_question_owner_user FOREIGN KEY(owner_email) REFERENCES users(email)');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='quiz_attempts' AND constraint_name='fk_attempt_student'), 'SELECT 1', 'ALTER TABLE quiz_attempts ADD CONSTRAINT fk_attempt_student FOREIGN KEY(student_email) REFERENCES users(email)');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='quiz_attempts' AND constraint_name='ck_attempt_score'), 'SELECT 1', 'ALTER TABLE quiz_attempts ADD CONSTRAINT ck_attempt_score CHECK(score >= 0 AND score <= maximum)');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='essay_submissions' AND constraint_name='fk_submission_teacher'), 'SELECT 1', 'ALTER TABLE essay_submissions ADD CONSTRAINT fk_submission_teacher FOREIGN KEY(owner_email) REFERENCES users(email)');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='essay_drafts' AND constraint_name='fk_draft_student'), 'SELECT 1', 'ALTER TABLE essay_drafts ADD CONSTRAINT fk_draft_student FOREIGN KEY(student_email) REFERENCES users(email)');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='essay_drafts' AND constraint_name='fk_draft_assignment'), 'SELECT 1', 'ALTER TABLE essay_drafts ADD CONSTRAINT fk_draft_assignment FOREIGN KEY(assignment_id) REFERENCES essay_assignments(row_key)');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
SET @smartstu_ddl = IF((SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='essay_ai_suggestions' AND constraint_name='fk_suggestion_submission'), 'SELECT 1', 'ALTER TABLE essay_ai_suggestions ADD CONSTRAINT fk_suggestion_submission FOREIGN KEY(row_key) REFERENCES essay_submissions(public_id)');
PREPARE smartstu_stmt FROM @smartstu_ddl; EXECUTE smartstu_stmt; DEALLOCATE PREPARE smartstu_stmt;
CREATE OR REPLACE VIEW student_lesson_progress AS
 SELECT p.row_key AS student_email,j.lesson_id FROM lesson_progress p
 JOIN JSON_TABLE(p.payload,'$[*]' COLUMNS(lesson_id VARCHAR(191) PATH '$')) j;
CREATE OR REPLACE VIEW student_flashcard_progress AS
 SELECT p.row_key AS student_email,j.flashcard_id FROM flashcard_progress p
 JOIN JSON_TABLE(p.payload,'$[*]' COLUMNS(flashcard_id VARCHAR(191) PATH '$')) j;
INSERT IGNORE INTO schema_migrations(version) VALUES(2);
-- Additive migration. Uploaded bytes stay outside WAR; MySQL stores ownership and metadata.
CREATE TABLE IF NOT EXISTS lesson_videos (
 id CHAR(36) CHARACTER SET ascii PRIMARY KEY,
 lesson_id VARCHAR(512) NOT NULL,
 owner_email VARCHAR(512) NOT NULL,
 title VARCHAR(200) NOT NULL,
 source_type ENUM('youtube','upload') NOT NULL,
 source_url VARCHAR(1000) NOT NULL DEFAULT '',
 source_label VARCHAR(200) NOT NULL DEFAULT '',
 storage_name VARCHAR(80) CHARACTER SET ascii NOT NULL DEFAULT '',
 mime_type VARCHAR(40) CHARACTER SET ascii NOT NULL DEFAULT '',
 byte_size BIGINT NOT NULL DEFAULT 0,
 archived BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY ix_lesson_video(lesson_id,archived,created_at),
 KEY ix_video_owner(owner_email),
 CONSTRAINT fk_video_lesson FOREIGN KEY(lesson_id) REFERENCES catalog_entries(row_key),
 CONSTRAINT fk_video_owner FOREIGN KEY(owner_email) REFERENCES users(row_key),
 CONSTRAINT chk_video_size CHECK(byte_size>=0 AND byte_size<=209715200)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
INSERT IGNORE INTO schema_migrations(version) VALUES (3);

-- Additive migration: essay scans are private data and part of MySQL backups.
USE smartstu;
CREATE TABLE IF NOT EXISTS essay_photos (
 id CHAR(36) NOT NULL PRIMARY KEY,
 assignment_id VARCHAR(512) NOT NULL,
 student_email VARCHAR(150) NOT NULL,
 submission_id CHAR(36) NULL,
 image_bytes MEDIUMBLOB NOT NULL,
 byte_size INT UNSIGNED NOT NULL,
 width INT UNSIGNED NOT NULL,
 height INT UNSIGNED NOT NULL,
 sha256 CHAR(64) NOT NULL,
 ocr_text TEXT NULL,
 ocr_warning TEXT NULL,
 ocr_confidence DOUBLE NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 draft_slot TINYINT GENERATED ALWAYS AS (IF(submission_id IS NULL,1,NULL)) STORED,
 UNIQUE KEY uq_essay_draft_photo(student_email,assignment_id,draft_slot),
 UNIQUE KEY uq_essay_submission_photo(submission_id),
 KEY ix_essay_photo_owner(student_email),
 CONSTRAINT fk_essay_photo_assignment FOREIGN KEY(assignment_id) REFERENCES essay_assignments(row_key),
 CONSTRAINT fk_essay_photo_student FOREIGN KEY(student_email) REFERENCES users(email),
 CONSTRAINT ck_essay_photo_bytes CHECK(byte_size BETWEEN 1 AND 2097152)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
