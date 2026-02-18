#!/bin/bash

###############################################################################
# 票据池管理功能测试脚本
#
# 功能：
# 1. 查询票据池
# 2. 查询可投资票据
# 3. 票据投资
# 4. 查询投资记录
#
# 使用方法：
# ./scripts/test_bill_pool.sh <server_url> <token> <enterprise_id>
#
# 示例：
# ./scripts/test_bill_pool.sh http://localhost:8080 token123 bank-uuid-001
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
INSTITUTION_ID="${3:-test-institution-001}"

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
    local headers="-H 'Content-Type: application/json' -H 'Authorization: Bearer ${TOKEN}' ${extra_headers}"

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

###############################################################################
# 主测试流程
###############################################################################

main() {
    log_info "票据池管理功能测试"
    log_info "服务器地址: $SERVER_URL"
    log_info "金融机构ID: $INSTITUTION_ID"
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
    # 测试1: 查询票据池
    ###########################################################################
    print_test_title "测试1: 查询票据池"

    log_info "查询票据池（无筛选条件）..."
    POOL_RESPONSE=$(http_request "GET" "/api/bill/pool?page=0&size=10" "" "")
    echo "票据池响应: $POOL_RESPONSE" | jq '.'

    if echo "$POOL_RESPONSE" | jq -e '.code == 200'; then
        log_success "查询票据池成功"

        # 显示票据数量
        TOTAL=$(echo "$POOL_RESPONSE" | jq -r '.data.totalElements // 0')
        log_info "票据池票据总数: $TOTAL"
    else
        log_error "查询票据池失败"
    fi

    wait_for_input

    ###########################################################################
    # 测试2: 带筛选条件查询票据池
    ###########################################################################
    print_test_title "测试2: 带筛选条件查询票据池"

    log_info "筛选条件：银行承兑汇票，面值50-200万，剩余30-180天"
    FILTERED_POOL_RESPONSE=$(http_request "GET" \
        "/api/bill/pool?billType=BANK_ACCEPTANCE_BILL&minAmount=500000&maxAmount=2000000&minRemainingDays=30&maxRemainingDays=180&page=0&size=5" "" "")
    echo "筛选结果: $FILTERED_POOL_RESPONSE" | jq '.'

    if echo "$FILTERED_POOL_RESPONSE" | jq -e '.code == 200'; then
        log_success "筛选查询成功"
    else
        log_error "筛选查询失败"
    fi

    wait_for_input

    ###########################################################################
    # 测试3: 查询可投资票据
    ###########################################################################
    print_test_title "测试3: 查询可投资票据"

    log_info "查询适合机构的可投资票据..."
    AVAILABLE_RESPONSE=$(http_request "GET" \
        "/api/bill/pool/available?institutionId=${INSTITUTION_ID}&billType=BANK_ACCEPTANCE_BILL&minAmount=100000" "" "")
    echo "可投资票据: $AVAILABLE_RESPONSE" | jq '.'

    if echo "$AVAILABLE_RESPONSE" | jq -e '.code == 200'; then
        log_success "查询可投资票据成功"

        # 显示可投资票据数量
        COUNT=$(echo "$AVAILABLE_RESPONSE" | jq -r '.data | length // 0')
        log_info "可投资票据数量: $COUNT"
    else
        log_warning "查询可投资票据失败（可能是因为没有票据或权限问题）"
    fi

    wait_for_input

    ###########################################################################
    # 测试4: 票据投资
    ###########################################################################
    print_test_title "测试4: 票据投资"

    # 注意：这需要一个实际存在的票据ID
    TEST_BILL_ID="test-bill-investment-$(date +%s)"

    log_warning "此测试需要一个有效的票据ID"
    log_info "使用测试票据ID: $TEST_BILL_ID"

    INVEST_REQUEST=$(cat <<EOF
{
  "investAmount": 950000.00,
  "investRate": 5.5,
  "investDate": "2026-02-03T14:00:00",
  "investmentNotes": "看好该票据，决定投资"
}
EOF
)

    echo "投资请求: $INVEST_REQUEST" | jq '.'

    INVEST_RESPONSE=$(http_request "POST" "/api/bill/pool/${TEST_BILL_ID}/invest" "$INVEST_REQUEST" \
        "-H 'X-User-Address: 0x1234567890abcdef'")

    echo "投资响应: $INVEST_RESPONSE" | jq '.'

    if echo "$INVEST_RESPONSE" | jq -e '.code == 200'; then
        log_success "票据投资成功"

        INVESTMENT_ID=$(echo "$INVEST_RESPONSE" | jq -r '.data.investmentId // empty')
        log_info "投资记录ID: $INVESTMENT_ID"
    else
        log_warning "票据投资失败（可能是因为测试票据不存在）"
        log_info "提示：请使用真实的票据ID进行测试"
    fi

    wait_for_input

    ###########################################################################
    # 测试5: 查询投资记录
    ###########################################################################
    print_test_title "测试5: 查询投资记录"

    log_info "查询投资记录..."
    RECORDS_RESPONSE=$(http_request "GET" \
        "/api/bill/pool/investments?institutionId=${INSTITUTION_ID}" "" "")
    echo "投资记录: $RECORDS_RESPONSE" | jq '.'

    if echo "$RECORDS_RESPONSE" | jq -e '.code == 200'; then
        log_success "查询投资记录成功"

        # 显示投资记录数量
        COUNT=$(echo "$RECORDS_RESPONSE" | jq -r '.data | length // 0')
        log_info "投资记录数量: $COUNT"

        # 显示第一笔投资（如果有）
        if [ "$COUNT" -gt 0 ]; then
            FIRST_INVEST=$(echo "$RECORDS_RESPONSE" | jq -r '.data[0]')
            log_info "第一笔投资: $FIRST_INVEST" | jq '.'
        fi
    else
        log_warning "查询投资记录失败"
    fi

    wait_for_input

    ###########################################################################
    # 测试6: 查询票据池统计信息
    ###########################################################################
    print_test_title "测试6: 查询票据池统计信息"

    log_info "查询票据池统计..."
    STATS_RESPONSE=$(http_request "GET" "/api/bill/pool/statistics" "" "")
    echo "统计信息: $STATS_RESPONSE" | jq '.'

    if echo "$STATS_RESPONSE" | jq -e '.code == 200'; then
        log_success "查询统计信息成功"
    else
        log_warning "查询统计信息失败"
    fi

    ###########################################################################
    # 测试总结
    ###########################################################################
    print_test_title "测试总结"

    log_info "票据池管理功能测试完成"
    echo ""
    echo "测试项目："
    echo "  ✅ 查询票据池（无筛选）"
    echo "  ✅ 查询票据池（带筛选条件）"
    echo "  ✅ 查询可投资票据"
    echo "  ✅ 票据投资（需要真实票据ID）"
    echo "  ✅ 查询投资记录"
    echo "  ✅ 查询统计信息"
    echo ""
    log_info "注意事项："
    echo "  1. 某些测试可能失败是因为测试数据不存在"
    echo "  2. 票据投资需要真实的票据ID"
    echo "  3. 建议先创建测试票据后再进行完整测试"
    echo "  4. 确保金融机构有正确的权限"
    echo ""
    log_success "测试脚本执行完毕"
}

# 等待用户输入
wait_for_input() {
    echo ""
    read -p "按 Enter 键继续下一个测试..."
}

# 执行主函数
main "$@"
