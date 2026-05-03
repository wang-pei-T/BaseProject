package com.baseproject.service.system.file;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baseproject.common.MinioProperties;
import com.baseproject.domain.system.file.SysFileMeta;
import com.baseproject.mapper.system.file.SysFileMetaMapper;
import com.baseproject.service.system.config.ConfigParsers;
import com.baseproject.service.system.config.EffectiveConfigService;
import com.baseproject.service.system.tenantaudit.TenantAuditEvents;
import com.baseproject.service.system.tenantaudit.TenantAuditLogBridge;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@DS("core")
public class FileService {

    private static final long ABSOLUTE_MAX_BYTES = 50L * 1024 * 1024;

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final SysFileMetaMapper fileMetaMapper;
    private final EffectiveConfigService effectiveConfigService;
    private final TenantAuditLogBridge tenantAuditLogBridge;
    private final ObjectMapper objectMapper;

    public FileService(
            MinioClient minioClient,
            MinioProperties minioProperties,
            SysFileMetaMapper fileMetaMapper,
            EffectiveConfigService effectiveConfigService,
            TenantAuditLogBridge tenantAuditLogBridge,
            ObjectMapper objectMapper) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
        this.fileMetaMapper = fileMetaMapper;
        this.effectiveConfigService = effectiveConfigService;
        this.tenantAuditLogBridge = tenantAuditLogBridge;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> upload(Long tenantId, Long userId, MultipartFile file, String bizHint) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("VALIDATION_ERROR");
        }
        long maxBytes = effectiveConfigService.getMaxUploadBytes(tenantId, ABSOLUTE_MAX_BYTES);
        if (file.getSize() > maxBytes) {
            throw new RuntimeException("FILE_TOO_LARGE");
        }
        String allowed = effectiveConfigService.getAllowedUploadTypes(tenantId);
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename().replaceAll("[\\\\/]", "_");
        if (!ConfigParsers.isAllowedExtension(original, allowed)) {
            throw new RuntimeException("FILE_TYPE_NOT_ALLOWED");
        }
        ensureBucket();
        String uid = UUID.randomUUID().toString().replace("-", "");
        String objectKey = tenantId + "/" + userId + "/" + uid + "/" + original;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        InputStream raw = file.getInputStream();
        DigestInputStream dis = new DigestInputStream(raw, md);
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioProperties.getBucket())
                        .object(objectKey)
                        .stream(dis, file.getSize(), -1)
                        .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                        .build());
        String sha256 = toHex(md.digest());
        long now = System.currentTimeMillis();
        SysFileMeta meta = new SysFileMeta();
        meta.setTenantId(tenantId);
        meta.setBucketName(minioProperties.getBucket());
        meta.setObjectKey(objectKey);
        meta.setOriginalFilename(original);
        meta.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        meta.setSizeBytes(file.getSize());
        meta.setSha256Hex(sha256);
        meta.setBizHint(bizHint);
        meta.setCreatedBy(userId);
        meta.setDeletedAt(null);
        meta.setCreatedAt(now);
        fileMetaMapper.insert(meta);
        String diff;
        try {
            diff = objectMapper.writeValueAsString(
                    java.util.Collections.singletonMap("fileId", String.valueOf(meta.getId())));
        } catch (Exception e) {
            diff = "{\"fileId\":\"" + meta.getId() + "\"}";
        }
        tenantAuditLogBridge.recordAuditAndLog(
                tenantId, userId, TenantAuditEvents.FILE_UPLOAD, String.valueOf(meta.getId()), diff, "file", null);
        return toUploadResponse(meta);
    }

    public Map<String, Object> downloadUrl(Long tenantId, Long fileId) throws Exception {
        SysFileMeta rec = getFile(tenantId, fileId);
        String url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(rec.getBucketName())
                        .object(rec.getObjectKey())
                        .expiry(10, TimeUnit.MINUTES)
                        .build());
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("url", url);
        m.put("expiresAt", Instant.now().plusSeconds(600).toString());
        return m;
    }

    public Map<String, Object> previewUrl(Long tenantId, Long fileId) throws Exception {
        SysFileMeta rec = getFile(tenantId, fileId);
        String ct = rec.getContentType() == null ? "" : rec.getContentType().toLowerCase();
        boolean image = ct.startsWith("image/");
        boolean pdf = "application/pdf".equals(ct);
        if (!image && !pdf) {
            throw new RuntimeException("FILE_NOT_PREVIEWABLE");
        }
        Map<String, Object> m = downloadUrl(tenantId, fileId);
        m.put("previewType", image ? "IMAGE" : "PDF");
        return m;
    }

    public SysFileMeta getFile(Long tenantId, Long fileId) {
        SysFileMeta rec = fileMetaMapper.selectById(fileId);
        if (rec == null || !rec.getTenantId().equals(tenantId) || rec.getDeletedAt() != null) {
            throw new RuntimeException("FILE_NOT_FOUND");
        }
        return rec;
    }

    private void ensureBucket() throws Exception {
        String bucket = minioProperties.getBucket();
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private Map<String, Object> toUploadResponse(SysFileMeta rec) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("fileId", String.valueOf(rec.getId()));
        m.put("filename", rec.getOriginalFilename());
        m.put("size", rec.getSizeBytes());
        m.put("contentType", rec.getContentType());
        m.put("sha256", rec.getSha256Hex());
        m.put("createdAt", Instant.ofEpochMilli(rec.getCreatedAt()).toString());
        return m;
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
