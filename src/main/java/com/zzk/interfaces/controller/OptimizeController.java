package com.zzk.interfaces.controller;

import com.zzk.application.service.OptimizeAppService;
import com.zzk.interfaces.dto.request.OptimizePromptRequest;
import com.zzk.interfaces.dto.response.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/optimize")
@RequiredArgsConstructor
public class OptimizeController {

    private final OptimizeAppService optimizeAppService;

    @PostMapping
    public Result<String> optimizePrompt(@RequestAttribute("userId") Long userId,
                                         @Valid @RequestBody OptimizePromptRequest request) {
        String optimizedContent = optimizeAppService.optimize(
                request.getOriginalContent(), 
                userId, 
                request.getModelId()
        );
        return Result.success(optimizedContent);
    }
}
