package com.zw.zwaicodemother.manager;

import com.zw.zwaicodemother.common.WebScreenshotUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 截图管理器
 * 功能：
 * 1. 使用任务队列机制，将截图请求按顺序排队处理
 * 2. 通过单线程执行器确保所有截图任务串行执行
 * 3. 采用CompletableFuture提交任务，支持异步返回结果
 * 4. 提供任务统计和监控功能
 * 5. 线程安全的任务管理机制
 * 属性：
 * - screenshotExecutor: 单线程执行器，确保截图任务串行执行
 * - taskCounter: 任务计数器，用于生成唯一任务ID
 * - completedTasks: 已完成任务计数
 * - failedTasks: 失败任务计数
 * 方法：
 * - takeScreenshotAsync(String): 异步截图方法，返回CompletableFuture
 * - getTaskStatistics(): 获取任务统计信息
 * - init(): 初始化方法
 * - destroy(): 销毁方法
 * 注意事项：
 * - 使用单线程执行器确保WebDriver线程安全
 * - 所有截图任务按提交顺序串行执行
 * - 支持异步调用，不阻塞业务线程
 * - 应用关闭时自动清理资源
 * 
 * @author ZW
 * @version 1.0
 * @since 2025-09-16
 */
@Component
@Slf4j
public class ScreenshotManager {

    /**
     * 单线程执行器，确保所有截图任务串行执行
     * 使用newSingleThreadExecutor()创建，保证线程安全
     */
    private ExecutorService screenshotExecutor;

    /**
     * 任务计数器，用于生成唯一的任务ID
     * 使用AtomicLong确保线程安全的递增
     */
    private final AtomicLong taskCounter = new AtomicLong(0);

    /**
     * 已完成任务计数器
     * 统计成功完成的截图任务数量
     */
    private final AtomicInteger completedTasks = new AtomicInteger(0);

    /**
     * 失败任务计数器
     * 统计执行失败的截图任务数量
     */
    private final AtomicInteger failedTasks = new AtomicInteger(0);

    /**
     * 初始化截图管理器
     * 功能：
     * 1. 创建单线程执行器
     * 2. 配置线程名称便于调试
     * 3. 记录初始化日志
     * 执行时机：Spring容器初始化完成后自动调用
     * 异常处理：
     * - 捕获初始化异常并记录日志
     * - 初始化失败不影响应用启动
     */
    @PostConstruct
    public void init() {
        try {
            // 创建单线程执行器，确保截图任务串行执行
            screenshotExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r);
                thread.setName("screenshot-worker-thread");
                thread.setDaemon(true); // 设置为守护线程
                return thread;
            });
            
            log.info("截图管理器初始化成功，单线程执行器已创建");
            
        } catch (Exception e) {
            log.error("截图管理器初始化失败", e);
            throw new RuntimeException("截图管理器初始化失败", e);
        }
    }

    /**
     * 异步执行网页截图任务
     * <p>
     * 功能：
     * 1. 将截图请求提交到任务队列
     * 2. 使用单线程执行器串行处理任务
     * 3. 返回CompletableFuture支持异步调用
     * 4. 自动统计任务执行情况
     * <p>
     * 参数：
     * @param webUrl 要截图的网页URL，不能为空
     * <p>
     * 返回值：
     * @return CompletableFuture<String> 异步返回截图文件路径
     *         - 成功：返回压缩后的截图文件完整路径
     *         - 失败：返回null
     * <p>
     * 异常处理：
     * - 参数验证异常：立即返回失败的CompletableFuture
     * - 执行异常：在Future中处理，不影响其他任务
     * - 线程池异常：记录日志并返回失败结果
     * <p>
     * 线程安全：
     * - 使用单线程执行器确保WebDriver线程安全
     * - 任务ID生成使用原子操作
     * - 统计计数器使用原子操作
     */
    public CompletableFuture<String> takeScreenshotAsync(String webUrl) {
        // 生成唯一任务ID
        long taskId = taskCounter.incrementAndGet();
        
        // 参数验证
        if (webUrl == null || webUrl.trim().isEmpty()) {
            log.error("任务[{}] 截图请求参数无效: webUrl为空", taskId);
            failedTasks.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        }
        
        // 检查执行器状态
        if (screenshotExecutor == null || screenshotExecutor.isShutdown()) {
            log.error("任务[{}] 截图执行器未初始化或已关闭", taskId);
            failedTasks.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        }
        
        log.info("任务[{}] 提交截图请求到队列: {}", taskId, webUrl);
        
        // 使用CompletableFuture.supplyAsync提交任务到单线程执行器
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                log.info("任务[{}] 开始执行截图: {}", taskId, webUrl);
                
                // 调用WebScreenshotUtils执行实际截图操作
                String screenshotPath = WebScreenshotUtils.saveWebPageScreenshot(webUrl);
                
                long executionTime = System.currentTimeMillis() - startTime;
                
                if (screenshotPath != null) {
                    // 截图成功
                    completedTasks.incrementAndGet();
                    log.info("任务[{}] 截图执行成功: {} -> {}, 耗时: {}ms", 
                        taskId, webUrl, screenshotPath, executionTime);
                    return screenshotPath;
                } else {
                    // 截图失败
                    failedTasks.incrementAndGet();
                    log.error("任务[{}] 截图执行失败: {}, 耗时: {}ms", 
                        taskId, webUrl, executionTime);
                    return null;
                }
                
            } catch (Exception e) {
                // 异常处理
                long executionTime = System.currentTimeMillis() - startTime;
                failedTasks.incrementAndGet();
                
                log.error("任务[{}] 截图执行异常: {}, 耗时: {}ms", 
                    taskId, webUrl, executionTime, e);
                log.error("任务[{}] 异常详情 - 异常类型: {}, 异常消息: {}", 
                    taskId, e.getClass().getSimpleName(), e.getMessage());
                
                return null;
            }
            
        }, screenshotExecutor);
    }

    /**
     * 获取任务统计信息
     * <p>
     * 功能：
     * 1. 返回当前任务执行统计
     * 2. 包含总任务数、成功数、失败数
     * 3. 计算成功率和失败率
     * <p>
     * 返回值：
     * @return TaskStatistics 任务统计信息对象
     * <p>
     * 线程安全：
     * - 使用原子操作读取计数器值
     * - 返回快照数据，不影响并发执行
     */
    public TaskStatistics getTaskStatistics() {
        long totalTasks = taskCounter.get();
        int completed = completedTasks.get();
        int failed = failedTasks.get();
        
        return new TaskStatistics(totalTasks, completed, failed);
    }

    /**
     * 销毁截图管理器
     * <p>
     * 功能：
     * 1. 优雅关闭线程池
     * 2. 等待正在执行的任务完成
     * 3. 记录最终统计信息
     * 4. 清理资源
     * <p>
     * 执行时机：Spring容器销毁前自动调用
     * <p>
     * 异常处理：
     * - 捕获关闭异常并记录日志
     * - 确保资源得到释放
     */
    @PreDestroy
    public void destroy() {
        try {
            if (screenshotExecutor != null && !screenshotExecutor.isShutdown()) {
                log.info("开始关闭截图管理器...");
                
                // 记录最终统计信息
                TaskStatistics finalStats = getTaskStatistics();
                log.info("截图管理器最终统计 - 总任务: {}, 成功: {}, 失败: {}, 成功率: {}%",
                    finalStats.totalTasks(),
                    finalStats.completedTasks(),
                    finalStats.failedTasks(),
                    finalStats.getSuccessRate());
                
                // 优雅关闭线程池
                screenshotExecutor.shutdown();
                
                // 等待正在执行的任务完成（最多等待5秒）
                if (!screenshotExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    log.warn("截图任务未在5秒内完成，强制关闭线程池");
                    screenshotExecutor.shutdownNow();
                }
                
                log.info("截图管理器已成功关闭");
            }
            
        } catch (Exception e) {
            log.error("关闭截图管理器时发生异常", e);
            
            // 强制关闭
            if (screenshotExecutor != null) {
                screenshotExecutor.shutdownNow();
            }
        }
    }

    /**
     * 任务统计信息内部类
     * <p>
     * 功能：
     * 1. 封装任务执行统计数据
     * 2. 提供成功率和失败率计算
     * 3. 支持格式化输出
     *
     * @param totalTasks     -- GETTER --
     *                       获取总任务数
     * @param completedTasks -- GETTER --
     *                       获取完成任务数
     * @param failedTasks    -- GETTER --
     *                       获取失败任务数
     */
        @Getter
        public record TaskStatistics(long totalTasks, int completedTasks, int failedTasks) {
        /**
         * 构造任务统计信息
         *
         * @param totalTasks     总任务数
         * @param completedTasks 完成任务数
         * @param failedTasks    失败任务数
         */
        public TaskStatistics {
        }

            /**
             * 计算成功率
             *
             * @return 成功率百分比
             */
            public double getSuccessRate() {
                if (totalTasks == 0) {
                    return 0.0;
                }
                return (completedTasks * 100.0) / totalTasks;
            }

            /**
             * 计算失败率
             *
             * @return 失败率百分比
             */
            public double getFailureRate() {
                if (totalTasks == 0) {
                    return 0.0;
                }
                return (failedTasks * 100.0) / totalTasks;
            }

            /**
             * 格式化输出统计信息
             *
             * @return 格式化的统计信息字符串
             */
            @Override
            public String toString() {
                return String.format("TaskStatistics{总任务=%d, 成功=%d, 失败=%d, 成功率=%.2f%%, 失败率=%.2f%%}",
                        totalTasks, completedTasks, failedTasks, getSuccessRate(), getFailureRate());
            }
        }
}