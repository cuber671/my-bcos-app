// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

/**
 * @title EnterpriseRegistry
 * @dev 企业注册与征信管理智能合约
 */
contract EnterpriseRegistry {
    // 企业状态枚举
    enum EnterpriseStatus {
        Pending,    // 待审核
        Active,     // 已激活
        Suspended,  // 已暂停
        Blacklisted, // 已拉黑
        Deleted     // 已删除（注销）
    }

    // 企业角色枚举
    enum EnterpriseRole {
        Supplier,       // 供应商
        CoreEnterprise, // 核心企业
        FinancialInstitution, // 金融机构
        Regulator       // 监管机构
 
    // 企业信息结构体
    struct Enterprise {
        string name;                    // 企业名称
        string creditCode;              // 统一社会信用代码
        string enterpriseAddress;       // 企业地址
        EnterpriseRole role;            // 企业角色
        EnterpriseStatus status;        // 企业状态
        uint256 creditRating;          // 信用评级 (0-100)
        uint256 creditLimit;           // 授信额度
        uint256 registeredAt;          // 注册时间
        uint256 updatedAt;             // 更新时间
        bool exists;                   // 是否存在
    }

    // 信用记录结构体
    struct CreditRecord {
        bytes32 recordId;               // 记录ID
        address enterpriseAddress;     // 企业地址
        int256 ratingChange;           // 评级变化
        uint256 timestamp;             // 时间戳
        string reason;                 // 原因
        string operator;               // 操作员
    }

    // 状态变量
    address public admin;                      // 管理员地址
    uint256 public enterpriseCount;           // 企业总数

    // 映射
    mapping(address => Enterprise) public enterprises;       // 企业信息
    mapping(string => address) public creditCodeToAddress;   // 信用代码->地址
    mapping(address => CreditRecord[]) public creditHistory; // 信用历史

    // 事件
    event EnterpriseRegistered(
        address indexed enterpriseAddr,
        string name,
        string creditCode,
        EnterpriseRole role
    );

    event EnterpriseStatusUpdated(
        address indexed enterpriseAddr,
        EnterpriseStatus oldStatus,
        EnterpriseStatus newStatus
    );

    event CreditRatingUpdated(
        address indexed enterpriseAddr,
        uint256 oldRating,
        uint256 newRating,
        string reason
    );

    event CreditLimitUpdated(
        address indexed enterpriseAddr,
        uint256 oldLimit,
        uint256 newLimit
    );

    // 修饰器
    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin can call this function");
        _;
    }

    modifier onlyExistingEnterprise(address enterpriseAddr) {
        require(enterprises[enterpriseAddr].exists, "Enterprise does not exist");
        _;
    }

    modifier onlyActiveEnterprise(address enterpriseAddr) {
        require(
            enterprises[enterpriseAddr].exists &&
            enterprises[enterpriseAddr].status == EnterpriseStatus.Active,
            "Enterprise is not active"
        );
        _;
    }

    /**
     * @dev 构造函数
     */
    constructor() {
        admin = msg.sender;
        enterpriseCount = 0;
    }

    /**
     * @dev 注册企业
     * @notice 企业可以自己注册，使用 msg.sender 作为企业地址
     */
    function registerEnterprise(
        string memory name,
        string memory creditCode,
        string memory enterpriseAddr,
        EnterpriseRole role
    ) public returns (bool) {
        require(bytes(name).length > 0, "Name cannot be empty");
        require(bytes(creditCode).length > 0, "Credit code cannot be empty");
        require(!enterprises[msg.sender].exists, "Enterprise already registered");
        require(creditCodeToAddress[creditCode] == address(0), "Credit code already registered");

        enterprises[msg.sender] = Enterprise({
            name: name,
            creditCode: creditCode,
            enterpriseAddress: enterpriseAddr,
            role: role,
            status: EnterpriseStatus.Pending,
            creditRating: 60, // 默认评级60分
            creditLimit: 0,
            registeredAt: block.timestamp,
            updatedAt: block.timestamp,
            exists: true
        });

        creditCodeToAddress[creditCode] = msg.sender;
        enterpriseCount++;

        emit EnterpriseRegistered(msg.sender, name, creditCode, role);
        return true;
    }

    /**
     * @dev 管理员代为注册企业
     * @notice 管理员可以指定企业地址进行注册，用于审核通过后的上链注册
     * @param name 企业名称
     * @param creditCode 统一社会信用代码
     * @param enterpriseAddr 企业区块链地址（将作为存储的key）
     * @param role 企业角色
     */
    function registerEnterpriseByAdmin(
        string memory name,
        string memory creditCode,
        address enterpriseAddr,
        EnterpriseRole role
    ) public onlyAdmin returns (bool) {
        require(bytes(name).length > 0, "Name cannot be empty");
        require(bytes(creditCode).length > 0, "Credit code cannot be empty");
        require(enterpriseAddr != address(0), "Enterprise address cannot be zero");
        require(!enterprises[enterpriseAddr].exists, "Enterprise already registered");
        require(creditCodeToAddress[creditCode] == address(0), "Credit code already registered");

        enterprises[enterpriseAddr] = Enterprise({
            name: name,
            creditCode: creditCode,
            enterpriseAddress: toHexString(enterpriseAddr),
            role: role,
            status: EnterpriseStatus.Active,  // 管理员注册的直接设为Active
            creditRating: 60, // 默认评级60分
            creditLimit: 0,
            registeredAt: block.timestamp,
            updatedAt: block.timestamp,
            exists: true
        });

        creditCodeToAddress[creditCode] = enterpriseAddr;
        enterpriseCount++;

        emit EnterpriseRegistered(enterpriseAddr, name, creditCode, role);
        return true;
    }

    /**
     * @dev 将地址转换为十六进制字符串
     */
    function toHexString(address addr) internal pure returns (string memory) {
        bytes20 value = bytes20(addr);
        bytes memory alphabet = "0123456789abcdef";
        bytes memory str = new bytes(42);  // 0x + 40个字符

        str[0] = '0';
        str[1] = 'x';

        for (uint256 i = 0; i < 20; i++) {
            str[2 + i * 2] = alphabet[uint8(value[i] >> 4)];
            str[3 + i * 2] = alphabet[uint8(value[i] & 0x0f)];
        }

        return string(str);
    }

    /**
     * @dev 审核企业
     */
    function approveEnterprise(address enterpriseAddr) public onlyAdmin onlyExistingEnterprise(enterpriseAddr) returns (bool) {
        Enterprise storage enterprise = enterprises[enterpriseAddr];
        EnterpriseStatus oldStatus = enterprise.status;

        enterprise.status = EnterpriseStatus.Active;
        enterprise.updatedAt = block.timestamp;

        emit EnterpriseStatusUpdated(enterpriseAddr, oldStatus, EnterpriseStatus.Active);
        return true;
    }

    /**
     * @dev 更新企业状态
     */
    function updateEnterpriseStatus(
        address enterpriseAddr,
        EnterpriseStatus newStatus
    ) public onlyAdmin onlyExistingEnterprise(enterpriseAddr) returns (bool) {
        Enterprise storage enterprise = enterprises[enterpriseAddr];
        EnterpriseStatus oldStatus = enterprise.status;

        enterprise.status = newStatus;
        enterprise.updatedAt = block.timestamp;

        emit EnterpriseStatusUpdated(enterpriseAddr, oldStatus, newStatus);
        return true;
    }

    /**
     * @dev 更新信用评级
     */
    function updateCreditRating(
        address enterpriseAddr,
        uint256 newRating,
        string memory reason
    ) public onlyAdmin onlyExistingEnterprise(enterpriseAddr) returns (bool) {
        require(newRating <= 100, "Rating must be <= 100");

        Enterprise storage enterprise = enterprises[enterpriseAddr];
        uint256 oldRating = enterprise.creditRating;
        int256 ratingChange = int256(newRating) - int256(oldRating);

        enterprise.creditRating = newRating;
        enterprise.updatedAt = block.timestamp;

        // 记录信用历史
        creditHistory[enterpriseAddr].push(CreditRecord({
            recordId: keccak256(abi.encodePacked(block.timestamp, enterpriseAddr, ratingChange)),
            enterpriseAddress: enterpriseAddr,
            ratingChange: ratingChange,
            timestamp: block.timestamp,
            reason: reason,
            operator: "Admin"
        }));

        emit CreditRatingUpdated(enterpriseAddr, oldRating, newRating, reason);
        return true;
    }

    /**
     * @dev 设置授信额度
     */
    function setCreditLimit(
        address enterpriseAddr,
        uint256 creditLimit
    ) public onlyAdmin onlyExistingEnterprise(enterpriseAddr) returns (bool) {
        Enterprise storage enterprise = enterprises[enterpriseAddr];
        uint256 oldLimit = enterprise.creditLimit;

        enterprise.creditLimit = creditLimit;
        enterprise.updatedAt = block.timestamp;

        emit CreditLimitUpdated(enterpriseAddr, oldLimit, creditLimit);
        return true;
    }

    /**
     * @dev 获取企业信息
     */
    function getEnterprise(address enterpriseAddr)
        public
        view
        onlyExistingEnterprise(enterpriseAddr)
        returns (Enterprise memory)
    {
        return enterprises[enterpriseAddr];
    }

    /**
     * @dev 根据信用代码获取企业地址
     */
    function getEnterpriseByCreditCode(string memory creditCode) public view returns (address) {
        return creditCodeToAddress[creditCode];
    }

    /**
     * @dev 获取信用历史记录数量
     */
    function getCreditHistoryCount(address enterpriseAddr) public view returns (uint256) {
        return creditHistory[enterpriseAddr].length;
    }

    /**
     * @dev 检查企业是否有效
     */
    function isEnterpriseValid(address enterpriseAddr) public view returns (bool) {
        return enterprises[enterpriseAddr].exists &&
               enterprises[enterpriseAddr].status == EnterpriseStatus.Active;
    }

    /**
     * @dev 获取所有活跃企业（分页）
     */
    function getActiveEnterprises(uint256 offset, uint256 limit)
        public
        view
        returns (address[] memory)
    {
        // 由于Solidity限制，这里只返回地址数组
        // 实际应用中可以通过前端遍历获取详细信息
        address[] memory activeAddresses = new address[](limit);
        uint256 count = 0;

        // 注意：此函数需要配合链下索引使用
        // 在实际应用中，应该维护一个活跃企业地址数组

        return activeAddresses;
    }

    /**
     * @dev 转移管理员权限
     */
    function transferAdmin(address newAdmin) public onlyAdmin {
        require(newAdmin != address(0), "New admin cannot be zero address");
        admin = newAdmin;
    }
}
