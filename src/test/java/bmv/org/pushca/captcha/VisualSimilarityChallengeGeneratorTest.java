package bmv.org.pushca.captcha;

import bmv.org.pushca.captcha.service.similarity.PuzzleImageGeneratorTypes.PuzzleChallenge;
import bmv.org.pushca.captcha.service.similarity.PuzzleImageGeneratorTypes.SimilarityChallenge;
import bmv.org.pushca.captcha.service.similarity.VisualSimilarityChallengeGenerator;

import static bmv.org.pushca.captcha.DynamicCaptchaApplicationTests.fileSystemCleanup;
import static bmv.org.pushca.captcha.DynamicCaptchaApplicationTests.saveBytesToFile;
import static bmv.org.pushca.captcha.service.CaptchaGenerator.bufferedImageToBytes;
import static bmv.org.pushca.captcha.service.similarity.PuzzleImageGeneratorUtils.drawDashedRectangleOnGridImage;
import static bmv.org.pushca.captcha.service.similarity.PuzzleImageGeneratorUtils.generateRandomColor;

import java.awt.image.BufferedImage;

public class VisualSimilarityChallengeGeneratorTest {

    public static void main(String[] args) {
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