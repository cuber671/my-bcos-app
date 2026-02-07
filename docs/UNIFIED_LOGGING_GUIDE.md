# 统一日志目录配置说明

## 📂 新的日志结构

所有日志文件现在统一存储在 `logs/` 目录中：

```
my-bcos-app/
└── logs/                          ← 统一的日志目录
    ├── README.md                  (日志目录说明)
    ├── my-bcos-app.log            (主日志 - 所有日志)
    ├── my-bcos-app-error.log      (错误日志 - 仅ERROR)
    ├── my-bcos-app-sql.log        (SQL日志 - 数据库操作)
    ├── my-bcos-app-blockchain.log (区块链日志 - FISCO BCOS)
    ├── my-bcos-app-request.log    (请求日志 - HTTP请求)
    │
    └── *.gz                       (历史日志，已压缩)
```

## 🎯 改进对比

### 改进前
```
my-bcos-app/
├── application.log                (散落在项目根目录)
├── application.log.2026-01-13.0.gz
├── application.log.2026-01-16.0.gz
└── ...                            (与其他文件混在一起)
```

### 改进后
```
my-bcos-app/
└── logs/                          (集中管理)
    ├── my-bcos-app.log
    ├── my-bcos-app-error.log
    ├── my-bcos-app-sql.log
    ├── my-bcos-app-blockchain.log
    ├── my-bcos-app-request.log
    └── ... (分类清晰)
```

## 📄 日志文件详解

| 文件 | 用途 | 保留时间 | 特点 |
|------|------|---------|------|
| **my-bcos-app.log** | 主日志，记录所有级别日志 | 30天 | 最全面，适合整体追踪 |
| **my-bcos-app-error.log** | 仅ERROR级别 | 90天 | 快速定位错误，保留更久 |
| **my-bcos-app-sql.log** | 数据库SQL操作 | 7天 | 调试数据库性能问题 |
| **my-bcos-app-blockchain.log** | FISCO BCOS操作 | 30天 | 区块链交易追踪 |
| **my-bcos-app-request.log** | HTTP请求和响应 | 15天 | API性能监控 |

## 🚀 快速开始

### 1. 查看日志位置

```bash
cd /home/llm_rca/fisco/my-bcos-app/logs
ls -lh
```

### 2. 实时查看主日志

```bash
tail -f my-bcos-app.log
```

### 3. 查看错误日志

```bash
# 实时查看错误
tail -f my-bcos-app-error.log

# 查看最近50个错误
tail -50 my-bcos-app-error.log
```

### 4. 查看SQL日志

```bash
tail -f my-bcos-app-sql.log
```

### 5. 查看请求日志

```bash
tail -f my-bcos-app-request.log
```

### 6. 查看区块链日志

```bash
tail -f my-bcos-app-blockchain.log
```

## 🔍 常用场景

### 场景1: 排查错误

```bash
# 步骤1: 查看最近的错误
tail -50 my-bcos-app-error.log

# 步骤2: 在主日志中查找上下文
grep -A 10 -B 10 "错误信息" my-bcos-app.log

# 步骤3: 查看相关请求
grep "2026-01-18 10:30" my-bcos-app-request.log
```

### 场景2: 性能分析

```bash
# 查找慢请求
grep "慢请求" my-bcos-app-request.log

# 查看请求时间分布
grep "duration=" my-bcos-app-request.log | awk -F'duration=' '{print $2}' | sort -n
```

### 场景3: 数据库调试

```bash
# 查看执行的SQL
tail -f my-bcos-app-sql.log

# 查找特定表的查询
grep "SELECT.*bill" my-bcos-app-sql.log
```

### 场景4: 区块链交易追踪

```bash
# 查看区块链操作
tail -f my-bcos-app-blockchain.log

# 搜索交易哈希
grep "txHash" my-bcos-app-blockchain.log
```

## ⚙️ 配置文件

日志配置位于: `src/main/resources/logback-spring.xml`

### 关键配置

```xml
<!-- 日志目录 -->
<property name="LOG_HOME" value="logs"/>

<!-- 主日志保留30天 -->
<maxHistory>30</maxHistory>

<!-- 错误日志保留90天 -->
<maxHistory>90</maxHistory>

<!-- SQL日志保留7天 -->
<maxHistory>7</maxHistory>

<!-- 单文件最大100MB -->
<maxFileSize>100MB</maxFileSize>

<!-- 总大小不超过5GB -->
<totalSizeCap>5GB</totalSizeCap>
```

## 📊 日志管理

### 查看磁盘占用

```bash
cd logs
du -sh .                    # 总大小
du -sh *.log                # 各日志文件大小
du -sh *.gz                 # 压缩文件大小
```

### 清理旧日志

日志会自动清理，无需手动操作。如需手动清理：

```bash
# 删除30天前的日志（已过期）
find . -name "*.gz" -mtime +30 -delete

# 删除所有压缩的历史日志（谨慎操作）
rm *.gz
```

## 💡 使用技巧

### 1. 多日志同时查看

```bash
# 同时监控主日志和错误日志
tail -f my-bcos-app.log my-bcos-app-error.log
```

### 2. 过滤特定内容

```bash
# 只看ERROR级别
grep "ERROR" my-bcos-app.log

# 只看特定时间范围
sed -n '/2026-01-18 10:00/,/2026-01-18 11:00/p' my-bcos-app.log

# 排除SQL日志
grep -v "Hibernate:" my-bcos-app.log
```

### 3. 统计分析

```bash
# 统计错误数量
grep ERROR my-bcos-app-error.log | wc -l

# 统计请求量（按小时）
grep "请求开始" my-bcos-app-request.log | cut -d: -f1-2 | sort | uniq -c

# 统计最慢的10个请求
grep "duration=" my-bcos-app-request.log | sort -t= -k2 -rn | head -10
```

## 🎁 额外功能

### 异步日志
日志使用异步写入，不影响应用性能：
```xml
<appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
    <queueSize>512</queueSize>
    <appender-ref ref="FILE_ALL"/>
</appender>
```

### 日志压缩
历史日志自动使用gzip压缩，节省磁盘空间：
```bash
# 查看压缩日志
zcat my-bcos-app.log.2026-01-16.0.gz
```

### 日志滚动
- **按时间**: 每天自动创建新文件
- **按大小**: 文件超过100MB自动滚动
- **命名格式**: `my-bcos-app.log.YYYY-MM-DD.N.gz`

## 📝 注意事项

1. **首次运行**: 启动应用后会自动创建 `logs/` 目录和日志文件
2. **修改配置**: 修改 `logback-spring.xml` 后需重启应用
3. **磁盘空间**: 确保磁盘有足够空间（建议至少10GB）
4. **权限问题**: 确保应用有写入 `logs/` 目录的权限

## 🔗 相关文档

- [日志使用完整指南](LOGGING_GUIDE.md)
- [日志目录说明](../logs/README.md)
- [日志改进总结](LOGGING_IMPROVEMENTS.md)

---

**配置完成时间**: 2026-01-18
**配置文件**: `src/main/resources/logback-spring.xml`
