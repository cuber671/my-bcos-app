package com.fisco.app.util;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * 统一社会信用代码验证工具
 *
 * 国家标准：GB 32100-2015《法人和其他组织统一社会信用代码编码规则》
 *
 * 代码结构（18位）：
 * - 第1位：登记管理部门代码（1-机构编制，5-民政，9-工商，Y-其他）
 * - 第2位：机构类别代码
 * - 第3-8位：登记管理机关行政区划码（6位数字）
 * - 第9-17位：主体识别码（9位）
 * - 第18位：校验码
 */
@Slf4j
public class CreditCodeValidator {

    /**
     * 基础格式：18位，由数字（0-9）和大写字母（A-Z，排除I/O/Z/S/V）组成
     * 排除这些字母的原因：
     * - I/O：容易与数字1/0混淆
     * - Z/S/V：算法中不使用的字母
     */
    private static final Pattern BASIC_PATTERN = Pattern.compile("^[0-9A-HJ-NPQ-TV-Z]{18}$");

    /**
     * 第1位：登记管理部门代码
     */
    private static final String DEPARTMENT_CODES = "159Y";

    /**
     * 加权因子
     */
    private static final int[] WEIGHTS = {
        1, 3, 9, 27, 19, 26, 16, 17, 20, 29, 25, 13, 8, 24, 10, 30, 28
    };

    /**
     * 字符集（31个字符，用于MOD 31计算）
     * 排除：I, O, Z, S, V
     */
    private static final String CODE_CHARS = "0123456789ABCDEFGHJKLMNPQRTUWXY";

    /**
     * 完整验证统一社会信用代码
     *
     * @param creditCode 统一社会信用代码
     * @return 验证结果对象
     */
    public static ValidationResult validate(String creditCode) {
        ValidationResult result = new ValidationResult();
        result.setCreditCode(creditCode);

        // 1. 基本检查
        if (creditCode == null || creditCode.isEmpty()) {
            result.setValid(false);
            result.setError("统一社会信用代码不能为空");
            return result;
        }

        // 2. 长度检查
        if (creditCode.length() != 18) {
            result.setValid(false);
            result.setError("统一社会信用代码必须是18位，当前为" + creditCode.length() + "位");
            return result;
        }

        // 3. 字符集检查
        if (!BASIC_PATTERN.matcher(creditCode).matches()) {
            result.setValid(false);
            result.setError("统一社会信用代码只能包含数字和大写字母（排除I/O/Z/S/V），且不能包含小写字母");
            return result;
        }

        // 4. 第1位检查：登记管理部门代码
        char firstChar = creditCode.charAt(0);
        if (DEPARTMENT_CODES.indexOf(firstChar) == -1) {
            result.setValid(false);
            result.setError("第1位必须是1/5/9/Y（登记管理部门代码），当前为：" + firstChar);
            return result;
        }

        // 5. 第3-8位检查：行政区划码必须是数字
        String districtCode = creditCode.substring(2, 8);
        if (!districtCode.matches("\\d{6}")) {
            result.setValid(false);
            result.setError("第3-8位必须是6位数字（行政区划码），当前为：" + districtCode);
            return result;
        }

        // 6. 校验码检查
        if (!validateChecksum(creditCode)) {
            result.setValid(false);
            result.setError("统一社会信用代码校验码不正确");
            return result;
        }

        // 所有检查通过
        result.setValid(true);
        result.setError(null);

        // 解析详细信息
        result.setDepartmentCode(firstChar);
        result.setDepartmentName(getDepartmentName(firstChar));
        result.setInstitutionCategory(getInstitutionCategory(firstChar, creditCode.charAt(1)));
        result.setDistrictCode(districtCode);

        return result;
    }

    /**
     * 快速验证：只检查基本格式和校验码
     *
     * @param creditCode 统一社会信用代码
     * @return 是否有效
     */
    public static boolean isValid(String creditCode) {
        return validate(creditCode).isValid();
    }

    /**
     * 验证校验码
     *
     * @param creditCode 统一社会信用代码
     * @return 校验码是否正确
     */
    private static boolean validateChecksum(String creditCode) {
        int sum = 0;

        try {
            // 计算前17位的加权和
            for (int i = 0; i < 17; i++) {
                char c = creditCode.charAt(i);
                int index = CODE_CHARS.indexOf(c);
                if (index == -1) {
                    log.warn("无效的统一社会信用代码字符：{}", c);
                    return false;
                }
                sum += index * WEIGHTS[i];
            }

            // 计算校验码
            int remainder = sum % 31;
            char calculatedCheckChar = CODE_CHARS.charAt(remainder);
            char actualCheckChar = creditCode.charAt(17);

            boolean valid = calculatedCheckChar == actualCheckChar;

            if (!valid) {
                log.warn("校验码不匹配：期望={}, 实际={}",
                    calculatedCheckChar, actualCheckChar);
            }

            return valid;

        } catch (Exception e) {
            log.error("校验码验证失败", e);
            return false;
        }
    }

    /**
     * 获取登记管理部门名称
     *
     * @param code 第1位代码
     * @return 部门名称
     */
    private static String getDepartmentName(char code) {
        switch (code) {
            case '1':
                return "机构编制";
            case '5':
                return "民政";
            case '9':
                return "市场监督管理";
            case 'Y':
                return "其他";
            default:
                return "未知";
        }
    }

    /**
     * 获取机构类别
     *
     * @param departmentCode 第1位（登记管理部门代码）
     * @param categoryCode 第2位（机构类别代码）
     * @return 机构类别名称
     */
    private static String getInstitutionCategory(char departmentCode, char categoryCode) {
        if (departmentCode == '9') {
            // 市场监督管理部门
            switch (categoryCode) {
                case '1':
                    return "企业";
                case '2':
                    return "个体工商户";
                case '3':
                    return "农民专业合作社";
                default:
                    return "未知类别";
            }
        } else if (departmentCode == '1') {
            // 机构编制
            switch (categoryCode) {
                case '1':
                    return "机关";
                case '2':
                    return "事业单位";
                case '3':
                    return "中央编办直接管理机构编制的群众团体";
                default:
                    return "其他";
            }
        } else if (departmentCode == '5') {
            // 民政部门
            switch (categoryCode) {
                case '1':
                    return "社会团体";
                case '2':
                    return "民办非企业单位";
                case '3':
                    return "基金会";
                default:
                    return "其他";
            }
        }
        return "未知";
    }

    /**
     * 生成测试用的统一社会信用代码（仅用于测试）
     * 注意：这不是真实的统一社会信用代码，仅用于开发测试
     *
     * @param departmentCode 登记管理部门代码（1/5/9/Y）
     * @param categoryCode 机构类别代码
     * @param districtCode 行政区划码（6位数字）
     * @param organizationId 主体识别码（9位）
     * @return 生成的统一社会信用代码
     */
    public static String generateForTest(char departmentCode, char categoryCode,
                                         String districtCode, String organizationId) {
        // 组合前17位
        StringBuilder sb = new StringBuilder();
        sb.append(departmentCode);
        sb.append(categoryCode);
        sb.append(districtCode);
        sb.append(organizationId.substring(0, 9)); // 确保是9位

        String code17 = sb.toString();

        // 计算校验码
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            char c = code17.charAt(i);
            int index = CODE_CHARS.indexOf(c);
            sum += index * WEIGHTS[i];
        }

        int remainder = sum % 31;
        char checkChar = CODE_CHARS.charAt(remainder);

        return code17 + checkChar;
    }

    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private String creditCode;
        private boolean valid;
        private String error;
        private Character departmentCode;
        private String departmentName;
        private String institutionCategory;
        private String districtCode;

        public String getCreditCode() {
            return creditCode;
        }

        public void setCreditCode(String creditCode) {
            this.creditCode = creditCode;
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public Character getDepartmentCode() {
            return departmentCode;
        }

        public void setDepartmentCode(Character departmentCode) {
            this.departmentCode = departmentCode;
        }

        public String getDepartmentName() {
            return departmentName;
        }

        public void setDepartmentName(String departmentName) {
            this.departmentName = departmentName;
        }

        public String getInstitutionCategory() {
            return institutionCategory;
        }

        public void setInstitutionCategory(String institutionCategory) {
            this.institutionCategory = institutionCategory;
        }

        public String getDistrictCode() {
            return districtCode;
        }

        public void setDistrictCode(String districtCode) {
            this.districtCode = districtCode;
        }

        @Override
        public String toString() {
            return "ValidationResult{" +
                    "creditCode='" + creditCode + '\'' +
                    ", valid=" + valid +
                    ", error='" + error + '\'' +
                    ", departmentCode=" + departmentCode +
                    ", departmentName='" + departmentName + '\'' +
                    ", institutionCategory='" + institutionCategory + '\'' +
                    ", districtCode='" + districtCode + '\'' +
                    '}';
        }
    }
}
