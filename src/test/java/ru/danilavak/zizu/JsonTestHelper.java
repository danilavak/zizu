package ru.danilavak.zizu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class JsonTestHelper {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonTestHelper() {
    }

    static Long idFrom(String json) throws Exception {
        return longValueFrom(json, "id");
    }

    static Long longValueFrom(String json, String field) throws Exception {
        JsonNode node = OBJECT_MAPPER.readTree(json);
        return node.get(field).asLong();
    }

    static String stringValueFrom(String json, String field) throws Exception {
        JsonNode node = OBJECT_MAPPER.readTree(json);
        return node.get(field).asText();
    }
}
