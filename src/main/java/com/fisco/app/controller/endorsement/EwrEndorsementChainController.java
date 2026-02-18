package com.fisco.app.controller.endorsement;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.dto.endorsement.EwrEndorsementChainResponse;
import com.fisco.app.dto.endorsement.EwrEndorsementConfirmRequest;
import com.fisco.app.dto.endorsement.EwrEndorsementCreateRequest;
import com.fisco.app.service.warehouse.EwrEndorsementChainService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;

/**
 * 背书链Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/ewr/endorsement")
@Api(tags = "电子仓单背书管理")
public class EwrEndorsementChainController {

    @Autowired
    private EwrEndorsementChainService endorsementService;

    /**
     * 创建背书请求
     */
    @PostMapping("/create")
    @ApiOperation(value = "创建背书请求", notes = "发起仓单背书请求")
    public ResponseEntity<EwrEndorsementChainResponse> createEndorsement(
            @Valid @RequestBody EwrEndorsementCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        log.info("收到创建背书请求, 仓单ID: {}, 转入方: {}", request.getReceiptId(), request.getEndorseTo());

        // 实际项目中应该从SecurityContext获取当前用户信息
        String operatorFromId = userId != null ? userId : "system-user";
        String operatorFromName = userName != null ? userName : "系统用户";

        EwrEndorsementChainResponse response = endorsementService.createEndorsement(
                request, operatorFromId, operatorFromName);
        return ResponseEntity.ok(response);
    }

    /**
     * 确认背书
     */
    @PostMapping("/confirm")
    @ApiOperation(value = "确认背书", notes = "确认或取消背书请求")
    public ResponseEntity<EwrEndorsementChainResponse> confirmEndorsement(
            @Valid @RequestBody EwrEndorsementConfirmRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        log.info("收到确认背书请求, ID: {}, 状态: {}", request.getId(), request.getConfirmStatus());

        // 实际项目中应该从SecurityContext获取当前用户信息
        String confirmerId = userId != null ? userId : "system-user";
        String confirmerName = userName != null ? userName : "系统用户";

        EwrEndorsementChainResponse response = endorsementService.confirmEndorsement(
                request, confirmerId, confirmerName);
        return ResponseEntity.ok(response);
    }

    /**
     * 查询仓单的背书链
     */
    @GetMapping("/chain/{receiptId}")
    @ApiOperation(value = "查询背书链", notes = "查询仓单的完整背书链记录")
    public ResponseEntity<List<EwrEndorsementChainResponse>> getEndorsementChain(
            @ApiParam(value = "仓单ID", required = true) @PathVariable String receiptId) {
        log.info("查询背书链, 仓单ID: {}", receiptId);
        List<EwrEndorsementChainResponse> chain = endorsementService.getEndorsementChain(receiptId);
        return ResponseEntity.ok(chain);
    }

    /**
     * 查询待确认的背书
     */
    @GetMapping("/pending/{endorseTo}")
    @ApiOperation(value = "查询待确认背书", notes = "查询指定企业待确认的背书请求")
    public ResponseEntity<List<EwrEndorsementChainResponse>> getPendingEndorsements(
            @ApiParam(value = "被背书方地址", required = true) @PathVariable String endorseTo) {
        log.info("查询待确认背书, 被背书方: {}", endorseTo);
        List<EwrEndorsementChainResponse> endorsements = endorsementService.getPendingEndorsements(endorseTo);
        return ResponseEntity.ok(endorsements);
    }

    /**
     * 根据ID查询背书
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "查询背书详情", notes = "根据ID查询背书详细信息")
    public ResponseEntity<EwrEndorsementChainResponse> getEndorsementById(
            @ApiParam(value = "背书ID", required = true) @PathVariable @NonNull String id) {
        log.info("查询背书详情, ID: {}", id);
        EwrEndorsementChainResponse response = endorsementService.getEndorsementById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 根据背书编号查询
     */
    @GetMapping("/by-no/{endorsementNo}")
    @ApiOperation(value = "根据编号查询背书", notes = "根据背书编号查询")
    public ResponseEntity<EwrEndorsementChainResponse> getEndorsementByNo(
            @ApiParam(value = "背书编号", required = true) @PathVariable String endorsementNo) {
        log.info("查询背书, 编号: {}", endorsementNo);
        EwrEndorsementChainResponse response = endorsementService.getEndorsementByNo(endorsementNo);
        return ResponseEntity.ok(response);
    }

    /**
     * 查询企业发起的背书（作为转出方）
     */
    @GetMapping("/from/{endorseFrom}")
    @ApiOperation(value = "查询发起的背书", notes = "查询指定企业发起的所有已确认背书")
    public ResponseEntity<List<EwrEndorsementChainResponse>> getEndorsementsByEndorseFrom(
            @ApiParam(value = "背书方地址", required = true) @PathVariable String endorseFrom) {
        log.info("查询发起的背书, 背书方: {}", endorseFrom);
        List<EwrEndorsementChainResponse> responses =
                endorsementService.getEndorsementsByEndorseFrom(endorseFrom);
        return ResponseEntity.ok(responses);
    }

    /**
     * 查询企业接收的背书（作为转入方）
     */
    @GetMapping("/to/{endorseTo}")
    @ApiOperation(value = "查询接收的背书", notes = "查询指定企业接收的所有已确认背书")
    public ResponseEntity<List<EwrEndorsementChainResponse>> getEndorsementsByEndorseTo(
            @ApiParam(value = "被背书方地址", required = true) @PathVariable String endorseTo) {
        log.info("查询接收的背书, 被背书方: {}", endorseTo);
        List<EwrEndorsementChainResponse> responses =
                endorsementService.getEndorsementsByEndorseTo(endorseTo);
        return ResponseEntity.ok(responses);
    }

    /**
     * 查询经手人的背书记录
     */
    @GetMapping("/by-operator/{operatorId}")
    @ApiOperation(value = "查询经手人的背书", notes = "查询指定经手人的所有背书记录")
    public ResponseEntity<List<EwrEndorsementChainResponse>> getEndorsementsByOperator(
            @ApiParam(value = "经手人ID", required = true) @PathVariable String operatorId) {
        log.info("查询经手人背书记录, 经手人ID: {}", operatorId);
        List<EwrEndorsementChainResponse> responses =
                endorsementService.getEndorsementsByOperator(operatorId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 统计仓单的背书次数
     */
    @GetMapping("/count/{receiptId}")
    @ApiOperation(value = "统计背书次数", notes = "统计仓单的背书总次数")
    public ResponseEntity<Long> countEndorsements(
            @ApiParam(value = "仓单ID", required = true) @PathVariable String receiptId) {
        log.info("统计背书次数, 仓单ID: {}", receiptId);
        Long count = endorsementService.countEndorsements(receiptId);
        return ResponseEntity.ok(count);
    }

    /**
     * 更新区块链上链信息
     */
    @PutMapping("/blockchain/{id}")
    @ApiOperation(value = "更新区块链信息", notes = "更新背书的区块链上链信息")
    public ResponseEntity<Void> updateBlockchainInfo(
            @PathVariable @NonNull String id,
            @RequestParam String txHash,
            @RequestParam Long blockNumber) {
        log.info("更新区块链信息, ID: {}, txHash: {}", id, txHash);
        endorsementService.updateBlockchainInfo(id, txHash, blockNumber);
        return ResponseEntity.ok().build();
    }
}
