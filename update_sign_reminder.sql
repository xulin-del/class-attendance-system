CREATE TABLE IF NOT EXISTS sign_reminders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    course_id INT NOT NULL,
    course_name VARCHAR(100),
    class_name VARCHAR(50),
    sign_code VARCHAR(50),
    teacher_id VARCHAR(50),
    status VARCHAR(20) DEFAULT 'pending',
    reminder_type VARCHAR(20) DEFAULT 'sign',
    schedule_id INT,
    reminder_date DATE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    expire_time DATETIME,
    FOREIGN KEY (course_id) REFERENCES courses(course_id)
);

-- 旧表升级时如果缺少以下字段，可手动执行；后端启动时也会自动补齐。
-- ALTER TABLE sign_reminders ADD COLUMN reminder_type VARCHAR(20) DEFAULT 'sign';
-- ALTER TABLE sign_reminders ADD COLUMN schedule_id INT NULL;
-- ALTER TABLE sign_reminders ADD COLUMN reminder_date DATE NULL;
