package com.job_Portal_Backend.job_portal_backend.service;

import com.job_Portal_Backend.job_portal_backend.dto.FileUploadDto;
import com.job_Portal_Backend.job_portal_backend.dto.FileUploadResponseDto;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

public interface FileUploadService {

    FileUploadResponseDto uploadFile(MultipartFile file, String entityType, Long entityId,
            String fileCategory, User user) throws IOException;

    FileUploadDto getFileById(Long fileId, User user);

    List<FileUploadDto> getFilesByUser(User user);

    List<FileUploadDto> getFilesByEntity(String entityType, Long entityId, User user);

    List<FileUploadDto> getFilesByUserAndCategory(User user, String category);

    void deleteFile(Long fileId, User user) throws IOException;

    byte[] downloadFile(Long fileId, User user) throws IOException;

    String getFileDownloadUrl(Long fileId, User user);

    long getTotalFileSizeByUser(User user);

    long countFilesByUser(User user);

    void cleanupOldDeletedFiles(int daysOld);

    boolean isFileOwner(Long fileId, User user);

    String getFilePath(Long fileId);

    String generateUniqueFilename(String originalFilename);
}