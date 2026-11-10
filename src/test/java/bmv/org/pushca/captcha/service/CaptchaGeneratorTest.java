package bmv.org.pushca.captcha.service;

import static bmv.org.pushca.captcha.service.CaptchaGenerator.bytesToBufferedImage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class CaptchaGeneratorTest {

  private static final int SIDE_LENGTH = 40;

  // Draws a solid square whose side length is taken from the text, so the output is predictable
  private final CaptchaGenerator generator = text -> {
    int side = Integer.parseInt(text);
    BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_INT_RGB);
    for (int x = 0; x < side; x++) {
      for (int y = 0; y < side; y++) {
        image.setRGB(x, y, Color.RED.getRGB());
      }
    }
    return image;
  };

  @Test
  void generateCaptchaEncodesImageInRequestedFormat() {
    byte[] png = generator.generateCaptcha(String.valueOf(SIDE_LENGTH), "png");

    assertNotNull(png);
    BufferedImage decoded = bytesToBufferedImage(png);
    assertNotNull(decoded);
    assertEquals(SIDE_LENGTH, decoded.getWidth());
    assertEquals(SIDE_LENGTH, decoded.getHeight());
    assertEquals(Color.RED.getRGB(), decoded.getRGB(SIDE_LENGTH / 2, SIDE_LENGTH / 2));
  }

  @Test
  void generateCaptchaSupportsJpeg() {
    byte[] jpg = generator.generateCaptcha(String.valueOf(SIDE_LENGTH), "jpg");

    // JPEG SOI marker
    assertEquals((byte) 0xFF, jpg[0]);
    assertEquals((byte) 0xD8, jpg[1]);
    BufferedImage decoded = bytesToBufferedImage(jpg);
    assertEquals(SIDE_LENGTH, decoded.getWidth());
    assertEquals(SIDE_LENGTH, decoded.getHeight());
  }
}
