package bmv.org.pushca.captcha.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bmv.org.pushca.captcha.service.similarity.PuzzleImageGeneratorTypes.PuzzleArea;
import bmv.org.pushca.captcha.service.similarity.PuzzleImageGeneratorTypes.PuzzleTaskFullData;
import org.junit.jupiter.api.Test;

class PuzzleCaptchaServiceTest {

  private final PuzzleCaptchaService service = new PuzzleCaptchaService();

  @Test
  void getGeneratorReturnsSameInstance() {
    ComplexPuzzleImageGenerator generator = service.getGenerator();

    assertNotNull(generator);
    assertSame(generator, service.getGenerator());
  }

  @Test
  void getGeneratorProducesPuzzleTaskWithSingleCorrectOption() {
    PuzzleTaskFullData data = service.getGenerator().generatePuzzleTaskFullData(200, 3, false);

    assertNotNull(data.puzzleGridImage());
    assertNotNull(data.target());
    long selected = 0;
    for (PuzzleArea option : data.options()) {
      if (option.selected()) {
        selected++;
      }
    }
    assertEquals(1, selected);
    assertTrue(data.getCorrectOption().selected());
  }
}
