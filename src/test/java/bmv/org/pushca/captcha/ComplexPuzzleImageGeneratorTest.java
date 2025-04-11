package bmv.org.pushca.captcha;

import static bmv.org.pushca.captcha.DynamicCaptchaApplicationTests.fileSystemCleanup;
import static bmv.org.pushca.captcha.DynamicCaptchaApplicationTests.saveBytesToFile;
import static bmv.org.pushca.captcha.service.CaptchaGenerator.bufferedImageToBytes;
import static bmv.org.pushca.captcha.service.ComplexPuzzleImageGenerator.PuzzleTaskFullData;

import java.awt.image.BufferedImage;

import bmv.org.pushca.captcha.service.ComplexPuzzleImageGenerator;

public class ComplexPuzzleImageGeneratorTest {

  public static void main(String[] args) {
    fileSystemCleanup();
    ComplexPuzzleImageGenerator generator = new ComplexPuzzleImageGenerator();
    int gridSize = 4; // Example grid size
    int squareSideLength = 400;

    PuzzleTaskFullData fullData = generator.generatePuzzleTaskFullData(squareSideLength, gridSize, true);
    saveBytesToFile(
        bufferedImageToBytes(fullData.puzzleGridImage(), "png"),
        "full_puzzle_board" + ".png"
    );

    saveBytesToFile(
        bufferedImageToBytes(fullData.getCorrectOption().image(), "png"),
        "correct_option" + ".png"
    );
    BufferedImage gridWithSelection = generator.drawDashedRectangleOnGridImage(fullData.puzzleGridImage(), fullData.getCorrectOption().rectangle());
    fullData = new PuzzleTaskFullData(fullData.options(), fullData.target(), gridWithSelection);

    BufferedImage gridWithTwoSelections = generator.drawDashedRectangleOnGridImage(fullData.puzzleGridImage(), fullData.target().rectangle());
    saveBytesToFile(
        bufferedImageToBytes(gridWithTwoSelections, "png"),
        "puzzle_options_grid_with_selection" + ".png"
    );
  }
}