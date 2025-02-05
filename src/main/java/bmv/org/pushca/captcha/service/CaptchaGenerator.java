package bmv.org.pushca.captcha.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.SplittableRandom;
import javax.imageio.ImageIO;

public interface CaptchaGenerator {

  SplittableRandom RANDOM = new SplittableRandom();

  BufferedImage generateCaptchaImage(String text);


  default byte[] generateCaptcha(String text, String formatName) {
    return bufferedImageToBytes(generateCaptchaImage(text), formatName);
  }

  default byte[] generateCaptcha(int x1, int x2, String formatName) {
    return generateCaptcha(generateRandomText(x1, x2), formatName);
  }

  static String generateRandomText(Integer x1, Integer x2) {
    int num1 = Optional.ofNullable(x1).orElse(RANDOM.nextInt(9) + 1);
    int num2 = Optional.ofNullable(x2).orElse(RANDOM.nextInt(9) + 1);
    return num1 + " x " + num2 + " =";
  }

  static byte[] bufferedImageToBytes(BufferedImage image, String formatName) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      ImageIO.write(image, formatName, baos);
      return baos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("Cannot convert BufferedImage to byte array", e);
    }
  }
}
