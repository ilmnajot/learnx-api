-- Separates tokens replaced by rotation from tokens revoked by logout or theft detection.
-- Only recently rotated tokens get a grace period.
ALTER TABLE refresh_tokens ADD COLUMN rotated_at timestamptz;
