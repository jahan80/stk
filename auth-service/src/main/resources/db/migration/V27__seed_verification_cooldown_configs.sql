-- ==================================================
-- V27: Seed cooldown configs for mobile + password reset
-- ==================================================
-- Email verification already has:
--   AUTH.EMAIL.VERIFICATION.RESEND.COOLDOWN.SECONDS = 60
-- This migration adds the equivalent for mobile + password reset.
--
-- These keys are read by VerificationThrottleService to enforce a
-- per-account cool-down between consecutive resend requests. Without
-- this, a single account can request unlimited verification codes
-- (email/SMS/password-reset spam, provider cost, brute-force surface).
--
-- Idempotency: uses ON CONFLICT DO NOTHING against the unique
-- config_key constraint.

INSERT INTO auth.configurations
    (config_key, config_value, default_value, value_type, description, enabled, created_at, updated_at)
VALUES
    ('AUTH.MOBILE.VERIFICATION.RESEND.COOLDOWN.SECONDS',
     '60', '60', 'INTEGER',
     'Cooldown between mobile verification resend requests (per account)',
     TRUE, NOW(), NOW()),

    ('AUTH.PASSWORD.RESET.RESEND.COOLDOWN.SECONDS',
     '60', '60', 'INTEGER',
     'Cooldown between password reset resend requests (per account)',
     TRUE, NOW(), NOW())
ON CONFLICT (config_key) DO NOTHING;

COMMENT ON COLUMN auth.configurations.config_key IS
    'Includes AUTH.*.RESEND.COOLDOWN.SECONDS keys used by VerificationThrottleService';
