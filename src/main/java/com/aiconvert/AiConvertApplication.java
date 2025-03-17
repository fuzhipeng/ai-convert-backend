package com.aiconvert;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.aiconvert.mapper")
public class AiConvertApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiConvertApplication.class, args);
    }
} 