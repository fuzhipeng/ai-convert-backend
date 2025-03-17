CREATE DATABASE ai_convert DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建文件上传表
CREATE TABLE file_upload (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    original_filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
    file_type VARCHAR(50) NOT NULL COMMENT '文件类型',
    upload_time DATETIME NOT NULL COMMENT '上传时间',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态:0-待转换,1-转换中,2-转换成功,3-转换失败',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件上传记录表';

-- 创建转换记录表
CREATE TABLE conversion_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_id BIGINT NOT NULL COMMENT '关联的文件ID',
    html_content LONGTEXT COMMENT '转换后的HTML内容',
    error_message VARCHAR(500) COMMENT '错误信息',
    start_time DATETIME COMMENT '转换开始时间',
    end_time DATETIME COMMENT '转换结束时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES file_upload(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转换记录表';