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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ResumeLibraryServiceTest {

    private final ResumeDocumentRepository resumeDocumentRepository = mock(ResumeDocumentRepository.class);
    private final FileUploadRepository fileUploadRepository = mock(FileUploadRepository.class);
    private final FileUploadService fileUploadService = mock(FileUploadService.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    private final ResumeLibraryService service = new ResumeLibraryService(
            resumeDocumentRepository, fileUploadRepository, fileUploadService, userRepository);

    @Test
    void firstUploadedResumeBecomesPrimaryAndSyncsUser() throws Exception {
        User candidate = user(1L);
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "content".getBytes());

        when(resumeDocumentRepository.countByUserId(1L)).thenReturn(0L);
        when(fileUploadService.uploadFile(any(), eq("RESUME"), any(), eq("DOCUMENT"), eq(candidate)))
                .thenReturn(FileUploadResponseDto.builder().id(100L).originalFilename("resume.pdf").build());
        FileUpload fileUpload = fileUpload(100L, "resume.pdf", "/uploads/resume.pdf");
        when(fileUploadRepository.findById(100L)).thenReturn(Optional.of(fileUpload));
        when(resumeDocumentRepository.save(any(ResumeDocument.class))).thenAnswer(invocation -> {
            ResumeDocument doc = invocation.getArgument(0);
            doc.setId(5L);
            return doc;
        });

        ResumeDocumentDto result = service.upload(candidate, file, "My Resume");

        assertTrue(result.isPrimary());
        assertEquals("My Resume", result.getLabel());
        verify(userRepository).save(argThat(u -> "/uploads/resume.pdf".equals(u.getResumePath())));
    }

    @Test
    void deletingPrimaryResumePromotesNextMostRecent() {
        User candidate = user(1L);
        ResumeDocument toDelete = resumeDocument(5L, candidate, true, "/uploads/old.pdf");
        ResumeDocument remaining = resumeDocument(6L, candidate, false, "/uploads/other.pdf");

        when(resumeDocumentRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(toDelete));
        when(resumeDocumentRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(remaining));

        service.delete(candidate, 5L);

        ArgumentCaptor<FileUpload> fileCaptor = ArgumentCaptor.forClass(FileUpload.class);
        verify(fileUploadRepository).save(fileCaptor.capture());
        assertTrue(fileCaptor.getValue().getIsDeleted());

        verify(resumeDocumentRepository).delete(toDelete);
        assertTrue(remaining.getIsPrimary());
        verify(userRepository).save(argThat(u -> "/uploads/other.pdf".equals(u.getResumePath())));
    }

    @Test
    void deletingLastResumeClearsUserResumePath() {
        User candidate = user(1L);
        ResumeDocument onlyResume = resumeDocument(5L, candidate, true, "/uploads/only.pdf");

        when(resumeDocumentRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(onlyResume));
        when(resumeDocumentRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        service.delete(candidate, 5L);

        verify(userRepository).save(argThat(u -> u.getResumePath() == null));
    }

    @Test
    void cannotAccessAnotherCandidatesResume() {
        User candidate = user(1L);
        when(resumeDocumentRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.rename(candidate, 99L, "New name"));
    }

    @Test
    void replaceDoesNotHardDeletePreviousFileOnDisk() throws Exception {
        User candidate = user(1L);
        FileUpload oldFile = fileUpload(10L, "old.pdf", "/uploads/old.pdf");
        ResumeDocument resume = resumeDocument(5L, candidate, true, oldFile);

        when(resumeDocumentRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(resume));
        when(fileUploadService.uploadFile(any(), eq("RESUME"), eq(5L), eq("DOCUMENT"), eq(candidate)))
                .thenReturn(FileUploadResponseDto.builder().id(11L).originalFilename("new.pdf").build());
        FileUpload newFile = fileUpload(11L, "new.pdf", "/uploads/new.pdf");
        when(fileUploadRepository.findById(11L)).thenReturn(Optional.of(newFile));
        when(resumeDocumentRepository.save(any(ResumeDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "new.pdf", "application/pdf", "content".getBytes());
        service.replace(candidate, 5L, file);

        // The old FileUpload row is only soft-deleted (isDeleted=true saved) via the repository;
        // ResumeLibraryService has no dependency capable of deleting a file from disk.
        verify(fileUploadRepository).save(argThat(f -> f.getId().equals(10L) && f.getIsDeleted()));
        verify(fileUploadService, times(1)).uploadFile(any(), eq("RESUME"), eq(5L), eq("DOCUMENT"), eq(candidate));
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("candidate" + id + "@example.com");
        return user;
    }

    private FileUpload fileUpload(Long id, String filename, String path) {
        FileUpload fileUpload = new FileUpload();
        fileUpload.setId(id);
        fileUpload.setOriginalFilename(filename);
        fileUpload.setFilePath(path);
        fileUpload.setIsDeleted(false);
        return fileUpload;
    }

    private ResumeDocument resumeDocument(Long id, User user, boolean primary, String path) {
        return resumeDocument(id, user, primary, fileUpload(id * 10, "resume.pdf", path));
    }

    private ResumeDocument resumeDocument(Long id, User user, boolean primary, FileUpload fileUpload) {
        ResumeDocument doc = new ResumeDocument();
        doc.setId(id);
        doc.setUser(user);
        doc.setFileUpload(fileUpload);
        doc.setLabel("Resume");
        doc.setIsPrimary(primary);
        return doc;
    }
}
