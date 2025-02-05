package bmv.org.pushca.captcha.config;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import java.util.Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KaptchaConfig {

  @Bean
  public DefaultKaptcha getKaptchaBean() {
    DefaultKaptcha kaptcha = new DefaultKaptcha();
    Properties properties = new Properties();

    // Configure basic CAPTCHA properties
    properties.setProperty("kaptcha.border", "yes");
    properties.setProperty("kaptcha.border.color", "0,0,0");
    properties.setProperty("kaptcha.textproducer.font.color", "black");
    properties.setProperty("kaptcha.image.width", "120");
    properties.setProperty("kaptcha.image.height", "120");
    properties.setProperty("kaptcha.textproducer.font.size", "40");
    properties.setProperty("kaptcha.textproducer.char.length", "6");
    properties.setProperty("kaptcha.textproducer.char.string",
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");

    // Background and Noise Configuration
    properties.setProperty("kaptcha.noise.color", "black");
    properties.setProperty("kaptcha.background.clear.from", "220,220,220");
    properties.setProperty("kaptcha.background.clear.to", "255,255,255");

    Config config = new Config(properties);
    kaptcha.setConfig(config);

    return kaptcha;
  }
}
