package bmv.org.pushca.captcha.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import javax.imageio.ImageIO;

public interface CaptchaGenerator {

  /**
   * Returns the random generator of the calling thread. Captchas are generated concurrently,
   * so never store the result in a field shared between threads.
   */
  static RandomGenerator random() {
    return ThreadLocalRandom.current();
  }

  BufferedImage generateCaptchaImage(String text);


  default byte[] generateCaptcha(String text, String formatName) {
    return bufferedImageToBytes(generateCaptchaImage(text), formatName);
  }

  static byte[] bufferedImageToBytes(BufferedImage image, String formatName) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      ImageIO.write(image, formatName, baos);
      return baos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("Cannot convert BufferedImage to byte array", e);
    }
  }

  static BufferedImage bytesToBufferedImage(byte[] imageBytes) {
    try (ByteArrayInputStream bytes = new ByteArrayInputStream(imageBytes)) {
      return ImageIO.read(bytes);
    } catch (IOException e) {
      throw new RuntimeException("Cannot convert byte array to BufferedImage", e);
    }
  }
}
