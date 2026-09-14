package com.job_Portal_Backend.job_portal_backend.resumelibrary.controller;

import com.job_Portal_Backend.job_portal_backend.dto.ApiResponse;
import com.job_Portal_Backend.job_portal_backend.entity.FileUpload;
import com.job_Portal_Backend.job_portal_backend.entity.ResumeDocument;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.resumelibrary.dto.ResumeDocumentDto;
import com.job_Portal_Backend.job_portal_backend.resumelibrary.dto.ResumeRenameRequest;
import com.job_Portal_Backend.job_portal_backend.resumelibrary.service.ResumeLibraryService;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/v1/candidate/resumes")
@PreAuthorize("hasRole('USER')")
public class ResumeLibraryController {

    private final ResumeLibraryService resumeLibraryService;

    public ResumeLibraryController(ResumeLibraryService resumeLibraryService) {
        this.resumeLibraryService = resumeLibraryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResumeDocumentDto>>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Resumes retrieved successfully", resumeLibraryService.list(user)));
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ResumeDocumentDto>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "label", required = false) String label,
            @AuthenticationPrincipal User user) throws IOException {
        return ResponseEntity.ok(new ApiResponse<>(true, "Resume uploaded successfully", resumeLibraryService.upload(user, file, label)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ResumeDocumentDto>> rename(
            @PathVariable Long id,
            @Valid @RequestBody ResumeRenameRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Resume renamed successfully", resumeLibraryService.rename(user, id, request.getLabel())));
    }

    @PostMapping(value = "/{id}/replace", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ResumeDocumentDto>> replace(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {
        return ResponseEntity.ok(new ApiResponse<>(true, "Resume replaced successfully", resumeLibraryService.replace(user, id, file)));
    }

    @PostMapping("/{id}/primary")
    public ResponseEntity<ApiResponse<ResumeDocumentDto>> setPrimary(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Primary resume updated", resumeLibraryService.setPrimary(user, id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, @AuthenticationPrincipal User user) {
        resumeLibraryService.delete(user, id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Resume deleted successfully", null));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean download,
            @AuthenticationPrincipal User user) throws IOException {
        ResumeDocument resume = resumeLibraryService.ownedResumeForDownload(user, id);
        FileUpload fileUpload = resume.getFileUpload();
        Path filePath = Paths.get(fileUpload.getFilePath());
        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("Resume file not found on disk");
        }
        String contentType = fileUpload.getContentType() != null ? fileUpload.getContentType() : "application/pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        (download ? "attachment" : "inline") + "; filename=\"" + fileUpload.getOriginalFilename() + "\"")
                .body(new ByteArrayResource(Files.readAllBytes(filePath)));
    }
}
