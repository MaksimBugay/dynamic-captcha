package bmv.org.pushca.captcha;

import static bmv.org.pushca.captcha.service.CaptchaGenerator.bufferedImageToBytes;
import static bmv.org.pushca.captcha.service.CaptchaGenerator.bytesToBufferedImage;
import static bmv.org.pushca.captcha.service.similarity.PuzzleImageGeneratorUtils.drawDashedRectangleOnGridImage;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import bmv.org.pushca.captcha.config.WebFluxConfig;
import bmv.org.pushca.captcha.serialization.json.JsonUtility;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import bmv.org.pushca.captcha.service.PuzzleCaptchaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.http.codec.support.DefaultClientCodecConfigurer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

@SpringBootTest(classes = {
    DynamicCaptchaApplication.class, WebFluxConfig.class}, webEnvironment = RANDOM_PORT)
class DynamicCaptchaApplicationTests {

  @Value("${spring.webflux.base-path:}")
  String contextPath;

  @LocalServerPort
  private String port;

  protected WebTestClient client;

  @BeforeEach
  public void prepare() {
    JacksonJsonEncoder jsonEncoder = new JacksonJsonEncoder(JsonUtility.getSimpleMapper());
    JacksonJsonDecoder jsonDecoder = new JacksonJsonDecoder(JsonUtility.getSimpleMapper());

    ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
        .codecs(clientCodecConfigurer -> {
          DefaultClientCodecConfigurer configurer =
              (DefaultClientCodecConfigurer) clientCodecConfigurer;
          configurer.defaultCodecs().jacksonJsonEncoder(jsonEncoder);
          configurer.defaultCodecs().jacksonJsonDecoder(jsonDecoder);
        })
        .build();

    client = WebTestClient
        .bindToServer()
        .baseUrl("http://localhost:" + port + contextPath)
        .responseTimeout(Duration.of(1, ChronoUnit.MINUTES))
        .exchangeStrategies(exchangeStrategies)
        .build();
  }

  @Test
  void puzzleCaptchaSetBinariesWebTest() {
    client.get()
        .uri("/dynamic-puzzle-captcha/generate-and-get?grid-size=3&piece-side-length=200&apply-noise=true")
        .exchange()
        .expectStatus().isOk()
        .expectBody(PuzzleCaptchaService.PuzzleCaptchaSet.class)
        .value(captchaSet -> {
          assertNotNull(captchaSet);
          fileSystemCleanup();
          saveBytesToFile(
              captchaSet.puzzleImage(),
              "full_puzzle_board" + ".png"
          );

          saveBytesToFile(
              captchaSet.correctOptionImage(),
              "correct_option" + ".png"
          );
          BufferedImage gridWithSelection = drawDashedRectangleOnGridImage(
              bytesToBufferedImage(captchaSet.puzzleImage()),
              captchaSet.correctOptionRectangle()
          );
          BufferedImage gridWithTwoSelections = drawDashedRectangleOnGridImage(
              gridWithSelection, captchaSet.targetRectangle()
          );
          saveBytesToFile(
              bufferedImageToBytes(gridWithTwoSelections, "png"),
              "puzzle_options_grid_with_selection" + ".png"
          );
        });
  }

  public static void saveBytesToFile(byte[] captcha, String imageName) {
    File outputFile = new File("captcha-set/" + imageName);
    if (outputFile.delete()) {
      System.out.println("cleanup");
    }
    try {
      Files.write(outputFile.toPath(), captcha);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    System.out.println("File saved at: " + outputFile.getAbsolutePath());
  }

  public static void fileSystemCleanup() {
    File directory = new File("captcha-set");
    if (directory.exists() && directory.isDirectory()) {
      for (File file : directory.listFiles()) {
        if (file.isFile()) {
          if (file.delete()) {
            System.out.println("Deleted file: " + file.getName());
          } else {
            System.err.println("Failed to delete file: " + file.getName());
          }
        }
      }
    }
  }
}
