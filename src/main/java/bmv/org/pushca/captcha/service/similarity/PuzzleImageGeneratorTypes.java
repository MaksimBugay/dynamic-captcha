package bmv.org.pushca.captcha.service.similarity;

import java.awt.image.BufferedImage;

import static bmv.org.pushca.captcha.service.similarity.PuzzleImageGeneratorUtils.bufferedImageToBytes;

public class PuzzleImageGeneratorTypes {
    public record Point(int x, int y) {
    }

    public record Rectangle(Point leftTopCorner, int width, int height) {
    }

    public record SimplePuzzlePiece(Rectangle area, BufferedImage image) {
    }

    public record BinaryPuzzlePiece(Rectangle area, byte[] image) {
        public BinaryPuzzlePiece(SimplePuzzlePiece simplePuzzlePiece) {
            this(
                    simplePuzzlePiece.area(),
                    bufferedImageToBytes(simplePuzzlePiece.image(), "png")
            );
        }
    }

    public record ObfuscatedPuzzlePiece(SimplePuzzlePiece origin, Double rotateAngle, Double scaleCoefficient,
                                        Rectangle area,
                                        BufferedImage image) {
    }

    public record PuzzleGrid(ObfuscatedPuzzlePiece matchedPiece, BufferedImage image) {

    }

    public record ComposedPuzzleGrid(ObfuscatedPuzzlePiece topMatchedPiece, ObfuscatedPuzzlePiece bottomMatchedPiece,
                                     BufferedImage image) {
    }

    public record PuzzleChallenge(SimplePuzzlePiece origin,
                                  SimplePuzzlePiece topMatchedPiece,
                                  SimplePuzzlePiece bottomMatchedPiece,
                                  BufferedImage image) {
        public PuzzleChallenge(ComposedPuzzleGrid composedPuzzleGrid) {
            this(
                    composedPuzzleGrid.topMatchedPiece().origin(),
                    new SimplePuzzlePiece(
                            composedPuzzleGrid.topMatchedPiece().origin().area(),
                            composedPuzzleGrid.topMatchedPiece().origin().image()
                    ),
                    new SimplePuzzlePiece(
                            composedPuzzleGrid.bottomMatchedPiece().origin().area(),
                            composedPuzzleGrid.bottomMatchedPiece().origin().image()
                    ),
                    composedPuzzleGrid.image
            );
        }
    }

    public record SimilarityChallenge(BinaryPuzzlePiece origin,
                                      BinaryPuzzlePiece topMatchedPiece,
                                      BinaryPuzzlePiece bottomMatchedPiece,
                                      byte[] image) {
        public SimilarityChallenge(PuzzleChallenge puzzleChallenge) {
            this(
                    new BinaryPuzzlePiece(puzzleChallenge.origin()),
                    new BinaryPuzzlePiece(puzzleChallenge.topMatchedPiece()),
                    new BinaryPuzzlePiece(puzzleChallenge.bottomMatchedPiece()),
                    bufferedImageToBytes(puzzleChallenge.image(), "png")
            );
        }
    }

}
