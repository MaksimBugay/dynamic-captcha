package bmv.org.pushca.captcha.serialization.exception;

public class JsonSerializationException extends RuntimeException {
    public JsonSerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonSerializationException(Throwable cause) {
        super(cause);
    }
}
