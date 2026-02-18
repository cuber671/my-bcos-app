// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract BillSplitV2 {
    // ==================== 1. 结构体封装 ====================
    enum BillStatus { NORMAL, SPLIT, PAID, DEFAULTED }

    struct BillCore {
        uint256 faceValue;
        address currentHolder;
        address drawee;
        uint256 dueDate;
        BillStatus status;
    }

    struct BillMeta {
        string billId;
        string billNo;
        string parentBillId;
        bytes32 dataHash; // 核心合同/发票的哈希
        uint256 createdAt;
    }

    // 封装输入参数，规避 Stack Too Deep
    struct SplitInput {
        string parentBillId;
        string[] subBillIds;
        string[] subBillNos;
        uint256[] amounts;
        bytes32[] dataHashes;
    }

    // ==================== 2. 状态变量 ====================
    address public admin;
    address public javaBackend;
    mapping(string => BillCore) public billCores;
    mapping(string => BillMeta) public billMetas;

    event BillSplitExecuted(string indexed parentId, string[] subIds, address operator);

    // ==================== 3. 权限控制 ====================
    modifier onlyAdmin() {
        require(msg.sender == admin, "Not admin");
        _;
    }

    modifier onlyJavaBackend() {
        require(msg.sender == javaBackend, "Not java backend");
        _;
    }

    constructor(address _admin) {
        admin = _admin;
        javaBackend = _admin;
    }

    function setJavaBackend(address _new) external onlyAdmin {
        javaBackend = _new;
    }

    // ==================== 4. 核心业务 ====================

    /**
     * @dev 拆分票据（参考 ReceivableV2 的优化模式）
     */
    function splitBill(SplitInput calldata input) external onlyJavaBackend returns (bool) {
        BillCore storage parent = billCores[input.parentBillId];
        
        // 校验：逻辑必须严密 (Checks)
        require(parent.status == BillStatus.NORMAL, "Invalid status");
        require(input.subBillIds.length == input.amounts.length, "Length mismatch");
        
        uint256 total = 0;
        for(uint i = 0; i < input.amounts.length; i++) {
            total += input.amounts[i];
        }
        require(total == parent.faceValue, "Amount not equal");

        // 执行：修改状态 (Effects)
        parent.status = BillStatus.SPLIT;

        for(uint j = 0; j < input.subBillIds.length; j++) {
            string memory sid = input.subBillIds[j];
            
            billCores[sid] = BillCore({
                faceValue: input.amounts[j],
                currentHolder: parent.currentHolder,
                drawee: parent.drawee,
                dueDate: parent.dueDate,
                status: BillStatus.NORMAL
            });

            billMetas[sid] = BillMeta({
                billId: sid,
                billNo: input.subBillNos[j],
                parentBillId: input.parentBillId,
                dataHash: input.dataHashes[j],
                createdAt: block.timestamp
            });
        }

        emit BillSplitExecuted(input.parentBillId, input.subBillIds, msg.sender);
        return true;
    }

    // 查询辅助函数 (参考 ReceivableV2 的分段查询)
    function getBillFullInfo(string memory _id) external view returns (BillCore memory, BillMeta memory) {
        return (billCores[_id], billMetas[_id]);
    }
}