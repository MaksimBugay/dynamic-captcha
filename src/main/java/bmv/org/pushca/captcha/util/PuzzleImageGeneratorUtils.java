package bmv.org.pushca.captcha.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import bmv.org.pushca.captcha.service.CaptchaGenerator;
import bmv.org.pushca.captcha.service.PuzzleImageGeneratorTypes;

public class PuzzleImageGeneratorUtils {


  /**
   * Rotates a BufferedImage by the specified angle while maintaining the shape.
   * The rotated image dimensions are calculated to fit the entire rotated content.
   * 
   * @param inputImage The image to rotate
   * @param angle      The rotation angle in degrees, or null to use a random angle (15-195 degrees)
   * @return A new BufferedImage with the rotated content
   * @throws IllegalArgumentException if inputImage is null
   */
  public static BufferedImage rotateImage(BufferedImage inputImage, Double angle) {
    if (inputImage == null) {
      throw new IllegalArgumentException("Input image cannot be null");
    }

    // Use provided angle or generate random angle
    double rotationAngle = (angle != null) ? angle : generateRandomAngle();
    double rotationRadians = Math.toRadians(rotationAngle);

    // Calculate rotated dimensions to fit the entire rotated image
    int originalWidth = inputImage.getWidth();
    int originalHeight = inputImage.getHeight();

    double sin = Math.abs(Math.sin(rotationRadians));
    double cos = Math.abs(Math.cos(rotationRadians));

    int rotatedWidth = (int) Math.ceil(originalWidth * cos + originalHeight * sin);
    int rotatedHeight = (int) Math.ceil(originalHeight * cos + originalWidth * sin);

    // Create rotated image with transparent background
    BufferedImage rotatedImage = new BufferedImage(rotatedWidth, rotatedHeight, inputImage.getType());
    Graphics2D graphics = null;

    try {
      graphics = rotatedImage.createGraphics();

      // Set rendering hints for quality
      graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
          java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      graphics.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
          java.awt.RenderingHints.VALUE_RENDER_QUALITY);
      graphics.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
          java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

      // Fill with transparent background
      graphics.setColor(new Color(0, 0, 0, 0));
      graphics.fillRect(0, 0, rotatedWidth, rotatedHeight);

      // Translate to center, rotate, and translate back to draw the image
      graphics.translate(rotatedWidth / 2.0, rotatedHeight / 2.0);
      graphics.rotate(rotationRadians);
      graphics.translate(-originalWidth / 2.0, -originalHeight / 2.0);

      // Draw the original image
      graphics.drawImage(inputImage, 0, 0, null);

    } finally {
      if (graphics != null) {
        graphics.dispose();
      }
    }

    return rotatedImage;
  }

  public static double generateRandomAngle() {
    return 15 + CaptchaGenerator.RANDOM.nextDouble() * 180;
  }

  public static BufferedImage drawDashedRectangleOnGridImage(BufferedImage gridImage, PuzzleImageGeneratorTypes.Rectangle rectangle) {
    Graphics2D graphics = null;

    try {
      graphics = gridImage.createGraphics();
      // Set up dashed stroke
      float[] dashPattern = {10, 10}; // Dash pattern: 10 pixels on, 10 pixels off
      graphics.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, dashPattern, 0));
      graphics.setColor(Color.RED); // Color for the dashed rectangle

      // Draw the dashed rectangle
      PuzzleImageGeneratorTypes.Point topLeft = rectangle.leftTopCorner();
      graphics.drawRect(topLeft.x(), topLeft.y(), rectangle.width(), rectangle.height());
    } finally {
      if (graphics != null) {
        graphics.dispose();
      }
    }

    return gridImage;
  }


  /**
   * Scales a BufferedImage by the given scale coefficient
   *
   * @param inputImage       The image to scale
   * @param scaleCoefficient The scale factor (1.0 = original size, 2.0 = double size, 0.5 = half size)
   * @return A new scaled BufferedImage
   */
  public static BufferedImage scaleBufferedImage(BufferedImage inputImage, double scaleCoefficient) {
    int originalWidth = inputImage.getWidth();
    int originalHeight = inputImage.getHeight();

    int newWidth = (int) Math.round(originalWidth * scaleCoefficient);
    int newHeight = (int) Math.round(originalHeight * scaleCoefficient);

    BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, inputImage.getType());
    Graphics2D graphics = null;

    try {
      graphics = scaledImage.createGraphics();
      graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
          java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      graphics.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
          java.awt.RenderingHints.VALUE_RENDER_QUALITY);
      graphics.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
          java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

      graphics.drawImage(inputImage, 0, 0, newWidth, newHeight, null);
    } finally {
      if (graphics != null) {
        graphics.dispose();
      }
    }

    return scaledImage;
  }

}
