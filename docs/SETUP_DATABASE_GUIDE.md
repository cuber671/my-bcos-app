# 数据库配置指南

## 问题说明

当前MySQL的root用户使用`auth_socket`认证插件，这会导致Java应用无法通过TCP/IP连接到数据库，会出现错误：

```
ERROR 1698 (28000): Access denied for user 'root'@'localhost'
```

## 解决方案

您有以下几种方案来解决这个问题：

### 方案1：创建专用的数据库用户（推荐）

这是最安全且推荐的方式，创建一个专门用于应用的数据库用户：

```bash
# 连接到MySQL（使用sudo和auth_socket）
sudo mysql

# 在MySQL命令行中执行：
CREATE USER 'fisco_admin'@'localhost' IDENTIFIED BY 'YourSecurePassword123!';
GRANT ALL PRIVILEGES ON bcos_supply_chain.* TO 'fisco_admin'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

然后更新`.env`文件：

```bash
# 修改.env文件
DB_USERNAME=fisco_admin
DB_PASSWORD=YourSecurePassword123!
```

### 方案2：修改root用户使用密码认证

如果您希望继续使用root用户，需要修改其认证方式：

```bash
# 连接到MySQL
sudo mysql

# 在MySQL命令行中执行：
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'MySQL@2024';
FLUSH PRIVILEGES;
EXIT;
```

然后确保`.env`文件中有正确的密码：

```bash
DB_USERNAME=root
DB_PASSWORD=MySQL@2024
```

### 方案3：临时禁用密码验证（不推荐，仅用于测试）

```bash
sudo mysql

# 在MySQL命令行中执行：
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '';
FLUSH PRIVILEGES;
EXIT;
```

## 验证配置

执行以下命令验证配置是否正确：

```bash
# 测试数据库连接
mysql -h 127.0.0.1 -u fisco_admin -p'YourSecurePassword123!' -e "SELECT 1;"
# 或者
mysql -h 127.0.0.1 -u root -p'MySQL@2024' -e "SELECT 1;"
```

如果连接成功，应该显示：

```
+---+
| 1 |
+---+
| 1 |
+---+
```

## 启动应用

配置完成后，使用启动脚本启动应用：

```bash
./start.sh
```

或者直接使用Maven：

```bash
mvn spring-boot:run
```

## 应用启动成功标志

当应用启动成功时，您应该看到类似以下的日志：

```
INFO o.s.b.w.e.tomcat.TomcatWebServer - Tomcat started on port(s): 8080 (http)
INFO c.f.a.BcosApplication - Started BcosApplication in X.XXX seconds
```

并且可以访问：
- Swagger UI: http://localhost:8080/swagger-ui.html
- 健康检查: http://localhost:8080/actuator/health

## 故障排查

### 问题1: ERROR 1698仍然出现

**原因**: MySQL用户仍使用auth_socket认证
**解决**: 确保按照上述方案修改了用户认证方式

### 问题2: 无法连接到数据库

**原因**: MySQL可能未运行
**解决**: 检查MySQL服务状态
```bash
ps aux | grep mysql
```

### 问题3: 数据库不存在

**原因**: bcos_supply_chain数据库未创建
**解决**: 应用启动时会自动创建，或者手动创建：
```bash
sudo mysql -e "CREATE DATABASE IF NOT EXISTS bcos_supply_chain;"
```

## 安全建议

1. **生产环境**: 务必使用方案1（专用数据库用户）
2. **密码强度**: 使用强密码，至少12位，包含大小写字母、数字和特殊字符
3. **权限控制**: 只授予必要的权限，不要使用ALL PRIVILEGES
4. **环境变量**: 不要将密码提交到Git，使用.env文件并确保在.gitignore中

## 示例：生产环境配置

```bash
# .env.production
DB_USERNAME=fisco_admin
DB_PASSWORD=VeryStrongPassword!@#123ABC
JWT_SECRET=$(openssl rand -base64 64)
```

启动时指定环境：

```bash
cp .env.production .env
./start.sh
```
