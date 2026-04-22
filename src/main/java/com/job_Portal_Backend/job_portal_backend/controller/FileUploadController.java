package com.job_Portal_Backend.job_portal_backend.controller;

import com.job_Portal_Backend.job_portal_backend.dto.FileUploadDto;
import com.job_Portal_Backend.job_portal_backend.dto.FileUploadResponseDto;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponseDto> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("entityType") String entityType,
            @RequestParam(value = "entityId", required = false) Long entityId,
            @RequestParam(value = "fileCategory", defaultValue = "DOCUMENT") String fileCategory,
            @AuthenticationPrincipal User user) throws IOException {

        FileUploadResponseDto response = fileUploadService.uploadFile(file, entityType, entityId, fileCategory, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<FileUploadDto> getFile(@PathVariable Long fileId, @AuthenticationPrincipal User user) {
        FileUploadDto file = fileUploadService.getFileById(fileId, user);
        return ResponseEntity.ok(file);
    }

    @GetMapping("/my-files")
    public ResponseEntity<List<FileUploadDto>> getMyFiles(@AuthenticationPrincipal User user) {
        List<FileUploadDto> files = fileUploadService.getFilesByUser(user);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<List<FileUploadDto>> getFilesByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @AuthenticationPrincipal User user) {

        List<FileUploadDto> files = fileUploadService.getFilesByEntity(entityType, entityId, user);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<FileUploadDto>> getFilesByCategory(
            @PathVariable String category,
            @AuthenticationPrincipal User user) {

        List<FileUploadDto> files = fileUploadService.getFilesByUserAndCategory(user, category);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId, @AuthenticationPrincipal User user)
            throws IOException {
        byte[] fileData = fileUploadService.downloadFile(fileId, user);
        FileUploadDto fileInfo = fileUploadService.getFileById(fileId, user);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileInfo.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileInfo.getOriginalFilename() + "\"")
                .body(new org.springframework.core.io.ByteArrayResource(fileData));
    }

    @GetMapping("/{fileId}/url")
    public ResponseEntity<String> getFileUrl(@PathVariable Long fileId, @AuthenticationPrincipal User user) {
        String url = fileUploadService.getFileDownloadUrl(fileId, user);
        return ResponseEntity.ok(url);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long fileId, @AuthenticationPrincipal User user)
            throws IOException {
        fileUploadService.deleteFile(fileId, user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<FileStats> getFileStats(@AuthenticationPrincipal User user) {
        long totalSize = fileUploadService.getTotalFileSizeByUser(user);
        long fileCount = fileUploadService.countFilesByUser(user);

        FileStats stats = new FileStats(totalSize, fileCount);
        return ResponseEntity.ok(stats);
    }

    // Inner class for file statistics
    public static class FileStats {
        private long totalSize;
        private long fileCount;

        public FileStats(long totalSize, long fileCount) {
            this.totalSize = totalSize;
            this.fileCount = fileCount;
        }

        public long getTotalSize() {
            return totalSize;
        }

        public long getFileCount() {
            return fileCount;
        }
    }
}