-- 创建文件信息表
CREATE TABLE `file_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `file_name` varchar(255) NOT NULL COMMENT '文件名',
  `file_size` bigint NOT NULL COMMENT '文件大小(字节)',
  `file_type` varchar(50) NOT NULL COMMENT '文件类型',
  `storage_path` varchar(255) NOT NULL COMMENT '存储路径',
  `storage_type` varchar(20) NOT NULL COMMENT '存储类型（MINIO, LOCAL等）',
  `md5` varchar(32) NOT NULL COMMENT '文件MD5',
  `status` varchar(20) NOT NULL COMMENT '文件状态（UPLOADING, UPLOADED, ERROR）',
  `upload_progress` int NOT NULL DEFAULT '0' COMMENT '上传进度(0-100)',
  `total_chunks` int DEFAULT NULL COMMENT '总分片数',
  `upload_id` varchar(100) DEFAULT NULL COMMENT '分片上传ID',
  `is_public` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否公开',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_md5` (`md5`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件信息表'; 