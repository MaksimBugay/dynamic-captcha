package bmv.org.pushca.captcha.config;

import bmv.org.pushca.captcha.serialization.json.JsonUtility;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.HttpResources;
import reactor.netty.resources.LoopResources;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableWebFlux
public class WebFluxConfig implements WebFluxConfigurer {

  private static final int CAPTCHA_GENERATION_QUEUE_CAPACITY = 1000;

  @Bean
  public HttpResources httpResources() {
    return HttpResources.set(
        LoopResources.create(
            "dynamic-captcha-http-server",
            5,
            50,
            true
        )
    );
  }

  /**
   * Captcha image generation is CPU-bound and blocking, so it runs here instead of on the Netty event loop.
   * One thread per core; once the queue is full new requests are rejected rather than piling up.
   */
  @Bean(destroyMethod = "dispose")
  public Scheduler captchaGenerationScheduler() {
    return Schedulers.newBoundedElastic(
        Runtime.getRuntime().availableProcessors(),
        CAPTCHA_GENERATION_QUEUE_CAPACITY,
        "captcha-generation"
    );
  }

  @Bean
  public JsonMapper customJsonMapper() {
    return JsonUtility.getSimpleMapper();
  }

  @Override
  public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
    JsonMapper customJsonMapper = customJsonMapper();

    configurer.defaultCodecs().jacksonJsonEncoder(new JacksonJsonEncoder(customJsonMapper));
    configurer.defaultCodecs().jacksonJsonDecoder(new JacksonJsonDecoder(customJsonMapper));
  }
}
