package com.aiconvert.controller;

import com.aiconvert.common.ApiResponse;
import com.aiconvert.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private UserService userService;
    
    @Value("${google.oauth.client-id}")
    private String clientId;
    
    @Value("${google.oauth.client-secret}")
    private String clientSecret;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Google OAuth登录
     * 路径: /api/auth/google
     */
    @PostMapping("/google")
    public ApiResponse<?> googleLogin(@RequestBody Map<String, String> payload) {
        logger.info("收到Google登录请求，payload: {}", payload);
        
        String idToken = payload.get("idToken");
        
        if (idToken == null || idToken.isEmpty()) {
            logger.error("ID Token为空");
            return ApiResponse.error(400, "ID Token不能为空");
        }
        
        try {
            logger.info("使用Google OAuth验证Token，client-id: {}", clientId);
            
            // 验证Google ID Token
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            Map<String, Object> tokenInfo;
            try {
                logger.info("准备调用Google TokenInfo API");
                ResponseEntity<Map> response = restTemplate.exchange(
                    "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken,
                    HttpMethod.GET,
                    entity,
                    Map.class
                );
                logger.info("Google TokenInfo API调用成功，状态码: {}", response.getStatusCodeValue());
                
                tokenInfo = response.getBody();
                
                if (tokenInfo == null) {
                    logger.error("TokenInfo为空");
                    return ApiResponse.error(401, "无效的ID Token");
                }
            } catch (HttpClientErrorException e) {
                logger.error("Google TokenInfo API调用失败: {}", e.getResponseBodyAsString());
                return ApiResponse.error(401, "无效的ID Token: " + e.getResponseBodyAsString());
            } catch (RestClientException e) {
                logger.error("调用Google TokenInfo API发生错误", e);
                return ApiResponse.error(500, "验证Token时发生错误: " + e.getMessage());
            }
            
            // 验证client_id是否匹配
            String tokenAud = (String) tokenInfo.get("aud");
            logger.info("Token aud: {}, 配置的client-id: {}", tokenAud, clientId);
            if (!clientId.equals(tokenAud)) {
                logger.error("Token的client_id不匹配: 期望值={}, 实际值={}", clientId, tokenAud);
                return ApiResponse.error(401, "无效的client_id");
            }
            

            // 获取用户信息
            String googleId = (String) tokenInfo.get("sub");
            String email = (String) tokenInfo.get("email");
            String name = (String) tokenInfo.get("name");
            String picture = (String) tokenInfo.get("picture");
            
            logger.info("解析的用户信息: googleId={}, email={}, name={}", googleId, email, name);
            
            // 验证邮箱
            if (email == null || !Boolean.TRUE.equals(tokenInfo.get("email_verified"))) {
                logger.error("邮箱未验证: {}", email);
                return ApiResponse.error(401, "邮箱未验证");
            }
            
            // 创建或更新用户
            logger.info("准备创建或更新用户");
            Map<String, Object> userData = userService.createOrUpdateGoogleUser(googleId, email, name, picture);
            
            // 返回用户信息和积分
            logger.info("Google登录成功: {}", email);
            return ApiResponse.success(userData);
            
        } catch (Exception e) {
            logger.error("Google登录失败", e);
            return ApiResponse.error(500, "Google登录失败: " + e.getMessage());
        }
    }
} 