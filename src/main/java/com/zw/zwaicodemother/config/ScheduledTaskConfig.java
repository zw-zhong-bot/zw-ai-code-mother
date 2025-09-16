package com.zw.zwaicodemother.config;

import com.zw.zwaicodemother.common.WebScreenshotUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 定时任务配置类
 * <p>
 * 功能：
 * 1. 使用Spring Scheduler定时清理本地过期的临时封面文件
 * 2. 在应用删除操作时自动删除关联的封面图
 * 3. 仅清理过期临时文件（24小时过期阈值）
 * 4. 异常处理记录完整日志
 * 5. 定时任务配置为每日凌晨2点执
 * 属性：
 * - 无特殊属性，依赖WebScreenshotUtils工具类
 * 方法：
 * - cleanupExpiredTempFiles(): 定时清理过期临时文件的核心方法
 * 注意事项：
 * - 定时任务在每日凌晨2点执行，避免业务高峰期
 * - 使用WebScreenshotUtils.cleanupTempFiles()实现核心清理逻辑
 * - 保持现有代码结构不变，仅添加定时任务功能
 * - 异常处理完整，确保定时任务稳定运行
 *
 * @author ZW
 * @version 1.0
 * @since 2025-09-16
 */
@Configuration
@EnableScheduling
@Slf4j
public class ScheduledTaskConfig {

    /**
     * 定时清理过期的临时封面文件
     * 功能：
     * 1. 每日凌晨2点自动执行清理任务
     * 2. 清理超过24小时的临时文件和目录
     * 3. 记录清理过程和结果日志
     * 4. 异常处理确保任务稳定性
     * 执行时间：每日凌晨2点（cron表达式：0 0 2 * * ?）
     * - 秒：0
     * - 分：0  
     * - 时：2
     * - 日：*（每日）
     * - 月：*（每月）
     * - 周：?（不指定）
     * 异常处理：
     * - 捕获所有异常并记录完整日志
     * - 异常不会影响后续定时任务的执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredTempFiles() {
        log.info("=== 开始执行定时清理过期临时文件任务 ===");
        
        try {
            // 记录任务开始时间
            long startTime = System.currentTimeMillis();
            
            // 调用WebScreenshotUtils的cleanupTempFiles方法执行核心清理逻辑
            int deletedCount = WebScreenshotUtils.cleanupTempFiles();
            
            // 计算任务执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 记录任务执行结果
            log.info("定时清理任务执行完成，共清理 {} 个过期文件/目录，耗时 {} 毫秒", 
                deletedCount, executionTime);
            
            // 如果清理了文件，记录详细信息
            if (deletedCount > 0) {
                log.info("成功清理了 {} 个过期的临时文件/目录，释放了磁盘空间", deletedCount);
            } else {
                log.info("没有发现需要清理的过期临时文件");
            }
            
        } catch (Exception e) {
            // 记录完整的异常信息
            log.error("执行定时清理过期临时文件任务时发生异常", e);
            log.error("异常详情 - 异常类型: {}, 异常消息: {}", 
                e.getClass().getSimpleName(), e.getMessage());
            
            // 记录堆栈跟踪信息（仅在debug级别）
            if (log.isDebugEnabled()) {
                log.debug("异常堆栈跟踪:", e);
            }
            
            // 注意：这里不重新抛出异常，确保定时任务框架不会停止后续的任务执行
        } finally {
            log.info("=== 定时清理过期临时文件任务结束 ===");
        }
    }
}