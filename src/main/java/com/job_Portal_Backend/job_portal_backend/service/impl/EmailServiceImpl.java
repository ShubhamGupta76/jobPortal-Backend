package com.job_Portal_Backend.job_portal_backend.service.impl;

import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:Job Portal}")
    private String appName;

    private String buildHtmlWrapper(String title, String greeting, String bodyContent) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                    <style>
                        body {
                            font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, Helvetica, Arial, sans-serif;
                            background-color: #f8fafc;
                            color: #1e293b;
                            margin: 0;
                            padding: 0;
                            -webkit-font-smoothing: antialiased;
                            -moz-osx-font-smoothing: grayscale;
                        }
                        .container {
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                        }
                        .header {
                            background: linear-gradient(135deg, #4f46e5 0%%, #7c3aed 100%%);
                            padding: 30px 20px;
                            border-radius: 12px 12px 0 0;
                            text-align: center;
                        }
                        .header h1 {
                            color: #ffffff;
                            font-size: 24px;
                            margin: 0;
                            font-weight: 700;
                            letter-spacing: -0.5px;
                        }
                        .card {
                            background-color: #ffffff;
                            border: 1px solid #e2e8f0;
                            border-radius: 0 0 12px 12px;
                            padding: 30px;
                            box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.02);
                        }
                        .greeting {
                            font-size: 18px;
                            font-weight: 600;
                            color: #0f172a;
                            margin-top: 0;
                            margin-bottom: 16px;
                        }
                        .content {
                            font-size: 15px;
                            line-height: 1.6;
                            color: #334155;
                            margin-bottom: 24px;
                        }
                        .highlight-box {
                            background-color: #f1f5f9;
                            border-left: 4px solid #4f46e5;
                            border-radius: 4px;
                            padding: 16px;
                            margin: 20px 0;
                        }
                        .otp-code {
                            font-family: 'Courier New', Courier, monospace;
                            font-size: 32px;
                            font-weight: 700;
                            letter-spacing: 6px;
                            color: #4f46e5;
                            text-align: center;
                            margin: 20px 0;
                            padding: 10px;
                            background-color: #e0e7ff;
                            border-radius: 8px;
                        }
                        .btn {
                            display: inline-block;
                            background: linear-gradient(135deg, #4f46e5 0%%, #7c3aed 100%%);
                            color: #ffffff !important;
                            text-decoration: none;
                            padding: 12px 24px;
                            border-radius: 8px;
                            font-weight: 600;
                            font-size: 15px;
                            text-align: center;
                            margin: 20px 0;
                            box-shadow: 0 4px 6px -1px rgba(79, 70, 229, 0.2);
                        }
                        .bullet-list {
                            margin: 16px 0;
                            padding-left: 20px;
                        }
                        .bullet-list li {
                            margin-bottom: 8px;
                            color: #334155;
                        }
                        .footer {
                            text-align: center;
                            margin-top: 30px;
                            font-size: 12px;
                            color: #64748b;
                            line-height: 1.5;
                        }
                        .footer a {
                            color: #4f46e5;
                            text-decoration: none;
                        }
                        .divider {
                            height: 1px;
                            background-color: #e2e8f0;
                            margin: 24px 0;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>%s</h1>
                        </div>
                        <div class="card">
                            <p class="greeting">%s</p>
                            <div class="content">
                                %s
                            </div>
                            <div class="divider"></div>
                            <p style="margin: 0; font-size: 14px; color: #64748b;">
                                Best regards,<br>
                                <strong>The %s Team</strong>
                            </p>
                        </div>
                        <div class="footer">
                            <p>This is an automated message from %s. Please do not reply directly to this email.</p>
                            <p>&copy; 2026 %s. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, title, title, greeting, bodyContent, appName, appName, appName);
    }

    @Override
    public void sendWelcomeEmail(User user) {
        String subject = "Welcome to " + appName + "!";
        String greeting = String.format("Dear %s,", user.getFirstName());
        String body = String.format("""
                <p>Welcome to <strong>%s</strong>! Your account has been successfully created.</p>
                <p>You can now start exploring the platform:</p>
                <ul class="bullet-list">
                    <li>Browse and apply for matching job opportunities</li>
                    <li>Create and manage a professional profile</li>
                    <li>Track your applications in real-time</li>
                    <li>Receive instant notifications about your application status</li>
                </ul>
                <p>If you have any questions, feel free to contact our support team at any time.</p>
                """, appName);

        String htmlContent = buildHtmlWrapper(subject, greeting, body);
        sendEmail(user.getEmail(), subject, htmlContent);
    }

    @Override
    public void sendJobApplicationConfirmation(User candidate, String jobTitle, String companyName) {
        String subject = "Application Submitted - " + jobTitle;
        String greeting = String.format("Dear %s,", candidate.getFirstName());
        String body = String.format("""
                <p>Your application for the position <strong>%s</strong> at <strong>%s</strong> has been successfully submitted.</p>
                <div class="highlight-box">
                    <strong>What happens next:</strong>
                    <ol style="margin: 8px 0 0 20px; padding: 0;">
                        <li>Our recruiting team will review your qualifications and resume.</li>
                        <li>You may be contacted if we need additional details.</li>
                        <li>If shortlisted, you will receive an invitation for an interview.</li>
                    </ol>
                </div>
                <p>You can track the live status of your application anytime directly in your user dashboard.</p>
                """, jobTitle, companyName);

        String htmlContent = buildHtmlWrapper(subject, greeting, body);
        sendEmail(candidate.getEmail(), subject, htmlContent);
    }

    @Override
    public void sendApplicationShortlistedEmail(User candidate, String jobTitle, String companyName) {
        String subject = "Congratulations! Your application has been shortlisted";
        String greeting = String.format("Dear %s,", candidate.getFirstName());
        String body = String.format("""
                <p style="font-size: 16px; color: #10b981; font-weight: 600;">Great news!</p>
                <p>Your application for <strong>%s</strong> at <strong>%s</strong> has been shortlisted.</p>
                <div class="highlight-box" style="border-left-color: #10b981;">
                    The recruiter will contact you soon to schedule an interview. Please ensure your contact information is up to date in your profile.
                </div>
                """, jobTitle, companyName);

        String htmlContent = buildHtmlWrapper(subject, greeting, body);
        sendEmail(candidate.getEmail(), subject, htmlContent);
    }

    @Override
    public void sendApplicationRejectedEmail(User candidate, String jobTitle, String companyName) {
        String subject = "Application Update - " + jobTitle;
        String greeting = String.format("Dear %s,", candidate.getFirstName());
        String body = String.format("""
                <p>Thank you for your interest in the <strong>%s</strong> position at <strong>%s</strong>.</p>
                <p>After careful consideration of all applications, we have decided to move forward with other candidates whose qualifications more closely align with our current needs.</p>
                <p>We appreciate the time you took to apply and encourage you to explore and apply for future opportunities that match your skills.</p>
                """, jobTitle, companyName);

        String htmlContent = buildHtmlWrapper(subject, greeting, body);
        sendEmail(candidate.getEmail(), subject, htmlContent);
    }

    @Override
    public void sendInterviewScheduledEmail(User candidate, String jobTitle, String companyName,
            String interviewType, String scheduledTime, String meetingLink) {
        String subject = "Interview Scheduled - " + jobTitle;
        String greeting = String.format("Dear %s,", candidate.getFirstName());
        
        String resolvedLink = meetingLink != null && !meetingLink.isEmpty() && !meetingLink.equals("To be provided") 
                ? String.format("<a href=\"%s\" class=\"btn\">Join Meeting</a>", meetingLink) 
                : "<p><em>Meeting link will be provided shortly by the recruiter.</em></p>";
                
        String body = String.format("""
                <p>Your interview for <strong>%s</strong> at <strong>%s</strong> has been scheduled!</p>
                <div class="highlight-box">
                    <strong>Interview Details:</strong>
                    <ul style="margin: 8px 0 0 0; padding-left: 20px;">
                        <li><strong>Type:</strong> %s</li>
                        <li><strong>Date & Time:</strong> %s</li>
                    </ul>
                </div>
                <p>Please test your hardware and join the meeting a few minutes early. If you need to reschedule, please contact the recruiter as soon as possible.</p>
                <div style="text-align: center;">
                    %s
                </div>
                """, jobTitle, companyName, interviewType, scheduledTime, resolvedLink);

        String htmlContent = buildHtmlWrapper(subject, greeting, body);
        sendEmail(candidate.getEmail(), subject, htmlContent);
    }

    @Override
    public void sendInterviewReminderEmail(User candidate, String jobTitle, String companyName,
            String interviewType, String scheduledTime, String meetingLink) {
        String subject = "Interview Reminder - " + jobTitle + " (Tomorrow)";
        String greeting = String.format("Dear %s,", candidate.getFirstName());
        
        String resolvedLink = meetingLink != null && !meetingLink.isEmpty() && !meetingLink.equals("To be provided") 
                ? String.format("<a href=\"%s\" class=\"btn\">Join Meeting</a>", meetingLink) 
                : "<p><em>Meeting link will be provided shortly.</em></p>";

        String body = String.format("""
                <p>This is a reminder about your upcoming interview for <strong>%s</strong> at <strong>%s</strong> tomorrow.</p>
                <div class="highlight-box">
                    <strong>Interview Details:</strong>
                    <ul style="margin: 8px 0 0 0; padding-left: 20px;">
                        <li><strong>Type:</strong> %s</li>
                        <li><strong>Date & Time:</strong> %s</li>
                    </ul>
                </div>
                <p>Please ensure you have a stable internet connection, functional camera/microphone, and join the meeting a few minutes early.</p>
                <div style="text-align: center;">
                    %s
                </div>
                """, jobTitle, companyName, interviewType, scheduledTime, resolvedLink);

        String htmlContent = buildHtmlWrapper(subject, greeting, body);
        sendEmail(candidate.getEmail(), subject, htmlContent);
    }

    @Override
    public void sendInterviewRescheduledEmail(User candidate, String jobTitle, String companyName,
            String newScheduledTime, String meetingLink) {
        String subject = "Interview Rescheduled - " + jobTitle;
        String greeting = String.format("Dear %s,", candidate.getFirstName());

        String resolvedLink = meetingLink != null && !meetingLink.isEmpty() && !meetingLink.equals("To be provided") 
                ? String.format("<a href=\"%s\" class=\"btn\">Join Meeting</a>", meetingLink) 
                : "<p><em>Meeting link will be provided shortly.</em></p>";

        String body = String.format("""
                <p>Your interview for <strong>%s</strong> at <strong>%s</strong> has been rescheduled.</p>
                <div class="highlight-box">
                    <strong>New Interview Details:</strong>
                    <ul style="margin: 8px 0 0 0; padding-left: 20px;">
                        <li><strong>Date & Time:</strong> %s</li>
                    </ul>
                </div>
                <p>Please update your calendar details accordingly.</p>
                <div style="text-align: center;">
                    %s
                </div>
                """, jobTitle, companyName, newScheduledTime, resolvedLink);

        String htmlContent = buildHtmlWrapper(subject, greeting, body);
        sendEmail(candidate.getEmail(), subject, htmlContent);
    }

    @Override
    public void sendInterviewCancelledEmail(User candidate, String jobTitle, String companyName) {
        String subject = "Interview Cancelled - " + jobTitle;
        String greeting = String.format("Dear %s,", candidate.getFirstName());
        String body = String.format("""
                <p style="color: #ef4444; font-weight: 600;">Interview Cancelled</p>
                <p>We regret to inform you that your scheduled interview for <strong>%s</strong> at <strong>%s</strong> has been cancelled.</p>
                <div class="highlight-box" style="border-left-color: #ef4444;">
                    We apologize for any inconvenience this may cause. We will keep your application profile on file for future matching opportunities.
                </div>
                """, jobTitle, companyName);

        String htmlContent = buildHtmlWrapper(subject, greeting, body);
        sendEmail(candidate.getEmail(), subject, htmlContent);
    }

    @Override
    public void sendJobPostedNotificationToCandidates(String jobTitle, String companyName, String[] candidateEmails) {
        String subject = "New Job Opportunity - " + jobTitle;
        String greeting = "Dear Job Seeker,";
        String body = String.format("""
                <p>A exciting new job opportunity matching your interests has been posted!</p>
                <div class="highlight-box">
                    <strong>Job Details:</strong>
                    <ul style="margin: 8px 0 0 0; padding-left: 20px;">
                        <li><strong>Position:</strong> %s</li>
                        <li><strong>Company:</strong> %s</li>
                    </ul>
                </div>
                <p>Visit our platform today to view the full job description, eligibility criteria, and submit your application.</p>
                """, jobTitle, companyName);

        String htmlContent = buildHtmlWrapper(subject, greeting, body);
        sendBulkEmail(candidateEmails, subject, htmlContent);
    }

    @Override
    public void sendPasswordResetEmail(User user, String resetToken) {
        String subject = "Password Reset Request";
        String resetLink = "http://localhost:3000/reset-password?token=" + resetToken; // TODO: Make configurable
        String greeting = String.format("Dear %s,", user.getFirstName());
        String body = String.format("""
                <p>You have requested to reset your password for your <strong>%s</strong> account.</p>
                <p>Click the button below to set a new password:</p>
                <div style="text-align: center;">
                    <a href="%s" class="btn">Reset Password</a>
                </div>
                <p style="font-size: 13px; color: #64748b; margin-top: 15px;">
                    This link will expire in 24 hours. If you did not request this password reset, please ignore this email; your password will remain secure.
                </p>
                """, appName, resetLink);

        String htmlContent = buildHtmlWrapper(subject, greeting, body);
        sendEmail(user.getEmail(), subject, htmlContent);
    }

    @Override
    public void sendAccountVerificationEmail(User user, String verificationToken) {
        String subject = "Verify Your Account";
        String verificationLink = "http://localhost:3000/verify?token=" + verificationToken; // TODO: Make configurable
        String greeting = String.format("Dear %s,", user.getFirstName());
        String body = String.format("""
                <p>Welcome to <strong>%s</strong>! Please verify your email address to complete your registration and activate your account.</p>
                <div style="text-align: center;">
                    <a href="%s" class="btn">Verify Account</a>
                </div>
                <p style="font-size: 13px; color: #64748b; margin-top: 15px;">
                    If you did not register for an account on our platform, you can safely ignore this email.
                </p>
                """, appName, verificationLink);

        String htmlContent = buildHtmlWrapper(subject, greeting, body);
        sendEmail(user.getEmail(), subject, htmlContent);
    }

    @Override
    public void sendRecruiterJobPostedEmail(User recruiter, String jobTitle) {
        String subject = "Job Posted Successfully - " + jobTitle;
        String greeting = String.format("Dear %s,", recruiter.getFirstName());
        String body = String.format("""
                <p>Your job posting <strong>%s</strong> has been successfully published on <strong>%s</strong>.</p>
                <div class="highlight-box">
                    <strong>Recruiter Actions Available:</strong>
                    <ul style="margin: 8px 0 0 0; padding-left: 20px;">
                        <li>Review incoming applications on your recruiter dashboard</li>
                        <li>Directly schedule interview dates</li>
                        <li>Edit or archive job listing as needed</li>
                    </ul>
                </div>
                """, jobTitle, appName);

        String htmlContent = buildHtmlWrapper(subject, greeting, body);
        sendEmail(recruiter.getEmail(), subject, htmlContent);
    }

    @Override
    public void sendRecruiterApplicationReceivedEmail(User recruiter, String jobTitle, String candidateName,
            String candidateEmail) {
        String subject = "New Application Received - " + jobTitle;
        String greeting = String.format("Dear %s,", recruiter.getFirstName());
        String body = String.format("""
                <p>A new candidate application has been submitted for your job posting <strong>%s</strong>.</p>
                <div class="highlight-box">
                    <strong>Applicant Details:</strong>
                    <ul style="margin: 8px 0 0 0; padding-left: 20px;">
                        <li><strong>Name:</strong> %s</li>
                        <li><strong>Email:</strong> %s</li>
                    </ul>
                </div>
                <p>Please log in to your recruiter dashboard to view their resume, cover letter, and progress their application status.</p>
                """, jobTitle, candidateName, candidateEmail);

        String htmlContent = buildHtmlWrapper(subject, greeting, body);
        sendEmail(recruiter.getEmail(), subject, htmlContent);
    }

    @Override
    public void sendContactFormEmail(String fromEmail, String subject, String message) {
        String fullSubject = "Contact Form: " + subject;
        String greeting = "Hello Admin,";
        String body = String.format("""
                <p>You have received a new contact form submission.</p>
                <div class="highlight-box">
                    <strong>Submission Details:</strong>
                    <ul style="margin: 8px 0 0 0; padding-left: 20px;">
                        <li><strong>From:</strong> %s</li>
                        <li><strong>Subject:</strong> %s</li>
                    </ul>
                    <p style="margin-top: 12px; font-style: italic;"><strong>Message:</strong></p>
                    <p style="margin: 4px 0 0 0; background: #ffffff; padding: 10px; border-radius: 4px; border: 1px solid #e2e8f0;">
                        %s
                    </p>
                </div>
                """, fromEmail, subject, message);

        String htmlContent = buildHtmlWrapper(fullSubject, greeting, body);
        sendEmail(this.fromEmail, fullSubject, htmlContent); // Send to admin/support email
    }

    @Override
    @Async
    public void sendOtpEmail(String email, String otp) {
        // Runs off the request thread: a slow/unreachable SMTP server (timeouts, DNS failures)
        // must never add seconds of latency to the register/login/resend-otp HTTP response, since
        // the OTP row is already persisted before this is called and nothing awaits its result.
        String subject = "Your JobPortal Verification Code";
        String greeting = "Dear User,";
        String body = String.format("""
                <p>Please use the following verification code to complete your security check on <strong>%s</strong>:</p>
                <div class="otp-code">
                    %s
                </div>
                <p style="font-size: 13px; color: #64748b;">
                    This security code will expire in 5 minutes. For safety, do not share this code with anyone.
                </p>
                """, appName, otp);

        String htmlContent = buildHtmlWrapper(subject, greeting, body);
        // Never log the OTP value itself, only that a send was attempted.
        log.info("Sending OTP email to [{}]", email);
        sendEmail(email, subject, htmlContent);
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

    private void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            if (appName != null && !appName.isEmpty()) {
                helper.setFrom(fromEmail, appName);
            } else {
                helper.setFrom(fromEmail);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML content

            mailSender.send(message);
        } catch (Exception e) {
            // Log the error but don't throw exception to avoid breaking the flow
            log.warn("Failed to send email to [{}]: {}", to, e.getMessage());
        }
    }
}
