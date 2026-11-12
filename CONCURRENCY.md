# 并发一致性设计

## 目标

考勤系统需要同时处理以下并发场景：

- 多名学生同时扫码、定位或输入数字口令签到；
- 同一学生重复点击或客户端超时重试；
- 教师重复发起、结束签到，或学生签到与教师结束签到同时发生；
- 多名教师同时审批同一条补签申请；
- 多实例定时任务或学生轮询同时生成课前提醒。

系统采用数据库约束、条件更新和 InnoDB 行锁解决这些问题，不依赖单 JVM 内的内存状态。

## 签到并发控制

每次二维码、定位或数字口令签到都会生成唯一场次标识：

```text
二维码/定位: SIGN:<sign_code_id>
数字口令:   TOKEN:<digital_token_id>
```

`attendance_records.session_key` 保存场次标识，数据库唯一索引
`(session_key, student_id)` 保证同一学生在同一场次最多只有一条记录。

签到使用单条条件更新：

```sql
UPDATE attendance_records
SET status = '已签到', sign_time = ?
WHERE session_key = ?
  AND student_id = ?
  AND status = '未签到';
```

只有受影响行数为 1 的请求会获得成功结果，并发重复请求不会重复记账。

## 场次切换控制

签到事务先对课程行执行 `SELECT ... FOR SHARE`，多个学生可以并行获得共享锁。发起签到、
结束签到和生成数字口令时对同一课程行执行 `SELECT ... FOR UPDATE`。

因此：

- 多名学生的签到请求可以并行执行；
- 教师重置或结束场次时，会等待已经开始的签到事务完成；
- 新签到只能在教师完成场次切换后读取新场次；
- 同一课程的多个场次切换请求被串行化。

结束时只执行一次条件批量更新：

```sql
UPDATE attendance_records
SET status = '缺勤'
WHERE course_id = ?
  AND status = '未签到';
```

签到成功与缺勤标记因此只有一个事务能获胜，不再出现结束签到后又被覆盖为已签到的问题。

## 补签审批

补签申请提交时锁定对应考勤记录，并使用 `INSERT IGNORE` 配合 `record_id` 唯一索引防止重复申请。

审批使用条件更新：

```sql
UPDATE attendance_applications
SET status = ?
WHERE id = ?
  AND status = 'pending';
```

并发审批或“先同意、后拒绝”只有第一个请求可以成功，后续请求返回“该申请已处理”。

## 课前提醒

课前提醒通过以下唯一索引保证幂等：

```text
(schedule_id, reminder_date, reminder_type)
```

生成提醒使用 `INSERT IGNORE`，定时任务、多实例部署和多个学生同时轮询都不会重复插入。

## 数据库自动升级

后端启动时，`ConcurrencySchemaInitializer` 会自动完成以下兼容升级：

- 为 `attendance_records` 添加 `session_key`；
- 创建课程、学生的查询索引和场次学生唯一索引；
- 为补签申请创建 `record_id` 唯一索引；
- 清理历史重复提醒并创建提醒唯一索引。

全新数据库也可以直接使用更新后的 `db.sql` 和 `update_sign_reminder.sql`。

## 验证

```bash
cd backend
mvn test
```

`ConcurrencyControlServiceTest` 会并发提交签到和补签审批，验证只有一个请求能成功修改业务数据。
