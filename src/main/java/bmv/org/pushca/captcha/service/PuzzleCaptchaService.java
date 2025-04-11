package bmv.org.pushca.captcha.service;

import static bmv.org.pushca.captcha.service.CaptchaGenerator.bufferedImageToBytes;
import static bmv.org.pushca.captcha.service.CaptchaService.ID_GENERATOR;
import static bmv.org.pushca.captcha.service.ComplexPuzzleImageGenerator.PuzzleTaskFullData;
import static bmv.org.pushca.captcha.service.ComplexPuzzleImageGenerator.Rectangle;

import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class PuzzleCaptchaService {

  private final ComplexPuzzleImageGenerator generator = new ComplexPuzzleImageGenerator();

  public PuzzleCaptchaSet generateCaptchaSet(int gridSize, int pieceSideLengthPx, boolean applyNoise) {
    return toPuzzleCaptchaSet(
        generator.generatePuzzleTaskFullData(pieceSideLengthPx, gridSize, applyNoise)
    );
  }

  public record PuzzleCaptchaSet(UUID id, byte[] puzzleImage,
                                 byte[] correctOptionImage,
                                 Rectangle targetRectangle,
                                 Rectangle correctOptionRectangle) {
  }

  public ComplexPuzzleImageGenerator getGenerator() {
    return generator;
  }

  private static PuzzleCaptchaSet toPuzzleCaptchaSet(PuzzleTaskFullData puzzleTaskFullData) {
    return new PuzzleCaptchaSet(
        ID_GENERATOR.generate(),
        bufferedImageToBytes(puzzleTaskFullData.puzzleGridImage(), "png"),
        bufferedImageToBytes(puzzleTaskFullData.getCorrectOption().image(), "png"),
        puzzleTaskFullData.target().rectangle(),
        puzzleTaskFullData.getCorrectOption().rectangle()
    );
  }
}
