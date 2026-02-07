#!/bin/bash

###############################################################################
# 票据背书转让功能测试脚本
# 功能：测试票据背书、历史查询、区块链验证等完整流程
###############################################################################

# 配置
BASE_URL="http://localhost:8080"
BILL_ID="test-bill-001"
COMPANY_A_ADDRESS="0x1234567890abcdef1234567890abcdef12345678"
COMPANY_B_ADDRESS="0xabcdef1234567890abcdef1234567890abcdef12"
COMPANY_C_ADDRESS="0xfedcba0987654321fedcba0987654321fedcba09"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

###############################################################################
# 工具函数
###############################################################################

print_section() {
    echo -e "\n${GREEN}========================================${NC}"
    echo -e "${GREEN}$1${NC}"
    echo -e "${GREEN}========================================${NC}\n"
}

print_test() {
    echo -e "\n${YELLOW}测试: $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# JWT Token（需要先登录获取）
COMPANY_A_TOKEN=""
COMPANY_B_TOKEN=""

###############################################################################
# 测试前准备
###############################################################################

setup() {
    print_section "测试前准备"

    # 1. 登录获取Token
    print_test "公司A登录"
    LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{
            "username": "company_a",
            "password": "password123"
        }')

    COMPANY_A_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.token // empty')

    if [ -z "$COMPANY_A_TOKEN" ]; then
        print_error "公司A登录失败，请先创建测试用户"
        echo "提示: 使用 /api/auth/register 注册测试用户"
        exit 1
    fi
    print_success "公司A登录成功"

    # 2. 检查票据是否存在
    print_test "检查测试票据"
    BILL_RESPONSE=$(curl -s -X GET "${BASE_URL}/api/bill/${BILL_ID}" \
        -H "Authorization: Bearer $COMPANY_A_TOKEN")

    BILL_STATUS=$(echo $BILL_RESPONSE | jq -r '.code // empty')

    if [ "$BILL_STATUS" != "200" ]; then
        print_error "测试票据不存在，请先创建票据"
        echo "票据ID: $BILL_ID"
        exit 1
    fi

    CURRENT_HOLDER=$(echo $BILL_RESPONSE | jq -r '.data.currentHolderAddress // empty')
    BILL_STATUS=$(echo $BILL_RESPONSE | jq -r '.data.billStatus // empty')

    print_success "票据存在"
    echo "  - 当前持票人: $CURRENT_HOLDER"
    echo "  - 票据状态: $BILL_STATUS"
}

###############################################################################
# 测试用例
###############################################################################

test_case_1() {
    print_section "测试用例1: 正常背书流程"

    print_test "A公司将票据背书转让给B公司"

    RESPONSE=$(curl -s -X POST "${BASE_URL}/api/bill/${BILL_ID}/endorse" \
        -H "Authorization: Bearer $COMPANY_A_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"endorseeAddress\": \"${COMPANY_B_ADDRESS}\",
            \"endorsementType\": \"NORMAL\",
            \"remark\": \"A公司转让给B公司用于货款结算\"
        }")

    CODE=$(echo $RESPONSE | jq -r '.code')
    MESSAGE=$(echo $RESPONSE | jq -r '.message')

    if [ "$CODE" == "200" ]; then
        print_success "背书成功"

        ENDORSEMENT_ID=$(echo $RESPONSE | jq -r '.data.id')
        ENDORSEMENT_SEQ=$(echo $RESPONSE | jq -r '.data.endorsementSequence')
        TX_HASH=$(echo $RESPONSE | jq -r '.data.txHash')

        echo "  - 背书ID: $ENDORSEMENT_ID"
        echo "  - 背书序号: $ENDORSEMENT_SEQ"
        echo "  - 交易哈希: $TX_HASH"
    else
        print_error "背书失败: $MESSAGE"
        return 1
    fi
}

test_case_2() {
    print_section "测试用例2: 查询背书历史"

    print_test "查询票据的所有背书记录"

    RESPONSE=$(curl -s -X GET "${BASE_URL}/api/bill/${BILL_ID}/endorsements" \
        -H "Authorization: Bearer $COMPANY_A_TOKEN")

    CODE=$(echo $RESPONSE | jq -r '.code')

    if [ "$CODE" == "200" ]; then
        print_success "查询成功"

        COUNT=$(echo $RESPONSE | jq -r '.data | length')
        echo "  - 背书记录数: $COUNT"

        echo "\n背书历史:"
        echo $RESPONSE | jq -r '.data[] | "
  序号: \(.endorsementSequence)
  背书人: \(.endorserAddress)
  被背书人: \(.endorseeAddress)
  类型: \(.endorsementType)
  日期: \(.endorsementDate)
  备注: \(.remark
  ---)"'
    else
        print_error "查询失败"
        return 1
    fi
}

test_case_3() {
    print_section "测试用例3: 从区块链查询背书历史"

    print_test "查询区块链上的背书记录"

    RESPONSE=$(curl -s -X GET "${BASE_URL}/api/bill/${BILL_ID}/endorsements/chain" \
        -H "Authorization: Bearer $COMPANY_A_TOKEN")

    CODE=$(echo $RESPONSE | jq -r '.code')

    if [ "$CODE" == "200" ]; then
        print_success "查询成功"

        COUNT=$(echo $RESPONSE | jq -r '.data | length')
        echo "  - 区块链记录数: $COUNT"

        echo "\n区块链记录:"
        echo $RESPONSE | jq -r '.data[] | "
  索引: \(.index)
  背书人: \(.endorser)
  被背书人: \(.endorsee)
  类型: \(.endorsementType)
  时间戳: \(.timestamp)
  ---)"'
    else
        print_error "查询失败"
        return 1
    fi
}

test_case_4() {
    print_section "测试用例4: 验证数据完整性"

    print_test "验证数据库和区块链数据一致性"

    RESPONSE=$(curl -s -X GET "${BASE_URL}/api/bill/${BILL_ID}/endorsements/validate" \
        -H "Authorization: Bearer $COMPANY_A_TOKEN")

    CODE=$(echo $RESPONSE | jq -r '.code')

    if [ "$CODE" == "200" ]; then
        print_success "验证完成"

        IS_VALID=$(echo $RESPONSE | jq -r '.data.isValid')
        DB_COUNT=$(echo $RESPONSE | jq -r '.data.dbCount')
        CHAIN_COUNT=$(echo $RESPONSE | jq -r '.data.chainCount')
        MESSAGE=$(echo $RESPONSE | jq -r '.data.message')

        echo "  - 数据库记录数: $DB_COUNT"
        echo "  - 区块链记录数: $CHAIN_COUNT"
        echo "  - 验证结果: $IS_VALID"
        echo "  - 消息: $MESSAGE"

        if [ "$IS_VALID" == "true" ]; then
            print_success "数据完整性验证通过"
        else
            print_error "数据完整性验证失败"
            echo $RESPONSE | jq -r '.data.mismatches'
        fi
    else
        print_error "验证失败"
        return 1
    fi
}

test_case_5() {
    print_section "测试用例5: 重复背书"

    print_test "B公司再次背书转让给C公司"

    # 注意: 这里需要B公司的Token，实际使用时需要B公司登录
    print_error "需要B公司登录后才能执行此测试"
    echo "  提示: 实际场景中，需要被背书人(B公司)登录后再执行背书"

    # 模拟请求（实际需要B公司的Token）
    cat <<EOF
    # 模拟请求:
    curl -X POST "${BASE_URL}/api/bill/${BILL_ID}/endorse" \\
        -H "Authorization: Bearer \$COMPANY_B_TOKEN" \\
        -H "Content-Type: application/json" \\
        -d '{
            "endorseeAddress": "${COMPANY_C_ADDRESS}",
            "endorsementType": "NORMAL",
            "remark": "B公司转让给C公司"
        }'
EOF
}

test_case_6() {
    print_section "测试用例6: 错误处理"

    # 测试6.1: 背书给自己
    print_test "6.1 测试背书给自己（应失败）"
    RESPONSE=$(curl -s -X POST "${BASE_URL}/api/bill/${BILL_ID}/endorse" \
        -H "Authorization: Bearer $COMPANY_A_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"endorseeAddress\": \"${COMPANY_A_ADDRESS}\",
            \"endorsementType\": \"NORMAL\",
            \"remark\": \"测试背书给自己\"
        }")

    CODE=$(echo $RESPONSE | jq -r '.code')
    if [ "$CODE" != "200" ]; then
        print_success "正确拒绝背书给自己"
        echo "  - 错误信息: $(echo $RESPONSE | jq -r '.message')"
    else
        print_error "未能正确拒绝背书给自己"
    fi

    # 测试6.2: 背书给不存在的地址
    print_test "6.2 测试背书给不存在的地址（应失败）"
    RESPONSE=$(curl -s -X POST "${BASE_URL}/api/bill/${BILL_ID}/endorse" \
        -H "Authorization: Bearer $COMPANY_A_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "endorseeAddress": "0x99999999999999999999999999999999999999",
            "endorsementType": "NORMAL",
            "remark": "测试背书给不存在的地址"
        }')

    CODE=$(echo $RESPONSE | jq -r '.code')
    if [ "$CODE" != "200" ]; then
        print_success "正确拒绝背书给不存在的地址"
        echo "  - 错误信息: $(echo $RESPONSE | jq -r '.message')"
    else
        print_error "未能正确拒绝背书给不存在的地址"
    fi
}

###############################################################################
# 统计报告
###############################################################################

print_summary() {
    print_section "测试总结"

    echo "背书功能测试完成"
    echo ""
    echo "已测试的功能:"
    echo "  ✓ 票据背书转让"
    echo "  ✓ 查询背书历史（数据库）"
    echo "  ✓ 查询背书历史（区块链）"
    echo "  ✓ 验证数据完整性"
    echo "  ✓ 错误处理"
    echo ""
    echo "注意事项:"
    echo "  - 确保区块链服务正常运行"
    echo "  - 确保测试票据已创建"
    echo "  - 确保测试企业已注册"
    echo "  - 重复背书需要前一个被背书人登录"
}

###############################################################################
# 主函数
###############################################################################

main() {
    echo "=========================================="
    echo "  票据背书转让功能测试"
    echo "=========================================="

    # 检查依赖
    if ! command -v jq &> /dev/null; then
        echo "错误: 需要安装 jq 工具"
        echo "安装命令: sudo apt-get install jq"
        exit 1
    fi

    if ! command -v curl &> /dev/null; then
        echo "错误: 需要安装 curl 工具"
        echo "安装命令: sudo apt-get install curl"
        exit 1
    fi

    # 执行测试
    setup || exit 1

    # 可根据需要选择执行的测试用例
    test_case_1
    test_case_2
    test_case_3
    test_case_4
    test_case_5
    test_case_6

    print_summary
}

# 执行主函数
main "$@"
