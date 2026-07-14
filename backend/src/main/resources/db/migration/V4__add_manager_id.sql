ALTER TABLE employees ADD COLUMN manager_id BIGINT;
ALTER TABLE employees ADD CONSTRAINT fk_employee_manager FOREIGN KEY (manager_id) REFERENCES employees(id);
