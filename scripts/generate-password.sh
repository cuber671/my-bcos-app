#!/bin/bash

###############################################################################
# 密码生成工具脚本
# 用途: 快速生成 BCrypt 加密密码
# 使用: ./generate-password.sh [命令] [参数]
###############################################################################

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目根目录
PROJECT_DIR="/home/llm_rca/fisco/my-bcos-app"

# 打印帮助信息
print_help() {
    echo -e "${BLUE}╔═══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║           密码生成工具 - Password Generator v1.0              ║${NC}"
    echo -e "${BLUE}╚═══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${GREEN}📖 使用方法:${NC}"
    echo "  $0 <command> [arguments]"
    echo ""
    echo -e "${GREEN}📋 命令列表:${NC}"
    echo "  gen, g <password>       - 生成 BCrypt 加密密码"
    echo "  verify, v <raw> <enc>   - 验证明文密码是否匹配加密密码"
    echo "  random, r [length=12]   - 生成随机密码并加密"
    echo "  help, h                - 显示此帮助信息"
    echo ""
    echo -e "${GREEN}💡 示例:${NC}"
    echo "  # 生成加密密码"
    echo "  $0 gen \"MyP@ssw0rd\""
    echo ""
    echo "  # 验证密码"
    echo "  $0 v \"MyP@ssw0rd\" \"\$2a\$12\$...\""
    echo ""
    echo "  # 生成12位随机密码"
    echo "  $0 random"
    echo ""
    echo "  # 生成16位随机密码"
    echo "  $0 r 16"
    echo ""
}

# 检查 Maven 是否安装
check_maven() {
    if ! command -v mvn &> /dev/null; then
        echo -e "${RED}❌ 错误: 未找到 Maven，请先安装 Maven${NC}"
        echo "安装命令: sudo apt install maven"
        exit 1
    fi
}

# 检查项目目录
check_project_dir() {
    if [ ! -d "$PROJECT_DIR" ]; then
        echo -e "${RED}❌ 错误: 项目目录不存在: $PROJECT_DIR${NC}"
        exit 1
    fi
}

# 编译项目（如果需要）
compile_project() {
    if [ ! -d "$PROJECT_DIR/target/classes" ]; then
        echo -e "${YELLOW}📦 首次运行，正在编译项目...${NC}"
        cd "$PROJECT_DIR"
        mvn compile -q
        if [ $? -ne 0 ]; then
            echo -e "${RED}❌ 编译失败${NC}"
            exit 1
        fi
        echo -e "${GREEN}✅ 编译成功${NC}"
    fi
}

# 构建 classpath
build_classpath() {
    cd "$PROJECT_DIR"
    CLASSPATH="$PROJECT_DIR/target/classes"

    # 添加 Maven 依赖到 classpath
    if [ -f "$PROJECT_DIR/target/.classpath" ]; then
        # 使用缓存的 classpath
        DEPS=$(cat "$PROJECT_DIR/target/.classpath")
    else
        # 构建 classpath 并缓存
        DEPS=$(mvn dependency:build-classpath -DincludeScope=compile -q -Dmdep.outputFile=/dev/stdout | grep -v '\[')
        echo "$DEPS" > "$PROJECT_DIR/target/.classpath"
    fi

    CLASSPATH="$CLASSPATH:$DEPS"
}

# 执行 Java 命令
run_password_generator() {
    check_maven
    check_project_dir
    compile_project
    build_classpath

    java -cp "$CLASSPATH" com.fisco.app.util.PasswordGenerator "$@"
}

# 主逻辑
if [ $# -eq 0 ]; then
    print_help
    exit 0
fi

COMMAND=$1
shift

case "$COMMAND" in
    gen|generate|g)
        if [ $# -lt 1 ]; then
            echo -e "${RED}❌ 错误: 请提供要加密的密码${NC}"
            echo ""
            echo "用法: $0 gen <password>"
            echo "示例: $0 gen \"MyP@ssw0rd\""
            exit 1
        fi
        run_password_generator generate "$@"
        ;;

    verify|v)
        if [ $# -lt 2 ]; then
            echo -e "${RED}❌ 错误: 请提供明文密码和加密密码${NC}"
            echo ""
            echo "用法: $0 verify <plaintext_password> <encrypted_password>"
            echo "示例: $0 v \"MyP@ssw0rd\" \"\$2a\$12\$...\""
            exit 1
        fi
        run_password_generator verify "$@"
        ;;

    random|r)
        LENGTH=${1:-12}
        run_password_generator random "$LENGTH"
        ;;

    help|h|--help|-h)
        print_help
        ;;

    *)
        echo -e "${RED}❌ 未知命令: $COMMAND${NC}"
        echo ""
        print_help
        exit 1
        ;;
esac
