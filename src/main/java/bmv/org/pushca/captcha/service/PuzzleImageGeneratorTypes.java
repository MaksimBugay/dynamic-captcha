package bmv.org.pushca.captcha.service;

import static bmv.org.pushca.captcha.util.PuzzleImageGeneratorUtils.scaleBufferedImage;

import java.awt.image.BufferedImage;
import java.util.Arrays;

public class PuzzleImageGeneratorTypes {
  public record Point(int x, int y) {
  }

  public record PuzzlePiece(Point leftTopCorner, BufferedImage image) {
  }

  public record Rectangle(Point leftTopCorner, int width, int height) {
  }

  public record SimplePuzzlePiece(Rectangle area, BufferedImage image) {
  }

  public record ObfuscatedPuzzlePiece(SimplePuzzlePiece origin, Double rotateAngle, Double scaleCoefficient, Rectangle area,
                                      BufferedImage image) {
  }

  public record PuzzleGrid(ObfuscatedPuzzlePiece matchedPiece, BufferedImage image) {

  }

  public record PuzzleChallenge(ObfuscatedPuzzlePiece topMatchedPiece, ObfuscatedPuzzlePiece bottomMatchedPiece, BufferedImage image) {
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

}
