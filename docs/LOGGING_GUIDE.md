# 日志系统使用指南

## 一、日志架构概述

```
┌─────────────────────────────────────────────────┐
│              日志层次结构                         │
├─────────────────────────────────────────────────┤
│                                                 │
│  1. 请求日志层 (RequestLoggingAspect)           │
│     ├─ 记录所有HTTP请求                          │
│     ├─ 记录方法执行时间                          │
│     ├─ 性能监控（>3秒警告）                      │
│     └─ 自动脱敏敏感参数                          │
│                                                 │
│  2. 审计日志层 (AuditLogAspect)                 │
│     ├─ 记录业务操作                              │
│     ├─ 持久化到数据库                            │
│     └─ 支持审计追踪                              │
│                                                 │
│  3. 异常日志层 (GlobalExceptionHandler)          │
│     ├─ 统一异常处理                              │
│     ├─ 详细错误信息                              │
│     └─ IP地址追踪                               │
│                                                 │
│  4. 业务日志层 (Service层)                      │
│     ├─ 关键业务流程                              │
│     ├─ 区块链交互                                │
│     └─ 数据变更记录                              │
│                                                 │
└─────────────────────────────────────────────────┘
```

## 二、日志级别说明

| 级别 | 用途 | 示例场景 |
|------|------|---------|
| **ERROR** | 错误日志 | 系统异常、数据库连接失败 |
| **WARN** | 警告日志 | 参数校验失败、认证失败、慢查询 |
| **INFO** | 信息日志 | 请求开始/结束、业务操作成功 |
| **DEBUG** | 调试日志 | 请求参数、SQL语句、详细流程 |
| **TRACE** | 跟踪日志 | 更细粒度的执行流程 |

## 三、日志格式

### 请求日志格式

```
>>> 请求开始: method=POST, uri=/api/bills, class=BillController, method=issueBill, user=admin, ip=192.168.1.100
<<< 请求成功: method=POST, uri=/api/bills, duration=1250ms
```

### 慢请求警告

```
⚠️  慢请求: method=POST, uri=/api/bills, duration=3500ms
```

### 异常日志格式

```
✗ 请求失败: method=POST, uri=/api/bills, class=BillController, method=issueBill, user=admin, duration=500ms, error=企业账户不存在
```

### 业务日志格式

```
2026-01-18 10:30:45 [INFO] 开具票据: billId=BILL001, amount=100000.00, issuer=0x123...
2026-01-18 10:30:46 [INFO] 票据上链成功: billId=BILL001, txHash=0xabc...
```

## 四、日志位置

### 日志文件

- **位置**: `/home/llm_rca/fisco/my-bcos-app/application.log`
- **滚动**: 每天自动滚动
- **保留**: 7天
- **格式**: `application.log.YYYY-MM-DD.0.gz`

### 控制台输出

启动应用时，日志会实时输出到控制台：
```bash
./start.sh
```

## 五、查看日志

### 1. 实时查看日志

```bash
# 查看最新日志
tail -f application.log

# 只看ERROR日志
tail -f application.log | grep ERROR

# 只看特定用户的操作
tail -f application.log | grep "user=admin"
```

### 2. 查看历史日志

```bash
# 查看最近100行
tail -100 application.log

# 查看特定时间段的日志
grep "2026-01-18 10:" application.log

# 查看慢请求
grep "慢请求" application.log

# 查看所有异常
grep "请求失败" application.log
```

### 3. 统计分析

```bash
# 统计请求量（按小时）
grep "请求开始" application.log | cut -d: -f1-2 | sort | uniq -c

# 统计错误数量
grep "ERROR" application.log | wc -l

# 统计最慢的10个请求
grep "duration=" application.log | sort -t= -k2 -rn | head -10
```

## 六、日志配置

### application.properties 配置

```properties
# 日志级别配置
logging.level.root=INFO                              # 根日志级别
logging.level.com.fisco.app=DEBUG                    # 应用日志级别
logging.level.org.fisco.bcos.sdk=INFO               # SDK日志级别
logging.level.org.springframework.web=INFO           # Web日志级别
logging.level.org.hibernate.SQL=DEBUG                # SQL日志
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE  # SQL参数

# 日志文件配置
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
logging.file.path=logs
logging.file.name=application.log
```

### 修改日志级别

#### 方式1: 修改配置文件

编辑 `application.properties`:
```properties
logging.level.com.fisco.app=TRACE  # 更详细的日志
```

#### 方式2: 动态调整（运行时）

使用 Actuator 端点（需要集成）:
```bash
curl -X POST http://localhost:8080/actuator/loggers/com.fisco.app \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'
```

## 七、性能监控日志

### 慢请求阈值

当前阈值：**3000ms (3秒)**

超过阈值的请求会记录警告日志：
```java
// RequestLoggingAspect.java
if (duration > 3000) {
    log.warn("⚠️  慢请求: method={}, uri={}, duration={}ms", ...);
}
```

### 自定义阈值

编辑 `RequestLoggingAspect.java`:
```java
// 修改慢请求阈值
private static final long SLOW_REQUEST_THRESHOLD = 5000; // 5秒

if (duration > SLOW_REQUEST_THRESHOLD) {
    log.warn("⚠️  慢请求: ...");
}
```

## 八、敏感数据脱敏

### 自动脱敏

以下字段会自动脱敏（显示为 `******`）：
- `password`
- `secret`

### 脱敏规则

```java
// RequestLoggingAspect.java
private String maskSensitiveData(String data) {
    return data.replaceAll("\"password\":\"[^\"]*\"", "\"password\":\"******\"")
               .replaceAll("\"secret\":\"[^\"]*\"", "\"secret\":\"******\"");
}
```

### 自定义脱敏

如需添加更多脱敏字段，修改 `RequestLoggingAspect`:
```java
private String maskSensitiveData(String data) {
    return data.replaceAll("\"password\":\"[^\"]*\"", "\"password\":\"******\"")
               .replaceAll("\"secret\":\"[^\"]*\"", "\"secret\":\"******\"")
               .replaceAll("\"apiKey\":\"[^\"]*\"", "\"apiKey\":\"******\"")  // 新增
               .replaceAll("\"token\":\"[^\"]*\"", "\"token\":\"******\"");   // 新增
}
```

## 九、审计日志

### 审计日志记录

使用 `@Audited` 注解标记需要审计的方法：

```java
@Audited(
    module = "票据管理",
    actionType = "开具",
    actionDesc = "开具新票据",
    entityType = "Bill",
    logRequest = true,
    logResponse = false
)
public Bill issueBill(IssueBillRequest request) {
    // 业务逻辑
}
```

### 查询审计日志

审计日志存储在数据库的 `audit_log` 表中：

```sql
-- 查询最近的操作记录
SELECT * FROM audit_log ORDER BY created_at DESC LIMIT 100;

-- 查询特定用户的操作
SELECT * FROM audit_log WHERE user_address = '0x123...' ORDER BY created_at DESC;

-- 查询失败的操作
SELECT * FROM audit_log WHERE is_success = false ORDER BY created_at DESC;
```

## 十、日志最佳实践

### 1. 开发环境

```properties
logging.level.com.fisco.app=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

### 2. 生产环境

```properties
logging.level.com.fisco.app=INFO
logging.level.org.hibernate.SQL=WARN
logging.level.org.springframework=WARN
```

### 3. 日志记录原则

✅ **推荐做法**：
```java
// 记录关键业务流程
log.info("创建票据: billId={}, amount={}", billId, amount);

// 记录异常（带堆栈）
log.error("票据创建失败: billId={}", billId, e);

// 记录警告
log.warn("企业未激活: address={}", address);
```

❌ **避免做法**：
```java
// 不要在生产环境使用DEBUG
log.debug("详细调试信息...");

// 不要打印敏感信息
log.info("用户密码: {}", password);  // 错误！

// 不要吞掉异常
try {
    // ...
} catch (Exception e) {
    // 不要什么都不做
}
```

## 十一、故障排查

### 1. 请求失败

查看请求失败日志：
```bash
grep "请求失败" application.log | tail -20
```

### 2. 性能问题

查看慢请求：
```bash
grep "慢请求" application.log | tail -20
```

### 3. 数据库问题

查看SQL日志（需DEBUG级别）：
```bash
grep "Hibernate:" application.log | tail -50
```

### 4. 区块链问题

查看区块链交互日志：
```bash
grep "区块链\|上链\|合约" application.log | tail -50
```

## 十二、日志管理建议

### 日志保留策略

- **开发环境**: 7天
- **测试环境**: 30天
- **生产环境**: 90天

### 日志告警

建议设置告警规则：
- ERROR日志数量 > 100/小时
- 慢请求数量 > 50/小时
- 特定异常出现

### 日志分析工具

推荐使用：
- **ELK Stack** (Elasticsearch + Logstash + Kibana)
- **Graylog**
- **Splunk**
- **阿里云日志服务**

## 十三、快速参考

### 常用日志命令

```bash
# 实时查看
tail -f application.log

# 查看错误
grep ERROR application.log

# 查看特定时间段
sed -n '/2026-01-18 10:00/,/2026-01-18 11:00/p' application.log

# 统计访问量
grep "请求开始" application.log | wc -l

# 查找慢请求
grep "duration=" application.log | awk -F'duration=' '{print $2}' | awk -F'ms' '{if($1>3000) print $0}' | sort -t= -k2 -rn | head -10
```

### 日志级别对照

```
TRACE → DEBUG → INFO → WARN → ERROR
  ↑        ↑      ↑      ↑      ↑
  最详细  最详细  正常   警告   错误
```

---

**相关文档**:
- [审计日志指南](AUDIT_LOG_GUIDE.md)
- [性能优化指南](PERFORMANCE_TUNING.md)
- [故障排查手册](TROUBLESHOOTING.md)
