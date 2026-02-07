// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

/**
 * @title DataPackUtils
 * @dev 数据分类存储工具库 - 哈希指纹生成与验证
 *
 * 核心原则：
 * 1. 上链信息：谁能做（地址）+ 什么时候做（时间）+ 给多少钱（金额）
 * 2. 哈希化信息：长文本描述 + 个人隐私敏感信息
 */
library DataPackUtils {
    // ==================== 上链数据类型定义 ====================

    /**
     * @dev 角色定义（地址 - 谁能做）
     */
    struct Roles {
        address owner;           // 所有者
        address counterparty;    // 交易对手方
        address financier;       // 金融机构
        address operator;        // 操作员
    }

    /**
     * @dev 时间节点（时间 - 什么时候做）
     */
    struct Timeline {
        uint64 createdAt;       // 创建时间
        uint64 verifiedAt;      // 验证时间
        uint64 executedAt;      // 执行时间
        uint64 completedAt;     // 完成时间
        uint64 expiredAt;       // 过期时间
    }

    /**
     * @dev 金额信息（金额 - 给多少钱）
     */
    struct Amounts {
        uint96 totalAmount;     // 总金额
        uint96 pledgedAmount;   // 质押金额
        uint96 financedAmount;  // 融资金额
        uint96 paidAmount;      // 已付金额
        uint96 remainingAmount; // 剩余金额
    }

    // ==================== 链下哈希数据类型 ====================

    /**
     * @dev 隐私数据类别（需要哈希化）
     */
    enum PrivacyDataType {
        PersonalInfo,        // 个人信息（姓名、电话、身份证）
        GoodsDetails,        // 货物详情（名称、规格、描述）
        LocationInfo,        // 位置信息（仓库位置、地址）
        ContractTerms,       // 合同条款（长文本描述）
        CustomDeclaration,   // 报关信息
        BankAccount          // 银行账户信息
    }

    // ==================== 哈希生成函数 ====================

    /**
     * @dev 生成个人信息哈希（隐私保护）
     * @param name 姓名
     * @param phone 电话（可脱敏）
     * @param idNumber 身份证号（不上链）
     * @param email 邮箱
     */
    function hashPersonalInfo(
        string memory name,
        string memory phone,
        string memory idNumber,
        string memory email
    ) internal pure returns (bytes32) {
        return keccak256(abi.encodePacked(
            "PERSONAL_INFO",
            name,
            phone,
            idNumber,
            email
        ));
    }

    /**
     * @dev 生成货物详情哈希（长文本）
     * @param goodsName 货物名称
     * @param goodsSpec 规格型号
     * @param goodsDescription 详细描述
     * @param quantity 数量
     * @param unit 单位
     */
    function hashGoodsDetails(
        string memory goodsName,
        string memory goodsSpec,
        string memory goodsDescription,
        uint256 quantity,
        string memory unit
    ) internal pure returns (bytes32) {
        return keccak256(abi.encodePacked(
            "GOODS_DETAILS",
            goodsName,
            goodsSpec,
            goodsDescription,
            quantity,
            unit
        ));
    }

    /**
     * @dev 生成位置信息哈希
     * @param warehouseName 仓库名称
     * @param warehouseAddress 仓库详细地址
     * @param zoneNumber 库区号
     * @param shelfNumber 货架号
     */
    function hashLocationInfo(
        string memory warehouseName,
        string memory warehouseAddress,
        string memory zoneNumber,
        string memory shelfNumber
    ) internal pure returns (bytes32) {
        return keccak256(abi.encodePacked(
            "LOCATION_INFO",
            warehouseName,
            warehouseAddress,
            zoneNumber,
            shelfNumber
        ));
    }

    /**
     * @dev 生成合同条款哈希
     * @param contractNumber 合同编号
     * @param terms 合同条款（长文本）
     * @param paymentTerms 付款条款
     * @param penaltyTerms 违约条款
     */
    function hashContractTerms(
        string memory contractNumber,
        string memory terms,
        string memory paymentTerms,
        string memory penaltyTerms
    ) internal pure returns (bytes32) {
        return keccak256(abi.encodePacked(
            "CONTRACT_TERMS",
            contractNumber,
            terms,
            paymentTerms,
            penaltyTerms
        ));
    }

    /**
     * @dev 生成银行账户哈希（敏感信息）
     * @param bankName 银行名称
     * @param accountNumber 账号（脱敏）
     * @param accountName 户名
     */
    function hashBankAccount(
        string memory bankName,
        string memory accountNumber,
        string memory accountName
    ) internal pure returns (bytes32) {
        return keccak256(abi.encodePacked(
            "BANK_ACCOUNT",
            bankName,
            accountNumber,
            accountName
        ));
    }

    /**
     * @dev 组合哈希（用于多个隐私数据）
     */
    function combineHashes(bytes32 hash1, bytes32 hash2)
        internal
        pure
        returns (bytes32)
    {
        return keccak256(abi.encodePacked(hash1, hash2));
    }

    /**
     * @dev 验证哈希一致性
     */
    function verifyHash(bytes32 storedHash, bytes32 providedHash)
        internal
        pure
        returns (bool)
    {
        return storedHash == providedHash;
    }

    // ==================== 辅助函数 ====================

    /**
     * @dev 生成资产唯一标识符
     */
    function generateAssetId(
        string memory prefix,
        address creator,
        uint256 timestamp
    ) internal pure returns (string memory) {
        return string(abi.encodePacked(
            prefix,
            "_",
            toHex(creator),
            "_",
            toString(timestamp)
        ));
    }

    /**
     * @dev 地址转十六进制字符串
     */
    function toHex(address addr) internal pure returns (string memory) {
        bytes32 value = bytes32(uint256(uint160(addr)));
        bytes memory alphabet = "0123456789abcdef";
        bytes memory str = new bytes(42);
        str[0] = '0';
        str[1] = 'x';
        for (uint256 i = 0; i < 20; i++) {
            str[2 + i * 2] = alphabet[uint8(value[i + 12] >> 4)];
            str[3 + i * 2] = alphabet[uint8(value[i + 12] & 0x0f)];
        }
        return string(str);
    }

    /**
     * @dev 数字转字符串
     */
    function toString(uint256 value) internal pure returns (string memory) {
        if (value == 0) {
            return "0";
        }
        uint256 temp = value;
        uint256 digits;
        while (temp != 0) {
            digits++;
            temp /= 10;
        }
        bytes memory buffer = new bytes(digits);
        while (value != 0) {
            digits -= 1;
            buffer[digits] = bytes1(uint8(48 + uint256(value % 10)));
            value /= 10;
        }
        return string(buffer);
    }
}


/**
 * @title DataValidator
 * @dev 数据验证工具 - 确保上链数据的完整性
 */
library DataValidator {
    /**
     * @dev 验证地址有效性
     */
    function validAddress(address addr) internal pure returns (bool) {
        return addr != address(0);
    }

    /**
     * @dev 验证金额有效性（uint96范围）
     */
    function validAmount(uint256 amount) internal pure returns (bool) {
        return amount > 0 && amount <= type(uint96).max;
    }

    /**
     * @dev 验证时间顺序
     */
    function validTimeline(uint64 start, uint64 end)
        internal
        pure
        returns (bool)
    {
        return end > start;
    }

    /**
     * @dev 验证利率范围（0-100%基点）
     */
    function validRate(uint256 rate) internal pure returns (bool) {
        return rate <= 10000; // 10000基点 = 100%
    }

    /**
     * @dev 验证哈希非零
     */
    function validHash(bytes32 hash) internal pure returns (bool) {
        return hash != bytes32(0);
    }
}
