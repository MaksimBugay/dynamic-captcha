package bmv.org.pushca.captcha.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import reactor.netty.http.HttpResources;
import reactor.netty.resources.LoopResources;

@Configuration
@EnableWebFlux
public class WebFluxConfig {

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
}
