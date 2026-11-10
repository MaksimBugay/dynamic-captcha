package bmv.org.pushca.captcha.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bmv.org.pushca.captcha.service.PuzzleCaptchaService;
import bmv.org.pushca.captcha.service.similarity.PuzzleImageGeneratorTypes.SimilarityChallenge;
import bmv.org.pushca.captcha.service.similarity.VisualSimilarityCaptchaService;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

class CaptchaApiControllerTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  private final VisualSimilarityCaptchaService similarityService = mock(VisualSimilarityCaptchaService.class);

  // One worker and a queue of one task, so the third concurrent request is rejected
  private final Scheduler scheduler = Schedulers.newBoundedElastic(1, 1, "captcha-generation-test");

  private final CaptchaApiController controller =
      new CaptchaApiController(mock(PuzzleCaptchaService.class), similarityService, scheduler);

  @AfterEach
  void disposeScheduler() {
    scheduler.dispose();
  }

  @Test
  void invalidParametersAreReportedAsBadRequestWithReason() {
    when(similarityService.generateCaptchaSet(anyInt(), anyInt(), anyBoolean()))
        .thenThrow(new IllegalArgumentException("Piece side length must be at least 300 px, but was 299"));

    StepVerifier.create(controller.generateVisualSimilarityCaptcha(3, 299, true))
        .expectErrorSatisfies(error -> assertStatus(error, HttpStatus.BAD_REQUEST,
            "Piece side length must be at least 300 px, but was 299"))
        .verify(TIMEOUT);
  }

  @Test
  void outOfMemoryCompletesRequestWithServerErrorInsteadOfHanging() {
    when(similarityService.generateCaptchaSet(anyInt(), anyInt(), anyBoolean()))
        .thenThrow(new OutOfMemoryError("Java heap space"));

    StepVerifier.create(controller.generateVisualSimilarityCaptcha(20, 1000, true))
        .expectErrorSatisfies(error -> assertStatus(error, HttpStatus.INTERNAL_SERVER_ERROR,
            "Failed to generate captcha"))
        .verify(TIMEOUT);
  }

  @Test
  void unexpectedFailureIsReportedAsServerErrorWithoutLeakingDetails() {
    when(similarityService.generateCaptchaSet(anyInt(), anyInt(), anyBoolean()))
        .thenThrow(new IllegalStateException("internal detail"));

    StepVerifier.create(controller.generateVisualSimilarityCaptcha(3, 300, true))
        .expectErrorSatisfies(error -> assertStatus(error, HttpStatus.INTERNAL_SERVER_ERROR,
            "Failed to generate captcha"))
        .verify(TIMEOUT);
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  void saturatedGenerationPoolIsReportedAsServiceUnavailable() throws InterruptedException {
    CountDownLatch release = new CountDownLatch(1);
    CountDownLatch running = new CountDownLatch(1);
    when(similarityService.generateCaptchaSet(anyInt(), anyInt(), anyBoolean())).thenAnswer(_ -> {
      running.countDown();
      release.await(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
      return null;
    });

    // Occupies the only worker, then the only queue slot
    Mono<SimilarityChallenge> busy = controller.generateVisualSimilarityCaptcha(3, 300, true);
    Mono<SimilarityChallenge> queued = controller.generateVisualSimilarityCaptcha(3, 300, true);
    busy.subscribe(_ -> { }, _ -> { });
    running.await(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
    queued.subscribe(_ -> { }, _ -> { });

    try {
      StepVerifier.create(controller.generateVisualSimilarityCaptcha(3, 300, true))
          .expectErrorSatisfies(error -> assertStatus(error, HttpStatus.SERVICE_UNAVAILABLE,
              "Captcha generation capacity is exhausted, retry later"))
          .verify(TIMEOUT);
    } finally {
      release.countDown();
    }
  }

  private static void assertStatus(Throwable error, HttpStatus status, String reason) {
    assertThat(error)
        .isInstanceOfSatisfying(ResponseStatusException.class, responseStatus -> {
          assertThat(responseStatus.getStatusCode()).isEqualTo(status);
          assertThat(responseStatus.getReason()).isEqualTo(reason);
        });
  }
}
