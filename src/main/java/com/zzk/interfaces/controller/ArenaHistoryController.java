package com.zzk.interfaces.controller;

import com.zzk.application.service.ArenaHistoryAppService;
import com.zzk.interfaces.dto.response.ArenaHistoryDTO;
import com.zzk.interfaces.dto.response.ArenaHistoryDetailDTO;
import com.zzk.interfaces.dto.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 竞技场历史控制器
 * 
 * @author zzk
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/arena/history")
@RequiredArgsConstructor
public class ArenaHistoryController {
    
    private final ArenaHistoryAppService arenaHistoryAppService;
    
    /**
     * 获取竞技历史列表
     */
    @GetMapping
    public Result<List<ArenaHistoryDTO>> getHistory(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "20") int limit) {
        List<ArenaHistoryDTO> history = arenaHistoryAppService.getHistory(userId, limit);
        return Result.success(history);
    }
    
    /**
     * 获取竞技详情
     */
    @GetMapping("/{sessionId}")
    public Result<ArenaHistoryDetailDTO> getDetail(@PathVariable Long sessionId) {
        ArenaHistoryDetailDTO detail = arenaHistoryAppService.getDetail(sessionId);
        return Result.success(detail);
    }
}
