# 🚀 API 文档快速使用指南

## 📍 文档位置

所有文档已导出到项目目录：
```
/home/llm_rca/fisco/my-bcos-app/api-docs/
```

---

## 📚 可用的文档格式

### 1. **在线查看（推荐）**

#### Swagger UI - 交互式文档
```
http://localhost:8080/swagger-ui/index.html
```
✅ 优点：界面美观，可以直接测试接口  
📝 适合：快速测试、接口调试

---

### 2. **离线文档**

#### 📄 Markdown 格式（推荐阅读）
```bash
cd /home/llm_rca/fisco/my-bcos-app/api-docs/
less API.md
# 或使用任何 Markdown 查看器
```
✅ 优点：易读，支持 GitHub/GitLab 渲染  
📝 适合：团队分享、版本控制

#### 📋 OpenAPI JSON 格式
```bash
cat openapi-formatted.json | less
```
✅ 优点：标准格式，工具支持广泛  
📝 适合：自动化工具、SDK 生成

#### 📝 OpenAPI YAML 格式
```bash
cat openapi.yaml | less
```
✅ 优点：人类可读，配置友好  
📝 适合：在线编辑、版本控制

---

## 🛠️ 实用工具

### 导入到 Postman（推荐测试）
1. 打开 Postman
2. 点击 `Import` → `Upload Files`
3. 选择 `postman-collection.json`
4. 所有接口自动导入，可直接测试

### 生成客户端代码
```bash
# 安装 swagger-codegen
docker pull swaggerapi/swagger-codegen-cli

# 生成 Python SDK
docker run --rm -v ${PWD}:/local swaggerapi/swagger-codegen-cli generate \
  -i /local/openapi.yaml \
  -g python \
  -o /local/output/python

# 生成 Java SDK
docker run --rm -v ${PWD}:/local swaggerapi/swagger-codegen-cli generate \
  -i /local/openapi.yaml \
  -g java \
  -o /local/output/java

# 生成 TypeScript SDK
docker run --rm -v ${PWD}:/local swaggerapi/swagger-codegen-cli generate \
  -i /local/openapi.yaml \
  -g typescript-fetch \
  -o /local/output/typescript
```

### 在线编辑和预览
1. 访问 https://editor.swagger.io/
2. 复制 `openapi.yaml` 的内容
3. 粘贴到编辑器
4. 实时预览和编辑

---

## 📋 快速参考：企业注册

### 使用 curl 测试
```bash
curl -X POST http://localhost:8080/api/enterprise/register \
  -H "Content-Type: application/json" \
  -d '{
    "address": "0x1234567890abcdef1234567890abcdef12345678",
    "name": "测试企业",
    "creditCode": "91110000MA001234XY",
    "role": "SUPPLIER",
    "initialPassword": "Test@123456"
  }'
```

### 使用 Swagger UI
1. 打开 http://localhost:8080/swagger-ui/index.html
2. 找到 "企业管理" → "注册企业"
3. 点击 "Try it out"
4. 填写参数
5. 点击 "Execute"

### 使用 Postman
1. 导入 `postman-collection.json`
2. 找到 "企业管理" → "注册企业"
3. 填写请求体
4. 点击 "Send"

---

## 🔐 认证测试

### 1. 企业登录获取 Token
```bash
curl -X POST http://localhost:8080/api/auth/login-enterprise \
  -H "Content-Type: application/json" \
  -d '{
    "address": "0x1234567890abcdef1234567890abcdef12345678",
    "password": "Test@123456"
  }'
```

### 2. 使用 Token 访问受保护接口
```bash
curl -X GET http://localhost:8080/api/enterprise/active \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

或在 Swagger UI 中：
1. 点击右上角 "Authorize" 按钮
2. 输入 `Bearer YOUR_TOKEN_HERE`
3. 点击 "Authorize"

---

## 📊 主要 API 模块

| 模块 | 路径 | 功能描述 |
|------|------|----------|
| 企业管理 | /api/enterprise | 注册、审核、状态管理 |
| 用户管理 | /api/users | 用户CRUD、审核 |
| 邀请码 | /api/invitation-codes | 生成、验证、管理 |
| 认证 | /api/auth | 登录、Token |
| 应收账款 | /api/receivables | 创建、转让 |
| 票据 | /api/bills | 开票、背书、贴现 |
| 仓单 | /api/warehouse-receipts | 创建、质押 |

---

## 💡 常见问题

### Q: 文档更新后如何重新导出？
A: 
```bash
cd /home/llm_rca/fisco/my-bcos-app
curl -s http://localhost:8080/v3/api-docs > api-docs/openapi.json
```

### Q: 如何生成 PDF 文档？
A:
```bash
# 安装 pandoc
sudo apt install pandoc

# 转换
pandoc api-docs/API.md -o api-docs/API.pdf
```

### Q: 如何生成 HTML 文档？
A:
```bash
# 安装 redoc
npm install -g @redocly/cli

# 生成
redocly build-docs api-docs/openapi.yaml -o api-docs/API.html
```

---

## 📞 获取帮助

- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **项目源码**: /home/llm_rca/fisco/my-bcos-app
- **API 文档**: /home/llm_rca/fisco/my-bcos-app/api-docs/

---

**最后更新**: 2026-01-18  
**API 版本**: 1.0.0  
**文档规范**: OpenAPI 3.0.3
