package bmv.org.pushca.captcha;

import static bmv.org.pushca.captcha.service.CaptchaService.CaptchaSet.toCaptchaSetBinaries;

import bmv.org.pushca.captcha.service.CaptchaService;
import bmv.org.pushca.captcha.service.CaptchaService.CaptchaSet;
import bmv.org.pushca.captcha.service.CaptchaService.CaptchaSetBinaries;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DynamicCaptchaApplicationTests {

  @Autowired
  private CaptchaService captchaService;

  @Test
  void captchaSetBinariesTest() {
    CaptchaSet captchaSet = captchaService.generateCaptchaSet(true);

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
}
