package bmv.org.pushca.captcha.serialization.json;

import static java.util.stream.Collectors.toMap;

import bmv.org.pushca.captcha.serialization.exception.JsonSerializationException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JsonUtility {

    private static final ObjectMapper SIMPLE_MAPPER = getSimpleMapperInstance();
    private static final ObjectMapper WITH_NULL_MAPPER = getWithNullMapperInstance();

    private static final MapType MAP_TYPE = SIMPLE_MAPPER.getTypeFactory()
            .constructMapType(Map.class, String.class, Object.class);

    private JsonUtility() {
    }

    public static ObjectMapper getSimpleMapperInstance() {
        return Initializer.init(new ObjectMapper());
    }

    public static ObjectMapper getWithNullMapperInstance() {
        return Initializer.init(new ObjectMapper(), JsonInclude.Include.ALWAYS, true);
    }

    public static String prettyPrintJson(String json, boolean writeNulls) throws
            IOException {
        if (writeNulls) {
            Object object = WITH_NULL_MAPPER.readValue(json, Object.class);
            return WITH_NULL_MAPPER.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(object);
        } else {
            Object object = SIMPLE_MAPPER.readValue(json, Object.class);
            return SIMPLE_MAPPER.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(object);
        }
    }

    public static String toJson(Object object) {
        return toJson(object, false);
    }

    public static String toJson(Object object, boolean writeNulls) {
        try {
            if (writeNulls) {
                return WITH_NULL_MAPPER.writeValueAsString(object);
            } else {
                return SIMPLE_MAPPER.writeValueAsString(object);
            }
        } catch (Exception jpe) {
            throw new JsonSerializationException(jpe);
        }
    }

    public static byte[] toJsonAsBytes(Object object) {
        return toJsonAsBytes(object, false);
    }

    public static byte[] toJsonAsBytes(Object object, boolean writeNulls) {
        try {
            if (writeNulls) {
                return WITH_NULL_MAPPER.writeValueAsBytes(object);
            } else {
                return SIMPLE_MAPPER.writeValueAsBytes(object);
            }
        } catch (Exception jpe) {
            throw new JsonSerializationException(jpe);
        }
    }

    public static <T> T fromJson(byte[] jsonBytes, Class<T> clazz) {
        try {
            return SIMPLE_MAPPER.readValue(jsonBytes, clazz);
        } catch (IOException jpe) {
            throw new JsonSerializationException(jpe);
        }
    }

    public static <T> T fromJson(String jsonString, Class<T> clazz) {
        try {
            return SIMPLE_MAPPER.readValue(jsonString, clazz);
        } catch (IOException jpe) {
            throw new JsonSerializationException(jpe);
        }
    }

    public static <T> T fromJson(InputStream is, Class<T> clazz) {
        try {
            return SIMPLE_MAPPER.readValue(is, clazz);
        } catch (IOException jpe) {
            throw new JsonSerializationException(jpe);
        }
    }

    public static <T> T fromJsonStrict(String jsonString, Class<T> clazz) {
        try {
            return WITH_NULL_MAPPER.readValue(jsonString, clazz);
        } catch (IOException jpe) {
            throw new JsonSerializationException(jpe);
        }
    }

    public static <T> T fromJsonStrict(byte[] jsonBytes, Class<T> clazz) {
        try {
            return WITH_NULL_MAPPER.readValue(jsonBytes, clazz);
        } catch (IOException jpe) {
            throw new JsonSerializationException(jpe);
        }
    }

    public static <T> T fromJsonWithTypeReference(String jsonString,
                                                  JavaType resolvedType) {
        try {
            return SIMPLE_MAPPER.readValue(jsonString, resolvedType);
        } catch (IOException jpe) {
            throw new JsonSerializationException(jpe);
        }
    }

    public static <T> Set<T> fromJsonToSet(String jsonInput, Class<T> clazz) {
        try {
            return SIMPLE_MAPPER.readValue(jsonInput,
                    SIMPLE_MAPPER.getTypeFactory()
                            .constructCollectionType(Set.class, clazz));
        } catch (Exception e) {
            throw new JsonSerializationException(e);
        }
    }

    public static <T> List<T> fromJsonToList(String jsonInput, Class<T> clazz) {
        try {
            return SIMPLE_MAPPER.readValue(jsonInput,
                    SIMPLE_MAPPER.getTypeFactory()
                            .constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            throw new JsonSerializationException(e);
        }
    }

    public static <T> List<T> fromJsonToList(String jsonInput,
                                             JavaType javaType) {
        try {
            return SIMPLE_MAPPER.readValue(jsonInput,
                    SIMPLE_MAPPER.getTypeFactory()
                            .constructCollectionType(List.class, javaType));
        } catch (Exception e) {
            throw new JsonSerializationException(e);
        }
    }

    public static Map<String, Object> convertToMap(Object content) {
        String jsonString = JsonUtility.toJson(content);
        return JsonUtility.fromJsonWithTypeReference(
                jsonString,
                MAP_TYPE
        );
    }

    public static Map<String, Object> convertToFilteredMap(
            Object content, List<String> filter) {
        if (content == null) {
            return null;
        }
        Map<String, Object> filteredMap = convertToMap(content);
        return filteredMap.entrySet().stream()
                .filter(entry -> filter.contains(entry.getKey()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static String emptyJson() {
        return "{}";
    }

    public static ObjectMapper getSimpleMapper() {
        return SIMPLE_MAPPER;
    }

    public static ObjectMapper getWithNullMapper() {
        return WITH_NULL_MAPPER;
    }

    public static boolean isValid(String json) {
        try {
            SIMPLE_MAPPER.readTree(json);
        } catch (JacksonException e) {
            return false;
        }
        return true;
    }
}
