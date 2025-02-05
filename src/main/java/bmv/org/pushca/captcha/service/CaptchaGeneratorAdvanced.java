package bmv.org.pushca.captcha.service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.SplittableRandom;
import org.springframework.stereotype.Service;

@Service
public class CaptchaGeneratorAdvanced implements CaptchaGenerator {

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

    // Configure Text Properties
    g.setFont(new Font("Arial", Font.PLAIN, randomInt(30, 40)));
    //g.setColor(getRandomColor(50, 100, 0.5f)); // Reduced contrast and alpha
    g.setColor(getRandomColor(50, 100, 0.7f));

    // Randomize Text Position and Rotation
    int textWidth = g.getFontMetrics().stringWidth(text);
    int textHeight = g.getFontMetrics().getHeight();
    int x = randomInt(50, width - textWidth - 50);
    int y = randomInt(textHeight, height - 20);
    double angle = Math.toRadians(randomInt(-10, 10));

    g.rotate(angle, x, y);
    g.drawString(text, x, y);
    g.rotate(-angle, x, y);

    // Add More Random Lines
    addRandomLines(g, width, height);

    // Apply Blur Effect
    BufferedImage blurredImage = applyBlurEffect(captchaImage, 3); // Increased blur radius

    g.dispose();
    return blurredImage;
  }

  private void addDottedBackground(Graphics2D g, int width, int height) {
    for (int i = 0; i < 1000; i++) { // Increased number of dots
      g.setColor(getRandomColor(0, 255, 0.4f)); // Slightly opaque dots
      int x = randomInt(0, width);
      int y = randomInt(0, height);
      int size = randomInt(3, 7); // Larger dots
      g.fillOval(x, y, size, size);
    }
  }

  private void addRandomLines(Graphics2D g, int width, int height) {
    for (int i = 0; i < 20; i++) { // Increased lines for more noise
      g.setColor(getRandomColor(0, 255, 0.5f));
      g.setStroke(new BasicStroke(randomInt(1, 3)));
      int x1 = randomInt(0, width);
      int y1 = randomInt(0, height);
      int x2 = randomInt(0, width);
      int y2 = randomInt(0, height);
      g.drawLine(x1, y1, x2, y2);
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