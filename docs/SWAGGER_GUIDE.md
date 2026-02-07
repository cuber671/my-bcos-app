# Swagger API 文档使用指南

## 概述

项目已集成Swagger 3.0，自动生成交互式API文档，方便开发和测试。

## 访问Swagger UI

启动应用后，通过以下地址访问：

- **Swagger UI**: http://localhost:8080/swagger-ui/
- **API文档(JSON格式)**: http://localhost:8080/v3/api-docs
- **API文档(YAML格式)**: http://localhost:8080/v3/api-docs.yaml

## 功能特性

### 1. 交互式API测试

Swagger UI提供交互式界面，可以直接在浏览器中测试API：

1. 打开 http://localhost:8080/swagger-ui/
2. 选择要测试的API接口
3. 点击"Try it out"按钮
4. 填写请求参数
5. 点击"Execute"执行请求
6. 查看响应结果

### 2. API文档查看

每个接口都包含：
- 接口描述
- 请求方法（GET/POST/PUT/DELETE）
- 请求参数说明
- 请求体示例（Schema）
- 响应格式说明
- 状态码说明

### 3. 模型查看

可以查看所有数据模型的详细信息：
- 字段名称
- 数据类型
- 是否必填
- 示例值
- 字段描述

## API分组

文档按业务模块分组：

### 企业管理 (Enterprise)
- 企业注册
- 企业审核
- 状态更新
- 信用评级管理
- 授信额度设置
- 企业信息查询

### 应收账款管理 (Receivable)
- 应收账款创建
- 应收账款确认
- 应收账款融资
- 应收账款还款
- 应收账款转让
- 应收账款查询

## 使用示例

### 示例1: 注册企业

1. 在Swagger UI中找到"企业管理"分组
2. 展开"注册企业"接口
3. 点击"Try it out"
4. 在Request body中输入以下JSON：

```json
{
  "address": "0x1234567890abcdef1234567890abcdef12345678",
  "name": "供应商A",
  "creditCode": "91110000MA001234XY",
  "enterpriseAddress": "北京市朝阳区",
  "role": "SUPPLIER",
  "creditRating": 75,
  "creditLimit": 1000000.00
}
```

5. 点击"Execute"执行请求
6. 查看响应结果

### 示例2: 创建应收账款

1. 在"应收账款管理"分组中找到"创建应收账款"
2. 点击"Try it out"
3. 输入以下JSON：

```json
{
  "receivableId": "REC20240113001",
  "supplierAddress": "0x1234567890abcdef",
  "coreEnterpriseAddress": "0xabcdef1234567890",
  "amount": 500000.00,
  "currency": "CNY",
  "issueDate": "2024-01-13T10:00:00",
  "dueDate": "2024-04-13T10:00:00",
  "description": "原材料采购款"
}
```

4. 执行并查看结果

### 示例3: 应收账款融资

1. 先创建并确认应收账款
2. 找到"应收账款融资"接口
3. 输入以下JSON：

```json
{
  "financierAddress": "0x567890abcdef1234",
  "financeAmount": 450000.00,
  "financeRate": 500
}
```

4. 执行融资操作

## 认证配置

如果API需要认证，Swagger支持配置JWT Token：

1. 点击页面右上角的"Authorize"按钮
2. 在弹出的对话框中输入JWT Token（格式：`Bearer <token>`）
3. 点击"Authorize"确认
4. 关闭对话框
5. 所有请求将自动携带认证信息

## 注解说明

### Controller注解

- `@Api`: 标记Controller类，描述API分组
  ```java
  @Api(tags = "企业管理", description = "企业注册、审核、信用评级等管理接口")
  ```

- `@ApiOperation`: 描述API接口
  ```java
  @ApiOperation(value = "注册企业", notes = "注册新的企业，需要提供企业基本信息")
  ```

- `@ApiParam`: 描述参数
  ```java
  @ApiParam(value = "企业区块链地址", required = true, example = "0x1234")
  ```

### Entity注解

- `@ApiModel`: 描述实体类
  ```java
  @ApiModel(value = "Enterprise", description = "企业信息实体")
  ```

- `@ApiModelProperty`: �描述字段
  ```java
  @ApiModelProperty(value = "企业名称", required = true, example = "供应商A")
  ```

## 配置修改

Swagger配置位于 `SwaggerConfig.java`，可以自定义：

### 修改API信息

```java
private ApiInfo apiInfo() {
    return new ApiInfoBuilder()
            .title("供应链金融系统 API文档")  // 修改标题
            .description("基于FISCO BCOS区块链的供应链金融系统接口文档")  // 修改描述
            .contact(new Contact("你的名字", "http://your-site.com", "your-email@example.com"))
            .version("1.0.0")
            .build();
}
```

### 修改扫描包

```java
.apis(RequestHandlerSelectors.basePackage("com.fisco.app.controller"))
```

### 修改路径匹配

```java
.paths(PathSelectors.any())  // 匹配所有路径
// .paths(PathSelectors.ant("/api/**"))  // 只匹配/api/开头的路径
```

## 生产环境配置

在生产环境中，建议禁用Swagger：

### 方案1: 配置文件控制

在 `application.properties` 中添加：

```properties
# 生产环境禁用Swagger
spring.profiles.active=prod
swagger.enabled=false
```

在 `SwaggerConfig.java` 中添加：

```java
@ConditionalOnProperty(name = "swagger.enabled", havingValue = "true", matchIfMissing = true)
@Configuration
public class SwaggerConfig {
    // ...
}
```

### 方案2: Profile控制

```java
@Profile({"dev", "test"})  // 只在开发和测试环境启用
@Configuration
public class SwaggerConfig {
    // ...
}
```

## 常见问题

### Q: Swagger UI无法访问？

**A**: 检查以下几点：
1. 应用是否正常启动
2. 端口是否正确（默认8080）
3. 依赖是否正确引入
4. 检查应用日志是否有错误信息

### Q: 接口显示不完整？

**A**: 确保：
1. Controller类在配置的扫描包内
2. 添加了正确的Swagger注解
3. 方法是public的

### Q: 模型显示异常？

**A**: 检查：
1. Entity类添加了`@ApiModel`注解
2. 字段添加了`@ApiModelProperty`注解
3. 实体类有getter方法（使用Lombok的@Data注解）

### Q: 请求参数显示不正确？

**A**: 确认：
1. 使用了`@ApiParam`注解
2. 参数类型正确
3. Bean Validation注解正确（如`@NotNull`, `@Valid`）

## 导出API文档

### 导出JSON格式

访问 http://localhost:8080/v3/api-docs
保存JSON文件

### 导出YAML格式

访问 http://localhost:8080/v3/api-docs.yaml
保存YAML文件

### 使用Swagger Codegen

导出的JSON/YAML可用于生成客户端代码：

```bash
# 生成Java客户端
java -jar swagger-codegen-cli.jar generate \
  -i http://localhost:8080/v3/api-docs \
  -l java \
  -o client/java

# 生成TypeScript客户端
java -jar swagger-codegen-cli.jar generate \
  -i http://localhost:8080/v3/api-docs \
  -l typescript-axios \
  -o client/ts
```

## 最佳实践

1. **保持文档更新**: 添加新接口时同步更新注解
2. **使用示例**: 为复杂参数提供example值
3. **详细描述**: notes字段提供详细的使用说明
4. **分组清晰**: 合理规划API分组，便于查找
5. **版本控制**: API变更时更新版本号
6. **安全考虑**: 生产环境禁用Swagger

## 相关资源

- Swagger官方文档: https://swagger.io/docs/
- SpringFox文档: https://springfox.github.io/springfox/docs/current/
- Swagger规范: https://swagger.io/specification/

## 更新日志

- 2024-01-13: 集成Swagger 3.0，添加企业管理和应收账款管理API文档
