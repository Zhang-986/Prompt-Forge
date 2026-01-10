package com.zzk.interfaces.controller;

import com.zzk.application.service.LobeChatSyncService;
import com.zzk.infrastructure.persistence.mapper.AvailableModelMapper;
import com.zzk.infrastructure.persistence.mapper.ModelProviderMapper;
import com.zzk.infrastructure.persistence.po.AvailableModelPO;
import com.zzk.infrastructure.persistence.po.ModelProviderPO;
import com.zzk.interfaces.dto.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin 模型管理控制器
 * 
 * <p>
 * 提供模型厂商和模型的增删改查，以及 Lobe Chat 同步功能
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/models")
@RequiredArgsConstructor
@Tag(name = "Admin-模型管理", description = "模型厂商和模型管理相关接口")
public class AdminModelController {

    private final ModelProviderMapper providerMapper;
    private final AvailableModelMapper modelMapper;
    private final LobeChatSyncService lobeChatSyncService;

    // ==================== Provider 管理 ====================

    @Operation(summary = "获取所有厂商列表")
    @GetMapping("/providers")
    public Result<List<ModelProviderPO>> getAllProviders() {
        List<ModelProviderPO> providers = providerMapper.findAllOrdered();
        return Result.success(providers);
    }

    @Operation(summary = "获取单个厂商")
    @GetMapping("/providers/{id}")
    public Result<ModelProviderPO> getProvider(@PathVariable String id) {
        ModelProviderPO provider = providerMapper.selectById(id);
        if (provider == null) {
            return Result.error("厂商不存在");
        }
        return Result.success(provider);
    }

    @Operation(summary = "创建厂商")
    @PostMapping("/providers")
    public Result<ModelProviderPO> createProvider(@Valid @RequestBody ProviderRequest request) {
        // 检查是否已存在
        if (providerMapper.selectById(request.getId()) != null) {
            return Result.error("厂商ID已存在");
        }

        ModelProviderPO provider = new ModelProviderPO();
        provider.setId(request.getId());
        provider.setName(request.getName());
        provider.setDefaultBaseUrl(request.getDefaultBaseUrl());
        provider.setDescription(request.getDescription());
        provider.setModelsUrl(request.getModelsUrl());
        provider.setSdkType(request.getSdkType() != null ? request.getSdkType() : "openai");
        provider.setEnabled(request.getEnabled() != null ? request.getEnabled() : 1);
        provider.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);

        providerMapper.insert(provider);
        log.info("创建厂商: {}", request.getId());
        return Result.success(provider);
    }

    @Operation(summary = "更新厂商")
    @PutMapping("/providers/{id}")
    public Result<ModelProviderPO> updateProvider(
            @PathVariable String id,
            @Valid @RequestBody ProviderRequest request) {
        ModelProviderPO provider = providerMapper.selectById(id);
        if (provider == null) {
            return Result.error("厂商不存在");
        }

        if (request.getName() != null)
            provider.setName(request.getName());
        if (request.getDefaultBaseUrl() != null)
            provider.setDefaultBaseUrl(request.getDefaultBaseUrl());
        if (request.getDescription() != null)
            provider.setDescription(request.getDescription());
        if (request.getModelsUrl() != null)
            provider.setModelsUrl(request.getModelsUrl());
        if (request.getSdkType() != null)
            provider.setSdkType(request.getSdkType());
        if (request.getEnabled() != null)
            provider.setEnabled(request.getEnabled());
        if (request.getSortOrder() != null)
            provider.setSortOrder(request.getSortOrder());

        providerMapper.updateById(provider);
        log.info("更新厂商: {}", id);
        return Result.success(provider);
    }

    @Operation(summary = "删除厂商")
    @DeleteMapping("/providers/{id}")
    public Result<Void> deleteProvider(@PathVariable String id) {
        ModelProviderPO provider = providerMapper.selectById(id);
        if (provider == null) {
            return Result.error("厂商不存在");
        }

        // 先删除该厂商下的所有模型
        modelMapper.deleteByProviderId(id);
        providerMapper.deleteById(id);
        log.info("删除厂商: {}", id);
        return Result.success(null);
    }

    @Operation(summary = "切换厂商启用状态")
    @PatchMapping("/providers/{id}/toggle")
    public Result<ModelProviderPO> toggleProviderEnabled(@PathVariable String id) {
        ModelProviderPO provider = providerMapper.selectById(id);
        if (provider == null) {
            return Result.error("厂商不存在");
        }

        provider.setEnabled(provider.getEnabled() == 1 ? 0 : 1);
        providerMapper.updateById(provider);
        log.info("切换厂商 {} 状态为: {}", id, provider.getEnabled() == 1 ? "启用" : "禁用");
        return Result.success(provider);
    }

    // ==================== Model 管理 ====================

    @Operation(summary = "获取所有模型列表")
    @GetMapping
    public Result<List<AvailableModelPO>> getAllModels(
            @Parameter(description = "厂商ID筛选") @RequestParam(required = false) String providerId) {
        List<AvailableModelPO> models;
        if (providerId != null && !providerId.isEmpty()) {
            models = modelMapper.findByProviderId(providerId);
        } else {
            models = modelMapper.selectList(null);
        }
        return Result.success(models);
    }

    @Operation(summary = "获取单个模型")
    @GetMapping("/{id}")
    public Result<AvailableModelPO> getModel(@PathVariable Long id) {
        AvailableModelPO model = modelMapper.selectById(id);
        if (model == null) {
            return Result.error("模型不存在");
        }
        return Result.success(model);
    }

    @Operation(summary = "创建模型")
    @PostMapping
    public Result<AvailableModelPO> createModel(@Valid @RequestBody ModelRequest request) {
        // 检查厂商是否存在
        if (providerMapper.selectById(request.getProviderId()) == null) {
            return Result.error("厂商不存在");
        }

        // 检查是否已存在
        if (modelMapper.findByProviderIdAndModelId(request.getProviderId(), request.getModelId()) != null) {
            return Result.error("该模型已存在");
        }

        AvailableModelPO model = new AvailableModelPO();
        model.setProviderId(request.getProviderId());
        model.setModelId(request.getModelId());
        model.setDisplayName(request.getDisplayName());
        model.setDescription(request.getDescription());
        model.setContextWindow(request.getContextWindow());
        model.setSupportsVision(request.getSupportsVision() != null ? request.getSupportsVision() : 0);
        model.setSupportsFunctionCall(
                request.getSupportsFunctionCall() != null ? request.getSupportsFunctionCall() : 0);
        model.setEnabled(request.getEnabled() != null ? request.getEnabled() : 1);
        model.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        model.setSource("manual");

        modelMapper.insert(model);
        log.info("创建模型: {} / {}", request.getProviderId(), request.getModelId());
        return Result.success(model);
    }

    @Operation(summary = "更新模型")
    @PutMapping("/{id}")
    public Result<AvailableModelPO> updateModel(
            @PathVariable Long id,
            @Valid @RequestBody ModelRequest request) {
        AvailableModelPO model = modelMapper.selectById(id);
        if (model == null) {
            return Result.error("模型不存在");
        }

        if (request.getDisplayName() != null)
            model.setDisplayName(request.getDisplayName());
        if (request.getDescription() != null)
            model.setDescription(request.getDescription());
        if (request.getContextWindow() != null)
            model.setContextWindow(request.getContextWindow());
        if (request.getSupportsVision() != null)
            model.setSupportsVision(request.getSupportsVision());
        if (request.getSupportsFunctionCall() != null)
            model.setSupportsFunctionCall(request.getSupportsFunctionCall());
        if (request.getEnabled() != null)
            model.setEnabled(request.getEnabled());
        if (request.getSortOrder() != null)
            model.setSortOrder(request.getSortOrder());

        modelMapper.updateById(model);
        log.info("更新模型: {}", id);
        return Result.success(model);
    }

    @Operation(summary = "删除模型")
    @DeleteMapping("/{id}")
    public Result<Void> deleteModel(@PathVariable Long id) {
        AvailableModelPO model = modelMapper.selectById(id);
        if (model == null) {
            return Result.error("模型不存在");
        }

        modelMapper.deleteById(id);
        log.info("删除模型: {}", id);
        return Result.success(null);
    }

    @Operation(summary = "切换模型启用状态")
    @PatchMapping("/{id}/toggle")
    public Result<AvailableModelPO> toggleModelEnabled(@PathVariable Long id) {
        AvailableModelPO model = modelMapper.selectById(id);
        if (model == null) {
            return Result.error("模型不存在");
        }

        model.setEnabled(model.getEnabled() == 1 ? 0 : 1);
        modelMapper.updateById(model);
        log.info("切换模型 {} 状态为: {}", id, model.getEnabled() == 1 ? "启用" : "禁用");
        return Result.success(model);
    }

    // ==================== 同步功能 ====================

    @Operation(summary = "手动触发 Lobe Chat 同步")
    @PostMapping("/sync")
    public Result<Map<String, Object>> syncFromLobeChat() {
        log.info("Admin 手动触发 Lobe Chat 同步");
        try {
            LobeChatSyncService.SyncResult result = lobeChatSyncService.syncAllProviders();

            Map<String, Object> response = new HashMap<>();
            response.put("successCount", result.successCount());
            response.put("failCount", result.failCount());
            response.put("failedProviders", result.failedProviders());

            return Result.success(response);
        } catch (Exception e) {
            log.error("Lobe Chat 同步失败", e);
            return Result.error("同步失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取同步统计")
    @GetMapping("/sync/stats")
    public Result<Map<String, Object>> getSyncStats() {
        List<ModelProviderPO> providers = providerMapper.findAllOrdered();
        List<AvailableModelPO> models = modelMapper.selectList(null);

        long enabledProviders = providers.stream().filter(p -> p.getEnabled() == 1).count();
        long enabledModels = models.stream().filter(m -> m.getEnabled() == 1).count();
        long syncedModels = models.stream().filter(m -> "sync".equals(m.getSource())).count();
        long manualModels = models.stream().filter(m -> "manual".equals(m.getSource())).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProviders", providers.size());
        stats.put("enabledProviders", enabledProviders);
        stats.put("totalModels", models.size());
        stats.put("enabledModels", enabledModels);
        stats.put("syncedModels", syncedModels);
        stats.put("manualModels", manualModels);

        return Result.success(stats);
    }

    // ==================== DTO ====================

    @Data
    public static class ProviderRequest {
        @NotBlank(message = "厂商ID不能为空")
        private String id;
        private String name;
        private String defaultBaseUrl;
        private String description;
        private String modelsUrl;
        private String sdkType;
        private Integer enabled;
        private Integer sortOrder;
    }

    @Data
    public static class ModelRequest {
        private String providerId;
        private String modelId;
        private String displayName;
        private String description;
        private Integer contextWindow;
        private Integer supportsVision;
        private Integer supportsFunctionCall;
        private Integer enabled;
        private Integer sortOrder;
    }
}
