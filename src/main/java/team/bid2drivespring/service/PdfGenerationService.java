package team.bid2drivespring.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import team.bid2drivespring.model.Auction;
import team.bid2drivespring.model.Bid;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class PdfGenerationService {

    public byte[] generateAuctionDeliveryPdf(Auction auction, String verifyLink) throws IOException, WriterException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType0Font font = PDType0Font.load(document,
                    new ClassPathResource("fonts/DejaVuSans.ttf").getInputStream());

            PDImageXObject logoImage = PDImageXObject.createFromFile("src/main/resources/static/logo.png", document);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float pageWidth = page.getMediaBox().getWidth();

                cs.drawImage(logoImage, (pageWidth - 200) / 2, 760, 200, 50);

                cs.beginText();
                cs.setFont(font, 20);
                cs.newLineAtOffset((pageWidth - font.getStringWidth("Auction Delivery Sheet") / 1000 * 20) / 2, 735);
                cs.showText("Auction Delivery Sheet");
                cs.endText();

                float leftX = 50;
                float rightX = 300;
                float y = 710;
                float leading = 14;

                cs.beginText();
                cs.setFont(font, 14);
                cs.newLineAtOffset((pageWidth - font.getStringWidth("Main Information:") / 1000 * 14) / 2, y);
                cs.showText("Main Information:");
                cs.endText();

                y -= 20;

                cs.beginText();
                cs.setFont(font, 11);
                cs.setLeading(leading);
                cs.newLineAtOffset(leftX, y);
                cs.showText("Brand: " + auction.getCarMake());
                cs.newLine();
                cs.showText("Model: " + auction.getCarModel());
                cs.newLine();
                cs.showText("Generation: " + auction.getCarGeneration());
                cs.newLine();
                cs.showText("Year: " + auction.getYear());
                cs.newLine();
                cs.showText("VIN: " + auction.getVin());
                cs.endText();

                cs.beginText();
                cs.setFont(font, 11);
                cs.setLeading(leading);
                cs.newLineAtOffset(rightX, y);
                cs.showText("Color: " + auction.getCarColor());
                cs.newLine();
                cs.showText("Mileage: " + auction.getMileage());
                cs.newLine();
                cs.showText("Engine: " + auction.getEngineMarking());
                cs.newLine();
                cs.showText("HP: " + auction.getHorsepower());
                cs.newLine();
                cs.showText("Fuel: " + auction.getFuelType());
                cs.endText();

                y -= 7 * leading;

                cs.beginText();
                cs.setFont(font, 11);
                cs.setLeading(leading);
                cs.newLineAtOffset(leftX, y);
                cs.showText("Transmission: " + auction.getTransmission());
                cs.newLine();
                cs.showText("Body Type: " + auction.getBodyType());
                cs.newLine();
                cs.showText("Drive Type: " + auction.getDriveType());
                cs.newLine();
                cs.showText("Engine Size: " + (auction.getEngineSize() != null ? auction.getEngineSize() : "-"));
                cs.newLine();
                cs.showText("Euro Standard: " + (auction.getEuroStandard() != null ? auction.getEuroStandard() : "-"));
                cs.newLine();
                cs.showText("EPA Standard: " + (auction.getEpaStandard() != null ? auction.getEpaStandard() : "-"));
                cs.endText();

                cs.beginText();
                cs.setFont(font, 11);
                cs.setLeading(leading);
                cs.newLineAtOffset(rightX, y);
                cs.showText("Location:");
                cs.newLine();
                cs.showText("Country: " + auction.getCountry());
                cs.newLine();
                cs.showText("Region: " + auction.getRegion());
                cs.newLine();
                cs.showText("City: " + auction.getCityOrVillage());
                cs.endText();

                y -= 9 * leading;

                cs.beginText();
                cs.setFont(font, 14);
                cs.newLineAtOffset(leftX, y);
                cs.showText("Seller:");
                cs.endText();

                cs.beginText();
                cs.setFont(font, 11);
                cs.setLeading(leading);
                cs.newLineAtOffset(leftX, y - 14);
                cs.showText("Name: " + auction.getSeller().getFirstName());
                cs.newLine();
                cs.showText("Surname: " + auction.getSeller().getLastName());
                cs.newLine();
                cs.showText("Email: " + auction.getSeller().getEmail());
                cs.newLine();
                cs.showText("Location: " + auction.getSeller().getCountry() + ", " + auction.getSeller().getCity());
                cs.endText();

                if (auction.getNewOwner() != null) {
                    cs.beginText();
                    cs.setFont(font, 14);
                    cs.newLineAtOffset(rightX, y);
                    cs.showText("Buyer:");
                    cs.endText();

                    cs.beginText();
                    cs.setFont(font, 11);
                    cs.setLeading(leading);
                    cs.newLineAtOffset(rightX, y - 14);
                    cs.showText("Name: " + auction.getNewOwner().getFirstName());
                    cs.newLine();
                    cs.showText("Surname: " + auction.getNewOwner().getLastName());
                    cs.newLine();
                    cs.showText("Email: " + auction.getNewOwner().getEmail());
                    cs.newLine();
                    cs.showText("Location: " + auction.getNewOwner().getCountry() + ", " + auction.getNewOwner().getCity());
                    cs.endText();
                }

                Bid lastUserBid = auction.getNewOwner() != null ? auction.getLastUserBid(auction.getNewOwner().getId()) : null;
                if (lastUserBid != null) {
                    cs.beginText();
                    cs.setFont(font, 16);
                    cs.newLineAtOffset((pageWidth - font.getStringWidth("Final Price: " + lastUserBid.getProposedPrice() + " USD") / 1000 * 16) / 2, 240);
                    cs.showText("Final Price: " + lastUserBid.getProposedPrice() + " USD");
                    cs.endText();
                }
            }

            BufferedImage qrImage = generateQRCode(verifyLink, 150, 150);
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, bufferedImageToBytes(qrImage), "verifyQR");

            try (PDPageContentStream cs = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true)) {
                cs.drawImage(pdImage, (page.getMediaBox().getWidth() - 130) / 2, 90, 130, 130);

                cs.beginText();
                cs.setFont(font, 10);
                cs.newLineAtOffset((page.getMediaBox().getWidth() - font.getStringWidth("Verify: " + verifyLink) / 1000 * 10) / 2, 60);
                cs.showText("Verify: " + verifyLink);
                cs.endText();
            }

            document.save(baos);
        }

        return baos.toByteArray();
    }

    private BufferedImage generateQRCode(String text, int width, int height) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    private byte[] bufferedImageToBytes(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        }
    }
}
