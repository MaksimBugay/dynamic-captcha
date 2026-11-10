package bmv.org.pushca.captcha.serialization.json;

import static bmv.org.pushca.captcha.serialization.json.SerializationConstants.DATE_TIME_FORMATTER;

import java.time.ZonedDateTime;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class ZonedDateTimeSerializer extends ValueSerializer<ZonedDateTime> {
  @Override
  public void serialize(ZonedDateTime value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
    gen.writeString(value.format(DATE_TIME_FORMATTER));
  }
}
