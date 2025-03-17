package com.aiconvert.controller;

import com.aiconvert.common.ApiResponse;
import com.aiconvert.entity.ConversionRecord;
import com.aiconvert.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/convert")
public class ConvertController {

    @Autowired
    private FileService fileService;

    @PostMapping("/task")
    public ApiResponse<ConversionRecord> createConvertTask(@RequestBody Map<String, Long> params) {
        Long fileId = params.get("fileId");
        ConversionRecord record = fileService.createConversionTask(fileId);
        return ApiResponse.success(record);
    }

    @GetMapping("/progress/{taskId}")
    public ApiResponse<Map<String, Object>> getConvertProgress(@PathVariable Long taskId) {
        Map<String, Object> progress = fileService.getConversionProgress(taskId);
        return ApiResponse.success(progress);
    }
} 