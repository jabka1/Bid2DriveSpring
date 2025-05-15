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

    public void sendCarApprovedEmail(String toEmail, String carMake, String carModel, String vin) {
        String subject = "Your Listing Has Been Approved!";
        String htmlContent = """
    <html>
    <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
        <div style="max-width: 600px; margin: auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
            <h1 style="color: #333; text-align: center;">Bid2Drive</h1>
            <h2 style="color: green;">Listing Approved</h2>
            <p>Congratulations! Your vehicle listing has been successfully approved and is now live on our platform.</p>
            <p><strong>Make:</strong> %s</p>
            <p><strong>Model:</strong> %s</p>
            <p><strong>VIN:</strong> %s</p>
            <p style="margin-top: 30px; font-size: 12px; color: #777;">Thank you for using Bid2Drive.</p>
        </div>
    </body>
    </html>
    """.formatted(carMake, carModel, vin);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            System.out.println("Car approval email sent to: " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Error sending email: " + e.getMessage());
        }
    }

    public void sendCarRejectedEmail(String toEmail, String carMake, String carModel, String vin, String verificationComment) {
        String subject = "Your Listing Has Been Rejected";
        String htmlContent = """
    <html>
    <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
        <div style="max-width: 600px; margin: auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
            <h1 style="color: #333; text-align: center;">Bid2Drive</h1>
            <h2 style="color: #e76f51;">Listing Rejected</h2>
            <p>Unfortunately, your vehicle listing was not approved.</p>
            <p><strong>Make:</strong> %s</p>
            <p><strong>Model:</strong> %s</p>
            <p><strong>VIN:</strong> %s</p>
            <p><strong>Reason:</strong> %s</p>
            <p style="margin-top: 30px; font-size: 12px; color: #777;">You may update your listing and resubmit it for verification.</p>
        </div>
    </body>
    </html>
    """.formatted(carMake, carModel, vin, verificationComment);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            System.out.println("Car rejection email sent to: " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Error sending email: " + e.getMessage());
        }
    }

    public void sendAccountDeactivatedEmail(String toEmail, String firstName, String lastName) {
        String subject = "Your Bid2Drive Account Has Been Deactivated";
        String htmlContent = """
        <html>
        <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
            <div style="max-width: 600px; margin: auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                <h1 style="color: #333; text-align: center;">Bid2Drive</h1>
                <h2 style="color: #e76f51; text-align: center;">Account Deactivated</h2>
                <p>Hello <strong>%s %s</strong>,</p>
                <p>This is to confirm that your Bid2Drive account associated with the email <strong>%s</strong> has been successfully deactivated.</p>
                <p>You can reactivate your account at any time by logging in again. After reactivation, you will be required to complete the verification process again in order to access all platform features.</p>
                <p>All your auctions and bids have been canceled and deleted.</p>
                <p style="margin-top: 30px; font-size: 12px; color: #777;">Thank you for using Bid2Drive.</p>
            </div>
        </body>
        </html>
        """.formatted(firstName, lastName, toEmail);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            System.out.println("Deactivation email sent to: " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Error sending deactivation email: " + e.getMessage());
        }
    }

    public void sendAccountReactivatedEmail(String toEmail, String firstName, String lastName) {
        String subject = "Welcome Back to Bid2Drive!";
        String htmlContent = """
    <html>
    <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
        <div style="max-width: 600px; margin: auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
            <h1 style="color: #333; text-align: center;">Bid2Drive</h1>
            <h2 style="color: #28a745; text-align: center;">Account Reactivated</h2>
            <p>Hello <strong>%s %s</strong>,</p>
            <p>We're glad to see you back! Your account associated with the email <strong>%s</strong> has been successfully reactivated.</p>
            <p>Please note: to regain full access to the platform, you will need to go through the ID verification process again.</p>
            <p style="margin-top: 30px; font-size: 12px; color: #777;">Thank you for returning to Bid2Drive!</p>
        </div>
    </body>
    </html>
    """.formatted(firstName, lastName, toEmail);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            System.out.println("Reactivation email sent to: " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Error sending reactivation email: " + e.getMessage());
        }
    }

    public void sendAdminDecisionEmail(String toEmail, String subject, String carModel, String carMake, String vin, String adminResponse) {
        String htmlContent = """
        <html>
        <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
            <div style="max-width: 600px; margin: auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                <h1 style="color: #333; text-align: center;">Bid2Drive</h1>
                <h2 style="color: #007bff; text-align: center;">%s</h2>
                <p>This message concerns one of your vehicle listings:</p>
                <p><strong>Make:</strong> %s</p>
                <p><strong>Model:</strong> %s</p>
                <p><strong>VIN:</strong> %s</p>
                <hr>
                <p><strong>Administrator's Message:</strong></p>
                <p>%s</p>
                <p style="margin-top: 30px; font-size: 12px; color: #777;">Please check your account for further updates.</p>
            </div>
        </body>
        </html>
    """.formatted(subject, carMake, carModel, vin, adminResponse);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            System.out.println("Admin decision email sent to: " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Error sending admin decision email: " + e.getMessage());
        }
    }

    public void sendAdminDecisionEmailForUser(String toEmail, String subject, String firstName, String lastName, String adminResponse) {
        String htmlContent = """
        <html>
        <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
            <div style="max-width: 600px; margin: auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                <h1 style="color: #333; text-align: center;">Bid2Drive</h1>
                <h2 style="color: #007bff; text-align: center;">%s</h2>
                <p>Hello <strong>%s %s</strong>,</p>
                <p>We would like to inform you regarding the recent report involving your account on Bid2Drive.</p>
                <hr>
                <p><strong>Administrator's Message:</strong></p>
                <p>%s</p>
                <p>If you have any questions, you may contact our support team.</p>
                <p style="margin-top: 30px; font-size: 12px; color: #777;">This message was sent in response to a formal complaint.</p>
            </div>
        </body>
        </html>
    """.formatted(subject, firstName, lastName, adminResponse);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            System.out.println("Admin decision email to user sent to: " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Error sending user decision email: " + e.getMessage());
        }
    }

}
