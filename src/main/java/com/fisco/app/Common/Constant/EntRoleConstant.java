package com.fisco.app.Common.Constant;

/**
 * 企业角色常量
 *
 * 定义企业业务角色编码
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public class EntRoleConstant {

    /** 核心企业 */
    public static final int CORE_ENTERPRISE = 1;

    /** 现货交易平台 */
    public static final int SPOT_PLATFORM = 2;

    /** 供应商 */
    public static final int SUPPLIER = 3;

    /** 金融机构 */
    public static final int FINANCIAL_INSTITUTION = 6;

    /** 仓储方 */
    public static final int WAREHOUSE = 9;

    /** 物流方 */
    public static final int LOGISTICS = 12;

    /**
     * 判断是否为仓储方
     */
    public static boolean isWarehouse(Integer entRole) {
        return entRole != null && entRole == WAREHOUSE;
    }

    /**
     * 判断是否为金融机构
     */
    public static boolean isFinancialInstitution(Integer entRole) {
        return entRole != null && entRole == FINANCIAL_INSTITUTION;
    }

    /**
     * 判断是否为仓单持有人（核心企业/供应商/现货平台）
     */
    public static boolean isReceiptOwner(Integer entRole) {
        return entRole != null && (entRole == CORE_ENTERPRISE || entRole == SUPPLIER || entRole == SPOT_PLATFORM);
    }

    /**
     * 获取角色名称
     */
    public static String getRoleName(Integer entRole) {
        if (entRole == null) return "未知";
        switch (entRole) {
            case CORE_ENTERPRISE: return "核心企业";
            case SPOT_PLATFORM: return "现货交易平台";
            case SUPPLIER: return "供应商";
            case FINANCIAL_INSTITUTION: return "金融机构";
            case WAREHOUSE: return "仓储方";
            case LOGISTICS: return "物流方";
            default: return "未知";
        }
    }
}
