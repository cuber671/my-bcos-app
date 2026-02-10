```
/home/llm_rca/fisco/my-bcos-app/
│
├── 📁 .github/                          # GitHub Actions 工作流
│
├── 📁 account/                          # 区块链账户存储目录
│   └── ecdsa/                          # ECDSA 密钥对文件
│
├── 📁 accounts/                         # 额外的账户文件
│
├── 📁 api-docs/                         # API 文档（8个文件）
│   ├── openapi.yaml                    # OpenAPI 3.0 规范
│   ├── API.md                          # RESTful API 文档
│   ├── QUICK_START.md                  # 快速入门指南
│   └── ...
│
├── 📁 conf/                             # FISCO BCOS 证书目录
│   ├── ca.crt                          # CA 根证书
│   ├── sdk.crt                         # SDK 证书
│   ├── sdk.key                         # SDK 私钥
│   └── cert.cnf                        # 证书配置文件
│
├── 📁 database/                         # 数据库相关文件
│
├── 📁 docs/                             # 完整文档（60+个Markdown文件）
│   ├── API_DOCS.md                     # API 详细文档
│   ├── PROJECT_STRUCTURE.md             # 项目结构说明
│   ├── SUPPLY_CHAIN_FINANCE_README.md  # 供应链金融说明
│   ├── PERMISSION_AND_AUTH_SYSTEM.md   # 权限认证系统
│   └── ...
│
├── 📁 image/                            # 图片/架构图
│
├── 📁 logs/                             # 应用运行日志目录
│   ├── my-bcos-app.log                 # 全部日志（30天保留）
│   ├── my-bcos-app-error.log           # 错误日志（90天保留）
│   ├── my-bcos-app-sql.log             # SQL日志（7天保留）
│   ├── my-bcos-app-blockchain.log      # 区块链操作日志
│   └── my-bcos-app-request.log         # HTTP请求日志
│
├── 📁 scripts/                          # Shell脚本工具（14个文件）
│   ├── start.sh                        # 应用启动脚本
│   ├── deploy-contract.sh              # 智能合约部署
│   ├── deploy-overdue-contract.sh      # 逾期合约部署
│   ├── DEPLOY_COMPLETE.sh              # 完整部署检查
│   ├── test_bill_lifecycle.sh          # 票据生命周期测试
│   ├── test_bill_pool.sh               # 票据池测试
│   ├── test_bill_financing.sh          # 票据融资测试
│   ├── test_endorsement.sh             # 背书测试
│   ├── check_chain.sh                  # 区块链连通性检查
│   ├── check_chain_enterprises.sh      # 链上企业检查
│   ├── list_all_chain_enterprises.sh   # 列出所有链上企业
│   ├── diagnose_db.sh                  # 数据库诊断
│   ├── generate-password.sh            # 密码生成工具
│   └── fix_vscode_imports.sh           # VS Code导入修复
│
├── 📁 sql/                              # SQL 脚本文件
│
├── 📁 src/                              # 源代码目录
│   └── main/
│       ├── java/com/fisco/app/
│       │   ├── 📄 BcosApplication.java              # Spring Boot 主入口
│       │   │
│       │   ├── 📁 annotation/                     # 自定义注解（8KB）
│       │   │   ├── RequireAdmin.java              # 管理员权限注解
│       │   │   ├── RequireEnterprise.java         # 企业权限注解
│       │   │   └── RequireRole.java               # 角色权限注解
│       │   │
│       │   ├── 📁 aspect/                         # AOP切面（44KB）
│       │   │   ├── LogAspect.java                # 日志记录切面
│       │   │   ├── PermissionAspect.java          # 权限检查切面
│       │   │   └── BlockchainAspect.java          # 区块链操作切面
│       │   │
│       │   ├── 📁 config/                         # 配置类（40KB）
│       │   │   ├── BcosConfig.java               # FISCO BCOS SDK配置
│       │   │   ├── SecurityConfig.java           # 安全配置（JWT/CORS）
│       │   │   ├── SwaggerConfig.java            # API文档配置
│       │   │   ├── AsyncConfig.java              # 异步处理配置
│       │   │   └── DruidConfig.java              # 数据库连接池配置
│       │   │
│       │   ├── 📁 contract/                       # 智能合约Java包装类（1.4MB）
│       │   │   └── com/fisco/app/contract/
│       │   │       ├── Bill.sol                  # 票据合约
│       │   │       ├── BillOptimized.sol         # 票据合约优化版
│       │   │       ├── CreditLimit.sol           # 授信额度合约
│       │   │       ├── DataPackUtils.sol         # 数据打包工具
│       │   │       ├── EnterpriseRegistry.sol    # 企业注册合约
│       │   │       ├── Receivable.sol            # 应收账款合约
│       │   │       ├── ReceivableOptimized.sol   # 应收账款优化版
│       │   │       ├── ReceivableWithOverdue.sol # 带逾期管理的应收账款
│       │   │       ├── WarehouseReceipt.sol      # 仓单合约
│       │   │       ├── WarehouseReceiptOptimized.sol
│       │   │       └── WarehouseReceiptWithFreeze.sol
│       │   │
│       │   ├── 📁 controller/                     # REST控制器（428KB）
│       │   │   ├── admin/                        # 管理员管理
│       │   │   │   ├── AdminEnterpriseController.java
│       │   │   │   ├── AdminSystemController.java
│       │   │   │   └── AdminCreditController.java
│       │   │   ├── bill/                         # 票据管理
│       │   │   │   ├── BillController.java
│       │   │   │   ├── BillPoolController.java
│       │   │   │   └── BillFinancingController.java
│       │   │   ├── blockchain/                   # 区块链操作
│       │   │   │   ├── BlockchainStatusController.java
│       │   │   │   └── ContractController.java
│       │   │   ├── credit/                       # 授信管理
│       │   │   │   └── CreditLimitController.java
│       │   │   ├── endorsement/                  # 背书处理
│       │   │   │   └── EndorsementController.java
│       │   │   ├── enterprise/                   # 企业管理
│       │   │   │   ├── EnterpriseController.java
│       │   │   │   └── EnterpriseAuthController.java
│       │   │   ├── notification/                 # 通知系统
│       │   │   │   └── NotificationController.java
│       │   │   ├── pledge/                       # 质押管理
│       │   │   │   └── PledgeController.java
│       │   │   ├── receivable/                   # 应收账款
│       │   │   │   └── ReceivableController.java
│       │   │   ├── risk/                         # 风险管理
│       │   │   │   └── RiskMonitoringController.java
│       │   │   ├── system/                       # 系统工具
│       │   │   │   ├── AuditLogController.java
│       │   │   │   ├── StatisticsController.java
│       │   │   │   └── DataMigrationController.java
│       │   │   ├── user/                         # 用户管理
│       │   │   │   └── UserController.java
│       │   │   └── warehouse/                    # 仓单管理
│       │   │       └── WarehouseReceiptController.java
│       │   │
│       │   ├── 📁 deployment/                     # 部署工具（4KB）
│       │   │   └── ContractDeployer.java         # 合约部署工具
│       │   │
│       │   ├── 📁 dto/                            # 数据传输对象（580KB）
│       │   │   ├── audit/
│       │   │   │   └── AuditLogDTO.java
│       │   │   ├── bill/
│       │   │   │   ├── BillDTO.java
│       │   │   │   ├── BillCreateRequest.java
│       │   │   │   └── BillEndorseRequest.java
│       │   │   ├── credit/
│       │   │   │   └── CreditLimitDTO.java
│       │   │   ├── endorsement/
│       │   │   │   └── EndorsementDTO.java
│       │   │   ├── enterprise/
│       │   │   │   ├── EnterpriseDTO.java
│       │   │   │   ├── EnterpriseRegisterRequest.java
│       │   │   │   └── EnterpriseApprovalRequest.java
│       │   │   ├── notification/
│       │   │   │   └── NotificationDTO.java
│       │   │   ├── pledge/
│       │   │   │   └── PledgeDTO.java
│       │   │   ├── receivable/
│       │   │   │   ├── ReceivableDTO.java
│       │   │   │   └── ReceivableCreateRequest.java
│       │   │   ├── risk/
│       │   │   │   └── RiskMetricsDTO.java
│       │   │   ├── statistics/
│       │   │   │   └── StatisticsDTO.java
│       │   │   ├── user/
│       │   │   │   ├── UserDTO.java
│       │   │   │   └── LoginRequest.java
│       │   │   └── warehouse/
│       │   │       └── WarehouseReceiptDTO.java
│       │   │
│       │   ├── 📁 entity/                         # JPA实体（400KB）
│       │   │   ├── bill/
│       │   │   │   ├── Bill.java
│       │   │   │   └── BillEndorsement.java
│       │   │   ├── credit/
│       │   │   │   └── CreditLimit.java
│       │   │   ├── enterprise/
│       │   │   │   └── Enterprise.java
│       │   │   ├── notification/
│       │   │   │   └── Notification.java
│       │   │   ├── pledge/
│       │   │   │   └── Pledge.java
│       │   │   ├── receivable/
│       │   │   │   └── Receivable.java
│       │   │   ├── risk/
│       │   │   │   └── RiskMonitoring.java
│       │   │   ├── system/
│       │   │   │   └── AuditLog.java
│       │   │   ├── user/
│       │   │   │   └── User.java
│       │   │   └── warehouse/
│       │   │       └── WarehouseReceipt.java
│       │   │
│       │   ├── 📁 enums/                          # 枚举类（28KB）
│       │   │   ├── UserRole.java                 # 用户角色
│       │   │   ├── EnterpriseStatus.java         # 企业状态
│       │   │   ├── EnterpriseType.java           # 企业类型
│       │   │   ├── BillStatus.java               # 票据状态
│       │   │   ├── ReceivableStatus.java         # 应收账款状态
│       │   │   ├── RiskLevel.java                # 风险等级
│       │   │   └── NotificationType.java         # 通知类型
│       │   │
│       │   ├── 📁 event/                          # 事件类（24KB）
│       │   │   ├── BlockchainEvent.java          # 区块链事件
│       │   │   └── BusinessEvent.java            # 业务事件
│       │   │
│       │   ├── 📁 exception/                      # 异常处理（16KB）
│       │   │   ├── BusinessException.java        # 业务异常
│       │   │   ├── BlockchainException.java      # 区块链异常
│       │   │   └── AuthException.java            # 认证异常
│       │   │
│       │   ├── 📁 repository/                     # JPA数据访问层（256KB）
│       │   │   ├── bill/
│       │   │   │   └── BillRepository.java
│       │   │   ├── credit/
│       │   │   │   └── CreditLimitRepository.java
│       │   │   ├── enterprise/
│       │   │   │   └── EnterpriseRepository.java
│       │   │   ├── notification/
│       │   │   │   └── NotificationRepository.java
│       │   │   ├── pledge/
│       │   │   │   └── PledgeRepository.java
│       │   │   ├── receivable/
│       │   │   │   └── ReceivableRepository.java
│       │   │   ├── risk/
│       │   │   │   └── RiskMonitoringRepository.java
│       │   │   ├── system/
│       │   │   │   └── AuditLogRepository.java
│       │   │   ├── user/
│       │   │   │   └── UserRepository.java
│       │   │   └── warehouse/
│       │   │       └── WarehouseReceiptRepository.java
│       │   │
│       │   ├── 📁 security/                       # 安全与认证（96KB）
│       │   │   ├── annotations/
│       │   │   │   └── *.java
│       │   │   ├── JwtTokenProvider.java         # JWT令牌生成器
│       │   │   ├── JwtAuthenticationFilter.java  # JWT认证过滤器
│       │   │   ├── PermissionChecker.java        # 权限检查器
│       │   │   ├── UserAuthentication.java       # 用户认证
│       │   │   ├── PasswordUtil.java             # 密码工具
│       │   │   ├── AdminAuthInterceptor.java     # 管理员拦截器
│       │   │   └── EnterpriseAuthInterceptor.java # 企业拦截器
│       │   │
│       │   ├── 📁 service/                        # 业务逻辑层（856KB）
│       │   │   ├── bill/
│       │   │   │   ├── BillService.java
│       │   │   │   ├── BillPoolService.java
│       │   │   │   └── BillFinancingService.java
│       │   │   ├── blockchain/
│       │   │   │   ├── BlockchainService.java
│       │   │   │   └── ContractService.java
│       │   │   ├── credit/
│       │   │   │   └── CreditLimitService.java
│       │   │   ├── enterprise/
│       │   │   │   ├── EnterpriseService.java
│       │   │   │   └── EnterpriseApprovalService.java
│       │   │   ├── impl/                         # 服务实现
│       │   │   │   └── *.java
│       │   │   ├── notification/
│       │   │   │   └── NotificationService.java
│       │   │   ├── pledge/
│       │   │   │   └── PledgeService.java
│       │   │   ├── receivable/
│       │   │   │   └── ReceivableService.java
│       │   │   ├── risk/
│       │   │   │   └── RiskMonitoringService.java
│       │   │   ├── system/
│       │   │   │   ├── AuditLogService.java
│       │   │   │   └── StatisticsService.java
│       │   │   ├── user/
│       │   │   │   └── UserService.java
│       │   │   └── warehouse/
│       │   │       └── WarehouseReceiptService.java
│       │   │
│       │   ├── 📁 util/                           # 工具类（64KB）
│       │   │   ├── BlockchainUtil.java           # 区块链工具
│       │   │   ├── DateUtil.java                 # 日期工具
│       │   │   └── ValidationUtil.java           # 验证工具
│       │   │
│       │   └── 📁 vo/                             # 视图对象（8KB）
│       │       ├── ResponseVO.java               # 响应对象
│       │       └── PageResult.java               # 分页结果
│       │
│       └── resources/
│           ├── 📁 account/ecdsa/                # 账户文件
│           ├── 📁 conf/                         # 证书配置
│           ├── 📁 contracts/                    # Solidity智能合约（11个）
│           │   ├── Bill.sol
│           │   ├── BillOptimized.sol
│           │   ├── CreditLimit.sol
│           │   ├── DataPackUtils.sol
│           │   ├── EnterpriseRegistry.sol
│           │   ├── Receivable.sol
│           │   ├── ReceivableOptimized.sol
│           │   ├── ReceivableWithOverdue.sol
│           │   ├── WarehouseReceipt.sol
│           │   ├── WarehouseReceiptOptimized.sol
│           │   └── WarehouseReceiptWithFreeze.sol
│           ├── 📁 db/migration/                 # Flyway数据库迁移（28个文件）
│           │   ├── V1__init_schema.sql
│           │   ├── V2__*.sql
│           │   ├── ...
│           │   └── V20__create_risk_monitoring_tables.sql
│           ├── 📁 sql/                          # 额外SQL脚本
│           ├── 📁 static/                       # 静态Web资源
│           ├── 📄 application.properties        # 主配置文件
│           ├── 📄 config.toml                   # FISCO BCOS SDK配置
│           └── 📄 logback-spring.xml            # 日志配置
│
├── 📁 target/                          # Maven构建输出
│
├── 📁 tmp/                             # 临时文件
│
├── 📄 .env                             # 环境变量（Git忽略）
├── 📄 .env.example                     # 环境变量模板
├── 📄 .gitignore                       # Git忽略规则
├── 📄 pom.xml                          # Maven配置文件
└── 📄 application.log                  # 应用运行时日志
```
