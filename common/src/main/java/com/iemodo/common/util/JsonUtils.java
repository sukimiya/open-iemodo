package com.iemodo.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for JSON serialization and deserialization.
 */
@Slf4j
public class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private JsonUtils() {
        // Utility class
    }

    /**
     * Serialize an object to JSON string.
     *
     * @param obj the object to serialize
     * @return JSON string, or null if serialization fails
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Deserialize JSON string to an object.
     *
     * @param json  the JSON string
     * @param clazz the target class
     * @param <T>   the type
     * @return deserialized object, or null if deserialization fails
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to {}: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * Deserialize JSON string to a type reference (for generics).
     *
     * @param json          the JSON string
     * @param typeReference the type reference
     * @param <T>           the type
     * @return deserialized object, or null if deserialization fails
     */
    public static <T> T fromJson(String json, com.fasterxml.jackson.core.type.TypeReference<T> typeReference) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Pretty print JSON string.
     *
     * @param json the JSON string
     * @return formatted JSON string
     */
    public static String prettyPrint(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }
        try {
            Object obj = OBJECT_MAPPER.readValue(json, Object.class);
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return json;
        }
    }

    /**
     * Convert an object to another type.
     *
     * @param source    the source object
     * @param targetClass the target class
     * @param <S>       source type
     * @param <T>       target type
     * @return converted object
     */
    public static <S, T> T convert(S source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        return OBJECT_MAPPER.convertValue(source, targetClass);
    }

    /**
     * Get the underlying ObjectMapper for advanced operations.
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }
}
