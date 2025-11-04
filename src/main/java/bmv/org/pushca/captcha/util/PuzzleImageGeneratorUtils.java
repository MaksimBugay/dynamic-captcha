package bmv.org.pushca.captcha.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import bmv.org.pushca.captcha.service.CaptchaGenerator;
import bmv.org.pushca.captcha.service.PuzzleImageGeneratorTypes;

/**
 * Utility class for generating and manipulating puzzle images.
 * This class provides methods for rotating, scaling, and drawing on images.
 */
public class PuzzleImageGeneratorUtils {


  /**
   * Generates a random boolean value.
   *
   * @return A random boolean (true or false with equal probability)
   */
  public static boolean generateRandomBoolean() {
    return CaptchaGenerator.RANDOM.nextBoolean();
  }

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
   * Scales a BufferedImage by the given scale coefficient.
   *
   * <p>The scaling operation:
   * <ul>
   *   <li>Maintains the image type (e.g., ARGB, RGB)</li>
   *   <li>Uses high-quality interpolation (bilinear) for smooth scaling</li>
   *   <li>Applies rendering hints for anti-aliasing and quality</li>
   * </ul>
   *
   * @param inputImage       The image to scale (must not be null)
   * @param scaleCoefficient The scale factor where:
   *                         - 1.0 = original size
   *                         - 2.0 = double size
   *                         - 0.5 = half size
   * @return A new scaled BufferedImage with dimensions adjusted by the scale coefficient
   * @throws IllegalArgumentException if inputImage is null or has non-positive dimensions
   */
  public static BufferedImage scaleImage(BufferedImage inputImage, double scaleCoefficient) {
    validateImageInput(inputImage);

    return scaleBufferedImage(inputImage, scaleCoefficient);
  }

  /**
   * Generates a random scale coefficient for image scaling.
   * Used to create visual variation in puzzle piece sizes during obfuscation.
   *
   * <p>The coefficient is generated within a specified range:
   * <ul>
   *   <li>Minimum value: {@code min}</li>
   *   <li>Maximum value: {@code min + delta}</li>
   *   <li>Distribution: Uniform random within the range</li>
   * </ul>
   *
   * <p>Typical usage:
   * <ul>
   *   <li>Top grid: scale between 0.6 and 0.8 (60-80% of original)</li>
   *   <li>Bottom grid: scale between 0.8 and 1.0 (80-100% of original)</li>
   * </ul>
   *
   * @param min   The minimum scale coefficient (e.g., 0.6 for 60% of original size)
   * @param delta The range width added to min to get maximum (e.g., 0.2 gives max = 0.8)
   * @return A random scale coefficient between min and (min + delta)
   */
  public static double generateRandomScaleCoefficient(double min, double delta) {
    return min + CaptchaGenerator.RANDOM.nextDouble() * delta;
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
   * Scales a BufferedImage to new dimensions using high-quality interpolation.
   *
   * <p>Implementation details:
   * <ul>
   *   <li>New dimensions are calculated: newWidth = originalWidth * scaleCoefficient, newHeight = originalHeight * scaleCoefficient</li>
   *   <li>Uses bilinear interpolation for smooth scaling</li>
   *   <li>Applies high-quality rendering hints (anti-aliasing, quality rendering)</li>
   *   <li>Preserves the original image type (ARGB, RGB, etc.)</li>
   * </ul>
   *
   * <p>This is an internal helper method primarily used by {@link #scaleImage(BufferedImage, double)}.
   *
   * @param inputImage       The image to scale (must not be null)
   * @param scaleCoefficient The scale factor where:
   *                         - 1.0 = original size
   *                         - 2.0 = double size
   *                         - 0.5 = half size
   * @return A new BufferedImage scaled to (originalWidth * scaleCoefficient) by (originalHeight * scaleCoefficient)
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
