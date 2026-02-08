# 📦 供应链金融平台 - 完整项目结构树状图

> 基于FISCO BCOS区块链的供应链金融平台
> 生成时间: 2026-02-08

---

## 📂 项目根目录

```
my-bcos-app/
├── 📂 src/                          # 源代码目录
├── 📂 api-docs/                     # API 文档 (11个文件)
├── 📂 docs/                         # 项目文档 (40个文档)
├── 📂 logs/                         # 日志文件
├── 📂 scripts/                      # 脚本目录
├── 📂 sql/                          # SQL 脚本
├── 📂 conf/                         # 配置文件
├── 📂 account/                      # 账户文件
├── 📂 accounts/                     # 账户公钥
├── 📂 database/                     # 数据库迁移
├── 📂 tmp/                          # 临时文件
├── 📄 pom.xml                       # Maven 配置
├── 📄 scripts/start.sh              # 启动脚本
├── 📄 scripts/deploy-contract.sh    # 合约部署脚本
├── 📄 scripts/check_chain.sh        # 链检查脚本
├── 📄 scripts/diagnose_db.sh        # 数据库诊断
└── 📄 各种报告文档 .md              # 项目报告
```

---

## 📦 src/main/java/com/fisco/app/

### 📄 根目录文件

```
src/main/java/com/fisco/app/
├── BcosApplication.java             # Spring Boot 应用启动类
└── (其他目录见下文)
```

---

## 🎯 annotation/ - 注解

```
annotation/
└── Audited.java                     # 审计注解
```

---

## 🔧 aspect/ - AOP 切面

```
aspect/
├── AuditLogAspect.java              # 审计日志切面
├── OnChainStatusAspect.java         # 链上状态切面
├── PermissionAspect.java            # 权限检查切面
├── RequestLoggingAspect.java        # 请求日志切面
└── RequireOnChain.java              # 链上要求注解
```

---

## ⚙️ config/ - 配置类

```
config/
├── AsyncConfig.java                 # 异步配置
├── BcosConfig.java                  # 区块链配置
├── CacheConfig.java                 # 缓存配置
├── GlobalExceptionHandler.java      # 全局异常处理
├── LoggingConfig.java               # 日志配置
├── SecurityConfig.java              # 安全配置
├── SwaggerConfig.java               # Swagger API 文档配置
└── WebConfig.java                   # Web 配置
```

---

## 📜 contract/ - 智能合约

```
contract/
├── BillContract.java                # 票据智能合约
├── EnterpriseRegistry.java          # 企业注册合约
├── HelloWorld.java                  # 示例合约
├── ReceivableContract.java          # 应收账款合约
├── ReceivableWithOverdue.java       # 逾期应收合约
├── WarehouseReceiptContract.java    # 仓单智能合约
├── EnterpriseRegistry.java.backup   # 备份文件
└── EnterpriseRegistry.java.backup2  # 备份文件2
```

---

## 🎮 controller/ - 控制器层 (11个模块)

```
controller/
├── BlockchainManagementController.java  # 区块链管理控制器
├── InvitationCodeController.java        # 邀请码控制器
│
├── 👤 admin/                           # 管理员模块
│   ├── AdminController.java            # 管理员控制器
│   └── AdminEnterpriseController.java  # 管理员-企业管理
│
├── 💰 bill/                            # 票据模块
│   ├── BillController.java             # 票据控制器
│   └── BillPoolController.java         # 票据池控制器
│
├── 🔗 blockchain/                      # 区块链管理
│   ├── BcosController.java             # BCOS控制器
│   ├── ContractStatusController.java   # 合约状态控制器
│   └── DeployController.java           # 部署控制器
│
├── 💳 credit/                          # 授信额度模块
│   └── CreditLimitController.java      # 授信额度控制器
│
├── ✍️ endorsement/                     # 背书流转
│   └── EwrEndorsementChainController.java  # 仓单背书控制器
│
├── 🏢 enterprise/                      # 企业模块
│   ├── EnterpriseController.java       # 企业控制器
│   └── EnterpriseUserRelationController.java  # 企业用户关系
│
├── 🔔 notification/                    # 通知模块
│   └── NotificationController.java     # 通知控制器
│
├── 📋 pledge/                          # 质押模块
│   └── PledgeManagementController.java # 质押管理控制器
│
├── 📊 receivable/                      # 应收账款
│   └── ReceivableController.java       # 应收账款控制器
│
├── ⚠️ risk/                            # 风险管理
│   └── RiskController.java             # 风险控制器
│
├── 🛠️ system/                          # 系统管理
│   ├── AuditLogController.java         # 审计日志控制器
│   ├── DataMigrationController.java    # 数据迁移控制器
│   └── StatisticsController.java       # 统计控制器
│
├── 👥 user/                            # 用户模块
│   ├── AuthController.java             # 认证控制器
│   └── UserController.java            # 用户控制器
│
└── 📦 warehouse/                       # 仓单模块
    ├── ElectronicWarehouseReceiptController.java  # 电子仓单控制器
    └── WarehouseReceiptController.java            # 仓单控制器
```

**控制器统计**: 21个控制器类

---

## 📦 dto/ - 数据传输对象 (11个模块)

```
dto/
├── 📋 audit/                          # 审计相关 DTO (3个)
│   ├── AuditBatchResult.java
│   ├── AuditLogQueryRequest.java
│   └── AuditLogStatistics.java
│
├── 💰 bill/                           # 票据相关 DTO (15个)
│   ├── ApproveFinanceRequest.java
│   ├── BillInvestRequest.java
│   ├── BillInvestResponse.java
│   ├── BillPoolFilter.java
│   ├── BillPoolView.java
│   ├── CancelBillRequest.java
│   ├── DiscountBillRequest.java
│   ├── DiscountBillResponse.java
│   ├── FinanceApplicationResponse.java
│   ├── FinanceBillRequest.java
│   ├── FreezeBillRequest.java
│   ├── IssueBillRequest.java
│   ├── RepayBillRequest.java
│   ├── RepayBillResponse.java
│   ├── RepayFinanceRequest.java
│   └── UnfreezeBillRequest.java
│
├── 💳 credit/                         # 授信相关 DTO (15个)
│   ├── CreditLimitAdjustApprovalRequest.java
│   ├── CreditLimitAdjustRequestDTO.java
│   ├── CreditLimitAdjustResponse.java
│   ├── CreditLimitCreateRequest.java
│   ├── CreditLimitDTO.java
│   ├── CreditLimitFreezeRequest.java
│   ├── CreditLimitFreezeResponse.java
│   ├── CreditLimitQueryRequest.java
│   ├── CreditLimitQueryResponse.java
│   ├── CreditLimitUsageDTO.java
│   ├── CreditLimitUsageQueryRequest.java
│   ├── CreditLimitUsageQueryResponse.java
│   ├── CreditLimitWarningDTO.java
│   ├── CreditLimitWarningQueryRequest.java
│   └── CreditLimitWarningQueryResponse.java
│
├── ✍️ endorsement/                    # 背书相关 DTO (5个)
│   ├── EndorseBillRequest.java
│   ├── EndorsementResponse.java
│   ├── EwrEndorsementChainResponse.java
│   ├── EwrEndorsementConfirmRequest.java
│   └── EwrEndorsementCreateRequest.java
│
├── 🏢 enterprise/                     # 企业相关 DTO (5个)
│   ├── EnterpriseDeletionRequest.java
│   ├── EnterpriseRegistrationRequest.java
│   ├── EnterpriseRegistrationResponse.java
│   ├── EnterpriseWithUsersDTO.java
│   └── UserWithEnterpriseDTO.java
│
├── 🔔 notification/                   # 通知相关 DTO (6个)
│   ├── NotificationBatchMarkRequest.java
│   ├── NotificationCreateRequest.java
│   ├── NotificationDTO.java
│   ├── NotificationQueryRequest.java
│   ├── NotificationStatisticsDTO.java
│   └── NotificationSubscriptionRequest.java
│
├── 📋 pledge/                         # 质押相关 DTO (10个)
│   ├── PledgeApplicationCreateRequest.java
│   ├── PledgeApplicationQueryRequest.java
│   ├── PledgeApplicationResponse.java
│   ├── PledgeApprovalRequest.java
│   ├── PledgeApprovalResponse.java
│   ├── PledgeConfirmRequest.java
│   ├── PledgeConfirmResponse.java
│   ├── PledgeInitiateRequest.java
│   ├── PledgeInitiateResponse.java
│   ├── PledgeRecordResponse.java
│   └── PledgeReleaseRequest.java
│
├── 📊 receivable/                     # 应收相关 DTO (17个)
│   ├── BadDebtQueryRequest.java
│   ├── BadDebtQueryResponse.java
│   ├── CreateReceivableRequest.java
│   ├── MergeApprovalRequest.java
│   ├── OverdueQueryRequest.java
│   ├── OverdueQueryResponse.java
│   ├── OverdueReceivableDTO.java
│   ├── PenaltyCalculateRequest.java
│   ├── PenaltyCalculateResponse.java
│   ├── ReceivableMergeRequest.java
│   ├── ReceivableMergeResponse.java
│   ├── ReceivableSplitRequest.java
│   ├── ReceivableSplitResponse.java
│   ├── RemindRequest.java
│   ├── RemindResponse.java
│   ├── SplitApplicationRequest.java
│   ├── SplitApplicationResponse.java
│   ├── SplitApprovalRequest.java
│   ├── SplitApprovalResponse.java
│   └── SplitDetailRequest.java
│
├── ⚠️ risk/                           # 风险相关 DTO (3个)
│   ├── RiskAssessmentRequest.java
│   ├── RiskAssessmentResponse.java
│   └── RiskStatisticsDTO.java
│
├── 📈 statistics/                     # 统计相关 DTO (4个)
│   ├── BusinessStatisticsDTO.java
│   ├── ComprehensiveReportDTO.java
│   ├── FinancingStatisticsDTO.java
│   └── StatisticsQueryRequest.java
│
├── 👤 user/                           # 用户相关 DTO (1个)
│   └── UserRegistrationRequest.java
│
└── 📦 warehouse/                      # 仓单相关 DTO (20个)
    ├── CancelApplicationRequest.java
    ├── CancelApplicationResponse.java
    ├── CancelApprovalRequest.java
    ├── CancelApprovalResponse.java
    ├── CreateWarehouseReceiptRequest.java
    ├── DeliveryUpdateRequest.java
    ├── ElectronicWarehouseReceiptCreateRequest.java
    ├── ElectronicWarehouseReceiptQueryRequest.java
    ├── ElectronicWarehouseReceiptResponse.java
    ├── ElectronicWarehouseReceiptUpdateRequest.java
    ├── FreezeApplicationResponse.java
    ├── FreezeApplicationReviewRequest.java
    ├── FreezeApplicationReviewResponse.java
    ├── FreezeApplicationSubmitRequest.java
    ├── GoodsInfo.java
    ├── ReceiptApprovalRequest.java
    ├── ReceiptApprovalResponse.java
    ├── ReceiptFreezeRequest.java
    ├── ReceiptFreezeResponse.java
    ├── ReceiptUnfreezeRequest.java
    ├── ReceiptUnfreezeResponse.java
    ├── ReleaseReceiptRequest.java
    └── ReleaseReceiptResponse.java
```

**DTO统计**: 11个模块，共104个DTO类

---

## 🗄️ entity/ - 实体类 (9个模块)

```
entity/
├── 💰 bill/                           # 票据实体 (10个)
│   ├── Bill.java
│   ├── BillDiscount.java
│   ├── BillEndorsement.java
│   ├── BillFinanceApplication.java
│   ├── BillInvestment.java
│   ├── BillPledgeApplication.java
│   ├── BillRecourse.java
│   ├── BillSettlement.java
│   ├── DiscountRecord.java
│   ├── Endorsement.java
│   └── RepaymentRecord.java
│
├── 💳 credit/                         # 授信实体 (4个)
│   ├── CreditLimit.java
│   ├── CreditLimitAdjustRequest.java
│   ├── CreditLimitUsage.java
│   └── CreditLimitWarning.java
│
├── 🏢 enterprise/                     # 企业实体 (2个)
│   ├── Enterprise.java
│   └── EnterpriseAuditLog.java
│
├── 🔔 notification/                   # 通知实体 (4个)
│   ├── Notification.java
│   ├── NotificationSendLog.java
│   ├── NotificationSubscription.java
│   └── NotificationTemplate.java
│
├── 📋 pledge/                         # 质押实体 (3个)
│   ├── PledgeApplication.java
│   ├── PledgeRecord.java
│   └── ReleaseRecord.java
│
├── 📊 receivable/                     # 应收实体 (1个)
│   └── Receivable.java
│
├── ⚠️ risk/                           # 风险实体 (5个)
│   ├── BadDebtRecord.java
│   ├── FinancingRecord.java
│   ├── OverduePenaltyRecord.java
│   ├── OverdueRemindRecord.java
│   └── RiskAssessment.java
│
├── 🛠️ system/                         # 系统实体 (2个)
│   ├── AuditLog.java
│   └── PermissionAuditLog.java
│
├── 👥 user/                           # 用户实体 (3个)
│   ├── Admin.java
│   ├── InvitationCode.java
│   └── User.java
│
└── 📦 warehouse/                      # 仓单实体 (6个)
    ├── ElectronicWarehouseReceipt.java
    ├── EwrEndorsementChain.java
    ├── ReceiptCancelApplication.java
    ├── ReceiptFreezeApplication.java
    ├── ReceiptSplitApplication.java
    └── WarehouseReceipt.java
```

**实体统计**: 9个模块，共40个实体类

---

## 📚 enums/ - 枚举类

```
enums/
├── CreditAdjustRequestStatus.java     # 授信调整请求状态
├── CreditAdjustType.java              # 授信调整类型
├── CreditLimitStatus.java             # 授信额度状态
├── CreditLimitType.java               # 授信额度类型
├── CreditUsageType.java               # 授信使用类型
└── CreditWarningLevel.java            # 授信警告级别
```

**枚举统计**: 6个枚举类

---

## 📡 event/ - 事件处理

```
event/
├── EnterpriseUpdatedEvent.java        # 企业更新事件
├── EventPublisher.java                # 事件发布器
├── PermissionUpdateListener.java      # 权限更新监听器
├── UserDeletedEvent.java              # 用户删除事件
└── UserUpdatedEvent.java              # 用户更新事件
```

**事件统计**: 5个事件类

---

## ⚠️ exception/ - 异常处理

```
exception/
├── BlockchainIntegrationException.java  # 区块链集成异常
└── BusinessException.java              # 业务异常
```

**异常统计**: 2个异常类

---

## 🗃️ repository/ - 数据访问层 (10个模块)

```
repository/
├── EnterpriseFilteredRepository.java  # 企业过滤Repository
│
├── 💰 bill/                           # 票据Repository (12个)
│   ├── BillDiscountRepository.java
│   ├── BillEndorsementRepository.java
│   ├── BillFinanceApplicationRepository.java
│   ├── BillInvestmentRepository.java
│   ├── BillPledgeApplicationRepository.java
│   ├── BillRecourseRepository.java
│   ├── BillRepository.java
│   ├── BillSettlementRepository.java
│   ├── DiscountRecordRepository.java
│   ├── EndorsementRepository.java
│   └── RepaymentRecordRepository.java
│
├── 💳 credit/                         # 授信Repository (4个)
│   ├── CreditLimitAdjustRequestRepository.java
│   ├── CreditLimitRepository.java
│   ├── CreditLimitUsageRepository.java
│   └── CreditLimitWarningRepository.java
│
├── 🏢 enterprise/                     # 企业Repository (2个)
│   ├── EnterpriseAuditLogRepository.java
│   └── EnterpriseRepository.java
│
├── 🔔 notification/                   # 通知Repository (4个)
│   ├── NotificationRepository.java
│   ├── NotificationSendLogRepository.java
│   ├── NotificationSubscriptionRepository.java
│   └── NotificationTemplateRepository.java
│
├── 📋 pledge/                         # 质押Repository (3个)
│   ├── PledgeApplicationRepository.java
│   ├── PledgeRecordRepository.java
│   └── ReleaseRecordRepository.java
│
├── 📊 receivable/                     # 应收Repository (1个)
│   └── ReceivableRepository.java
│
├── ⚠️ risk/                           # 风险Repository (4个)
│   ├── BadDebtRecordRepository.java
│   ├── FinancingRecordRepository.java
│   ├── OverduePenaltyRecordRepository.java
│   └── OverdueRemindRecordRepository.java
│
├── 🛠️ system/                         # 系统Repository (2个)
│   ├── AuditLogRepository.java
│   └── PermissionAuditLogRepository.java
│
├── 👥 user/                           # 用户Repository (3个)
│   ├── AdminRepository.java
│   ├── InvitationCodeRepository.java
│   └── UserRepository.java
│
└── 📦 warehouse/                      # 仓单Repository (6个)
    ├── ElectronicWarehouseReceiptRepository.java
    ├── EwrEndorsementChainRepository.java
    ├── ReceiptCancelApplicationRepository.java
    ├── ReceiptFreezeApplicationRepository.java
    ├── ReceiptSplitApplicationRepository.java
    └── WarehouseReceiptRepository.java
```

**Repository统计**: 10个模块，共41个Repository接口

---

## 🔐 security/ - 安全模块

```
security/
├── annotations/                       # 安全注解
│   ├── RequireEnterpriseAccess.java    # 企业访问权限注解
│   ├── RequireEnterpriseAdmin.java     # 企业管理员注解
│   └── RequireRole.java                # 角色要求注解
│
├── AdminAuthInterceptor.java          # 管理员认证拦截器
├── EnterpriseAuthInterceptor.java     # 企业认证拦截器
├── JwtAuthenticationEntryPoint.java   # JWT认证入口点
├── JwtAuthenticationFilter.java       # JWT认证过滤器
├── JwtTokenProvider.java              # JWT令牌提供者
├── PasswordUtil.java                  # 密码工具
├── PermissionCacheService.java        # 权限缓存服务
├── PermissionChecker.java             # 权限检查器
├── RequireAdmin.java                  # 需要管理员注解
├── RequireEnterprise.java             # 需要企业注解
└── UserAuthentication.java            # 用户认证
```

**安全统计**: 12个安全相关类

---

## 💼 service/ - 业务逻辑层 (10个模块)

```
service/
├── InvitationCodeService.java          # 邀请码服务
├── ContractService.java.backup         # 合约服务备份
├── EnterpriseService.java.backup       # 企业服务备份
│
├── 💰 bill/                            # 票据服务 (2个)
│   ├── BillPoolService.java            # 票据池服务
│   └── BillService.java                # 票据服务
│
├── 🔗 blockchain/                      # 区块链服务 (1个)
│   └── ContractService.java            # 合约服务
│
├── 💳 credit/                          # 授信服务 (1个)
│   └── CreditLimitService.java         # 授信额度服务
│
├── 🏢 enterprise/                      # 企业服务 (1个)
│   └── EnterpriseService.java          # 企业服务
│
├── 🔔 notification/                    # 通知服务 (1个)
│   └── NotificationService.java        # 通知服务
│
├── 📋 pledge/                          # 质押服务 (1个)
│   └── PledgeService.java              # 质押服务
│
├── 📊 receivable/                      # 应收服务 (2个)
│   ├── ReceivableOverdueService.java   # 应收逾期服务
│   └── ReceivableService.java          # 应收服务
│
├── ⚠️ risk/                            # 风险服务 (1个)
│   └── RiskService.java                # 风险服务
│
├── 🛠️ system/                          # 系统服务 (4个)
│   ├── AuditLogService.java            # 审计日志服务
│   ├── DataMigrationService.java       # 数据迁移服务
│   ├── PermissionAuditService.java     # 权限审计服务
│   └── StatisticsService.java          # 统计服务
│
├── 👥 user/                            # 用户服务 (2个)
│   ├── AdminService.java               # 管理员服务
│   └── UserService.java                # 用户服务
│
├── 📦 warehouse/                       # 仓单服务 (3个)
│   ├── ElectronicWarehouseReceiptService.java    # 电子仓单服务
│   ├── EwrEndorsementChainService.java           # 仓单背书服务
│   └── WarehouseReceiptService.java              # 仓单服务
│
└── impl/                              # 服务实现类目录
```

**Service统计**: 10个模块，共18个服务接口/类

---

## 🛠️ util/ - 工具类

```
util/
├── AddressGenerator.java              # 地址生成器
├── ContractDeployer.java              # 合约部署器
├── CreditCodeValidator.java           # 统一社会信用代码验证器
├── DataHashUtil.java                  # 数据哈希工具
├── LoggingHelper.java                 # 日志助手
└── PasswordGenerator.java             # 密码生成器
```

**工具统计**: 6个工具类

---

## 📤 vo/ - 视图对象

```
vo/
└── Result.java                        # 统一返回结果
```

**VO统计**: 1个VO类

---

## 📊 src/main/resources/ - 资源文件

```
src/main/resources/
├── application.properties             # 应用配置文件
├── application.properties.backup.*    # 配置备份文件
├── config.toml                        # 区块链配置
├── config.toml.backup                 # 配置备份
├── logback-spring.xml                 # 日志配置
│
├── account/                           # 账户文件
│   └── ecdsa/
│       └── 0xca419c1b5aebd3f09fcdf1a059f8bbebec1a52c9.pem
│
├── conf/                              # 证书配置
│   ├── ca.crt
│   ├── cert.cnf
│   ├── sdk.crt
│   └── sdk.key
│
├── contracts/                         # 智能合约Solidity文件 (12个)
│   ├── Bill.sol
│   ├── BillOptimized.sol
│   ├── CreditLimit.sol
│   ├── DataPackUtils.sol
│   ├── EnterpriseRegistry.sol
│   ├── OPTIMIZATION_GUIDE.md
│   ├── QUICK_REFERENCE.md
│   ├── Receivable.sol
│   ├── ReceivableOptimized.sol
│   ├── ReceivableWithOverdue.sol
│   ├── WarehouseReceipt.sol
│   ├── WarehouseReceiptOptimized.sol
│   └── WarehouseReceiptWithFreeze.sol
│
├── db/                                # 数据库迁移脚本
│   └── migration/
│       ├── V1__init_schema.sql
│       ├── V2__*_*.sql (多个)
│       ├── V3__*_*.sql
│       ├── V4__*_*.sql
│       ├── V5__*_*.sql
│       ├── V8__*_*.sql
│       ├── V9__*_*.sql
│       ├── V10__*_*.sql
│       ├── V11__*_*.sql (多个)
│       ├── V12__*_*.sql (多个)
│       ├── V13__*_*.sql (多个)
│       ├── V14__*_*.sql
│       ├── V15__*_*.sql
│       ├── V16__*_*.sql
│       ├── V17__*_*.sql
│       ├── V18__*_*.sql
│       ├── V19__*_*.sql
│       └── V20__*_*.sql
│
├── sql/                               # SQL脚本
│   └── admin_schema.sql
│
└── static/                            # 静态资源
    └── index.html
```

---

## 📈 项目统计

### Java 源代码统计

| 层级 | 模块数 | 文件数 |
|------|--------|--------|
| **Controller** | 11 | 21 |
| **DTO** | 11 | 104 |
| **Entity** | 9 | 40 |
| **Repository** | 10 | 41 |
| **Service** | 10 | 18 |
| **Contract** | - | 6 |
| **Security** | - | 12 |
| **Aspect** | - | 5 |
| **Config** | - | 8 |
| **Enum** | - | 6 |
| **Event** | - | 5 |
| **Exception** | - | 2 |
| **Util** | - | 6 |
| **VO** | - | 1 |
| **Annotation** | - | 1 |
| **总计** | **71** | **297** |

### 业务模块

1. **💰 票据模块** - 票据生命周期、融资、投资、质押
2. **💳 授信模块** - 企业授信额度管理
3. **📊 应收账款** - 应收账款登记、拆分、合并、逾期管理
4. **📦 仓单模块** - 电子仓单、背书流转、冻结、解冻
5. **🏢 企业模块** - 企业注册、用户管理、权限管理
6. **⚠️ 风险模块** - 风险评估、坏账管理、逾期提醒
7. **🔔 通知模块** - 系统通知、消息推送
8. **📋 质押模块** - 质押申请、审批、释放
9. **🔗 区块链** - 智能合约部署和管理
10. **🛠️ 系统管理** - 审计日志、数据统计、数据迁移

### 技术栈

- **框架**: Spring Boot 2.x
- **区块链**: FISCO BCOS 3.x
- **数据库**: MySQL (使用Flyway迁移)
- **安全**: JWT + Spring Security
- **API文档**: Swagger/OpenAPI
- **日志**: Logback
- **构建工具**: Maven

---

## 📝 项目架构特点

### 分层架构
```
┌─────────────────────────────────────┐
│   Controller 层 (控制器)              │
├─────────────────────────────────────┤
│   Service 层 (业务逻辑)               │
├─────────────────────────────────────┤
│   Repository 层 (数据访问)            │
├─────────────────────────────────────┤
│   Entity 层 (数据实体)                │
└─────────────────────────────────────┘
```

### 模块化设计
- 按业务功能模块组织代码
- 每个模块包含独立的 Controller、Service、Repository、Entity、DTO
- 便于维护和扩展

### 安全机制
- JWT Token 认证
- 基于角色的访问控制 (RBAC)
- 企业级权限管理
- 区块链地址认证

### 区块链集成
- 智能合约管理
- 链上数据存证
- 链上状态同步
- 事件监听机制

---

**生成时间**: 2026-02-08
**项目名称**: 供应链金融平台 (Supply Chain Finance Platform)
**基于**: FISCO BCOS 区块链
