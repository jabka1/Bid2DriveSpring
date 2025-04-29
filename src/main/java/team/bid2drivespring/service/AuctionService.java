package team.bid2drivespring.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import team.bid2drivespring.model.Auction;
import team.bid2drivespring.repository.AuctionRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AmazonS3 s3Client;
    private final AuctionRepository auctionRepository;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private static final String AUCTION_PHOTO_FOLDER = "auction_photo/";

    private List<String> uploadFilesToS3(MultipartFile[] files) throws IOException {
        List<String> uploadedUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String key = AUCTION_PHOTO_FOLDER + UUID.randomUUID() + "-" + file.getOriginalFilename();

                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(file.getSize());
                metadata.setContentType(file.getContentType());

                s3Client.putObject(bucketName, key, file.getInputStream(), metadata);

                String fileUrl = s3Client.getUrl(bucketName, key).toString();
                uploadedUrls.add(fileUrl);
            }
        }

        return uploadedUrls;
    }

    @Transactional
    public void uploadImgsAndSaveAuction(Auction auction, MultipartFile[] carImages) throws IOException {
        List<String> uploadedUrls = uploadFilesToS3(carImages);
        auction.setCarImagesUrls(uploadedUrls);
        saveAuction(auction);
    }

    @Transactional
    public void saveAuction(Auction auction) {
        auctionRepository.save(auction);
    }
}
