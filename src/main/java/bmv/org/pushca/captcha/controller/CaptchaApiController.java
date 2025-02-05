package bmv.org.pushca.captcha.controller;

import bmv.org.pushca.captcha.service.CaptchaService;
import bmv.org.pushca.captcha.service.CaptchaService.CaptchaSet;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class CaptchaApiController {

  private final CaptchaService captchaService;

  public CaptchaApiController(CaptchaService captchaService) {
    this.captchaService = captchaService;
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
}
