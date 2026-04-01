package com.polo.boot.storage.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class OperationResult {
    // 基础信息
    private String operationId;           // 上传任务ID
    private OperationType operationType;       //操作类型
    private String fieldName;          // 表单字段名
    private String originalFilename;   // 原始文件名
    private String filename;           // 存储文件名
    private String originalFilepath;           // 存储路径

    // 访问链接
    private String originalUrl;                // 永久访问URL（公有读）
    private String signedUrl;          // 签名URL（私有读，临时）
    private Integer signedUrlExpire;   // 签名URL过期时间（秒）

    // 缩略图
    private String thumbnailUrl;       // 缩略图URL
    private Integer thumbnailWidth;
    private Integer thumbnailHeight;

    // 文件信息
    private Long size;                 // 文件大小（字节）
    private String contentType;        // MIME类型
    private String fileMd5;            // 文件MD5
    private String storageType;        // 存储类型

    // 元数据
    private Map<String, Object> metadata;  // 宽高等扩展信息

    // 业务关联（用户填充）
    private String bizType;            // 业务类型（用户自定义）
    private Long bizId;                // 业务ID（用户填充）

    public enum OperationType {

        DELETE("删除"),
        DELETE_BUCKET("删除存储桶"),
        UPDATE("更新");

        private final String type;

        OperationType(String type){
            this.type = type;
        }

        public String getType(){
            return type;
        }
    }
}
