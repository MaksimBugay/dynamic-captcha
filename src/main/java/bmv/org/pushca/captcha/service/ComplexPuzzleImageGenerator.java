package bmv.org.pushca.captcha.service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

public class ComplexPuzzleImageGenerator {

  public PuzzleTaskData generateComplexPuzzleImages(int squareSideLength, int gridSize, boolean applyNoise,
                                                    boolean drawCenters, boolean drawGridCenters) {
    // Create an image with transparency
    BufferedImage image = new BufferedImage(squareSideLength, squareSideLength, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = null;
    GridCellAddress maxPieceCellAddress;
    try {
      graphics = image.createGraphics();
      //graphics.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256))); // Random color

      // Set background to transparent
      graphics.setComposite(java.awt.AlphaComposite.Clear);
      graphics.fillRect(0, 0, squareSideLength, squareSideLength);
      graphics.setComposite(java.awt.AlphaComposite.Src);

      // Draw complex puzzle grid
      int squareSize = squareSideLength / gridSize; // Dynamic grid size
      double maxDistance = -1;
      maxPieceCellAddress = new GridCellAddress(0, 0);

      for (int i = 0; i < gridSize; i++) {
        for (int j = 0; j < gridSize; j++) {
          int minSpacing = squareSize * 2 / 25;
          int maxSpacing = squareSize * 2 / 3;
          int spacingX = minSpacing + CaptchaGenerator.RANDOM.nextInt(maxSpacing - minSpacing + 1); // Random spacing for X
          int spacingY = minSpacing + CaptchaGenerator.RANDOM.nextInt(maxSpacing - minSpacing + 1); // Random spacing for Y
          int pieceSize = squareSize - Math.max(spacingX, spacingY); // Adjusted piece size

          int pieceX = i * squareSize + spacingX / 2;
          int pieceY = j * squareSize + spacingY / 2;

          // Calculate centers
          int pieceCenterX = pieceX + pieceSize / 2;
          int pieceCenterY = pieceY + pieceSize / 2;
          int cellCenterX = i * squareSize + squareSize / 2;
          int cellCenterY = j * squareSize + squareSize / 2;

          // Calculate distance
          double distance = Math.sqrt(Math.pow(pieceCenterX - cellCenterX, 2) + Math.pow(pieceCenterY - cellCenterY, 2));

          // Check if this is the maximum distance
          if (distance > maxDistance) {
            maxDistance = distance;
            maxPieceCellAddress = new GridCellAddress(i, j);
          }

          graphics.setColor(new Color(CaptchaGenerator.RANDOM.nextInt(256), CaptchaGenerator.RANDOM.nextInt(256),
              CaptchaGenerator.RANDOM.nextInt(256))); // Random color
          drawComplexPuzzlePiece(graphics, pieceX, pieceY, pieceSize, applyNoise);

          // Draw center marker for each puzzle piece
          if (drawCenters) {
            drawCenterMarker(graphics, pieceX, pieceY, pieceSize);
          }
        }
      }

      // Draw center markers for each cell in the grid
      if (drawGridCenters) {
        drawCellCenters(graphics, squareSideLength, gridSize);
      }

      // Draw green center marker for the piece with the maximum distance
      //graphics.setColor(Color.GREEN);
      //graphics.fillOval(maxPieceCenterX - 2, maxPieceCenterY - 2, 4, 4);

    } finally {
      Optional.ofNullable(graphics).ifPresent(Graphics2D::dispose);
    }

    return new PuzzleTaskData(
        splitImageIntoRandomisedCells(image, gridSize),
        maxPieceCellAddress
    );
  }

  private void drawComplexPuzzlePiece(Graphics2D graphics, int x, int y, int size, boolean applyNoise) {
    Path2D path = new Path2D.Double();
    int offset = size / 7; // Increased offset for more randomness

    // Move to the first corner with random offset
    path.moveTo(x + CaptchaGenerator.RANDOM.nextInt(offset) - offset / 2.0, y + CaptchaGenerator.RANDOM.nextInt(offset) - offset / 2.0);

    // Top side with random Bezier curve
    drawRandomEdge(path, x, y, x + size, y, applyNoise);

    // Right side with random Bezier curve
    drawRandomEdge(path, x + size, y, x + size, y + size, applyNoise);

    // Bottom side with random Bezier curve
    drawRandomEdge(path, x + size, y + size, x, y + size, applyNoise);

    // Left side with random Bezier curve
    drawRandomEdge(path, x, y + size, x, y, applyNoise);

    path.closePath();
    graphics.draw(path);
  }

  private void drawRandomEdge(Path2D path, int x1, int y1, int x2, int y2, boolean applyNoise) {
    int segments = 3; // Number of segments per edge for more complexity
    int noiseFactor = applyNoise ? 30 : 15;
    double dx = (x2 - x1) / (double) segments;
    double dy = (y2 - y1) / (double) segments;

    for (int i = 0; i < segments; i++) {
      int controlX1 = (int) (x1 + i * dx + CaptchaGenerator.RANDOM.nextInt(noiseFactor) - noiseFactor / 2.0);
      int controlY1 = (int) (y1 + i * dy + CaptchaGenerator.RANDOM.nextInt(noiseFactor) - noiseFactor / 2.0);
      int controlX2 = (int) (x1 + (i + 1) * dx + CaptchaGenerator.RANDOM.nextInt(noiseFactor) - noiseFactor / 2.0);
      int controlY2 = (int) (y1 + (i + 1) * dy + CaptchaGenerator.RANDOM.nextInt(noiseFactor) - noiseFactor / 2.0);
      int endX = (int) (x1 + (i + 1) * dx);
      int endY = (int) (y1 + (i + 1) * dy);
      path.curveTo(controlX1, controlY1, controlX2, controlY2, endX, endY);
    }
  }

  private void drawCenterMarker(Graphics2D graphics, int x, int y, int size) {
    int centerX = x + size / 2;
    int centerY = y + size / 2;
    graphics.setColor(Color.RED); // Color for the center marker
    graphics.fillOval(centerX - 2, centerY - 2, 4, 4); // Draw a small dot at the center
  }

  private void drawCellCenters(Graphics2D graphics, int squareSideLength, int gridSize) {
    int cellSize = squareSideLength / gridSize; // Size of each cell in the grid
    graphics.setColor(Color.BLUE); // Color for the cell center markers

    for (int i = 0; i < gridSize; i++) {
      for (int j = 0; j < gridSize; j++) {
        int centerX = i * cellSize + cellSize / 2;
        int centerY = j * cellSize + cellSize / 2;
        graphics.fillOval(centerX - 2, centerY - 2, 4, 4); // Draw a small dot at the center of each cell
      }
    }
  }

  public PuzzleImage[] splitImageIntoRandomisedCells(BufferedImage image, int gridSize) {
    // Number of rows
    // Number of columns
    int cellWidth = image.getWidth() / gridSize;
    int cellHeight = image.getHeight() / gridSize;
    PuzzleImage[] gridImages = new PuzzleImage[gridSize * gridSize];

    for (int row = 0; row < gridSize; row++) {
      for (int col = 0; col < gridSize; col++) {
        // Calculate the position and size of each grid cell
        int x = col * cellWidth;
        int y = row * cellHeight;

        // Create a new BufferedImage for each grid cell
        BufferedImage cellImage = new BufferedImage(cellWidth, cellHeight, image.getType());
        cellImage.getGraphics().drawImage(image, 0, 0, cellWidth, cellHeight, x, y, x + cellWidth, y + cellHeight, null);

        // Store the cell image in the array
        gridImages[col * gridSize + row] = new PuzzleImage(new GridCellAddress(col, row), addRandomMarginsAndResize(cellImage));
      }
    }

    return gridImages;
  }

  public PuzzlePiece addRandomMargins(BufferedImage image, int targetLength) {
    int originalWidth = image.getWidth();
    int originalHeight = image.getHeight();

    if (targetLength <= originalWidth || targetLength <= originalHeight) {
      throw new IllegalArgumentException("Target length must be greater than the original dimensions.");
    }

    int totalWidthMargin = targetLength - originalWidth;
    int totalHeightMargin = targetLength - originalHeight;

    int leftMargin = CaptchaGenerator.RANDOM.nextInt(totalWidthMargin + 1);
    int topMargin = CaptchaGenerator.RANDOM.nextInt(totalHeightMargin + 1);

    // Create a new image with the target size
    BufferedImage newImage = new BufferedImage(targetLength, targetLength, image.getType());
    Graphics2D graphics = null;
    try {
      graphics = newImage.createGraphics();

      // Fill the new image with a transparent background
      graphics.setColor(new Color(0, 0, 0, 0)); // Transparent color
      graphics.fillRect(0, 0, targetLength, targetLength);

      // Draw the original image onto the new image with margins
      graphics.drawImage(image, leftMargin, topMargin, null);
    } finally {
      if (graphics != null) {
        graphics.dispose();
      }
    }

    return new PuzzlePiece(new Point(leftMargin, topMargin), newImage);
  }

  public BufferedImage addRandomMarginsAndResize(BufferedImage image) {
    int originalWidth = image.getWidth();
    int originalHeight = image.getHeight();

    // Calculate maximum additional width and height (30% of original dimensions)
    int maxAdditionalWidth = (int) (originalWidth * 0.5);
    int maxAdditionalHeight = (int) (originalHeight * 0.5);

    // Randomly determine the actual additional width and height
    int additionalWidth = CaptchaGenerator.RANDOM.nextInt(maxAdditionalWidth + 1);
    int additionalHeight = CaptchaGenerator.RANDOM.nextInt(maxAdditionalHeight + 1);

    // Calculate new dimensions
    int newWidth = originalWidth + additionalWidth;
    int newHeight = originalHeight + additionalHeight;

    // Create a new image with the new dimensions
    BufferedImage newImage = new BufferedImage(newWidth, newHeight, image.getType());
    Graphics2D graphics = null;
    try {
      graphics = newImage.createGraphics();

      // Fill the new image with a transparent background
      graphics.setColor(new Color(0, 0, 0, 0)); // Transparent color
      graphics.fillRect(0, 0, newWidth, newHeight);

      // Calculate random margins
      int leftMargin = CaptchaGenerator.RANDOM.nextInt(additionalWidth + 1);
      int topMargin = CaptchaGenerator.RANDOM.nextInt(additionalHeight + 1);

      // Draw the original image onto the new image with random margins
      graphics.drawImage(image, leftMargin, topMargin, null);
    } finally {
      if (graphics != null) {
        graphics.dispose();
      }
    }

    return newImage;
  }

  public BufferedImage applyRandomColorTransformation(BufferedImage inputImage) {
    Function<Color, Color> colorTransformation = color -> {
      // Generate random values to modify the color
      int red = (color.getRed() + CaptchaGenerator.RANDOM.nextInt(256)) % 256;
      int green = (color.getGreen() + CaptchaGenerator.RANDOM.nextInt(256)) % 256;
      int blue = (color.getBlue() + CaptchaGenerator.RANDOM.nextInt(256)) % 256;

      // Return the new color with the same alpha value
      return new Color(red, green, blue, color.getAlpha());
    };

    int width = inputImage.getWidth();
    int height = inputImage.getHeight();
    BufferedImage outputImage = new BufferedImage(width, height, inputImage.getType());

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        // Get the original color of the pixel
        Color originalColor = new Color(inputImage.getRGB(x, y), true);

        // Apply a random transformation to the color
        Color transformedColor = colorTransformation.apply(originalColor);

        // Set the transformed color to the output image
        outputImage.setRGB(x, y, transformedColor.getRGB());
      }
    }

    return outputImage;
  }

  public BufferedImage addHarmonicDistortions(BufferedImage inputImage) {
    int width = inputImage.getWidth();
    int height = inputImage.getHeight();
    BufferedImage distortedImage = new BufferedImage(width, height, inputImage.getType());
    Graphics2D graphics = null;

    try {
      graphics = distortedImage.createGraphics();

      // Set high-quality rendering hints
      graphics.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
          java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
          java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      graphics.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
          java.awt.RenderingHints.VALUE_RENDER_QUALITY);

      // Fill with transparent background
      graphics.setColor(new Color(0, 0, 0, 0));
      graphics.fillRect(0, 0, width, height);

      // Apply harmonic distortion by sampling pixels with wave displacement
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          // Calculate harmonic wave distortions (very subtle)
          double waveAmplitude = 0.45; // Maximum displacement in pixels (very subtle distortion)
          double waveFrequency = 0.105; // Wave frequency (very subtle)
          double phaseShift = CaptchaGenerator.RANDOM.nextDouble() * Math.PI * 2; // Random phase for variation

          // Create multiple overlapping sine waves for complex distortion
          double horizontalWave = Math.sin(y * waveFrequency + phaseShift) * waveAmplitude;
          double verticalWave = Math.sin(x * waveFrequency + phaseShift * 1.3) * waveAmplitude;

          // Add secondary harmonic for more natural look (very subtle amplitude)
          double secondaryHorizontal = Math.sin(y * waveFrequency * 2.1 + phaseShift * 0.7) * (waveAmplitude * 0.15);
          double secondaryVertical = Math.sin(x * waveFrequency * 1.8 + phaseShift * 1.1) * (waveAmplitude * 0.15);

          // Calculate source coordinates with distortion
          int sourceX = (int) Math.round(x - horizontalWave - secondaryHorizontal);
          int sourceY = (int) Math.round(y - verticalWave - secondaryVertical);

          // Ensure source coordinates are within bounds
          if (sourceX >= 0 && sourceX < width && sourceY >= 0 && sourceY < height) {
            int pixel = inputImage.getRGB(sourceX, sourceY);
            Color pixelColor = new Color(pixel, true);

            // Only apply distortion to visible pixels (alpha > 0)
            if (pixelColor.getAlpha() > 0) {
              // Apply subtle distortion to the contour edges
              if (isNearContourEdge(inputImage, sourceX, sourceY, 2)) {
                // Enhanced distortion for edge pixels to create wavy contours (very subtle)
                double edgeDistortionFactor = 1.15; // Very subtle edge distortion
                double edgeHorizontal = Math.sin(sourceY * waveFrequency * 0.8 + phaseShift) * (waveAmplitude * edgeDistortionFactor);
                double edgeVertical = Math.sin(sourceX * waveFrequency * 0.9 + phaseShift * 1.2) * (waveAmplitude * edgeDistortionFactor);

                int edgeSourceX = (int) Math.round(x - edgeHorizontal);
                int edgeSourceY = (int) Math.round(y - edgeVertical);

                if (edgeSourceX >= 0 && edgeSourceX < width && edgeSourceY >= 0 && edgeSourceY < height) {
                  pixel = inputImage.getRGB(edgeSourceX, edgeSourceY);
                }
              }

              distortedImage.setRGB(x, y, pixel);
            }
          }
        }
      }

      // Add subtle tremor effect to make shapes appear slightly shaky
      addTremorEffect(distortedImage);

    } finally {
      if (graphics != null) {
        graphics.dispose();
      }
    }

    return distortedImage;
  }

  /**
   * Checks if a pixel is near the contour edge of a shape
   */
  private boolean isNearContourEdge(BufferedImage image, int x, int y, int radius) {
    int width = image.getWidth();
    int height = image.getHeight();

    Color centerPixel = new Color(image.getRGB(x, y), true);

    // If center pixel is transparent, it's not part of the shape
    if (centerPixel.getAlpha() == 0) {
      return false;
    }

    // Check surrounding pixels within radius
    for (int dy = -radius; dy <= radius; dy++) {
      for (int dx = -radius; dx <= radius; dx++) {
        if (dx == 0 && dy == 0) {
          continue; // Skip center pixel
        }

        int checkX = x + dx;
        int checkY = y + dy;

        // If we're at the image boundary, consider it an edge
        if (checkX < 0 || checkX >= width || checkY < 0 || checkY >= height) {
          return true;
        }

        Color neighborPixel = new Color(image.getRGB(checkX, checkY), true);

        // If we find a transparent pixel nearby, we're near an edge
        if (neighborPixel.getAlpha() == 0) {
          return true;
        }

        // If we find a significantly different color, we're near an edge
        int colorDiff = Math.abs(centerPixel.getRed() - neighborPixel.getRed()) +
                        Math.abs(centerPixel.getGreen() - neighborPixel.getGreen()) +
                        Math.abs(centerPixel.getBlue() - neighborPixel.getBlue());

        if (colorDiff > 100) { // Threshold for color difference
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Removes a connected region of colored pixels using systematic edge detection
   */
  public BufferedImage removeConnectedPixelRegion(BufferedImage inputImage) {
    BufferedImage resultImage = cloneBufferedImage(inputImage);

    // Step 1: Scan piece and collect all colored pixels with their neighbors
    java.util.Map<Point, java.util.List<Point>> coloredPixelMap = buildColoredPixelMap(inputImage);

    if (coloredPixelMap.isEmpty()) {
      return resultImage; // No colored pixels found
    }

    // Step 2: Find edge pixels (pixels with fewer than 8 colored neighbors)
    java.util.List<Point> edgePixels = findEdgePixels(coloredPixelMap);

    if (edgePixels.isEmpty()) {
      return resultImage; // No edge pixels found
    }

    // Step 3: Create multiple large gaps for better visibility
    java.util.Set<Point> transparentPixels = new java.util.HashSet<>();

    // Create 3-4 gaps per piece for better visibility
    int numberOfGaps = 3 + CaptchaGenerator.RANDOM.nextInt(2); // 3-4 gaps

    for (int i = 0; i < numberOfGaps && !edgePixels.isEmpty(); i++) {
      // Pick a random edge pixel that hasn't been made transparent yet
      Point startEdgePixel = null;
      for (int attempt = 0; attempt < 20 && startEdgePixel == null; attempt++) {
        Point candidate = edgePixels.get(CaptchaGenerator.RANDOM.nextInt(edgePixels.size()));
        if (!transparentPixels.contains(candidate)) {
          startEdgePixel = candidate;
        }
      }

      if (startEdgePixel != null) {
        // Large visible gaps: 120-180 pixels each
        createBidirectionalGapFromEdge(resultImage, startEdgePixel, coloredPixelMap, transparentPixels, 120, 180);
      }
    }

    // Additional moderate circular gaps for visibility
    createModerateCircularGaps(resultImage, coloredPixelMap, transparentPixels);

    return resultImage;
  }

  private static BufferedImage cloneBufferedImage(BufferedImage inputImage) {
    int width = inputImage.getWidth();
    int height = inputImage.getHeight();
    BufferedImage resultImage = new BufferedImage(width, height, inputImage.getType());

    // Copy the original image
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        resultImage.setRGB(x, y, inputImage.getRGB(x, y));
      }
    }
    return resultImage;
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

  /**
   * Creates additional moderate circular gaps for visibility
   */
  private void createModerateCircularGaps(BufferedImage image, java.util.Map<Point, java.util.List<Point>> coloredPixelMap,
                                          java.util.Set<Point> transparentPixels) {
    // Create 1 additional moderate circular gap
    int circularGaps = 1;

    for (int i = 0; i < circularGaps; i++) {
      // Find a random colored pixel that hasn't been removed
      Point center = null;
      for (int attempt = 0; attempt < 30; attempt++) {
        java.util.Set<Point> allPoints = coloredPixelMap.keySet();
        if (allPoints.isEmpty()) {
          break;
        }

        Point candidate = allPoints.iterator().next();
        int skipCount = CaptchaGenerator.RANDOM.nextInt(allPoints.size());
        for (Point p : allPoints) {
          if (skipCount-- <= 0) {
            candidate = p;
            break;
          }
        }

        if (!transparentPixels.contains(candidate)) {
          center = candidate;
          break;
        }
      }

      if (center != null) {
        // Create a larger circular gap (10-15 pixel radius)
        int radius = 10 + CaptchaGenerator.RANDOM.nextInt(6);

        for (int dy = -radius; dy <= radius; dy++) {
          for (int dx = -radius; dx <= radius; dx++) {
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance <= radius) {
              Point gapPoint = new Point(center.x() + dx, center.y() + dy);
              if (coloredPixelMap.containsKey(gapPoint) && !transparentPixels.contains(gapPoint)) {
                image.setRGB(gapPoint.x(), gapPoint.y(), 0x00000000);
                transparentPixels.add(gapPoint);
              }
            }
          }
        }
      }
    }
  }

  private Point[] gatherAgeColoredPoints(BufferedImage image) {
    TreeMap<Integer, Integer> coloredX2YPixelMap = new TreeMap<>();
    TreeMap<Integer, Integer> coloredY2XPixelMap = new TreeMap<>();

    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        Color pixel = new Color(image.getRGB(x, y), true);
        if (pixel.getAlpha() > 0) {
          coloredX2YPixelMap.put(x, y);
          coloredY2XPixelMap.put(y, x);
        }
      }
    }

    return new Point[] {
        new Point(coloredX2YPixelMap.firstEntry().getKey(), coloredX2YPixelMap.firstEntry().getValue()),
        new Point(coloredX2YPixelMap.lastEntry().getKey(), coloredX2YPixelMap.lastEntry().getValue()),
        new Point(coloredY2XPixelMap.firstEntry().getValue(), coloredY2XPixelMap.firstEntry().getKey()),
        new Point(coloredY2XPixelMap.lastEntry().getValue(), coloredY2XPixelMap.lastEntry().getKey())
    };
  }

  /**
   * Builds a map of all colored pixels with their colored neighbors
   */
  private java.util.Map<Point, java.util.List<Point>> buildColoredPixelMap(BufferedImage image) {
    java.util.Map<Point, java.util.List<Point>> coloredPixelMap = new java.util.HashMap<>();

    // First pass: collect all colored pixels
    java.util.Set<Point> allColoredPixels = new java.util.HashSet<>();
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        Color pixel = new Color(image.getRGB(x, y), true);
        if (pixel.getAlpha() > 0) {
          allColoredPixels.add(new Point(x, y));
        }
      }
    }

    // Second pass: for each colored pixel, find its colored neighbors
    for (Point coloredPixel : allColoredPixels) {
      java.util.List<Point> neighbors = new java.util.ArrayList<>();

      // Check 8-connected neighbors
      for (int dy = -1; dy <= 1; dy++) {
        for (int dx = -1; dx <= 1; dx++) {
          if (dx == 0 && dy == 0) {
            continue; // Skip self
          }

          int nx = coloredPixel.x() + dx;
          int ny = coloredPixel.y() + dy;
          Point neighbor = new Point(nx, ny);

          if (allColoredPixels.contains(neighbor)) {
            neighbors.add(neighbor);
          }
        }
      }

      coloredPixelMap.put(coloredPixel, neighbors);
    }

    return coloredPixelMap;
  }

  /**
   * Finds edge pixels (pixels with fewer than 8 colored neighbors)
   */
  private java.util.List<Point> findEdgePixels(java.util.Map<Point, java.util.List<Point>> coloredPixelMap) {
    java.util.List<Point> edgePixels = new java.util.ArrayList<>();

    for (java.util.Map.Entry<Point, java.util.List<Point>> entry : coloredPixelMap.entrySet()) {
      Point pixel = entry.getKey();
      java.util.List<Point> neighbors = entry.getValue();

      // If pixel has fewer than 8 neighbors, it's an edge pixel
      if (neighbors.size() < 8) {
        edgePixels.add(pixel);
      }
    }

    return edgePixels;
  }

  /**
   * Creates bidirectional gap from edge pixel, avoiding already transparent pixels
   */
  private void createBidirectionalGapFromEdge(BufferedImage image, Point startPixel,
                                              java.util.Map<Point, java.util.List<Point>> coloredPixelMap,
                                              java.util.Set<Point> transparentPixels,
                                              int minPixels, int maxPixels) {

    int targetPixels = minPixels + CaptchaGenerator.RANDOM.nextInt(maxPixels - minPixels + 1);

    // Find two different directions from the edge pixel
    java.util.List<Point> neighbors = coloredPixelMap.get(startPixel);
    if (neighbors == null || neighbors.isEmpty()) {
      return;
    }

    // Pick two random directions if possible
    Point direction1 = neighbors.get(CaptchaGenerator.RANDOM.nextInt(neighbors.size()));
    Point direction2 = neighbors.size() > 1 ?
        neighbors.get(CaptchaGenerator.RANDOM.nextInt(neighbors.size())) : direction1;

    // Calculate direction vectors
    int[] dir1 = {direction1.x() - startPixel.x(), direction1.y() - startPixel.y()};
    int[] dir2 = {direction2.x() - startPixel.x(), direction2.y() - startPixel.y()};

    // Remove starting pixel
    image.setRGB(startPixel.x(), startPixel.y(), 0x00000000);
    transparentPixels.add(startPixel);
    int removedCount = 1;

    // Remove pixels in first direction
    removedCount += removePixelsInDirectionFromMap(image, startPixel, dir1,
        targetPixels / 2, coloredPixelMap, transparentPixels);

    // Remove pixels in second direction
    removePixelsInDirectionFromMap(image, startPixel, dir2,
        targetPixels - removedCount, coloredPixelMap, transparentPixels);
  }

  /**
   * Removes pixels in a direction using the colored pixel map
   */
  private int removePixelsInDirectionFromMap(BufferedImage image, Point startPoint, int[] direction,
                                             int maxPixels, java.util.Map<Point, java.util.List<Point>> coloredPixelMap,
                                             java.util.Set<Point> transparentPixels) {
    int removedCount = 0;
    int currentX = startPoint.x();
    int currentY = startPoint.y();
    int consecutiveTransparentHits = 0;

    while (removedCount < maxPixels && consecutiveTransparentHits < 3) {
      currentX += direction[0];
      currentY += direction[1];

      Point currentPoint = new Point(currentX, currentY);

      // Check if this pixel exists in our colored pixel map and is not already transparent
      if (coloredPixelMap.containsKey(currentPoint) && !transparentPixels.contains(currentPoint)) {
        image.setRGB(currentX, currentY, 0x00000000);
        transparentPixels.add(currentPoint);
        removedCount++;
        consecutiveTransparentHits = 0; // Reset counter when we find colored pixel

        // Remove larger areas around each pixel for wider gaps
        for (int dy = -3; dy <= 3; dy++) {
          for (int dx = -3; dx <= 3; dx++) {
            if (dx == 0 && dy == 0) {
              continue;
            }
            if (removedCount >= maxPixels) {
              break;
            }

            Point neighborPoint = new Point(currentX + dx, currentY + dy);
            if (coloredPixelMap.containsKey(neighborPoint) && !transparentPixels.contains(neighborPoint) &&
                CaptchaGenerator.RANDOM.nextDouble() < 0.6) {
              image.setRGB(neighborPoint.x(), neighborPoint.y(), 0x00000000);
              transparentPixels.add(neighborPoint);
              removedCount++;
            }
          }
        }
      } else {
        consecutiveTransparentHits++;
        // Try to continue for a few pixels in case there are more colored pixels ahead
      }
    }

    return removedCount;
  }

  /**
   * Adds a subtle tremor effect to make the image appear slightly shaky
   */
  private void addTremorEffect(BufferedImage distortedImage) {
    int width = distortedImage.getWidth();
    int height = distortedImage.getHeight();

    // Create a temporary image for the tremor effect
    BufferedImage tremorImage = new BufferedImage(width, height, distortedImage.getType());
    Graphics2D graphics = null;

    try {
      graphics = tremorImage.createGraphics();
      graphics.setColor(new Color(0, 0, 0, 0));
      graphics.fillRect(0, 0, width, height);

      // Apply random micro-displacement to simulate hand tremor
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          Color pixel = new Color(distortedImage.getRGB(x, y), true);

          if (pixel.getAlpha() > 0) {
            // Random micro-displacement (very subtle)
            double tremorAmplitude = 0.10; // Very subtle tremor effect
            int tremorX = (int) Math.round((CaptchaGenerator.RANDOM.nextGaussian() * tremorAmplitude));
            int tremorY = (int) Math.round((CaptchaGenerator.RANDOM.nextGaussian() * tremorAmplitude));

            int newX = Math.max(0, Math.min(width - 1, x + tremorX));
            int newY = Math.max(0, Math.min(height - 1, y + tremorY));

            tremorImage.setRGB(newX, newY, pixel.getRGB());
          }
        }
      }

      // Blend the tremor effect back onto the distorted image
      graphics = distortedImage.createGraphics();
      graphics.setComposite(java.awt.AlphaComposite.SrcOver);
      graphics.drawImage(tremorImage, 0, 0, null);

    } finally {
      if (graphics != null) {
        graphics.dispose();
      }
    }
  }

  public PuzzleTaskFullData concatenateImagesIntoGrid(PuzzleTaskData puzzleTaskData, boolean applyNoise) {
    PuzzleImage[] images = puzzleTaskData.images();
    if (images == null || images.length == 0) {
      throw new IllegalArgumentException("Images array cannot be null or empty.");
    }

    // Determine the grid dimensions
    int maxCellX = 0;
    int maxCellY = 0;
    Map<GridCellAddress, BufferedImage> imageMap = new HashMap<>();

    for (PuzzleImage puzzleImage : images) {
      GridCellAddress address = puzzleImage.address();
      imageMap.put(address, puzzleImage.image());
      maxCellX = Math.max(maxCellX, address.cellX());
      maxCellY = Math.max(maxCellY, address.cellY());
    }

    // Calculate total dimensions of the grid
    int totalWidth = 0;
    int totalHeight = 0;
    int[] columnWidths = new int[maxCellX + 1];
    int[] rowHeights = new int[maxCellY + 1];

    for (Map.Entry<GridCellAddress, BufferedImage> entry : imageMap.entrySet()) {
      GridCellAddress address = entry.getKey();
      BufferedImage image = entry.getValue();
      columnWidths[address.cellX()] = Math.max(columnWidths[address.cellX()], image.getWidth());
      rowHeights[address.cellY()] = Math.max(rowHeights[address.cellY()], image.getHeight());
    }

    for (int width : columnWidths) {
      totalWidth += width;
    }
    for (int height : rowHeights) {
      totalHeight += height;
    }

    // Create a new image with the total dimensions
    BufferedImage gridImage = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
    // Array to hold PuzzleArea objects
    PuzzleArea[] puzzleAreas = new PuzzleArea[images.length];
    Graphics2D graphics = null;
    try {
      graphics = gridImage.createGraphics();
      // Draw each image onto the grid image
      int currentY = 0;
      for (int row = 0; row <= maxCellY; row++) {
        int currentX = 0;
        for (int col = 0; col <= maxCellX; col++) {
          GridCellAddress address = new GridCellAddress(col, row);
          BufferedImage image = imageMap.get(address);
          if (image != null) {
            // Apply harmonic distortions to each puzzle piece in the grid
            image = addHarmonicDistortions(image);

            // Remove connected pixel region from each puzzle piece
            //image = removeConnectedPixelRegion(image);

            BufferedImage withDeformedAgesImage = drawRandomCurvedLineThroughPoints(
                image,
                new Color(255, 0, 0, 0)
            );


            // scale and rotate
            Rectangle rectangle = prepareOptionPieceRectangle(
                graphics, withDeformedAgesImage, currentX, currentY, columnWidths[col], rowHeights[row], applyNoise
            );

            // Create a PuzzleArea object
            puzzleAreas[col * (maxCellY + 1) + row] = new PuzzleArea(
                rectangle,
                address,
                (row == puzzleTaskData.selectedCell.cellY()) && (col == puzzleTaskData.selectedCell.cellX()),
                image);
          }
          currentX += columnWidths[col];
        }
        currentY += rowHeights[row];
      }
    } finally {
      if (graphics != null) {
        graphics.dispose();
      }
    }

    return new PuzzleTaskFullData(puzzleAreas, null, gridImage, null);
  }

  private Rectangle prepareOptionPieceRectangle(Graphics2D graphics, BufferedImage image,
                                                int currentX, int currentY,
                                                int columnWidths, int rowHeights,
                                                boolean applyNoise) {
    final int currentXFinal = currentX;
    final int cellWidth = columnWidths;
    final int currentYFinal = currentY;
    final int cellHeight = rowHeights;
    RotateAndScaleImageResult rotateAndScaleImageResult = rotateAndScaleRandomly(
        image,
        applyNoise ? null : 0.0,
        finalWidth0 -> currentXFinal + ((cellWidth - finalWidth0) / 2),
        finalHeight0 -> currentYFinal + ((cellHeight - finalHeight0) / 2)
    );
    // Draw the rotated and scaled image
    graphics.drawImage(
        rotateAndScaleImageResult.image(),
        rotateAndScaleImageResult.drawX(),
        rotateAndScaleImageResult.drawY(),
        null
    );

    // Create a Rectangle representing the position and size of the transformed image
    return new Rectangle(
        new Point(rotateAndScaleImageResult.drawX, rotateAndScaleImageResult.drawY),
        rotateAndScaleImageResult.finalWidth,
        rotateAndScaleImageResult.finalHeight
    );
  }

  private record RotateAndScaleImageResult(BufferedImage image, int drawX, int drawY, int finalWidth, int finalHeight) {
  }

  private RotateAndScaleImageResult rotateAndScaleRandomly(BufferedImage image,
                                                           Double inRotationAngle,
                                                           Function<Integer, Integer> drawXSupplier,
                                                           Function<Integer, Integer> drawYSupplier) {
    // Random scale coefficient: 1.1 to 1.4 (all pieces enlarged)
    double randomizer = CaptchaGenerator.RANDOM.nextDouble(0.3);
    double scaleCoefficient = 1.1 + randomizer; // 1.1 to 1.4

    int scaledWidth = image.getWidth();
    int scaledHeight = image.getHeight();

    // Random rotation angle: 15 to 165 degrees
    double rotationAngle =
        (inRotationAngle == null) ? 20 + CaptchaGenerator.RANDOM.nextDouble() * 340 : inRotationAngle; // 20 to 340 degrees
    double rotationRadians = Math.toRadians(rotationAngle);

    // Calculate rotated and scaled dimensions
    double sin = Math.abs(Math.sin(rotationRadians));
    double cos = Math.abs(Math.cos(rotationRadians));

    int finalWidth = (int) Math.floor(scaledWidth * cos + scaledHeight * sin);
    int finalHeight = (int) Math.floor(scaledHeight * cos + scaledWidth * sin);

    // Center the rotated and scaled image within the original cell area
    int drawX = drawXSupplier.apply(finalWidth);
    int drawY = drawYSupplier.apply(finalHeight);

    // Create rotated and scaled image
    BufferedImage rotatedScaledImage = new BufferedImage(finalWidth, finalHeight, image.getType());
    Graphics2D g2d = rotatedScaledImage.createGraphics();
    try {
      // Set rendering hints for quality
      g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
          java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
          java.awt.RenderingHints.VALUE_RENDER_QUALITY);
      g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
          java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

      // Fill with transparent background
      g2d.setColor(new Color(0, 0, 0, 0));
      g2d.fillRect(0, 0, finalWidth, finalHeight);

      // Apply rotation and scaling
      g2d.translate(finalWidth / 2, finalHeight / 2);
      g2d.rotate(rotationRadians);
      g2d.scale(scaleCoefficient, scaleCoefficient);
      g2d.translate(-image.getWidth() / 2, -image.getHeight() / 2);

      // Draw the original image
      g2d.drawImage(image, 0, 0, null);
    } finally {
      g2d.dispose();
    }
    return new RotateAndScaleImageResult(rotatedScaledImage, drawX, drawY, finalWidth, finalHeight);
  }


  public BufferedImage drawDashedRectangleOnGridImage(BufferedImage gridImage, Rectangle rectangle) {
    Graphics2D graphics = null;

    try {
      graphics = gridImage.createGraphics();
      // Set up dashed stroke
      float[] dashPattern = {10, 10}; // Dash pattern: 10 pixels on, 10 pixels off
      graphics.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, dashPattern, 0));
      graphics.setColor(Color.RED); // Color for the dashed rectangle

      // Draw the dashed rectangle
      Point topLeft = rectangle.leftTopCorner();
      graphics.drawRect(topLeft.x(), topLeft.y(), rectangle.width(), rectangle.height());
    } finally {
      if (graphics != null) {
        graphics.dispose();
      }
    }

    return gridImage;
  }

  public PuzzleTaskFullData concatenateSelectedCellWithGrid(PuzzleTaskData puzzleTaskData, boolean applyNoise) {
    PuzzleImage puzzleImage = puzzleTaskData.getSelectedCellImage();
    PuzzleTaskFullData fullData = concatenateImagesIntoGrid(puzzleTaskData, applyNoise);
    if (fullData == null || puzzleImage == null) {
      throw new IllegalArgumentException("PuzzleTaskFullData and image cannot be null.");
    }

    // Align the width of the image with the width of the puzzle grid image
    int gridWidth = fullData.puzzleGridImage().getWidth();
    double scaleCoefficient = 1.6 + CaptchaGenerator.RANDOM.nextDouble(0.4);
    BufferedImage scaledTopPieceImage = scaleBufferedImage(puzzleImage.image, scaleCoefficient);
    PuzzlePiece puzzlePieceWithMargins = addRandomMargins(
        scaledTopPieceImage,
        gridWidth
    );
    BufferedImage imageWithMargins = applyRandomColorTransformation(puzzlePieceWithMargins.image());

    PuzzleArea target = new PuzzleArea(
        new Rectangle(puzzlePieceWithMargins.leftTopCorner(), scaledTopPieceImage.getWidth(), scaledTopPieceImage.getHeight()),
        puzzleImage.address,
        true,
        imageWithMargins
    );

    // Calculate new dimensions for the composed image
    int minAdditionalSpace = (int) (fullData.puzzleGridImage().getHeight() * 0.1);
    int maxAdditionalSpace = (int) (fullData.puzzleGridImage().getHeight() * 0.3);
    int additionalSpace = minAdditionalSpace + CaptchaGenerator.RANDOM.nextInt(maxAdditionalSpace - minAdditionalSpace + 1);

    int composedHeight = fullData.puzzleGridImage().getHeight() + imageWithMargins.getHeight() + additionalSpace;
    BufferedImage composedImage = new BufferedImage(gridWidth, composedHeight, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = null;

    try {
      graphics = composedImage.createGraphics();
      // Set a soft beige background color
      //graphics.setColor(new Color(245, 245, 220));
      graphics.setColor(Color.WHITE);
      graphics.fillRect(0, 0, gridWidth, composedHeight);

      // Draw the image with margins at the top
      graphics.drawImage(imageWithMargins, 0, 0, null);

      // Draw the puzzle grid image
      graphics.drawImage(fullData.puzzleGridImage(), 0, imageWithMargins.getHeight() + additionalSpace, null);

      // Draw a dividing line
      graphics.setColor(Color.BLACK);
      graphics.drawLine(0, imageWithMargins.getHeight(), gridWidth, imageWithMargins.getHeight());
    } finally {
      if (graphics != null) {
        graphics.dispose();
      }
    }

    // Update PuzzleArea array with increased top positions
    PuzzleArea[] updatedAreas = new PuzzleArea[fullData.options().length];
    for (int i = 0; i < fullData.options().length; i++) {
      PuzzleArea area = fullData.options()[i];
      Rectangle updatedRectangle = new Rectangle(
          new Point(area.rectangle().leftTopCorner().x(),
              area.rectangle().leftTopCorner().y() + imageWithMargins.getHeight() + additionalSpace),
          area.rectangle().width(),
          area.rectangle().height()
      );
      updatedAreas[i] = new PuzzleArea(updatedRectangle, area.address(), area.selected(), area.image());
    }

    // Return new PuzzleTaskFullData with the composed image and updated areas
    return new PuzzleTaskFullData(updatedAreas, target, composedImage, scaleCoefficient);
  }

  public PuzzleTaskFullData generatePuzzleTaskFullData(int squareSideLength, int gridSize, boolean applyNoise) {
    return concatenateSelectedCellWithGrid(
        generateComplexPuzzleImages(
            squareSideLength,
            gridSize,
            applyNoise,
            false,
            false
        ),
        applyNoise
    );
  }

  public record Point(int x, int y) {
  }

  public record PuzzlePiece(Point leftTopCorner, BufferedImage image) {
  }

  public record Rectangle(Point leftTopCorner, int width, int height) {
  }

  public record PuzzleArea(Rectangle rectangle, GridCellAddress address, boolean selected, BufferedImage image) {
  }

  public record GridCellAddress(int cellX, int cellY) {
  }

  public record PuzzleImage(GridCellAddress address, BufferedImage image) {
  }

  public record PuzzleTaskData(PuzzleImage[] images, GridCellAddress selectedCell) {
    public PuzzleImage getSelectedCellImage() {
      return Arrays.stream(images())
          .filter(p -> p.address.equals(selectedCell()))
          .findFirst().orElseThrow(() -> new IllegalStateException("Selected cell not found"));
    }
  }

  public record PuzzleTaskFullData(PuzzleArea[] options, PuzzleArea target, BufferedImage puzzleGridImage, Double scaleCoefficient) {
    public PuzzleArea getCorrectOption() {
      PuzzleArea correctOption = Arrays.stream(options)
          .filter(PuzzleArea::selected)
          .findFirst().orElseThrow(() -> new IllegalStateException("Correct option not found"));
      if (scaleCoefficient == null) {
        return correctOption;
      }

      return new PuzzleArea(
          new Rectangle(
              correctOption.rectangle.leftTopCorner,
              (int) (correctOption.rectangle.width * scaleCoefficient),
              (int) (correctOption.rectangle.height * scaleCoefficient)
          ),
          correctOption.address,
          correctOption.selected,
          scaleBufferedImage(correctOption.image, scaleCoefficient)
      );
    }
  }

  public BufferedImage drawRandomCurvedLineThroughPoints(BufferedImage inputImage, Color color) {
    double width = (0.02 + CaptchaGenerator.RANDOM.nextDouble(0.02)) * ((1.0 * inputImage.getWidth() + 1.0 * inputImage.getHeight()) / 2);

    Point[] agePoints = gatherAgeColoredPoints(inputImage);
    int ageIndex = CaptchaGenerator.RANDOM.nextInt(4);

    BufferedImage bufferedImage = drawCurvedLineThroughPoints(
        inputImage,
        color,
        (int) width,
        agePoints[ageIndex],
        agePoints[getNextAgeIndex(ageIndex)]
    );

    ageIndex = getNextAgeIndex(getNextAgeIndex(ageIndex));
    bufferedImage = drawCurvedLineThroughPoints(
        bufferedImage,
        color,
        (int) width,
        agePoints[ageIndex],
        agePoints[getNextAgeIndex(ageIndex)]
    );
    return bufferedImage;
  }

  private int getNextAgeIndex(int ageIndex) {
    return ageIndex == 3 ? 0 : ageIndex + 1;
  }

  /**
   * Draws a random curved line through two points, extending to image borders
   *
   * @param inputImage The BufferedImage to draw on
   * @param color      The color of the line
   * @param width      The width of the line
   * @param point1     First point the line should pass through
   * @param point2     Second point the line should pass through
   * @return A new BufferedImage with the curved line drawn
   */
  public BufferedImage drawCurvedLineThroughPoints(BufferedImage inputImage, Color color, int width, Point point1, Point point2) {
    // Create a deep copy of the input image
    BufferedImage outputImage = cloneBufferedImage(inputImage);

    Graphics2D graphics = null;
    try {
      graphics = outputImage.createGraphics();
      graphics.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
          java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
          java.awt.RenderingHints.VALUE_RENDER_QUALITY);

      graphics.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

      // Calculate direction vector from point1 to point2
      double dx = point2.x() - point1.x();
      double dy = point2.y() - point1.y();
      double length = Math.sqrt(dx * dx + dy * dy);

      if (length == 0) {
        // Points are the same, draw a horizontal line across the image
        dx = 1;
        dy = 0;
        length = 1;
      }

      // Normalize direction vector
      double unitX = dx / length;
      double unitY = dy / length;

      // Extend line to image borders
      Point startPoint = extendPointToBorder(outputImage, point1, -unitX, -unitY);
      Point endPoint = extendPointToBorder(outputImage, point2, unitX, unitY);

      // Create curved path with random control points
      Path2D path = new Path2D.Double();
      path.moveTo(startPoint.x(), startPoint.y());

      // Calculate intermediate points for creating curves
      int segments = 4 + CaptchaGenerator.RANDOM.nextInt(3); // 4-6 segments for variety

      for (int i = 1; i <= segments; i++) {
        double t = (double) i / segments;

        // Linear interpolation between start and end points
        double x = startPoint.x() + t * (endPoint.x() - startPoint.x());
        double y = startPoint.y() + t * (endPoint.y() - startPoint.y());

        // Add random curvature
        double maxDeviation = Math.min(outputImage.getWidth(), outputImage.getHeight()) * 0.1; // 10% of image size
        double deviation = (CaptchaGenerator.RANDOM.nextDouble() - 0.5) * maxDeviation;

        // Perpendicular direction for curve deviation
        double perpX = -unitY;

        double curveX = x + deviation * perpX;
        double curveY = y + deviation * unitX;

        // Ensure curve points stay within image bounds
        curveX = Math.max(0, Math.min(outputImage.getWidth() - 1, curveX));
        curveY = Math.max(0, Math.min(outputImage.getHeight() - 1, curveY));

        if (i == segments) {
          // Last segment, draw to end point
          path.lineTo(endPoint.x(), endPoint.y());
        } else {
          // Create smooth curve using quadratic curve
          double nextT = (double) (i + 1) / segments;
          double nextX = startPoint.x() + nextT * (endPoint.x() - startPoint.x());
          double nextY = startPoint.y() + nextT * (endPoint.y() - startPoint.y());

          path.quadTo(curveX, curveY, nextX, nextY);
        }
      }

      // Step 1: Clear pixels along the path (make them transparent)
      graphics.setComposite(java.awt.AlphaComposite.Clear);
      graphics.draw(path);

      // Step 2: Draw the colored line on the cleared path
      graphics.setComposite(java.awt.AlphaComposite.SrcOver);
      graphics.setColor(color);
      graphics.draw(path);

    } finally {
      if (graphics != null) {
        graphics.dispose();
      }
    }

    return outputImage;
  }

  /**
   * Extends a point to the image border in the given direction
   */
  private Point extendPointToBorder(BufferedImage image, Point point, double dirX, double dirY) {
    int width = image.getWidth();
    int height = image.getHeight();

    double x = point.x();
    double y = point.y();

    // Calculate intersection with image borders
    double tToRightBorder = (width - 1 - x) / dirX;
    double tToLeftBorder = (0 - x) / dirX;
    double tToBottomBorder = (height - 1 - y) / dirY;
    double tToTopBorder = (0 - y) / dirY;

    // Find the smallest positive t (closest border in the direction)
    double t = Double.MAX_VALUE;

    if (dirX > 0 && tToRightBorder > 0) {
      t = Math.min(t, tToRightBorder);
    }
    if (dirX < 0 && tToLeftBorder > 0) {
      t = Math.min(t, tToLeftBorder);
    }
    if (dirY > 0 && tToBottomBorder > 0) {
      t = Math.min(t, tToBottomBorder);
    }
    if (dirY < 0 && tToTopBorder > 0) {
      t = Math.min(t, tToTopBorder);
    }

    // If no valid intersection found, use a large value
    if (t == Double.MAX_VALUE) {
      t = Math.max(width, height);
    }

    // Calculate extended point
    int extendedX = (int) Math.round(x + t * dirX);
    int extendedY = (int) Math.round(y + t * dirY);

    // Clamp to image bounds
    extendedX = Math.max(0, Math.min(width - 1, extendedX));
    extendedY = Math.max(0, Math.min(height - 1, extendedY));

    return new Point(extendedX, extendedY);
  }
}