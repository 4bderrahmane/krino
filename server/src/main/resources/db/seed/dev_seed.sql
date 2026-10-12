-- ===========================================================================
-- Krino — local development seed data
-- ===========================================================================
-- Purpose : Populate Departments, Skills, Job postings, and their required /
--           preferred skills for local development and manual testing.
--
-- Run it  : Apply V1 with Flyway by starting the app once, then run this from
--           the server/ directory:
--
--             set -a && . ./.env && set +a
--             PGPASSWORD="$SPRING_DATASOURCE_PASSWORD" \
--               psql -h localhost -p 5432 -U "$SPRING_DATASOURCE_USERNAME" -d krino \
--               -f src/main/resources/db/seed/dev_seed.sql
--
-- Re-run  : Idempotent — safe to run repeatedly. Existing rows are matched on
--           their natural keys (department name, skill slug, job
--           reference_code, job+skill pair) and skipped, so rebuilding the DB
--           and re-running restores the same catalogue.
--
-- Users   : NO users are seeded. The first ADMIN account is created at
--           startup by AdminInitializer from APP_ADMIN_EMAIL /
--           APP_ADMIN_PASSWORD; every other account is meant to come from the
--           real registration flow. Because of that, the tables that hang off
--           users — slots, applications, interviews — are not seeded either:
--           they need interviewer and candidate rows that only exist once you
--           have registered them.
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
    (gen_random_uuid(), 'Sales',            'Drives revenue growth and manages client relationships.',     0),
    (gen_random_uuid(), 'Marketing',        'Brand, growth, and product marketing.',                       0),
    (gen_random_uuid(), 'Finance',          'Accounting, budgeting, and financial planning.',              0),
    (gen_random_uuid(), 'Design',           'Product design, design system, and user research.',           0)
ON CONFLICT (name) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Skills
-- ---------------------------------------------------------------------------
-- The slug is the natural key; the name is what the UI shows.
INSERT INTO skills (name, slug)
VALUES
    ('Java',              'java'),
    ('Spring Boot',       'spring-boot'),
    ('PostgreSQL',        'postgresql'),
    ('Docker',            'docker'),
    ('Kubernetes',        'kubernetes'),
    ('CI/CD',             'ci-cd'),
    ('React',             'react'),
    ('TypeScript',        'typescript'),
    ('CSS',               'css'),
    ('REST APIs',         'rest-apis'),
    ('Test Automation',   'test-automation'),
    ('SQL',               'sql'),
    ('Excel',             'excel'),
    ('Accounting',        'accounting'),
    ('Figma',             'figma'),
    ('User Research',     'user-research'),
    ('SEO',               'seo'),
    ('Copywriting',       'copywriting'),
    ('Salesforce',        'salesforce'),
    ('Negotiation',       'negotiation'),
    ('Technical Sourcing','technical-sourcing'),
    ('Communication',     'communication'),
    ('French',            'french'),
    ('English',           'english')
ON CONFLICT (slug) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Job postings
-- ---------------------------------------------------------------------------
-- department_id is resolved from the department name so no hardcoded IDs leak.
-- The set covers every JobStatus, and each row respects the invariants Job
-- enforces on persist: OPEN/PAUSED need published_at, SCHEDULED needs
-- scheduled_publish_at, CLOSED/FILLED/CANCELLED/ARCHIVED need closed_at,
-- ON_SITE/HYBRID need a city, a deadline must fall after published_at, and a
-- salary needs both a currency and a period.
INSERT INTO job_postings (
    public_id, reference_code, slug, department_id, title, description,
    application_deadline, planned_start_date,
    salary_min, salary_max, salary_currency, salary_period, salary_negotiable,
    city, remote_policy, experience_level, minimum_experience_years, open_positions,
    employment_type, contract_type, status,
    scheduled_publish_at, published_at, closed_at, version
)
SELECT
    gen_random_uuid(), v.reference_code, v.slug, d.id, v.title, v.description,
    v.application_deadline, v.planned_start_date,
    v.salary_min, v.salary_max, v.salary_currency, v.salary_period, v.salary_negotiable,
    v.city, v.remote_policy, v.experience_level, v.minimum_experience_years, v.open_positions,
    v.employment_type, v.contract_type, v.status,
    v.scheduled_publish_at, v.published_at, v.closed_at, 0
FROM (VALUES
    ('JOB-2026-0001', 'senior-backend-engineer-casablanca', 'Engineering',
        'Senior Backend Engineer', 'Design and build core backend services in Java/Spring Boot, own them from schema to production.',
        NOW() + INTERVAL '30 days', DATE '2026-09-01',
        25000, 40000, 'MAD', 'MONTHLY', FALSE,
        'CASABLANCA', 'ON_SITE', 'SENIOR', 5, 2,
        'FULL_TIME', 'PERMANENT', 'OPEN',
        NULL, NOW() - INTERVAL '10 days', NULL),

    ('JOB-2026-0002', 'frontend-engineer-react-remote', 'Engineering',
        'Frontend Engineer (React)', 'Build the candidate-facing web experience with React and TypeScript.',
        NOW() + INTERVAL '45 days', DATE '2026-08-15',
        18000, 30000, 'MAD', 'MONTHLY', TRUE,
        NULL, 'REMOTE', 'MID_LEVEL', 3, 1,
        'FULL_TIME', 'PERMANENT', 'OPEN',
        NULL, NOW() - INTERVAL '5 days', NULL),

    ('JOB-2026-0003', 'account-executive-casablanca', 'Sales',
        'Account Executive', 'Own the full sales cycle and grow a portfolio of enterprise clients.',
        NOW() + INTERVAL '20 days', DATE '2026-08-01',
        15000, 25000, 'MAD', 'MONTHLY', TRUE,
        'CASABLANCA', 'HYBRID', 'MID_LEVEL', 2, 3,
        'FULL_TIME', 'PERMANENT', 'OPEN',
        NULL, NOW() - INTERVAL '3 days', NULL),

    ('JOB-2026-0004', 'growth-marketing-manager-marrakech', 'Marketing',
        'Growth Marketing Manager', 'Lead acquisition and retention campaigns across all channels.',
        NOW() + INTERVAL '60 days', DATE '2026-10-01',
        20000, 35000, 'MAD', 'MONTHLY', FALSE,
        'MARRAKECH', 'HYBRID', 'MANAGER', 6, 1,
        'FULL_TIME', 'PERMANENT', 'OPEN',
        NULL, NOW() - INTERVAL '1 days', NULL),

    ('JOB-2026-0005', 'technical-recruiter-rabat', 'Human Resources',
        'Technical Recruiter', 'Source and screen engineering talent end to end.',
        NOW() + INTERVAL '25 days', DATE '2026-08-20',
        12000, 18000, 'MAD', 'MONTHLY', FALSE,
        'RABAT', 'ON_SITE', 'JUNIOR', 1, 1,
        'PART_TIME', 'FIXED_TERM', 'OPEN',
        NULL, NOW() - INTERVAL '2 days', NULL),

    ('JOB-2026-0006', 'freelance-content-writer-remote', 'Marketing',
        'Freelance Content Writer', 'Write long-form articles and product copy for the Krino blog, two days a week.',
        NOW() + INTERVAL '14 days', DATE '2026-09-15',
        150, 250, 'MAD', 'HOURLY', TRUE,
        NULL, 'REMOTE', 'ENTRY_LEVEL', 1, 2,
        'PART_TIME', 'FREELANCE', 'OPEN',
        NULL, NOW() - INTERVAL '7 days', NULL),

    -- Draft: never published, so most of the timeline is still empty.
    ('JOB-2026-0007', 'devops-intern-rabat', 'Engineering',
        'DevOps Intern', 'Support CI/CD and cloud infrastructure as a six-month intern.',
        NULL, DATE '2026-11-02',
        NULL, NULL, NULL, NULL, FALSE,
        'RABAT', 'ON_SITE', 'INTERN', 0, 1,
        'FULL_TIME', 'INTERNSHIP', 'DRAFT',
        NULL, NULL, NULL),

    -- Scheduled: goes live on its own in a week.
    ('JOB-2026-0008', 'data-analyst-tangier', 'Finance',
        'Data Analyst', 'Turn hiring and revenue data into dashboards the leadership team actually reads.',
        NOW() + INTERVAL '40 days', DATE '2026-10-15',
        14000, 22000, 'MAD', 'MONTHLY', FALSE,
        'TANGIER', 'HYBRID', 'ENTRY_LEVEL', 1, 1,
        'FULL_TIME', 'PERMANENT', 'SCHEDULED',
        NOW() + INTERVAL '7 days', NULL, NULL),

    -- Paused: published once, temporarily off the board.
    ('JOB-2026-0009', 'qa-engineer-agadir', 'Engineering',
        'QA Engineer', 'Own the automated test suite and keep the release pipeline honest.',
        NOW() + INTERVAL '15 days', DATE '2026-09-01',
        16000, 24000, 'MAD', 'MONTHLY', FALSE,
        'AGADIR', 'ON_SITE', 'MID_LEVEL', 3, 1,
        'FULL_TIME', 'PERMANENT', 'PAUSED',
        NULL, NOW() - INTERVAL '20 days', NULL),

    -- Closed without a hire.
    ('JOB-2026-0010', 'payroll-specialist-casablanca', 'Finance',
        'Payroll Specialist', 'Run the monthly payroll cycle and keep social declarations on time.',
        NOW() - INTERVAL '20 days', DATE '2026-06-01',
        11000, 16000, 'MAD', 'MONTHLY', FALSE,
        'CASABLANCA', 'ON_SITE', 'JUNIOR', 2, 1,
        'FULL_TIME', 'PERMANENT', 'CLOSED',
        NULL, NOW() - INTERVAL '90 days', NOW() - INTERVAL '10 days'),

    -- Filled: closed because someone was hired.
    ('JOB-2026-0011', 'product-designer-remote', 'Design',
        'Product Designer', 'Shape the end-to-end candidate experience, from research to shipped interface.',
        NOW() - INTERVAL '60 days', DATE '2026-05-04',
        20000, 32000, 'MAD', 'MONTHLY', FALSE,
        NULL, 'REMOTE', 'MID_LEVEL', 4, 1,
        'FULL_TIME', 'PERMANENT', 'FILLED',
        NULL, NOW() - INTERVAL '120 days', NOW() - INTERVAL '30 days'),

    -- Cancelled: the headcount went away.
    ('JOB-2026-0012', 'sales-development-representative-tetouan', 'Sales',
        'Sales Development Representative', 'Prospect, qualify, and hand over opportunities to the account executives.',
        NOW() - INTERVAL '10 days', DATE '2026-07-01',
        9000, 14000, 'MAD', 'MONTHLY', TRUE,
        'TETOUAN', 'ON_SITE', 'ENTRY_LEVEL', 0, 2,
        'FULL_TIME', 'FIXED_TERM', 'CANCELLED',
        NULL, NOW() - INTERVAL '40 days', NOW() - INTERVAL '5 days'),

    -- Archived: out of the way, kept for the record.
    ('JOB-2026-0013', 'office-manager-fes', 'Human Resources',
        'Office Manager', 'Keep the Fes office running: suppliers, facilities, and on-site onboarding.',
        NOW() - INTERVAL '150 days', DATE '2026-01-05',
        10000, 15000, 'MAD', 'MONTHLY', FALSE,
        'FES', 'ON_SITE', 'JUNIOR', 2, 1,
        'FULL_TIME', 'PERMANENT', 'ARCHIVED',
        NULL, NOW() - INTERVAL '200 days', NOW() - INTERVAL '60 days')
) AS v(
    reference_code, slug, dept_name, title, description,
    application_deadline, planned_start_date,
    salary_min, salary_max, salary_currency, salary_period, salary_negotiable,
    city, remote_policy, experience_level, minimum_experience_years, open_positions,
    employment_type, contract_type, status,
    scheduled_publish_at, published_at, closed_at
)
JOIN departments d ON d.name = v.dept_name
ON CONFLICT (reference_code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Job skills
-- ---------------------------------------------------------------------------
-- Both sides are resolved by natural key (job reference_code, skill slug).
INSERT INTO job_skills (job_id, skill_id, importance)
SELECT j.id, s.id, v.importance
FROM (VALUES
    ('JOB-2026-0001', 'java',               'REQUIRED'),
    ('JOB-2026-0001', 'spring-boot',        'REQUIRED'),
    ('JOB-2026-0001', 'postgresql',         'REQUIRED'),
    ('JOB-2026-0001', 'rest-apis',          'REQUIRED'),
    ('JOB-2026-0001', 'docker',             'PREFERRED'),
    ('JOB-2026-0001', 'kubernetes',         'PREFERRED'),

    ('JOB-2026-0002', 'react',              'REQUIRED'),
    ('JOB-2026-0002', 'typescript',         'REQUIRED'),
    ('JOB-2026-0002', 'css',                'REQUIRED'),
    ('JOB-2026-0002', 'figma',              'PREFERRED'),
    ('JOB-2026-0002', 'english',            'PREFERRED'),

    ('JOB-2026-0003', 'salesforce',         'REQUIRED'),
    ('JOB-2026-0003', 'negotiation',        'REQUIRED'),
    ('JOB-2026-0003', 'french',             'REQUIRED'),
    ('JOB-2026-0003', 'english',            'PREFERRED'),

    ('JOB-2026-0004', 'seo',                'REQUIRED'),
    ('JOB-2026-0004', 'copywriting',        'REQUIRED'),
    ('JOB-2026-0004', 'communication',      'REQUIRED'),
    ('JOB-2026-0004', 'sql',                'PREFERRED'),

    ('JOB-2026-0005', 'technical-sourcing', 'REQUIRED'),
    ('JOB-2026-0005', 'communication',      'REQUIRED'),
    ('JOB-2026-0005', 'english',            'PREFERRED'),

    ('JOB-2026-0006', 'copywriting',        'REQUIRED'),
    ('JOB-2026-0006', 'seo',                'PREFERRED'),
    ('JOB-2026-0006', 'french',             'PREFERRED'),

    ('JOB-2026-0007', 'docker',             'PREFERRED'),
    ('JOB-2026-0007', 'ci-cd',              'PREFERRED'),
    ('JOB-2026-0007', 'english',            'PREFERRED'),

    ('JOB-2026-0008', 'sql',                'REQUIRED'),
    ('JOB-2026-0008', 'excel',              'REQUIRED'),
    ('JOB-2026-0008', 'communication',      'PREFERRED'),

    ('JOB-2026-0009', 'test-automation',    'REQUIRED'),
    ('JOB-2026-0009', 'ci-cd',              'REQUIRED'),
    ('JOB-2026-0009', 'java',               'PREFERRED'),

    ('JOB-2026-0010', 'accounting',         'REQUIRED'),
    ('JOB-2026-0010', 'excel',              'REQUIRED'),

    ('JOB-2026-0011', 'figma',              'REQUIRED'),
    ('JOB-2026-0011', 'user-research',      'REQUIRED'),
    ('JOB-2026-0011', 'css',                'PREFERRED'),

    ('JOB-2026-0012', 'communication',      'REQUIRED'),
    ('JOB-2026-0012', 'negotiation',        'PREFERRED'),

    ('JOB-2026-0013', 'communication',      'REQUIRED'),
    ('JOB-2026-0013', 'excel',              'PREFERRED')
) AS v(reference_code, skill_slug, importance)
JOIN job_postings j ON j.reference_code = v.reference_code
JOIN skills s ON s.slug = v.skill_slug
ON CONFLICT (job_id, skill_id) DO NOTHING;

COMMIT;

-- ---------------------------------------------------------------------------
-- Summary (printed when run via psql)
-- ---------------------------------------------------------------------------
SELECT 'departments' AS table_name, count(*) AS rows FROM departments
UNION ALL SELECT 'skills',       count(*) FROM skills
UNION ALL SELECT 'job_postings', count(*) FROM job_postings
UNION ALL SELECT 'job_skills',   count(*) FROM job_skills
UNION ALL SELECT 'users',        count(*) FROM users
ORDER BY 1;
