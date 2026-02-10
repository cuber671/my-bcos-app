package com.fisco.app.controller.enterprise;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fisco.app.dto.enterprise.EnterpriseCreditScoreDTO;
import com.fisco.app.dto.enterprise.EnterpriseProfileDTO;
import com.fisco.app.dto.enterprise.UpdateRatingRequest;
import com.fisco.app.entity.enterprise.Enterprise;
import com.fisco.app.entity.user.Admin;
import com.fisco.app.security.RequireAdmin;
import com.fisco.app.security.annotations.RequireEnterpriseAccess;
import com.fisco.app.service.enterprise.EnterpriseService;
import com.fisco.app.vo.Result;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;

/**
 * 企业画像与信用评分Controller
 * 提供企业画像、信用评分查询和评级更新功能
 */
@Slf4j
@RestController
@RequestMapping("/api/enterprise")
@RequiredArgsConstructor
@Validated
@Api(tags = "企业画像与信用")
public class EnterpriseProfileController {

    private final EnterpriseService enterpriseService;

    /**
     * GET /api/enterprise/{id}/profile
     * 获取企业画像
     *
     * 权限：
     * - 企业用户：只能查看自己的画像
     * - 管理员：可以查看所有企业的画像
     */
    @GetMapping("/{id}/profile")
    @ApiOperation(value = "获取企业画像",
                  notes = "查询企业的详细画像信息，包括经营状况、信用状况、交易习惯等")
    @RequireEnterpriseAccess(param = "id", allowSystemAdmin = true)
    public Result<EnterpriseProfileDTO> getProfile(
            @ApiParam(value = "企业ID", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @NonNull @PathVariable String id,
            HttpServletRequest request) {

        log.info("请求获取企业画像: enterpriseId={}", id);

        try {
            EnterpriseProfileDTO profile = enterpriseService.getEnterpriseProfile(id);
            log.info("获取企业画像成功: enterpriseId={}, name={}", id, profile.getName());
            return Result.success(profile);
        } catch (Exception e) {
            log.error("获取企业画像失败: enterpriseId={}, error={}", id, e.getMessage(), e);
            return Result.error("获取企业画像失败: " + e.getMessage());
        }
    }

    /**
     * GET /api/enterprise/{id}/credit
     * 获取信用评分
     *
     * 权限：
     * - 企业用户：只能查看自己的信用评分
     * - 管理员：可以查看所有企业的信用评分
     */
    @GetMapping("/{id}/credit")
    @ApiOperation(value = "获取信用评分",
                  notes = "查询企业的信用评分和信用历史")
    @RequireEnterpriseAccess(param = "id", allowSystemAdmin = true)
    public Result<EnterpriseCreditScoreDTO> getCreditScore(
            @ApiParam(value = "企业ID", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @NonNull @PathVariable String id,
            HttpServletRequest request) {

        log.info("请求获取信用评分: enterpriseId={}", id);

        try {
            EnterpriseCreditScoreDTO score = enterpriseService.getCreditScore(id);
            log.info("获取信用评分成功: enterpriseId={}, currentRating={}, level={}",
                     id, score.getCurrentRating(), score.getRatingLevel());
            return Result.success(score);
        } catch (Exception e) {
            log.error("获取信用评分失败: enterpriseId={}, error={}", id, e.getMessage(), e);
            return Result.error("获取信用评分失败: " + e.getMessage());
        }
    }

    /**
     * POST /api/enterprise/{id}/rating
     * 更新信用评级
     *
     * 权限：仅管理员可操作
     */
    @PostMapping("/{id}/rating")
    @ApiOperation(value = "更新信用评级",
                  notes = "管理员更新企业的信用评级，记录变更原因和历史")
    @RequireAdmin(RequireAdmin.AdminRole.ADMIN)
    public Result<String> updateRating(
            @ApiParam(value = "企业ID", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @NonNull @PathVariable String id,
            @Valid @RequestBody UpdateRatingRequest request,
            HttpServletRequest httpRequest) {

        Admin admin = (Admin) httpRequest.getAttribute("currentAdmin");
        log.info("请求更新信用评级: enterpriseId={}, newRating={}, reason={}, operator={}",
                 id, request.getCreditRating(), request.getReason(), admin.getUsername());

        try {
            // 获取企业信息
            Enterprise enterprise = enterpriseService.getEnterpriseById(id);

            // 更新评级
            enterpriseService.updateCreditRating(
                enterprise.getAddress(),
                request.getCreditRating(),
                request.getReason(),
                admin.getUsername()
            );

            log.info("信用评级更新成功: enterpriseId={}, name={}, oldRating={}, newRating={}",
                     id, enterprise.getName(),
                     enterprise.getCreditRating() - request.getCreditRating() + request.getCreditRating(),
                     request.getCreditRating());

            return Result.success("信用评级更新成功");
        } catch (Exception e) {
            log.error("更新信用评级失败: enterpriseId={}, error={}", id, e.getMessage(), e);
            return Result.error("更新信用评级失败: " + e.getMessage());
        }
    }
}
