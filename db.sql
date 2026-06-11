-- 创建数据库
CREATE DATABASE IF NOT EXISTS attendance_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE attendance_system;

-- 创建教师表
CREATE TABLE IF NOT EXISTS teachers (
    teacher_id VARCHAR(20) PRIMARY KEY,
    teacher_name VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    phone VARCHAR(20)
);

-- 创建学生表
CREATE TABLE IF NOT EXISTS students (
    student_id VARCHAR(20) PRIMARY KEY,
    student_name VARCHAR(50) NOT NULL,
    class_name VARCHAR(50) NOT NULL,
    password VARCHAR(100),
    phone VARCHAR(20)
);

-- 创建课程表
CREATE TABLE IF NOT EXISTS courses (
    course_id INT AUTO_INCREMENT PRIMARY KEY,
    course_name VARCHAR(100) NOT NULL,
    teacher_id VARCHAR(20) NOT NULL,
    class_name VARCHAR(50) NOT NULL,
    FOREIGN KEY (teacher_id) REFERENCES teachers(teacher_id)
);

-- 创建考勤记录表
CREATE TABLE IF NOT EXISTS attendance_records (
    record_id INT AUTO_INCREMENT PRIMARY KEY,
    course_id INT NOT NULL,
    student_id VARCHAR(20) NOT NULL,
    sign_time DATETIME,
    status VARCHAR(10) NOT NULL DEFAULT '未签到',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES courses(course_id),
    FOREIGN KEY (student_id) REFERENCES students(student_id)
);

-- 创建签到码表（持久化存储签到码）
CREATE TABLE IF NOT EXISTS sign_codes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sign_code VARCHAR(50) NOT NULL UNIQUE,
    course_id INT NOT NULL,
    status VARCHAR(20) DEFAULT 'active',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    expire_time DATETIME,
    FOREIGN KEY (course_id) REFERENCES courses(course_id)
);

-- 创建定位签到表
CREATE TABLE IF NOT EXISTS location_signs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    course_id INT NOT NULL,
    sign_code VARCHAR(50) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    radius INT DEFAULT 100,
    status VARCHAR(20) DEFAULT '进行中',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    end_time DATETIME,
    FOREIGN KEY (course_id) REFERENCES courses(course_id)
);

-- 创建数字口令表
CREATE TABLE IF NOT EXISTS digital_tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    course_id INT NOT NULL,
    teacher_id VARCHAR(20) NOT NULL,
    token VARCHAR(10) NOT NULL,
    status VARCHAR(20) DEFAULT 'active',
    expire_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES courses(course_id)
);

-- 插入测试数据
-- 插入1个教师
INSERT INTO teachers (teacher_id, teacher_name, password, phone) VALUES ('T001', '张老师', '123456', '13800000001');

-- 插入3个学生
INSERT INTO students (student_id, student_name, class_name, password, phone) VALUES
('S001', '学生1', '班级1', '123456', '13800000002'),
('S002', '学生2', '班级1', '123456', '13800000003'),
('S003', '学生3', '班级1', '123456', '13800000004');

-- 插入课程
INSERT INTO courses (course_name, teacher_id, class_name) VALUES ('高等数学', 'T001', '班级1');
INSERT INTO courses (course_name, teacher_id, class_name) VALUES ('数据结构', 'T001', '班级1');

-- 如果表已存在，添加缺失的字段
-- ALTER TABLE attendance_records ADD COLUMN IF NOT EXISTS create_time DATETIME DEFAULT CURRENT_TIMESTAMP;
