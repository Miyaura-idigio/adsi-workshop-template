-- password: admin1234 (BCrypt encoded)
INSERT INTO employees (employee_code, name, email, password, role, active, version, created_at, updated_at)
VALUES ('EMP001', '管理者', 'admin@example.com', '$2a$10$y9Ok1/062g5uFUy.4zIMGePkw2qIQTLC/q.40Ih9BCuQ1ItHy5PgO', 'ADMIN', TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
