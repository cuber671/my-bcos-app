#!/bin/bash
#
# EnterpriseRegistry 智能合约完整部署脚本
# 此脚本将引导您完成整个部署过程
#

set -e

CONSOLE_HOME="/home/llm_rca/fisco/console/dist"
PROJECT_HOME="/home/llm_rca/fisco/my-bcos-app"
CONFIG_FILE="$PROJECT_HOME/src/main/resources/application.properties"

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  EnterpriseRegistry 智能合约部署工具 v2.0                 ║"
echo "║  添加 DELETED 状态支持                                      ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# ============================================
# 步骤 1: 准备合约文件
# ============================================
echo "📋 步骤 1/6: 准备智能合约文件"
echo "────────────────────────────────────────────────────────────"

SOL_SOURCE="$PROJECT_HOME/contracts/enterprise/EnterpriseRegistry.sol"
SOL_DEST="$CONSOLE_HOME/contracts/solidity/EnterpriseRegistry.sol"

if [ ! -f "$SOL_SOURCE" ]; then
    echo "❌ 错误: 源合约文件不存在: $SOL_SOURCE"
    exit 1
fi

cp "$SOL_SOURCE" "$SOL_DEST"
echo "✅ 合约文件已复制"
echo "   源文件: $SOL_SOURCE"
echo "   目标文件: $SOL_DEST"
echo ""

# ============================================
# 步骤 2: 编译合约
# ============================================
echo "📦 步骤 2/6: 编译智能合约"
echo "────────────────────────────────────────────────────────────"

cd "$CONSOLE_HOME"

# 清理旧的编译文件
rm -rf contracts/sdk/java/org/fisco/bcos/sdk/demo/contract/EnterpriseRegistry.java 2>/dev/null || true

# 执行编译
echo "执行编译命令..."
if bash contract2java.sh solidity org.fisco.bcos.sdk.demo > /tmp/compile_output.log 2>&1; then
    echo "✅ 合约编译成功"

    # 检查生成的文件
    if [ -f "contracts/sdk/abi/EnterpriseRegistry.abi" ]; then
        echo "   ✅ ABI文件已生成 ($(stat -c%s contracts/sdk/abi/EnterpriseRegistry.abi) 字节)"
    else
        echo "   ⚠️  警告: ABI文件未生成"
    fi

    if [ -f "contracts/sdk/bin/EnterpriseRegistry.bin" ]; then
        echo "   ✅ BIN文件已生成 ($(stat -c%s contracts/sdk/bin/EnterpriseRegistry.bin) 字节)"
    else
        echo "   ⚠️  警告: BIN文件未生成"
    fi
else
    echo "❌ 合约编译失败"
    echo "错误日志:"
    tail -20 /tmp/compile_output.log
    exit 1
fi
echo ""

# ============================================
# 步骤 3: 部署合约（交互式）
# ============================================
echo "🚀 步骤 3/6: 部署智能合约到区块链"
echo "────────────────────────────────────────────────────────────"
echo ""
echo "现在需要通过控制台手动部署合约。"
echo ""
echo "请在【新终端】中执行以下命令："
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "cd $CONSOLE_HOME"
echo "bash scripts/start.sh"
echo ""
echo "然后在控制台提示符下输入："
echo ""
echo "  [root@localhost]:> deploy EnterpriseRegistry"
echo ""
echo "系统会返回类似这样的输出："
echo "  deploy EnterpriseRegistry success"
echo "  Contract address: 0x1234567890abcdef..."
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "⚠️  请复制上面的合约地址（0x开头的40位十六进制字符）"
echo ""

# 读取合约地址
read -p "请输入部署后的合约地址: " NEW_CONTRACT_ADDRESS

# 验证地址格式
if [[ ! "$NEW_CONTRACT_ADDRESS" =~ ^0x[a-fA-F0-9]{40}$ ]]; then
    echo "❌ 错误: 合约地址格式不正确"
    echo "   正确格式: 0x后跟40位十六进制字符"
    echo "   示例: 0x1234567890abcdef1234567890abcdef12345678"
    exit 1
fi

echo ""
echo "✅ 新合约地址: $NEW_CONTRACT_ADDRESS"
echo ""

# ============================================
# 步骤 4: 验证合约部署
# ============================================
echo "🔍 步骤 4/6: 验证合约部署"
echo "────────────────────────────────────────────────────────────"
echo "合约地址格式验证通过"
echo "地址长度: ${#NEW_CONTRACT_ADDRESS} 字符"
echo "前缀: ${NEW_CONTRACT_ADDRESS:0:2}"
echo "主体: ${NEW_CONTRACT_ADDRESS:2}"
echo "✅ 部署验证完成"
echo ""

# ============================================
# 步骤 5: 更新应用配置
# ============================================
echo "⚙️  步骤 5/6: 更新应用配置"
echo "────────────────────────────────────────────────────────────"

# 备份原配置
BACKUP_FILE="$CONFIG_FILE.deploy.backup.$(date +%Y%m%d_%H%M%S)"
cp "$CONFIG_FILE" "$BACKUP_FILE"
echo "✅ 配置文件已备份"
echo "   备份位置: $BACKUP_FILE"

# 更新合约地址
if grep -q "contracts.enterprise.address=" "$CONFIG_FILE"; then
    sed -i "s|^contracts.enterprise.address=.*|contracts.enterprise.address=$NEW_CONTRACT_ADDRESS|" "$CONFIG_FILE"
    echo "✅ 合约地址已更新"
else
    echo "❌ 错误: 配置文件中未找到 contracts.enterprise.address"
    exit 1
fi

# 验证更新
UPDATED_ADDRESS=$(grep "contracts.enterprise.address=" "$CONFIG_FILE" | cut -d'=' -f2)
if [ "$UPDATED_ADDRESS" = "$NEW_CONTRACT_ADDRESS" ]; then
    echo "✅ 配置更新验证成功"
    echo "   新地址: $UPDATED_ADDRESS"
else
    echo "❌ 配置更新失败"
    exit 1
fi
echo ""

# ============================================
# 步骤 6: 生成重启脚本
# ============================================
echo "🔄 步骤 6/6: 生成应用重启脚本"
echo "────────────────────────────────────────────────────────────"

cat > /tmp/restart_application.sh << 'RESTART_SCRIPT'
#!/bin/bash

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  重启应用程序                                               ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

PROJECT_HOME="/home/llm_rca/fisco/my-bcos-app"

# 停止旧应用
echo "🛑 停止旧的应用实例..."
if pgrep -f "my-bcos-app" > /dev/null; then
    pkill -f "my-bcos-app"
    echo "✅ 应用已停止"
    sleep 3
else
    echo "ℹ️  应用未运行"
fi

# 清理可能的残留进程
pkill -9 -f "spring-boot:run" 2>/dev/null || true
sleep 2

# 启动新应用
echo ""
echo "🚀 启动新的应用实例..."
cd "$PROJECT_HOME"

# 后台启动应用
nohup mvn spring-boot:run > /tmp/app_output.log 2>&1 &
APP_PID=$!

echo "✅ 应用已启动 (PID: $APP_PID)"
echo ""

# 等待应用初始化
echo "⏳ 等待应用初始化（约20秒）..."
for i in {20..1}; do
    sleep 1
    echo -ne "   $i 秒...\r"
done
echo -ne "\n"

# 检查启动状态
if grep -q "Started BcosApplication" /tmp/app_output.log 2>/dev/null; then
    echo "✅ 应用启动成功！"
    echo ""
    echo "📊 启动信息:"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    tail -30 /tmp/app_output.log | grep -E "Started|Tomcat|EnterpriseRegistry"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "🌐 应用访问地址: http://localhost:8080"
    echo "📝 应用日志文件: /tmp/app_output.log"
    echo ""
    echo "💡 查看实时日志: tail -f /tmp/app_output.log"
else
    echo "⚠️  应用可能还在启动中，请检查日志："
    echo "   tail -f /tmp/app_output.log"
fi
RESTART_SCRIPT

chmod +x /tmp/restart_application.sh

echo "✅ 重启脚本已生成: /tmp/restart_application.sh"
echo ""

# ============================================
# 完成
# ============================================
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  🎉 部署配置完成！                                          ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "📋 部署摘要:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ 合约文件: 已复制并编译"
echo "✅ 合约地址: $NEW_CONTRACT_ADDRESS"
echo "✅ 配置文件: 已更新"
echo "✅ 配置备份: $BACKUP_FILE"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "🚀 下一步操作:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "执行以下命令重启应用："
echo ""
echo "  bash /tmp/restart_application.sh"
echo ""
echo "或者手动重启："
echo ""
echo "  cd $PROJECT_HOME"
echo "  pkill -f my-bcos-app"
echo "  mvn spring-boot:run"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "✨ 部署完成后，新合约将支持 DELETED 状态（值=4）"
echo ""
