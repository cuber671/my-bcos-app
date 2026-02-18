// SPDX-License-Identifier: MIT
pragma solidity >=0.4.22 <0.9.0;

contract BillSplitV2 {
    
    // 票据状态枚举
    enum BillStatus { NORMAL, ENDORSED, DISCOUNTED, PAID, CANCELLED, SPLIT }

    // 票据结构体
    struct Bill {
        string billId;            // 票据唯一标识
        string billNo;            // 票据编号
        uint256 faceValue;        // 票面金额
        address currentHolder;    // 当前持票人
        address drawee;           // 承兑人
        uint256 dueDate;          // 到期日
        BillStatus status;        // 状态
        string parentBillId;      // 父票据ID (溯源用)
        bool isSplit;             // 是否已被拆分
    }

    mapping(string => Bill) public bills;
    
    // 事件：用于后端监听
    event BillSplitEvent(
        string indexed parentBillId, 
        string[] subBillIds, 
        uint256[] amounts, 
        address operator
    );

    /**
     * @dev 拆分票据核心方法
     * @param _billId 原票据ID
     * @param _subBillIds 预生成的子票据ID列表（由后端UUID生成）
     * @param _subBillNos 子票据编号列表
     * @param _amounts 拆分金额列表
     */
    function splitBill(
        string memory _billId,
        string[] memory _subBillIds,
        string[] memory _subBillNos,
        uint256[] memory _amounts
    ) public {
        Bill storage originalBill = bills[_billId];

        // 1. 基础校验
        require(bytes(originalBill.billId).length != 0, "Original bill does not exist");
        require(originalBill.currentHolder == msg.sender, "Only the holder can split the bill");
        require(originalBill.status == BillStatus.NORMAL, "Only NORMAL bills can be split");
        require(_subBillIds.length == _amounts.length, "Input arrays length mismatch");
        require(_subBillIds.length > 1, "Must split into at least two bills");

        // 2. 校验金额总和
        uint256 totalAmount = 0;
        for (uint i = 0; i < _amounts.length; i++) {
            totalAmount += _amounts[i];
        }
        require(totalAmount == originalBill.faceValue, "Total split amount must equal original face value");

        // 3. 处理原票据状态
        originalBill.status = BillStatus.SPLIT;
        originalBill.isSplit = true;

        // 4. 生成子票据
        for (uint j = 0; j < _subBillIds.length; j++) {
            require(bytes(bills[_subBillIds[j]].billId).length == 0, "Sub bill ID already exists");

            bills[_subBillIds[j]] = Bill({
                billId: _subBillIds[j],
                billNo: _subBillNos[j],
                faceValue: _amounts[j],
                currentHolder: originalBill.currentHolder,
                drawee: originalBill.drawee,
                dueDate: originalBill.dueDate,
                status: BillStatus.NORMAL,
                parentBillId: _billId,
                isSplit: false
            });
        }

        // 5. 抛出事件，供后端同步数据库
        emit BillSplitEvent(_billId, _subBillIds, _amounts, msg.sender);
    }

    /**
     * @dev 辅助方法：查询票据详情
     */
    function getBillInfo(string memory _billId) public view returns (
        string memory, uint256, address, BillStatus, string memory
    ) {
        Bill memory b = bills[_billId];
        return (b.billNo, b.faceValue, b.currentHolder, b.status, b.parentBillId);
    }
}