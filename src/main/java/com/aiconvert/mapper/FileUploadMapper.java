package com.aiconvert.mapper;

import com.aiconvert.entity.FileUpload;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FileUploadMapper {
    int insert(FileUpload fileUpload);
    FileUpload selectById(@Param("id") Long id);
    int updateById(FileUpload fileUpload);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
} 