package bmv.org.pushca.captcha;

import static bmv.org.pushca.captcha.DynamicCaptchaApplicationTests.fileSystemCleanup;
import static bmv.org.pushca.captcha.DynamicCaptchaApplicationTests.saveBytesToFile;
import static bmv.org.pushca.captcha.service.CaptchaGenerator.bufferedImageToBytes;
import static bmv.org.pushca.captcha.util.PuzzleImageGeneratorUtils.drawDashedRectangleOnGridImage;

import java.awt.Color;
import java.awt.image.BufferedImage;

import bmv.org.pushca.captcha.service.ComplexPuzzleImageGenerator;
import bmv.org.pushca.captcha.service.NewPuzzleImageGenerator;
import bmv.org.pushca.captcha.service.PuzzleImageGeneratorTypes;
import bmv.org.pushca.captcha.service.PuzzleImageGeneratorTypes.ObfuscatedPuzzlePiece;
import bmv.org.pushca.captcha.service.PuzzleImageGeneratorTypes.Point;
import bmv.org.pushca.captcha.service.PuzzleImageGeneratorTypes.PuzzleGrid;
import bmv.org.pushca.captcha.service.PuzzleImageGeneratorTypes.PuzzlePiece;
import bmv.org.pushca.captcha.service.PuzzleImageGeneratorTypes.PuzzleTaskFullData;
import bmv.org.pushca.captcha.service.PuzzleImageGeneratorTypes.Rectangle;
import bmv.org.pushca.captcha.service.PuzzleImageGeneratorTypes.SimplePuzzlePiece;

public class ComplexPuzzleImageGeneratorTest {

  public static void main(String[] args) {
    fileSystemCleanup();
    //ComplexPuzzleImageGenerator generator = new ComplexPuzzleImageGenerator();
    NewPuzzleImageGenerator generator = new NewPuzzleImageGenerator();
    int gridSize = 3; // Example grid size
    int squareSideLength = 400;

    for (int i = 0; i < 10; i++) {
      SimplePuzzlePiece puzzlePiece = generator.drawComplexPuzzlePiece(90, Color.BLUE);

      saveBytesToFile(
          bufferedImageToBytes(puzzlePiece.image(), "png"),
          "full_puzzle_board" + ".png"
      );

      /*BufferedImage puzzlePieceWithMarkedOrigin = drawDashedRectangleOnGridImage(obfuscatedPuzzlePiece.image(), obfuscatedPuzzlePiece.origin().area());
      BufferedImage puzzlePieceWithMarkedOriginWithBorders = drawDashedRectangleOnGridImage(
          puzzlePieceWithMarkedOrigin, new Rectangle(new Point(0, 0), obfuscatedPuzzlePiece.image().getWidth(), obfuscatedPuzzlePiece.image().getHeight()));
      saveBytesToFile(
          bufferedImageToBytes(puzzlePieceWithMarkedOriginWithBorders, "png"),
          "puzzle_piece_with_selection" + ".png"
      );*/

      PuzzleGrid puzzleGrid = generator.drawPuzzleGrid(
          puzzlePiece,
          3,
          240,
          Color.BLUE
      );
      BufferedImage puzzleGridWithMarkedOrigin = drawDashedRectangleOnGridImage(
          puzzleGrid.image(),
          puzzleGrid.matchedPiece().origin().area()
      );
      BufferedImage puzzleGridWithMarkedOrigin1 = drawDashedRectangleOnGridImage(
          puzzleGridWithMarkedOrigin,
          puzzleGrid.matchedPiece().area()
      );
      saveBytesToFile(
          bufferedImageToBytes(puzzleGridWithMarkedOrigin1, "png"),
          "puzzle_grid" + ".png"
      );
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    if(true) return;

    PuzzleTaskFullData fullData = generator.generatePuzzleTaskFullData(squareSideLength, gridSize, true);
    saveBytesToFile(
        bufferedImageToBytes(fullData.puzzleGridImage(), "png"),
        "full_puzzle_board" + ".png"
    );

    /*saveBytesToFile(
        bufferedImageToBytes(fullData.getCorrectOption().image(), "png"),
        "correct_option" + ".png"
    );*/

    /*BufferedImage gridWithSelection = generator.drawDashedRectangleOnGridImage(fullData.puzzleGridImage(), fullData.getCorrectOption().rectangle());
    fullData = new PuzzleTaskFullData(fullData.options(), fullData.target(), gridWithSelection, fullData.scaleCoefficient());

    BufferedImage gridWithTwoSelections = generator.drawDashedRectangleOnGridImage(fullData.puzzleGridImage(), fullData.target().rectangle());
    saveBytesToFile(
        bufferedImageToBytes(gridWithTwoSelections, "png"),
        "puzzle_options_grid_with_selection" + ".png"
    );*/
  }
}