package com.fisco.app.service.blockchain;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisco.app.dto.blockchain.ContractAbiDTO;
import com.fisco.app.dto.blockchain.ContractEventDTO;
import com.fisco.app.dto.blockchain.ContractEventQueryRequest;
import com.fisco.app.dto.blockchain.ContractListQueryRequest;
import com.fisco.app.dto.blockchain.ContractMetadataDTO;
import com.fisco.app.dto.blockchain.DeployGenericContractRequest;
import com.fisco.app.dto.blockchain.DeployGenericContractResponse;
import com.fisco.app.entity.blockchain.ContractEvent;
import com.fisco.app.entity.blockchain.ContractMetadata;
import com.fisco.app.exception.BlockchainIntegrationException;
import com.fisco.app.exception.BusinessException;
import com.fisco.app.repository.blockchain.ContractEventRepository;
import com.fisco.app.repository.blockchain.ContractMetadataRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 通用合约管理服务
 * 提供合约部署、查询、事件索引等功能
 *
 * @author FISCO BCOS Team
 * @since 2026-02-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenericContractService {

    private final ContractMetadataRepository metadataRepository;
    private final ContractEventRepository eventRepository;
    private final CryptoKeyPair cryptoKeyPair;
    private final ObjectMapper objectMapper;

    private static final int MAX_ABI_LENGTH = 100000; // 100KB
    private static final int MAX_BYTECODE_LENGTH = 50000; // 50KB
    private static final int MAX_PAGE_SIZE = 100;
    private static final String CONTRACT_ADDRESS_PATTERN = "^0x[a-fA-F0-9]{40}$";

    /**
     * 验证合约地址格式
     */
    private void validateContractAddress(String address) {
        if (address == null || !address.matches(CONTRACT_ADDRESS_PATTERN)) {
            throw new BusinessException.InvalidStatusException("合约地址格式错误，应为42位十六进制字符串（0x开头）");
        }
    }

    /**
     * 清理日志中的敏感字符，防止日志注入
     */
    private String sanitizeForLog(String input) {
        if (input == null) return "";
        // 移除换行符和回车符，防止日志注入
        return input.replaceAll("[\r\n]", "_");
    }

    /**
     * 部署通用合约
     * 注意：由于FISCO BCOS SDK v3的通用合约部署API较为复杂，
     * 本方法提供简化实现。实际部署请参考ContractDeployer.java使用生成的合约类。
     */
    public DeployGenericContractResponse deployGenericContract(DeployGenericContractRequest request) {
        log.info("开始部署通用合约: contractName={}", sanitizeForLog(request.getContractName()));

        try {
            // 验证ABI格式和长度
            validateAbi(request.getAbi());

            // 验证字节码长度
            if (request.getBytecode() != null && request.getBytecode().length() > MAX_BYTECODE_LENGTH) {
                throw new BusinessException.InvalidStatusException("字节码长度超过限制（最大" + MAX_BYTECODE_LENGTH + "字符）");
            }

            // 生成临时合约地址
            String tempAddress = "0x" + UUID.randomUUID().toString().replace("-", "").substring(0, 40);

            // 并发控制：检查合约地址是否已存在（虽然UUID几乎不会重复，但为了完整性）
            if (metadataRepository.findByContractAddress(tempAddress).isPresent()) {
                throw new BusinessException.InvalidStatusException("合约地址冲突，请重试");
            }

            // 保存合约元数据（简化实现：先保存记录，实际地址需要通过SDK获取）
            // 注意：当前实现使用临时地址，实际生产环境必须通过FISCO SDK部署获取真实地址
            ContractMetadata metadata = new ContractMetadata();

            metadata.setContractAddress(tempAddress);
            metadata.setContractName(request.getContractName());
            metadata.setContractType(request.getContractType() != null ? request.getContractType() : "Generic");
            metadata.setContractVersion(request.getContractVersion());
            metadata.setAbi(request.getAbi());
            metadata.setBytecode(request.getBytecode());
            metadata.setCompilerVersion(request.getCompilerVersion());
            metadata.setOptimizationEnabled(request.getOptimizationEnabled());
            metadata.setConstructorParams(convertToJson(request.getConstructorParams()));
            metadata.setDeployTransactionHash(""); // 需要通过SDK获取
            metadata.setDeployerAddress(cryptoKeyPair.getAddress());
            metadata.setDeployBlockNumber(0L); // 需要通过SDK获取
            metadata.setDeploymentTimestamp(LocalDateTime.now());
            metadata.setStatus(ContractMetadata.ContractStatus.ACTIVE.getCode());
            metadata.setDescription(request.getDescription());

            // 构建标签：包含合约类型和"custom"标签
            String tags = "generic,custom";
            if (request.getContractType() != null && !request.getContractType().isEmpty()) {
                tags = request.getContractType().toLowerCase() + ",custom";
            }
            metadata.setTags(tags);

            metadataRepository.save(metadata);

            log.info("通用合约元数据已保存: contractAddress={}", metadata.getContractAddress());

            // 构建响应
            DeployGenericContractResponse response = new DeployGenericContractResponse();
            response.setContractAddress(metadata.getContractAddress());
            response.setTransactionHash(metadata.getDeployTransactionHash());
            response.setBlockNumber(metadata.getDeployBlockNumber());
            response.setGasUsed(0L); // 需要通过SDK获取
            response.setDeployer(metadata.getDeployerAddress());
            response.setDeploymentTimestamp(metadata.getDeploymentTimestamp());

            return response;

        } catch (Exception e) {
            log.error("部署通用合约失败: contractName={}", request.getContractName(), e);
            throw new BlockchainIntegrationException.ContractCallException(
                null, "deployGeneric", "合约部署失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取合约列表
     */
    public Page<ContractMetadataDTO> getContractList(ContractListQueryRequest request) {
        // 验证分页参数
        if (request.getSize() != null && request.getSize() > MAX_PAGE_SIZE) {
            throw new BusinessException.InvalidStatusException("每页数量不能超过" + MAX_PAGE_SIZE);
        }

        log.info("查询合约列表: contractType={}, status={}",
                 sanitizeForLog(request.getContractType()), sanitizeForLog(request.getStatus()));

        try {
            // 构建分页和排序
            Sort sort = Sort.by(
                "DESC".equalsIgnoreCase(request.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                request.getSortBy() != null ? request.getSortBy() : "deploymentTimestamp"
            );
            Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

            // 查询
            Page<ContractMetadata> page = metadataRepository.findByFilters(
                request.getContractType(),
                request.getStatus(),
                pageable
            );

            // 转换为DTO
            return page.map(this::convertToDTO);

        } catch (Exception e) {
            log.error("查询合约列表失败", e);
            throw new BusinessException("查询合约列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取合约事件
     */
    public Page<ContractEventDTO> getContractEvents(String address, ContractEventQueryRequest request) {
        // 验证合约地址格式
        validateContractAddress(address);

        // 验证分页参数
        if (request.getSize() != null && request.getSize() > MAX_PAGE_SIZE) {
            throw new BusinessException.InvalidStatusException("每页数量不能超过" + MAX_PAGE_SIZE);
        }

        log.info("查询合约事件: contractAddress={}, eventName={}",
                 sanitizeForLog(address), sanitizeForLog(request.getEventName()));

        // 验证合约是否存在（查询后不使用，仅用于验证）
        if (!metadataRepository.findByContractAddress(address).isPresent()) {
            throw new BusinessException.ContractNotFoundException(address);
        }

        try {
            Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

            Page<ContractEvent> page = eventRepository.findByFilters(
                address,
                request.getEventName(),
                request.getFromBlock(),
                request.getToBlock(),
                pageable
            );

            return page.map(this::convertToEventDTO);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询合约事件失败: contractAddress={}", address, e);
            throw new BusinessException("查询合约事件失败: " + e.getMessage());
        }
    }

    /**
     * 获取合约ABI
     */
    public ContractAbiDTO getContractAbi(String address) {
        // 验证合约地址格式
        validateContractAddress(address);

        log.info("查询合约ABI: contractAddress={}", sanitizeForLog(address));

        ContractMetadata metadata = metadataRepository.findByContractAddress(address)
            .orElseThrow(() -> new BusinessException.ContractNotFoundException(address));

        ContractAbiDTO dto = new ContractAbiDTO();
        dto.setContractAddress(metadata.getContractAddress());
        dto.setContractName(metadata.getContractName());
        dto.setContractType(metadata.getContractType());
        dto.setContractVersion(metadata.getContractVersion());
        dto.setAbi(metadata.getAbi());
        dto.setBytecode(metadata.getBytecode());
        dto.setCompilerVersion(metadata.getCompilerVersion());
        dto.setSourceCode(metadata.getSourceCode());
        dto.setConstructorParams(parseJsonToList(metadata.getConstructorParams()));

        return dto;
    }

    // ==================== 私有辅助方法 ====================

    private void validateAbi(String abi) {
        // 验证长度限制，防止DoS
        if (abi == null || abi.length() > MAX_ABI_LENGTH) {
            throw new BusinessException.InvalidContractAbiException(
                "ABI长度无效，最大允许" + MAX_ABI_LENGTH + "字符");
        }

        try {
            objectMapper.readTree(abi);
        } catch (Exception e) {
            throw new BusinessException.InvalidContractAbiException("ABI格式错误: " + e.getMessage());
        }
    }

    private String convertToJson(List<String> list) {
        if (list == null) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.warn("转换JSON失败", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> parseJsonToList(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            log.warn("解析JSON失败", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonToMap(String json) {
        if (json == null) return new HashMap<>();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("解析JSON失败", e);
            return new HashMap<>();
        }
    }

    private ContractMetadataDTO convertToDTO(ContractMetadata metadata) {
        ContractMetadataDTO dto = new ContractMetadataDTO();
        dto.setContractAddress(metadata.getContractAddress());
        dto.setContractName(metadata.getContractName());
        dto.setContractType(metadata.getContractType());
        dto.setContractVersion(metadata.getContractVersion());
        dto.setDeployer(metadata.getDeployerAddress());
        dto.setDeploymentTimestamp(metadata.getDeploymentTimestamp());
        dto.setStatus(metadata.getStatus());
        dto.setDescription(metadata.getDescription());
        dto.setDeployBlockNumber(metadata.getDeployBlockNumber());
        dto.setDeployTransactionHash(metadata.getDeployTransactionHash());
        return dto;
    }

    private ContractEventDTO convertToEventDTO(ContractEvent event) {
        ContractEventDTO dto = new ContractEventDTO();
        dto.setEventName(event.getEventName());
        dto.setBlockNumber(event.getBlockNumber());
        dto.setTransactionHash(event.getTransactionHash());
        dto.setEventTimestamp(event.getEventTimestamp());

        // 解码事件参数
        if (event.getDecodedParams() != null) {
            dto.setDecodedParams(parseJsonToMap(event.getDecodedParams()));
        } else {
            dto.setDecodedParams(new HashMap<>());
        }

        return dto;
    }
}
