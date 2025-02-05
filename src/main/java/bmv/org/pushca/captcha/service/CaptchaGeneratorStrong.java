package bmv.org.pushca.captcha.service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.util.SplittableRandom;
import org.springframework.stereotype.Service;

@Service
public class CaptchaGeneratorStrong implements CaptchaGenerator {

  private static final SplittableRandom RANDOM = new SplittableRandom();

  @Override
  public BufferedImage generateCaptchaImage(String text) {
    int width = 300;
    int height = 100;

    BufferedImage captchaImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = captchaImage.createGraphics();

    // Gradient Background
    GradientPaint gradient = new GradientPaint(
        0, 0, getRandomColor(180, 220),
        width, height, getRandomColor(200, 255)
    );
    g.setPaint(gradient);
    g.fillRect(0, 0, width, height);

    // Add Enhanced Dotted Background
    addDottedBackground(g, width, height);

    // Add Irregular Shapes
    addIrregularShapes(g, width, height);

    // Configure Text Properties
    g.setFont(new Font("Arial", Font.PLAIN, randomInt(30, 40)));
    g.setColor(getRandomColor(50, 100, 0.7f)); // Reduced contrast text

    // Randomize Text Position and Rotation
    drawDistortedText(g, text, width, height);

    // Add Distorted Lines
    addDistortedLines(g, width, height);

    // Apply Noise Overlay
    applyNoise(g, width, height);

    // Apply Blur Effect
    BufferedImage blurredImage = applyBlurEffect(captchaImage, 3);

    g.dispose();
    return blurredImage;
  }

  private void addDottedBackground(Graphics2D g, int width, int height) {
    for (int i = 0; i < 1200; i++) {
      g.setColor(getRandomColor(0, 255, 0.4f));
      int x = randomInt(0, width);
      int y = randomInt(0, height);
      int size = randomInt(2, 6);
      g.fillOval(x, y, size, size);
    }
  }

  private void addIrregularShapes(Graphics2D g, int width, int height) {
    for (int i = 0; i < 10; i++) {
      g.setColor(getRandomColor(0, 255, 0.3f));
      int x = randomInt(0, width);
      int y = randomInt(0, height);
      int w = randomInt(20, 50);
      int h = randomInt(20, 50);
      g.fillRoundRect(x, y, w, h, randomInt(5, 20), randomInt(5, 20));
    }
  }

  private void drawDistortedText(Graphics2D g, String text, int width, int height) {
    int x = randomInt(50, Math.max(50, width - 150)); // Ensure starting point leaves enough space
    int y = randomInt(50, height - 20);

    for (char c : text.toCharArray()) {
      g.rotate(Math.toRadians(randomInt(-15, 15)), x, y);
      g.drawString(String.valueOf(c), x, y);
      g.rotate(-Math.toRadians(randomInt(-15, 15)), x, y);

      // Add character width and spacing
      x += g.getFontMetrics().charWidth(c) + randomInt(2, 5);

      // Stop drawing if text exceeds canvas width
      if (x > width - 50) {
        break; // Stop drawing further characters
      }
    }
  }

  private void addDistortedLines(Graphics2D g, int width, int height) {
    for (int i = 0; i < 15; i++) {
      g.setColor(getRandomColor(0, 255, 0.5f));
      g.setStroke(new BasicStroke(randomInt(1, 3)));
      int x1 = randomInt(0, width);
      int y1 = randomInt(0, height);
      int x2 = randomInt(0, width);
      int y2 = randomInt(0, height);

      // Add distortion
      int ctrlX = randomInt(0, width);
      int ctrlY = randomInt(0, height);
      g.draw(new QuadCurve2D.Float(x1, y1, ctrlX, ctrlY, x2, y2));
    }
  }

  private void applyNoise(Graphics2D g, int width, int height) {
    for (int i = 0; i < width * height / 10; i++) {
      g.setColor(getRandomColor(0, 255, 0.1f));
      int x = randomInt(0, width);
      int y = randomInt(0, height);
      g.fillRect(x, y, 1, 1);
    }
  }

  private BufferedImage applyBlurEffect(BufferedImage image, int radius) {
    int width = image.getWidth();
    int height = image.getHeight();
    BufferedImage blurredImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int r = 0, g = 0, b = 0, a = 0, count = 0;

        for (int dy = -radius; dy <= radius; dy++) {
          for (int dx = -radius; dx <= radius; dx++) {
            int nx = x + dx;
            int ny = y + dy;

            if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
              Color neighborColor = new Color(image.getRGB(nx, ny), true);
              r += neighborColor.getRed();
              g += neighborColor.getGreen();
              b += neighborColor.getBlue();
              a += neighborColor.getAlpha();
              count++;
            }
          }
        }

        r /= count;
        g /= count;
        b /= count;
        a /= count;

        blurredImage.setRGB(x, y, new Color(r, g, b, a).getRGB());
      }
    }

    return blurredImage;
  }

  private Color getRandomColor(int min, int max) {
    return new Color(randomInt(min, max), randomInt(min, max), randomInt(min, max));
  }

  private Color getRandomColor(int min, int max, float alpha) {
    return new Color(randomInt(min, max) / 255f, randomInt(min, max) / 255f,
        randomInt(min, max) / 255f, alpha);
  }

  private int randomInt(int min, int max) {
    return RANDOM.nextInt((max - min) + 1) + min;
  }
}
