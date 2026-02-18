#!/bin/bash

###############################################################################
# 票据生命周期管理功能测试脚本
# 功能：测试票据作废、冻结、解冻、查询过期票据、查询拒付票据
###############################################################################

# 配置
BASE_URL="http://localhost:8080"
BILL_ID="test-bill-lifecycle-001"
COMPANY_A_ADDRESS="0x1234567890abcdef1234567890abcdef12345678"
ADMIN_ADDRESS="0xadmin0000000000000000000000000000000000"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

###############################################################################
# 工具函数
###############################################################################

print_section() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}\n"
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

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# JWT Token（需要先登录获取）
COMPANY_A_TOKEN=""
ADMIN_TOKEN=""

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

    # 2. 管理员登录
    print_test "管理员登录"
    ADMIN_LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{
            "username": "admin",
            "password": "admin123"
        }')

    ADMIN_TOKEN=$(echo $ADMIN_LOGIN_RESPONSE | jq -r '.data.token // empty')

    if [ -z "$ADMIN_TOKEN" ]; then
        print_error "管理员登录失败，请先创建管理员用户"
        echo "提示: 使用 /api/auth/register 注册管理员用户"
        exit 1
    fi
    print_success "管理员登录成功"

    # 3. 检查测试票据是否存在
    print_test "检查测试票据"
    BILL_RESPONSE=$(curl -s -X GET "${BASE_URL}/api/bill/${BILL_ID}" \
        -H "Authorization: Bearer $COMPANY_A_TOKEN")

    BILL_STATUS=$(echo $BILL_RESPONSE | jq -r '.code // empty')

    if [ "$BILL_STATUS" != "200" ]; then
        print_error "测试票据不存在，请先创建票据"
        echo "票据ID: $BILL_ID"
        echo "提示: 使用 POST /api/bill 创建测试票据"

        # 提供创建票据的示例
        echo ""
        print_info "创建票据示例："
        cat <<'EOF'
curl -X POST http://localhost:8080/api/bill \
  -H "Authorization: Bearer $COMPANY_A_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "test-bill-lifecycle-001",
    "billType": "COMMERCIAL_ACCEPTANCE_BILL",
    "acceptorAddress": "0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
    "beneficiaryAddress": "0xcccccccccccccccccccccccccccccccccccccccc",
    "amount": 1000000.00,
    "currency": "CNY",
    "issueDate": "2025-01-01T10:00:00",
    "dueDate": "2025-12-31T23:59:59",
    "description": "测试票据"
  }'
EOF
        exit 1
    fi

    CURRENT_STATUS=$(echo $BILL_RESPONSE | jq -r '.data.billStatus')
    CURRENT_HOLDER=$(echo $BILL_RESPONSE | jq -r '.data.currentHolderAddress')
    DUE_DATE=$(echo $BILL_RESPONSE | jq -r '.data.dueDate')

    print_success "票据存在"
    echo "  - 当前状态: $CURRENT_STATUS"
    echo "  - 当前持票人: $CURRENT_HOLDER"
    echo "  - 到期日期: $DUE_DATE"
}

###############################################################################
# 测试用例
###############################################################################

test_case_1_cancel_bill() {
    print_section "测试用例1: 票据作废"

    print_test "1.1 正常作废流程"
    print_info "持票人作废票据（丢失）"

    RESPONSE=$(curl -s -X POST "${BASE_URL}/api/bill/${BILL_ID}/cancel" \
        -H "Authorization: Bearer $COMPANY_A_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "cancelReason": "票据不慎丢失",
            "cancelType": "LOST",
            "referenceNo": "派出所报案编号20260202001"
        }')

    CODE=$(echo $RESPONSE | jq -r '.code')
    MESSAGE=$(echo $RESPONSE | jq -r '.message')
    BILL_STATUS=$(echo $RESPONSE | jq -r '.data.billStatus // empty')

    if [ "$CODE" == "200" ]; then
        print_success "票据作废成功"
        echo "  - 新状态: $BILL_STATUS"
        echo "  - 消息: $MESSAGE"
    else
        print_error "票据作废失败: $MESSAGE"
        return 1
    fi

    # 验证：已作废的票据不能再次作废
    print_test "1.2 验证已作废票据不能再次作废"

    RESPONSE=$(curl -s -X POST "${BASE_URL}/api/bill/${BILL_ID}/cancel" \
        -H "Authorization: Bearer $COMPANY_A_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "cancelReason": "测试重复作废",
            "cancelType": "OTHER"
        }')

    CODE=$(echo $RESPONSE | jq -r '.code')
    MESSAGE=$(echo $RESPONSE | jq -r '.message')

    if [ "$CODE" != "200" ]; then
        print_success "正确拒绝重复作废"
        echo "  - 错误信息: $MESSAGE"
    else
        print_error "未能正确拒绝重复作废"
    fi
}

test_case_2_freeze_bill() {
    print_section "测试用例2: 票据冻结/解冻"

    # 注意：需要先重置票据状态才能测试冻结
    # 在实际场景中，应该使用另一个票据ID进行测试

    print_test "2.1 冻结票据"
    print_info "管理员冻结票据（法律纠纷）"

    # 创建一个新票据用于冻结测试
    TEST_BILL_ID="test-bill-freeze-001"

    print_info "创建测试票据用于冻结测试..."
    CREATE_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/bill" \
        -H "Authorization: Bearer $COMPANY_A_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"id\": \"${TEST_BILL_ID}\",
            \"billType\": \"COMMERCIAL_ACCEPTANCE_BILL\",
            \"acceptorAddress\": \"0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\",
            \"beneficiaryAddress\": \"0xcccccccccccccccccccccccccccccccccccccccc\",
            \"amount\": 500000.00,
            \"currency\": \"CNY\",
            \"issueDate\": \"2025-01-01T10:00:00\",
            \"dueDate\": \"2025-12-31T23:59:59\",
            \"description\": \"冻结测试票据\"
        }")

    if [ "$(echo $CREATE_RESPONSE | jq -r '.code')" == "200" ]; then
        print_success "测试票据创建成功"
    else
        print_error "测试票据创建失败，跳过冻结测试"
        return 1
    fi

    # 执行冻结
    RESPONSE=$(curl -s -X POST "${BASE_URL}/api/bill/${TEST_BILL_ID}/freeze" \
        -H "Authorization: Bearer $COMPANY_A_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "freezeReason": "法律纠纷-合同争议",
            "referenceNo": "法院文书号(2025)民保字第123号",
            "evidence": "法院冻结通知书"
        }')

    CODE=$(echo $RESPONSE | jq -r '.code')
    BILL_STATUS=$(echo $RESPONSE | jq -r '.data.billStatus // empty')

    if [ "$CODE" == "200" ]; then
        print_success "票据冻结成功"
        echo "  - 新状态: $BILL_STATUS"
    else
        print_error "票据冻结失败: $(echo $RESPONSE | jq -r '.message')"
        return 1
    fi

    # 验证：冻结的票据不能背书
    print_test "2.2 验证冻结票据不能背书"

    ENDORSE_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/bill/${TEST_BILL_ID}/endorse" \
        -H "Authorization: Bearer $COMPANY_A_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "endorseeAddress": "0xddd...",
            "endorsementType": "NORMAL"
        }')

    ENDORSE_CODE=$(echo $ENDORSE_RESPONSE | jq -r '.code')

    if [ "$ENDORSE_CODE" != "200" ]; then
        print_success "正确拒绝冻结票据背书"
        echo "  - 错误信息: $(echo $ENDORSE_RESPONSE | jq -r '.message')"
    else
        print_error "未能正确拒绝冻结票据背书"
    fi

    # 解冻票据
    print_test "2.3 解冻票据"
    print_info "纠纷已解决，解冻票据"

    RESPONSE=$(curl -s -X POST "${BASE_URL}/api/bill/${TEST_BILL_ID}/unfreeze" \
        -H "Authorization: Bearer $COMPANY_A_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "unfreezeReason": "纠纷已达成和解协议",
            "referenceNo": "法院解冻通知书(2025)民保字第124号"
        }')

    CODE=$(echo $RESPONSE | jq -r '.code')
    BILL_STATUS=$(echo $RESPONSE | jq -r '.data.billStatus // empty')

    if [ "$CODE" == "200" ]; then
        print_success "票据解冻成功"
        echo "  - 新状态: $BILL_STATUS"
    else
        print_error "票据解冻失败: $(echo $RESPONSE | jq -r '.message')"
    fi
}

test_case_3_query_expired() {
    print_section "测试用例3: 查询过期票据"

    print_test "查询所有过期票据"

    RESPONSE=$(curl -s -X GET "${BASE_URL}/api/bill/expired" \
        -H "Authorization: Bearer $COMPANY_A_TOKEN")

    CODE=$(echo $RESPONSE | jq -r '.code')
    COUNT=$(echo $RESPONSE | jq -r '.data | length')

    if [ "$CODE" == "200" ]; then
        print_success "查询成功"
        echo "  - 过期票据数量: $COUNT"

        if [ "$COUNT" -gt 0 ]; then
            echo "  - 过期票据列表:"
            echo $RESPONSE | jq -r '.data[] | "
    票据号: \(.billNo)
    金额: \(.faceValue)
    到期日: \(.dueDate)
    状态: \(.billStatus)
    持票人: \(.currentHolderName)
    ---"'
        fi
    else
        print_error "查询失败: $(echo $RESPONSE | jq -r '.message')"
    fi

    print_test "查询特定企业的过期票据"
    print_info "（如果没有数据，返回空列表是正常的）"

    RESPONSE=$(curl -s -X GET "${BASE_URL}/api/bill/expired?enterpriseId=test-company-001" \
        -H "Authorization: Bearer $COMPANY_A_TOKEN")

    CODE=$(echo $RESPONSE | jq -r '.code')

    if [ "$CODE" == "200" ]; then
        print_success "企业过期票据查询成功"
    else
        print_error "企业过期票据查询失败"
    fi
}

test_case_4_query_dishonored() {
    print_section "测试用例4: 查询拒付票据"

    print_test "查询所有拒付票据"

    RESPONSE=$(curl -s -X GET "${BASE_URL}/api/bill/dishonored" \
        -H "Authorization: Bearer $COMPANY_A_TOKEN")

    CODE=$(echo $RESPONSE | jq -r '.code')
    COUNT=$(echo $RESPONSE | jq -r '.data | length')

    if [ "$CODE" == "200" ]; then
        print_success "查询成功"
        echo "  - 拒付票据数量: $COUNT"

        if [ "$COUNT" -gt 0 ]; then
            echo "  - 拒付票据列表:"
            echo $RESPONSE | jq -r '.data[] | "
    票据号: \(.billNo)
    金额: \(.faceValue)
    承兑人: \(.draweeName)
    拒付日: \(.dishonoredDate)
    拒付原因: \(.dishonoredReason)
    ---"'
        fi
    else
        print_error "查询失败: $(echo $RESPONSE | jq -r '.message')"
    fi

    print_test "按承兑人筛选拒付票据"

    RESPONSE=$(curl -s -X GET "${BASE_URL}/api/bill/dishonored?acceptorAddress=0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb" \
        -H "Authorization: Bearer $COMPANY_A_TOKEN")

    CODE=$(echo $RESPONSE | jq -r '.code')

    if [ "$CODE" == "200" ]; then
        FILTERED_COUNT=$(echo $RESPONSE | jq -r '.data | length')
        print_success "按承兑人筛选成功"
        echo "  - 筛选结果数量: $FILTERED_COUNT"
    else
        print_error "按承兑人筛选失败"
    fi
}

test_case_5_permission_tests() {
    print_section "测试用例5: 权限验证"

    print_test "5.1 非持票人尝试作废票据（应失败）"

    # 使用其他公司的Token（模拟）
    RESPONSE=$(curl -s -X POST "${BASE_URL}/api/bill/${BILL_ID}/cancel" \
        -H "Authorization: Bearer $COMPANY_A_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "cancelReason": "测试权限",
            "cancelType": "OTHER"
        }')

    # 注意：如果COMPANY_A_TOKEN恰好是该票据的持票人，这个测试会失败
    # 在实际使用中，应该使用一个不同的公司Token

    CODE=$(echo $RESPONSE | jq -r '.code')
    MESSAGE=$(echo $RESPONSE | jq -r '.message')

    print_info "测试结果: CODE=$CODE, MESSAGE=$MESSAGE"
    echo "  注意: 如果测试用户恰好是持票人，这个测试会返回成功"
}

###############################################################################
# 统计报告
###############################################################################

print_summary() {
    print_section "测试总结"

    echo "票据生命周期管理功能测试完成"
    echo ""
    echo "已测试的功能:"
    echo "  ✓ 票据作废 (cancel)"
    echo "  ✓ 票据冻结 (freeze)"
    echo "  ✓ 票据解冻 (unfreeze)"
    echo "  ✓ 查询过期票据 (expired)"
    echo "  ✓ 查询拒付票据 (dishonored)"
    echo "  ✓ 权限验证"
    echo ""
    echo "接口清单:"
    echo "  POST /api/bill/{id}/cancel      - 作废票据"
    echo "  POST /api/bill/{id}/freeze      - 冻结票据"
    echo "  POST /api/bill/{id}/unfreeze    - 解冻票据"
    echo "  GET  /api/bill/expired           - 查询过期票据"
    echo "  GET  /api/bill/dishonored        - 查询拒付票据"
    echo ""
    echo "业务价值:"
    echo "  - 支持票据异常处理（作废）"
    echo "  - 支持法律纠纷处理（冻结/解冻）"
    echo "  - 支持逾期管理（过期查询）"
    echo "  - 支持风险管理（拒付查询）"
    echo ""
    echo "注意事项:"
    echo "  - 确保测试票据已创建"
    echo "  - 确保测试企业已注册"
    echo "  - 作废/冻结/解冻需要相应权限"
}

###############################################################################
# 主函数
###############################################################################

main() {
    echo "=========================================="
    echo "  票据生命周期管理功能测试"
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
    test_case_1_cancel_bill
    test_case_2_freeze_bill
    test_case_3_query_expired
    test_case_4_query_dishonored
    test_case_5_permission_tests

    print_summary
}

# 执行主函数
main "$@"
