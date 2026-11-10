package bmv.org.pushca.captcha.controller;

import static bmv.org.pushca.captcha.service.PuzzleCaptchaService.PuzzleCaptchaSet;

import bmv.org.pushca.captcha.service.PuzzleCaptchaService;
import bmv.org.pushca.captcha.service.similarity.PuzzleImageGeneratorTypes.SimilarityChallenge;
import bmv.org.pushca.captcha.service.similarity.VisualSimilarityCaptchaService;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@RestController
public class CaptchaApiController {

  private final PuzzleCaptchaService puzzleCaptchaService;

  private final VisualSimilarityCaptchaService visualSimilarityCaptchaService;

  private final Scheduler captchaGenerationScheduler;

  public CaptchaApiController(PuzzleCaptchaService puzzleCaptchaService,
                              VisualSimilarityCaptchaService visualSimilarityCaptchaService,
                              Scheduler captchaGenerationScheduler) {
    this.puzzleCaptchaService = puzzleCaptchaService;
    this.visualSimilarityCaptchaService = visualSimilarityCaptchaService;
    this.captchaGenerationScheduler = captchaGenerationScheduler;
  }

  @GetMapping(value = "/dynamic-puzzle-captcha/generate-and-get")
  Mono<PuzzleCaptchaSet> generatePuzzleCaptcha(
      @RequestParam(name = "grid-size") int gridSize,
      @RequestParam(name = "piece-side-length") int pieceSideLengthPx,
      @RequestParam(name = "apply-noise", required = false, defaultValue = "true") boolean applyNoise
  ) {
    return generateOnDedicatedPool(() -> puzzleCaptchaService.generateCaptchaSet(
        gridSize, pieceSideLengthPx, applyNoise
    ));
  }

  @GetMapping(value = "/visual-similarity-captcha/generate-and-get")
  Mono<SimilarityChallenge> generateVisualSimilarityCaptcha(
      @RequestParam(name = "grid-size") int gridSize,
      @RequestParam(name = "piece-side-length") int pieceSideLengthPx,
      @RequestParam(name = "apply-noise", required = false, defaultValue = "true") boolean applyNoise
  ) {
    return generateOnDedicatedPool(() -> visualSimilarityCaptchaService.generateCaptchaSet(
        gridSize, pieceSideLengthPx, applyNoise
    ));
  }

  /**
   * Runs blocking, CPU-bound captcha generation off the Netty event loop and maps failures to HTTP statuses.
   */
  private <T> Mono<T> generateOnDedicatedPool(Callable<T> generation) {
    return Mono.fromCallable(() -> callSurvivingOutOfMemory(generation))
        .subscribeOn(captchaGenerationScheduler)
        .onErrorMap(CaptchaApiController::toResponseStatusException);
  }

  /**
   * Reactor rethrows JVM-fatal errors on the worker thread instead of signalling them downstream, which leaves
   * the request hanging without a response. A failed oversized image allocation is recoverable, so it is turned
   * into a regular error.
   */
  private static <T> T callSurvivingOutOfMemory(Callable<T> generation) throws Exception {
    try {
      return generation.call();
    } catch (OutOfMemoryError error) {
      throw new IllegalStateException("Not enough memory to generate captcha", error);
    }
  }

  /**
   * 400 for invalid request parameters, 503 when the generation pool is saturated, 500 for everything else.
   * The 500 reason is fixed so internal details do not leak; the cause is still logged by the error handler.
   */
  private static Throwable toResponseStatusException(Throwable error) {
    return switch (error) {
      case ResponseStatusException alreadyMapped -> alreadyMapped;
      case IllegalArgumentException invalidParameters ->
          new ResponseStatusException(HttpStatus.BAD_REQUEST, invalidParameters.getMessage(), invalidParameters);
      case RejectedExecutionException rejected -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "Captcha generation capacity is exhausted, retry later", rejected);
      default -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate captcha", error);
    };
  }
}
