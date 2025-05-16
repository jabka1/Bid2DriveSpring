package team.bid2drivespring.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UrlSafeEncryptionUtil {

    private final EncryptionUtil encryptionUtil;

    public String encodeId(long id) {
        String encrypted = encryptionUtil.encrypt(String.valueOf(id));
        return encrypted.replace("+", "-")
                .replace("/", "_")
                .replace("=", "");
    }

    public long decodeToken(String token) {
        String padded = token.replace("-", "+")
                .replace("_", "/");
        while (padded.length() % 4 != 0) {
            padded += "=";
        }
        String decrypted = encryptionUtil.decrypt(padded);
        return Long.parseLong(decrypted);
    }
}
