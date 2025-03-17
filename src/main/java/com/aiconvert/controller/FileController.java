package com.aiconvert.controller;

import com.aiconvert.common.ApiResponse;
import com.aiconvert.entity.FileUpload;
import com.aiconvert.entity.ConversionRecord;
import com.aiconvert.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private FileService fileService;

    @PostMapping("/upload")
    public ApiResponse<FileUpload> uploadFile(@RequestParam("file") MultipartFile file) throws Exception {
        FileUpload fileUpload = fileService.uploadFile(file);
        return ApiResponse.success(fileUpload);
    }

    @GetMapping("/conversion/{fileId}")
    public ApiResponse<ConversionRecord> getConversionResult(@PathVariable Long fileId) {
        ConversionRecord record = fileService.getConversionResult(fileId);
        return ApiResponse.success(record);
    }
} 