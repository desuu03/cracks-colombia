-- Run once on PostgreSQL before deploying the verifier administration feature.
-- The content-moderation role was renamed from MODERADOR to VERIFICADOR. Hibernate generated a
-- CHECK constraint with the old enum values and ddl-auto=update does not refresh it.
ALTER TABLE user_accounts DROP CONSTRAINT IF EXISTS user_accounts_role_check;
UPDATE user_accounts SET role = 'VERIFICADOR' WHERE role = 'MODERADOR';
ALTER TABLE user_accounts ADD CONSTRAINT user_accounts_role_check
    CHECK (role IN ('DEPORTISTA', 'OJEADOR_ENTRENADOR', 'VERIFICADOR', 'ADMIN'));

-- Hibernate adds this column on its own with ddl-auto=update; this is for schemas managed by hand.
ALTER TABLE user_accounts ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;
