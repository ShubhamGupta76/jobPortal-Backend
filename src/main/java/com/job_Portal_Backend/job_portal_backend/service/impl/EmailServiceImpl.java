package com.job_Portal_Backend.job_portal_backend.service.impl;

import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:Job Portal}")
    private String appName;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void sendWelcomeEmail(User user) {
        String subject = "Welcome to " + appName + "!";
        String content = String.format("""
                Dear %s,

                Welcome to %s! Your account has been successfully created.

                You can now:
                - Browse and apply for jobs
                - Create and manage your profile
                - Track your applications
                - Receive notifications about new opportunities

                If you have any questions, feel free to contact our support team.

                Best regards,
                The %s Team
                """, user.getFirstName(), appName, appName);

        sendEmail(user.getEmail(), subject, content);
    }

    @Override
    public void sendJobApplicationConfirmation(User candidate, String jobTitle, String companyName) {
        String subject = "Application Submitted - " + jobTitle;
        String content = String.format("""
                Dear %s,

                Your application for the position "%s" at %s has been successfully submitted.

                What happens next:
                1. Our team will review your application
                2. You may be contacted for additional information
                3. If shortlisted, you'll be invited for an interview

                You can track your application status in your dashboard.

                Best regards,
                The %s Team
                """, candidate.getFirstName(), jobTitle, companyName, appName);

        sendEmail(candidate.getEmail(), subject, content);
    }

    @Override
    public void sendApplicationShortlistedEmail(User candidate, String jobTitle, String companyName) {
        String subject = "Congratulations! Your application has been shortlisted";
        String content = String.format(
                """
                        Dear %s,

                        Great news! Your application for "%s" at %s has been shortlisted.

                        The recruiter will contact you soon to schedule an interview. Please ensure your contact information is up to date in your profile.

                        Best regards,
                        The %s Team
                        """,
                candidate.getFirstName(), jobTitle, companyName, appName);

        sendEmail(candidate.getEmail(), subject, content);
    }

    @Override
    public void sendApplicationRejectedEmail(User candidate, String jobTitle, String companyName) {
        String subject = "Application Update - " + jobTitle;
        String content = String.format(
                """
                        Dear %s,

                        Thank you for your interest in the "%s" position at %s.

                        After careful consideration, we have decided to move forward with other candidates whose qualifications more closely match our current needs.

                        We encourage you to apply for future opportunities that match your skills and experience.

                        Best regards,
                        The %s Team
                        """,
                candidate.getFirstName(), jobTitle, companyName, appName);

        sendEmail(candidate.getEmail(), subject, content);
    }

    @Override
    public void sendInterviewScheduledEmail(User candidate, String jobTitle, String companyName,
            String interviewType, String scheduledTime, String meetingLink) {
        String subject = "Interview Scheduled - " + jobTitle;
        String content = String.format("""
                Dear %s,

                Your interview for "%s" at %s has been scheduled!

                Interview Details:
                - Type: %s
                - Date & Time: %s
                - Meeting Link: %s

                Please join the meeting a few minutes early. If you have any questions, contact the recruiter directly.

                Best regards,
                The %s Team
                """, candidate.getFirstName(), jobTitle, companyName, interviewType, scheduledTime,
                meetingLink != null ? meetingLink : "To be provided", appName);

        sendEmail(candidate.getEmail(), subject, content);
    }

    @Override
    public void sendInterviewReminderEmail(User candidate, String jobTitle, String companyName,
            String interviewType, String scheduledTime, String meetingLink) {
        String subject = "Interview Reminder - " + jobTitle + " (Tomorrow)";
        String content = String.format("""
                Dear %s,

                This is a reminder about your upcoming interview for "%s" at %s.

                Interview Details:
                - Type: %s
                - Date & Time: %s
                - Meeting Link: %s

                Please ensure you have a stable internet connection and join a few minutes early.

                Best regards,
                The %s Team
                """, candidate.getFirstName(), jobTitle, companyName, interviewType, scheduledTime,
                meetingLink != null ? meetingLink : "To be provided", appName);

        sendEmail(candidate.getEmail(), subject, content);
    }

    @Override
    public void sendInterviewRescheduledEmail(User candidate, String jobTitle, String companyName,
            String newScheduledTime, String meetingLink) {
        String subject = "Interview Rescheduled - " + jobTitle;
        String content = String.format("""
                Dear %s,

                Your interview for "%s" at %s has been rescheduled.

                New Interview Details:
                - Date & Time: %s
                - Meeting Link: %s

                Please update your calendar accordingly.

                Best regards,
                The %s Team
                """, candidate.getFirstName(), jobTitle, companyName, newScheduledTime,
                meetingLink != null ? meetingLink : "To be provided", appName);

        sendEmail(candidate.getEmail(), subject, content);
    }

    @Override
    public void sendInterviewCancelledEmail(User candidate, String jobTitle, String companyName) {
        String subject = "Interview Cancelled - " + jobTitle;
        String content = String.format(
                """
                        Dear %s,

                        We regret to inform you that your interview for "%s" at %s has been cancelled.

                        We apologize for any inconvenience this may cause. We will keep your application on file for future opportunities.

                        Best regards,
                        The %s Team
                        """,
                candidate.getFirstName(), jobTitle, companyName, appName);

        sendEmail(candidate.getEmail(), subject, content);
    }

    @Override
    public void sendJobPostedNotificationToCandidates(String jobTitle, String companyName, String[] candidateEmails) {
        String subject = "New Job Opportunity - " + jobTitle;
        String content = String.format("""
                Dear Job Seeker,

                A new job opportunity has been posted that might interest you!

                Job Details:
                - Position: %s
                - Company: %s

                Visit our platform to view the full job description and apply.

                Best regards,
                The %s Team
                """, jobTitle, companyName, appName);

        sendBulkEmail(candidateEmails, subject, content);
    }

    @Override
    public void sendPasswordResetEmail(User user, String resetToken) {
        String subject = "Password Reset Request";
        String resetLink = "http://localhost:3000/reset-password?token=" + resetToken; // TODO: Make configurable
        String content = String.format("""
                Dear %s,

                You have requested to reset your password for your %s account.

                Click the link below to reset your password:
                %s

                This link will expire in 24 hours.

                If you didn't request this password reset, please ignore this email.

                Best regards,
                The %s Team
                """, user.getFirstName(), appName, resetLink, appName);

        sendEmail(user.getEmail(), subject, content);
    }

    @Override
    public void sendAccountVerificationEmail(User user, String verificationToken) {
        String subject = "Verify Your Account";
        String verificationLink = "http://localhost:3000/verify?token=" + verificationToken; // TODO: Make configurable
        String content = String.format("""
                Dear %s,

                Welcome to %s! Please verify your email address to complete your registration.

                Click the link below to verify your account:
                %s

                If you didn't create this account, please ignore this email.

                Best regards,
                The %s Team
                """, user.getFirstName(), appName, verificationLink, appName);

        sendEmail(user.getEmail(), subject, content);
    }

    @Override
    public void sendRecruiterJobPostedEmail(User recruiter, String jobTitle) {
        String subject = "Job Posted Successfully - " + jobTitle;
        String content = String.format("""
                Dear %s,

                Your job posting "%s" has been successfully published on %s.

                You can now:
                - View applications in your recruiter dashboard
                - Manage interview schedules
                - Update job details as needed

                Best regards,
                The %s Team
                """, recruiter.getFirstName(), jobTitle, appName, appName);

        sendEmail(recruiter.getEmail(), subject, content);
    }

    @Override
    public void sendRecruiterApplicationReceivedEmail(User recruiter, String jobTitle, String candidateName,
            String candidateEmail) {
        String subject = "New Application Received - " + jobTitle;
        String content = String.format("""
                Dear %s,

                A new application has been received for your job posting "%s".

                Applicant Details:
                - Name: %s
                - Email: %s

                Please review the application in your recruiter dashboard and take appropriate action.

                Best regards,
                The %s Team
                """, recruiter.getFirstName(), jobTitle, candidateName, candidateEmail, appName);

        sendEmail(recruiter.getEmail(), subject, content);
    }

    @Override
    public void sendContactFormEmail(String fromEmail, String subject, String message) {
        String fullSubject = "Contact Form: " + subject;
        String content = String.format("""
                New contact form submission:

                From: %s
                Subject: %s

                Message:
                %s
                """, fromEmail, subject, message);

        sendEmail(this.fromEmail, fullSubject, content); // Send to admin/support email
    }

    @Override
    public void sendOtpEmail(String email, String otp) {
        String subject = "Your JobPortal Verification Code";
        String content = String.format("""
                Dear User,

                Your JobPortal verification code is:

                🔐 %s 🔐

                This code will expire in 5 minutes. Do not share this code with anyone.

                Best regards,
                The JobPortal Team
                """, otp);

        System.out.println("=== SENDING OTP [" + otp + "] to [" + email + "] ===");
        sendEmail(email, subject, content);
    }

    @Override
    public boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    @Override
    public void sendBulkEmail(String[] toEmails, String subject, String content) {
        Arrays.stream(toEmails)
                .filter(this::isValidEmail)
                .forEach(email -> sendEmail(email, subject, content));
    }

    private void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, false); // false = plain text

            mailSender.send(message);
        } catch (MessagingException e) {
            // Log the error but don't throw exception to avoid breaking the flow
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
        }
    }
}