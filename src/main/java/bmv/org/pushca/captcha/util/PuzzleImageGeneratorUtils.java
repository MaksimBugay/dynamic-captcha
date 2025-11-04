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
      graphics = getGraphics2D(rotatedImage);

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

  /**
   * Generates a random rotation angle between 15 and 195 degrees.
   * Used for random puzzle piece rotations to increase visual variation.
   *
   * @return A random angle in degrees between 15 and 195
   */
  public static double generateRandomAngle() {
    return 15 + CaptchaGenerator.RANDOM.nextDouble() * 180;
  }

  /**
   * Scales down a BufferedImage by the given scale coefficient.
   * If no coefficient is provided, a random scale factor between 0.6 and 0.9 is used.
   *
   * <p>Algorithm:
   * <ul>
   *   <li>If scaleCoefficient is null: generates random value in range [0.6, 0.9]</li>
   *   <li>Delegates to {@link #scaleBufferedImage(BufferedImage, double)} for actual scaling</li>
   *   <li>Maintains image quality with high-quality rendering hints</li>
   * </ul>
   *
   * @param inputImage       The image to scale down
   * @param scaleCoefficient The scale factor (1.0 = original, 0.5 = half size), or null for random (0.6-0.9)
   * @return A new scaled-down BufferedImage
   * @throws IllegalArgumentException if inputImage is null or has invalid dimensions
   */
  public static BufferedImage scaleImage(BufferedImage inputImage, Double scaleCoefficient) {
    validateImageInput(inputImage);

    // Use provided coefficient or generate random value between 0.6 and 0.9
    double coefficient = (scaleCoefficient != null) ? scaleCoefficient : generateRandomScaleCoefficient();

    return scaleBufferedImage(inputImage, coefficient);
  }

  /**
   * Generates a random scale coefficient between 0.6 and 0.9 for downscaling.
   * Ensures the image is always reduced by 10-40% of its original size.
   *
   * @return A random scale factor in range [0.6, 0.9]
   */
  public static double generateRandomScaleCoefficient() {
    // 0.3 = 0.9 - 0.6, gives range [0.6, 0.9]
    return 0.6 + CaptchaGenerator.RANDOM.nextDouble() * 0.3;
  }

  /**
   * Draws a dashed red rectangle on a grid image to highlight a specific area.
   * Used for visual debugging and highlighting the matched puzzle piece area.
   *
   * <p>Details:
   * <ul>
   *   <li>Dash pattern: 10 pixels on, 10 pixels off</li>
   *   <li>Stroke width: 2 pixels</li>
   *   <li>Color: Red (for visibility)</li>
   * </ul>
   *
   * @param gridImage The grid image to draw on
   * @param rectangle The rectangle to draw (position and dimensions)
   * @return The modified grid image with the dashed rectangle drawn
   * @throws IllegalArgumentException if gridImage is null or rectangle is null
   */
  public static BufferedImage drawDashedRectangleOnGridImage(BufferedImage gridImage, PuzzleImageGeneratorTypes.Rectangle rectangle) {
    if (gridImage == null) {
      throw new IllegalArgumentException("Grid image cannot be null");
    }
    if (rectangle == null) {
      throw new IllegalArgumentException("Rectangle cannot be null");
    }
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
      graphics = getGraphics2D(scaledImage);

      graphics.drawImage(inputImage, 0, 0, newWidth, newHeight, null);
    } finally {
      if (graphics != null) {
        graphics.dispose();
      }
    }

    return scaledImage;
  }

  private static Graphics2D getGraphics2D(BufferedImage scaledImage) {
    Graphics2D graphics;
    graphics = scaledImage.createGraphics();
    graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
        java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    graphics.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
        java.awt.RenderingHints.VALUE_RENDER_QUALITY);
    graphics.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
    return graphics;
  }

  private static void validateImageInput(BufferedImage inputImage) {
    if (inputImage == null) {
      throw new IllegalArgumentException("Input image cannot be null");
    }
    if (inputImage.getWidth() <= 0 || inputImage.getHeight() <= 0) {
      throw new IllegalArgumentException("Input image must have positive dimensions");
    }
  }
}
