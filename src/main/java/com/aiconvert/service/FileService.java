package com.aiconvert.service;

import com.aiconvert.entity.FileUpload;
import com.aiconvert.entity.ConversionRecord;
import com.aiconvert.mapper.FileUploadMapper;
import com.aiconvert.mapper.ConversionRecordMapper;
import com.aiconvert.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

@Service
public class FileService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    
    private static final List<String> SUPPORTED_FILE_TYPES = Arrays.asList(
        "pdf", "doc", "docx", "txt", "md", "json", "xml", "html", "csv"
    );
    
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    
    @Value("${file.upload.path}")
    private String uploadPath;
    
    @Autowired
    private FileUploadMapper fileUploadMapper;
    
    @Autowired
    private ConversionRecordMapper conversionRecordMapper;
    
    @Autowired
    private ClaudeService claudeService;
    
    @Autowired
    @Qualifier("conversionExecutor")
    private Executor conversionExecutor;

    public FileUpload uploadFile(MultipartFile file) throws Exception {
        // 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            logger.error("文件大小超过限制：{}", file.getOriginalFilename());
            throw new IllegalArgumentException("文件大小不能超过50MB");
        }
        
        // 检查文件类型
        String originalFilename = file.getOriginalFilename();
        String fileType = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!SUPPORTED_FILE_TYPES.contains(fileType)) {
            logger.error("不支持的文件类型：{}", fileType);
            throw new IllegalArgumentException("不支持的文件类型：" + fileType);
        }

        logger.info("开始上传文件：{}", originalFilename);
        
        // 保存文件
        String filePath = FileUtils.saveFile(file, uploadPath);
        
        // 保存文件记录
        FileUpload fileUpload = new FileUpload();
        fileUpload.setOriginalFilename(originalFilename);
        fileUpload.setFilePath(filePath);
        fileUpload.setFileSize(file.getSize());
        fileUpload.setFileType(fileType);
        fileUpload.setUploadTime(new Date());
        fileUpload.setStatus(0); // 待转换
        
        fileUploadMapper.insert(fileUpload);
        logger.info("文件上传成功：{}", originalFilename);

        // 异步开始转换
        startConversion(fileUpload);

        return fileUpload;
    }

    @Async("conversionExecutor")
    protected void startConversion(FileUpload fileUpload) {
        logger.info("开始转换文件：{}", fileUpload.getOriginalFilename());
        
        // 更新文件状态为转换中
        fileUpload.setStatus(1);
        fileUploadMapper.updateStatus(fileUpload.getId(), 1);

        // 创建转换记录
        ConversionRecord record = new ConversionRecord();
        record.setFileId(fileUpload.getId());
        record.setStartTime(new Date());
        record.setStatus(1); // 设置初始状态为转换中
        conversionRecordMapper.insert(record);

        try {
            String htmlContent = claudeService.convertToHtml(fileUpload.getFilePath(), fileUpload.getFileType());
            
            // 更新转换记录
            record.setHtmlContent(htmlContent);
            record.setEndTime(new Date());
            record.setStatus(2); // 设置状态为转换成功
            conversionRecordMapper.update(record);

            // 更新文件状态为转换成功
            fileUploadMapper.updateStatus(fileUpload.getId(), 2);
            logger.info("文件转换成功：{}", fileUpload.getOriginalFilename());
        } catch (Exception e) {
            // 转换失败
            logger.error("文件转换失败：{}", fileUpload.getOriginalFilename(), e);
            record.setErrorMessage(e.getMessage());
            record.setEndTime(new Date());
            record.setStatus(3); // 设置状态为转换失败
            conversionRecordMapper.update(record);
            
            fileUploadMapper.updateStatus(fileUpload.getId(), 3);
        }
    }

    public ConversionRecord getConversionResult(Long fileId) {
        return conversionRecordMapper.selectByFileId(fileId);
    }

    public ConversionRecord createConversionTask(Long fileId) {
        // 获取文件记录
        FileUpload fileUpload = fileUploadMapper.selectById(fileId);
        if (fileUpload == null) {
            throw new IllegalArgumentException("文件不存在");
        }

        // 创建转换记录
        ConversionRecord record = new ConversionRecord();
        record.setFileId(fileId);
        record.setStartTime(new Date());
        record.setStatus(0); // 0: 待转换
        conversionRecordMapper.insert(record);

        // 异步开始转换
        startConversion(fileUpload, record);

        return record;
    }

    public Map<String, Object> getConversionProgress(Long taskId) {
        ConversionRecord record = conversionRecordMapper.selectById(taskId);
        if (record == null) {
            throw new IllegalArgumentException("转换任务不存在");
        }

        Map<String, Object> result = new HashMap<>();
        
        // 计算进度
        if (record.getStatus() == 0) { // 待转换
            result.put("progress", 0);
            result.put("status", "PENDING");
        } else if (record.getStatus() == 1) { // 转换中
            result.put("progress", 50);
            result.put("status", "CONVERTING");
        } else if (record.getStatus() == 2) { // 转换完成
            result.put("progress", 100);
            result.put("status", "COMPLETED");
        } else { // 转换失败
            result.put("progress", 0);
            result.put("status", "FAILED");
            result.put("error", record.getErrorMessage());
        }

        return result;
    }

    @Async("conversionExecutor")
    protected void startConversion(FileUpload fileUpload, ConversionRecord record) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                // 更新状态为转换中
                fileUpload.setStatus(1);
                fileUploadMapper.updateById(fileUpload);
                record.setStatus(1);
                conversionRecordMapper.updateById(record);

                // 调用Claude API进行转换
                String htmlContent = claudeService.convertToHtml(fileUpload.getFilePath(), fileUpload.getFileType());

                // 更新转换结果
                record.setHtmlContent(htmlContent);
                record.setStatus(2); // 转换完成
                record.setEndTime(new Date());
                conversionRecordMapper.updateById(record);

                // 更新文件状态
                fileUpload.setStatus(2); // 转换完成
                fileUploadMapper.updateById(fileUpload);

            } catch (Exception e) {
                handleConversionError(fileUpload, record, e.getMessage());
            }
        }, conversionExecutor);

        try {
            // 设置1分钟超时
            future.get(1, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            logger.error("文件转换超时：{}", fileUpload.getOriginalFilename());
            handleConversionError(fileUpload, record, "转换超时（超过1分钟）");
        } catch (Exception e) {
            logger.error("文件转换失败：{}", fileUpload.getOriginalFilename(), e);
            handleConversionError(fileUpload, record, e.getMessage());
        }
    }

    private void handleConversionError(FileUpload fileUpload, ConversionRecord record, String errorMessage) {
        // 更新失败状态
        record.setStatus(3); // 转换失败
        record.setErrorMessage(errorMessage);
        record.setEndTime(new Date());
        conversionRecordMapper.updateById(record);

        fileUpload.setStatus(3); // 转换失败
        fileUploadMapper.updateById(fileUpload);
    }
} 