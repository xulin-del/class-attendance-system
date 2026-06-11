# 课堂考勤系统 - 部署运行说明

## 一、系统架构

```
课堂考勤系统
├── backend/          # SpringBoot后端
│   ├── src/main/java/com/attendance/
│   │   ├── Application.java          # 启动类
│   │   ├── entity/                   # 实体类
│   │   ├── mapper/                   # 数据访问层
│   │   ├── service/                  # 业务逻辑层
│   │   ├── controller/               # 控制器层
│   │   ├── config/                   # 配置类
│   │   └── common/                   # 公共类
│   └── src/main/resources/
│       ├── application.properties    # 配置文件
│       └── mapper/                   # MyBatis XML
├── miniprogram/      # 微信小程序前端
│   ├── pages/                        # 页面
│   │   ├── login/                    # 登录页
│   │   ├── teacher/                  # 教师端页面
│   │   └── student/                  # 学生端页面
│   ├── utils/                        # 工具类
│   ├── app.js                        # 小程序入口
│   ├── app.json                      # 小程序配置
│   └── app.wxss                      # 全局样式
└── db.sql            # 数据库脚本
```

## 二、环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 8.0+
- 微信开发者工具

## 三、数据库配置

1. 创建数据库并导入数据：
```sql
-- 在MySQL中执行 db.sql 文件
source d:/JAVA/小程序/db.sql
```

2. 测试账号：
- 教师：工号 T001，密码 123456
- 学生：学号 S001，姓名 学生1
- 学生：学号 S002，姓名 学生2
- 学生：学号 S003，姓名 学生3

## 四、后端部署

1. 修改数据库配置：
```properties
# 打开 backend/src/main/resources/application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/attendance_system?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=你的数据库密码
```

2. 编译运行：
```bash
cd backend
mvn clean package
java -jar target/attendance-system-1.0.0.jar
```

或在IDE中直接运行 Application.java

3. 验证后端：
访问 http://localhost:8080/api/teacher/login 应返回错误信息

## 五、小程序配置

1. 打开微信开发者工具，导入项目：
   - 项目目录：d:/JAVA/小程序/miniprogram
   - AppID：使用测试号或自己的AppID

2. 修改后端地址（如需修改）：
```javascript
// 打开 miniprogram/app.js
globalData: {
  baseUrl: 'http://localhost:8080/api',  // 修改为实际后端地址
  ...
}
```

3. 配置服务器域名（正式环境）：
   - 登录微信公众平台
   - 开发 -> 开发管理 -> 开发设置 -> 服务器域名
   - 添加 request 合法域名

4. 本地调试设置：
   - 微信开发者工具 -> 详情 -> 本地设置
   - 勾选"不校验合法域名、web-view（业务域名）、TLS版本以及HTTPS证书"

## 六、功能说明

### 教师端功能
1. 首页：显示今日课程和考勤概览
2. 课程管理：增删改查课程
3. 发起签到：
   - 扫码签到：生成二维码，设置5/10分钟倒计时
   - 定位签到：获取教师位置，50米范围校验
   - 实时显示签到人数
   - 签到结束自动标记缺勤
4. 考勤统计：查看已签到/未签到/缺勤学生
5. 我的：个人信息和退出登录

### 学生端功能
1. 首页：显示可签到课程和考勤概览
2. 签到中心：扫码签到和定位签到
3. 我的考勤：查看个人考勤记录
4. 我的：个人信息和退出登录

## 七、API接口

### 教师接口
- POST /api/teacher/login - 教师登录
- GET /api/teacher/info - 获取教师信息

### 学生接口
- POST /api/student/login - 学生登录
- GET /api/student/info - 获取学生信息

### 课程接口
- GET /api/course/list - 获取教师课程列表
- GET /api/course/listByClass - 获取班级课程列表
- POST /api/course/add - 添加课程
- POST /api/course/update - 更新课程
- POST /api/course/delete - 删除课程

### 考勤接口
- POST /api/attendance/start - 发起签到
- POST /api/attendance/signIn - 学生签到
- POST /api/attendance/end - 结束签到
- GET /api/attendance/records - 获取考勤记录
- GET /api/attendance/studentRecords - 获取学生考勤记录
- GET /api/attendance/statistics - 获取考勤统计

## 八、常见问题

1. 后端启动失败：
   - 检查数据库连接配置
   - 确保MySQL服务已启动
   - 确保数据库已创建

2. 小程序请求失败：
   - 检查后端是否启动
   - 检查baseUrl配置
   - 开发环境勾选不校验域名

3. 签到失败：
   - 确保教师已发起签到
   - 检查学生是否在正确班级
   - 检查签到是否已结束

## 九、项目特点

1. 完整的双角色系统：教师端和学生端独立功能
2. 底部TabBar导航：所有主页面都有固定底部导航栏
3. 现代化UI设计：卡片式布局、大圆角、轻微阴影
4. 实时签到：支持扫码和定位两种签到方式
5. 自动缺勤标记：签到结束自动将未签到学生标记为缺勤
6. 登录状态持久化：支持退出登录功能