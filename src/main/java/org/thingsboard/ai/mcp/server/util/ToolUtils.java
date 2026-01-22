package org.thingsboard.ai.mcp.server.util;

import org.thingsboard.common.util.RegexUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;

import java.util.Arrays;
import java.util.regex.Pattern;

public class ToolUtils {

    public static final Pattern PROPERTY_PATTERN = Pattern.compile("^[\\p{L}0-9_-]+$");
    public static final int PAGE_SIZE = 10;
    public static final int PAGE_NUMBER = 0;

    public static TimePageLink createTimePageLink(String pageSize, String page, String textSearch, String sortProperty, String sortOrder, String startTimeStr, String endTimeStr) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        Long startTime = parseLong(startTimeStr);
        Long endTime = parseLong(endTimeStr);
        return new TimePageLink(pageLink, startTime, endTime);
    }

    public static PageLink createPageLink(String pageSizeStr, String pageStr, String textSearch, String sortProperty, String sortOrder) throws ThingsboardException {
        final int pageSize = sanitizePageSize(parseIntOrDefault(pageSizeStr, PAGE_SIZE));
        final int page = sanitizePageNumber(parseIntOrDefault(pageStr, PAGE_NUMBER));
        final String sanitizedTextSearch = sanitizeStringParam(textSearch);
        final String sanitizedSortProperty = sanitizeStringParam(sortProperty);

        if (StringUtils.isBlank(sanitizedSortProperty)) {
            return new PageLink(pageSize, page, sanitizedTextSearch);
        }

        if (!isValidProperty(sanitizedSortProperty)) {
            throw new IllegalArgumentException("Invalid sort property");
        }

        final SortOrder.Direction direction = resolveSortDirection(sanitizeStringParam(sortOrder));
        final SortOrder sort = new SortOrder(sanitizedSortProperty, direction);
        return new PageLink(pageSize, page, sanitizedTextSearch, sort);
    }

    public static String sanitizeStringParam(String value) {
        if (isNullOrBlank(value)) {
            return null;
        }
        return value.trim();
    }

    public static EntityDataPageLink createPageLink(String pageSizeStr, String pageStr, String textSearch, String sortOrderKey, String sortOrderType, String sortOrder) throws ThingsboardException {
        final int pageSize = sanitizePageSize(parseIntOrDefault(pageSizeStr, PAGE_SIZE));
        final int page = sanitizePageNumber(parseIntOrDefault(pageStr, PAGE_NUMBER));
        // Sanitize string parameters - LLMs may send "null" strings
        final String sanitizedTextSearch = sanitizeStringParam(textSearch);
        final String sanitizedSortOrderKey = sanitizeStringParam(sortOrderKey);
        final String sanitizedSortOrderType = sanitizeStringParam(sortOrderType);
        final String sanitizedSortOrder = sanitizeStringParam(sortOrder);

        EntityKey entityKey = null;
        if (StringUtils.isNotEmpty(sanitizedSortOrderKey) && StringUtils.isNotEmpty(sanitizedSortOrderType)) {
            try {
                EntityKeyType type = EntityKeyType.valueOf(sanitizedSortOrderType);
                entityKey = new EntityKey(type, sanitizedSortOrderKey);
            } catch (IllegalArgumentException e) {
                throw new ThingsboardException("Unsupported entity key type '" + sanitizedSortOrderType + "'! Only " + Arrays.toString(EntityKeyType.values()) + " are allowed. ", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
        }
        EntityDataSortOrder.Direction direction = EntityDataSortOrder.Direction.ASC;
        if (StringUtils.isNotEmpty(sanitizedSortOrder)) {
            try {
                direction = EntityDataSortOrder.Direction.valueOf(sanitizedSortOrder.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ThingsboardException("Unsupported sort order '" + sanitizedSortOrder + "'! Only 'ASC' or 'DESC' types are allowed.", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
        }

        if (entityKey == null) {
            return new EntityDataPageLink(pageSize, page, sanitizedTextSearch, null);
        }

        EntityDataSortOrder entityDataSortOrder = new EntityDataSortOrder(entityKey, direction);
        return new EntityDataPageLink(pageSize, page, sanitizedTextSearch, entityDataSortOrder);
    }

    private static int sanitizePageSize(int value) {
        return value > 0 ? value : PAGE_SIZE;
    }

    private static int sanitizePageNumber(int value) {
        return Math.max(value, 0);
    }

    private static SortOrder.Direction resolveSortDirection(String sortOrder) throws ThingsboardException {
        if (StringUtils.isBlank(sortOrder)) {
            return SortOrder.Direction.ASC;
        }
        try {
            return SortOrder.Direction.valueOf(sortOrder.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ThingsboardException(
                    "Unsupported sort order '" + sortOrder + "'! Only 'ASC' or 'DESC' types are allowed.",
                    ThingsboardErrorCode.BAD_REQUEST_PARAMS
            );
        }
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

    private static boolean isValidProperty(String key) {
        return StringUtils.isEmpty(key) || RegexUtils.matches(key, PROPERTY_PATTERN);
    }

}
