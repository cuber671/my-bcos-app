#!/bin/bash
# =============================================================================
# FISCO BCOS 部署环境检查脚本
# 用法: ./scripts/check-environment.sh
# =============================================================================

set -e

echo "=========================================="
echo "FISCO BCOS 部署环境检查"
echo "=========================================="
echo ""

ERROR_COUNT=0

# 1. 检查 Docker 容器状态
echo "[1/6] 检查 Docker 容器状态..."
check_container_status() {
    local container=$1
    if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
        echo "  ✓ $container 运行中"
        return 0
    else
        echo "  ✗ $container 未运行"
        ERROR_COUNT=$((ERROR_COUNT + 1))
        return 1
    fi
}

check_container_status "fisco-node0"
check_container_status "fisco-node1"
check_container_status "fisco-node2"
check_container_status "fisco-node3"
check_container_status "fisco-console"
echo ""

# 2. 检查节点 RPC 连通性
echo "[2/6] 检查节点 RPC 连通性..."
check_rpc() {
    local port=$1
    local name=$2
    response=$(curl -s -X POST http://localhost:$port -H "Content-Type: application/json" \
        -d '{"jsonrpc":"2.0","method":"getBlockNumber","params":[],"id":1}' 2>/dev/null || echo '{"error":{}}')

    if echo "$response" | grep -q '"result"'; then
        block=$(echo "$response" | grep -o '"result":"0x[^"]*"' | cut -d'"' -f4)
        if [ -n "$block" ]; then
            block_dec=$((16#${block#0x}))
            echo "  ✓ $name 连通正常 (块高: $block_dec)"
        else
            echo "  ✓ $name 连通正常"
        fi
        return 0
    else
        echo "  ✗ $name 无法连接"
        ERROR_COUNT=$((ERROR_COUNT + 1))
        return 1
    fi
}

check_rpc 20000 "fisco-node0"
check_rpc 20001 "fisco-node1"
check_rpc 20002 "fisco-node2"
check_rpc 20003 "fisco-node3"
echo ""

# 3. 检查控制台与节点连接
echo "[3/6] 检查控制台与节点连接..."
if docker exec fisco-console bash -c "curl -s -m 5 -X POST http://fisco-node0:20000 -H 'Content-Type: application/json' -d \"{\\\"jsonrpc\\\":\\\"2.0\\\",\\\"method\\\":\\\"getNodeVersion\\\",\\\"params\\\":[],\\\"id\\\":1}\"" > /dev/null 2>&1; then
    echo "  ✓ 控制台可连接节点"
else
    echo "  ✗ 控制台无法连接节点 (检查 config.toml 网络配置)"
    ERROR_COUNT=$((ERROR_COUNT + 1))
fi
echo ""

# 4. 检查共识状态
echo "[4/6] 检查共识机制状态..."
response=$(curl -s -X POST http://localhost:20000 -H "Content-Type: application/json" \
    -d '{"jsonrpc":"2.0","method":"getConsensusStatus","params":[],"id":1}' 2>/dev/null || echo '{"error":{}}')

if echo "$response" | grep -q '"error"'; then
    echo "  ✗ 共识状态查询失败"
    ERROR_COUNT=$((ERROR_COUNT + 1))
else
    # 检查共识节点数量 - 尝试多种格式
    sealer_count=$(echo "$response" | grep -o '"sealerList":\[[^]]*\]' | grep -o ',' | wc -l || echo "0")
    observer_count=$(echo "$response" | grep -o '"observerList":\[[^]]*\]' | grep -o ',' | wc -l || echo "0")

    # 更准确的方式：提取节点数组
    if echo "$response" | grep -q 'sealerList'; then
        # 提取 sealerList 内容
        sealer_content=$(echo "$response" | sed 's/.*"sealerList":\([^}]*\).*/\1/')
        if [ -n "$sealer_content" ] && [ "$sealer_content" != "[]" ]; then
            # 计算节点数
            node_count=$(echo "$sealer_content" | grep -o '{' | wc -l)
            if [ "$node_count" -gt 0 ]; then
                echo "  ✓ 共识机制正常 (共识节点数: $node_count)"
            else
                echo "  ⚠ 共识节点可能不足"
            fi
        else
            echo "  ⚠ 共识节点列表为空"
        fi
    else
        echo "  ⚠ 无法获取共识状态"
    fi
fi
echo ""

# 5. 检查 SDK 证书
echo "[5/6] 检查 SDK 证书..."
if [ -d "./fisco/nodes/127.0.0.1/sdk" ]; then
    if [ -f "./fisco/nodes/127.0.0.1/sdk/sdk.crt" ] && [ -f "./fisco/nodes/127.0.0.1/sdk/sdk.key" ]; then
        echo "  ✓ SDK 证书存在"
    else
        echo "  ✗ SDK 证书缺失 (sdk.crt 或 sdk.key 不存在)"
        ERROR_COUNT=$((ERROR_COUNT + 1))
    fi
else
    echo "  ✗ SDK 目录不存在 (./fisco/nodes/127.0.0.1/sdk)"
    ERROR_COUNT=$((ERROR_COUNT + 1))
fi
echo ""

# 6. 检查合约源码
echo "[6/6] 检查合约源码..."
contract_count=$(find ./contracts -name "*.sol" 2>/dev/null | wc -l)
if [ "$contract_count" -gt 0 ]; then
    echo "  ✓ 合约源码存在 ($contract_count 个 .sol 文件)"
else
    echo "  ✗ 未找到合约源码 (./contracts/*.sol)"
    ERROR_COUNT=$((ERROR_COUNT + 1))
fi
echo ""

# 检查控制台配置
echo "[额外] 检查控制台配置..."
if [ -f "./console/conf/config.toml" ]; then
    echo "  ✓ 控制台配置文件存在"

    # 检查 peers 配置
    if grep -q 'peers' ./console/conf/config.toml; then
        echo "  ✓ 节点配置存在"
    else
        echo "  ⚠ 未找到 peers 配置"
    fi
else
    echo "  ✗ 控制台配置文件不存在 (./console/conf/config.toml)"
    ERROR_COUNT=$((ERROR_COUNT + 1))
fi
echo ""

# 检查控制台 JAR
echo "[额外] 检查控制台 JAR..."
if [ -f "./console/apps/console.jar" ]; then
    echo "  ✓ 控制台 JAR 存在"
else
    echo "  ✗ 控制台 JAR 不存在 (./console/apps/console.jar)"
    ERROR_COUNT=$((ERROR_COUNT + 1))
fi
echo ""

# 汇总结果
echo "=========================================="
if [ "$ERROR_COUNT" -eq 0 ]; then
    echo "✓ 环境检查通过，可以进行部署"
    echo "=========================================="
    exit 0
else
    echo "✗ 环境检查未通过，发现 $ERROR_COUNT 个问题"
    echo "请修复上述问题后重新运行此脚本"
    echo "=========================================="
    exit 1
fi
