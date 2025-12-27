package com.zzk.application.service;

import com.zzk.domain.model.aggregate.Prompt;
import com.zzk.domain.model.entity.PromptVersion;
import com.zzk.domain.repository.PromptRepository;
import com.zzk.domain.repository.PromptVersionRepository;
import com.zzk.domain.service.PromptDomainService;
import com.zzk.infrastructure.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PromptAppService 单元测试
 * 
 * @author zzk
 */
@ExtendWith(MockitoExtension.class)
class PromptAppServiceTest {

    @Mock
    private PromptRepository promptRepository;

    @Mock
    private PromptVersionRepository versionRepository;

    @Mock
    private PromptDomainService promptDomainService;

    @InjectMocks
    private PromptAppService promptAppService;

    private Prompt testPrompt;
    private PromptVersion testVersion;

    @BeforeEach
    void setUp() {
        testPrompt = Prompt.builder()
                .id(1L)
                .name("测试 Prompt")
                .description("这是一个测试描述")
                .workspaceId(1L)
                .creatorId(1L)
                .latestVersionId(1L)
                .isPublic(false)
                .status(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testVersion = PromptVersion.builder()
                .id(1L)
                .promptId(1L)
                .versionNumber(1)
                .content("测试内容")
                .commitMessage("初始版本")
                .authorId(1L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("创建 Prompt 成功")
    void createPrompt_Success() {
        // Given
        when(promptDomainService.commit(anyLong(), anyString(), isNull(), anyString(), anyLong()))
                .thenReturn(testVersion);

        // When
        Prompt result = promptAppService.createPrompt(
                "测试 Prompt", "描述", "内容", 1L, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("测试 Prompt");
        verify(promptRepository).save(any(Prompt.class));
        verify(promptDomainService).commit(anyLong(), eq("内容"), isNull(), eq("初始版本"), eq(1L));
    }

    @Test
    @DisplayName("获取 Prompt 详情成功")
    void getPromptById_Success() {
        // Given
        when(promptRepository.findById(1L)).thenReturn(Optional.of(testPrompt));

        // When
        Prompt result = promptAppService.getPromptById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("测试 Prompt");
    }

    @Test
    @DisplayName("获取不存在的 Prompt 抛出异常")
    void getPromptById_NotFound() {
        // Given
        when(promptRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> promptAppService.getPromptById(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Prompt 不存在");
    }

    @Test
    @DisplayName("获取工作空间的 Prompt 列表")
    void getPromptsByWorkspace_Success() {
        // Given
        when(promptRepository.findByWorkspaceId(1L)).thenReturn(List.of(testPrompt));
        when(versionRepository.findById(1L)).thenReturn(Optional.of(testVersion));

        // When
        List<Prompt> result = promptAppService.getPromptsByWorkspace(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLatestVersionNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("获取空工作空间返回空列表")
    void getPromptsByWorkspace_Empty() {
        // Given
        when(promptRepository.findByWorkspaceId(1L)).thenReturn(Collections.emptyList());

        // When
        List<Prompt> result = promptAppService.getPromptsByWorkspace(1L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("获取最新版本成功")
    void getLatestVersion_Success() {
        // Given
        when(promptRepository.findById(1L)).thenReturn(Optional.of(testPrompt));
        when(versionRepository.findById(1L)).thenReturn(Optional.of(testVersion));

        // When
        PromptVersion result = promptAppService.getLatestVersion(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getVersionNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("获取版本历史")
    void getVersionHistory_Success() {
        // Given
        List<PromptVersion> versions = List.of(testVersion);
        when(promptDomainService.getVersionHistory(1L)).thenReturn(versions);

        // When
        List<PromptVersion> result = promptAppService.getVersionHistory(1L);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("删除 Prompt 成功")
    void deletePrompt_Success() {
        // Given
        when(promptRepository.findById(1L)).thenReturn(Optional.of(testPrompt));

        // When
        promptAppService.deletePrompt(1L, 1L);

        // Then
        verify(promptRepository).deleteById(1L);
    }

    @Test
    @DisplayName("无权限删除 Prompt 抛出异常")
    void deletePrompt_NoPermission() {
        // Given
        testPrompt.setCreatorId(999L); // 其他用户创建的
        when(promptRepository.findById(1L)).thenReturn(Optional.of(testPrompt));

        // When & Then
        assertThatThrownBy(() -> promptAppService.deletePrompt(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("没有删除权限");
    }
}
