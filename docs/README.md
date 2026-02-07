# FISCO BCOS 供应链金融项目文档

欢迎查阅项目文档中心！本文档目录包含了项目的所有技术文档、使用指南和开发说明。

---

## 📚 快速导航

### 🎯 入门文档

| 文档 | 说明 | 适合人群 |
|------|------|---------|
| [../README.md](../README.md) | 项目主README | 所有人 |
| [API_DOCS.md](API_DOCS.md) | RESTful API完整文档 | 开发者、API使用者 |
| [CONFIG_README.md](CONFIG_README.md) | SDK和Application配置 | 运维、部署人员 |
| [SWAGGER_GUIDE.md](SWAGGER_GUIDE.md) | Swagger API文档使用指南 | 开发者、测试人员 |

### 🏗️ 架构与设计

| 文档 | 说明 |
|------|------|
| [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) | 项目结构、代码组织说明 |
| [SERVICE_LAYER_GUIDE.md](SERVICE_LAYER_GUIDE.md) | Service层设计模式和最佳实践 |
| [SUPPLY_CHAIN_FINANCE_README.md](SUPPLY_CHAIN_FINANCE_README.md) | 供应链金融业务模型和数据流 |

### 🔐 安全与认证

| 文档 | 说明 | 重要度 |
|------|------|--------|
| [PERMISSION_AND_AUTH_SYSTEM.md](PERMISSION_AND_AUTH_SYSTEM.md) | **权限和角色体系完整说明** | ⭐⭐⭐⭐⭐ |
| [USER_AUTHENTICATION_SYSTEM.md](USER_AUTHENTICATION_SYSTEM.md) | 用户认证系统实现 | ⭐⭐⭐⭐⭐ |
| [JWT_AUTH_GUIDE.md](JWT_AUTH_GUIDE.md) | JWT令牌使用指南 | ⭐⭐⭐⭐ |
| [AUTHENTICATION_IMPROVEMENTS.md](AUTHENTICATION_IMPROVEMENTS.md) | 认证系统改进历史 | ⭐⭐⭐ |

### 📊 项目进展

| 文档 | 说明 |
|------|------|
| [PROJECT_COMPLETION_SUMMARY.md](PROJECT_COMPLETION_SUMMARY.md) | 已完成功能总结 |
| [NEXT_STEPS.md](NEXT_STEPS.md) | 待开发功能和TODO列表 |
| [HEALTH_CHECK_REPORT.md](HEALTH_CHECK_REPORT.md) | 系统健康检查报告 |
| [IDE_FIX_GUIDE.md](IDE_FIX_GUIDE.md) | VS Code开发环境问题解决 |

---

## 🗂️ 文档分类说明

### 1. 核心业务文档

#### [SUPPLY_CHAIN_FINANCE_README.md](SUPPLY_CHAIN_FINANCE_README.md)
供应链金融系统的业务模型，包括：
- 应收账款管理
- 仓单质押融资
- 企业信用评级
- 业务流程图

#### [PERMISSION_AND_AUTH_SYSTEM.md](PERMISSION_AND_AUTH_SYSTEM.md)
**强烈推荐阅读** - 权限和登录体系完整说明：
- 企业角色（供应商、核心企业、金融机构、监管机构）
- 用户角色（管理员、企业用户、审计员、操作员）
- 三种登录方式详解
- 权限控制架构
- 使用场景示例

### 2. 技术实现文档

#### [USER_AUTHENTICATION_SYSTEM.md](USER_AUTHENTICATION_SYSTEM.md)
用户认证系统完整实现：
- User实体和Repository
- UserService和UserController
- 用户CRUD操作
- 密码管理（修改、重置）
- 用户状态管理

#### [JWT_AUTH_GUIDE.md](JWT_AUTH_GUIDE.md)
JWT认证机制：
- JWT配置和生成
- 令牌验证流程
- 环境变量设置
- API请求示例
- 错误处理

#### [AUTHENTICATION_IMPROVEMENTS.md](AUTHENTICATION_IMPROVEMENTS.md)
安全改进历史：
- 原有安全问题分析
- 密码验证实现
- API密钥机制
- 迁移指南

### 3. 开发指南

#### [API_DOCS.md](API_DOCS.md)
完整的API端点文档：
- 认证接口
- 企业管理接口
- 用户管理接口
- 应收账款接口
- 仓单管理接口

#### [SWAGGER_GUIDE.md](SWAGGER_GUIDE.md)
Swagger UI使用：
- 访问地址
- 接口测试
- 认证设置
- 示例截图

#### [SERVICE_LAYER_GUIDE.md](SERVICE_LAYER_GUIDE.md)
Service层开发规范：
- 事务管理
- 异常处理
- 区块链集成
- 最佳实践

### 4. 配置和部署

#### [CONFIG_README.md](CONFIG_README.md)
详细的配置说明：
- FISCO BCOS SDK配置
- 数据库配置
- Spring Security配置
- 环境变量设置

#### [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)
项目结构说明：
- 代码组织
- 包结构
- 文件职责

---

## 🚀 推荐阅读路径

### 路径1：新手入门
```
1. ../README.md                    # 了解项目概况
2. SUPPLY_CHAIN_FINANCE_README.md  # 理解业务场景
3. API_DOCS.md                     # 了解可用接口
4. SWAGGER_GUIDE.md                # 学习测试API
```

### 路径2：开发者
```
1. PROJECT_STRUCTURE.md            # 理解项目架构
2. SERVICE_LAYER_GUIDE.md          # 学习Service层
3. PERMISSION_AND_AUTH_SYSTEM.md   # 理解权限体系
4. JWT_AUTH_GUIDE.md               # 学习认证机制
5. API_DOCS.md                     # 参考API文档
```

### 路径3：安全审计
```
1. AUTHENTICATION_IMPROVEMENTS.md  # 安全改进历史
2. JWT_AUTH_GUIDE.md               # JWT实现
3. USER_AUTHENTICATION_SYSTEM.md   # 用户认证
4. PERMISSION_AND_AUTH_SYSTEM.md   # 权限体系
```

### 路径4：部署运维
```
1. CONFIG_README.md                # 配置说明
2. HEALTH_CHECK_REPORT.md          # 健康检查
3. API_DOCS.md                     # API文档
4. NEXT_STEPS.md                   # 已知问题和TODO
```

---

## 📝 文档更新记录

| 日期 | 文档 | 更新内容 |
|------|------|---------|
| 2026-01-16 | PERMISSION_AND_AUTH_SYSTEM.md | 新增权限和登录体系完整说明 |
| 2026-01-16 | USER_AUTHENTICATION_SYSTEM.md | 新增用户认证系统文档 |
| 2026-01-16 | JWT_AUTH_GUIDE.md | 新增JWT使用指南 |
| 2026-01-16 | AUTHENTICATION_IMPROVEMENTS.md | 新增认证改进说明 |
| 2026-01-16 | 所有文档 | 统一移动到docs/目录 |

---

## 🔍 快速查找

### 按关键词查找

**业务相关：**
- 应收账款 → [SUPPLY_CHAIN_FINANCE_README.md](SUPPLY_CHAIN_FINANCE_README.md)
- 仓单质押 → [SUPPLY_CHAIN_FINANCE_README.md](SUPPLY_CHAIN_FINANCE_README.md)
- 企业角色 → [PERMISSION_AND_AUTH_SYSTEM.md](PERMISSION_AND_AUTH_SYSTEM.md)

**认证相关：**
- 登录 → [PERMISSION_AND_AUTH_SYSTEM.md](PERMISSION_AND_AUTH_SYSTEM.md)
- JWT → [JWT_AUTH_GUIDE.md](JWT_AUTH_GUIDE.md)
- 权限 → [PERMISSION_AND_AUTH_SYSTEM.md](PERMISSION_AND_AUTH_SYSTEM.md)
- 用户管理 → [USER_AUTHENTICATION_SYSTEM.md](USER_AUTHENTICATION_SYSTEM.md)

**开发相关：**
- API → [API_DOCS.md](API_DOCS.md)
- Swagger → [SWAGGER_GUIDE.md](SWAGGER_GUIDE.md)
- 配置 → [CONFIG_README.md](CONFIG_README.md)
- 项目结构 → [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)

**问题解决：**
- VS Code错误 → [IDE_FIX_GUIDE.md](IDE_FIX_GUIDE.md)
- 编译问题 → [IDE_FIX_GUIDE.md](IDE_FIX_GUIDE.md)
- 健康检查 → [HEALTH_CHECK_REPORT.md](HEALTH_CHECK_REPORT.md)

---

## 💡 提示

1. **文档顺序**：推荐按照"推荐阅读路径"顺序阅读文档
2. **重点标记**：带有 ⭐ 标记的文档为重点文档
3. **实时更新**：文档会随着项目开发持续更新
4. **反馈建议**：如有疑问或建议，请查看项目Issue

---

## 📧 联系方式

如有问题，请参考：
- 项目主README: [../README.md](../README.md)
- 问题排查: [IDE_FIX_GUIDE.md](IDE_FIX_GUIDE.md)
- 已知问题: [NEXT_STEPS.md](NEXT_STEPS.md)

---

**最后更新时间**: 2026-01-16
**文档版本**: v1.0
