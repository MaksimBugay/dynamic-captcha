package bmv.org.pushca.captcha.controller;

import static bmv.org.pushca.captcha.service.PuzzleCaptchaService.PuzzleCaptchaSet;

import bmv.org.pushca.captcha.service.CaptchaService;
import bmv.org.pushca.captcha.service.CaptchaService.CaptchaSet;
import bmv.org.pushca.captcha.service.PuzzleCaptchaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class CaptchaApiController {

  private final CaptchaService captchaService;

  private final PuzzleCaptchaService puzzleCaptchaService;

  public CaptchaApiController(CaptchaService captchaService, PuzzleCaptchaService puzzleCaptchaService) {
    this.captchaService = captchaService;
    this.puzzleCaptchaService = puzzleCaptchaService;
  }

  @GetMapping(value = "/dynamic-captcha/generate-and-get")
  Mono<CaptchaSet> generateCaptcha(
      @RequestParam(name = "with-strong", required = false) Boolean withStrongGenerator
  ) {
    return Mono.just(captchaService.generateCaptchaSet(
            Boolean.TRUE.equals(withStrongGenerator)
        ))
        .onErrorResume(error -> Mono.error(
            new RuntimeException("Failed generate captcha set attempt", error)));
  }

  @GetMapping(value = "/dynamic-puzzle-captcha/generate-and-get")
  Mono<PuzzleCaptchaSet> generatePuzzleCaptcha(
      @RequestParam(name = "grid-size") int gridSize,
      @RequestParam(name = "piece-side-length") int pieceSideLengthPx,
      @RequestParam(name = "apply-noise", required = false, defaultValue = "true") boolean applyNoise
  ) {
    return Mono.just(puzzleCaptchaService.generateCaptchaSet(
            gridSize, pieceSideLengthPx, applyNoise
        ))
        .onErrorResume(error -> Mono.error(
            new RuntimeException("Failed generate puzzle captcha set attempt", error)));
  }
}
