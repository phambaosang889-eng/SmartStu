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
