package bmv.org.pushca.captcha.controller;

import static bmv.org.pushca.captcha.service.CaptchaGenerator.bytesToBufferedImage;
import static bmv.org.pushca.captcha.service.similarity.VisualSimilarityCaptchaService.MIN_PIECE_SIDE_LENGTH_PX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import bmv.org.pushca.captcha.DynamicCaptchaApplication;
import bmv.org.pushca.captcha.config.WebFluxConfig;
import bmv.org.pushca.captcha.serialization.json.JsonUtility;
import bmv.org.pushca.captcha.service.similarity.PuzzleImageGeneratorTypes.BinaryPuzzlePiece;
import bmv.org.pushca.captcha.service.similarity.PuzzleImageGeneratorTypes.Rectangle;
import bmv.org.pushca.captcha.service.similarity.PuzzleImageGeneratorTypes.SimilarityChallenge;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.http.codec.support.DefaultClientCodecConfigurer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

/**
 * Exercises /visual-similarity-captcha/generate-and-get over real HTTP against the fully started application.
 */
@SpringBootTest(classes = {
    DynamicCaptchaApplication.class, WebFluxConfig.class}, webEnvironment = RANDOM_PORT)
class VisualSimilarityCaptchaApiIntegrationTests {

  private static final String ENDPOINT = "/visual-similarity-captcha/generate-and-get";

  private static final Duration RESPONSE_TIMEOUT = Duration.ofMinutes(1);

  private static final int PARALLEL_REQUESTS = 1000;

  private static final int CONNECT_BATCH_SIZE = 20;

  private static final Duration CONNECT_BATCH_INTERVAL = Duration.ofMillis(20);

  // Max per-channel difference allowed between a returned piece and the grid region it points to
  private static final int PIXEL_TOLERANCE = 3;

  @Value("${spring.webflux.base-path:}")
  String contextPath;

  @LocalServerPort
  private String port;

  private String baseUrl;

  private WebTestClient client;

  @BeforeEach
  void prepare() {
    baseUrl = "http://localhost:" + port + contextPath;

    client = WebTestClient
        .bindToServer()
        .baseUrl(baseUrl)
        .responseTimeout(RESPONSE_TIMEOUT)
        .exchangeStrategies(jsonExchangeStrategies())
        .build();
  }

  @ParameterizedTest
  @ValueSource(ints = {2, 3, 4})
  void returnsChallengeWhoseMatchedPiecesPointAtTheirGridRegions(int gridSize) {
    SimilarityChallenge challenge = requestChallenge(gridSize, MIN_PIECE_SIDE_LENGTH_PX, false);

    assertThat(challenge.id()).isNotNull();

    BufferedImage image = bytesToBufferedImage(challenge.image());
    int gridSidePx = gridSize * MIN_PIECE_SIDE_LENGTH_PX;
    // Top and bottom grids are concatenated vertically
    assertThat(image.getWidth()).isEqualTo(gridSidePx);
    assertThat(image.getHeight()).isEqualTo(2 * gridSidePx);

    BinaryPuzzlePiece top = challenge.topMatchedPiece();
    BinaryPuzzlePiece bottom = challenge.bottomMatchedPiece();
    assertThat(top.area().leftTopCorner().y() + top.area().height()).isLessThanOrEqualTo(gridSidePx);
    assertThat(bottom.area().leftTopCorner().y()).isGreaterThanOrEqualTo(gridSidePx);

    for (BinaryPuzzlePiece piece : List.of(challenge.origin(), top, bottom)) {
      assertLiesWithinSingleCell(piece.area(), image);
      BufferedImage pieceImage = assertImageMatchesArea(piece);
      assertThat(countOpaquePixels(pieceImage)).as("piece must contain a visible shape").isPositive();
    }
    // Without noise the grid region at each matched area must show exactly that piece
    assertGridRegionShowsPiece(image, top);
    assertGridRegionShowsPiece(image, bottom);
  }

  @Test
  void appliesNoiseByDefault() {
    SimilarityChallenge challenge = client.get()
        .uri(ENDPOINT + "?grid-size=3&piece-side-length=" + MIN_PIECE_SIDE_LENGTH_PX)
        .exchange()
        .expectStatus().isOk()
        .expectBody(SimilarityChallenge.class)
        .returnResult()
        .getResponseBody();

    assertThat(challenge).isNotNull();
    BufferedImage image = bytesToBufferedImage(challenge.image());
    // Noise lines are 30-60 px apart, so they always cross the matched regions and break the exact match
    assertThat(maxDifferenceFromPiece(image, challenge.topMatchedPiece())).isGreaterThan(PIXEL_TOLERANCE);
    assertThat(maxDifferenceFromPiece(image, challenge.bottomMatchedPiece())).isGreaterThan(PIXEL_TOLERANCE);
  }

  @Test
  void servesParallelRequestsSuccessfully() {
    // The default client pool opens only max(cores, 8) x 2 connections and makes the rest wait, so the server would
    // see a few requests at a time. One connection per request makes all of them hit the server in parallel.
    ConnectionProvider connections = ConnectionProvider.builder("parallel-captcha-requests")
        .maxConnections(PARALLEL_REQUESTS)
        .pendingAcquireMaxCount(PARALLEL_REQUESTS)
        .build();
    // Own event loops, so the client does not compete with the server for its I/O threads
    LoopResources clientLoops = LoopResources.create("parallel-captcha-client");
    try {
      WebClient parallelClient = WebClient.builder()
          .baseUrl(baseUrl)
          .clientConnector(new ReactorClientHttpConnector(HttpClient.create(connections).runOn(clientLoops)))
          .exchangeStrategies(jsonExchangeStrategies())
          .build();
      AtomicInteger inFlight = new AtomicInteger();
      AtomicInteger peakInFlight = new AtomicInteger();

      // Opening all connections in the same instant overflows the OS accept queue (128 on macOS) and the kernel
      // resets the excess. Small batches stay under it and still open all connections within ~1 s.
      List<RequestOutcome> outcomes = Flux.range(0, PARALLEL_REQUESTS / CONNECT_BATCH_SIZE)
          .delayElements(CONNECT_BATCH_INTERVAL)
          .concatMap(_ -> Flux.range(0, CONNECT_BATCH_SIZE))
          .flatMap(_ -> requestOutcome(parallelClient)
              .doOnSubscribe(_ -> peakInFlight.accumulateAndGet(inFlight.incrementAndGet(), Math::max))
              .doFinally(_ -> inFlight.decrementAndGet()), PARALLEL_REQUESTS)
          .collectList()
          .block(RESPONSE_TIMEOUT);
      System.out.println("Peak requests in flight: " + peakInFlight.get() + " of " + PARALLEL_REQUESTS);

      assertThat(outcomes).hasSize(PARALLEL_REQUESTS);
      assertThat(outcomes).filteredOn(outcome -> !outcome.succeeded()).as("failed requests").isEmpty();
      assertThat(peakInFlight.get()).as("requests in flight at the same time")
          .isGreaterThanOrEqualTo(PARALLEL_REQUESTS / 2);
      assertThat(outcomes).extracting(RequestOutcome::challengeId).doesNotHaveDuplicates();
      // Every request must get its own challenge. This does not prove the random generator is thread-safe:
      // a racy shared generator yields correlated values, which rarely produce byte-identical images.
      assertThat(outcomes).extracting(RequestOutcome::imageHash).doesNotHaveDuplicates();
    } finally {
      connections.dispose();
      clientLoops.dispose();
    }
  }

  /**
   * Captures the result of one request instead of failing fast, so a failure report lists every failed request.
   * The response is validated on arrival and only its id and image hash are kept, to bound memory use.
   */
  private static Mono<RequestOutcome> requestOutcome(WebClient webClient) {
    return webClient.get()
        .uri(ENDPOINT + "?grid-size=3&piece-side-length=" + MIN_PIECE_SIDE_LENGTH_PX + "&apply-noise=true")
        .exchangeToMono(response -> response.statusCode().is2xxSuccessful()
            ? response.bodyToMono(SimilarityChallenge.class)
                .map(challenge -> RequestOutcome.validated(response.statusCode(), challenge))
            : response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> RequestOutcome.failed(response.statusCode(), body)))
        .defaultIfEmpty(RequestOutcome.failed(null, "empty response body"))
        .onErrorResume(error -> Mono.just(RequestOutcome.failed(null, error.toString())));
  }

  private record RequestOutcome(HttpStatusCode status, UUID challengeId, UUID imageHash, String failure) {

    static RequestOutcome validated(HttpStatusCode status, SimilarityChallenge challenge) {
      assertThat(challenge.id()).isNotNull();
      assertThat(bytesToBufferedImage(challenge.image())).isNotNull();
      assertImageMatchesArea(challenge.topMatchedPiece());
      assertImageMatchesArea(challenge.bottomMatchedPiece());
      return new RequestOutcome(status, challenge.id(), UUID.nameUUIDFromBytes(challenge.image()), null);
    }

    static RequestOutcome failed(HttpStatusCode status, String failure) {
      return new RequestOutcome(status, null, null, failure);
    }

    boolean succeeded() {
      return failure == null && status != null && status.is2xxSuccessful();
    }
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      grid-size=3&piece-side-length=299 | Piece side length must be at least 300 px, but was 299
      grid-size=3&piece-side-length=0   | Piece side length must be at least 300 px, but was 0
      grid-size=0&piece-side-length=300 | Grid size must be greater than 0
      grid-size=3                       | Required query parameter 'piece-side-length' is not present.
      piece-side-length=300             | Required query parameter 'grid-size' is not present.
      """)
  void rejectsInvalidParametersWithReason(String query, String expectedMessage) {
    client.get()
        .uri(ENDPOINT + "?" + query)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.status").isEqualTo(400)
        .jsonPath("$.message").isEqualTo(expectedMessage);
  }

  @Test
  void rejectsNonNumericParameter() {
    client.get()
        .uri(ENDPOINT + "?grid-size=three&piece-side-length=300")
        .exchange()
        .expectStatus().isBadRequest();
  }

  private SimilarityChallenge requestChallenge(int gridSize, int pieceSideLengthPx, boolean applyNoise) {
    SimilarityChallenge challenge = client.get()
        .uri(ENDPOINT + "?grid-size=" + gridSize + "&piece-side-length=" + pieceSideLengthPx
             + "&apply-noise=" + applyNoise)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody(SimilarityChallenge.class)
        .returnResult()
        .getResponseBody();
    assertThat(challenge).isNotNull();
    return challenge;
  }

  private static BufferedImage assertImageMatchesArea(BinaryPuzzlePiece piece) {
    BufferedImage pieceImage = bytesToBufferedImage(piece.image());
    assertThat(pieceImage).isNotNull();
    assertThat(pieceImage.getWidth()).isEqualTo(piece.area().width());
    assertThat(pieceImage.getHeight()).isEqualTo(piece.area().height());
    return pieceImage;
  }

  private static void assertLiesWithinSingleCell(Rectangle area, BufferedImage image) {
    int x = area.leftTopCorner().x();
    int y = area.leftTopCorner().y();
    assertThat(x).isNotNegative();
    assertThat(y).isNotNegative();
    assertThat(x + area.width()).isLessThanOrEqualTo(image.getWidth());
    assertThat(y + area.height()).isLessThanOrEqualTo(image.getHeight());
    assertThat((x + area.width() - 1) / MIN_PIECE_SIDE_LENGTH_PX).as("same cell column")
        .isEqualTo(x / MIN_PIECE_SIDE_LENGTH_PX);
    assertThat((y + area.height() - 1) / MIN_PIECE_SIDE_LENGTH_PX).as("same cell row")
        .isEqualTo(y / MIN_PIECE_SIDE_LENGTH_PX);
  }

  private static void assertGridRegionShowsPiece(BufferedImage grid, BinaryPuzzlePiece piece) {
    assertThat(maxDifferenceFromPiece(grid, piece)).as("grid region at %s differs from the piece", piece.area())
        .isLessThanOrEqualTo(PIXEL_TOLERANCE);
  }

  /**
   * The grid is rendered on white, so a grid region showing the piece equals the piece alpha-blended over white.
   *
   * @return the largest per-channel difference between the grid region at the piece area and that blend
   */
  private static int maxDifferenceFromPiece(BufferedImage grid, BinaryPuzzlePiece piece) {
    BufferedImage pieceImage = bytesToBufferedImage(piece.image());
    int offsetX = piece.area().leftTopCorner().x();
    int offsetY = piece.area().leftTopCorner().y();
    int maxDifference = 0;

    for (int y = 0; y < pieceImage.getHeight(); y++) {
      for (int x = 0; x < pieceImage.getWidth(); x++) {
        int expected = blendOverWhite(pieceImage.getRGB(x, y));
        int actual = grid.getRGB(offsetX + x, offsetY + y);
        maxDifference = Math.max(maxDifference, maxChannelDifference(expected, actual));
      }
    }
    return maxDifference;
  }

  private static int blendOverWhite(int argb) {
    double alpha = (argb >>> 24) / 255.0;
    int red = (int) Math.round(((argb >> 16) & 0xFF) * alpha + 255 * (1 - alpha));
    int green = (int) Math.round(((argb >> 8) & 0xFF) * alpha + 255 * (1 - alpha));
    int blue = (int) Math.round((argb & 0xFF) * alpha + 255 * (1 - alpha));
    return (red << 16) | (green << 8) | blue;
  }

  private static int maxChannelDifference(int rgb1, int rgb2) {
    int max = 0;
    for (int shift = 0; shift <= 16; shift += 8) {
      max = Math.max(max, Math.abs(((rgb1 >> shift) & 0xFF) - ((rgb2 >> shift) & 0xFF)));
    }
    return max;
  }

  private static long countOpaquePixels(BufferedImage image) {
    long count = 0;
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        if ((image.getRGB(x, y) >>> 24) > 0) {
          count++;
        }
      }
    }
    return count;
  }

  private static ExchangeStrategies jsonExchangeStrategies() {
    JacksonJsonEncoder jsonEncoder = new JacksonJsonEncoder(JsonUtility.getSimpleMapper());
    JacksonJsonDecoder jsonDecoder = new JacksonJsonDecoder(JsonUtility.getSimpleMapper());
    return ExchangeStrategies.builder()
        .codecs(clientCodecConfigurer -> {
          DefaultClientCodecConfigurer configurer = (DefaultClientCodecConfigurer) clientCodecConfigurer;
          configurer.defaultCodecs().jacksonJsonEncoder(jsonEncoder);
          configurer.defaultCodecs().jacksonJsonDecoder(jsonDecoder);
          // A 4x4 grid challenge is a few MB of base64 PNG, above the 256 KB default
          configurer.defaultCodecs().maxInMemorySize(32 * 1024 * 1024);
        })
        .build();
  }
}
