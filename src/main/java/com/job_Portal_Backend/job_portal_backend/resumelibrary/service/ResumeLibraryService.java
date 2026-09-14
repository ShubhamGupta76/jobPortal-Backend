package com.job_Portal_Backend.job_portal_backend.resumelibrary.service;

import com.job_Portal_Backend.job_portal_backend.dto.FileUploadResponseDto;
import com.job_Portal_Backend.job_portal_backend.entity.FileUpload;
import com.job_Portal_Backend.job_portal_backend.entity.ResumeDocument;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.repository.FileUploadRepository;
import com.job_Portal_Backend.job_portal_backend.repository.ResumeDocumentRepository;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import com.job_Portal_Backend.job_portal_backend.resumelibrary.dto.ResumeDocumentDto;
import com.job_Portal_Backend.job_portal_backend.service.FileUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class ResumeLibraryService {

    private static final int MAX_RESUMES_PER_CANDIDATE = 10;

    private final ResumeDocumentRepository resumeDocumentRepository;
    private final FileUploadRepository fileUploadRepository;
    private final FileUploadService fileUploadService;
    private final UserRepository userRepository;

    public ResumeLibraryService(ResumeDocumentRepository resumeDocumentRepository,
            FileUploadRepository fileUploadRepository,
            FileUploadService fileUploadService,
            UserRepository userRepository) {
        this.resumeDocumentRepository = resumeDocumentRepository;
        this.fileUploadRepository = fileUploadRepository;
        this.fileUploadService = fileUploadService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ResumeDocumentDto> list(User user) {
        return resumeDocumentRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ResumeDocumentDto upload(User user, MultipartFile file, String label) throws IOException {
        if (resumeDocumentRepository.countByUserId(user.getId()) >= MAX_RESUMES_PER_CANDIDATE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You can keep at most " + MAX_RESUMES_PER_CANDIDATE + " resumes. Delete one before adding another.");
        }

        FileUploadResponseDto uploaded = fileUploadService.uploadFile(file, "RESUME", null, "DOCUMENT", user);
        FileUpload fileUpload = fileUploadRepository.findById(uploaded.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Uploaded file not found"));

        ResumeDocument resume = new ResumeDocument();
        resume.setUser(user);
        resume.setFileUpload(fileUpload);
        resume.setLabel(hasText(label) ? label.trim() : stripExtension(file.getOriginalFilename()));

        boolean isFirstResume = resumeDocumentRepository.countByUserId(user.getId()) == 0;
        resume.setIsPrimary(isFirstResume);

        ResumeDocument saved = resumeDocumentRepository.save(resume);
        if (isFirstResume) {
            syncPrimaryOnUser(user, saved);
        }
        return toDto(saved);
    }

    @Transactional
    public ResumeDocumentDto rename(User user, Long resumeId, String label) {
        ResumeDocument resume = ownedResume(user, resumeId);
        resume.setLabel(label.trim());
        return toDto(resumeDocumentRepository.save(resume));
    }

    @Transactional
    public ResumeDocumentDto replace(User user, Long resumeId, MultipartFile file) throws IOException {
        ResumeDocument resume = ownedResume(user, resumeId);
        FileUpload previousFile = resume.getFileUpload();

        FileUploadResponseDto uploaded = fileUploadService.uploadFile(file, "RESUME", resumeId, "DOCUMENT", user);
        FileUpload newFile = fileUploadRepository.findById(uploaded.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Uploaded file not found"));

        resume.setFileUpload(newFile);
        ResumeDocument saved = resumeDocumentRepository.save(resume);

        // Soft-delete the previous physical file's record only; the file itself is left on disk
        // because an already-submitted Application may have snapshotted its exact path.
        previousFile.setIsDeleted(true);
        fileUploadRepository.save(previousFile);

        if (Boolean.TRUE.equals(saved.getIsPrimary())) {
            syncPrimaryOnUser(user, saved);
        }
        return toDto(saved);
    }

    @Transactional
    public void delete(User user, Long resumeId) {
        ResumeDocument resume = ownedResume(user, resumeId);
        boolean wasPrimary = Boolean.TRUE.equals(resume.getIsPrimary());

        FileUpload fileUpload = resume.getFileUpload();
        fileUpload.setIsDeleted(true);
        fileUploadRepository.save(fileUpload);
        resumeDocumentRepository.delete(resume);

        if (wasPrimary) {
            Optional<ResumeDocument> nextPrimary = resumeDocumentRepository
                    .findByUserIdOrderByCreatedAtDesc(user.getId())
                    .stream()
                    .findFirst();
            if (nextPrimary.isPresent()) {
                ResumeDocument next = nextPrimary.get();
                next.setIsPrimary(true);
                resumeDocumentRepository.save(next);
                syncPrimaryOnUser(user, next);
            } else {
                user.setResumePath(null);
                userRepository.save(user);
            }
        }
    }

    @Transactional
    public ResumeDocumentDto setPrimary(User user, Long resumeId) {
        ResumeDocument resume = ownedResume(user, resumeId);
        resumeDocumentRepository.findByUserIdAndIsPrimaryTrue(user.getId()).ifPresent(current -> {
            if (!current.getId().equals(resume.getId())) {
                current.setIsPrimary(false);
                resumeDocumentRepository.save(current);
            }
        });
        resume.setIsPrimary(true);
        ResumeDocument saved = resumeDocumentRepository.save(resume);
        syncPrimaryOnUser(user, saved);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public ResumeDocument ownedResumeForDownload(User user, Long resumeId) {
        return ownedResume(user, resumeId);
    }

    private ResumeDocument ownedResume(User user, Long resumeId) {
        return resumeDocumentRepository.findByIdAndUserId(resumeId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));
    }

    private void syncPrimaryOnUser(User user, ResumeDocument resume) {
        user.setResumePath(resume.getFileUpload().getFilePath());
        userRepository.save(user);
    }

    private ResumeDocumentDto toDto(ResumeDocument resume) {
        ResumeDocumentDto dto = new ResumeDocumentDto();
        dto.setId(resume.getId());
        dto.setLabel(resume.getLabel());
        dto.setFilename(resume.getFileUpload().getOriginalFilename());
        dto.setContentType(resume.getFileUpload().getContentType());
        dto.setFileSize(resume.getFileUpload().getFileSize());
        dto.setPrimary(Boolean.TRUE.equals(resume.getIsPrimary()));
        dto.setUploadedAt(resume.getCreatedAt());
        dto.setDownloadUrl("/api/v1/candidate/resumes/" + resume.getId() + "/download");
        return dto;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String stripExtension(String filename) {
        if (filename == null) return "Resume";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
