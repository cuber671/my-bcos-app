#!/bin/bash

# VSCode 导入问题一键修复脚本
# 适用于：VSCode + Java + Maven 项目

echo "========================================="
echo "VSCode Java 项目导入修复工具"
echo "========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 步骤1：清理Maven
echo -e "${YELLOW}步骤 1/6: 清理Maven项目...${NC}"
mvn clean
echo ""

# 步骤2：删除重复文件
echo -e "${YELLOW}步骤 2/6: 删除重复的实体类...${NC}"
DUPLICATES=(
    "src/main/java/com/fisco/app/entity/CreditLimit.java"
    "src/main/java/com/fisco/app/entity/CreditLimitAdjustRequest.java"
    "src/main/java/com/fisco/app/entity/CreditLimitUsage.java"
    "src/main/java/com/fisco/app/entity/CreditLimitWarning.java"
    "src/main/java/com/fisco/app/entity/EnterpriseAuditLog.java"
)

for file in "${DUPLICATES[@]}"; do
    if [ -f "$file" ]; then
        rm "$file"
        echo -e "${GREEN}✓${NC} 已删除: $file"
    fi
done
echo ""

# 步骤3：运行Python修复脚本
echo -e "${YELLOW}步骤 3/6: 运行导入修复脚本...${NC}"

if [ -f "fix_missing_imports.py" ]; then
    echo -e "${GREEN}运行: fix_missing_imports.py${NC}"
    python3 fix_missing_imports.py > /dev/null 2>&1
fi

if [ -f "fix_all_imports_comprehensive.py" ]; then
    echo -e "${GREEN}运行: fix_all_imports_comprehensive.py${NC}"
    python3 fix_all_imports_comprehensive.py > /dev/null 2>&1
fi

if [ -f "fix_final_issues.py" ]; then
    echo -e "${GREEN}运行: fix_final_issues.py${NC}"
    python3 fix_final_issues.py > /dev/null 2>&1
fi

echo ""

# 步骤4：编译验证
echo -e "${YELLOW}步骤 4/6: 编译项目并检查错误...${NC}"
ERROR_COUNT=$(mvn compile -DskipTests 2>&1 | grep -c "ERROR")
echo -e "当前错误数: ${RED}$ERROR_COUNT${NC}"
echo ""

# 步骤5：VSCode设置建议
echo -e "${YELLOW}步骤 5/6: VSCode设置建议${NC}"
echo "请在VSCode中执行以下操作："
echo ""
echo "1. 按 Ctrl+Shift+P (Mac: Cmd+Shift+P)"
echo "2. 输入并选择: 'Java: Clean Java Language Server Workspace'"
echo "3. 点击 'Restart and delete'"
echo "4. 等待VSCode重新索引项目（底部状态栏显示 'Building workspace'）"
echo ""

# 步骤6：自动导入设置
echo -e "${YELLOW}步骤 6/6: 启用自动导入${NC}"
echo "在VSCode settings.json中添加："
echo ""
cat <<'EOF'
{
    "java.saveActions.organizeImports": true,
    "java.format.enabled": true,
    "java.completion.importOrder": [
        "java",
        "javax",
        "org",
        "com"
    ]
}
EOF
echo ""

echo "========================================="
echo "修复完成！"
echo "========================================="
echo ""
echo "接下来："
echo "1. 在VSCode中打开任意Java文件"
echo "2. 按 Ctrl+Shift+P"
echo "3. 输入 'Organize Imports' 并选择 'Organize Imports in All Java Files'"
echo ""
echo "或者："
echo "1. 按 Ctrl+S 保存文件时，VSCode会自动整理导入"
echo ""
