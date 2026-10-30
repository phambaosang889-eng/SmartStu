-- Optional repair for repeated historical HTTP regression fixtures only.
-- Keep the earliest matching record. No DELETE; archived copies remain recoverable.
-- Export catalog_entries first. This does not merge different user-authored topics.
USE smartstu;
START TRANSACTION;
CREATE TEMPORARY TABLE smartstu_duplicate_topic_ids (id VARCHAR(512) PRIMARY KEY);
INSERT INTO smartstu_duplicate_topic_ids
SELECT row_key FROM (
 SELECT row_key,ROW_NUMBER() OVER (ORDER BY sequence_id) AS duplicate_number
 FROM catalog_entries
 WHERE kind='topics' AND owner_email='admin@smartstudy.vn' AND status='published'
 AND BINARY JSON_UNQUOTE(JSON_EXTRACT(payload,'$.title'))=BINARY 'Đo lường'
 AND BINARY JSON_UNQUOTE(JSON_EXTRACT(payload,'$.topic'))=BINARY 'Chủ đề'
 AND BINARY JSON_UNQUOTE(JSON_EXTRACT(payload,'$.body'))=BINARY 'Sử dụng dụng cụ đo, đơn vị SI và biểu diễn sai số.'
) duplicates WHERE duplicate_number>1;
UPDATE catalog_entries c JOIN smartstu_duplicate_topic_ids d ON d.id=c.row_key
SET c.payload=JSON_SET(c.payload,'$.status','archived',
 '$.version',CAST(JSON_UNQUOTE(JSON_EXTRACT(c.payload,'$.version')) AS UNSIGNED)+1,
 '$.updatedAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'));
SELECT ROW_COUNT() AS archived_duplicate_topics;
DROP TEMPORARY TABLE smartstu_duplicate_topic_ids;
COMMIT;
