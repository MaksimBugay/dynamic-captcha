package bmv.org.pushca.captcha.config;

import bmv.org.pushca.captcha.serialization.json.JsonUtility;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import reactor.netty.http.HttpResources;
import reactor.netty.resources.LoopResources;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableWebFlux
public class WebFluxConfig implements WebFluxConfigurer {

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
