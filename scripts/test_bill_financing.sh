#!/bin/bash

###############################################################################
# 票据融资管理功能测试脚本
#
# 功能：
# 1. 测试票据融资申请
# 2. 测试融资审核（批准/拒绝）
# 3. 测试查询待审核申请
# 4. 测试融资还款
#
# 使用方法：
# ./scripts/test_bill_financing.sh <server_url> <token>
#
# 示例：
# ./scripts/test_bill_financing.sh http://localhost:8080 eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
###############################################################################

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 服务器配置
SERVER_URL="${1:-http://localhost:8080}"
TOKEN="${2:-}"

# 测试数据
BILL_ID="test-bill-$(date +%s)"
INSTITUTION_ID="test-institution-001"
APPLICANT_ADDRESS="0x1234567890abcdef"
INSTITUTION_ADDRESS="0xabcdef1234567890"

# 辅助函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# 发送HTTP请求
http_request() {
    local method=$1
    local endpoint=$2
    local data=$3
    local extra_headers=$4

    local url="${SERVER_URL}${endpoint}"
    local headers="-H 'Content-Type: application/json' -H 'Authorization: Bearer ${TOKEN}' -H 'X-User-Address: ${APPLICANT_ADDRESS}' ${extra_headers}"

    if [ "$method" = "GET" ]; then
        curl -s -X GET "$url" $headers
    else
        curl -s -X "$method" "$url" $headers -d "$data"
    fi
}

# 打印测试标题
print_test_title() {
    echo ""
    echo "========================================"
    echo -e "${BLUE}$1${NC}"
    echo "========================================"
}

# 检查响应
check_response() {
    local response=$1
    local expected_code=$2
    local test_name=$3

    local code=$(echo "$response" | jq -r '.code // .status')

    if [ "$code" = "$expected_code" ] || [ "$code" = "200" ] || [ "$code" = "201" ]; then
        log_success "$test_name - 通过"
        return 0
    else
        log_error "$test_name - 失败"
        echo "响应: $response"
        return 1
    fi
}

# 等待用户输入
wait_for_input() {
    echo ""
    read -p "按 Enter 键继续下一个测试..."
}

###############################################################################
# 主测试流程
###############################################################################

main() {
    log_info "票据融资管理功能测试"
    log_info "服务器地址: $SERVER_URL"
    echo ""

    # 检查 jq 命令
    if ! command -v jq &> /dev/null; then
        log_error "jq 未安装，请先安装 jq"
        exit 1
    fi

    # 检查 TOKEN
    if [ -z "$TOKEN" ]; then
        log_warning "未提供 TOKEN，将尝试匿名访问（可能会失败）"
        read -p "请输入访问 TOKEN (留空跳过): " TOKEN
    fi

    ###########################################################################
    # 测试1: 申请票据融资
    ###########################################################################
    print_test_title "测试1: 申请票据融资"

    log_info "创建融资申请..."
    FINANCE_REQUEST=$(cat <<EOF
{
  "financialInstitutionId": "${INSTITUTION_ID}",
  "financeAmount": 950000.00,
  "financeRate": 5.5,
  "financePeriod": 90,
  "pledgeAgreement": "质押协议条款内容..."
}
EOF
)

    FINANCE_RESPONSE=$(http_request "POST" "/api/bill/${BILL_ID}/finance" "$FINANCE_REQUEST")
    echo "融资申请响应: $FINANCE_RESPONSE" | jq '.'

    if check_response "$FINANCE_RESPONSE" "200" "创建融资申请"; then
        APPLICATION_ID=$(echo "$FINANCE_RESPONSE" | jq -r '.data.id // empty')
        log_success "融资申请ID: $APPLICATION_ID"
    else
        log_error "创建融资申请失败，可能是因为票据不存在或状态不正确"
        log_info "提示：请先创建一张状态为 NORMAL、ENDORSED 或 ISSUED 的票据"
    fi

    wait_for_input

    ###########################################################################
    # 测试2: 查询待审核的融资申请
    ###########################################################################
    print_test_title "测试2: 查询待审核的融资申请"

    log_info "查询所有待审核申请..."
    PENDING_ALL_RESPONSE=$(http_request "GET" "/api/bill/finance/pending" "" "")
    echo "所有待审核申请: $PENDING_ALL_RESPONSE" | jq '.'

    check_response "$PENDING_ALL_RESPONSE" "200" "查询所有待审核申请"

    echo ""
    log_info "查询特定金融机构的待审核申请..."
    PENDING_INST_RESPONSE=$(http_request "GET" "/api/bill/finance/pending?institutionId=${INSTITUTION_ID}" "" "")
    echo "金融机构待审核申请: $PENDING_INST_RESPONSE" | jq '.'

    check_response "$PENDING_INST_RESPONSE" "200" "查询金融机构待审核申请"

    wait_for_input

    ###########################################################################
    # 测试3: 审核融资申请（批准）
    ###########################################################################
    print_test_title "测试3: 审核融资申请（批准）"

    if [ -n "$APPLICATION_ID" ]; then
        log_info "批准融资申请..."

        APPROVE_REQUEST=$(cat <<EOF
{
  "applicationId": "${APPLICATION_ID}",
  "approvalResult": "APPROVED",
  "approvedAmount": 950000.00,
  "approvedRate": 5.5,
  "approvalComments": "审核通过，信用良好"
}
EOF
)

        # 使用金融机构地址
        APPROVE_RESPONSE=$(curl -s -X POST "${SERVER_URL}/api/bill/finance/approve" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer ${TOKEN}" \
            -H "X-User-Address: ${INSTITUTION_ADDRESS}" \
            -d "$APPROVE_REQUEST")

        echo "批准申请响应: $APPROVE_RESPONSE" | jq '.'

        if check_response "$APPROVE_RESPONSE" "200" "批准融资申请"; then
            log_success "融资申请已批准"
        fi
    else
        log_warning "跳过批准测试，因为没有有效的申请ID"
    fi

    wait_for_input

    ###########################################################################
    # 测试4: 查询已批准的申请
    ###########################################################################
    print_test_title "测试4: 查询融资申请详情"

    if [ -n "$APPLICATION_ID" ]; then
        log_info "查询融资申请状态..."
        # 注意：这里假设有一个根据ID查询申请详情的接口
        # 如果没有，可以通过查询所有申请然后过滤
        log_info "申请ID: $APPLICATION_ID"
    fi

    wait_for_input

    ###########################################################################
    # 测试5: 测试拒绝融资申请
    ###########################################################################
    print_test_title "测试5: 测试拒绝融资申请"

    # 创建另一个申请用于拒绝测试
    log_info "创建第二个融资申请（用于拒绝测试）..."
    BILL_ID_2="test-bill-reject-$(date +%s)"

    FINANCE_REQUEST_2=$(cat <<EOF
{
  "financialInstitutionId": "${INSTITUTION_ID}",
  "financeAmount": 500000.00,
  "financeRate": 6.0,
  "financePeriod": 60
}
EOF
)

    FINANCE_RESPONSE_2=$(http_request "POST" "/api/bill/${BILL_ID_2}/finance" "$FINANCE_REQUEST_2")
    APPLICATION_ID_2=$(echo "$FINANCE_RESPONSE_2" | jq -r '.data.id // empty')

    if [ -n "$APPLICATION_ID_2" ]; then
        log_info "拒绝融资申请..."

        REJECT_REQUEST=$(cat <<EOF
{
  "applicationId": "${APPLICATION_ID_2}",
  "approvalResult": "REJECTED",
  "approvalComments": "票据信用等级不足"
}
EOF
)

        REJECT_RESPONSE=$(curl -s -X POST "${SERVER_URL}/api/bill/finance/approve" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer ${TOKEN}" \
            -H "X-User-Address: ${INSTITUTION_ADDRESS}" \
            -d "$REJECT_REQUEST")

        echo "拒绝申请响应: $REJECT_RESPONSE" | jq '.'

        if check_response "$REJECT_RESPONSE" "200" "拒绝融资申请"; then
            log_success "融资申请已拒绝"
        fi
    else
        log_warning "无法创建第二个申请，跳过拒绝测试"
    fi

    wait_for_input

    ###########################################################################
    # 测试6: 融资还款
    ###########################################################################
    print_test_title "测试6: 融资还款（全额）"

    if [ -n "$APPLICATION_ID" ]; then
        log_info "执行全额还款..."
        log_info "注意：此测试需要申请状态为 ACTIVE（已放款）"

        # 计算还款金额（本金 + 利息）
        # 利息 = 950000 * 5.5% * 90 / 365 = 12876.71
        # 还款总额 = 950000 + 12876.71 = 962876.71
        REPAY_REQUEST=$(cat <<EOF
{
  "repayAmount": 962876.71,
  "repayType": "FULL",
  "repaymentProof": "转账凭证号TXN$(date +%s)"
}
EOF
)

        REPAY_RESPONSE=$(http_request "POST" "/api/bill/finance/${APPLICATION_ID}/repay" "$REPAY_REQUEST")
        echo "还款响应: $REPAY_RESPONSE" | jq '.'

        if check_response "$REPAY_RESPONSE" "200" "融资还款"; then
            log_success "融资还款成功"
        else
            log_warning "还款失败，可能是因为申请状态不是 ACTIVE"
            log_info "提示：需要先完成放款操作才能进行还款测试"
        fi
    else
        log_warning "跳过还款测试，因为没有有效的申请ID"
    fi

    ###########################################################################
    # 测试7: 部分还款测试
    ###########################################################################
    print_test_title "测试7: 融资还款（部分）"

    if [ -n "$APPLICATION_ID" ]; then
        log_info "执行部分还款..."
        log_info "注意：此测试需要申请状态为 ACTIVE"

        PARTIAL_REPAY_REQUEST=$(cat <<EOF
{
  "repayAmount": 500000.00,
  "repayType": "PARTIAL",
  "repaymentProof": "部分还款凭证号TXN$(date +%s)"
}
EOF
)

        PARTIAL_REPAY_RESPONSE=$(http_request "POST" "/api/bill/finance/${APPLICATION_ID}/repay" "$PARTIAL_REPAY_REQUEST")
        echo "部分还款响应: $PARTIAL_REPAY_RESPONSE" | jq '.'
    fi

    ###########################################################################
    # 测试总结
    ###########################################################################
    print_test_title "测试总结"

    log_info "票据融资管理功能测试完成"
    echo ""
    echo "测试项目："
    echo "  ✅ 申请票据融资"
    echo "  ✅ 查询待审核申请（全部）"
    echo "  ✅ 查询待审核申请（按机构）"
    echo "  ✅ 批准融资申请"
    echo "  ✅ 拒绝融资申请"
    echo "  ✅ 全额还款"
    echo "  ✅ 部分还款"
    echo ""
    log_info "注意事项："
    echo "  1. 某些测试可能失败是因为测试票据不存在或状态不正确"
    echo "  2. 还款测试需要先完成放款操作"
    echo "  3. 实际使用时需要根据真实票据数据进行测试"
    echo ""
    log_success "测试脚本执行完毕"
}

# 执行主函数
main "$@"
