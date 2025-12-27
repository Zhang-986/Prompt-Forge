package com.zzk.interfaces.controller;

import com.zzk.application.service.UserModelConfigAppService;
import com.zzk.domain.model.entity.UserModelConfig;
import com.zzk.interfaces.dto.response.Result;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户模型配置控制器
 * 
 * @author zzk
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/user/model-configs")
@RequiredArgsConstructor
public class UserModelConfigController {

    private final UserModelConfigAppService configService;

    /**
     * 获取支持的提供商列表
     */
    @GetMapping("/providers")
    public Result<List<UserModelConfigAppService.ProviderInfo>> getProviders() {
        return Result.success(configService.getSupportedProviders());
    }

    /**
     * 获取当前用户的所有配置
     */
    @GetMapping
    public Result<List<UserModelConfig>> getConfigs(@RequestAttribute("userId") Long userId) {
        return Result.success(configService.getUserConfigs(userId));
    }

    /**
     * 获取当前用户启用的配置
     */
    @GetMapping("/enabled")
    public Result<List<UserModelConfig>> getEnabledConfigs(@RequestAttribute("userId") Long userId) {
        return Result.success(configService.getEnabledConfigs(userId));
    }

    /**
     * 创建配置
     */
    @PostMapping
    public Result<UserModelConfig> createConfig(@RequestAttribute("userId") Long userId,
                                                  @RequestBody CreateConfigRequest request) {
        UserModelConfig config = configService.createConfig(
                userId,
                request.getProvider(),
                request.getApiKey(),
                request.getBaseUrl(),
                request.getModelName()
        );
        return Result.success(config);
    }

    /**
     * 更新配置
     */
    @PutMapping("/{id}")
    public Result<UserModelConfig> updateConfig(@RequestAttribute("userId") Long userId,
                                                  @PathVariable Long id,
                                                  @RequestBody UpdateConfigRequest request) {
        UserModelConfig config = configService.updateConfig(
                id,
                userId,
                request.getApiKey(),
                request.getBaseUrl(),
                request.getModelName(),
                request.getEnabled()
        );
        return Result.success(config);
    }

    /**
     * 删除配置
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteConfig(@RequestAttribute("userId") Long userId,
                                      @PathVariable Long id) {
        configService.deleteConfig(id, userId);
        return Result.success(null);
    }

    /**
     * 切换启用状态
     */
    @PostMapping("/{id}/toggle")
    public Result<UserModelConfig> toggleEnabled(@RequestAttribute("userId") Long userId,
                                                   @PathVariable Long id) {
        return Result.success(configService.toggleEnabled(id, userId));
    }

    @Data
    public static class CreateConfigRequest {
        private String provider;
        private String apiKey;
        private String baseUrl;
        private String modelName;
    }

    @Data
    public static class UpdateConfigRequest {
        private String apiKey;
        private String baseUrl;
        private String modelName;
        private Boolean enabled;
    }
}
