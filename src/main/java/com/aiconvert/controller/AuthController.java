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
     */
    @PostMapping("/google")
    public ApiResponse<?> googleLogin(@RequestBody Map<String, String> payload) {
        String idToken = payload.get("idToken");
        
        logger.info("使用Google OAuth验证Token，idToken: {}",idToken);

        if (idToken == null || idToken.isEmpty()) {
            return ApiResponse.error(400, "ID Token不能为空");
        }
        
        try {
            logger.info("使用Google OAuth验证Token，client-id: {}", clientId.substring(0, 10) + "...");
            
            // 验证Google ID Token
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            Map<String, Object> tokenInfo = response.getBody();
            
            if (tokenInfo == null) {
                return ApiResponse.error(401, "无效的ID Token");
            }
            
            // 验证client_id是否匹配
            String tokenAud = (String) tokenInfo.get("aud");
            if (!clientId.equals(tokenAud)) {
                logger.error("Token的client_id不匹配: 期望值={}, 实际值={}", clientId, tokenAud);
                return ApiResponse.error(401, "无效的client_id");
            }
            
            // 获取用户信息
            String googleId = (String) tokenInfo.get("sub");
            String email = (String) tokenInfo.get("email");
            String name = (String) tokenInfo.get("name");
            String picture = (String) tokenInfo.get("picture");
            
            // 验证邮箱
            if (email == null || !Boolean.TRUE.equals(tokenInfo.get("email_verified"))) {
                return ApiResponse.error(401, "邮箱未验证");
            }
            
            // 创建或更新用户
            Map<String, Object> userData = userService.createOrUpdateGoogleUser(googleId, email, name, picture);
            
            // 返回用户信息和积分
            return ApiResponse.success(userData);
            
        } catch (Exception e) {
            logger.error("Google登录失败", e);
            return ApiResponse.error(500, "Google登录失败: " + e.getMessage());
        }
    }
} 