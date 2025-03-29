package com.aiconvert.controller;

import com.aiconvert.common.ApiResponse;
import com.aiconvert.entity.FileUpload;
import com.aiconvert.entity.ConversionRecord;
import com.aiconvert.service.FileService;
import com.aiconvert.entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;


@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private FileService fileService;

    @Value("${claude.api.prompts.default}")
    private String defaultPrompt;

    @Value("${claude.api.data_prompts.default}")
    private String defaultDataPrompt;

    @Value("${claude.api.user_prompts.default}")
    private String defaultUserPrompt;

    @Value("${claude.api.ui_prompts.default}")
    private String defaultUiPrompt;
    

    @PostMapping("/upload")
    public ApiResponse<FileUpload> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId) throws Exception {
        FileUpload fileUpload = fileService.uploadDataFile(file, userId,defaultPrompt);
        return ApiResponse.success(fileUpload);
    }

    
    @PostMapping("/uploadData")
    public ApiResponse<FileUpload> uploaduploadDataFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId) throws Exception {
        FileUpload fileUpload = fileService.uploadDataFile(file, userId,defaultDataPrompt);
        return ApiResponse.success(fileUpload);
    }

    @PostMapping("/uploadUserData")
    public ApiResponse<FileUpload> uploaduploadUserFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId) throws Exception {
        FileUpload fileUpload = fileService.uploadDataFile(file, userId,defaultUserPrompt);
        return ApiResponse.success(fileUpload);
    }


    
    @PostMapping("/stringUiData")
    public ApiResponse<Result> stringUiData(
            @RequestParam("content") String dataString) throws Exception {
            Result  result = fileService.uploadStringData(dataString,defaultUiPrompt);
        return ApiResponse.success(result);
    }
 
    @PostMapping("/uploadUiData")
    public ApiResponse<FileUpload> uploaduploadUiFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId) throws Exception {
        FileUpload fileUpload = fileService.uploadDataFile(file, userId,defaultUiPrompt);
        return ApiResponse.success(fileUpload);
    }
    

    @GetMapping("/conversion/{fileId}")
    public ApiResponse<ConversionRecord> getConversionResult(@PathVariable Long fileId) {
        ConversionRecord record = fileService.getConversionResult(fileId);
        return ApiResponse.success(record);
    }
} 