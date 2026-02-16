package org.thingsboard.ai.mcp.server.constant;

public class ControllerConstants {

    public static final String PE_ONLY_AVAILABLE = "Available only in Professional edition (PE). ";

    public static final String ENTITY_GROUP_ID_PARAM_DESCRIPTION = "A string value representing the Entity Group Id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    public static final String ENTITY_GROUP_ID_CREATE_PARAM_DESCRIPTION = "A string value representing the Entity Group Id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'. " +
            "If specified, the entity will be added to the corresponding entity group.";

    public static final String ENTITY_GROUP_IDS_CREATE_PARAM_DESCRIPTION = "A list of string values, separated by comma ',' representing the Entity Group Ids. For example, '784f394c-42b6-435a-983c-b7beff2784f9','a84f394c-42b6-435a-083c-b7beff2784f9'. " +
            "If specified, the entity will be added to the corresponding entity groups.";
    public static final String DEVICE_ID_PARAM_DESCRIPTION = "A string value representing the device id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";

    public static final String TENANT_ID_PARAM_DESCRIPTION = "A string value representing the tenant id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    public static final String CUSTOMER_ID_PARAM_DESCRIPTION = "A string value representing the customer id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    public static final String USER_ID_PARAM_DESCRIPTION = "A string value representing the user id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    public static final String ASSET_ID_PARAM_DESCRIPTION = "A string value representing the asset id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    public static final String ALARM_ID_PARAM_DESCRIPTION = "A string value representing the alarm id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    public static final String ENTITY_ID_PARAM_DESCRIPTION = "A string value representing the entity id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    public static final String ENTITY_TYPE_PARAM_DESCRIPTION = "A string value representing the entity type. For example, 'DEVICE'";

    public static final String PAGE_SIZE_DESCRIPTION = "Maximum amount of entities in a one page";
    public static final String PAGE_NUMBER_DESCRIPTION = "Sequence number of page starting from 0";
    public static final String DEVICE_TYPE_DESCRIPTION = "Device type as the name of the device profile";
    public static final String ASSET_TYPE_DESCRIPTION = "Asset type";
    public static final String ASSET_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the asset name.";
    public static final String DEVICE_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the device name.";

    public static final String CUSTOMER_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the customer title.";

    public static final String SORT_PROPERTY_DESCRIPTION = "Property of entity to sort by";
    public static final String SORT_ORDER_DESCRIPTION = "Sort order. ASC (ASCENDING) or DESC (DESCENDING)";

    public static final String DEVICE_NAME_DESCRIPTION = "A string value representing the Device name.";
    public static final String ASSET_NAME_DESCRIPTION = "A string value representing the Asset name.";

    public static final String RELATION_TYPE_PARAM_DESCRIPTION = "A string value representing relation type between entities. For example, 'Contains', 'Manages'. It can be any string value.";
    public static final String RELATION_TYPE_GROUP_PARAM_DESCRIPTION = "A string value representing relation type group. For example, 'COMMON'";

    public static final String SINGLE_ENTITY =
            "Filter by a single entity ID. JSON: {\"type\":\"singleEntity\",\"singleEntity\":{\"id\":\"<UUID>\",\"entityType\":\"DEVICE|ASSET|...\"}}";

    public static final String ENTITY_LIST =
            "Filter by multiple entity IDs. JSON: {\"type\":\"entityList\",\"entityType\":\"DEVICE\",\"entityList\":[\"<UUID1>\",\"<UUID2>\"]}";

    public static final String ENTITY_NAME =
            "Filter entities by name prefix ('starts with' match). JSON: {\"type\":\"entityName\",\"entityType\":\"DEVICE\",\"entityNameFilter\":\"<prefix>\"}";

    public static final String ENTITY_TYPE_FILTER =
            "Filter all entities of a specific type. JSON: {\"type\":\"entityType\",\"entityType\":\"DEVICE|ASSET|CUSTOMER|USER|DASHBOARD|ENTITY_VIEW|EDGE|TENANT\"}";

    public static final String ASSET_TYPE =
            "Filter assets by profile/type and optional name prefix. JSON: {\"type\":\"assetType\",\"assetType\":\"<profile>\",\"assetNameFilter\":\"<prefix>\"}";

    public static final String DEVICE_TYPE =
            "Filter devices by profile/type and optional name prefix. JSON: {\"type\":\"deviceType\",\"deviceType\":\"<profile>\",\"deviceNameFilter\":\"<prefix>\"}";

    public static final String EDGE_TYPE =
            "Filter edges by type and optional name prefix. JSON: {\"type\":\"edgeType\",\"edgeType\":\"<type>\",\"edgeNameFilter\":\"<prefix>\"}";

    public static final String ENTITY_VIEW_TYPE =
            "Filter entity views by type and optional name prefix. JSON: {\"type\":\"entityViewType\",\"entityViewType\":\"<type>\",\"entityViewNameFilter\":\"<prefix>\"}";

    public static final String API_USAGE =
            "Query API usage statistics. If customerId provided, returns customer's API usage; otherwise returns tenant API usage. " +
                    "JSON: {\"type\":\"apiUsageState\",\"customerId\":{\"id\":\"<UUID>\",\"entityType\":\"CUSTOMER\"}} - customerId optional";

    public static final String RELATIONS_QUERY_FILTER =
            "Query entities related to a root entity via relations. Direction: FROM=outgoing, TO=incoming relations. maxLevel: recursion depth (1=direct relations only). fetchLastLevelOnly: if true with maxLevel>1, returns only deepest level entities. " +
                    "JSON: {\"type\":\"relationsQuery\",\"rootEntity\":{\"entityType\":\"ASSET\",\"id\":\"<UUID>\"},\"direction\":\"FROM|TO\",\"maxLevel\":1,\"fetchLastLevelOnly\":false,\"filters\":[{\"relationType\":\"Contains\",\"entityTypes\":[\"DEVICE\",\"ASSET\"]}]}";

    public static final String ASSET_QUERY_FILTER =
            "Find assets related to a root entity via specific relation type and asset profiles. " +
                    "JSON: {\"type\":\"assetSearchQuery\",\"rootEntity\":{\"entityType\":\"ASSET\",\"id\":\"<UUID>\"},\"direction\":\"FROM|TO\",\"maxLevel\":1,\"fetchLastLevelOnly\":false,\"relationType\":\"Contains\",\"assetTypes\":[\"<profile>\"]}";

    public static final String DEVICE_QUERY_FILTER =
            "Find devices related to a root entity via specific relation type and device profiles. " +
                    "JSON: {\"type\":\"deviceSearchQuery\",\"rootEntity\":{\"entityType\":\"ASSET\",\"id\":\"<UUID>\"},\"direction\":\"FROM|TO\",\"maxLevel\":2,\"fetchLastLevelOnly\":true,\"relationType\":\"Contains\",\"deviceTypes\":[\"<profile>\"]}";


    public static final String EV_QUERY_FILTER =
            "Find entity views related to a root entity via specific relation type and entity view types. " +
                    "JSON: {\"type\":\"entityViewSearchQuery\",\"rootEntity\":{\"entityType\":\"ASSET\",\"id\":\"<UUID>\"},\"direction\":\"FROM|TO\",\"maxLevel\":1,\"fetchLastLevelOnly\":false,\"relationType\":\"Contains\",\"entityViewTypes\":[\"<type>\"]}";


    public static final String EDGE_QUERY_FILTER =
            "Find edge instances related to a root entity via specific relation type and edge types. " +
                    "JSON: {\"type\":\"edgeSearchQuery\",\"rootEntity\":{\"entityType\":\"ASSET\",\"id\":\"<UUID>\"},\"direction\":\"FROM|TO\",\"maxLevel\":2,\"fetchLastLevelOnly\":true,\"relationType\":\"Contains\",\"edgeTypes\":[\"<type>\"]}";


    public static final String ENTITY_GROUP_FILTER =
            "Filter entities belonging to a specific entity group by group ID. " +
                    "JSON: {\"type\":\"entityGroup\",\"groupType\":\"DEVICE|ASSET|...\",\"entityGroup\":\"<UUID>\"}";

    public static final String ENTITY_GROUP_LIST_FILTER =
            "Return multiple entity groups by their IDs. " +
                    "JSON: {\"type\":\"entityGroupList\",\"groupType\":\"DEVICE|ASSET|...\",\"entityGroupList\":[\"<UUID1>\",\"<UUID2>\"]}";

    public static final String ENTITY_GROUP_NAME_FILTER =
            "Filter entity groups by name prefix ('starts with' match). " +
                    "JSON: {\"type\":\"entityGroupName\",\"groupType\":\"DEVICE|ASSET|...\",\"entityGroupNameFilter\":\"<prefix>\"}";

    public static final String ENTITIES_BY_GROUP_NAME_FILTER =
            "Filter entities belonging to a group by group name. Optional ownerId to specify group owner (Tenant or Customer). " +
                    "JSON: {\"type\":\"entitiesByGroupName\",\"groupType\":\"DEVICE|ASSET|...\",\"entityGroupNameFilter\":\"<name>\",\"ownerId\":{\"entityType\":\"CUSTOMER\",\"id\":\"<UUID>\"}}";

    public static final String ENTITY_OWNER_FILTER =
            "Fetch the owner (Tenant or Customer) of a specified entity. " +
                    "JSON: {\"type\":\"stateEntityOwner\",\"singleEntity\":{\"id\":\"<UUID>\",\"entityType\":\"DEVICE|ASSET|...\"}}";

    public static final String KEY_FILTERS_JSON =
            "Optional JSON array string for filtering entities. Call getKeyFiltersGuide() for full details.\n\n" +
                    "Each filter requires: keyType, key, valueType, predicateType, operation, defaultValue.\n\n" +
                    "**Example** - temperature > 20:\n" +
                    "[{\"keyType\":\"TIME_SERIES\",\"key\":\"temperature\",\"valueType\":\"NUMERIC\",\"predicateType\":\"NUMERIC\",\"operation\":\"GREATER\",\"defaultValue\":20}]\n\n" +
                    "**Example** - name contains 'sensor':\n" +
                    "[{\"keyType\":\"ENTITY_FIELD\",\"key\":\"name\",\"valueType\":\"STRING\",\"predicateType\":\"STRING\",\"operation\":\"CONTAINS\",\"defaultValue\":\"sensor\"}]\n\n" +
                    "**keyType**: TIME_SERIES, ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, ENTITY_FIELD\n" +
                    "**valueType/predicateType**: NUMERIC, STRING, BOOLEAN, DATE_TIME\n" +
                    "**Numeric ops**: EQUAL, NOT_EQUAL, GREATER, LESS, GREATER_OR_EQUAL, LESS_OR_EQUAL\n" +
                    "**String ops**: EQUAL, NOT_EQUAL, STARTS_WITH, ENDS_WITH, CONTAINS, NOT_CONTAINS";

    public static final String ENTITY_FIELDS_JSON =
            "Optional JSON array string specifying which entity fields to return. Pass as a JSON string.\n\n" +
                    "**Example**: \"[{\\\"type\\\":\\\"ENTITY_FIELD\\\",\\\"key\\\":\\\"name\\\"},{\\\"type\\\":\\\"ENTITY_FIELD\\\",\\\"key\\\":\\\"type\\\"}]\"\n\n" +
                    "**Available ENTITY_FIELD keys**: name, type, label, createdTime, additionalInfo\n" +
                    "**Type values**: ENTITY_FIELD (for entity properties), ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES";

    public static final String LATEST_VALUES_JSON =
            "Optional JSON array string specifying which attributes or telemetry latest values to return. Pass as a JSON string.\n\n" +
                    "**Example for attributes**: \"[{\\\"type\\\":\\\"ATTRIBUTE\\\",\\\"key\\\":\\\"model\\\"},{\\\"type\\\":\\\"ATTRIBUTE\\\",\\\"key\\\":\\\"firmware\\\"}]\"\n\n" +
                    "**Example for telemetry**: \"[{\\\"type\\\":\\\"TIME_SERIES\\\",\\\"key\\\":\\\"temperature\\\"},{\\\"type\\\":\\\"TIME_SERIES\\\",\\\"key\\\":\\\"humidity\\\"}]\"\n\n" +
                    "**Type values**: ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, TIME_SERIES";

    public static final String ATTRIBUTES_SCOPE_DESCRIPTION = "A string value representing the attributes scope. For example, 'SERVER_SCOPE'.";
    public static final String ATTRIBUTES_KEYS_DESCRIPTION = "A string value representing the comma-separated list of attributes keys. For example, 'active,inactivityAlarmTime'.";
    public static final String ATTRIBUTES_JSON_REQUEST_DESCRIPTION = "A string value representing the json object. For example, '{\"key\":\"value\"}'. See API call description for more details.";

    public static final String TELEMETRY_KEYS_BASE_DESCRIPTION = "A string value representing the comma-separated list of telemetry keys.";
    public static final String TELEMETRY_KEYS_DESCRIPTION = TELEMETRY_KEYS_BASE_DESCRIPTION + " If keys are not selected, the result will return all latest time series. For example, 'temperature,humidity'.";
    public static final String TELEMETRY_JSON_REQUEST_DESCRIPTION = "A JSON with the telemetry values. See API call description for more details.";

    public static final String STRICT_DATA_TYPES_DESCRIPTION = "Enables/disables conversion of telemetry values to strings. Conversion is enabled by default. Set parameter to 'true' in order to disable the conversion.";

}
