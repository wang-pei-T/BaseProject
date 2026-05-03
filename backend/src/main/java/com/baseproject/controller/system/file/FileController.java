package com.baseproject.controller.system.file;

import com.baseproject.config.common.ApiResponse;
import com.baseproject.config.common.RequestIdHolder;
import com.baseproject.service.system.file.FileService;
import com.baseproject.security.AuthContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/tenant")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/files:upload")
    public ApiResponse<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bizHint", required = false) String bizHint) throws Exception {
        Map<String, Object> data = fileService.upload(AuthContext.getTenantId(), AuthContext.getUserId(), file, bizHint);
        return ApiResponse.success(data, RequestIdHolder.get());
    }

    @GetMapping("/files/{fileId}:download")
    public ApiResponse<Map<String, Object>> download(@PathVariable("fileId") Long fileId) throws Exception {
        return ApiResponse.success(fileService.downloadUrl(AuthContext.getTenantId(), fileId), RequestIdHolder.get());
    }

    @GetMapping("/files/{fileId}:preview")
    public ApiResponse<Map<String, Object>> preview(@PathVariable("fileId") Long fileId) throws Exception {
        return ApiResponse.success(fileService.previewUrl(AuthContext.getTenantId(), fileId), RequestIdHolder.get());
    }
}
