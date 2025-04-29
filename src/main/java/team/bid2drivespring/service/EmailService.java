package team.bid2drivespring.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import team.bid2drivespring.model.User;
import team.bid2drivespring.repository.UserRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository userRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendActivationEmail(String toEmail, String activationToken, HttpServletRequest request) {
        String subject = "Activate your Bid2Drive account";

        String baseUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "");
        String activationLink = baseUrl + "/activate?token=" + activationToken;

        String htmlContent = """
            <html>
            <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                <div style="max-width: 600px; margin: auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                    <h1 style="color: #333; text-align: center;">Bid2Drive</h1>
                    <h2 style="color: #333;">Welcome to Bid2Drive!</h2>
                    <p>Click the button below to activate your account:</p>
                    <a href="%s" style="display: inline-block; margin-top: 20px; padding: 12px 20px; background-color: #007bff; color: white; text-decoration: none; border-radius: 5px;">Activate Account</a>
                    <p style="margin-top: 30px; font-size: 12px; color: #777;">If you did not request this, please ignore this email.</p>
                </div>
            </body>
            </html>
            """.formatted(activationLink);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = is HTML

            mailSender.send(mimeMessage);
            System.out.println("HTML activation email sent to: " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Error sending email: " + e.getMessage());
        }
    }

    public void sendTwoFactorCode(String toEmail, String twoFactorCode) {
        String subject = "Your 2FA Code";
        String htmlContent = """
            <html>
            <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                <div style="max-width: 600px; margin: auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                    <h1 style="color: #333; text-align: center;">Bid2Drive</h1>
                    <h2 style="color: #333;">Your 2FA Code</h2>
                    <p style="font-size: 18px;">Your 2FA code is: <strong>%s</strong></p>
                    <p style="margin-top: 30px; font-size: 12px; color: #777;">If you did not request this, please ignore this email.</p>
                </div>
            </body>
            </html>
            """.formatted(twoFactorCode);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            System.out.println("HTML 2FA email sent to: " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Error sending email: " + e.getMessage());
        }
    }

    public void sendPasswordRecoveryEmail(String username, String token) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String htmlContent = """
            <html>
            <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                <div style="max-width: 600px; margin: auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                    <h1 style="color: #333; text-align: center;">Bid2Drive</h1>
                    <h2 style="color: #333;">Password Recovery</h2>
                    <p>Use this token to reset your password:</p>
                    <p style="font-size: 18px; font-weight: bold;">%s</p>
                    <p style="margin-top: 30px; font-size: 12px; color: #777;">If you did not request this, please ignore this email.</p>
                </div>
            </body>
            </html>
            """.formatted(token);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("Password Recovery");
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            System.out.println("HTML password recovery email sent to: " + user.getEmail());
        } catch (MessagingException e) {
            System.err.println("Error sending email: " + e.getMessage());
        }
    }

    public void sendVerificationApprovedEmail(String toEmail) {
        String subject = "Passport Verification Approved";
        String htmlContent = """
        <html>
        <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
            <div style="max-width: 600px; margin: auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                <h1 style="color: #333; text-align: center;">Bid2Drive</h1>
                <h2 style="color: green;">Verification Approved</h2>
                <p>We’re happy to let you know that your passport verification has been successfully approved. You now have full access to all platform features without restrictions.</p>
                <p style="margin-top: 30px; font-size: 12px; color: #777;">You can try uploading a new photo from your account settings.</p>
            </div>
        </body>
        </html>
        """;

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            System.out.println("Verification denied email sent to: " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Error sending email: " + e.getMessage());
        }
    }

    public void sendVerificationRejectedEmail(String toEmail) {
        String subject = "Passport Verification Rejected";
        String htmlContent = """
        <html>
        <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
            <div style="max-width: 600px; margin: auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                <h1 style="color: #333; text-align: center;">Bid2Drive</h1>
                <h2 style="color: #e76f51;">Verification Rejected</h2>
                <p>Your passport verification was rejected. It may be due to incorrect or mismatched data.</p>
                <p>Please make sure to upload a valid photo and ensure that all required fields are correctly filled.</p>
                <p style="margin-top: 30px; font-size: 12px; color: #777;">You can re-submit the verification from your account page.</p>
            </div>
        </body>
        </html>
        """;

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            System.out.println("Verification rejected email sent to: " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Error sending email: " + e.getMessage());
        }
    }

}
