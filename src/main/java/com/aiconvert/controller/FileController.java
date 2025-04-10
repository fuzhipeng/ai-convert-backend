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


    @Value("${claude.api.tariff_prompts.default}")
    private String defaultTariffPrompt;

    @Value("${claude.api.story_prompts.default}")
    private String defaultStoryPrompt;

    @Value("${claude.api.front_prompts.default}")
    private String defaultFrontPrompt;
    
    @Value("${claude.api.photo_prompts.default}")
    private String defaultPhotoPrompt;

    @Value("${claude.api.dog_prompts.default}")
    private String defaultDogPrompt;

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


    @PostMapping("/stringTariffData")
    public ApiResponse<Result> stringTariffData(
            @RequestParam("content") String dataString) throws Exception {
            Result  result = fileService.stringTariffData(dataString,defaultTariffPrompt,"anthropic/claude-3-haiku");
        return ApiResponse.success(result);
    }


    @PostMapping("/stringStoryData")
    public ApiResponse<Result> stringStoryData(
            @RequestParam("content") String dataString) throws Exception {
            Result  result = fileService.stringTariffData(dataString,defaultStoryPrompt,"google/gemini-2.5-pro-exp-03-25:free");
        return ApiResponse.success(result);
    }

    @PostMapping("/stringFrontData")
    public ApiResponse<Result> stringFrontData(
            @RequestParam("content") String dataString) throws Exception {
        Result  result = fileService.uploadStringData(dataString,defaultFrontPrompt);
        return ApiResponse.success(result);
    }

    @PostMapping("/imagePhotoData")
    public ApiResponse<Result> imageFrontData(
            @RequestParam("file") MultipartFile file) throws Exception {
        Result result = fileService.processImageFile(file, defaultPhotoPrompt);
        return ApiResponse.success(result);
    }

    @PostMapping("/imageDogData")
    public ApiResponse<Result> imageDogData(
            @RequestParam("file") MultipartFile file) throws Exception {
        Result result = fileService.processImageFile(file, defaultDogPrompt);
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