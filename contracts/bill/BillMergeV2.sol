// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

/**
 * @title 票据合并管理合约 V2 (哈希优化版)
 */
contract BillMergeV2 {
    
    enum AssetType { BILL, WAREHOUSE_RECEIPT }

    struct MergeAction {
        bytes32[] parentHashes;   // 显式：多个父票据（来源）的哈希
        bytes32 newAssetHash;     // 显式：生成的新票据哈希
        uint256 totalAmount;      // 显式：合并后的总金额
        uint256 timestamp;        // 显式：处理时间
        address operator;         // 显式：操作者
        AssetType assetType;      // 显式：资产类型
        bool exists;
    }

    // 存储：新资产哈希 => 合并详情 (用于向下追溯来源)
    mapping(bytes32 => MergeAction) private _mergeRecords;
    
    // 存储：父资产哈希 => 新资产哈希 (用于向上查找到向，防止父单重复合并)
    mapping(bytes32 => bytes32) private _parentToNew;

    event BillMergeExecuted(
        bytes32 indexed newAssetHash,
        uint256 parentsCount,
        uint256 totalAmount,
        address indexed operator,
        uint256 timestamp
    );

    /**
     * @dev 执行合并上链
     * @param _pHashes 待合并的父票据哈希数组
     * @param _newHash 生成的新票据哈希
     * @param _amount 合并后的总金额
     * @param _type 资产类型 (0-Bill, 1-Receipt)
     */
    function executeMerge(
        bytes32[] memory _pHashes,
        bytes32 _newHash,
        uint256 _amount,
        uint8 _type
    ) external {
        // 1. 安全校验
        require(_newHash != bytes32(0), "Invalid new asset hash");
        require(_pHashes.length >= 2, "Must merge at least 2 assets");
        require(!_mergeRecords[_newHash].exists, "New asset hash collision");

        // 2. 校验父单状态并锁定关系
        for (uint256 i = 0; i < _pHashes.length; i++) {
            bytes32 pHash = _pHashes[i];
            require(pHash != bytes32(0), "Invalid parent hash");
            // 核心：确保这些父单之前没有被合并过，防止“双重合并”
            require(_parentToNew[pHash] == bytes32(0), "Parent asset already merged");
            
            _parentToNew[pHash] = _newHash;
        }

        // 3. 记录合并详情
        MergeAction storage action = _mergeRecords[_newHash];
        action.parentHashes = _pHashes;
        action.newAssetHash = _newHash;
        action.totalAmount = _amount;
        action.timestamp = block.timestamp;
        action.operator = msg.sender;
        action.assetType = AssetType(_type);
        action.exists = true;

        emit BillMergeExecuted(_newHash, _pHashes.length, _amount, msg.sender, block.timestamp);
    }

    // --- 查询接口 ---

    // 1. 溯源：通过新资产查所有来源父资产
    function getSourceHashes(bytes32 _newHash) external view returns (bytes32[] memory) {
        return _mergeRecords[_newHash].parentHashes;
    }

    // 2. 查去向：通过父资产查它合并到了哪个新资产
    function getDestinationHash(bytes32 _pHash) external view returns (bytes32) {
        return _parentToNew[_pHash];
    }

    // 3. 获取合并完整记录
    function getMergeDetail(bytes32 _newHash) external view returns (
        uint256 amount,
        uint256 time,
        address op,
        bytes32[] memory parents
    ) {
        require(_mergeRecords[_newHash].exists, "Record not found");
        MergeAction storage action = _mergeRecords[_newHash];
        return (action.totalAmount, action.timestamp, action.operator, action.parentHashes);
    }
}