package org.thingsboard.ai.mcp.server.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.ai.mcp.server.data.KeyFilterInput;
import org.thingsboard.client.model.EntityDataPageLink;
import org.thingsboard.client.model.Direction;
import org.thingsboard.client.model.EntityDataSortOrder;
import org.thingsboard.client.model.EntityKey;
import org.thingsboard.client.model.EntityKeyType;
import org.thingsboard.client.model.KeyFilter;

import java.util.List;
import java.util.regex.Pattern;

public class ToolUtils {

    public static final Pattern PROPERTY_PATTERN = Pattern.compile("^[\\p{L}0-9_-]+$");
    public static final int PAGE_SIZE = 10;
    public static final int PAGE_NUMBER = 0;

    public static String sanitizeStringParam(String value) {
        if (isNullOrBlank(value)) {
            return null;
        }
        return value.trim();
    }

    public static EntityDataPageLink createPageLink(String pageSizeStr, String pageStr, String textSearch, String sortOrderKey, String sortOrderType, String sortOrder) {
        final int pageSize = sanitizePageSize(parseIntOrDefault(pageSizeStr, PAGE_SIZE));
        final int page = sanitizePageNumber(parseIntOrDefault(pageStr, PAGE_NUMBER));
        final String sanitizedTextSearch = sanitizeStringParam(textSearch);
        final String sanitizedSortOrderKey = sanitizeStringParam(sortOrderKey);
        final String sanitizedSortOrderType = sanitizeStringParam(sortOrderType);
        final String sanitizedSortOrder = sanitizeStringParam(sortOrder);

        EntityKey entityKey = null;
        if (StringUtils.isNotEmpty(sanitizedSortOrderKey) && StringUtils.isNotEmpty(sanitizedSortOrderType)) {
            try {
                EntityKeyType type = EntityKeyType.valueOf(sanitizedSortOrderType);
                entityKey = new EntityKey().type(type).key(sanitizedSortOrderKey);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unsupported entity key type '" + sanitizedSortOrderType + "'!");
            }
        }
        Direction direction = Direction.ASC;
        if (StringUtils.isNotEmpty(sanitizedSortOrder)) {
            try {
                direction = Direction.valueOf(sanitizedSortOrder.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unsupported sort order '" + sanitizedSortOrder + "'! Only 'ASC' or 'DESC' types are allowed.");
            }
        }

        EntityDataPageLink pageLink = new EntityDataPageLink()
                .pageSize(pageSize)
                .page(page)
                .textSearch(sanitizedTextSearch);

        if (entityKey != null) {
            EntityDataSortOrder entityDataSortOrder = new EntityDataSortOrder()
                    .key(entityKey)
                    .direction(direction);
            pageLink.sortOrder(entityDataSortOrder);
        }

        return pageLink;
    }

    private static int sanitizePageSize(int value) {
        return value > 0 ? value : PAGE_SIZE;
    }

    private static int sanitizePageNumber(int value) {
        return Math.max(value, 0);
    }

    public static Long parseLong(String value, Long defaultValue) {
        if (isNullOrBlank(value)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    public static Integer parseIntOrDefault(String candidate, Integer defaultValue) {
        if (isNullOrBlank(candidate)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(candidate.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    public static boolean isNullOrBlank(String value) {
        if (value == null) {
            return true;
        }
        String trimmed = value.trim().toLowerCase();
        return trimmed.isEmpty() || "null".equals(trimmed) || "none".equals(trimmed) || "undefined".equals(trimmed);
    }

    public static Long parseLong(String value) {
        return parseLong(value, null);
    }

    public static List<KeyFilter> parseKeyFilters(String keyFiltersJson) {
        if (isNullOrBlank(keyFiltersJson)) {
            return null;
        }
        String normalizedJson = normalizeKeyFiltersFormat(keyFiltersJson);
        List<KeyFilterInput> inputs = JsonUtils.fromString(normalizedJson, new TypeReference<>() {});
        if (inputs == null || inputs.isEmpty()) {
            return null;
        }
        return inputs.stream().map(KeyFilterInput::toKeyFilter).toList();
    }

    private static String normalizeKeyFiltersFormat(String json) {
        try {
            JsonNode arrayNode = JsonUtils.toJsonNode(json);
            if (arrayNode == null || !arrayNode.isArray() || arrayNode.isEmpty()) {
                return json;
            }
            JsonNode first = arrayNode.get(0);
            if (!first.has("key") || !first.get("key").isObject()) {
                return json;
            }
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode result = mapper.createArrayNode();
            for (JsonNode node : arrayNode) {
                result.add(convertCanonicalFilter(node, mapper));
            }
            return result.toString();
        } catch (Exception e) {
            return json;
        }
    }

    private static ObjectNode convertCanonicalFilter(JsonNode node, ObjectMapper mapper) {
        ObjectNode flat = mapper.createObjectNode();

        JsonNode keyNode = node.get("key");
        if (keyNode != null && keyNode.isObject()) {
            flat.put("keyType", keyNode.path("type").asText());
            flat.put("key", keyNode.path("key").asText());
        }

        if (node.has("valueType")) {
            flat.put("valueType", node.get("valueType").asText());
        }

        JsonNode predicate = node.get("predicate");
        if (predicate != null) {
            convertPredicate(predicate, flat, mapper);
        }

        return flat;
    }

    private static void convertPredicate(JsonNode predicate, ObjectNode flat, ObjectMapper mapper) {
        String predicateType = predicate.path("type").asText(null);
        if (predicateType != null) {
            flat.put("predicateType", predicateType);
        }

        if ("COMPLEX".equals(predicateType)) {
            if (predicate.has("operation")) {
                flat.put("complexOperation", predicate.get("operation").asText());
            }
            if (predicate.has("predicates") && predicate.get("predicates").isArray()) {
                ArrayNode nestedArray = mapper.createArrayNode();
                for (JsonNode nestedPred : predicate.get("predicates")) {
                    ObjectNode nestedFlat = mapper.createObjectNode();
                    convertPredicate(nestedPred, nestedFlat, mapper);
                    nestedArray.add(nestedFlat);
                }
                flat.set("nestedPredicates", nestedArray);
            }
        } else {
            if (predicate.has("operation")) {
                flat.put("operation", predicate.get("operation").asText());
            }
            JsonNode value = predicate.get("value");
            if (value != null) {
                if (value.has("defaultValue") && !value.get("defaultValue").isNull()) {
                    flat.set("defaultValue", value.get("defaultValue"));
                }
                if (value.has("userValue") && !value.get("userValue").isNull()) {
                    flat.set("userValue", value.get("userValue"));
                }
                JsonNode dynVal = value.get("dynamicValue");
                if (dynVal != null && !dynVal.isNull()) {
                    if (dynVal.has("sourceType")) {
                        flat.put("dynamicValueSourceType", dynVal.get("sourceType").asText());
                    }
                    if (dynVal.has("sourceAttribute")) {
                        flat.put("dynamicValueSourceAttribute", dynVal.get("sourceAttribute").asText());
                    }
                    if (dynVal.has("inherit")) {
                        flat.put("dynamicValueInherit", dynVal.get("inherit").asBoolean());
                    }
                }
            }
            if (predicate.has("ignoreCase")) {
                flat.put("ignoreCase", predicate.get("ignoreCase").asBoolean());
            }
        }
    }

    public static List<EntityKey> parseEntityKeys(String entityKeysJson) {
        if (isNullOrBlank(entityKeysJson)) {
            return null;
        }
        return JsonUtils.fromString(entityKeysJson, new TypeReference<>() {});
    }

}
