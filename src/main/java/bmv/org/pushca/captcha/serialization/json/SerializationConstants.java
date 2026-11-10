package bmv.org.pushca.captcha.serialization.json;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

public final class SerializationConstants {
  public static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
      .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
      .optionalStart()
      .appendFraction(ChronoField.MILLI_OF_SECOND, 1, 3, true)
      .optionalEnd()
      .appendPattern("X")
      .toFormatter();

  private SerializationConstants() {
  }
}