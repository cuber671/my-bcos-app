#!/bin/bash
# =============================================================================
# FISCO BCOS Java 绑定生成脚本
# 用法: ./scripts/generate-java-bindings.sh
# 注意: 需要先部署合约并配置 .env 中的合约地址
# =============================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
ABI_DIR="$PROJECT_DIR/src/main/resources/contracts/abi"
JAVA_OUT_DIR="$PROJECT_DIR/src/main/java/com/fisco/app/contract"
CONSOLE_DIR="$PROJECT_DIR/console"

# 合约列表
declare -a CONTRACTS=(
    "EnterpriseRegistryV2"
    "EnterpriseRegistryAuth"
    "CreditLimitCore"
    "CreditLimitScore"
    "WarehouseReceiptCore"
    "WarehouseReceiptOps"
    "WarehouseReceiptHistory"
    "ReceivableCore"
    "ReceivableRepayment"
    "BillCore"
    "BillOps"
    "LogisticsCore"
    "LogisticsOps"
)

# 映射表: 合约名 -> 模块目录
declare -A CONTRACT_MODULES=(
    ["EnterpriseRegistryV2"]="enterprise"
    ["EnterpriseRegistryAuth"]="enterprise"
    ["CreditLimitCore"]="credit"
    ["CreditLimitScore"]="credit"
    ["WarehouseReceiptCore"]="warehouse"
    ["WarehouseReceiptOps"]="warehouse"
    ["WarehouseReceiptHistory"]="warehouse"
    ["ReceivableCore"]="receivable"
    ["ReceivableRepayment"]="receivable"
    ["BillCore"]="bill"
    ["BillOps"]="bill"
    ["LogisticsCore"]="logistics"
    ["LogisticsOps"]="logistics"
)

echo "=========================================="
echo "FISCO BCOS Java 绑定生成"
echo "=========================================="
echo ""

# 创建 ABI 目录并复制文件
mkdir -p "$ABI_DIR"
if [ -d "$CONSOLE_DIR/contracts/build" ]; then
    echo "从控制台复制 ABI 文件..."
    cp -f "$CONSOLE_DIR/contracts/build/"*.abi "$ABI_DIR/" 2>/dev/null || true
fi

# 检查 ABI 文件
if [ ! -d "$ABI_DIR" ] || [ -z "$(ls -A $ABI_DIR 2>/dev/null)" ]; then
    echo -e "${RED}错误: 未找到 ABI 文件${NC}"
    echo "ABI 目录: $ABI_DIR"
    exit 1
fi

# 创建输出目录
mkdir -p "$JAVA_OUT_DIR"

# 创建模块子目录
for module in enterprise credit warehouse receivable bill logistics; do
    mkdir -p "$JAVA_OUT_DIR/$module"
done

echo "ABI 目录: $ABI_DIR"
echo "输出目录: $JAVA_OUT_DIR"
echo ""

# 检查控制台 contract2java.sh
if [ ! -f "$CONSOLE_DIR/contract2java.sh" ]; then
    echo -e "${RED}错误: 未找到 contract2java.sh${NC}"
    exit 1
fi

# 复制 ABI 到控制台目录
echo "复制 ABI 到控制台目录..."
cp -f "$ABI_DIR/"*.abi "$CONSOLE_DIR/contracts/" 2>/dev/null || true

# 生成 Java 绑定 (一次性编译所有合约)
echo ""
echo "生成 Java 绑定 (这可能需要几分钟)..."
echo ""

# 清理之前的输出
docker exec fisco-console bash -c "rm -rf /data/src" 2>/dev/null || true

# 运行 contract2java (编译所有合约)
echo "编译合约中..."
if docker exec fisco-console bash -c "cd /data && timeout 600 ./contract2java.sh solidity -n -o /data/src -p com.fisco.app.contract 2>&1"; then
    echo -e "${GREEN}编译完成${NC}"
else
    echo -e "${YELLOW}编译过程有警告，但继续尝试复制文件...${NC}"
fi

# 检查输出并复制到模块目录
echo ""
echo "组织 Java 文件到模块目录..."

TOTAL=0
SUCCESS=0

for contract in "${CONTRACTS[@]}"; do
    module="${CONTRACT_MODULES[$contract]}"
    if [ -z "$module" ]; then
        module="other"
    fi

    # 查找生成的 Java 文件
    java_file="docker exec fisco-console bash -c \"ls /data/src/com/fisco/app/contract/${contract}.java 2>/dev/null\""

    if eval "$java_file" >/dev/null 2>&1; then
        # 复制到模块目录
        docker cp "fisco-console:/data/src/com/fisco/app/contract/${contract}.java" "$JAVA_OUT_DIR/$module/" 2>/dev/null || true
        docker cp "fisco-console:/data/src/com/fisco/app/contract/${contract}.java" "$JAVA_OUT_DIR/$module/" 2>/dev/null || true

        # 同时复制相关的内部类文件
        docker exec fisco-console bash -c "ls /data/src/com/fisco/app/contract/${contract}*.java 2>/dev/null" 2>/dev/null | while read f; do
            fname=$(basename "$f")
            docker cp "fisco-console:/data/src/com/fisco/app/contract/$fname" "$JAVA_OUT_DIR/$module/" 2>/dev/null || true
        done

        TOTAL=$((TOTAL + 1))
    fi
done

# 清理临时文件
docker exec fisco-console bash -c "rm -rf /data/src" 2>/dev/null || true

echo ""
echo "=========================================="
echo "Java 绑定生成完成"
echo "=========================================="

if [ -d "$JAVA_OUT_DIR" ]; then
    echo "生成的 Java 文件位于: $JAVA_OUT_DIR"
    echo ""
    echo "文件列表:"
    for dir in "$JAVA_OUT_DIR"/*/; do
        if [ -d "$dir" ] && [ -n "$(ls -A $dir 2>/dev/null)" ]; then
            module=$(basename "$dir")
            echo "  [$module]"
            ls -1 "$dir"*.java 2>/dev/null | xargs -n1 basename 2>/dev/null | sed 's/^/    /'
        fi
    done
fi

echo ""
echo -e "${GREEN}✓ 完成${NC}"
exit 0
