package com.aiconvert.mapper;

import com.aiconvert.entity.ConversionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ConversionRecordMapper {
    int insert(ConversionRecord record);
    ConversionRecord selectByFileId(@Param("fileId") Long fileId);
    ConversionRecord selectById(@Param("id") Long id);
    int updateById(ConversionRecord record);
    int update(ConversionRecord record);
} 