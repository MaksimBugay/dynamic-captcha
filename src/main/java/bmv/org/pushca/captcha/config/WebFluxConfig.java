package bmv.org.pushca.captcha.config;

import bmv.org.pushca.captcha.serialization.json.JsonUtility;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import reactor.netty.http.HttpResources;
import reactor.netty.resources.LoopResources;

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
  public ObjectMapper customObjectMapper() {
    return JsonUtility.getSimpleMapper();
  }

  @Override
  public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
    ObjectMapper customObjectMapper = customObjectMapper();

    configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(customObjectMapper));
    configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(customObjectMapper));
  }
}
