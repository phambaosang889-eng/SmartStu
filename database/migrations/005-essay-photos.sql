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
