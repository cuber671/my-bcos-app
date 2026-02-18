// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract BillPoolV2 {
    // 票据状态枚举（对应 Java 中的 BillStatus）
    enum BillStatus { DRAFT, PENDING_ISSUANCE, ISSUED, NORMAL, ENDORSED, PLEDGED, DISCOUNTED, FINANCED, FROZEN, EXPIRED, DISHONORED, CANCELLED, PAID, SETTLED }
    
    // 票据类型
    enum BillType { BANK_ACCEPTANCE_BILL, COMMERCIAL_ACCEPTANCE_BILL }

    struct Bill {
        string billId;
        string billNo;
        uint256 faceValue;
        uint256 dueDate;
        address currentHolder;
        BillStatus status;
        string parentBillId; // 溯源 ID
        bool isExists;
    }

    mapping(string => Bill) private _bills;
    
    // 事件：用于 Java 后端异步监听更新数据库
    event BillInvested(string indexed billId, address indexed investor, uint256 investAmount, string txHash);
    event BillSplit(string indexed parentId, string[] subBillIds, uint256[] amounts);

    /**
     * @dev 票据投资（核心逻辑：对应 Java 的 investBill）
     * 本质是带有贴现金额记录的权利转移
     */
    function investBill(
        string memory _billId, 
        address _investor,
        uint256 _investAmount
    ) public {
        Bill storage bill = _bills[_billId];
        
        // 对应 Java 中的 validateBillForInvestment
        require(bill.isExists, "Bill not found");
        require(bill.status == BillStatus.NORMAL, "Invalid status");
        require(bill.dueDate > block.timestamp, "Bill expired");
        require(bill.currentHolder != _investor, "Cannot invest self-owned bill");
        
        // 业务规则校验：投资额度（10%-100%）
        require(_investAmount <= bill.faceValue, "Over face value");
        require(_investAmount >= bill.faceValue / 10, "Under 10% face value");

        // 执行权利转移
        bill.currentHolder = _investor;
        bill.status = BillStatus.DISCOUNTED; // 投资后状态变更为贴现/已投资

        emit BillInvested(_billId, _investor, _investAmount, "INTERNAL_TX");
    }

    /**
     * @dev 票据拆分（核心逻辑：对应 Java 的 splitBill）
     * 逻辑：作废原票，生成新票
     */
    function splitBill(
        string memory _parentId,
        string[] memory _subIds,
        string[] memory _subNos,
        uint256[] memory _amounts
    ) public {
        Bill storage parent = _bills[_parentId];
        
        require(parent.isExists, "Parent bill not found");
        require(parent.status == BillStatus.NORMAL, "Parent not splittable");
        require(_subIds.length == _amounts.length, "Array length mismatch");

        uint256 totalAmount = 0;
        for (uint256 i = 0; i < _amounts.length; i++) {
            totalAmount += _amounts[i];
            
            // 创建子票据
            _bills[_subIds[i]] = Bill({
                billId: _subIds[i],
                billNo: _subNos[i],
                faceValue: _amounts[i],
                dueDate: parent.dueDate,
                currentHolder: parent.currentHolder,
                status: BillStatus.NORMAL,
                parentBillId: _parentId,
                isExists: true
            });
        }

        // 验证总额相等（对应 Java 步骤2）
        require(totalAmount == parent.faceValue, "Amount sum mismatch");

        // 作废原票（对应 Java 步骤4）
        parent.status = BillStatus.CANCELLED;

        emit BillSplit(_parentId, _subIds, _amounts);
    }

    // Getter 方法供 Web3j 调用
    function getBill(string memory _id) public view returns (
        string memory, uint256, address, BillStatus
    ) {
        Bill memory b = _bills[_id];
        return (b.billNo, b.faceValue, b.currentHolder, b.status);
    }
}