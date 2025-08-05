package org.thingsboard.ai.mcp.server.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.ai.mcp.server.rest.RestClientService;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;

import java.util.Collections;
import java.util.List;

import static org.thingsboard.ai.mcp.server.tools.ControllerConstants.ENTITY_DATA_QUERY_DESCRIPTION;

@Service
public class EntityQueryTools {

    @Autowired
    private RestClientService clientService;

    @Tool(description = ENTITY_DATA_QUERY_DESCRIPTION)
    public String findEntityDataByQuery(List<String> deviceTypes, String nameFilter, String latestKey, String latestKeysNumericFilterValue, String numericOperation, int pageSize, int page) {
        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(deviceTypes);
        filter.setDeviceNameFilter(nameFilter);

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"),
                EntityDataSortOrder.Direction.ASC
        );

        EntityDataPageLink pageLink = new EntityDataPageLink(pageSize, page, null, sortOrder);

        List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        List<EntityKey> latestValues = null;
        if (latestKey != null) {
            latestValues = List.of(new EntityKey(EntityKeyType.TIME_SERIES, latestKey));
        }

        List<KeyFilter> keyFilters = null;
        if (latestKey != null && latestKeysNumericFilterValue != null && numericOperation != null) {
            keyFilters = createNumericKeyFilters(latestKey, EntityKeyType.TIME_SERIES, NumericFilterPredicate.NumericOperation.valueOf(numericOperation), latestKeysNumericFilterValue);
        }

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }


    @Tool(description = ENTITY_DATA_QUERY_DESCRIPTION)
    public String findDevicesByTypeAndNumericTimeseriesKeyValue(List<String> deviceTypes, String nameFilter, String latestKey, String latestKeysNumericFilterValue, String numericOperation, int pageSize, int page) {
        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(deviceTypes);
        filter.setDeviceNameFilter(nameFilter);

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"),
                EntityDataSortOrder.Direction.ASC
        );

        EntityDataPageLink pageLink = new EntityDataPageLink(pageSize, page, null, sortOrder);

        List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        List<EntityKey> latestValues = null;
        if (latestKey != null) {
            latestValues = List.of(new EntityKey(EntityKeyType.TIME_SERIES, latestKey));
        }

        List<KeyFilter> keyFilters = null;
        if (latestKey != null && latestKeysNumericFilterValue != null && numericOperation != null) {
            keyFilters = createNumericKeyFilters(latestKey, EntityKeyType.TIME_SERIES, NumericFilterPredicate.NumericOperation.valueOf(numericOperation), latestKeysNumericFilterValue);
        }

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFilters);
        return JacksonUtil.toString(clientService.getClient().findEntityDataByQuery(query));
    }

    private List<KeyFilter> createNumericKeyFilters(String key, EntityKeyType keyType, NumericFilterPredicate.NumericOperation operation, String value) {
        KeyFilter filter = new KeyFilter();
        filter.setKey(new EntityKey(keyType, key));
        filter.setValueType(EntityKeyValueType.NUMERIC);

        NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromDouble(Double.parseDouble(value)));
        predicate.setOperation(operation);

        filter.setPredicate(predicate);
        return Collections.singletonList(filter);
    }
}
