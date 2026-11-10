package bmv.org.pushca.captcha;

import static bmv.org.pushca.captcha.DynamicCaptchaApplicationTests.fileSystemCleanup;
import static bmv.org.pushca.captcha.DynamicCaptchaApplicationTests.saveBytesToFile;
import static bmv.org.pushca.captcha.service.CaptchaGenerator.bufferedImageToBytes;
import static bmv.org.pushca.captcha.service.CaptchaGenerator.bytesToBufferedImage;
import static bmv.org.pushca.captcha.service.similarity.PuzzleImageGeneratorUtils.drawDashedRectangleOnGridImage;
import static bmv.org.pushca.captcha.service.similarity.PuzzleImageGeneratorUtils.generateRandomColor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;

import bmv.org.pushca.captcha.service.similarity.VisualSimilarityChallengeGenerator;
import bmv.org.pushca.captcha.service.similarity.PuzzleImageGeneratorTypes.BinaryPuzzlePiece;
import bmv.org.pushca.captcha.service.similarity.PuzzleImageGeneratorTypes.PuzzleChallenge;
import bmv.org.pushca.captcha.service.similarity.PuzzleImageGeneratorTypes.Rectangle;
import bmv.org.pushca.captcha.service.similarity.PuzzleImageGeneratorTypes.SimilarityChallenge;
import org.junit.jupiter.api.Test;

public class VisualSimilarityChallengeGeneratorTest {

  @Test
  void generateVisualSimilarityChallengeProducesConsistentImages() {
    VisualSimilarityChallengeGenerator generator = new VisualSimilarityChallengeGenerator();

    SimilarityChallenge challenge = generator.generateVisualSimilarityChallenge(
        100, 300, 2, generateRandomColor(), true
    );

    assertNotNull(challenge.id());
    BufferedImage grid = bytesToBufferedImage(challenge.image());
    assertNotNull(grid);
    assertPieceMatchesImage(challenge.origin());
    assertPieceMatchesImage(challenge.topMatchedPiece());
    assertPieceMatchesImage(challenge.bottomMatchedPiece());
    assertWithin(grid, challenge.topMatchedPiece().area());
    assertWithin(grid, challenge.bottomMatchedPiece().area());
    // Bottom grid is concatenated below the top one, so its match sits lower
    assertTrue(challenge.bottomMatchedPiece().area().leftTopCorner().y()
        > challenge.topMatchedPiece().area().leftTopCorner().y());
  }

  private static void assertPieceMatchesImage(BinaryPuzzlePiece piece) {
    BufferedImage image = bytesToBufferedImage(piece.image());
    assertNotNull(image);
    assertEquals(piece.area().width(), image.getWidth());
    assertEquals(piece.area().height(), image.getHeight());
  }

  private static void assertWithin(BufferedImage grid, Rectangle area) {
    assertTrue(area.leftTopCorner().x() >= 0 && area.leftTopCorner().y() >= 0);
    assertTrue(area.leftTopCorner().x() + area.width() <= grid.getWidth());
    assertTrue(area.leftTopCorner().y() + area.height() <= grid.getHeight());
  }

  static void main() {
    fileSystemCleanup();
    VisualSimilarityChallengeGenerator generator = new VisualSimilarityChallengeGenerator();
    int gridSize = 2; // Example grid size

    for (int i = 0; i < 5; i++) {
      PuzzleChallenge puzzleChallenge = generator.generateFullPuzzleChallenge(
          100,
          300,
          gridSize,
          generateRandomColor(),//Color.BLUE,
          true
      );

      SimilarityChallenge similarityChallenge = new SimilarityChallenge(
          puzzleChallenge
      );

      saveBytesToFile(
          similarityChallenge.origin().image(),
          "origin" + ".png"
      );
      saveBytesToFile(
          similarityChallenge.topMatchedPiece().image(),
          "top_origin" + ".png"
      );
      if (puzzleChallenge.topMatchedPiece().area().width() != puzzleChallenge.topMatchedPiece().image().getWidth()
          ||
          puzzleChallenge.topMatchedPiece().area().height() != puzzleChallenge.topMatchedPiece().image().getHeight()
      ) {
        throw new RuntimeException("topMatchedPiece area is not equal to image size");
      }
      saveBytesToFile(
          similarityChallenge.bottomMatchedPiece().image(),
          "bottom_origin" + ".png"
      );
      if (puzzleChallenge.bottomMatchedPiece().area().width() != puzzleChallenge.bottomMatchedPiece().image().getWidth()
          ||
          puzzleChallenge.bottomMatchedPiece().area().height() != puzzleChallenge.bottomMatchedPiece().image().getHeight()
      ) {
        throw new RuntimeException("topMatchedPiece area is not equal to image size");
      }

      saveBytesToFile(
          similarityChallenge.image(),
          "puzzle_grid" + ".png"
      );
      //===========================================================================
      BufferedImage puzzleGridWithMarkedMatches0 = drawDashedRectangleOnGridImage(
          puzzleChallenge.image(),
          puzzleChallenge.topMatchedPiece().area()
      );
      BufferedImage puzzleGridWithMarkedMatches1 = drawDashedRectangleOnGridImage(
          puzzleGridWithMarkedMatches0,
          puzzleChallenge.topMatchedPiece().area()
      );
      BufferedImage puzzleGridWithMarkedMatches2 = drawDashedRectangleOnGridImage(
          puzzleGridWithMarkedMatches1,
          puzzleChallenge.bottomMatchedPiece().area()
      );
      BufferedImage puzzleGridWithMarkedMatches3 = drawDashedRectangleOnGridImage(
          puzzleGridWithMarkedMatches2,
          puzzleChallenge.bottomMatchedPiece().area()
      );
      saveBytesToFile(
          bufferedImageToBytes(puzzleGridWithMarkedMatches3, "png"),
          "puzzle_grid_with_areas" + ".png"
      );


      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}