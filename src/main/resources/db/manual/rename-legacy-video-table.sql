-- Run once on PostgreSQL before starting the file-based video implementation.
-- The previous table only contains external links; retain it privately for audit/recovery.
ALTER TABLE IF EXISTS skill_videos RENAME TO skill_videos_legacy_links;

-- Free the index names so Hibernate can create indexes for the new skill_videos table.
ALTER INDEX IF EXISTS idx_skill_videos_athlete_uploaded
    RENAME TO idx_legacy_skill_videos_athlete_uploaded;
ALTER INDEX IF EXISTS idx_skill_videos_status_uploaded
    RENAME TO idx_legacy_skill_videos_status_uploaded;
ALTER INDEX IF EXISTS skill_videos_pkey
    RENAME TO skill_videos_legacy_pkey;
