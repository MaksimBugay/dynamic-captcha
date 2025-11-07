package bmv.org.pushca.captcha;

import static bmv.org.pushca.captcha.DynamicCaptchaApplicationTests.fileSystemCleanup;
import static bmv.org.pushca.captcha.DynamicCaptchaApplicationTests.saveBytesToFile;
import static bmv.org.pushca.captcha.service.CaptchaGenerator.bufferedImageToBytes;
import static bmv.org.pushca.captcha.util.PuzzleImageGeneratorUtils.drawDashedRectangleOnGridImage;

import java.awt.Color;
import java.awt.image.BufferedImage;

import bmv.org.pushca.captcha.service.NewPuzzleImageGenerator;
import bmv.org.pushca.captcha.service.PuzzleImageGeneratorTypes;
import bmv.org.pushca.captcha.service.PuzzleImageGeneratorTypes.PuzzleGrid;
import bmv.org.pushca.captcha.service.PuzzleImageGeneratorTypes.SimplePuzzlePiece;

public class ComplexPuzzleImageGeneratorTest {

  public static void main(String[] args) {
    fileSystemCleanup();
    //ComplexPuzzleImageGenerator generator = new ComplexPuzzleImageGenerator();
    NewPuzzleImageGenerator generator = new NewPuzzleImageGenerator();
    int gridSize = 2; // Example grid size
    boolean withSectors = true;
    //int squareSideLength = 400;

    for (int i = 0; i < 3; i++) {
      PuzzleImageGeneratorTypes.PuzzleChallenge puzzleChallenge = generator.generateFullPuzzleChallenge(
          90,
          200,
          gridSize,
          Color.BLUE
      );

      //SimplePuzzlePiece puzzlePiece = generator.drawComplexPuzzlePiece(90, Color.BLUE);

      saveBytesToFile(
          bufferedImageToBytes(puzzleChallenge.topMatchedPiece().origin().image(), "png"),
          "full_puzzle_board" + ".png"
      );

      /*BufferedImage puzzlePieceWithMarkedOrigin = drawDashedRectangleOnGridImage(obfuscatedPuzzlePiece.image(), obfuscatedPuzzlePiece.origin().area());
      BufferedImage puzzlePieceWithMarkedOriginWithBorders = drawDashedRectangleOnGridImage(
          puzzlePieceWithMarkedOrigin, new Rectangle(new Point(0, 0), obfuscatedPuzzlePiece.image().getWidth(), obfuscatedPuzzlePiece.image().getHeight()));
      saveBytesToFile(
          bufferedImageToBytes(puzzlePieceWithMarkedOriginWithBorders, "png"),
          "puzzle_piece_with_selection" + ".png"
      );*/

      /*PuzzleGrid puzzleGrid1 = generator.drawPuzzleGrid(
          puzzlePiece,
          gridSize,
          200,
          Color.BLUE,
          true
      );
      System.out.println(puzzleGrid1.matchedPiece().scaleCoefficient());
      System.out.println(puzzleGrid1.matchedPiece().rotateAngle());*/
      if (withSectors) {
        BufferedImage puzzleGridWithMarkedMatches0 = drawDashedRectangleOnGridImage(
            puzzleChallenge.image(),
            puzzleChallenge.topMatchedPiece().origin().area()
        );
        BufferedImage puzzleGridWithMarkedMatches1 = drawDashedRectangleOnGridImage(
            puzzleGridWithMarkedMatches0,
            puzzleChallenge.topMatchedPiece().area()
        );
        BufferedImage puzzleGridWithMarkedMatches2 = drawDashedRectangleOnGridImage(
            puzzleGridWithMarkedMatches1,
            puzzleChallenge.bottomMatchedPiece().origin().area()
        );
        BufferedImage puzzleGridWithMarkedMatches3 = drawDashedRectangleOnGridImage(
            puzzleGridWithMarkedMatches2,
            puzzleChallenge.bottomMatchedPiece().area()
        );
        saveBytesToFile(
            bufferedImageToBytes(puzzleGridWithMarkedMatches3, "png"),
            "puzzle_grid1" + ".png"
        );
      } else {
        saveBytesToFile(
            bufferedImageToBytes(puzzleChallenge.image(), "png"),
            "puzzle_grid1" + ".png"
        );
      }

      /*PuzzleGrid puzzleGrid2 = generator.drawPuzzleGrid(
          puzzlePiece,
          gridSize,
          200,
          Color.BLUE,
          false
      );
      System.out.println(puzzleGrid2.matchedPiece().scaleCoefficient());
      System.out.println(puzzleGrid2.matchedPiece().rotateAngle());
      if (withSectors) {
        BufferedImage puzzleGridWithMarkedOrigin2 = drawDashedRectangleOnGridImage(
            puzzleGrid2.image(),
            puzzleGrid2.matchedPiece().origin().area()
        );
        BufferedImage puzzleGridWithMarkedOrigin2a = drawDashedRectangleOnGridImage(
            puzzleGridWithMarkedOrigin2,
            puzzleGrid2.matchedPiece().area()
        );
        saveBytesToFile(
            bufferedImageToBytes(puzzleGridWithMarkedOrigin2a, "png"),
            "puzzle_grid2" + ".png"
        );
      } else {
        saveBytesToFile(
            bufferedImageToBytes(puzzleGrid2.image(), "png"),
            "puzzle_grid2" + ".png"
        );
      }*/
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    /*PuzzleTaskFullData fullData = generator.generatePuzzleTaskFullData(squareSideLength, gridSize, true);
    saveBytesToFile(
        bufferedImageToBytes(fullData.puzzleGridImage(), "png"),
        "full_puzzle_board" + ".png"
    );*/

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