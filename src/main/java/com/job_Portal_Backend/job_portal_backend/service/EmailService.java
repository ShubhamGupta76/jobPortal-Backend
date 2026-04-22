package com.job_Portal_Backend.job_portal_backend.service;

import com.job_Portal_Backend.job_portal_backend.entity.User;

public interface EmailService {

        void sendWelcomeEmail(User user);

        void sendJobApplicationConfirmation(User candidate, String jobTitle, String companyName);

        void sendApplicationShortlistedEmail(User candidate, String jobTitle, String companyName);

        void sendApplicationRejectedEmail(User candidate, String jobTitle, String companyName);

        void sendInterviewScheduledEmail(User candidate, String jobTitle, String companyName,
                        String interviewType, String scheduledTime, String meetingLink);

        void sendInterviewReminderEmail(User candidate, String jobTitle, String companyName,
                        String interviewType, String scheduledTime, String meetingLink);

        void sendInterviewRescheduledEmail(User candidate, String jobTitle, String companyName,
                        String newScheduledTime, String meetingLink);

        void sendInterviewCancelledEmail(User candidate, String jobTitle, String companyName);

        void sendJobPostedNotificationToCandidates(String jobTitle, String companyName, String[] candidateEmails);

        void sendPasswordResetEmail(User user, String resetToken);

        void sendAccountVerificationEmail(User user, String verificationToken);

        void sendRecruiterJobPostedEmail(User recruiter, String jobTitle);

        void sendRecruiterApplicationReceivedEmail(User recruiter, String jobTitle, String candidateName,
                        String candidateEmail);

        void sendContactFormEmail(String fromEmail, String subject, String message);

        void sendOtpEmail(String email, String otp);

        boolean isValidEmail(String email);

        void sendBulkEmail(String[] toEmails, String subject, String content);
}
