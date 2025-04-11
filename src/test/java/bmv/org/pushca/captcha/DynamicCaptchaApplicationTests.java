package bmv.org.pushca.captcha;

import static bmv.org.pushca.captcha.service.CaptchaGenerator.bufferedImageToBytes;
import static bmv.org.pushca.captcha.service.CaptchaGenerator.bytesToBufferedImage;
import static bmv.org.pushca.captcha.service.CaptchaService.CaptchaSet.toCaptchaSetBinaries;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import bmv.org.pushca.captcha.config.WebFluxConfig;
import bmv.org.pushca.captcha.serialization.json.JsonUtility;
import bmv.org.pushca.captcha.service.CaptchaService;
import bmv.org.pushca.captcha.service.CaptchaService.CaptchaSet;
import bmv.org.pushca.captcha.service.CaptchaService.CaptchaSetBinaries;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import bmv.org.pushca.captcha.service.PuzzleCaptchaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
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

  @Autowired
  private CaptchaService captchaService;

  @Autowired
  private PuzzleCaptchaService puzzleCaptchaService;

  protected WebTestClient client;

  @BeforeEach
  public void prepare() {
    Jackson2JsonEncoder jsonEncoder = new Jackson2JsonEncoder(JsonUtility.getSimpleMapper());
    Jackson2JsonDecoder jsonDecoder = new Jackson2JsonDecoder(JsonUtility.getSimpleMapper());

    ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
        .codecs(clientCodecConfigurer -> {
          DefaultClientCodecConfigurer configurer =
              (DefaultClientCodecConfigurer) clientCodecConfigurer;
          configurer.defaultCodecs().jackson2JsonEncoder(jsonEncoder);
          configurer.defaultCodecs().jackson2JsonDecoder(jsonDecoder);
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
  void captchaSetBinariesWebTest() {
    client.get()
        .uri("/dynamic-captcha/generate-and-get?with-strong=true")
        .exchange()
        .expectStatus().isOk()
        .expectBody(CaptchaSet.class)
        .value(captchaSet -> {
          assertNotNull(captchaSet);
          processCaptchaSet(captchaSet);
        });
  }

  @Test
  void captchaSetBinariesTest() {
    CaptchaSet captchaSet = captchaService.generateCaptchaSet(true);
    String json = JsonUtility.toJson(captchaSet);
    captchaSet = JsonUtility.fromJson(json, CaptchaSet.class);
    processCaptchaSet(captchaSet);
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
          BufferedImage gridWithSelection = puzzleCaptchaService.getGenerator().drawDashedRectangleOnGridImage(
              bytesToBufferedImage(captchaSet.puzzleImage()),
              captchaSet.correctOptionRectangle()
          );
          BufferedImage gridWithTwoSelections = puzzleCaptchaService.getGenerator().drawDashedRectangleOnGridImage(
              gridWithSelection, captchaSet.targetRectangle()
          );
          saveBytesToFile(
              bufferedImageToBytes(gridWithTwoSelections, "png"),
              "puzzle_options_grid_with_selection" + ".png"
          );
        });
  }

  private static void processCaptchaSet(CaptchaSet captchaSet) {
    System.out.printf("x1 = %d; x2 = %d", captchaSet.x1(), captchaSet.x2());
    System.out.println(" ");

    byte[] payload = captchaSet.toPayloadBytes();
    CaptchaSetBinaries binaries = toCaptchaSetBinaries(payload);
    saveBytesToFile(binaries.captcha(), "captcha.png");
    int i = -1;
    for (byte[] result : binaries.results()) {
      i++;
      saveBytesToFile(result, "result" + i + ".jpg");
    }
    System.out.println("Result index = " + captchaSet.correctResultIndex());
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
