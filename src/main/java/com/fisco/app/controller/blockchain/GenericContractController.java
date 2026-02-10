package com.fisco.app.controller.blockchain;

import com.fisco.app.dto.blockchain.*;
import com.fisco.app.service.blockchain.GenericContractService;
import com.fisco.app.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;

/**
 * 通用合约管理Controller
 * 提供通用合约部署、查询、事件等接口
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Slf4j
@RestController
@RequestMapping("/api/blockchain/contract")
@RequiredArgsConstructor
@Api(tags = "BlockchainContractManagement")
public class GenericContractController {

    private final GenericContractService genericContractService;

    /**
     * 部署通用合约
     * POST /api/blockchain/contract/deploy-generic
     */
    @PostMapping("/deploy-generic")
    @ApiOperation(value = "部署通用合约",
        notes = "部署自定义智能合约到区块链。" +
                "参数说明：" +
                "- abi: 合约ABI（JSON格式的接口定义）；" +
                "- bytecode: 合约字节码（十六进制，0x开头）；" +
                "- constructorParams: 构造函数参数列表（可选）；" +
                "- contractName: 合约名称；" +
                "- contractVersion: 合约版本（可选）。" +
                "返回：合约地址、交易哈希、区块号等信息。" +
                "注意事项：" +
                "- 本接口使用FISCO SDK的动态部署功能；" +
                "- 部署需要消耗Gas，请确保账户有足够余额；" +
                "- 部署成功后会自动保存合约元数据到数据库。" +
                "【公开接口】无需认证即可访问。")
    @ApiResponses({
        @ApiResponse(code = 200, message = "合约部署成功", response = DeployGenericContractResponse.class),
        @ApiResponse(code = 400, message = "参数错误（ABI格式错误、字节码无效）"),
        @ApiResponse(code = 500, message = "部署失败（区块链网络错误、合约执行失败）")
    })
    public ResponseEntity<Result<DeployGenericContractResponse>> deployGenericContract(
            @Valid @RequestBody DeployGenericContractRequest request) {

        log.info("接收到通用合约部署请求: contractName={}", request.getContractName());

        DeployGenericContractResponse response = genericContractService.deployGenericContract(request);

        return ResponseEntity.ok(Result.success("合约部署成功", response));
    }

    /**
     * 查询已部署合约列表
     * GET /api/blockchain/contract/list
     */
    @GetMapping("/list")
    @ApiOperation(value = "查询已部署合约列表",
        notes = "分页查询已部署的智能合约列表。" +
                "查询参数：" +
                "- contractType: 合约类型（可选，如Generic、Bill等）；" +
                "- status: 合约状态（可选，默认ACTIVE）；" +
                "- page: 页码（默认0）；" +
                "- size: 每页数量（默认10）；" +
                "- sortBy: 排序字段（默认deploymentTimestamp）；" +
                "- sortOrder: 排序方向（ASC/DESC，默认DESC）。" +
                "返回：分页的合约列表，包含合约地址、名称、类型、部署时间等信息。" +
                "【公开接口】无需认证即可访问。")
    @ApiResponses({
        @ApiResponse(code = 200, message = "查询成功", response = ContractMetadataDTO.class, responseContainer = "Page"),
        @ApiResponse(code = 400, message = "查询参数错误"),
        @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public ResponseEntity<Result<Page<ContractMetadataDTO>>> getContractList(
            @ApiParam(value = "合约类型", example = "Generic")
            @RequestParam(required = false) String contractType,

            @ApiParam(value = "合约状态", example = "ACTIVE")
            @RequestParam(required = false, defaultValue = "ACTIVE") String status,

            @ApiParam(value = "页码", example = "0")
            @RequestParam(required = false, defaultValue = "0") Integer page,

            @ApiParam(value = "每页数量（最大100）", example = "10")
            @RequestParam(required = false, defaultValue = "10")
            @Max(value = 100, message = "每页数量不能超过100") Integer size,

            @ApiParam(value = "排序字段", example = "deploymentTimestamp")
            @RequestParam(required = false, defaultValue = "deploymentTimestamp") String sortBy,

            @ApiParam(value = "排序方向（ASC/DESC）", example = "DESC")
            @RequestParam(required = false, defaultValue = "DESC") String sortOrder) {

        log.info("查询合约列表: contractType={}, status={}, page={}",
                 contractType, status, page);

        ContractListQueryRequest request = new ContractListQueryRequest();
        request.setContractType(contractType);
        request.setStatus(status);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortOrder(sortOrder);

        Page<ContractMetadataDTO> result = genericContractService.getContractList(request);

        return ResponseEntity.ok(Result.success("查询成功", result));
    }

    /**
     * 查询合约事件
     * GET /api/blockchain/contract/{address}/events
     */
    @GetMapping("/{address}/events")
    @ApiOperation(value = "查询合约事件",
        notes = "查询指定合约的事件日志。" +
                "路径参数：" +
                "- address: 合约地址（42位十六进制字符串，0x开头）。" +
                "查询参数：" +
                "- eventName: 事件名称（可选，不传则查询所有事件）；" +
                "- fromBlock: 起始区块号（可选）；" +
                "- toBlock: 结束区块号（可选）；" +
                "- page: 页码（默认0）；" +
                "- size: 每页数量（默认20）。" +
                "返回：分页的事件列表，包含事件名称、区块号、交易哈希、解码参数等。" +
                "注意事项：" +
                "- 事件数据从数据库中读取，需要先通过其他方式索引事件；" +
                "- 解码参数包含事件的具体参数值。" +
                "【公开接口】无需认证即可访问。")
    @ApiResponses({
        @ApiResponse(code = 200, message = "查询成功", response = ContractEventDTO.class, responseContainer = "Page"),
        @ApiResponse(code = 400, message = "查询参数错误"),
        @ApiResponse(code = 404, message = "合约不存在"),
        @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public ResponseEntity<Result<Page<ContractEventDTO>>> getContractEvents(
            @ApiParam(value = "合约地址", required = true, example = "0x1234567890abcdef1234567890abcdef12345678")
            @PathVariable String address,

            @ApiParam(value = "事件名称", example = "BillIssued")
            @RequestParam(required = false) String eventName,

            @ApiParam(value = "起始区块号", example = "1000")
            @RequestParam(required = false) Long fromBlock,

            @ApiParam(value = "结束区块号", example = "2000")
            @RequestParam(required = false) Long toBlock,

            @ApiParam(value = "页码", example = "0")
            @RequestParam(required = false, defaultValue = "0") Integer page,

            @ApiParam(value = "每页数量（最大100）", example = "20")
            @RequestParam(required = false, defaultValue = "20")
            @Max(value = 100, message = "每页数量不能超过100") Integer size) {

        log.info("查询合约事件: contractAddress={}, eventName={}", address, eventName);

        ContractEventQueryRequest request = new ContractEventQueryRequest();
        request.setEventName(eventName);
        request.setFromBlock(fromBlock);
        request.setToBlock(toBlock);
        request.setPage(page);
        request.setSize(size);

        Page<ContractEventDTO> result = genericContractService.getContractEvents(address, request);

        return ResponseEntity.ok(Result.success("查询成功", result));
    }

    /**
     * 查询合约ABI
     * GET /api/blockchain/contract/{address}/abi
     */
    @GetMapping("/{address}/abi")
    @ApiOperation(value = "查询合约ABI",
        notes = "查询指定合约的ABI、字节码等元数据信息。" +
                "路径参数：" +
                "- address: 合约地址（42位十六进制字符串，0x开头）。" +
                "返回：合约的ABI、字节码、编译器版本、源代码等元数据。" +
                "注意事项：" +
                "- 只有通过本系统部署的合约才会有完整的元数据；" +
                "- ABI可用于生成合约调用代码或进行离线签名。" +
                "【公开接口】无需认证即可访问。")
    @ApiResponses({
        @ApiResponse(code = 200, message = "查询成功", response = ContractAbiDTO.class),
        @ApiResponse(code = 400, message = "合约地址格式错误"),
        @ApiResponse(code = 404, message = "合约ABI不存在"),
        @ApiResponse(code = 500, message = "服务器内部错误")
    })
    public ResponseEntity<Result<ContractAbiDTO>> getContractAbi(
            @ApiParam(value = "合约地址", required = true, example = "0x1234567890abcdef1234567890abcdef12345678")
            @PathVariable String address) {

        log.info("查询合约ABI: contractAddress={}", address);

        ContractAbiDTO result = genericContractService.getContractAbi(address);

        return ResponseEntity.ok(Result.success("查询成功", result));
    }
}
