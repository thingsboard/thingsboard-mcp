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

        if (StringUtils.isBlank(sortProperty)) {
            return new PageLink(pageSize, page, textSearch);
        }

        if (!isValidProperty(sortProperty)) {
            throw new IllegalArgumentException("Invalid sort property");
        }

        final SortOrder.Direction direction = resolveSortDirection(sortOrder);
        final SortOrder sort = new SortOrder(sortProperty, direction);
        return new PageLink(pageSize, page, textSearch, sort);
    }

    public static EntityDataPageLink createPageLink(String pageSizeStr, String pageStr, String textSearch, String sortOrderKey, String sortOrderType, String sortOrder) throws ThingsboardException {
        final int pageSize = sanitizePageSize(parseIntOrDefault(pageSizeStr, PAGE_SIZE));
        final int page = sanitizePageNumber(parseIntOrDefault(pageStr, PAGE_NUMBER));
        EntityKey entityKey = null;
        if (StringUtils.isNotEmpty(sortOrderKey) && StringUtils.isNotEmpty(sortOrderType)) {
            try {
                EntityKeyType type = EntityKeyType.valueOf(sortOrderType);
                entityKey = new EntityKey(type, sortOrderKey);
            } catch (IllegalArgumentException e) {
                throw new ThingsboardException("Unsupported entity key type '" + sortOrderType + "'! Only " + Arrays.toString(EntityKeyType.values()) + " are allowed. ", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
        }
        EntityDataSortOrder.Direction direction = EntityDataSortOrder.Direction.ASC;
        if (StringUtils.isNotEmpty(sortOrder)) {
            try {
                direction = EntityDataSortOrder.Direction.valueOf(sortOrder.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ThingsboardException("Unsupported sort order '" + sortOrder + "'! Only 'ASC' or 'DESC' types are allowed.", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
        }

        if (entityKey == null) {
            return new EntityDataPageLink(pageSize, page, textSearch, null);
        }

        EntityDataSortOrder entityDataSortOrder = new EntityDataSortOrder(entityKey, direction);
        return new EntityDataPageLink(pageSize, page, textSearch, entityDataSortOrder);
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
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    public static Integer parseIntOrDefault(String candidate, Integer defaultValue) {
        try {
            return Integer.parseInt(candidate);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    public static Long parseLong(String value) {
        return parseLong(value, null);
    }

    private static boolean isValidProperty(String key) {
        return StringUtils.isEmpty(key) || RegexUtils.matches(key, PROPERTY_PATTERN);
    }

}
