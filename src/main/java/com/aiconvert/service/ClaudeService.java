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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

@Service
public class ClaudeService {

    private static final Logger logger = LoggerFactory.getLogger(ClaudeService.class);
    private static final List<Charset> CHARSETS = Arrays.asList(
        StandardCharsets.UTF_8,
        Charset.forName("GBK"),
        Charset.forName("GB2312"),
        Charset.forName("GB18030"),
        Charset.forName("Big5"),
        StandardCharsets.UTF_16LE,
        StandardCharsets.UTF_16BE,
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

    @Value("${claude.api.data_prompts.default}")
    private String defaultDataPrompt;

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

    
    /**
     * 将文件内容转换为HTML
     * 修改为直接返回预定义的HTML，不调用Claude API
     */
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
        
        if ("pdf".equalsIgnoreCase(fileType)) {
            content = extractPdfContent(file);
            usedCharset = StandardCharsets.UTF_8;
        } else if ("docx".equalsIgnoreCase(fileType)) {
            content = extractDocxContent(file);
            usedCharset = StandardCharsets.UTF_8;
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
    


     /**
     * 将文件内容转换为HTML
     * 修改为直接返回预定义的HTML，不调用Claude API
     */
    public String convertToDataHtml(String filePath, String fileType) throws Exception {
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
        
        if ("pdf".equalsIgnoreCase(fileType)) {
            content = extractPdfContent(file);
            usedCharset = StandardCharsets.UTF_8;
        } else if ("docx".equalsIgnoreCase(fileType)) {
            content = extractDocxContent(file);
            usedCharset = StandardCharsets.UTF_8;
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
        String result = callDataClaudeApi(content, fileType);

        long endTime = System.currentTimeMillis();
        logger.info("文件转换完成：{}, 总耗时：{} ms", filePath, (endTime - startTime));
        return result;
    }
    /**
     * 返回预定义的HTML代码
     */
    private String getPredefinedHtml() {
        return "<!DOCTYPE html>\n" +
               "<html lang=\"zh\">\n" +
               "<head>\n" +
               "  <meta charset=\"UTF-8\">\n" +
               "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
               "  <title>深圳服务贸易发展概念卡片</title>\n" +
               "  <script src=\"https://cdn.tailwindcss.com\"></script>\n" +
               "  <link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css\">\n" +
               "  \n" +
               "  <style type=\"text/tailwindcss\">\n" +
               "    @layer utilities {\n" +
               "      .w-card {\n" +
               "        width: 1080px;\n" +
               "      }\n" +
               "      .h-card {\n" +
               "        height: 800px;\n" +
               "      }\n" +
               "    }\n" +
               "  </style>\n" +
               "  \n" +
               "  <style>\n" +
               "    :root {\n" +
               "      --color-primary: #0056b3;\n" +
               "      --color-secondary: #34c3ff;\n" +
               "      --color-accent: #ff6b35;\n" +
               "    }\n" +
               "    \n" +
               "    .text-clamp-2 {\n" +
               "      display: -webkit-box;\n" +
               "      -webkit-line-clamp: 2;\n" +
               "      -webkit-box-orient: vertical;\n" +
               "      overflow: hidden;\n" +
               "    }\n" +
               "    \n" +
               "    .text-clamp-3 {\n" +
               "      display: -webkit-box;\n" +
               "      -webkit-line-clamp: 3;\n" +
               "      -webkit-box-orient: vertical;\n" +
               "      overflow: hidden;\n" +
               "    }\n" +
               "    \n" +
               "    .text-primary { color: var(--color-primary); }\n" +
               "    .text-secondary { color: var(--color-secondary); }\n" +
               "    .text-accent { color: var(--color-accent); }\n" +
               "    \n" +
               "    .bg-primary { background-color: var(--color-primary); }\n" +
               "    .bg-secondary { background-color: var(--color-secondary); }\n" +
               "    .bg-accent { background-color: var(--color-accent); }\n" +
               "  </style>\n" +
               "</head>\n" +
               "<body class=\"bg-gray-100 flex justify-center items-center min-h-screen p-5\">\n" +
               "  <div class=\"w-card h-card bg-white rounded-xl shadow-lg overflow-hidden\">\n" +
               "    <div class=\"p-8 h-full flex flex-col\">\n" +
               "      <header class=\"mb-6\">\n" +
               "        <div class=\"flex items-center justify-between\">\n" +
               "          <div>\n" +
               "            <h1 class=\"text-3xl font-bold text-primary text-clamp-2\">首破万亿美元，深圳服务贸易大有潜力</h1>\n" +
               "            <p class=\"text-gray-500 mt-2\">深圳发布 | 2025年03月12日</p>\n" +
               "          </div>\n" +
               "          <div class=\"flex items-center justify-center bg-primary text-white rounded-full w-20 h-20\">\n" +
               "            <i class=\"fas fa-globe text-4xl\"></i>\n" +
               "          </div>\n" +
               "        </div>\n" +
               "      </header>\n" +
               "      \n" +
               "      <main class=\"flex-grow flex flex-col gap-6 overflow-hidden\">\n" +
               "        <!-- 核心指标 -->\n" +
               "        <div class=\"flex gap-6 justify-between mb-2\">\n" +
               "          <div class=\"bg-blue-50 rounded-lg p-5 flex-1 flex items-center gap-4\">\n" +
               "            <div class=\"text-primary text-3xl\"><i class=\"fas fa-chart-line\"></i></div>\n" +
               "            <div>\n" +
               "              <h3 class=\"font-bold text-lg\">2024年服务贸易进出口总额</h3>\n" +
               "              <p class=\"text-2xl font-bold text-accent\">1402.4亿美元</p>\n" +
               "              <p class=\"text-sm text-gray-600\">创历史新高</p>\n" +
               "            </div>\n" +
               "          </div>\n" +
               "          <div class=\"bg-blue-50 rounded-lg p-5 flex-1 flex items-center gap-4\">\n" +
               "            <div class=\"text-primary text-3xl\"><i class=\"fas fa-bullseye\"></i></div>\n" +
               "            <div>\n" +
               "              <h3 class=\"font-bold text-lg\">2025年服务贸易目标</h3>\n" +
               "              <p class=\"text-2xl font-bold text-accent\">1500亿美元以上</p>\n" +
               "              <p class=\"text-sm text-gray-600\">高质量发展行动计划</p>\n" +
               "            </div>\n" +
               "          </div>\n" +
               "        </div>\n" +
               "        \n" +
               "        <!-- 行业现状 -->\n" +
               "        <div class=\"flex gap-6 mb-2\">\n" +
               "          <div class=\"bg-gray-50 rounded-lg p-5 flex-1\">\n" +
               "            <div class=\"flex items-center gap-3 mb-3\">\n" +
               "              <i class=\"fas fa-sitemap text-secondary text-xl\"></i>\n" +
               "              <h3 class=\"font-bold text-lg\">现状与挑战</h3>\n" +
               "            </div>\n" +
               "            <ul class=\"space-y-2\">\n" +
               "              <li class=\"flex gap-2\">\n" +
               "                <i class=\"fas fa-circle-check text-primary mt-1\"></i>\n" +
               "                <p>深圳服务贸易排名全国第三，低于上海、北京</p>\n" +
               "              </li>\n" +
               "              <li class=\"flex gap-2\">\n" +
               "                <i class=\"fas fa-circle-check text-primary mt-1\"></i>\n" +
               "                <p>服务贸易仅占全国外贸总额的14.6%</p>\n" +
               "              </li>\n" +
               "              <li class=\"flex gap-2\">\n" +
               "                <i class=\"fas fa-circle-check text-primary mt-1\"></i>\n" +
               "                <p>知识密集型服务贸易占比仅38.5%</p>\n" +
               "              </li>\n" +
               "            </ul>\n" +
               "          </div>\n" +
               "          <div class=\"bg-gray-50 rounded-lg p-5 flex-1\">\n" +
               "            <div class=\"flex items-center gap-3 mb-3\">\n" +
               "              <i class=\"fas fa-lightbulb text-secondary text-xl\"></i>\n" +
               "              <h3 class=\"font-bold text-lg\">深圳优势</h3>\n" +
               "            </div>\n" +
               "            <ul class=\"space-y-2\">\n" +
               "              <li class=\"flex gap-2\">\n" +
               "                <i class=\"fas fa-circle-check text-primary mt-1\"></i>\n" +
               "                <p>雄厚的数字经济、先进制造业基础</p>\n" +
               "              </li>\n" +
               "              <li class=\"flex gap-2\">\n" +
               "                <i class=\"fas fa-circle-check text-primary mt-1\"></i>\n" +
               "                <p>华为、中兴、腾讯等企业国际竞争力强</p>\n" +
               "              </li>\n" +
               "              <li class=\"flex gap-2\">\n" +
               "                <i class=\"fas fa-circle-check text-primary mt-1\"></i>\n" +
               "                <p>跨境电商等数字订购贸易成为新动能</p>\n" +
               "              </li>\n" +
               "            </ul>\n" +
               "          </div>\n" +
               "        </div>\n" +
               "        \n" +
               "        <!-- 发展策略 -->\n" +
               "        <div class=\"bg-gray-50 rounded-lg p-5\">\n" +
               "          <div class=\"flex items-center gap-3 mb-3\">\n" +
               "            <i class=\"fas fa-chess text-secondary text-xl\"></i>\n" +
               "            <h3 class=\"font-bold text-lg\">深圳服务贸易发展策略</h3>\n" +
               "          </div>\n" +
               "          <div class=\"grid grid-cols-2 gap-4\">\n" +
               "            <div class=\"flex gap-3 items-start\">\n" +
               "              <div class=\"bg-primary text-white rounded-full p-2 flex items-center justify-center w-8 h-8\">\n" +
               "                <i class=\"fas fa-door-open text-sm\"></i>\n" +
               "              </div>\n" +
               "              <div>\n" +
               "                <h4 class=\"font-bold\">深化制度型开放</h4>\n" +
               "                <p class=\"text-sm text-gray-600 text-clamp-2\">推动规则、规制、管理、标准等制度型开放，与时俱进优化各项举措</p>\n" +
               "              </div>\n" +
               "            </div>\n" +
               "            <div class=\"flex gap-3 items-start\">\n" +
               "              <div class=\"bg-primary text-white rounded-full p-2 flex items-center justify-center w-8 h-8\">\n" +
               "                <i class=\"fas fa-cubes text-sm\"></i>\n" +
               "              </div>\n" +
               "              <div>\n" +
               "                <h4 class=\"font-bold\">平台建设</h4>\n" +
               "                <p class=\"text-sm text-gray-600 text-clamp-2\">发挥自贸试验区、综合保税区等平台优势，争取国家级平台落地</p>\n" +
               "              </div>\n" +
               "            </div>\n" +
               "            <div class=\"flex gap-3 items-start\">\n" +
               "              <div class=\"bg-primary text-white rounded-full p-2 flex items-center justify-center w-8 h-8\">\n" +
               "                <i class=\"fas fa-users text-sm\"></i>\n" +
               "              </div>\n" +
               "              <div>\n" +
               "                <h4 class=\"font-bold\">培育服务贸易主体</h4>\n" +
               "                <p class=\"text-sm text-gray-600 text-clamp-2\">加快培育和引进服务贸易主体，支持企业和个体经营者开放发展</p>\n" +
               "              </div>\n" +
               "            </div>\n" +
               "            <div class=\"flex gap-3 items-start\">\n" +
               "              <div class=\"bg-primary text-white rounded-full p-2 flex items-center justify-center w-8 h-8\">\n" +
               "                <i class=\"fas fa-building-columns text-sm\"></i>\n" +
               "              </div>\n" +
               "              <div>\n" +
               "                <h4 class=\"font-bold\">构建服务体系</h4>\n" +
               "                <p class=\"text-sm text-gray-600 text-clamp-2\">构建便捷高效的金融服务体系，构建强大的跨境服务链</p>\n" +
               "              </div>\n" +
               "            </div>\n" +
               "          </div>\n" +
               "        </div>\n" +
               "        \n" +
               "        <!-- 未来机遇 -->\n" +
               "        <div class=\"bg-blue-50 rounded-lg p-5\">\n" +
               "          <div class=\"flex items-center gap-3 mb-3\">\n" +
               "            <i class=\"fas fa-rocket text-accent text-xl\"></i>\n" +
               "            <h3 class=\"font-bold text-lg\">未来发展机遇</h3>\n" +
               "          </div>\n" +
               "          <div class=\"flex gap-4\">\n" +
               "            <div class=\"flex flex-col items-center bg-white rounded-lg p-3 flex-1\">\n" +
               "              <i class=\"fas fa-robot text-accent text-2xl mb-2\"></i>\n" +
               "              <p class=\"text-center text-sm font-bold\">人工智能</p>\n" +
               "            </div>\n" +
               "            <div class=\"flex flex-col items-center bg-white rounded-lg p-3 flex-1\">\n" +
               "              <i class=\"fas fa-atom text-accent text-2xl mb-2\"></i>\n" +
               "              <p class=\"text-center text-sm font-bold\">量子科技</p>\n" +
               "            </div>\n" +
               "            <div class=\"flex flex-col items-center bg-white rounded-lg p-3 flex-1\">\n" +
               "              <i class=\"fas fa-cloud text-accent text-2xl mb-2\"></i>\n" +
               "              <p class=\"text-center text-sm font-bold\">云计算</p>\n" +
               "            </div>\n" +
               "            <div class=\"flex flex-col items-center bg-white rounded-lg p-3 flex-1\">\n" +
               "              <i class=\"fas fa-film text-accent text-2xl mb-2\"></i>\n" +
               "              <p class=\"text-center text-sm font-bold\">文化创意</p>\n" +
               "            </div>\n" +
               "          </div>\n" +
               "        </div>\n" +
               "      </main>\n" +
               "      \n" +
               "      <footer class=\"mt-4 pt-4 border-t border-gray-100 text-sm text-gray-500 flex justify-between\">\n" +
               "        <div>来源：深圳卫视深视新闻 | 作者：靳阳懿 李婷 赵畅</div>\n" +
               "        <div>文章概念卡片 © 2025</div>\n" +
               "      </footer>\n" +
               "    </div>\n" +
               "  </div>\n" +
               "</body>\n" +
               "</html>";
    }

    private Charset detectFileEncoding(File file) {
        try {
            // 读取文件的前4096字节来检测编码
            byte[] bytes = new byte[4096];
            try (FileInputStream fis = new FileInputStream(file)) {
                int read = fis.read(bytes);
                if (read > 0) {
                    bytes = Arrays.copyOf(bytes, read);
                }
            }

            // 检查BOM标记
            if (bytes.length >= 3 && bytes[0] == (byte)0xEF && bytes[1] == (byte)0xBB && bytes[2] == (byte)0xBF) {
                logger.debug("检测到UTF-8 BOM标记");
                return StandardCharsets.UTF_8;
            }
            if (bytes.length >= 2 && bytes[0] == (byte)0xFE && bytes[1] == (byte)0xFF) {
                logger.debug("检测到UTF-16BE BOM标记");
                return StandardCharsets.UTF_16BE;
            }
            if (bytes.length >= 2 && bytes[0] == (byte)0xFF && bytes[1] == (byte)0xFE) {
                logger.debug("检测到UTF-16LE BOM标记");
                return StandardCharsets.UTF_16LE;
            }

            // 检测UTF-8编码
            // UTF-8编码规则:
            // 1字节: 0xxxxxxx
            // 2字节: 110xxxxx 10xxxxxx
            // 3字节: 1110xxxx 10xxxxxx 10xxxxxx
            // 4字节: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
            int utf8Score = 0;
            int totalBytes = 0;
            
            for (int i = 0; i < bytes.length; i++) {
                totalBytes++;
                int b = bytes[i] & 0xFF;
                
                if (b < 0x80) { // ASCII范围
                    utf8Score++;
                    continue;
                }
                
                if ((b & 0xE0) == 0xC0) { // 2字节UTF-8序列开始
                    if (i + 1 < bytes.length && (bytes[i + 1] & 0xC0) == 0x80) {
                        utf8Score += 2;
                        i++;
                        continue;
                    }
                }
                
                if ((b & 0xF0) == 0xE0) { // 3字节UTF-8序列开始
                    if (i + 2 < bytes.length && 
                        (bytes[i + 1] & 0xC0) == 0x80 && 
                        (bytes[i + 2] & 0xC0) == 0x80) {
                        utf8Score += 3;
                        i += 2;
                        continue;
                    }
                }
                
                if ((b & 0xF8) == 0xF0) { // 4字节UTF-8序列开始
                    if (i + 3 < bytes.length &&
                        (bytes[i + 1] & 0xC0) == 0x80 &&
                        (bytes[i + 2] & 0xC0) == 0x80 &&
                        (bytes[i + 3] & 0xC0) == 0x80) {
                        utf8Score += 4;
                        i += 3;
                        continue;
                    }
                }
            }
            
            // 如果90%以上的字节符合UTF-8编码规则,认为是UTF-8
            if (totalBytes > 0 && (double)utf8Score / totalBytes > 0.9) {
                logger.debug("检测到UTF-8编码(无BOM) - 符合度: {:.2f}%", (double)utf8Score / totalBytes * 100);
                return StandardCharsets.UTF_8;
            }
            
            // 检测中文编码
            int gbkCount = 0;
            for (int i = 0; i < bytes.length - 1; i++) {
                // GBK编码中，汉字的第一个字节的范围是0x81-0xFE
                // 第二个字节的范围是0x40-0xFE
                if ((bytes[i] & 0xFF) >= 0x81 && (bytes[i] & 0xFF) <= 0xFE) {
                    if (i + 1 < bytes.length && 
                        (bytes[i+1] & 0xFF) >= 0x40 && 
                        (bytes[i+1] & 0xFF) <= 0xFE) {
                        gbkCount++;
                        i++; // 跳过下一个字节
                    }
                }
            }
            
            // 如果GBK特征字符占比超过20%,可能是GBK编码
            if (bytes.length > 0 && (double)gbkCount * 2 / bytes.length > 0.2) {
                logger.debug("检测到GBK编码 - 特征字符占比: {:.2f}%", (double)gbkCount * 2 / bytes.length * 100);
                return Charset.forName("GBK");
            }

            logger.debug("无法确定文件编码");
            return null;
            
        } catch (IOException e) {
            logger.warn("检测文件编码失败：{}", e.getMessage());
            return null;
        }
    }

    private String tryReadWithCharset(File file, Charset charset) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), charset))) {
            
            StringBuilder content = new StringBuilder();
            String line;
            int lineCount = 0;
            int validLineCount = 0;
            int totalChars = 0;
            int validChars = 0;
            
            // 读取所有内容进行分析
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
                lineCount++;
                totalChars += line.length();
                
                // 检查行内容是否有效
                if (!line.trim().isEmpty()) {
                    // 统计有效字符数
                    for (char c : line.toCharArray()) {
                        if (isValidChar(c)) {
                            validChars++;
                        }
                    }
                    if (hasValidContent(line)) {
                        validLineCount++;
                    }
                }
                
                // 每100行记录一次日志
                if (lineCount % 100 == 0) {
                    logger.debug("已读取 {} 行, 有效行: {}, 总字符: {}, 有效字符: {}", 
                        lineCount, validLineCount, totalChars, validChars);
                }
            }
            
            // 计算有效内容比例
            double validLineRatio = lineCount > 0 ? (double) validLineCount / lineCount : 0;
            double validCharRatio = totalChars > 0 ? (double) validChars / totalChars : 0;
            
            logger.info("使用编码 {} 读取完成 - 总行数: {}, 有效行: {}, 有效行比例: {:.2f}%, 总字符: {}, 有效字符: {}, 有效字符比例: {:.2f}%",
                charset, lineCount, validLineCount, validLineRatio * 100, totalChars, validChars, validCharRatio * 100);
            
            // 检查内容是否可能是乱码
            if (validLineRatio < 0.3 || validCharRatio < 0.3) {
                logger.debug("使用编码 {} 读取的内容可能是乱码 - 有效行比例: {:.2f}%, 有效字符比例: {:.2f}%", 
                    charset, validLineRatio * 100, validCharRatio * 100);
                return null;
            }
            
            String result = content.toString();
            // 记录内容预览
            String preview = result.length() > 200 ? result.substring(0, 200) + "..." : result;
            logger.debug("使用编码 {} 读取的内容预览:\n{}", charset, preview);
            
            return result;
            
        } catch (IOException e) {
            logger.debug("使用编码 {} 读取失败：{}", charset, e.getMessage());
            return null;
        }
    }
    
    private boolean isValidChar(char c) {
        // 检查是否是可打印字符
        if (Character.isWhitespace(c)) return true;
        if (Character.isLetterOrDigit(c)) return true;
        if (Character.isIdeographic(c)) return true; // 检查是否是表意文字(如中文)
        if (isPunctuation(c)) return true;
        return false;
    }
    
    private boolean isPunctuation(char c) {
        // 常见中英文标点符号
        return (c >= 0x2000 && c <= 0x206F) || // 常用标点
               (c >= 0x3000 && c <= 0x303F) || // 中文标点
               (c >= 0xFF00 && c <= 0xFFEF) || // 全角字符
               ",.!?;:\"'()[]{}+-*/=@#$%&|\\".indexOf(c) >= 0;
    }
    
    private boolean hasValidContent(String line) {
        if (line == null || line.trim().isEmpty()) return false;
        
        int validChars = 0;
        for (char c : line.toCharArray()) {
            if (isValidChar(c)) {
                validChars++;
            }
        }
        
        // 如果有效字符占比超过50%,认为这行是有效的
        return (double) validChars / line.length() > 0.5;
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



    /**
     * 数据模板
     * @param content
     * @param fileType
     * @return
     */
    private String callDataClaudeApi(String content, String fileType) {
        long startTime = System.currentTimeMillis();
        
        // 获取对应文件类型的提示词
        String prompt = defaultDataPrompt;
        
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

    /**
     * 提取PDF文件的文本内容
     */
    private String extractPdfContent(File file) {
        try (PDDocument document = PDDocument.load(file)) {
            // 检查PDF是否加密
            if (document.isEncrypted()) {
                logger.error("PDF文件已加密,无法读取");
                throw new RuntimeException("PDF文件已加密,请先解密");
            }

            // 获取页数
            int pageCount = document.getNumberOfPages();
            logger.info("PDF文件页数: {}", pageCount);
            if (pageCount == 0) {
                logger.error("PDF文件页数为0");
                throw new RuntimeException("PDF文件页数为0");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            // 设置读取顺序为从左到右,从上到下
            stripper.setSortByPosition(true);
            // 设置读取所有页
            stripper.setStartPage(1);
            stripper.setEndPage(pageCount);

            String content = stripper.getText(document);
            
            // 检查提取的内容
            if (content == null || content.trim().isEmpty()) {
                logger.error("提取的PDF内容为空");
                throw new RuntimeException("提取的PDF内容为空,可能是扫描件需要OCR");
            }

            logger.info("成功提取PDF文件内容，页数：{}, 长度：{} 字符", pageCount, content.length());
            // 记录内容预览
            String preview = content.length() > 200 ? content.substring(0, 200) + "..." : content;
            logger.debug("PDF内容预览:\n{}", preview);
            
            return content;
        } catch (Exception e) {
            logger.error("提取PDF文件内容失败：{}", e.getMessage(), e);
            throw new RuntimeException("无法读取PDF文件内容：" + e.getMessage());
        }
    }
} 