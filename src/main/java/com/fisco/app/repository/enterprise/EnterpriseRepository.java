package com.fisco.app.repository.enterprise;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fisco.app.entity.enterprise.Enterprise;

/**
 * 企业Repository
 */
@Repository
public interface EnterpriseRepository extends JpaRepository<Enterprise, String>, JpaSpecificationExecutor<Enterprise> {

    /**
     * 根据区块链地址查找企业
     */
    Optional<Enterprise> findByAddress(String address);

    /**
     * 根据信用代码查找企业
     */
    Optional<Enterprise> findByCreditCode(String creditCode);

    /**
     * 根据API密钥查找企业
     */
    Optional<Enterprise> findByApiKey(String apiKey);

    /**
     * 根据用户名查找企业
     */
    Optional<Enterprise> findByUsername(String username);

    /**
     * 根据邮箱查找企业
     */
    Optional<Enterprise> findByEmail(String email);

    /**
     * 根据手机号查找企业
     */
    Optional<Enterprise> findByPhone(String phone);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 检查手机号是否存在
     */
    boolean existsByPhone(String phone);

    /**
     * 根据状态查找企业
     */
    List<Enterprise> findByStatus(Enterprise.EnterpriseStatus status);

    /**
     * 根据状态查找企业（分页）
     */
    Page<Enterprise> findByStatus(Enterprise.EnterpriseStatus status, Pageable pageable);

    /**
     * 根据状态统计企业数量
     */
    long countByStatus(Enterprise.EnterpriseStatus status);

    /**
     * 根据角色查找企业
     */
    List<Enterprise> findByRole(Enterprise.EnterpriseRole role);

    /**
     * 检查信用代码是否存在
     */
    boolean existsByCreditCode(String creditCode);

    /**
     * 检查地址是否存在
     */
    boolean existsByAddress(String address);

    /**
     * 查找所有活跃企业
     */
    @Query("SELECT e FROM Enterprise e WHERE e.status = 'ACTIVE' ORDER BY e.creditRating DESC")
    List<Enterprise> findAllActiveEnterprises();

    /**
     * 根据信用评级范围查找企业
     */
    @Query("SELECT e FROM Enterprise e WHERE e.creditRating BETWEEN :minRating AND :maxRating AND e.status = 'ACTIVE'")
    List<Enterprise> findByCreditRatingRange(@Param("minRating") Integer minRating,
                                              @Param("maxRating") Integer maxRating);

    /**
     * 根据企业ID（UUID）查询企业及其所有用户（使用JOIN FETCH避免N+1查询）
     */
    @Query("SELECT e FROM Enterprise e LEFT JOIN FETCH e.users WHERE e.id = :enterpriseId")
    Optional<Enterprise> findByIdWithUsers(@Param("enterpriseId") String enterpriseId);

    /**
     * 根据地址查询企业及其所有用户（使用JOIN FETCH避免N+1查询）
     */
    @Query("SELECT e FROM Enterprise e LEFT JOIN FETCH e.users WHERE e.address = :address")
    Optional<Enterprise> findByAddressWithUsers(@Param("address") String address);

    /**
     * 根据用户名查询企业及其所有用户（使用JOIN FETCH避免N+1查询）
     */
    @Query("SELECT e FROM Enterprise e LEFT JOIN FETCH e.users WHERE e.username = :username")
    Optional<Enterprise> findByUsernameWithUsers(@Param("username") String username);

    /**
     * 查询所有活跃企业及其用户数量
     */
    @Query("SELECT DISTINCT e FROM Enterprise e LEFT JOIN FETCH e.users WHERE e.status = 'ACTIVE' ORDER BY e.creditRating DESC")
    List<Enterprise> findAllActiveEnterprisesWithUsers();
}
