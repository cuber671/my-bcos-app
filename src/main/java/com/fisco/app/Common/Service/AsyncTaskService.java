package com.fisco.app.Common.Service;

import com.fisco.app.Common.Utils.AsyncTaskResult;

/**
 * 异步任务服务接口
 * 提供异步任务提交和状态查询功能
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
public interface AsyncTaskService {

    /**
     * 提交异步任务
     *
     * @param task     任务执行逻辑
     * @param <T>      返回值类型
     * @return 任务ID
     */
    <T> String submit(AsyncTask<T> task);

    /**
     * 提交异步任务（带描述）
     *
     * @param task        任务执行逻辑
     * @param description 任务描述
     * @param <T>         返回值类型
     * @return 任务ID
     */
    <T> String submit(AsyncTask<T> task, String description);

    /**
     * 获取任务结果
     *
     * @param taskId 任务ID
     * @return 任务结果
     */
    AsyncTaskResult<?> getTaskResult(String taskId);

    /**
     * 取消任务
     *
     * @param taskId 任务ID
     * @return 是否取消成功
     */
    boolean cancelTask(String taskId);

    /**
     * 清理所有任务缓存
     */
    void clearAll();

    /**
     * 异步任务执行接口
     *
     * @param <T> 返回值类型
     */
    @FunctionalInterface
    interface AsyncTask<T> {
        /**
         * 执行异步任务
         *
         * @return 任务结果
         * @throws Exception 任务执行异常
         */
        T execute() throws Exception;
    }
}
