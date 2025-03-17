package com.aiconvert.service;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ClaudeService {

    private static final Logger logger = LoggerFactory.getLogger(ClaudeService.class);
    private static final List<Charset> CHARSETS = Arrays.asList(
        StandardCharsets.UTF_8,
        Charset.forName("GBK"),
        Charset.forName("GB2312"),
        StandardCharsets.ISO_8859_1
    );

    @Value("${claude.api.key}")
    private String apiKey;

    @Value("${claude.api.url}")
    private String apiUrl;

    @Value("${claude.api.model}")
    private String model;

    @Value("${claude.api.prompts.default}")
    private String defaultPrompt;

    @Value("${claude.api.prompts.pdf:${claude.api.prompts.default}}")
    private String pdfPrompt;

    @Value("${claude.api.prompts.doc:${claude.api.prompts.default}}")
    private String docPrompt;

    @Value("${claude.api.prompts.txt:${claude.api.prompts.default}}")
    private String txtPrompt;

    @Value("${claude.api.prompts.md:${claude.api.prompts.default}}")
    private String mdPrompt;

    private final RestTemplate restTemplate;

    public ClaudeService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String convertToHtml(String filePath, String fileType) throws Exception {
        logger.info("开始转换文件：{}, 文件类型：{}", filePath, fileType);
        long startTime = System.currentTimeMillis();
        
        File file = new File(filePath);
        if (!file.exists()) {
            logger.error("文件不存在：{}", filePath);
            throw new IllegalArgumentException("文件不存在");
        }

        // 根据文件类型选择不同的处理方式
        String content = null;
        Charset usedCharset = null;
        
        // 处理Office文档
        if ("docx".equalsIgnoreCase(fileType)) {
            content = extractDocxContent(file);
            usedCharset = StandardCharsets.UTF_8; // DOCX内容使用UTF-8编码
        } else {
            // 处理文本文件，尝试不同的编码
            Exception lastException = null;

            // 首先尝试检测文件编码
            Charset detectedCharset = detectFileEncoding(file);
            if (detectedCharset != null) {
                content = tryReadWithCharset(file, detectedCharset);
                if (content != null) {
                    usedCharset = detectedCharset;
                    logger.info("使用检测到的编码 {} 成功读取文件", detectedCharset);
                }
            }

            // 如果检测的编码不成功，尝试预定义的编码列表
            if (content == null) {
                for (Charset charset : CHARSETS) {
                    content = tryReadWithCharset(file, charset);
                    if (content != null) {
                        usedCharset = charset;
                        logger.info("使用编码 {} 成功读取文件", charset);
                        break;
                    }
                }
            }
        }

        if (content == null) {
            logger.error("无法读取文件内容");
            throw new IOException("无法读取文件内容");
        }

        logger.info("文件内容读取完成，总字符数：{}", content.length());
        
        // 添加文件内容预览日志
        String preview = content.length() > 500 ? content.substring(0, 500) + "..." : content;
        logger.info("文件内容预览：\n---内容开始---\n{}\n---内容结束---", preview);

        // 调用Claude API进行转换
        String result = callClaudeApi(content, fileType);

        long endTime = System.currentTimeMillis();
        logger.info("文件转换完成：{}, 总耗时：{} ms", filePath, (endTime - startTime));
        return result;
    }

    private Charset detectFileEncoding(File file) {
        try {
            // 读取文件的前几个字节来检测编码
            byte[] bytes = Files.readAllBytes(file.toPath());
            if (bytes.length >= 3 && bytes[0] == (byte)0xEF && bytes[1] == (byte)0xBB && bytes[2] == (byte)0xBF) {
                return StandardCharsets.UTF_8; // UTF-8 with BOM
            }
            if (bytes.length >= 2 && bytes[0] == (byte)0xFE && bytes[1] == (byte)0xFF) {
                return StandardCharsets.UTF_16BE; // UTF-16 Big Endian
            }
            if (bytes.length >= 2 && bytes[0] == (byte)0xFF && bytes[1] == (byte)0xFE) {
                return StandardCharsets.UTF_16LE; // UTF-16 Little Endian
            }
            
            // 尝试检测中文编码
            // 如果文件中包含大量的中文字符，很可能是GBK或GB2312
            int gbkCount = 0;
            for (int i = 0; i < bytes.length - 1; i++) {
                // GBK编码中，汉字的第一个字节的范围是0x81-0xFE
                // 第二个字节的范围是0x40-0xFE
                if ((bytes[i] & 0xFF) >= 0x81 && (bytes[i] & 0xFF) <= 0xFE) {
                    if (i + 1 < bytes.length && (bytes[i+1] & 0xFF) >= 0x40 && (bytes[i+1] & 0xFF) <= 0xFE) {
                        gbkCount++;
                    }
                }
            }
            
            if (gbkCount > bytes.length / 10) { // 如果GBK特征字符占比超过10%
                return Charset.forName("GBK");
            }
        } catch (IOException e) {
            logger.warn("检测文件编码失败：{}", e.getMessage());
        }
        return null;
    }

    private String tryReadWithCharset(File file, Charset charset) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), charset))) {
            
            StringBuilder content = new StringBuilder();
            String line;
            int lineCount = 0;
            boolean hasValidContent = false;
            
            // 读取前几行进行预览
            while ((line = reader.readLine()) != null && lineCount < 5) {
                content.append(line).append("\n");
                // 检查行内容是否包含有效字符
                if (!line.trim().isEmpty() && !containsOnlySpecialChars(line)) {
                    hasValidContent = true;
                }
                lineCount++;
            }
            
            // 预览内容检查
            String preview = content.toString();
            logger.debug("使用编码 {} 读取前 {} 行预览：\n{}", charset, lineCount, preview);
            
            // 如果预览内容看起来是乱码，返回null
            if (!hasValidContent || containsMostlyUnreadableChars(preview)) {
                logger.debug("使用编码 {} 读取的内容可能是乱码，尝试下一个编码", charset);
                return null;
            }
            
            // 继续读取剩余内容
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            
            return content.toString();
            
        } catch (IOException e) {
            logger.debug("使用编码 {} 读取失败：{}", charset, e.getMessage());
            return null;
        }
    }
    
    private boolean containsOnlySpecialChars(String text) {
        return text.chars().allMatch(ch -> !Character.isLetterOrDigit(ch));
    }
    
    private boolean containsMostlyUnreadableChars(String text) {
        if (text.isEmpty()) return true;
        
        int unreadableCount = 0;
        for (char c : text.toCharArray()) {
            // 检查是否是常见的不可读字符
            if (c == '?' || c == '\uFFFF' || c == '\uFFFD' || 
                (c >= '\u0000' && c <= '\u001F' && c != '\n' && c != '\r' && c != '\t')) {
                unreadableCount++;
            }
        }
        
        // 如果不可读字符占比超过20%，认为是乱码
        return (double) unreadableCount / text.length() > 0.2;
    }

    private String callClaudeApi(String content, String fileType) {
        long startTime = System.currentTimeMillis();
        
        // 获取对应文件类型的提示词
        String prompt = getPromptForFileType(fileType);
        
        // 记录完整的提示词和内容（为了调试）
        logger.debug("完整的提示词和内容：\n---提示词开始---\n{}\n---提示词结束---\n---内容开始---\n{}\n---内容结束---", 
            prompt, content);
        
        // 准备请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("HTTP-Referer", "https://aiconvert.app");
        headers.set("X-Title", "AI Document Converter");
        
        // 记录请求头信息
        logger.debug("API请求头信息：Content-Type={}, Authorization=Bearer {}..., HTTP-Referer={}, X-Title={}", 
            headers.getContentType(), 
            apiKey.substring(0, Math.min(10, apiKey.length())) + "...", 
            headers.getFirst("HTTP-Referer"), 
            headers.getFirst("X-Title"));

        // 准备请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        
        // 使用正确的消息结构：系统消息包含提示词，用户消息包含文件内容
        List<Map<String, String>> messages = Arrays.asList(
            Map.of(
                "role", "system",
                "content", prompt
            ),
            Map.of(
                "role", "user",
                "content", content
            )
        );
        requestBody.put("messages", messages);
        
        // 记录请求体信息
        logger.info("API请求参数 - 模型: {}, 系统消息(提示词)长度: {}, 用户消息(文件内容)长度: {}, 总消息数: {}", 
            model, 
            prompt.length(),
            content.length(),
            messages.size());
        
        // 估算token数量（粗略估计，每4个字符约1个token）
        int estimatedTokens = (content.length() + prompt.length()) / 4;
        logger.info("估计token数量: 约 {} tokens", estimatedTokens);
        
        // 记录完整请求体（debug级别）
        logger.debug("API完整请求体：{}", requestBody);

        // 记录请求信息
        logger.info("准备调用 Claude API - URL: {}, Model: {}, 文件类型: {}, 内容长度: {} 字符", 
            apiUrl, model, fileType, content.length());

        try {
            // 发送请求
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // 记录发送请求时间
            logger.info("开始发送API请求: {}", new java.util.Date());
            
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
            
            long endTime = System.currentTimeMillis();
            logger.info("Claude API调用完成，耗时：{} ms, 状态码: {}", (endTime - startTime), response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map responseBody = response.getBody();
                // 记录完整的API响应（为了调试）
                logger.debug("API完整响应：{}", responseBody);
                
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    if (message != null) {
                        String result = (String) message.get("content");
                        logger.info("成功获取API响应，响应内容长度：{} 字符", result.length());
                        
                        // 记录使用的token信息（如果API返回）
                        if (responseBody.containsKey("usage")) {
                            Map<String, Object> usage = (Map<String, Object>) responseBody.get("usage");
                            logger.info("API使用token统计 - 输入: {}, 输出: {}, 总计: {}", 
                                usage.get("prompt_tokens"), 
                                usage.get("completion_tokens"), 
                                usage.get("total_tokens"));
                        }
                        
                        // 记录API返回的内容（为了调试）
                        String resultPreview = result.length() > 500 ? result.substring(0, 500) + "..." : result;
                        logger.debug("API返回内容预览：\n---内容开始---\n{}\n---内容结束---", resultPreview);
                        return result;
                    }
                }
                logger.error("API响应格式异常：{}", responseBody);
                throw new RuntimeException("API响应格式错误");
            } else {
                logger.error("API响应状态码异常：{}, 响应体：{}", 
                    response.getStatusCode(), response.getBody());
                throw new RuntimeException("API调用失败：" + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            logger.error("API调用异常：{}, 响应体：{}", e.getMessage(), e.getResponseBodyAsString());
            throw new RuntimeException("API调用异常：" + e.getMessage());
        }
    }

    private String getPromptForFileType(String fileType) {
        if (fileType == null) {
            logger.info("未指定文件类型，使用默认提示词");
            return defaultPrompt;
        }
        
        String selectedPrompt = switch (fileType.toLowerCase()) {
            case "pdf" -> pdfPrompt;
            case "doc", "docx" -> docPrompt;
            case "txt" -> txtPrompt;
            case "md" -> mdPrompt;
            default -> defaultPrompt;
        };
        
        logger.info("文件类型 [{}] 使用{}提示词", fileType, 
            selectedPrompt.equals(defaultPrompt) ? "默认" : "专用");
        return selectedPrompt;
    }

    private String escapeHtml(String content) {
        return content
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    /**
     * 提取DOCX文件的文本内容
     */
    private String extractDocxContent(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            
            String content = extractor.getText();
            logger.info("成功提取DOCX文件内容，长度：{} 字符", content.length());
            return content;
            
        } catch (Exception e) {
            logger.error("提取DOCX文件内容失败：{}", e.getMessage(), e);
            return "无法读取DOCX文件内容。错误信息：" + e.getMessage();
        }
    }
} 