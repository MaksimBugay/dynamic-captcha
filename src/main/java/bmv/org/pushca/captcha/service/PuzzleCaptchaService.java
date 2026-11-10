package bmv.org.pushca.captcha.service;

import static bmv.org.pushca.captcha.service.CaptchaGenerator.bufferedImageToBytes;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import java.util.UUID;

import bmv.org.pushca.captcha.service.similarity.PuzzleImageGeneratorTypes.PuzzleTaskFullData;
import bmv.org.pushca.captcha.service.similarity.PuzzleImageGeneratorTypes.Rectangle;
import org.springframework.stereotype.Service;

@Service
public class PuzzleCaptchaService {

  public static final TimeBasedEpochGenerator ID_GENERATOR = Generators.timeBasedEpochGenerator();

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
