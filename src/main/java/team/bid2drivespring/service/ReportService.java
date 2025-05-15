package team.bid2drivespring.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import team.bid2drivespring.model.*;
import team.bid2drivespring.repository.AuctionRepository;
import team.bid2drivespring.repository.ReportRepository;
import team.bid2drivespring.repository.UserRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final AmazonS3 s3Client;
    private final ReportRepository reportRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Transactional
    public void submitAuctionReport(Long auctionId, String description, MultipartFile image, User reporter) throws IOException {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found"));

        Report report = new Report();
        report.setReporter(reporter);
        report.setReportedAuction(auction);
        report.setType(Report.ReportType.AUCTION);
        report.setStatus(Report.ReportStatus.PENDING);
        report.setDescription(description);
        report.setCreatedAt(Instant.now());
        String imageUrl = uploadToS3(image);
        report.setImageUrl(imageUrl);

        reportRepository.save(report);
    }

    @Transactional
    public void submitUserReport(Long userId, String description, MultipartFile image, User reporter) throws IOException {
        User reportedUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Report report = new Report();
        report.setReporter(reporter);
        report.setReportedUser(reportedUser);
        report.setType(Report.ReportType.USER);
        report.setStatus(Report.ReportStatus.PENDING);
        report.setDescription(description);
        report.setCreatedAt(Instant.now());
        String imageUrl = uploadToS3(image);
        report.setImageUrl(imageUrl);

        reportRepository.save(report);
    }

    private String uploadToS3(MultipartFile file) throws IOException {
        String key = "reports/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        s3Client.putObject(bucketName, key, file.getInputStream(), metadata);
        return s3Client.getUrl(bucketName, key).toString();
    }
}
