package bmv.org.pushca.captcha.service;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CaptchaGeneratorSimple implements CaptchaGenerator {

  private final DefaultKaptcha kaptcha;

  public CaptchaGeneratorSimple(DefaultKaptcha kaptcha) {
    this.kaptcha = kaptcha;
  }

  @Override
  public BufferedImage generateCaptchaImage(String text) {
    return Optional.of(kaptcha.createImage(text))
        .map(this::reduceTextVisibility)
        .orElseThrow();
  }

  private BufferedImage reduceTextVisibility(BufferedImage image) {
    int width = image.getWidth();
    int height = image.getHeight();
    Graphics2D g = image.createGraphics();

    // Apply a translucent overlay to reduce visibility
    g.setColor(new Color(255, 255, 255, 60)); // Slight white overlay
    g.fillRect(0, 0, width, height);

    // Add light distortions over the text
    for (int i = 0; i < 100; i++) {
      g.setColor(new Color(
          (int) (Math.random() * 100),
          (int) (Math.random() * 100),
          (int) (Math.random() * 100),
          50 // Very light distortion
      ));
      int x = (int) (Math.random() * width);
      int y = (int) (Math.random() * height);
      int size = (int) (Math.random() * 20) + 5;
      g.fillOval(x, y, size, size);
    }

    g.dispose();
    return image;
  }
}
