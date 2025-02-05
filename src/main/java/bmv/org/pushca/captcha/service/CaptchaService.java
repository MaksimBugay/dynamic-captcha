package bmv.org.pushca.captcha.service;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CaptchaService {

  public static final TimeBasedEpochGenerator ID_GENERATOR = Generators.timeBasedEpochGenerator();

  private final CaptchaGeneratorSimple captchaGeneratorSimple;
  private final CaptchaGeneratorAdvanced captchaGeneratorAdvanced;
  private final CaptchaGeneratorStrong captchaGeneratorStrong;

  public CaptchaService(CaptchaGeneratorSimple captchaGeneratorSimple,
      CaptchaGeneratorAdvanced captchaGeneratorAdvanced,
      CaptchaGeneratorStrong captchaGeneratorStrong) {
    this.captchaGeneratorSimple = captchaGeneratorSimple;
    this.captchaGeneratorAdvanced = captchaGeneratorAdvanced;
    this.captchaGeneratorStrong = captchaGeneratorStrong;
  }

  public CaptchaSet generateCaptchaSet(boolean withStrongGenerator) {
    final int x1 = CaptchaGenerator.RANDOM.nextInt(9) + 1;
    final int x2 = CaptchaGenerator.RANDOM.nextInt(9) + 1;

    byte[] captcha = withStrongGenerator ? captchaGeneratorStrong.generateCaptcha(x1, x2, "png") :
        captchaGeneratorAdvanced.generateCaptcha(x1, x2, "png");

    Set<Integer> results = new HashSet<>();
    final int resultValue = x1 * x2;
    results.add(resultValue);
    while (results.size() < 9) {
      results.add(generateRandomResult());
    }

    int correctResultIndex = -1;
    int index = -1;
    ResultCaptcha[] resultArray = new ResultCaptcha[9];
    for (Integer r : results) {
      index++;
      if (r == resultValue) {
        correctResultIndex = index;
      }
      resultArray[index] =
          new ResultCaptcha(r, captchaGeneratorSimple.generateCaptcha(r.toString(), "jpg"));
    }

    return new CaptchaSet(
        ID_GENERATOR.generate(),
        x1, x2,
        captcha,
        resultArray,
        correctResultIndex
    );
  }

  private int generateRandomResult() {
    int x1 = CaptchaGenerator.RANDOM.nextInt(9) + 1;
    int x2 = CaptchaGenerator.RANDOM.nextInt(9) + 1;
    return x1 * x2;
  }

  public record ResultCaptcha(int result, byte[] image) {

  }

  public record CaptchaSet(UUID id, int x1, int x2, byte[] captcha,
                           ResultCaptcha[] results,
                           int correctResultIndex) {

    private static final String FIRST_LEVEL_DELIMITER = "::";
    private static final String SECOND_LEVEL_DELIMITER = "|";

    public CaptchaSetPure purify() {
      return new CaptchaSetPure(
          this.id,
          this.x1,
          this.x2,
          Arrays.stream(this.results).map(ResultCaptcha::result).toList().toArray(Integer[]::new),
          this.correctResultIndex
      );
    }

    public byte[] toPayloadBytes() {
      return MessageFormat.format("{0}{1}{2}",
          Base64.getEncoder().encodeToString(this.captcha),
          FIRST_LEVEL_DELIMITER,
          Arrays.stream(this.results)
              .map(ResultCaptcha::image)
              .map(bytes -> Base64.getEncoder().encodeToString(bytes))
              .collect(Collectors.joining(SECOND_LEVEL_DELIMITER))
      ).getBytes(StandardCharsets.UTF_8);
    }

    public static CaptchaSetBinaries toCaptchaSetBinaries(byte[] payload) {
      String payloadStr = new String(payload, StandardCharsets.UTF_8);
      String[] parts = payloadStr.split("::");
      byte[][] results = Arrays.stream(parts[1].split("\\|"))
          .map(s -> Base64.getDecoder().decode(s))
          .toList().toArray(new byte[0][0]);
      return new CaptchaSetBinaries(
          Base64.getDecoder().decode(parts[0]),
          results
      );
    }
  }


  public record CaptchaSetPure(UUID id, int x1, int x2, Integer[] results,
                               int correctResultIndex) {

  }

  public record CaptchaSetBinaries(byte[] captcha, byte[][] results) {

  }

}
