package ru.danilavak.zizu.signature;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JsonCanonicalizationService {
    private final ObjectMapper objectMapper;

    public JsonCanonicalizationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public byte[] canonicalize(Object payload) {
        return canonicalizeToString(payload).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public String canonicalizeToString(Object payload) {
        JsonNode root = objectMapper.valueToTree(payload);
        StringBuilder builder = new StringBuilder();
        appendNode(builder, root);
        return builder.toString();
    }

    private void appendNode(StringBuilder builder, JsonNode node) {
        if (node == null || node.isNull()) {
            builder.append("null");
            return;
        }
        if (node.isObject()) {
            appendObject(builder, node);
            return;
        }
        if (node.isArray()) {
            appendArray(builder, node);
            return;
        }
        if (node.isTextual()) {
            appendQuoted(builder, node.textValue());
            return;
        }
        if (node.isBoolean()) {
            builder.append(node.booleanValue());
            return;
        }
        if (node.isNumber()) {
            builder.append(canonicalNumber(node));
            return;
        }
        throw new SignatureConfigurationException("Unsupported JSON node type for canonicalization: " + node.getNodeType());
    }

    private void appendObject(StringBuilder builder, JsonNode node) {
        builder.append('{');
        List<String> names = new ArrayList<>();
        Iterator<String> fields = node.fieldNames();
        while (fields.hasNext()) {
            names.add(fields.next());
        }
        names.sort(String::compareTo);
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            if (i > 0) {
                builder.append(',');
            }
            appendQuoted(builder, name);
            builder.append(':');
            appendNode(builder, node.get(name));
        }
        builder.append('}');
    }

    private void appendArray(StringBuilder builder, JsonNode node) {
        builder.append('[');
        for (int i = 0; i < node.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            appendNode(builder, node.get(i));
        }
        builder.append(']');
    }

    private void appendQuoted(StringBuilder builder, String value) {
        try {
            builder.append(objectMapper.writeValueAsString(value));
        } catch (IOException exception) {
            throw new SignatureConfigurationException("Failed to escape JSON string", exception);
        }
    }

    private String canonicalNumber(JsonNode node) {
        Number value = node.numberValue();
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long || value instanceof BigInteger) {
            return value.toString();
        }
        if (value instanceof BigDecimal decimal) {
            return normalizeDecimal(decimal);
        }
        if (value instanceof Float || value instanceof Double) {
            double doubleValue = value.doubleValue();
            if (!Double.isFinite(doubleValue)) {
                throw new SignatureConfigurationException("Non-finite floating point values are not supported");
            }
            return normalizeDecimal(BigDecimal.valueOf(doubleValue));
        }
        return node.asText();
    }

    private String normalizeDecimal(BigDecimal decimal) {
        BigDecimal normalized = decimal.stripTrailingZeros();
        if (normalized.scale() < 0) {
            normalized = normalized.setScale(0);
        }
        return normalized.toPlainString();
    }
}
