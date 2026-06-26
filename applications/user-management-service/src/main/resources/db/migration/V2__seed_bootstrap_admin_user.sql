-- Seed an initial ADMIN user so a new environment can immediately manage users.
-- Password is hashed inside Postgres using bcrypt via pgcrypto and never stored in plain text.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (
    email,
    full_name,
    password_hash,
    status,
    failed_login_attempts,
    locked_until,
    created_at,
    updated_at
)
SELECT
    '${bootstrap_admin_email}',
    '${bootstrap_admin_full_name}',
    crypt('${bootstrap_admin_password}', gen_salt('bf', ${bootstrap_admin_bcrypt_rounds})),
    'ACTIVE',
    0,
    NULL,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = '${bootstrap_admin_email}'
);

INSERT INTO user_roles (user_id, role)
SELECT u.id, 'ADMIN'
FROM users u
WHERE u.email = '${bootstrap_admin_email}'
  AND NOT EXISTS (
    SELECT 1 FROM user_roles ur
    WHERE ur.user_id = u.id AND ur.role = 'ADMIN'
  );
