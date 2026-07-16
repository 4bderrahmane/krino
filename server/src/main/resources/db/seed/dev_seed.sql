-- ===========================================================================
-- Krino — local development seed data
-- ===========================================================================
-- Purpose : Populate Departments, Job postings, and Users (all roles) for
--           local development and manual testing.
--
-- Run it  : The schema must already exist (start the app once with
--           ddl-auto=update, or apply the Flyway migration). Then:
--
--             PGPASSWORD=<db-password> \
--               psql -h localhost -p 5432 -U krino -d krino \
--               -f src/main/resources/db/seed/dev_seed.sql
--
-- Re-run  : Idempotent — safe to run repeatedly. Existing rows are matched on
--           their natural keys (department name, job reference_code, user
--           email) and skipped, so wiping the DB and re-running just rebuilds
--           the same data set.
--
-- Login   : Every seeded user shares the password below (DEV ONLY):
--
--             Password123!
--
--           The stored value is a BCrypt hash (cost 12) of that password,
--           matching the application's BCryptPasswordEncoder.
--
-- NOTE    : This file is NOT a Flyway migration and is never picked up
--           automatically. It lives outside db/migration on purpose.
-- ===========================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- Departments
-- ---------------------------------------------------------------------------
INSERT INTO departments (public_id, name, description, version)
VALUES
    (gen_random_uuid(), 'Engineering',      'Builds and maintains the Krino platform and infrastructure.', 0),
    (gen_random_uuid(), 'Human Resources',  'Recruiting, people operations, and employee experience.',     0),
    (gen_random_uuid(), 'Sales',            'Drives revenue growth and manages client relationships.',      0),
    (gen_random_uuid(), 'Marketing',        'Brand, growth, and product marketing.',                        0),
    (gen_random_uuid(), 'Finance',          'Accounting, budgeting, and financial planning.',               0)
ON CONFLICT (name) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Users
-- ---------------------------------------------------------------------------
-- Password for every account below is "Password123!" (BCrypt, cost 12).
INSERT INTO users (
    public_id, email, password, first_name, last_name, phone_number,
    is_approved, email_verified, must_change_password
)
VALUES
    -- Staff (approved, can log in immediately)
    (gen_random_uuid(), 'admin@krino.dev',             '$2b$12$VbteJqK9AivEE7WvZAHPfeTfoLRjDZ0snAazpoyFgfQ2Kv/nSKoY.', 'Nadia',   'Cherkaoui',  '+212600000001', TRUE,  TRUE, FALSE),
    (gen_random_uuid(), 'sofia.bennani@krino.dev',     '$2b$12$VbteJqK9AivEE7WvZAHPfeTfoLRjDZ0snAazpoyFgfQ2Kv/nSKoY.', 'Sofia',   'Bennani',    '+212600000002', TRUE,  TRUE, FALSE),
    (gen_random_uuid(), 'youssef.alami@krino.dev',     '$2b$12$VbteJqK9AivEE7WvZAHPfeTfoLRjDZ0snAazpoyFgfQ2Kv/nSKoY.', 'Youssef', 'Alami',      '+212600000003', TRUE,  TRUE, FALSE),
    (gen_random_uuid(), 'karim.idrissi@krino.dev',     '$2b$12$VbteJqK9AivEE7WvZAHPfeTfoLRjDZ0snAazpoyFgfQ2Kv/nSKoY.', 'Karim',   'Idrissi',    '+212600000004', TRUE,  TRUE, FALSE),
    (gen_random_uuid(), 'yasmine.elfassi@krino.dev',   '$2b$12$VbteJqK9AivEE7WvZAHPfeTfoLRjDZ0snAazpoyFgfQ2Kv/nSKoY.', 'Yasmine', 'El Fassi',   '+212600000005', TRUE,  TRUE, FALSE),
    -- Candidates
    (gen_random_uuid(), 'amine.tazi@example.com',      '$2b$12$VbteJqK9AivEE7WvZAHPfeTfoLRjDZ0snAazpoyFgfQ2Kv/nSKoY.', 'Amine',   'Tazi',       '+212611000006', TRUE,  TRUE, FALSE),
    (gen_random_uuid(), 'salma.bourkadi@example.com',  '$2b$12$VbteJqK9AivEE7WvZAHPfeTfoLRjDZ0snAazpoyFgfQ2Kv/nSKoY.', 'Salma',   'Bourkadi',   '+212611000007', TRUE,  TRUE, FALSE),
    (gen_random_uuid(), 'omar.benjelloun@example.com', '$2b$12$VbteJqK9AivEE7WvZAHPfeTfoLRjDZ0snAazpoyFgfQ2Kv/nSKoY.', 'Omar',    'Benjelloun', '+212611000008', FALSE, TRUE, FALSE),
    (gen_random_uuid(), 'lina.haddad@example.com',     '$2b$12$VbteJqK9AivEE7WvZAHPfeTfoLRjDZ0snAazpoyFgfQ2Kv/nSKoY.', 'Lina',    'Haddad',     '+212611000009', TRUE,  TRUE, FALSE)
ON CONFLICT (email) DO NOTHING;

-- Roles (a user can hold several; Youssef is both HR_MANAGER and INTERVIEWER).
INSERT INTO user_roles (user_id, roles)
SELECT u.id, r.role
FROM (VALUES
    ('admin@krino.dev',             'ADMIN'),
    ('sofia.bennani@krino.dev',     'HR_MANAGER'),
    ('youssef.alami@krino.dev',     'HR_MANAGER'),
    ('youssef.alami@krino.dev',     'INTERVIEWER'),
    ('karim.idrissi@krino.dev',     'INTERVIEWER'),
    ('yasmine.elfassi@krino.dev',   'INTERVIEWER'),
    ('amine.tazi@example.com',      'CANDIDATE'),
    ('salma.bourkadi@example.com',  'CANDIDATE'),
    ('omar.benjelloun@example.com', 'CANDIDATE'),
    ('lina.haddad@example.com',     'CANDIDATE')
) AS r(email, role)
JOIN users u ON u.email = r.email
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.roles = r.role
);

-- ---------------------------------------------------------------------------
-- Job postings
-- ---------------------------------------------------------------------------
-- department_id is resolved from the department name so no hardcoded IDs leak.
INSERT INTO job_postings (
    public_id, reference_code, slug, department_id, title, description,
    application_deadline, planned_start_date,
    salary_min, salary_max, salary_currency, salary_period, salary_negotiable,
    city, remote_policy, experience_level, minimum_experience_years, open_positions,
    employment_type, contract_type, status, published_at, version
)
SELECT
    gen_random_uuid(), v.reference_code, v.slug, d.id, v.title, v.description,
    v.application_deadline, v.planned_start_date,
    v.salary_min, v.salary_max, v.salary_currency, v.salary_period, v.salary_negotiable,
    v.city, v.remote_policy, v.experience_level, v.minimum_experience_years, v.open_positions,
    v.employment_type, v.contract_type, v.status, v.published_at, 0
FROM (VALUES
    ('JOB-2026-0001', 'senior-backend-engineer-casablanca', 'Engineering',
        'Senior Backend Engineer', 'Design and build core backend services in Java/Spring Boot.',
        NOW() + INTERVAL '30 days', DATE '2026-09-01',
        25000, 40000, 'MAD', 'MONTHLY', FALSE,
        'CASABLANCA', 'ON_SITE', 'SENIOR', 5, 2,
        'FULL_TIME', 'PERMANENT', 'OPEN', NOW() - INTERVAL '10 days'),

    ('JOB-2026-0002', 'frontend-engineer-react-remote', 'Engineering',
        'Frontend Engineer (React)', 'Build the candidate-facing web experience with React and TypeScript.',
        NOW() + INTERVAL '45 days', DATE '2026-08-15',
        18000, 30000, 'MAD', 'MONTHLY', TRUE,
        NULL, 'REMOTE', 'MID_LEVEL', 3, 1,
        'FULL_TIME', 'PERMANENT', 'OPEN', NOW() - INTERVAL '5 days'),

    ('JOB-2026-0003', 'account-executive-casablanca', 'Sales',
        'Account Executive', 'Own the full sales cycle and grow a portfolio of enterprise clients.',
        NOW() + INTERVAL '20 days', DATE '2026-08-01',
        15000, 25000, 'MAD', 'MONTHLY', TRUE,
        'CASABLANCA', 'HYBRID', 'MID_LEVEL', 2, 3,
        'FULL_TIME', 'PERMANENT', 'OPEN', NOW() - INTERVAL '3 days'),

    ('JOB-2026-0004', 'growth-marketing-manager-marrakech', 'Marketing',
        'Growth Marketing Manager', 'Lead acquisition and retention campaigns across all channels.',
        NOW() + INTERVAL '60 days', DATE '2026-10-01',
        20000, 35000, 'MAD', 'MONTHLY', FALSE,
        'MARRAKECH', 'HYBRID', 'MANAGER', 6, 1,
        'FULL_TIME', 'PERMANENT', 'OPEN', NOW() - INTERVAL '1 days'),

    ('JOB-2026-0005', 'technical-recruiter-rabat', 'Human Resources',
        'Technical Recruiter', 'Source and screen engineering talent end to end.',
        NOW() + INTERVAL '25 days', DATE '2026-08-20',
        12000, 18000, 'MAD', 'MONTHLY', FALSE,
        'RABAT', 'ON_SITE', 'JUNIOR', 1, 1,
        'PART_TIME', 'FIXED_TERM', 'OPEN', NOW() - INTERVAL '2 days'),

    ('JOB-2026-0006', 'devops-intern-rabat', 'Engineering',
        'DevOps Intern', 'Support CI/CD and cloud infrastructure as a six-month intern.',
        NULL, DATE '2026-07-15',
        NULL, NULL, NULL, NULL, FALSE,
        'RABAT', 'ON_SITE', 'INTERN', 0, 1,
        'FULL_TIME', 'INTERNSHIP', 'DRAFT', NULL)
) AS v(
    reference_code, slug, dept_name, title, description,
    application_deadline, planned_start_date,
    salary_min, salary_max, salary_currency, salary_period, salary_negotiable,
    city, remote_policy, experience_level, minimum_experience_years, open_positions,
    employment_type, contract_type, status, published_at
)
JOIN departments d ON d.name = v.dept_name
ON CONFLICT (reference_code) DO NOTHING;

COMMIT;

-- ---------------------------------------------------------------------------
-- Summary (printed when run via psql)
-- ---------------------------------------------------------------------------
SELECT 'departments' AS table, count(*) AS rows FROM departments
UNION ALL SELECT 'users',        count(*) FROM users
UNION ALL SELECT 'user_roles',   count(*) FROM user_roles
UNION ALL SELECT 'job_postings', count(*) FROM job_postings
ORDER BY 1;
