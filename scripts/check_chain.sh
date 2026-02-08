#!/bin/bash
# 快速检查区块链状态的脚本

cd /home/llm_rca/fisco/console/dist

# 创建临时命令文件
cat > /tmp/console_commands.txt << 'EOF'
getBlockNumber
getNodeInfo
getPeers
getSealerList
quit
EOF

# 使用非交互模式执行（如果支持）或提供命令说明
echo "=========================================="
echo "FISCO BCOS 区块链状态检查"
echo "=========================================="
echo ""
echo "方法1: 通过 Java SDK 查询"
echo "----------------------------------------"

# 检查应用是否运行
if curl -s http://localhost:8080/api/health > /dev/null 2>&1; then
    echo "✓ 应用正在运行"
    echo ""
    echo "请重启应用后访问以下 URL:"
    echo "  健康检查: curl http://localhost:8080/api/health"
    echo "  区块高度: curl http://localhost:8080/api/block/latest"
    echo "  节点列表: curl http://localhost:8080/api/nodes"
else
    echo "✗ 应用未运行或需要认证"
fi

echo ""
echo "方法2: 查看节点日志"
echo "----------------------------------------"
echo "当前区块高度:"
tail -100 /home/llm_rca/fisco/nodes/127.0.0.1/node0/log/log_*.log | grep "groupBlockNumber" | tail -1

echo ""
echo "方法3: 使用 FISCO BCOS 控制台（交互式）"
echo "----------------------------------------"
echo "请在控制台中依次执行以下命令:"
echo "  1. getBlockNumber   # 查看区块高度"
echo "  2. getNodeInfo      # 查看节点信息"
echo "  3. getPeers         # 查看连接的节点"
echo "  4. getSealerList    # 查看共识节点列表"
echo ""
echo "启动控制台: cd /home/llm_rca/fisco/console/dist && ./start.sh"
echo ""
echo "=========================================="

# 清理
rm -f /tmp/console_commands.txt
