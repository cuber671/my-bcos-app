package com.fisco.app.Common.Utils;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fisco.app.Common.Enums.AsyncTaskStatus;

import lombok.Data;

/**
 * 异步任务结果类
 * 用于存储异步任务的执行状态和结果
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Data
public class AsyncTaskResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务状态
     */
    private AsyncTaskStatus status;

    /**
     * 任务执行结果
     */
    private T result;

    /**
     * 错误信息（失败时）
     */
    private String errorMessage;

    /**
     * 已重试次数
     */
    private Integer retryCount;

    /**
     * 任务创建时间
     */
    private LocalDateTime createTime;

    /**
     * 任务更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 任务描述
     */
    private String description;

    public AsyncTaskResult() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        this.retryCount = 0;
        this.status = AsyncTaskStatus.PENDING;
    }

    public AsyncTaskResult(String taskId) {
        this();
        this.taskId = taskId;
    }

    /**
     * 设置为处理中状态
     */
    public void setProcessing() {
        this.status = AsyncTaskStatus.PROCESSING;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 设置为成功状态
     */
    public void setSuccess(T result) {
        this.status = AsyncTaskStatus.SUCCESS;
        this.result = result;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 设置为失败状态
     */
    public void setFailed(String errorMessage) {
        this.status = AsyncTaskStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 设置为超时状态
     */
    public void setTimeout() {
        this.status = AsyncTaskStatus.TIMEOUT;
        this.errorMessage = "任务执行超时";
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 增加重试次数
     */
    public void incrementRetry() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 判断任务是否完成
     */
    public boolean isCompleted() {
        return status == AsyncTaskStatus.SUCCESS ||
               status == AsyncTaskStatus.FAILED ||
               status == AsyncTaskStatus.TIMEOUT;
    }

    /**
     * 判断任务是否成功
     */
    public boolean isSuccess() {
        return status == AsyncTaskStatus.SUCCESS;
    }
}
