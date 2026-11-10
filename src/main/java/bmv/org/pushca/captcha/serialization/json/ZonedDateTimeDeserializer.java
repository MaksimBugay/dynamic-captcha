package bmv.org.pushca.captcha.serialization.json;

import static bmv.org.pushca.captcha.serialization.json.SerializationConstants.DATE_TIME_FORMATTER;

import java.time.ZonedDateTime;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class ZonedDateTimeDeserializer extends ValueDeserializer<ZonedDateTime> {

  @Override
  public ZonedDateTime deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
    String dateTimeString = parser.getString();
    return ZonedDateTime.parse(dateTimeString, DATE_TIME_FORMATTER);
  }
}
