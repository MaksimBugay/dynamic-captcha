package bmv.org.pushca.captcha;

import static bmv.org.pushca.captcha.DynamicCaptchaApplicationTests.fileSystemCleanup;
import static bmv.org.pushca.captcha.DynamicCaptchaApplicationTests.saveBytesToFile;
import static bmv.org.pushca.captcha.service.CaptchaGenerator.bufferedImageToBytes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.Random;

public class ComplexPuzzleImageGenerator1 {

  private final Random random = new Random();

  public PuzzleTaskData generateComplexPuzzleImages(int squareSideLength, int gridSize, boolean applyNoise,
                                                    boolean drawCenters, boolean drawGridCenters) {
    // Create an image with transparency
    BufferedImage image = new BufferedImage(squareSideLength, squareSideLength, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = null;
    GridCellAddress maxPieceCellAddress;
    try {
      graphics = image.createGraphics();

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
      graphics.setColor(Color.GREEN);
      graphics.fillOval(maxPieceCenterX - 2, maxPieceCenterY - 2, 4, 4);

    } finally {
      Optional.ofNullable(graphics).ifPresent(Graphics2D::dispose);
    }

    return new PuzzleTaskData(
        splitImageIntoCells(image, gridSize),
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

  public PuzzleImage[] splitImageIntoCells(BufferedImage image, int gridSize) {
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
        gridImages[col * gridSize + row] = new PuzzleImage(new GridCellAddress(col, row), cellImage);
      }
    }

    return gridImages;
  }

  public PuzzleArea addRandomMargins(BufferedImage image, int targetLength) {
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
    Graphics2D graphics = newImage.createGraphics();

    // Fill the new image with a transparent background
    graphics.setColor(new Color(0, 0, 0, 0)); // Transparent color
    graphics.fillRect(0, 0, targetLength, targetLength);

    // Draw the original image onto the new image with margins
    graphics.drawImage(image, leftMargin, topMargin, null);
    graphics.dispose();

    return new PuzzleArea(new Point(leftMargin, topMargin), newImage);
  }

  public static void main(String[] args) {
    fileSystemCleanup();
    ComplexPuzzleImageGenerator1 generator = new ComplexPuzzleImageGenerator1();
    int gridSize = 3; // Example grid size
    int squareSideLength = 400;
    PuzzleTaskData puzzleTaskData = generator.generateComplexPuzzleImages(
        squareSideLength,
        gridSize,
        true,
        false,
        false
    );

    for (PuzzleImage cellImage : puzzleTaskData.images()) {
      saveBytesToFile(
          bufferedImageToBytes(cellImage.image(), "png"),
          "complex_puzzle_image_%d_%d".formatted(cellImage.address().cellX(), cellImage.address().cellY()) + ".png"
      );
      if (cellImage.address.equals(puzzleTaskData.selectedCell())) {
        PuzzleArea selectedWithMargins = generator.addRandomMargins(cellImage.image(), squareSideLength);
        System.out.println(selectedWithMargins.leftTopCorner);
        saveBytesToFile(
            bufferedImageToBytes(selectedWithMargins.image, "png"),
            "complex_puzzle_image_selected" + ".png"
        );
      }
    }
  }

  public record Point(int x, int y) {
  }

  public record PuzzleArea(Point leftTopCorner, BufferedImage image) {
  }

  public record GridCellAddress(int cellX, int cellY) {
  }

  public record PuzzleImage(GridCellAddress address, BufferedImage image) {
  }

  public record PuzzleTaskData(PuzzleImage[] images, GridCellAddress selectedCell) {
  }
}