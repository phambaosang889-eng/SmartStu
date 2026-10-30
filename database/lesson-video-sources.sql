-- Optional curated public video links, researched 27/10/2026. No videos are downloaded/re-uploaded.
-- Add only to the existing matching lesson; do not replace teacher content or duplicate sources.
INSERT INTO lesson_videos(id,lesson_id,owner_email,title,source_type,source_url,source_label)
SELECT '27f63dd1-5b93-4c02-b719-000000000001',row_key,owner_email,'Ba định luật Newton về chuyển động','youtube','https://www.youtube.com/watch?v=gFJgWqZV-aQ','VietJack · Cô Nguyễn Mai Phương'
FROM catalog_entries WHERE row_key='lesson-newton' AND kind='lesson' AND NOT EXISTS(SELECT 1 FROM lesson_videos WHERE lesson_id='lesson-newton' AND source_url='https://www.youtube.com/watch?v=gFJgWqZV-aQ');
INSERT INTO lesson_videos(id,lesson_id,owner_email,title,source_type,source_url,source_label)
SELECT '27f63dd1-5b93-4c02-b719-000000000002',row_key,owner_email,'Chuyển động thẳng biến đổi đều','youtube','https://www.youtube.com/watch?v=-scFOvSVvkQ','VietJack · Thầy Lê Xuân Vượng'
FROM catalog_entries WHERE row_key='lesson-motion' AND kind='lesson' AND NOT EXISTS(SELECT 1 FROM lesson_videos WHERE lesson_id='lesson-motion' AND source_url='https://www.youtube.com/watch?v=-scFOvSVvkQ');
INSERT INTO lesson_videos(id,lesson_id,owner_email,title,source_type,source_url,source_label)
SELECT '27f63dd1-5b93-4c02-b719-000000000003',row_key,owner_email,'Công và công suất','youtube','https://www.youtube.com/watch?v=-zafKyYDuSo','VietJack · Thầy Lê Xuân Vượng'
FROM catalog_entries WHERE row_key='lesson-energy' AND kind='lesson' AND NOT EXISTS(SELECT 1 FROM lesson_videos WHERE lesson_id='lesson-energy' AND source_url='https://www.youtube.com/watch?v=-zafKyYDuSo');
INSERT INTO lesson_videos(id,lesson_id,owner_email,title,source_type,source_url,source_label)
SELECT '27f63dd1-5b93-4c02-b719-000000000004',row_key,owner_email,'Lực ma sát','youtube','https://www.youtube.com/watch?v=MOSsaEyPLBs','VietJack · Thầy Lê Xuân Vượng'
FROM catalog_entries WHERE row_key='lesson-friction' AND kind='lesson' AND NOT EXISTS(SELECT 1 FROM lesson_videos WHERE lesson_id='lesson-friction' AND source_url='https://www.youtube.com/watch?v=MOSsaEyPLBs');
