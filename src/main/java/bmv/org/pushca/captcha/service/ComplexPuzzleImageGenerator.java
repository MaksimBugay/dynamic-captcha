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
import java.util.Random;
import java.util.function.Function;

public class ComplexPuzzleImageGenerator {

  private final Random random = new Random();

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
          int spacingX = minSpacing + random.nextInt(maxSpacing - minSpacing + 1); // Random spacing for X
          int spacingY = minSpacing + random.nextInt(maxSpacing - minSpacing + 1); // Random spacing for Y
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

          graphics.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256))); // Random color
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
    path.moveTo(x + random.nextInt(offset) - offset / 2.0, y + random.nextInt(offset) - offset / 2.0);

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
      int controlX1 = (int) (x1 + i * dx + random.nextInt(noiseFactor) - noiseFactor / 2.0);
      int controlY1 = (int) (y1 + i * dy + random.nextInt(noiseFactor) - noiseFactor / 2.0);
      int controlX2 = (int) (x1 + (i + 1) * dx + random.nextInt(noiseFactor) - noiseFactor / 2.0);
      int controlY2 = (int) (y1 + (i + 1) * dy + random.nextInt(noiseFactor) - noiseFactor / 2.0);
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

    int leftMargin = random.nextInt(totalWidthMargin + 1);
    int topMargin = random.nextInt(totalHeightMargin + 1);

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
    int additionalWidth = random.nextInt(maxAdditionalWidth + 1);
    int additionalHeight = random.nextInt(maxAdditionalHeight + 1);

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
      int leftMargin = random.nextInt(additionalWidth + 1);
      int topMargin = random.nextInt(additionalHeight + 1);

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
      int red = (color.getRed() + random.nextInt(256)) % 256;
      int green = (color.getGreen() + random.nextInt(256)) % 256;
      int blue = (color.getBlue() + random.nextInt(256)) % 256;

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



  public BufferedImage rotateImageRandomly(BufferedImage inputImage) {
    // Generate random angle between 15 and 175 degrees
    double angle = 15 + random.nextDouble() * (175 - 15);
    double radians = Math.toRadians(angle);

    // Calculate dimensions for the rotated image
    double sin = Math.abs(Math.sin(radians));
    double cos = Math.abs(Math.cos(radians));

    int originalWidth = inputImage.getWidth();
    int originalHeight = inputImage.getHeight();

    int newWidth = (int) Math.floor(originalWidth * cos + originalHeight * sin);
    int newHeight = (int) Math.floor(originalHeight * cos + originalWidth * sin);

    // Create new image with calculated dimensions
    BufferedImage rotatedImage = new BufferedImage(newWidth, newHeight, inputImage.getType());
    Graphics2D graphics = null;

    try {
      graphics = rotatedImage.createGraphics();

      // Fill with transparent background
      graphics.setColor(new Color(0, 0, 0, 0));
      graphics.fillRect(0, 0, newWidth, newHeight);

      // Set rendering hints for better quality
      graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                               java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      graphics.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                               java.awt.RenderingHints.VALUE_RENDER_QUALITY);
      graphics.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                               java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

      // Translate to center, rotate, then translate back
      graphics.translate(newWidth / 2, newHeight / 2);
      graphics.rotate(radians);
      graphics.translate(-originalWidth / 2, -originalHeight / 2);

      // Draw the original image
      graphics.drawImage(inputImage, 0, 0, null);

    } finally {
      if (graphics != null) {
        graphics.dispose();
      }
    }

    return rotatedImage;
  }

  public BufferedImage addRandomTransparentHoles(BufferedImage inputImage) {
    // No dashing noise - return image unchanged
    return inputImage;
  }



  public BufferedImage addColorMatchingNoise(BufferedImage inputImage, int gridSize) {
    int width = inputImage.getWidth();
    int height = inputImage.getHeight();
    BufferedImage noisyImage = new BufferedImage(width, height, inputImage.getType());
    Graphics2D graphics = null;

    try {
      graphics = noisyImage.createGraphics();

      // Draw the original image first
      graphics.drawImage(inputImage, 0, 0, null);

      // Set rendering hints for smooth lines
      graphics.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                               java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                               java.awt.RenderingHints.VALUE_RENDER_QUALITY);

      // Sample colors from the transformed image to match the color transformation
      Color[] sampleColors = sampleColorsFromImage(inputImage, 10);

      // No noise lines
      int numberOfNoiseElements = 0;

      for (int i = 0; i < numberOfNoiseElements; i++) {
        // Choose random color from sampled colors
        Color noiseColor = sampleColors[random.nextInt(sampleColors.length)];

        // Add some transparency to make noise subtle
        Color transparentNoiseColor = new Color(
            noiseColor.getRed(),
            noiseColor.getGreen(),
            noiseColor.getBlue(),
            80 + random.nextInt(100) // Alpha between 80-179 for partial transparency
        );

        graphics.setColor(transparentNoiseColor);

        // Use same line thickness as puzzle pieces (1 pixel default stroke)
        float lineWidth = 1.0f;

        // Set solid stroke (dashing will happen later in final stage)
        graphics.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Generate puzzle piece shape that avoids puzzle piece content
        drawNoisePuzzlePiece(graphics, inputImage, width, height, transparentNoiseColor);
      }

    } finally {
      if (graphics != null) {
        graphics.dispose();
      }
    }

    return noisyImage;
  }

  private Color[] sampleColorsFromImage(BufferedImage image, int sampleCount) {
    Color[] colors = new Color[sampleCount];
    int width = image.getWidth();
    int height = image.getHeight();

    for (int i = 0; i < sampleCount; i++) {
      int x = random.nextInt(width);
      int y = random.nextInt(height);
      int rgb = image.getRGB(x, y);
      colors[i] = new Color(rgb, true);
    }

    return colors;
  }

  private void drawNoisePuzzlePiece(Graphics2D graphics, BufferedImage image, int imageWidth, int imageHeight, Color outlineColor) {
    // Try multiple times to find a good position that doesn't heavily overlap puzzle piece
    for (int attempt = 0; attempt < 10; attempt++) {
      // Random position and size for puzzle piece noise
      int x = random.nextInt(imageWidth - 50);
      int y = random.nextInt(imageHeight - 50);
      int size = 20 + random.nextInt(30); // 20-49 pixels size

      // Check if this area has too much puzzle piece content
      if (hasTooMuchPuzzleContent(image, x + size/2, y + size/2, size, size)) {
        continue; // Try another position
      }

      // Draw each side with different random colors (multicolored line)
      int offset = size / 7; // Same offset calculation as puzzle pieces
      Path2D topSide = new Path2D.Double();
      topSide.moveTo(x + random.nextInt(offset) - offset / 2.0, y + random.nextInt(offset) - offset / 2.0);
      drawRandomEdge(topSide, x, y, x + size, y, true);
      graphics.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256))); // Random color
      graphics.draw(topSide);

      Path2D rightSide = new Path2D.Double();
      rightSide.moveTo(x + size, y);
      drawRandomEdge(rightSide, x + size, y, x + size, y + size, true);
      graphics.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256))); // Random color
      graphics.draw(rightSide);

      Path2D bottomSide = new Path2D.Double();
      bottomSide.moveTo(x + size, y + size);
      drawRandomEdge(bottomSide, x + size, y + size, x, y + size, true);
      graphics.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256))); // Random color
      graphics.draw(bottomSide);

      Path2D leftSide = new Path2D.Double();
      leftSide.moveTo(x, y + size);
      drawRandomEdge(leftSide, x, y + size, x, y, true);
      graphics.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256))); // Random color
      graphics.draw(leftSide);

      return; // Successfully drew a multicolored noise puzzle piece, exit
    }
  }

  private boolean hasTooMuchPuzzleContent(BufferedImage image, int centerX, int centerY, int sizeX, int sizeY) {
    int totalPixels = 0;
    int puzzlePixels = 0;

    // Check area around the noise line position - make area bigger to be more strict
    int checkX1 = Math.max(0, centerX - sizeX);
    int checkY1 = Math.max(0, centerY - sizeY);
    int checkX2 = Math.min(image.getWidth() - 1, centerX + sizeX);
    int checkY2 = Math.min(image.getHeight() - 1, centerY + sizeY);

    // Sample every pixel for accurate detection
    for (int y = checkY1; y <= checkY2; y += 2) { // Sample every 2nd pixel
      for (int x = checkX1; x <= checkX2; x += 2) {
        totalPixels++;
        Color pixel = new Color(image.getRGB(x, y), true);

        // Much stricter detection: any pixel with alpha > 30 is considered puzzle content
        if (pixel.getAlpha() > 30) {
          puzzlePixels++;
        }
      }
    }

    // Zero tolerance: reject if ANY puzzle content is found
    return puzzlePixels > 0;
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
            // Random scale coefficient: 1.1 to 1.4 (all pieces enlarged)
            double scaleCoefficient = 1.1 + random.nextDouble() * 0.3; // 1.1 to 1.4

            int finalWidth, finalHeight, drawX, drawY;

            if (applyNoise) {
              // Apply both scaling and rotation when noise is enabled

              // Random rotation angle: 15 to 165 degrees
              double rotationAngle = 15 + random.nextDouble() * 150; // 15 to 165 degrees
              double rotationRadians = Math.toRadians(rotationAngle);

              // Calculate rotated and scaled dimensions
              double sin = Math.abs(Math.sin(rotationRadians));
              double cos = Math.abs(Math.cos(rotationRadians));
              int originalWidth = (int) (image.getWidth() * scaleCoefficient);
              int originalHeight = (int) (image.getHeight() * scaleCoefficient);

              finalWidth = (int) Math.floor(originalWidth * cos + originalHeight * sin);
              finalHeight = (int) Math.floor(originalHeight * cos + originalWidth * sin);

              // Center the rotated and scaled image within the original cell area
              int offsetX = (columnWidths[col] - finalWidth) / 2;
              int offsetY = (rowHeights[row] - finalHeight) / 2;
              drawX = currentX + offsetX;
              drawY = currentY + offsetY;

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

              // Draw the rotated and scaled image
              graphics.drawImage(rotatedScaledImage, drawX, drawY, null);
            } else {
              // Apply only scaling when noise is disabled

              finalWidth = (int) (image.getWidth() * scaleCoefficient);
              finalHeight = (int) (image.getHeight() * scaleCoefficient);

              // Center the scaled image within the original cell area
              int offsetX = (columnWidths[col] - finalWidth) / 2;
              int offsetY = (rowHeights[row] - finalHeight) / 2;
              drawX = currentX + offsetX;
              drawY = currentY + offsetY;

              // Draw the scaled image without rotation
              graphics.drawImage(image, drawX, drawY, finalWidth, finalHeight, null);
            }

            // Create a Rectangle representing the position and size of the transformed image
            Rectangle rectangle = new Rectangle(new Point(drawX, drawY), finalWidth, finalHeight);

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

    return new PuzzleTaskFullData(puzzleAreas, null, gridImage);
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

  public PuzzleTaskFullData concatenateSelectedCellWithGrid(PuzzleTaskData puzzleTaskData, int gridSize, boolean applyNoise) {
    PuzzleImage puzzleImage = puzzleTaskData.getSelectedCellImage();
    PuzzleTaskFullData fullData = concatenateImagesIntoGrid(puzzleTaskData, applyNoise);
    if (fullData == null || puzzleImage == null) {
      throw new IllegalArgumentException("PuzzleTaskFullData and image cannot be null.");
    }

    // Align the width of the image with the width of the puzzle grid image
    int gridWidth = fullData.puzzleGridImage().getWidth();
    PuzzlePiece puzzlePieceWithMargins = addRandomMargins(puzzleImage.image, gridWidth);
    BufferedImage imageWithColorTransformation = applyRandomColorTransformation(puzzlePieceWithMargins.image());
    BufferedImage imageWithNoise = addColorMatchingNoise(imageWithColorTransformation, gridSize);
    BufferedImage imageWithMargins = addRandomTransparentHoles(imageWithNoise);

    PuzzleArea target = new PuzzleArea(
        new Rectangle(puzzlePieceWithMargins.leftTopCorner(), puzzleImage.image().getWidth(), puzzleImage.image().getHeight()),
        puzzleImage.address,
        true,
        imageWithMargins
    );

    // Calculate new dimensions for the composed image
    int minAdditionalSpace = (int) (fullData.puzzleGridImage().getHeight() * 0.1);
    int maxAdditionalSpace = (int) (fullData.puzzleGridImage().getHeight() * 0.3);
    int additionalSpace = minAdditionalSpace + random.nextInt(maxAdditionalSpace - minAdditionalSpace + 1);

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
    return new PuzzleTaskFullData(updatedAreas, target, composedImage);
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
        gridSize,
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

  public record PuzzleTaskFullData(PuzzleArea[] options, PuzzleArea target, BufferedImage puzzleGridImage) {
    public PuzzleArea getCorrectOption() {
      return Arrays.stream(options)
          .filter(PuzzleArea::selected)
          .findFirst().orElseThrow(() -> new IllegalStateException("Correct option not found"));
    }
  }
}