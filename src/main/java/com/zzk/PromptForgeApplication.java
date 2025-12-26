package com.zzk;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Prompt-Forge 企业级 Prompt 协同平台
 * 
 * <p>核心功能：
 * <ul>
 *   <li>多模型竞技场 (Arena) - 并行调用多个 AI 模型对比效果</li>
 *   <li>Prompt 版本控制 - 类 Git 的链式版本管理</li>
 *   <li>多租户工作空间 - 企业级权限隔离</li>
 * </ul>
 * 
 * @author zzk
 * @since 1.0.0
 */
@SpringBootApplication
@EnableAsync
@MapperScan("com.zzk.infrastructure.persistence.mapper")
public class PromptForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(PromptForgeApplication.class, args);
        System.out.println("""
            
            ╔═══════════════════════════════════════════════════════════════╗
            ║                                                               ║
            ║   🚀 Prompt-Forge 启动成功!                                    ║
            ║                                                               ║
            ║   📋 API 文档: http://localhost:8080/swagger-ui.html          ║
            ║   📊 健康检查: http://localhost:8080/actuator/health          ║
            ║                                                               ║
            ╚═══════════════════════════════════════════════════════════════╝
            """);
    }
}
