package com.fisco.app.Common.Config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fisco.app.Common.Annotation.StockOrderOwnership;
import com.fisco.app.Common.Annotation.WarehousePermissionCheck;
import com.fisco.app.Common.Annotation.WarehouseReceiptOwnership;
import com.fisco.app.Common.Annotation.WarehouseStatusCheck;
import com.fisco.app.Common.Utils.JwtUtil;
import com.fisco.app.Modules.Warehouse.Entity.StockOrder;
import com.fisco.app.Modules.Warehouse.Entity.WarehouseReceipt;
import com.fisco.app.Modules.Warehouse.Mapper.StockOrderMapper;
import com.fisco.app.Modules.Warehouse.Mapper.WarehouseReceiptMapper;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;

/**
 * 仓单模块ABAC权限拦截器
 * 基于方法上的 @WarehouseReceiptOwnership、@WarehouseStatusCheck、@WarehousePermissionCheck 注解进行权限校验
 *
 * 工作流程：
 * 1. 拦截请求，检查目标方法是否有仓单相关权限注解
 * 2. 从 JWT 获取当前用户的企业ID（entId）和角色（role）
 * 3. 根据注解类型执行对应的校验逻辑：
 *    - @WarehouseReceiptOwnership: 校验仓单是否属于当前企业
 *    - @WarehouseStatusCheck: 校验仓单状态是否满足操作条件
 *    - @WarehousePermissionCheck: 校验企业角色是否匹配
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
public class WarehouseABACInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WarehouseReceiptMapper warehouseReceiptMapper;
    private final StockOrderMapper stockOrderMapper;

    public WarehouseABACInterceptor(WarehouseReceiptMapper warehouseReceiptMapper, StockOrderMapper stockOrderMapper) {
        this.warehouseReceiptMapper = warehouseReceiptMapper;
        this.stockOrderMapper = stockOrderMapper;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler)
            throws Exception {

        // 1. 只处理 Controller 方法
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 2. 获取 JWT Claims
        Claims claims = (Claims) request.getAttribute(JwtAuthenticationFilter.ATTR_CLAIMS);
        if (claims == null) {
            log.warn("JWT Claims为空，无法进行仓单权限校验: {}", request.getRequestURI());
            sendForbiddenResponse(response, "Authentication required");
            return false;
        }

        // 3. 检查是否有仓单/入库单权限注解
        WarehouseReceiptOwnership ownership = handlerMethod.getMethodAnnotation(WarehouseReceiptOwnership.class);
        WarehouseStatusCheck statusCheck = handlerMethod.getMethodAnnotation(WarehouseStatusCheck.class);
        WarehousePermissionCheck permissionCheck = handlerMethod.getMethodAnnotation(WarehousePermissionCheck.class);
        StockOrderOwnership stockOrderOwnership = handlerMethod.getMethodAnnotation(StockOrderOwnership.class);

        // 没有仓单/入库单相关注解，放行
        if (ownership == null && statusCheck == null && permissionCheck == null && stockOrderOwnership == null) {
            return true;
        }

        // 4. 获取用户权限范围
        Integer scope = JwtUtil.getScope(claims);

        // 5. 系统管理员(scope=1)可绕过所有校验（如果允许）
        boolean adminBypass = (ownership != null && ownership.adminBypass())
                || (permissionCheck != null && permissionCheck.adminBypass());
        if (adminBypass && Objects.equals(1, scope)) {
            log.debug("系统管理员跳过仓单权限校验，URI: {}", request.getRequestURI());
            return true;
        }

        // 6. 执行仓单归属校验
        if (ownership != null) {
            if (!checkOwnership(request, response, ownership, claims)) {
                return false;
            }
        }

        // 7. 执行仓单状态校验
        if (statusCheck != null) {
            if (!checkStatus(request, response, statusCheck)) {
                return false;
            }
        }

        // 8. 执行仓单权限校验
        if (permissionCheck != null) {
            if (!checkPermission(request, response, permissionCheck, claims)) {
                return false;
            }
        }

        // 9. 执行入库单归属校验
        if (stockOrderOwnership != null) {
            if (!checkStockOrderOwnership(request, response, stockOrderOwnership, claims)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 校验入库单归属
     */
    private boolean checkStockOrderOwnership(HttpServletRequest request, HttpServletResponse response, StockOrderOwnership annotation, Claims claims)
            throws IOException {

        Long userEntId = JwtUtil.getEntId(claims);
        if (userEntId == null) {
            log.warn("用户无企业ID，无法进行入库单归属校验");
            sendForbiddenResponse(response, "Enterprise information required");
            return false;
        }

        // 支持数字ID或字符串stockNo查询
        StockOrder stockOrder = getStockOrder(request, annotation.paramName());
        if (stockOrder == null) {
            log.debug("入库单参数 {} 不存在或格式错误，跳过归属校验", annotation.paramName());
            return true;
        }

        if (!Objects.equals(stockOrder.getEntId(), userEntId)) {
            String errorMsg = annotation.errorMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = "Access denied: not the stock order owner";
            }
            log.warn("入库单归属校验失败，当前企业ID: {}, 入库单持有人: {}, 入库单ID: {}",
                    userEntId, stockOrder.getEntId(), stockOrder.getId());
            sendForbiddenResponse(response, errorMsg);
            return false;
        }

        log.debug("入库单归属校验通过，企业ID: {}, 入库单ID: {}", userEntId, stockOrder.getId());
        return true;
    }

    /**
     * 根据ID或stockNo获取入库单
     */
    private StockOrder getStockOrder(HttpServletRequest request, String paramName) {
        if (paramName == null || paramName.isEmpty()) {
            return null;
        }

        // 从PathVariable获取
        Object uriVariables = request.getAttribute("org.springframework.web.servlet.HandlerMapping.uriTemplateVariables");
        String value = null;
        if (uriVariables instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> pathVars = (Map<String, String>) uriVariables;
            value = pathVars.get(paramName);
        }

        // 如果PathVariable没有，从RequestParam获取
        if (value == null || value.isEmpty()) {
            value = request.getParameter(paramName);
        }

        if (value == null || value.isEmpty()) {
            return null;
        }

        // 判断是否为数字（ID）
        if (value.matches("^\\d+$")) {
            try {
                Long id = Long.parseLong(value);
                return stockOrderMapper.selectById(id);
            } catch (NumberFormatException e) {
                log.warn("PathVariable {} 无法转换为Long: {}", paramName, value);
            }
        } else {
            // 字符串按stockNo查询
            return stockOrderMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StockOrder>()
                    .eq(StockOrder::getStockNo, value)
            );
        }

        return null;
    }

    /**
     * 校验仓单归属
     */
    private boolean checkOwnership(HttpServletRequest request, HttpServletResponse response, WarehouseReceiptOwnership annotation, Claims claims)
            throws IOException {

        Long userEntId = JwtUtil.getEntId(claims);
        if (userEntId == null) {
            log.warn("用户无企业ID，无法进行仓单归属校验");
            sendForbiddenResponse(response, "Enterprise information required");
            return false;
        }

        Long receiptId = getReceiptId(request, annotation.paramName(), annotation.fromBody());
        if (receiptId == null) {
            log.debug("仓单ID参数 {} 不存在，跳过归属校验", annotation.paramName());
            return true;
        }

        WarehouseReceipt receipt = warehouseReceiptMapper.selectById(receiptId);
        if (receipt == null) {
            log.warn("仓单不存在: {}", receiptId);
            sendForbiddenResponse(response, "Receipt not found");
            return false;
        }

        if (!Objects.equals(receipt.getOwnerEntId(), userEntId)) {
            String errorMsg = annotation.errorMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = "Access denied: not the receipt owner";
            }
            log.warn("仓单归属校验失败，当前企业ID: {}, 仓单持有人: {}, 仓单ID: {}",
                    userEntId, receipt.getOwnerEntId(), receiptId);
            sendForbiddenResponse(response, errorMsg);
            return false;
        }

        log.debug("仓单归属校验通过，企业ID: {}, 仓单ID: {}", userEntId, receiptId);
        return true;
    }

    /**
     * 校验仓单状态
     */
    private boolean checkStatus(HttpServletRequest request, HttpServletResponse response, WarehouseStatusCheck annotation)
            throws IOException {

        Long receiptId = getReceiptId(request, annotation.paramName(), false);
        if (receiptId == null) {
            log.debug("仓单ID参数 {} 不存在，跳过状态校验", annotation.paramName());
            return true;
        }

        WarehouseReceipt receipt = warehouseReceiptMapper.selectById(receiptId);
        if (receipt == null) {
            log.warn("仓单不存在: {}", receiptId);
            sendForbiddenResponse(response, "Receipt not found");
            return false;
        }

        // 校验锁定状态
        if (annotation.requiredLocked() && receipt.getIsLocked()) {
            String errorMsg = annotation.errorMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = "仓单已锁定，无法进行此操作";
            }
            log.warn("仓单状态校验失败：仓单已锁定，仓单ID: {}", receiptId);
            sendForbiddenResponse(response, errorMsg);
            return false;
        }

        // 校验业务状态
        int[] requiredStatus = annotation.requiredStatus();
        if (requiredStatus != null && requiredStatus.length > 0) {
            boolean statusValid = false;
            for (int status : requiredStatus) {
                if (Objects.equals(receipt.getStatus(), status)) {
                    statusValid = true;
                    break;
                }
            }
            if (!statusValid) {
                String errorMsg = annotation.errorMessage();
                if (errorMsg == null || errorMsg.isEmpty()) {
                    errorMsg = "仓单状态不满足操作条件，当前状态: " + receipt.getStatus();
                }
                log.warn("仓单状态校验失败：状态不匹配，仓单ID: {}, 当前状态: {}, 要求状态: {}",
                        receiptId, receipt.getStatus(), java.util.Arrays.toString(requiredStatus));
                sendForbiddenResponse(response, errorMsg);
                return false;
            }
        }

        log.debug("仓单状态校验通过，仓单ID: {}, 锁定: {}, 状态: {}", receiptId, receipt.getIsLocked(), receipt.getStatus());
        return true;
    }

    /**
     * 校验仓单权限
     */
    private boolean checkPermission(HttpServletRequest request, HttpServletResponse response, WarehousePermissionCheck annotation, Claims claims)
            throws IOException {

        Long userEntId = JwtUtil.getEntId(claims);
        // 获取企业角色（整数类型）- 优先从entRole claim获取，其次从role字符串尝试解析
        Integer entRole = JwtUtil.getEntRole(claims);
        if (entRole == null) {
            // 兼容旧版token：尝试从role字符串解析
            String entRoleStr = JwtUtil.getRole(claims);
            if (entRoleStr != null) {
                try {
                    entRole = Integer.parseInt(entRoleStr);
                } catch (NumberFormatException e) {
                    log.warn("企业角色无法转换为整数: {}", entRoleStr);
                }
            }
        }

        // 校验企业角色
        int[] allowedRoles = annotation.allowedRoles();
        if (allowedRoles != null && allowedRoles.length > 0) {
            if (entRole == null) {
                log.warn("用户无企业角色信息");
                sendForbiddenResponse(response, "Enterprise role required");
                return false;
            }

            boolean roleValid = false;
            for (int role : allowedRoles) {
                if (Objects.equals(entRole, role)) {
                    roleValid = true;
                    break;
                }
            }

            if (!roleValid) {
                String errorMsg = annotation.errorMessage();
                if (errorMsg == null || errorMsg.isEmpty()) {
                    errorMsg = "无权限操作：需要特定企业角色";
                }
                log.warn("企业角色校验失败，当前角色: {}, 要求角色: {}", entRole, java.util.Arrays.toString(allowedRoles));
                sendForbiddenResponse(response, errorMsg);
                return false;
            }
        }

        // 校验监管方权限
        if (annotation.requireWarehouseOwner() && userEntId != null) {
            Long receiptId = getReceiptId(request, annotation.receiptIdParam(), false);
            if (receiptId != null) {
                WarehouseReceipt receipt = warehouseReceiptMapper.selectById(receiptId);
                if (receipt != null && !Objects.equals(receipt.getWarehouseEntId(), userEntId)) {
                    String errorMsg = annotation.errorMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = "无权限操作：仅监管方可执行此操作";
                    }
                    log.warn("监管方权限校验失败，当前企业ID: {}, 监管方ID: {}", userEntId, receipt.getWarehouseEntId());
                    sendForbiddenResponse(response, errorMsg);
                    return false;
                }
            }
        }

        // 校验持有人权限
        if (annotation.requireReceiptOwner() && userEntId != null) {
            Long receiptId = getReceiptId(request, annotation.receiptIdParam(), false);
            if (receiptId != null) {
                WarehouseReceipt receipt = warehouseReceiptMapper.selectById(receiptId);
                if (receipt != null && !Objects.equals(receipt.getOwnerEntId(), userEntId)) {
                    String errorMsg = annotation.errorMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = "无权限操作：仅仓单持有人可执行此操作";
                    }
                    log.warn("持有人权限校验失败，当前企业ID: {}, 持有人ID: {}", userEntId, receipt.getOwnerEntId());
                    sendForbiddenResponse(response, errorMsg);
                    return false;
                }
            }
        }

        log.debug("仓单权限校验通过，企业ID: {}, 角色: {}", userEntId, entRole);
        return true;
    }

    /**
     * 从请求中获取仓单ID
     */
    private Long getReceiptId(HttpServletRequest request, String paramName, boolean fromBody) {
        if (paramName == null || paramName.isEmpty()) {
            return null;
        }

        // 从PathVariable获取
        Object uriVariables = request.getAttribute("org.springframework.web.servlet.HandlerMapping.uriTemplateVariables");
        if (uriVariables instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> pathVars = (Map<String, String>) uriVariables;
            String value = pathVars.get(paramName);
            if (value != null) {
                try {
                    return Long.parseLong(value);
                } catch (NumberFormatException e) {
                    log.warn("PathVariable {} 无法转换为Long: {}", paramName, value);
                }
            }
        }

        // 从RequestParam获取
        String value = request.getParameter(paramName);
        if (value != null && !value.isEmpty()) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                log.warn("参数 {} 无法转换为Long: {}", paramName, value);
            }
        }

        return null;
    }

    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        if (response == null) {
            return;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> error = new HashMap<>();
        error.put("code", 403);
        error.put("message", message);

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
