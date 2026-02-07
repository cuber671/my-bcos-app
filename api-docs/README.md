# 供应链金融系统 - API 文档导出

## 📦 文档列表

本目录包含了从 Swagger 自动导出的多种格式 API 文档。

---

### 1. **OpenAPI JSON 格式** 
**文件**: `openapi.json` 和 `openapi-formatted.json`

**用途**:
- 标准化的 OpenAPI 3.0 规范
- 可导入到各种 API 工具（Postman, Insomnia 等）
- 可用于生成客户端 SDK
- 版本控制和自动化测试

**使用方法**:
```bash
# 查看文档
cat openapi-formatted.json | less

# 导入到其他工具
# 例如：swagger-codegen 生成客户端代码
```

---

### 2. **OpenAPI YAML 格式**
**文件**: `openapi.yaml`

**用途**:
- 更易读的配置格式
- 适用于版本控制（Git）
- 可被多种工具直接使用

**使用方法**:
```bash
# 查看文档
cat openapi.yaml | less

# 在线编辑器使用
# 访问 https://editor.swagger.io/ 粘贴内容
```

---

### 3. **Markdown 文档**
**文件**: `API.md`

**用途**:
- 人类可读的文档格式
- 可直接在 GitHub/GitLab 上预览
- 适合团队内部文档分享
- 可转换为 PDF、HTML 等格式

**内容**:
- 完整的 API 接口列表
- 每个接口的详细说明
- 请求/响应示例
- 参数验证规则
- 错误码说明

**预览方法**:
```bash
# 使用 Markdown 查看器
less API.md

# 或在 GitHub/GitLab 上直接查看
```

---

### 4. **Postman Collection**
**文件**: `postman-collection.json`

**用途**:
- 导入到 Postman 进行 API 测试
- 包含所有接口的预配置请求
- 支持环境变量和自动化测试

**使用方法**:
1. 打开 Postman 应用
2. 点击左上角 "Import" 按钮
3. 选择 `postman-collection.json` 文件
4. 即可在 Postman 中测试所有接口

**快捷测试**:
```bash
# 使用 Postman CLI (newman)
npm install -g newman
newman run postman-collection.json
```

---

## 🚀 在线查看

### Swagger UI（推荐）
```
http://localhost:8080/swagger-ui/index.html
```

### Swagger Editor（在线编辑）
1. 访问: https://editor.swagger.io/
2. 复制 `openapi.yaml` 的内容
3. 粘贴到编辑器中
4. 实时预览和编辑

---

## 📚 文档格式对比

| 格式 | 大小 | 可读性 | 工具支持 | 推荐场景 |
|------|------|--------|----------|----------|
| JSON | 129KB | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 自动化、API 集成 |
| YAML | 169KB | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 人类阅读、版本控制 |
| Markdown | 40KB | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 文档分享、团队协作 |
| Postman | - | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | API 测试、调试 |

---

## 🛠️ 其他工具

### 生成客户端 SDK
```bash
# 使用 swagger-codegen
docker run --rm -v ${PWD}:/local swaggerapi/swagger-codegen-cli generate \
  -i /local/openapi.yaml \
  -g python \
  -o /local/output/python

# 支持的语言: java, python, php, javascript, typescript, go 等
```

### 生成 HTML 文档
```bash
# 使用 redoc
npm install -g @redocly/cli
redocly build-docs openapi.yaml -o api.html
```

### 生成静态 PDF
```bash
# 使用 pandoc
pandoc API.md -o api.pdf
```

---

## 📋 快速参考

### 企业注册接口示例

**端点**: `POST /api/enterprise/register`

**请求体**:
```json
{
  "address": "0x1234567890abcdef1234567890abcdef12345678",
  "name": "测试企业001",
  "creditCode": "91110000MA001234XY",
  "role": "SUPPLIER",
  "initialPassword": "Test@123456"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "企业注册成功，等待审核",
  "data": {
    "id": 1,
    "apiKey": "生成的API密钥",
    "status": "PENDING"
  }
}
```

---

## 📞 技术支持

如有问题，请参考：
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- 源代码: /home/llm_rca/fisco/my-bcos-app

---

**文档生成时间**: 2026-01-18  
**API 版本**: 1.0.0  
**规范**: OpenAPI 3.0.3
