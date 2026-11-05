-- Departments become their own table. Existing employees keep their
-- department: each distinct name (ignoring case and surrounding spaces)
-- becomes one department row, and employees are linked to it by id.
CREATE TABLE department (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_department_name UNIQUE (name)
);

INSERT INTO department (name)
SELECT MIN(TRIM(department)) FROM employee GROUP BY LOWER(TRIM(department));

ALTER TABLE employee ADD COLUMN department_id BIGINT;

UPDATE employee SET department_id = (
    SELECT d.id FROM department d
    WHERE LOWER(d.name) = LOWER(TRIM(employee.department))
);

ALTER TABLE employee MODIFY department_id BIGINT NOT NULL;

ALTER TABLE employee ADD CONSTRAINT fk_employee_department
    FOREIGN KEY (department_id) REFERENCES department (id);

ALTER TABLE employee DROP COLUMN department;
