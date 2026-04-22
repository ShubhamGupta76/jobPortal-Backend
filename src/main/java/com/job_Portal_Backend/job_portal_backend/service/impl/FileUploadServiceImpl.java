package com.job_Portal_Backend.job_portal_backend.service.impl;

import com.job_Portal_Backend.job_portal_backend.dto.FileUploadDto;
import com.job_Portal_Backend.job_portal_backend.dto.FileUploadResponseDto;
import com.job_Portal_Backend.job_portal_backend.entity.FileUpload;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.repository.FileUploadRepository;
import com.job_Portal_Backend.job_portal_backend.service.FileUploadService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Autowired
    private FileUploadRepository fileUploadRepository;

    @Value("${file.upload.dir:${user.home}/jobportal/uploads}")
    private String uploadDir;

    @Value("${file.max.size:10485760}") // 10MB default
    private long maxFileSize;

    private static final String[] ALLOWED_EXTENSIONS = {
            "pdf", "doc", "docx", "txt", "rtf", "jpg", "jpeg", "png", "gif"
    };

    @Override
    public FileUploadResponseDto uploadFile(MultipartFile file, String entityType, Long entityId,
            String fileCategory, User user) throws IOException {
        // Validate file
        validateFile(file);

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + "." + fileExtension;
        Path filePath = uploadPath.resolve(uniqueFilename);

        // Save file to disk
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Save file metadata to database
        FileUpload fileUpload = new FileUpload();
        fileUpload.setUser(user);
        fileUpload.setOriginalFilename(originalFilename);
        fileUpload.setStoredFilename(uniqueFilename);
        fileUpload.setFilePath(filePath.toString());
        fileUpload.setFileSize(file.getSize());
        fileUpload.setContentType(file.getContentType());
        fileUpload.setEntityType(entityType);
        fileUpload.setEntityId(entityId);
        fileUpload.setFileCategory(fileCategory);
        fileUpload.setUploadedAt(LocalDateTime.now());
        fileUpload.setIsDeleted(false);

        FileUpload savedFile = fileUploadRepository.save(fileUpload);

        return convertToResponseDto(savedFile);
    }

    @Override
    public FileUploadDto getFileById(Long fileId, User user) {
        Optional<FileUpload> fileUpload = fileUploadRepository.findByIdAndUserAndIsDeletedFalse(fileId, user);
        return fileUpload.map(this::convertToDto).orElse(null);
    }

    @Override
    public List<FileUploadDto> getFilesByEntity(String entityType, Long entityId, User user) {
        List<FileUpload> files = fileUploadRepository.findByEntityTypeAndEntityIdAndUserAndIsDeletedFalse(entityType,
                entityId, user);
        return files.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<FileUploadDto> getFilesByUser(User user) {
        List<FileUpload> files = fileUploadRepository.findByUserAndIsDeletedFalse(user);
        return files.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<FileUploadDto> getFilesByUserAndCategory(User user, String category) {
        List<FileUpload> files = fileUploadRepository.findByUserAndCategory(user, category);
        return files.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteFile(Long fileId, User user) throws IOException {
        Optional<FileUpload> fileUpload = fileUploadRepository.findByIdAndUserAndIsDeletedFalse(fileId, user);
        if (fileUpload.isPresent()) {
            FileUpload file = fileUpload.get();
            file.setIsDeleted(true);
            fileUploadRepository.save(file);

            // Delete physical file
            Files.deleteIfExists(Paths.get(file.getFilePath()));
        } else {
            throw new IOException("File not found or access denied");
        }
    }

    @Override
    public byte[] downloadFile(Long fileId, User user) throws IOException {
        Optional<FileUpload> fileUpload = fileUploadRepository.findByIdAndUserAndIsDeletedFalse(fileId, user);
        if (fileUpload.isPresent()) {
            Path filePath = Paths.get(fileUpload.get().getFilePath());
            if (Files.exists(filePath)) {
                return Files.readAllBytes(filePath);
            } else {
                throw new IOException("File not found on disk");
            }
        }
        throw new IOException("File record not found in database");
    }

    @Override
    public String getFileDownloadUrl(Long fileId, User user) {
        Optional<FileUpload> fileUpload = fileUploadRepository.findByIdAndUserAndIsDeletedFalse(fileId, user);
        return fileUpload.map(file -> "/api/v1/files/" + file.getId() + "/download").orElse(null);
    }

    @Override
    public long getTotalFileSizeByUser(User user) {
        Long size = fileUploadRepository.getTotalFileSizeByUser(user);
        return size != null ? size : 0L;
    }

    @Override
    public long countFilesByUser(User user) {
        return fileUploadRepository.countByUserAndNotDeleted(user);
    }

    @Override
    public void cleanupOldDeletedFiles(int daysOld) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysOld);
        List<FileUpload> oldFiles = fileUploadRepository.findSoftDeletedFilesOlderThan(cutoff);
        for (FileUpload file : oldFiles) {
            try {
                Files.deleteIfExists(Paths.get(file.getFilePath()));
            } catch (IOException e) {
                // Silently ignore delete errors
            }
        }
    }

    @Override
    public boolean isFileOwner(Long fileId, User user) {
        return fileUploadRepository.findByIdAndUserAndIsDeletedFalse(fileId, user).isPresent();
    }

    @Override
    public String getFilePath(Long fileId) {
        Optional<FileUpload> fileOpt = fileUploadRepository.findById(fileId);
        return fileOpt.filter(f -> !f.isDeleted()).map(FileUpload::getFilePath).orElse(null);
    }

    @Override
    public String generateUniqueFilename(String originalFilename) {
        String fileExtension = getFileExtension(originalFilename);
        return UUID.randomUUID().toString() + "." + fileExtension;
    }

    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new IOException("File size exceeds maximum allowed size: " + maxFileSize + " bytes");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IOException("Invalid filename");
        }

        String fileExtension = getFileExtension(originalFilename).toLowerCase();
        boolean isAllowed = false;
        for (String allowedExt : ALLOWED_EXTENSIONS) {
            if (allowedExt.equals(fileExtension)) {
                isAllowed = true;
                break;
            }
        }

        if (!isAllowed) {
            throw new IOException("File type not allowed. Allowed types: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }

    private FileUploadDto convertToDto(FileUpload fileUpload) {
        return FileUploadDto.builder()
                .id(fileUpload.getId())
                .userId(fileUpload.getUser().getId())
                .userEmail(fileUpload.getUser().getEmail())
                .originalFilename(fileUpload.getOriginalFilename())
                .storedFilename(fileUpload.getStoredFilename())
                .filePath(fileUpload.getFilePath())
                .fileSize(fileUpload.getFileSize())
                .contentType(fileUpload.getContentType())
                .entityType(fileUpload.getEntityType())
                .entityId(fileUpload.getEntityId())
                .fileCategory(fileUpload.getFileCategory())
                .uploadedAt(fileUpload.getUploadedAt())
                .build();
    }

    private FileUploadResponseDto convertToResponseDto(FileUpload fileUpload) {
        return FileUploadResponseDto.builder()
                .id(fileUpload.getId())
                .originalFilename(fileUpload.getOriginalFilename())
                .storedFilename(fileUpload.getStoredFilename())
                .filePath(fileUpload.getFilePath())
                .fileSize(fileUpload.getFileSize())
                .contentType(fileUpload.getContentType())
                .entityType(fileUpload.getEntityType())
                .entityId(fileUpload.getEntityId())
                .fileCategory(fileUpload.getFileCategory())
                .downloadUrl("/api/v1/files/" + fileUpload.getId() + "/download")
                .build();
    }
}