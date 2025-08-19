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

    public static TimePageLink createTimePageLink(int pageSize, int page, String textSearch, String sortProperty, String sortOrder, Long startTime, Long endTime) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return new TimePageLink(pageLink, startTime, endTime);
    }

    public static PageLink createPageLink(int pageSize, int page, String textSearch, String sortProperty, String sortOrder) throws ThingsboardException {
        if (StringUtils.isNotEmpty(sortProperty)) {
            if (!isValidProperty(sortProperty)) {
                throw new IllegalArgumentException("Invalid sort property");
            }
            SortOrder.Direction direction = SortOrder.Direction.ASC;
            if (StringUtils.isNotEmpty(sortOrder)) {
                try {
                    direction = SortOrder.Direction.valueOf(sortOrder.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new ThingsboardException("Unsupported sort order '" + sortOrder + "'! Only 'ASC' or 'DESC' types are allowed.", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
                }
            }
            SortOrder sort = new SortOrder(sortProperty, direction);
            return new PageLink(pageSize, page, textSearch, sort);
        } else {
            return new PageLink(pageSize, page, textSearch);
        }
    }

    public static EntityDataPageLink createPageLink(int pageSize, int page, String textSearch, String sortOrderKey, String sortOrderType, String sortOrder) throws ThingsboardException {
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

    public static boolean isValidProperty(String key) {
        return StringUtils.isEmpty(key) || RegexUtils.matches(key, PROPERTY_PATTERN);
    }

}
