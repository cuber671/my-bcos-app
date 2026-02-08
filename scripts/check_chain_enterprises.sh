#!/bin/bash
# 查询区块链上已注册企业的脚本
# 作者：Claude Code
# 日期：2025-01-22

set -e

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  查询区块链已注册企业${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# 切换到console目录
cd /home/llm_rca/fisco/console/dist

echo -e "${YELLOW}[1/4] 查询区块链上的企业总数...${NC}"
./console.sh call EnterpriseRegistry.enterpriseCount
echo ""

echo -e "${YELLOW}[2/4] 获取管理员地址...${NC}"
./console.sh call EnterpriseRegistry.admin
echo ""

echo -e "${YELLOW}[3/4] 检查特定企业是否已注册${NC}"
echo -e "${BLUE}请输入企业区块链地址（0x开头），或按 Enter 跳过：${NC}"
read -p "> " enterprise_addr

if [ ! -z "$enterprise_addr" ]; then
    echo -e "${YELLOW}查询企业地址: $enterprise_addr${NC}"
    ./console.sh call EnterpriseRegistry.getEnterprise $enterprise_addr
    echo ""
fi

echo -e "${YELLOW}[4/4] 检查统一社会信用代码是否已注册${NC}"
echo -e "${BLUE}请输入统一社会信用代码（18位），或按 Enter 跳过：${NC}"
read -p "> " credit_code

if [ ! -z "$credit_code" ]; then
    echo -e "${YELLOW}查询信用代码: $credit_code${NC}"
    ./console.sh call EnterpriseRegistry.getAddressByCreditCode $credit_code
    echo ""
fi

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  查询完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${BLUE}提示：${NC}"
echo -e "  - ${GREEN}exists=true${NC} 表示企业已成功上链"
echo -e "  - ${RED}exists=false${NC} 或 revert 表示企业未上链"
echo -e "  - 可以通过审核日志的 txHash 在区块链浏览器中查看交易详情"
