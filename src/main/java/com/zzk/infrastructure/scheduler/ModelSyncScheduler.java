package com.zzk.infrastructure.scheduler;

import com.zzk.application.service.LobeChatSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 模型同步定时任务
 * 每周一凌晨 3 点自动从 Lobe Chat 同步模型配置
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ModelSyncScheduler {

    private final LobeChatSyncService lobeChatSyncService;

    /**
     * 每周一凌晨 3 点执行同步
     * Cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 3 * * MON")
    public void weeklySync() {
        log.info("开始执行每周模型同步任务...");
        try {
            LobeChatSyncService.SyncResult result = lobeChatSyncService.syncAllProviders();
            log.info("每周模型同步完成: 成功={}, 失败={}", result.successCount(), result.failCount());
        } catch (Exception e) {
            log.error("每周模型同步失败", e);
        }
    }
}
