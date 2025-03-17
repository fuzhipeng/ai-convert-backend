package com.aiconvert.utils;

import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class FileUtils {
    public static String saveFile(MultipartFile file, String uploadPath) throws IOException {
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = UUID.randomUUID().toString() + extension;
        
        File destFile = new File(uploadPath + File.separator + newFilename);
        file.transferTo(destFile);
        
        return destFile.getAbsolutePath();
    }
} 