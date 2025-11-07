package bmv.org.pushca.captcha.service;

import static bmv.org.pushca.captcha.util.PuzzleImageGeneratorUtils.addNoise;
import static bmv.org.pushca.captcha.util.PuzzleImageGeneratorUtils.concatenateImages;
import static bmv.org.pushca.captcha.util.PuzzleImageGeneratorUtils.generateRandomAngle;
import static bmv.org.pushca.captcha.util.PuzzleImageGeneratorUtils.generateRandomBoolean;
import static bmv.org.pushca.captcha.util.PuzzleImageGeneratorUtils.generateRandomScaleCoefficient;
import static bmv.org.pushca.captcha.util.PuzzleImageGeneratorUtils.rotateImage;
import static bmv.org.pushca.captcha.util.PuzzleImageGeneratorUtils.scaleImage;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

import bmv.org.pushca.captcha.service.PuzzleImageGeneratorTypes.ObfuscatedPuzzlePiece;
import bmv.org.pushca.captcha.service.PuzzleImageGeneratorTypes.Point;
import bmv.org.pushca.captcha.service.PuzzleImageGeneratorTypes.PuzzleChallenge;
import bmv.org.pushca.captcha.service.PuzzleImageGeneratorTypes.PuzzleGrid;
import bmv.org.pushca.captcha.service.PuzzleImageGeneratorTypes.Rectangle;
import bmv.org.pushca.captcha.service.PuzzleImageGeneratorTypes.SimplePuzzlePiece;

/**
 * Generator for creating puzzle images with complex, randomly curved shapes.
 * Each puzzle piece has 5-10 edges with randomized Bezier curves.
 * Shapes can be both convex and concave with significant irregularity for organic appearance.
 *
 * <p>Key features:
 * <ul>
 *   <li>Exactly one vertex on each border side (top, right, bottom, left)</li>
 *   <li>Remaining vertices distributed randomly with radial and tangential variations</li>
 *   <li>All edges drawn as smooth Bezier curves with random control points</li>
 * </ul>
 */
public class NewPuzzleImageGenerator {

  // Shape generation constants
  private static final int MIN_EDGES = 5;
  private static final int MAX_EDGES = 10;
  private static final double SHAPE_RADIUS_RATIO = 0.35; // Ratio of shape radius to square side
  private static final double MIN_RADIUS_VARIATION = 0.3; // Minimum radius (allows deep concave sections)
  private static final double MAX_RADIUS_VARIATION = 1.2; // Maximum radius (allows convex protrusions)
  private static final double TANGENTIAL_PERTURBATION = 0.15; // Random tangential displacement as ratio of base radius

  // Edge curve constants
  private static final int MIN_CURVE_SEGMENTS = 1;
  private static final int MAX_CURVE_SEGMENTS = 3;
  private static final double CURVE_DEVIATION_RATIO = 0.2; // Maximum curve deviation as ratio of square side
  private static final double ANGLE_VARIATION_FACTOR = 0.5; // Angular variation from regular polygon positioning

  // Bezier control point positions (as fraction along the edge)
  private static final double FIRST_CONTROL_POINT_POSITION = 0.33;
  private static final double SECOND_CONTROL_POINT_POSITION = 0.67;

  // Mathematical constants
  private static final double RIGHT_ANGLE_RADIANS = Math.PI / 2;
  private static final double FULL_CIRCLE_RADIANS = 2 * Math.PI;
  private static final double TANGENTIAL_SCALE_FACTOR = 2.0; // Factor for converting random [0,1] to [-1,1] range

  // Border constraint constants
  private static final double BORDER_MARGIN_RATIO = 0.1; // Margin from corners as ratio of side length
  private static final int MIN_BORDER_VERTICES = 4; // One vertex per side
  private static final int TOP_BORDER_INDEX = 0;
  private static final int RIGHT_BORDER_INDEX = 1;
  private static final int BOTTOM_BORDER_INDEX = 2;
  private static final int LEFT_BORDER_INDEX = 3;

  // Obfuscation margin constants
  private static final int MIN_MARGIN_SIZE = 10; // Minimum empty space on each side in pixels
  private static final double RANDOMNESS_MULTIPLIER = 1.5; // Multiplier for increased margin randomness
  private static final int MIN_TOTAL_MARGIN_FOR_BOTH_SIDES = 2 * MIN_MARGIN_SIZE; // Ensure both sides have minimum

  public PuzzleChallenge generateFullPuzzleChallenge(int originSquareSideLength, int cellSideLength, int gridSize, Color color,
                                                     boolean withNoise) {
    SimplePuzzlePiece puzzlePiece = drawComplexPuzzlePiece(originSquareSideLength,color);

    PuzzleGrid topPuzzleGrid = drawPuzzleGrid(
        puzzlePiece,
        gridSize,
        cellSideLength,
        color,
        true
    );

    PuzzleGrid bottomPuzzleGrid = drawPuzzleGrid(
        puzzlePiece,
        gridSize,
        cellSideLength,
        color,
        false
    );

    ObfuscatedPuzzlePiece bottomMatchedPiece = bottomPuzzleGrid.matchedPiece();

    BufferedImage concatenatedImages = concatenateImages(topPuzzleGrid.image(), bottomPuzzleGrid.image());
    return new PuzzleChallenge(
        topPuzzleGrid.matchedPiece(),
        new ObfuscatedPuzzlePiece(
            new SimplePuzzlePiece(
                new Rectangle(
                    new Point(
                        bottomMatchedPiece.origin().area().leftTopCorner().x(),
                        bottomMatchedPiece.origin().area().leftTopCorner().y() + topPuzzleGrid.image().getHeight()
                    ),
                    bottomMatchedPiece.origin().area().width(),
                    bottomMatchedPiece.origin().area().height()
                ),
                bottomMatchedPiece.origin().image()
            ),
            bottomMatchedPiece.rotateAngle(),
            bottomMatchedPiece.scaleCoefficient(),
            new Rectangle(
                new Point(
                    bottomMatchedPiece.area().leftTopCorner().x(),
                    bottomMatchedPiece.area().leftTopCorner().y() + topPuzzleGrid.image().getHeight()
                ),
                bottomMatchedPiece.area().width(),
                bottomMatchedPiece.area().height()
            ),
            bottomMatchedPiece.image()
        ),
        withNoise ? addNoise(concatenatedImages, color) : concatenatedImages
    );
  }

  /**
   * Generates a puzzle grid by combining a provided obfuscated puzzle piece with randomly generated pieces.
   *
   * <p>Grid Composition Algorithm:
   * <ul>
   *   <li>Creates a gridSize × gridSize matrix of puzzle pieces</li>
   *   <li>Generates gridSize² - 1 random obfuscated pieces to fill the grid</li>
   *   <li>Places the provided piece at a random cell position in the grid</li>
   *   <li>Combines all pieces into a single BufferedImage</li>
   *   <li>Updates the provided piece's origin rectangle to reflect its position in the grid</li>
   * </ul>
   *
   * <p>Origin Rectangle Update:
   * <ul>
   *   <li>Preserves original width and height from the provided piece</li>
   *   <li>Updates the top-left corner by offsetting with the grid cell position</li>
   *   <li>Formula: newX = gridCellX + originalX, newY = gridCellY + originalY</li>
   * </ul>
   *
   * @param puzzlePiece The original puzzle piece to include in the grid (must not be null)
   * @param gridSize    The grid dimensions (gridSize × gridSize cells, must be > 0)
   * @param cellSize    The size of each cell in the grid (must be > 0)
   * @param color       The color used for drawing randomly generated puzzle pieces (must not be null)
   * @param topGrid     Whether this is the top grid (true) or bottom grid (false), affects puzzle piece scaling ranges
   * @return PuzzleGrid containing the matched piece with grid cell area and combined grid image
   * @throws IllegalArgumentException if puzzlePiece is null, gridSize <= 0, cellSize <= 0, or color is null
   */
  public PuzzleGrid drawPuzzleGrid(SimplePuzzlePiece puzzlePiece, int gridSize, int cellSize, Color color, boolean topGrid) {
    ObfuscatedPuzzlePiece obfuscatedPuzzlePiece = obfuscatePuzzlePiece(puzzlePiece, cellSize, topGrid);
    validateGridInputs(obfuscatedPuzzlePiece, gridSize);

    // Get dimensions from the provided obfuscated piece
    int cellWidth = obfuscatedPuzzlePiece.image().getWidth();
    int cellHeight = obfuscatedPuzzlePiece.image().getHeight();

    // Create grid to hold all pieces
    ObfuscatedPuzzlePiece[][] grid = new ObfuscatedPuzzlePiece[gridSize][gridSize];
    // Place the provided piece at a random position
    int selectedCellX = CaptchaGenerator.RANDOM.nextInt(gridSize);
    int selectedCellY = CaptchaGenerator.RANDOM.nextInt(gridSize);
    grid[selectedCellY][selectedCellX] = obfuscatedPuzzlePiece;

    // Add generated pieces to grid
    for (int row = 0; row < gridSize; row++) {
      for (int col = 0; col < gridSize; col++) {
        if (grid[row][col] == null) {
          grid[row][col] = generateRandomPuzzlePiece(
              puzzlePiece.area().width(),
              cellSize,
              color,
              generateRandomBoolean()
          );
        }
      }
    }

    // Create combined grid image
    int gridImageWidth = gridSize * cellWidth;
    int gridImageHeight = gridSize * cellHeight;
    BufferedImage gridImage = createTransparentImage(gridImageWidth, gridImageHeight);

    // Update origin rectangle with proportional coordinates and draw pieces onto grid
    ObfuscatedPuzzlePiece matchedPieceWithUpdatedOrigin = drawPiecesOntoGrid(
        gridImage, grid, cellWidth, cellHeight, obfuscatedPuzzlePiece);

    return new PuzzleGrid(matchedPieceWithUpdatedOrigin, gridImage);
  }

  /**
   * Validates input parameters for grid generation.
   *
   * @param obfuscatedPuzzlePiece The obfuscated piece to validate
   * @param gridSize              The grid size to validate
   * @throws IllegalArgumentException if inputs are invalid
   */
  private void validateGridInputs(ObfuscatedPuzzlePiece obfuscatedPuzzlePiece, int gridSize) {
    if (obfuscatedPuzzlePiece == null || obfuscatedPuzzlePiece.image() == null) {
      throw new IllegalArgumentException("ObfuscatedPuzzlePiece and its image cannot be null");
    }

    if (gridSize <= 0) {
      throw new IllegalArgumentException("Grid size must be greater than 0");
    }
  }

  /**
   * Generates a random puzzle piece with the specified dimensions.
   *
   * <p>Process:
   * <ul>
   *   <li>Creates a new puzzle piece with a complex curved shape</li>
   *   <li>Applies obfuscation with random margins and rotation</li>
   * </ul>
   *
   * @param originSideLength The side length of the original puzzle piece before obfuscation
   * @param finalSideLength  The final side length after obfuscation (must accommodate margins)
   * @param color            The color used for drawing the puzzle piece outline
   * @param topGrid          Whether this is the top grid (true) or bottom grid (false), affects puzzle piece scaling ranges
   * @return An obfuscated puzzle piece with random rotation and margins
   */
  private ObfuscatedPuzzlePiece generateRandomPuzzlePiece(int originSideLength, int finalSideLength, Color color, boolean topGrid) {
    SimplePuzzlePiece puzzlePiece = drawComplexPuzzlePiece(originSideLength, color);
    return obfuscatePuzzlePiece(puzzlePiece, finalSideLength, topGrid);
  }

  /**
   * Draws a closed shape with randomly determined number of edges (5-10), where each edge is a random Bezier curve.
   * The curves are generated using random control points that are not derived from the endpoints,
   * ensuring truly organic and unpredictable shapes. Every shape has exactly one point on each side
   * of the square border (top, left, right, bottom) to ensure proper coverage.
   *
   * @param squareSideLength The side length of the bounding square for the puzzle piece
   * @param color            The color to use for drawing the puzzle piece outline
   * @return SimplePuzzlePiece containing the drawn puzzle piece with transparent background
   */
  public SimplePuzzlePiece drawComplexPuzzlePiece(int squareSideLength, Color color) {
    BufferedImage image = createTransparentImage(squareSideLength);
    Graphics2D graphics = null;

    try {
      graphics = image.createGraphics();
      configureGraphicsForQuality(graphics);
      graphics.setColor(color);

      int totalEdgeCount = generateRandomEdgeCount();
      Vertex[] vertices = generateVerticesWithBorderConstraints(totalEdgeCount, squareSideLength);
      Path2D shapePath = createCurvedShapePath(vertices, squareSideLength);

      graphics.draw(shapePath);

    } finally {
      if (graphics != null) {
        graphics.dispose();
      }
    }

    return new SimplePuzzlePiece(new Rectangle(new Point(0, 0), image.getWidth(), image.getHeight()), image);
  }

  /**
   * Draws all puzzle pieces onto the grid image and updates the matched piece's origin rectangle
   * with coordinates that reflect its position in the final grid.
   *
   * <p>For the matched piece, the origin rectangle is updated such that:
   * <ul>
   *   <li>Original width and height are preserved</li>
   *   <li>Top-left corner is offset by the grid cell position (drawX, drawY)</li>
   *   <li>The result represents the piece's location in the final grid image</li>
   * </ul>
   *
   * @param gridImage    The target grid image to draw on
   * @param grid         The 2D array of obfuscated pieces
   * @param cellWidth    The width of each cell
   * @param cellHeight   The height of each cell
   * @param matchedPiece The original matched piece (to track its position)
   * @return The matched piece with updated origin rectangle containing proportional coordinates
   */
  private ObfuscatedPuzzlePiece drawPiecesOntoGrid(
      BufferedImage gridImage, ObfuscatedPuzzlePiece[][] grid,
      int cellWidth, int cellHeight, ObfuscatedPuzzlePiece matchedPiece) {

    Graphics2D graphics = null;
    ObfuscatedPuzzlePiece matchedPieceWithUpdatedOrigin = null;

    try {
      graphics = gridImage.createGraphics();
      configureGraphicsForQuality(graphics);

      for (int row = 0; row < grid.length; row++) {
        for (int col = 0; col < grid[row].length; col++) {
          ObfuscatedPuzzlePiece piece = grid[row][col];
          int drawX = col * cellWidth;
          int drawY = row * cellHeight;

          // Draw the piece image onto the grid
          graphics.drawImage(piece.image(), drawX, drawY, null);

          // If this is the matched piece, update its origin rectangle with proportional coordinates
          if (piece == matchedPiece) {
            SimplePuzzlePiece updatedOrigin = updateAreaOfMatchedPuzzlePiece(piece, drawX, drawY);
            matchedPieceWithUpdatedOrigin = new ObfuscatedPuzzlePiece(
                updatedOrigin,
                piece.rotateAngle(),
                piece.scaleCoefficient(),
                new Rectangle(new Point(drawX, drawY), piece.area().width(), piece.area().height()),
                piece.image()
            );
          }
        }
      }

    } finally {
      if (graphics != null) {
        graphics.dispose();
      }
    }

    return matchedPieceWithUpdatedOrigin;
  }

  private static SimplePuzzlePiece updateAreaOfMatchedPuzzlePiece(ObfuscatedPuzzlePiece piece, int drawX, int drawY) {
    Rectangle originalRectangle = piece.origin().area();

    // Update position: offset the original top-left corner by the cell position in the grid
    int updatedLeftTopX = drawX + originalRectangle.leftTopCorner().x();
    int updatedLeftTopY = drawY + originalRectangle.leftTopCorner().y();

    // Preserve original width and height
    int updatedWidth = originalRectangle.width();
    int updatedHeight = originalRectangle.height();

    // Create the updated origin rectangle with new position but original dimensions
    Rectangle updatedRectangle = new Rectangle(
        new Point(updatedLeftTopX, updatedLeftTopY),
        updatedWidth,
        updatedHeight
    );

    return new SimplePuzzlePiece(updatedRectangle, piece.origin().image());
  }

  /**
   * Creates an obfuscated puzzle piece by applying two-stage obfuscation with increasing square sizes.
   *
   * <p>Two-Stage Process:
   * <ul>
   *   <li>Stage 1: Obfuscates the puzzle piece with a smaller square size (squareSideLength - 60)</li>
   *   <li>Stage 2: Applies obfuscation again using the full target square size with the first stage as a sampler</li>
   *   <li>Sampling ensures consistent rotation and scaling between stages for coherent results</li>
   * </ul>
   *
   * @param puzzlePiece      The puzzle piece containing the original image (must not be null)
   * @param squareSideLength The final target square side length (must be > 0)
   * @param topGrid          Whether this is the top grid (true) or bottom grid (false), affects puzzle piece scaling ranges
   * @return ObfuscatedPuzzlePiece containing the resized image and position information
   * @throws IllegalArgumentException if puzzlePiece is null or squareSideLength <= 0
   */
  public ObfuscatedPuzzlePiece obfuscatePuzzlePiece(SimplePuzzlePiece puzzlePiece, int squareSideLength, boolean topGrid) {
    ObfuscatedPuzzlePiece obfuscatedPuzzlePiece0 = obfuscatePuzzlePiece(puzzlePiece, squareSideLength - 100, topGrid, null);
    return obfuscatePuzzlePiece(
        new SimplePuzzlePiece(
            new Rectangle(new Point(0, 0), obfuscatedPuzzlePiece0.area().width(), obfuscatedPuzzlePiece0.area().height()),
            obfuscatedPuzzlePiece0.image()
        ),
        squareSideLength,
        topGrid,
        obfuscatedPuzzlePiece0
    );
  }

  /**
   * Creates an obfuscated puzzle piece by placing the original image on a larger transparent square
   * with randomly distributed margins on all sides.
   *
   * <p>Algorithm:
   * <ul>
   *   <li>Each margin (left, right, top, bottom) is guaranteed to have at least MIN_MARGIN_SIZE pixels</li>
   *   <li>Margins are randomly distributed using a randomness multiplier for increased variation</li>
   *   <li>The randomness multiplier allows non-uniform distribution, favoring larger variations</li>
   * </ul>
   *
   * <p>Constraints:
   * <ul>
   *   <li>Target square must have enough space for minimum margins on all sides</li>
   *   <li>If insufficient space, fallback to minimum margin size on each side</li>
   * </ul>
   *
   * <p>Sampling Behavior:
   * <ul>
   *   <li>If sampler is null: generates new random rotation angle and scale coefficient</li>
   *   <li>If sampler is provided: reuses sampler's rotation and scale for consistency between obfuscation stages</li>
   * </ul>
   *
   * @param puzzlePiece      The puzzle piece containing the original image (must not be null)
   * @param squareSideLength The side length of the target square (must be > 0)
   * @param topGrid          Whether this is the top grid (true) or bottom grid (false), affects puzzle piece scaling ranges
   * @param sampler          An obfuscated piece to sample rotation and scale from, or null to generate new values
   * @return ObfuscatedPuzzlePiece containing the resized image and position information
   * @throws IllegalArgumentException if puzzlePiece is null or squareSideLength <= 0
   */
  private ObfuscatedPuzzlePiece obfuscatePuzzlePiece(SimplePuzzlePiece puzzlePiece, int squareSideLength, boolean topGrid,
                                                     ObfuscatedPuzzlePiece sampler) {
    validateObfuscationInputs(puzzlePiece, squareSideLength);

    double rotationAngle;
    double scaleCoefficient;
    BufferedImage originalImage;

    if (sampler == null) {
      rotationAngle = generateRandomAngle(topGrid ? 15 : 195, 180);
      scaleCoefficient = generateRandomScaleCoefficient(1.0, generateRandomBoolean() ? 0.4 : -0.4);
      BufferedImage tmpImage = rotateImage(puzzlePiece.image(), rotationAngle);
      originalImage = scaleImage(tmpImage, scaleCoefficient);
    } else {
      rotationAngle = sampler.rotateAngle();
      scaleCoefficient = sampler.scaleCoefficient();
      originalImage = puzzlePiece.image();
    }

    ImageDimensions imageDimensions = extractImageDimensions(originalImage);

    MarginOffsets marginOffsets = calculateMarginOffsets(
        imageDimensions.width, imageDimensions.height, squareSideLength);

    BufferedImage obfuscatedImage = createObfuscatedImage(
        originalImage, squareSideLength, marginOffsets.left, marginOffsets.top);

    Rectangle originRectangle = createOriginRectangle(
        marginOffsets.left, marginOffsets.top, imageDimensions.width, imageDimensions.height);

    return new ObfuscatedPuzzlePiece(
        new SimplePuzzlePiece(originRectangle, puzzlePiece.image()),
        rotationAngle,
        scaleCoefficient,
        new Rectangle(new Point(0, 0), squareSideLength, squareSideLength),
        obfuscatedImage
    );
  }

  /**
   * Validates input parameters for obfuscation.
   *
   * @param puzzlePiece      The puzzle piece to validate
   * @param squareSideLength The target square side length to validate
   * @throws IllegalArgumentException if inputs are invalid
   */
  private void validateObfuscationInputs(SimplePuzzlePiece puzzlePiece, int squareSideLength) {
    if (puzzlePiece == null || puzzlePiece.image() == null) {
      throw new IllegalArgumentException("PuzzlePiece and its image cannot be null");
    }

    if (squareSideLength <= 0) {
      throw new IllegalArgumentException("Square side length must be greater than 0");
    }

    BufferedImage image = puzzlePiece.image();
    if (image.getWidth() <= 0 || image.getHeight() <= 0) {
      throw new IllegalArgumentException("Image dimensions must be greater than 0");
    }
  }

  /**
   * Extracts the dimensions from a buffered image.
   *
   * @param image The image to extract dimensions from
   * @return ImageDimensions containing width and height
   */
  private ImageDimensions extractImageDimensions(BufferedImage image) {
    return new ImageDimensions(image.getWidth(), image.getHeight());
  }

  /**
   * Calculates random margin offsets for positioning an image within a larger square.
   * Ensures BOTH opposite margins (left+right and top+bottom) have at least MIN_MARGIN_SIZE pixels.
   *
   * @param imageWidth       The width of the original image
   * @param imageHeight      The height of the original image
   * @param squareSideLength The side length of the target square
   * @return MarginOffsets containing left and top margins
   */
  private MarginOffsets calculateMarginOffsets(int imageWidth, int imageHeight, int squareSideLength) {
    int horizontalMarginSpace = calculateMarginSpace(imageWidth, squareSideLength);
    int verticalMarginSpace = calculateMarginSpace(imageHeight, squareSideLength);

    int leftMargin = calculateBalancedMargin(horizontalMarginSpace);
    int topMargin = calculateBalancedMargin(verticalMarginSpace);

    return new MarginOffsets(leftMargin, topMargin);
  }

  /**
   * Calculates the total available margin space between an image and target square.
   *
   * @param imageDimension   The width or height of the original image
   * @param squareSideLength The side length of the target square
   * @return The total space available for margins on both sides
   */
  private int calculateMarginSpace(int imageDimension, int squareSideLength) {
    return Math.max(0, squareSideLength - imageDimension);
  }

  /**
   * Calculates a balanced margin that ensures both sides have at least MIN_MARGIN_SIZE.
   * Splits available space between two opposite sides with randomness.
   * <p>
   * Example: totalSpace=100, minPerSide=10
   * - Space for each side: [10, 40] with random distribution
   * - Opposite side automatically gets the remaining space: [60, 90]
   * - Guarantee: Both sides >= 10
   *
   * @param totalMarginSpace The total space available for both opposite margins
   * @return The margin for the first side (e.g., left margin)
   */
  private int calculateBalancedMargin(int totalMarginSpace) {
    // Handle insufficient space for both minimum margins
    if (totalMarginSpace <= MIN_TOTAL_MARGIN_FOR_BOTH_SIDES) {
      return distributeMarginEqually(totalMarginSpace);
    }

    // Calculate randomizable space and apply randomness
    int randomizableSpace = totalMarginSpace - MIN_TOTAL_MARGIN_FOR_BOTH_SIDES;
    int randomOffset = generateRandomOffsetWithMultiplier(randomizableSpace);

    // Return: minimum + random share
    // Opposite side will get: minimum + (randomizableSpace - randomOffset)
    return MIN_MARGIN_SIZE + randomOffset;
  }

  /**
   * Distributes margin space equally between two opposite sides when space is limited.
   * Ensures graceful degradation when total space is less than required minimums.
   *
   * @param totalMarginSpace The total space available for both opposite margins
   * @return Half of the available space for each side
   */
  private int distributeMarginEqually(int totalMarginSpace) {
    return Math.max(0, totalMarginSpace / 2);
  }

  /**
   * Creates the origin rectangle containing position and size information of the original image.
   *
   * @param leftMargin  The left position of the original image
   * @param topMargin   The top position of the original image
   * @param imageWidth  The width of the original image
   * @param imageHeight The height of the original image
   * @return Rectangle with position and dimensions
   */
  private Rectangle createOriginRectangle(int leftMargin, int topMargin, int imageWidth, int imageHeight) {
    return new Rectangle(
        new Point(leftMargin, topMargin),
        imageWidth,
        imageHeight
    );
  }

  /**
   * Generates a random offset within the randomizable space using a randomness multiplier.
   * The multiplier allows the offset to potentially exceed the randomizable space,
   * which is then clamped to the valid range for increased variation in distribution.
   *
   * @param randomizableSpace The space available for random variation
   * @return A random offset between 0 and randomizableSpace (inclusive)
   */
  private int generateRandomOffsetWithMultiplier(int randomizableSpace) {
    // Generate random factor in [0.0, 1.0] range, then scale by multiplier
    double randomFactor = CaptchaGenerator.RANDOM.nextDouble() * RANDOMNESS_MULTIPLIER;
    int randomOffset = (int) (randomizableSpace * randomFactor);

    // Clamp to valid range [0, randomizableSpace]
    return Math.min(randomOffset, randomizableSpace);
  }

  /**
   * Creates an obfuscated image by placing the original image on a larger transparent background.
   *
   * @param originalImage The original puzzle piece image
   * @param targetSize    The target size for the new image (targetSize x targetSize)
   * @param leftMargin    The left margin (X position of original image)
   * @param topMargin     The top margin (Y position of original image)
   * @return A new BufferedImage with the original image positioned at the calculated location
   */
  private BufferedImage createObfuscatedImage(BufferedImage originalImage, int targetSize, int leftMargin, int topMargin) {
    BufferedImage obfuscatedImage = createTransparentImage(targetSize);
    Graphics2D graphics = null;

    try {
      graphics = obfuscatedImage.createGraphics();
      configureGraphicsForQuality(graphics);

      // Draw the original image at the calculated position
      graphics.drawImage(originalImage, leftMargin, topMargin, null);

    } finally {
      if (graphics != null) {
        graphics.dispose();
      }
    }

    return obfuscatedImage;
  }

  /**
   * Creates a square transparent BufferedImage with the specified side length.
   * Convenience overload that delegates to {@link #createTransparentImage(int, int)}.
   *
   * @param sideLength The width and height of the square image in pixels
   * @return A new square BufferedImage with fully transparent background (ARGB)
   */
  private BufferedImage createTransparentImage(int sideLength) {
    return createTransparentImage(sideLength, sideLength);
  }

  /**
   * Creates a rectangular transparent BufferedImage with the specified dimensions.
   * The image uses ARGB color model and is initialized with full transparency.
   *
   * @param width  The width of the image in pixels
   * @param height The height of the image in pixels
   * @return A new BufferedImage with fully transparent background (ARGB)
   */
  private BufferedImage createTransparentImage(int width, int height) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = image.createGraphics();
    try {
      graphics.setComposite(AlphaComposite.Clear);
      graphics.fillRect(0, 0, width, height);
      graphics.setComposite(AlphaComposite.Src);
    } finally {
      graphics.dispose();
    }
    return image;
  }

  /**
   * Configures Graphics2D object with high-quality rendering hints.
   *
   * @param graphics The Graphics2D object to configure
   */
  private void configureGraphicsForQuality(Graphics2D graphics) {
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
  }

  /**
   * Generates a random number of edges between MIN_EDGES and MAX_EDGES (inclusive).
   *
   * @return A random integer between MIN_EDGES and MAX_EDGES (inclusive)
   */
  private int generateRandomEdgeCount() {
    return MIN_EDGES + CaptchaGenerator.RANDOM.nextInt(MAX_EDGES - MIN_EDGES + 1);
  }

  /**
   * Generates vertices with the constraint that exactly one vertex must be on each side
   * of the square border (top, left, right, bottom). The remaining vertices are positioned
   * randomly with radial and tangential variations.
   *
   * @param totalEdgeCount   The total number of vertices to generate (must be at least 4)
   * @param squareSideLength The side length of the bounding square
   * @return Array of vertices sorted in angular order for proper shape closure
   */
  private Vertex[] generateVerticesWithBorderConstraints(int totalEdgeCount, int squareSideLength) {
    validateMinimumEdgeCount(totalEdgeCount);

    Vertex[] borderVertices = generateBorderVertices(squareSideLength);
    int interiorVertexCount = calculateInteriorVertexCount(totalEdgeCount);
    Vertex[] interiorVertices = generateInteriorVertices(interiorVertexCount, squareSideLength);

    Vertex[] allVertices = combineVertices(borderVertices, interiorVertices);

    return sortVerticesByAngle(allVertices, squareSideLength);
  }

  /**
   * Validates that the edge count meets the minimum requirement for border constraints.
   *
   * @param totalEdgeCount The total number of edges to validate
   * @throws IllegalArgumentException if edge count is less than MIN_BORDER_VERTICES
   */
  private void validateMinimumEdgeCount(int totalEdgeCount) {
    if (totalEdgeCount < MIN_BORDER_VERTICES) {
      throw new IllegalArgumentException(
          "Edge count must be at least " + MIN_BORDER_VERTICES + " to have one point on each border");
    }
  }

  /**
   * Calculates the number of interior vertices needed.
   *
   * @param totalEdgeCount The total number of edges
   * @return The number of interior vertices (total - border vertices)
   */
  private int calculateInteriorVertexCount(int totalEdgeCount) {
    return totalEdgeCount - MIN_BORDER_VERTICES;
  }

  /**
   * Combines border and interior vertices into a single array.
   *
   * @param borderVertices   The vertices on the borders
   * @param interiorVertices The vertices in the interior
   * @return Combined array of all vertices
   */
  private Vertex[] combineVertices(Vertex[] borderVertices, Vertex[] interiorVertices) {
    Vertex[] allVertices = new Vertex[borderVertices.length + interiorVertices.length];
    System.arraycopy(borderVertices, 0, allVertices, 0, borderVertices.length);
    System.arraycopy(interiorVertices, 0, allVertices, borderVertices.length, interiorVertices.length);
    return allVertices;
  }

  /**
   * Generates exactly 4 vertices, one on each side of the square border.
   * Positions are randomized along each side with some margin from corners.
   *
   * @param squareSideLength The side length of the bounding square
   * @return Array of 4 vertices on the borders (top, right, bottom, left order)
   */
  private Vertex[] generateBorderVertices(int squareSideLength) {
    double margin = squareSideLength * BORDER_MARGIN_RATIO;
    double availableRange = squareSideLength - 2 * margin;

    Vertex[] borderVertices = new Vertex[MIN_BORDER_VERTICES];

    // Top side (y = 0)
    borderVertices[TOP_BORDER_INDEX] = createBorderVertex(
        margin, availableRange, squareSideLength, true, false);

    // Right side (x = squareSideLength)
    borderVertices[RIGHT_BORDER_INDEX] = createBorderVertex(
        margin, availableRange, squareSideLength, false, false);

    // Bottom side (y = squareSideLength)
    borderVertices[BOTTOM_BORDER_INDEX] = createBorderVertex(
        margin, availableRange, squareSideLength, true, true);

    // Left side (x = 0)
    borderVertices[LEFT_BORDER_INDEX] = createBorderVertex(
        margin, availableRange, squareSideLength, false, true);

    return borderVertices;
  }

  /**
   * Creates a single border vertex on a specific side of the square.
   *
   * @param margin           The margin distance from corners
   * @param availableRange   The available range for random positioning
   * @param squareSideLength The side length of the square
   * @param isHorizontal     True for top/bottom sides, false for left/right sides
   * @param isMaxEdge        True for bottom/right edges (at squareSideLength), false for top/left (at 0)
   * @return A vertex positioned on the specified border
   */
  private Vertex createBorderVertex(double margin, double availableRange, int squareSideLength,
                                    boolean isHorizontal, boolean isMaxEdge) {
    double randomPosition = margin + CaptchaGenerator.RANDOM.nextDouble() * availableRange;
    double fixedPosition = isMaxEdge ? squareSideLength : 0;

    return isHorizontal
        ? new Vertex(randomPosition, fixedPosition)
        : new Vertex(fixedPosition, randomPosition);
  }

  /**
   * Generates interior vertices positioned randomly within the square,
   * using the original circular distribution with variations.
   *
   * @param count            The number of interior vertices to generate
   * @param squareSideLength The side length of the bounding square
   * @return Array of interior vertices
   */
  private Vertex[] generateInteriorVertices(int count, int squareSideLength) {
    if (count == 0) {
      return new Vertex[0];
    }

    double[] angles = generateRandomAngles(count);
    Vertex center = calculateCenter(squareSideLength);
    double baseRadius = calculateBaseRadius(squareSideLength);

    Vertex[] vertices = new Vertex[count];
    for (int i = 0; i < count; i++) {
      vertices[i] = createRandomVertex(center, baseRadius, angles[i]);
    }

    return vertices;
  }

  /**
   * Sorts vertices by their angle from the center point to ensure proper
   * clockwise or counterclockwise ordering for shape drawing.
   *
   * @param vertices         The vertices to sort
   * @param squareSideLength The side length of the square
   * @return Sorted array of vertices in angular order
   */
  private Vertex[] sortVerticesByAngle(Vertex[] vertices, int squareSideLength) {
    Vertex center = calculateCenter(squareSideLength);

    return java.util.Arrays.stream(vertices)
        .sorted((v1, v2) -> compareByAngle(v1, v2, center))
        .toArray(Vertex[]::new);
  }

  /**
   * Compares two vertices by their angle from a center point.
   *
   * @param v1     First vertex
   * @param v2     Second vertex
   * @param center The center point for angle calculation
   * @return Negative if v1 has smaller angle, positive if v2 has smaller angle, 0 if equal
   */
  private int compareByAngle(Vertex v1, Vertex v2, Vertex center) {
    double angle1 = calculateAngleFromCenter(v1, center);
    double angle2 = calculateAngleFromCenter(v2, center);
    return Double.compare(angle1, angle2);
  }

  /**
   * Calculates the angle of a vertex from a center point using atan2.
   *
   * @param vertex The vertex to calculate angle for
   * @param center The center point
   * @return The angle in radians
   */
  private double calculateAngleFromCenter(Vertex vertex, Vertex center) {
    return Math.atan2(vertex.y - center.y, vertex.x - center.x);
  }

  /**
   * Creates a single random vertex with radial and tangential variations.
   *
   * @param center     The center point of the shape
   * @param baseRadius The base radius for the shape
   * @param angle      The angular position for this vertex
   * @return A vertex with randomized position
   */
  private Vertex createRandomVertex(Vertex center, double baseRadius, double angle) {
    double randomizedRadius = calculateRandomizedRadius(baseRadius);
    double tangentialOffset = calculateTangentialOffset(baseRadius);

    // Calculate base position with radial variation
    double baseX = center.x + randomizedRadius * Math.cos(angle);
    double baseY = center.y + randomizedRadius * Math.sin(angle);

    // Add tangential perturbation (perpendicular to radial direction)
    double tangentAngle = angle + RIGHT_ANGLE_RADIANS;
    double x = baseX + tangentialOffset * Math.cos(tangentAngle);
    double y = baseY + tangentialOffset * Math.sin(tangentAngle);

    return new Vertex(x, y);
  }

  /**
   * Calculates the base radius for shape generation.
   *
   * @param squareSideLength The side length of the square
   * @return The base radius as a fraction of the square side length
   */
  private double calculateBaseRadius(int squareSideLength) {
    return squareSideLength * SHAPE_RADIUS_RATIO;
  }

  /**
   * Calculates the center point of the square.
   *
   * @param squareSideLength The side length of the square
   * @return A Vertex representing the center point
   */
  private Vertex calculateCenter(int squareSideLength) {
    return new Vertex(squareSideLength / 2.0, squareSideLength / 2.0);
  }

  /**
   * Calculates a randomized radius with variation between MIN_RADIUS_VARIATION and MAX_RADIUS_VARIATION
   * of the base radius. This allows vertices to be both closer (concave) and farther (convex) from center.
   *
   * @param baseRadius The base radius to apply variation to
   * @return A randomized radius value
   */
  private double calculateRandomizedRadius(double baseRadius) {
    double radiusRange = MAX_RADIUS_VARIATION - MIN_RADIUS_VARIATION;
    double randomFactor = MIN_RADIUS_VARIATION + CaptchaGenerator.RANDOM.nextDouble() * radiusRange;
    return baseRadius * randomFactor;
  }

  /**
   * Calculates a random tangential offset to perturb vertices perpendicular to the radial direction.
   * This creates irregular shapes by shifting vertices along the circle's tangent.
   *
   * @param baseRadius The base radius used to scale the perturbation
   * @return A random offset value (can be positive or negative)
   */
  private double calculateTangentialOffset(double baseRadius) {
    return generateRandomDeviation(TANGENTIAL_SCALE_FACTOR * baseRadius * TANGENTIAL_PERTURBATION);
  }

  /**
   * Generates random angles for vertex positioning. Each angle is based on regular polygon
   * positioning with random variation to create irregular shapes.
   *
   * @param edgeCount The number of angles to generate
   * @return Array of angles in radians
   */
  private double[] generateRandomAngles(int edgeCount) {
    double[] angles = new double[edgeCount];
    double angleStep = FULL_CIRCLE_RADIANS / edgeCount;

    for (int i = 0; i < edgeCount; i++) {
      double baseAngle = i * angleStep;
      double variation = generateRandomDeviation(angleStep * ANGLE_VARIATION_FACTOR);
      angles[i] = baseAngle + variation;
    }

    return angles;
  }

  /**
   * Creates a closed Path2D shape with curved edges connecting all vertices.
   *
   * @param vertices         The vertices that define the corners of the shape
   * @param squareSideLength The side length used for calculating curve deviation
   * @return A closed Path2D object representing the curved shape
   */
  private Path2D createCurvedShapePath(Vertex[] vertices, int squareSideLength) {
    Path2D path = new Path2D.Double();
    path.moveTo(vertices[0].x, vertices[0].y);

    for (int i = 0; i < vertices.length; i++) {
      Vertex currentVertex = vertices[i];
      Vertex nextVertex = vertices[(i + 1) % vertices.length];
      addCurvedEdgeToPath(path, currentVertex, nextVertex, squareSideLength);
    }

    path.closePath();
    return path;
  }

  /**
   * Adds a randomly curved edge between two vertices to the path.
   * The curve consists of 1-3 cubic Bezier curve segments with randomly positioned control points.
   *
   * @param path             The Path2D object to add the curved edge to
   * @param start            The starting vertex of the edge
   * @param end              The ending vertex of the edge
   * @param squareSideLength The side length used for calculating curve deviation
   */
  private void addCurvedEdgeToPath(Path2D path, Vertex start, Vertex end, int squareSideLength) {
    int segmentCount = MIN_CURVE_SEGMENTS + CaptchaGenerator.RANDOM.nextInt(MAX_CURVE_SEGMENTS - MIN_CURVE_SEGMENTS + 1);
    double maxDeviation = squareSideLength * CURVE_DEVIATION_RATIO;

    double currentX = start.x;
    double currentY = start.y;
    double deltaX = (end.x - start.x) / segmentCount;
    double deltaY = (end.y - start.y) / segmentCount;

    for (int i = 0; i < segmentCount; i++) {
      boolean isLastSegment = (i == segmentCount - 1);
      double segmentEndX = isLastSegment ? end.x : start.x + (i + 1) * deltaX;
      double segmentEndY = isLastSegment ? end.y : start.y + (i + 1) * deltaY;

      BezierControlPoints controlPoints = generateRandomControlPoints(
          currentX, currentY, deltaX, deltaY, maxDeviation
      );

      path.curveTo(
          controlPoints.x1, controlPoints.y1,
          controlPoints.x2, controlPoints.y2,
          segmentEndX, segmentEndY
      );

      currentX = segmentEndX;
      currentY = segmentEndY;
    }
  }

  /**
   * Generates random control points for a cubic Bezier curve.
   * Control points are positioned with random deviation to create organic curves.
   *
   * @param currentX     The current X position along the edge
   * @param currentY     The current Y position along the edge
   * @param deltaX       The X distance of the segment
   * @param deltaY       The Y distance of the segment
   * @param maxDeviation The maximum deviation allowed for control points
   * @return BezierControlPoints containing the two control points
   */
  private BezierControlPoints generateRandomControlPoints(
      double currentX, double currentY, double deltaX, double deltaY, double maxDeviation) {

    double controlX1 = currentX + deltaX * FIRST_CONTROL_POINT_POSITION
                       + generateRandomDeviation(maxDeviation);
    double controlY1 = currentY + deltaY * FIRST_CONTROL_POINT_POSITION
                       + generateRandomDeviation(maxDeviation);

    double controlX2 = currentX + deltaX * SECOND_CONTROL_POINT_POSITION
                       + generateRandomDeviation(maxDeviation);
    double controlY2 = currentY + deltaY * SECOND_CONTROL_POINT_POSITION
                       + generateRandomDeviation(maxDeviation);

    return new BezierControlPoints(controlX1, controlY1, controlX2, controlY2);
  }

  /**
   * Generates a random deviation value centered around zero.
   *
   * @param maxDeviation The maximum absolute deviation
   * @return A random value between -maxDeviation/2 and +maxDeviation/2
   */
  private double generateRandomDeviation(double maxDeviation) {
    return (CaptchaGenerator.RANDOM.nextDouble() - 0.5) * maxDeviation;
  }

  /**
   * Represents a vertex in 2D space.
   *
   * @param x The X coordinate
   * @param y The Y coordinate
   */
  private record Vertex(double x, double y) {
  }

  /**
   * Represents the two control points needed for a cubic Bezier curve.
   *
   * @param x1 The X coordinate of the first control point
   * @param y1 The Y coordinate of the first control point
   * @param x2 The X coordinate of the second control point
   * @param y2 The Y coordinate of the second control point
   */
  private record BezierControlPoints(double x1, double y1, double x2, double y2) {
  }

  /**
   * Represents the dimensions of an image (width and height).
   *
   * @param width  The width of the image in pixels
   * @param height The height of the image in pixels
   */
  private record ImageDimensions(int width, int height) {
  }

  /**
   * Represents the margin offsets for positioning an image within a larger square.
   *
   * @param left The left margin (X offset from the left edge)
   * @param top  The top margin (Y offset from the top edge)
   */
  private record MarginOffsets(int left, int top) {
  }
}
