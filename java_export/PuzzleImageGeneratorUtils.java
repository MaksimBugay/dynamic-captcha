package bmv.org.pushca.captcha.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import bmv.org.pushca.captcha.service.CaptchaGenerator;
import bmv.org.pushca.captcha.service.PuzzleImageGeneratorTypes;
import bmv.org.pushca.captcha.service.PuzzleImageGeneratorTypes.Rectangle;

/**
 * Utility class for generating and manipulating puzzle images.
 *
 * <p>This class provides methods for:
 * <ul>
 *   <li>Rotating and scaling images with high-quality interpolation</li>
 *   <li>Concatenating multiple images vertically</li>
 *   <li>Adding noise patterns with curved lines</li>
 *   <li>Drawing highlights on grid images</li>
 *   <li>Random angle and scale coefficient generation</li>
 * </ul>
 */
@SuppressWarnings("unused")
public class PuzzleImageGeneratorUtils {

  // Noise generation constants
  private static final int NOISE_MIN_SPACING = 30; // Minimum pixel spacing between noise lines
  private static final int NOISE_MAX_SPACING = 60; // Maximum pixel spacing between noise lines
  private static final int NOISE_MAX_LINE_LENGTH = 7; // Maximum length of noise lines in pixels
  private static final int NOISE_MIN_THICKNESS = 1; // Minimum stroke thickness for noise lines
  private static final int NOISE_MAX_THICKNESS = 3; // Maximum stroke thickness for noise lines
  private static final int NOISE_CURVE_VARIATION = 2; // Vertical/horizontal variation for curved effect

  /**
   * Generates a random boolean value.
   *
   * @return A random boolean (true or false with equal probability)
   */
  public static boolean generateRandomBoolean() {
    return CaptchaGenerator.RANDOM.nextBoolean();
  }

  /**
   * Generates a random highly saturated, high-contrast color with no white-ish tones.
   *
   * <p>Color Generation Details:
   * <ul>
   *   <li>Uses HSB (Hue, Saturation, Brightness) color space for consistent visibility</li>
   *   <li>Hue: Random across full spectrum (0-360°)</li>
   *   <li>Saturation: 80-100% (ensures deep, vibrant colors with maximum contrast)</li>
   *   <li>Brightness: 50-90% (avoids light/white-ish colors while maintaining visibility)</li>
   * </ul>
   *
   * @return A random high-contrast vibrant Color that is always clearly visible and avoids white tones
   */
  public static Color generateRandomColor() {
    // Generate random hue across full spectrum (0-360 degrees)
    float hue = CaptchaGenerator.RANDOM.nextFloat();

    // Generate saturation between 80% and 100% for maximum color richness and saturation
    float saturation = 0.8f + CaptchaGenerator.RANDOM.nextFloat() * 0.2f;

    // Generate brightness between 50% and 90% to avoid white-ish/light colors
    float brightness = 0.5f + CaptchaGenerator.RANDOM.nextFloat() * 0.4f;

    return new Color(Color.HSBtoRGB(hue, saturation, brightness));
  }

  /**
   * Validates that both an image and a color are not null.
   *
   * @param source The image to validate
   * @param color  The color to validate
   * @throws IllegalArgumentException if either parameter is null
   */
  private static void validateImageAndColor(BufferedImage source, Color color) {
    if (source == null) {
      throw new IllegalArgumentException("Source image cannot be null");
    }
    if (color == null) {
      throw new IllegalArgumentException("Color cannot be null");
    }
  }

  public static byte[] bufferedImageToBytes(BufferedImage image, String formatName) {
    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
      ImageIO.write(image, formatName, byteArrayOutputStream);
      return byteArrayOutputStream.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("Cannot convert BufferedImage to byte array", e);
    }
  }

  public static BufferedImage bytesToBufferedImage(byte[] imageBytes) {
    try (ByteArrayInputStream bytes = new ByteArrayInputStream(imageBytes)) {
      return ImageIO.read(bytes);
    } catch (IOException e) {
      throw new RuntimeException("Cannot convert byte array to BufferedImage", e);
    }
  }

  public static BufferedImage extractArea(BufferedImage source, Rectangle rectangle) {
    if (source == null) {
      throw new IllegalArgumentException("Source image cannot be null");
    }
    if (rectangle == null) {
      throw new IllegalArgumentException("Rectangle cannot be null");
    }
    
    PuzzleImageGeneratorTypes.Point topLeft = rectangle.leftTopCorner();
    int x = topLeft.x();
    int y = topLeft.y();
    int width = rectangle.width();
    int height = rectangle.height();
    
    // Ensure the extracted area is within image bounds
    int maxX = Math.min(x + width, source.getWidth());
    int maxY = Math.min(y + height, source.getHeight());
    
    // Validate coordinates
    if (x < 0 || y < 0 || x >= source.getWidth() || y >= source.getHeight()) {
      throw new IllegalArgumentException("Rectangle position is outside source image bounds");
    }
    
    int extractWidth = maxX - x;
    int extractHeight = maxY - y;
    
    if (extractWidth <= 0 || extractHeight <= 0) {
      throw new IllegalArgumentException("Rectangle dimensions must result in positive area");
    }
    
    return source.getSubimage(x, y, extractWidth, extractHeight);
  }

  /**
   * Adds noise pattern to an image using curved lines with varying thickness.
   *
   * <p>Noise Characteristics:
   * <ul>
   *   <li>Short curved lines: 1-7 pixels in length</li>
   *   <li>High density coverage: 30-60 pixel spacing between lines</li>
   *   <li>Variable thickness: 1-3 pixels per line</li>
   *   <li>Both horizontal and vertical line orientations</li>
   *   <li>Even distribution across the entire image</li>
   * </ul>
   *
   * @param source The source image to add noise to (must not be null)
   * @param color  The color used for drawing the noise lines (must not be null)
   * @return A new BufferedImage with noise pattern applied
   * @throws IllegalArgumentException if source or color is null
   */
  public static BufferedImage addNoise(BufferedImage source, Color color) {
    validateImageAndColor(source, color);

    BufferedImage noisyImage = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
    Graphics2D graphics = null;

    try {
      graphics = noisyImage.createGraphics();
      graphics.drawImage(source, 0, 0, null);
      graphics.setColor(color);

      addHorizontalNoiseLines(graphics, source.getWidth(), source.getHeight());
      addVerticalNoiseLines(graphics, source.getWidth(), source.getHeight());

    } finally {
      if (graphics != null) {
        graphics.dispose();
      }
    }

    return noisyImage;
  }

  /**
   * Adds horizontal noise lines to the image with vertical offset variation.
   *
   * @param graphics The Graphics2D object to draw on
   * @param width    The width of the image
   * @param height   The height of the image
   */
  private static void addHorizontalNoiseLines(Graphics2D graphics, int width, int height) {
    for (int y = 0; y < height; y += generateRandomSpacing()) {
      drawNoiseSegmentsHorizontally(graphics, y, width);
    }
  }

  /**
   * Adds vertical noise lines to the image with horizontal offset variation.
   *
   * @param graphics The Graphics2D object to draw on
   * @param width    The width of the image
   * @param height   The height of the image
   */
  private static void addVerticalNoiseLines(Graphics2D graphics, int width, int height) {
    for (int x = 0; x < width; x += generateRandomSpacing()) {
      drawNoiseSegmentsVertically(graphics, x, height);
    }
  }

  /**
   * Draws horizontal noise line segments starting from a given y-coordinate.
   *
   * @param graphics The Graphics2D object to draw on
   * @param y        The y-coordinate for horizontal lines
   * @param width    The width of the image
   */
  private static void drawNoiseSegmentsHorizontally(Graphics2D graphics, int y, int width) {
    int x = 0;
    while (x < width) {
      int lineLength = CaptchaGenerator.RANDOM.nextInt(NOISE_MAX_LINE_LENGTH) + 1;
      int endX = Math.min(x + lineLength, width - 1);
      int controlY = y + CaptchaGenerator.RANDOM.nextInt(2 * NOISE_CURVE_VARIATION + 1) - NOISE_CURVE_VARIATION;

      applyRandomThicknessAndDraw(graphics, x, y, endX, controlY);
      x = endX + generateRandomSpacing();
    }
  }

  /**
   * Draws vertical noise line segments starting from a given x-coordinate.
   *
   * @param graphics The Graphics2D object to draw on
   * @param x        The x-coordinate for vertical lines
   * @param height   The height of the image
   */
  private static void drawNoiseSegmentsVertically(Graphics2D graphics, int x, int height) {
    int y = 0;
    while (y < height) {
      int lineLength = CaptchaGenerator.RANDOM.nextInt(NOISE_MAX_LINE_LENGTH) + 1;
      int endY = Math.min(y + lineLength, height - 1);
      int controlX = x + CaptchaGenerator.RANDOM.nextInt(2 * NOISE_CURVE_VARIATION + 1) - NOISE_CURVE_VARIATION;

      applyRandomThicknessAndDraw(graphics, x, y, controlX, endY);
      y = endY + generateRandomSpacing();
    }
  }

  /**
   * Applies random stroke thickness and draws a line segment.
   *
   * @param graphics The Graphics2D object to draw on
   * @param x1       The starting x-coordinate
   * @param y1       The starting y-coordinate
   * @param x2       The ending x-coordinate
   * @param y2       The ending y-coordinate
   */
  private static void applyRandomThicknessAndDraw(Graphics2D graphics, int x1, int y1, int x2, int y2) {
    float randomThickness = CaptchaGenerator.RANDOM.nextInt(NOISE_MAX_THICKNESS - NOISE_MIN_THICKNESS + 1)
                            + NOISE_MIN_THICKNESS;
    graphics.setStroke(new BasicStroke(randomThickness));
    graphics.drawLine(x1, y1, x2, y2);
  }

  /**
   * Generates a random spacing value between NOISE_MIN_SPACING and NOISE_MAX_SPACING.
   *
   * @return A random spacing value in pixels
   */
  private static int generateRandomSpacing() {
    return CaptchaGenerator.RANDOM.nextInt(NOISE_MAX_SPACING - NOISE_MIN_SPACING + 1) + NOISE_MIN_SPACING;
  }

  /**
   * Concatenates two images vertically into a single image.
   *
   * <p>Concatenation Details:
   * <ul>
   *   <li>Result width: maximum of the two image widths</li>
   *   <li>Result height: sum of both image heights</li>
   *   <li>Background: filled with white color</li>
   *   <li>Top image is drawn at position (0, 0)</li>
   *   <li>Bottom image is drawn at position (0, topImage.getHeight())</li>
   * </ul>
   *
   * @param topImage    The image to place at the top (must not be null)
   * @param bottomImage The image to place at the bottom (must not be null)
   * @return A new BufferedImage with the two images concatenated vertically
   * @throws IllegalArgumentException if either topImage or bottomImage is null
   */
  public static BufferedImage concatenateImages(BufferedImage topImage, BufferedImage bottomImage) {
    if (topImage == null || bottomImage == null) {
      throw new IllegalArgumentException("Both topImage and bottomImage cannot be null");
    }

    int width = Math.max(topImage.getWidth(), bottomImage.getWidth());
    int height = topImage.getHeight() + bottomImage.getHeight();

    BufferedImage concatenatedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = null;

    try {
      graphics = concatenatedImage.createGraphics();
      graphics.setColor(Color.WHITE);
      graphics.fillRect(0, 0, width, height);

      // Draw top image
      graphics.drawImage(topImage, 0, 0, null);

      // Draw bottom image below the top image
      graphics.drawImage(bottomImage, 0, topImage.getHeight(), null);

    } finally {
      if (graphics != null) {
        graphics.dispose();
      }
    }

    return concatenatedImage;
  }

  /**
   * Rotates a BufferedImage by the specified angle while maintaining the complete rotated shape.
   *
   * <p>Rotation Details:
   * <ul>
   *   <li>Result dimensions are calculated to fit the entire rotated content without clipping</li>
   *   <li>Background is filled with full transparency (ARGB)</li>
   *   <li>Uses high-quality rendering hints (antialiasing, bilinear interpolation)</li>
   * </ul>
   *
   * @param inputImage The image to rotate (must not be null)
   * @param angle      The rotation angle in degrees. If null, uses a random angle (15-195 degrees)
   * @return A new BufferedImage with the rotated content and transparent background
   * @throws IllegalArgumentException if inputImage is null
   */
  public static BufferedImage rotateImage(BufferedImage inputImage, Double angle) {
    if (inputImage == null) {
      throw new IllegalArgumentException("Input image cannot be null");
    }

    // Use provided angle or generate random angle
    double rotationAngle = (angle != null) ? angle : generateRandomAngle(15, 180);
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
   * Generates a random rotation angle within a specified range.
   *
   * @param min   The minimum angle in degrees
   * @param delta The range width (maximum angle = min + delta)
   * @return A random angle between min and (min + delta) degrees
   */
  public static double generateRandomAngle(int min, double delta) {
    return min + CaptchaGenerator.RANDOM.nextDouble() * delta;
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
   *
   * <p>This method is primarily used for visual debugging and highlighting the matched puzzle piece area.
   *
   * <p>Rectangle Style:
   * <ul>
   *   <li>Color: Red</li>
   *   <li>Stroke width: 2 pixels</li>
   *   <li>Dash pattern: 10 pixels on, 10 pixels off</li>
   * </ul>
   *
   * @param gridImage The grid image to draw on (must not be null)
   * @param rectangle The rectangle to draw - contains position and dimensions (must not be null)
   * @return The modified grid image with the dashed rectangle drawn
   * @throws IllegalArgumentException if gridImage or rectangle is null
   */
  public static BufferedImage drawDashedRectangleOnGridImage(BufferedImage gridImage, Rectangle rectangle) {
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

  /**
   * Configures a Graphics2D object with high-quality rendering hints.
   *
   * <p>Applied Rendering Hints:
   * <ul>
   *   <li>Bilinear interpolation for smooth image scaling</li>
   *   <li>High-quality rendering</li>
   *   <li>Anti-aliasing enabled</li>
   * </ul>
   *
   * @param scaledImage The BufferedImage to configure graphics for
   * @return A configured Graphics2D object with quality rendering hints
   */
  private static Graphics2D getGraphics2D(BufferedImage scaledImage) {
    Graphics2D graphics = scaledImage.createGraphics();
    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
        RenderingHints.VALUE_RENDER_QUALITY);
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
    return graphics;
  }

  /**
   * Validates that a BufferedImage is not null and has positive dimensions.
   *
   * @param inputImage The image to validate
   * @throws IllegalArgumentException if inputImage is null or has non-positive dimensions
   */
  private static void validateImageInput(BufferedImage inputImage) {
    if (inputImage == null) {
      throw new IllegalArgumentException("Input image cannot be null");
    }
    if (inputImage.getWidth() <= 0 || inputImage.getHeight() <= 0) {
      throw new IllegalArgumentException("Input image must have positive dimensions");
    }
  }
}
