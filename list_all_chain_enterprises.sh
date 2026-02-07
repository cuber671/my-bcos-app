#!/bin/bash
# 列出区块链上所有已注册企业的脚本
# 使用数据库中的企业地址逐一查询区块链状态

set -e

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}============================================${NC}"
echo -e "${CYAN}   区块链企业注册状态检查工具${NC}"
echo -e "${CYAN}============================================${NC}"
echo ""

# 需要管理员TOKEN
TOKEN_FILE="/tmp/token2.txt"
if [ ! -f "$TOKEN_FILE" ]; then
    echo -e "${RED}错误：未找到TOKEN文件 $TOKEN_FILE${NC}"
    echo -e "${YELLOW}请先登录管理员账号获取TOKEN${NC}"
    exit 1
fi

TOKEN=$(cat $TOKEN_FILE | tr -d '\n')
API_URL="http://localhost:8080/api/admin/enterprise"

echo -e "${YELLOW}[步骤1] 查询数据库中所有已激活的企业...${NC}"
echo ""

# 获取所有ACTIVE状态的企业（无分页限制，获取前100个）
RESPONSE=$(curl -s -X GET "$API_URL/list?status=ACTIVE&page=0&size=100" \
  -H "Authorization: Bearer $TOKEN")

# 检查响应
if echo "$RESPONSE" | jq -e '.code' > /dev/null 2>&1; then
    CODE=$(echo "$RESPONSE" | jq -r '.code')
    if [ "$CODE" != "200" ]; then
        echo -e "${RED}API请求失败：$RESPONSE${NC}"
        exit 1
    fi
else
    echo -e "${RED}无效的API响应${NC}"
    echo "$RESPONSE"
    exit 1
fi

# 提取企业数量
TOTAL=$(echo "$RESPONSE" | jq -r '.data.totalElements')
echo -e "${GREEN}找到 $TOTAL 家已激活的企业${NC}"
echo ""

if [ "$TOTAL" -eq 0 ]; then
    echo -e "${YELLOW}没有已激活的企业${NC}"
    exit 0
fi

echo -e "${YELLOW}[步骤2] 检查每个企业在区块链上的注册状态...${NC}"
echo ""
echo -e "${CYAN}----------------------------------------------------------------------------------------${NC}"
printf "${CYAN}%-5s %-45s %-20s %-10s${NC}\n" "序号" "企业地址" "企业名称" "上链状态"
echo -e "${CYAN}----------------------------------------------------------------------------------------${NC}"

# 切换到console目录
cd /home/llm_rca/fisco/console/dist

# 遍历所有企业
echo "$RESPONSE" | jq -r '.data.content[] | @json' | while read -r enterprise_json; do
    # 解析JSON字段
    id=$(echo "$enterprise_json" | jq -r '.id')
    address=$(echo "$enterprise_json" | jq -r '.address')
    name=$(echo "$enterprise_json" | jq -r '.name')
    status=$(echo "$enterprise_json" | jq -r '.status')

    # 查询区块链
    chain_result=$(./console.sh call EnterpriseRegistry.getEnterprise $address 2>&1 | grep -A 20 "Return values:" || echo "")

    # 判断是否上链
    if echo "$chain_result" | grep -q "exists : true"; then
        chain_status="${GREEN}✓ 已上链${NC}"
        # 提取链上企业名称
        chain_name=$(echo "$chain_result" | grep "name" | awk -F': ' '{print $2}' | xargs || echo "")
    else
        chain_status="${RED}✗ 未上链${NC}"
        chain_name=""
    fi

    # 输出结果
    count=$((count + 1))
    printf "%-5s %-45s %-20s %b\n" "$count" "$address" "$name" "$chain_status"

    # 如果已上链，显示链上信息
    if [ ! -z "$chain_name" ]; then
        echo -e "       ${BLUE}链上名称: $chain_name${NC}"
    fi
done

echo -e "${CYAN}----------------------------------------------------------------------------------------${NC}"
echo ""

echo -e "${YELLOW}[步骤3] 统计区块链上的企业总数...${NC}"
chain_count=$(./console.sh call EnterpriseRegistry.enterpriseCount 2>&1 | grep "uint256" | awk -F': ' '{print $2}' | xargs || echo "未知")
echo -e "${GREEN}区块链上注册的企业总数: $chain_count${NC}"
echo ""

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}   检查完成！${NC}"
echo -e "${GREEN}============================================${NC}"
