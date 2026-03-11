package com.fisco.app.Common.Service.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fisco.app.Common.Enums.AsyncTaskStatus;
import com.fisco.app.Common.Service.AsyncTaskService;
import com.fisco.app.Common.Utils.AsyncTaskResult;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * 异步任务服务实现
 * 使用线程池执行任务，支持重试机制
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Slf4j
@Service
public class AsyncTaskServiceImpl implements AsyncTaskService {

    /**
     * 任务结果缓存
     */
    private final Cache<String, AsyncTaskResult<?>> taskCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build();

    /**
     * 异步任务执行线程池
     */
    private ExecutorService executorService;

    @Value("${async.core-pool-size:10}")
    private int corePoolSize;

    @Value("${async.max-pool-size:50}")
    private int maxPoolSize;

    @Value("${async.queue-capacity:100}")
    private int queueCapacity;

    @Value("${async.max-retry-count:3}")
    private int maxRetryCount;

    @Value("${async.task-timeout-seconds:60}")
    private int taskTimeoutSeconds;

    @PostConstruct
    public void init() {
        executorService = Executors.newFixedThreadPool(corePoolSize, new ThreadFactoryBuilder()
                .setNameFormat("async-task-%d")
                .setDaemon(false)
                .build());
        log.info("异步任务服务初始化完成，核心线程数: {}, 最大线程数: {}", corePoolSize, maxPoolSize);
    }

    @PreDestroy
    public void destroy() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("异步任务服务已关闭");
        }
    }

    @Override
    public <T> String submit(AsyncTaskService.AsyncTask<T> task) {
        return submit(task, null);
    }

    @Override
    public <T> String submit(AsyncTaskService.AsyncTask<T> task, String description) {
        // 生成任务ID
        String taskId = generateTaskId();

        // 创建任务结果对象
        AsyncTaskResult<T> taskResult = new AsyncTaskResult<>(taskId);
        if (description != null) {
            taskResult.setDescription(description);
        }

        // 放入缓存
        taskCache.put(taskId, taskResult);

        // 提交任务到线程池
        executorService.submit(() -> {
            executeTask(taskId, task);
        });

        log.info("异步任务已提交: taskId={}, description={}", taskId, description);
        return taskId;
    }

    /**
     * 执行任务（带重试机制）
     */
    @SuppressWarnings("unchecked")
    private <T> void executeTask(String taskId, AsyncTaskService.AsyncTask<T> task) {
        AsyncTaskResult<T> taskResult = (AsyncTaskResult<T>) taskCache.getIfPresent(taskId);
        if (taskResult == null) {
            log.warn("任务不存在: taskId={}", taskId);
            return;
        }

        // 设置为处理中
        taskResult.setProcessing();

        AtomicInteger retryCount = new AtomicInteger(0);

        while (retryCount.get() < maxRetryCount) {
            try {
                // 执行任务，设置超时
                T result = executeWithTimeout(() -> task.execute(), taskTimeoutSeconds, TimeUnit.SECONDS);

                // 任务成功
                taskResult.setSuccess(result);
                log.info("异步任务执行成功: taskId={}, retryCount={}", taskId, retryCount.get());
                return;

            } catch (Exception e) {
                int currentRetry = retryCount.incrementAndGet();
                log.warn("异步任务执行失败: taskId={}, retryCount={}, error={}",
                        taskId, currentRetry, e.getMessage());

                if (currentRetry >= maxRetryCount) {
                    // 重试次数用完，任务失败
                    taskResult.setFailed(e.getMessage());
                    log.error("异步任务重试次数用完，执行失败: taskId={}", taskId);
                } else {
                    // 增加重试次数
                    taskResult.incrementRetry();
                    // 短暂等待后重试
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        taskResult.setFailed("任务被中断");
                        return;
                    }
                }
            }
        }
    }

    /**
     * 带超时执行
     */
    private <T> T executeWithTimeout(Callable<T> callable, long timeout, TimeUnit unit) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            return executor.submit(callable::call).get(timeout, unit);
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 函数式接口
     */
    @FunctionalInterface
    private interface Callable<T> {
        T call() throws Exception;
    }

    @Override
    public AsyncTaskResult<?> getTaskResult(String taskId) {
        AsyncTaskResult<?> result = taskCache.getIfPresent(taskId);
        if (result == null) {
            AsyncTaskResult<?> notFound = new AsyncTaskResult<>(taskId);
            notFound.setFailed("任务不存在");
            return notFound;
        }
        return result;
    }

    @Override
    public boolean cancelTask(String taskId) {
        AsyncTaskResult<?> result = taskCache.getIfPresent(taskId);
        if (result == null) {
            return false;
        }

        if (result.getStatus() == AsyncTaskStatus.PENDING ||
            result.getStatus() == AsyncTaskStatus.PROCESSING) {
            result.setFailed("任务被取消");
            log.info("异步任务已取消: taskId={}", taskId);
            return true;
        }

        return false;
    }

    @Override
    public void clearAll() {
        taskCache.invalidateAll();
        log.info("所有异步任务缓存已清理");
    }

    /**
     * 生成任务ID
     */
    private String generateTaskId() {
        return "async_" + System.currentTimeMillis() + "_" +
               (int) (Math.random() * 10000);
    }
}
