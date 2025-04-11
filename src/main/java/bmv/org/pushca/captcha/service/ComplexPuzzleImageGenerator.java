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
      int maxPieceCenterX = 0, maxPieceCenterY = 0;
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
            maxPieceCenterX = pieceCenterX;
            maxPieceCenterY = pieceCenterY;
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

  public PuzzleTaskFullData concatenateImagesIntoGrid(PuzzleTaskData puzzleTaskData) {
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
            // Draw the image at the specified position
            graphics.drawImage(image, currentX, currentY, null);

            // Create a Rectangle representing the position and size of the embedded image
            Rectangle rectangle = new Rectangle(new Point(currentX, currentY), image.getWidth(), image.getHeight());

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

  public PuzzleTaskFullData concatenateSelectedCellWithGrid(PuzzleTaskData puzzleTaskData) {
    PuzzleImage puzzleImage = puzzleTaskData.getSelectedCellImage();
    PuzzleTaskFullData fullData = concatenateImagesIntoGrid(puzzleTaskData);
    if (fullData == null || puzzleImage == null) {
      throw new IllegalArgumentException("PuzzleTaskFullData and image cannot be null.");
    }

    // Align the width of the image with the width of the puzzle grid image
    int gridWidth = fullData.puzzleGridImage().getWidth();
    PuzzlePiece puzzlePieceWithMargins = addRandomMargins(puzzleImage.image, gridWidth);
    BufferedImage imageWithMargins = applyRandomColorTransformation(puzzlePieceWithMargins.image());

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
        )
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