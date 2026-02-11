package org.thingsboard.ai.mcp.server.constant;

public class ControllerConstants {

    public static final String PE_ONLY_AVAILABLE = "Available only in Professional edition (PE). ";
    public static final String NEW_LINE = "\n\n";

    public static final String PAGE_DATA_PARAMETERS = "You can specify parameters to filter the results. " +
            "The result is wrapped with PageData object that allows you to iterate over result set using pagination. " +
            "See response schema for more details. ";

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

    public static final String SYSTEM_AUTHORITY_PARAGRAPH = "\n\nAvailable for users with 'SYS_ADMIN' authority.";
    public static final String SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH = "\n\nAvailable for users with 'SYS_ADMIN' or 'TENANT_ADMIN' authority.";
    public static final String TENANT_AUTHORITY_PARAGRAPH = "\n\nAvailable for users with 'TENANT_ADMIN' authority.";
    public static final String TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH = "\n\nAvailable for users with 'TENANT_ADMIN' or 'CUSTOMER_USER' authority.";
    public static final String CUSTOMER_AUTHORITY_PARAGRAPH = "\n\nAvailable for users with 'CUSTOMER_USER' authority.";
    public static final String SYSTEM_OR_TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH = "\n\nAvailable for users with 'SYS_ADMIN' or 'TENANT_ADMIN' or 'CUSTOMER_USER' authority.";
    public static final String AVAILABLE_FOR_ANY_AUTHORIZED_USER = "\n\nAvailable for any authorized user. ";
    public static final String PAGE_SIZE_DESCRIPTION = "Maximum amount of entities in a one page";
    public static final String PAGE_NUMBER_DESCRIPTION = "Sequence number of page starting from 0";
    public static final String DEVICE_TYPE_DESCRIPTION = "Device type as the name of the device profile";
    public static final String ASSET_TYPE_DESCRIPTION = "Asset type";
    public static final String ASSET_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the asset name.";
    public static final String DEVICE_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the device name.";

    public static final String CUSTOMER_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the customer title.";

    public static final String SORT_PROPERTY_DESCRIPTION = "Property of entity to sort by";

    public static final String SORT_ORDER_DESCRIPTION = "Sort order. ASC (ASCENDING) or DESC (DESCENDING)";
    public static final String ALARM_INFO_DESCRIPTION = "Alarm Info is an extension of the default Alarm object that also contains name of the alarm originator.";
    public static final String RELATION_INFO_DESCRIPTION = "Relation Info is an extension of the default Relation object that contains information about the 'from' and 'to' entity names. ";

    public static final String DEVICE_NAME_DESCRIPTION = "A string value representing the Device name.";
    public static final String ASSET_NAME_DESCRIPTION = "A string value representing the Asset name.";

    public static final String MARKDOWN_CODE_BLOCK_START = "```json\n";
    public static final String MARKDOWN_CODE_BLOCK_END = "\n```";

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

    public static final String MAX_LEVEL_DESCRIPTION = "Possible direction values are 'TO' and 'FROM'. The 'maxLevel' defines how many relation levels should the query search 'recursively'. ";
    public static final String FETCH_LAST_LEVEL_ONLY_DESCRIPTION = "Assuming the 'maxLevel' is > 1, the 'fetchLastLevelOnly' defines either to return all related entities or only entities that are on the last level of relations. ";

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

    public static final String ENTITY_FILTERS =
            "\n\n # Entity Filters" +
                    "\nEntity Filter body depends on the 'type' parameter. Let's review available entity filter types. In fact, they do correspond to available dashboard aliases." +
                    SINGLE_ENTITY + ENTITY_GROUP_FILTER + ENTITY_LIST + ENTITY_NAME + ENTITY_TYPE_FILTER + ENTITY_GROUP_LIST_FILTER + ENTITY_GROUP_NAME_FILTER + ENTITIES_BY_GROUP_NAME_FILTER +
                    ENTITY_OWNER_FILTER + ASSET_TYPE + DEVICE_TYPE + EDGE_TYPE + ENTITY_VIEW_TYPE + API_USAGE + RELATIONS_QUERY_FILTER
                    + ASSET_QUERY_FILTER + DEVICE_QUERY_FILTER + EV_QUERY_FILTER + EDGE_QUERY_FILTER;

    public static final String FILTER_KEY = "\n\n## Filter Key\n\n" +
            "Filter Key defines either entity field, attribute or telemetry. It is a JSON object that consists the key name and type. " +
            "The following filter key types are supported: \n\n" +
            " * 'CLIENT_ATTRIBUTE' - used for client attributes; \n" +
            " * 'SHARED_ATTRIBUTE' - used for shared attributes; \n" +
            " * 'SERVER_ATTRIBUTE' - used for server attributes; \n" +
            " * 'ATTRIBUTE' - used for any of the above; \n" +
            " * 'TIME_SERIES' - used for time series values; \n" +
            " * 'ENTITY_FIELD' - used for accessing entity fields like 'name', 'label', etc. The list of available fields depends on the entity type; \n" +
            " * 'ALARM_FIELD' - similar to entity field, but is used in alarm queries only; \n" +
            "\n\n Let's review the example:\n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"TIME_SERIES\",\n" +
            "  \"key\": \"temperature\"\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "";

    public static final String FILTER_KEY_SUMMARY =
            """
                    **1. key** - Specifies what to filter:
                       • TIME_SERIES: telemetry data (e.g., temperature)
                       • ATTRIBUTE/CLIENT_ATTRIBUTE/SHARED_ATTRIBUTE/SERVER_ATTRIBUTE: entity attributes
                       • ENTITY_FIELD: entity properties (name, label, etc.)
                       • ALARM_FIELD: alarm properties
                       Example: {"type": "TIME_SERIES", "key": "temperature"}""";

    public static final String FILTER_VALUE_TYPE_SUMMARY =
            """
                    **2. valueType** - Data type (affects available operations):
                       • NUMERIC: numbers (ops: GREATER, LESS, EQUAL, etc.)
                       • STRING: text (ops: CONTAINS, STARTS_WITH, ENDS_WITH, etc.)
                       • BOOLEAN: true/false (ops: EQUAL, NOT_EQUAL)
                       • DATE_TIME: timestamps as milliseconds (ops: same as NUMERIC)""";

    public static final String FILTER_PREDICATE_SUMMARY =
            """
                    **3. predicate** - The condition to evaluate:
                       • Simple: {"operation": "GREATER", "value": {"defaultValue": 20}, "type": "NUMERIC"}
                       • Complex: Use "type": "COMPLEX" with "operation": "AND"/"OR" and nested predicates array
                       • Dynamic: Replace defaultValue with dynamicValue: {"sourceType": "CURRENT_USER", "sourceAttribute": "threshold"}""";

    public static final String KEY_FILTERS_JSON =
            "Optional JSON array string for filtering entities. Pass as a JSON string (not an object). " +
                    "Call getKeyFiltersGuide() for full structure details.\n\n" +
                    "Each filter object requires: 'key', 'valueType', and 'predicate'.\n\n" +
                    "**Quick Example** - Filter where temperature > 20:\n" +
                    "\"[{\\\"key\\\":{\\\"type\\\":\\\"TIME_SERIES\\\",\\\"key\\\":\\\"temperature\\\"},\\\"valueType\\\":\\\"NUMERIC\\\",\\\"predicate\\\":{\\\"operation\\\":\\\"GREATER\\\",\\\"value\\\":{\\\"defaultValue\\\":20},\\\"type\\\":\\\"NUMERIC\\\"}}]\"\n\n" +
                    "**Key types**: TIME_SERIES, ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE, SERVER_ATTRIBUTE, ENTITY_FIELD\n" +
                    "**Value types**: NUMERIC, STRING, BOOLEAN, DATE_TIME\n" +
                    "**Numeric operations**: EQUAL, NOT_EQUAL, GREATER, LESS, GREATER_OR_EQUAL, LESS_OR_EQUAL\n" +
                    "**String operations**: EQUAL, NOT_EQUAL, STARTS_WITH, ENDS_WITH, CONTAINS, NOT_CONTAINS";

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

    public static final String KEY_FILTERS =
            "COMPLEX STRUCTURE - Read carefully or call getKeyFiltersGuide() for full details!\n\n" +
                    "KeyFilter allows complex logical expressions over entity fields, attributes, or time-series values.\n" +
                    "Each filter has 3 required parts: 'key', 'valueType', and 'predicate'.\n" +
                    "Multiple filters use logical AND.\n\n" +
                    "## Quick Example - Temperature > 20\n" +
                    "```json\n" +
                    "[\n" +
                    "  {\n" +
                    "    \"key\": {\"type\": \"TIME_SERIES\", \"key\": \"temperature\"},\n" +
                    "    \"valueType\": \"NUMERIC\",\n" +
                    "    \"predicate\": {\n" +
                    "      \"operation\": \"GREATER\",\n" +
                    "      \"value\": {\n" +
                    "        \"defaultValue\": 20,\n" +
                    "        \"dynamicValue\": {\n" +
                    "          \"sourceType\": \"CURRENT_USER\",\n" +
                    "          \"sourceAttribute\": \"tempThreshold\",\n" +
                    "          \"inherit\": false\n" +
                    "        }\n" +
                    "      },\n" +
                    "      \"type\": \"NUMERIC\"\n" +
                    "    }\n" +
                    "  }\n" +
                    "]\n" +
                    "```\n" +
                    FILTER_KEY_SUMMARY + "\n" + FILTER_VALUE_TYPE_SUMMARY + "\n" + FILTER_PREDICATE_SUMMARY + "\n" +
                    "For complex predicates (OR/AND logic), nested conditions, and dynamic values → call getKeyFiltersGuide()";

    public static final String ENTITY_COUNT_QUERY_DESCRIPTION =
            "Allows to run complex queries to search the count of platform entities (devices, assets, customers, etc) " +
                    "based on the combination of main entity filter and multiple key filters. Returns the number of entities that match the query definition.\n\n" +
                    "# Query Definition\n\n" +
                    "\n\nMain **entity filter** is mandatory and defines generic search criteria. " +
                    "For example, \"find all devices with profile 'Moisture Sensor'\" or \"Find all devices related to asset 'Building A'\"" +
                    "\n\nOptional **key filters** allow to filter results of the entity filter by complex criteria against " +
                    "main entity fields (name, label, type, etc), attributes and telemetry. " +
                    "For example, \"temperature > 20 or temperature< 10\" or \"name starts with 'T', and attribute 'model' is 'T1000', and time series field 'batteryLevel' > 40\"." +
                    "\n\nLet's review the example:" +
                    "\n\n" + MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  \"entityFilter\": {\n" +
                    "    \"type\": \"entityType\",\n" +
                    "    \"entityType\": \"DEVICE\"\n" +
                    "  },\n" +
                    "  \"keyFilters\": [\n" +
                    "    {\n" +
                    "      \"key\": {\n" +
                    "        \"type\": \"ATTRIBUTE\",\n" +
                    "        \"key\": \"active\"\n" +
                    "      },\n" +
                    "      \"valueType\": \"BOOLEAN\",\n" +
                    "      \"predicate\": {\n" +
                    "        \"operation\": \"EQUAL\",\n" +
                    "        \"value\": {\n" +
                    "          \"defaultValue\": true,\n" +
                    "          \"dynamicValue\": null\n" +
                    "        },\n" +
                    "        \"type\": \"BOOLEAN\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}" +
                    MARKDOWN_CODE_BLOCK_END +
                    "\n\n Example mentioned above search all devices which have attribute 'active' set to 'true'. Now let's review available entity filters and key filters syntax:" +
                    ENTITY_FILTERS +
                    KEY_FILTERS +
                    ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

    public static final String ENTITY_DATA_QUERY_DESCRIPTION =
            "Allows to run complex queries over platform entities (devices, assets, customers, etc) " +
                    "based on the combination of main entity filter and multiple key filters. " +
                    "Returns the paginated result of the query that contains requested entity fields and latest values of requested attributes and time series data.\n\n" +
                    "# Query Definition\n\n" +
                    "\n\nMain **entity filter** is mandatory and defines generic search criteria. " +
                    "For example, \"find all devices with profile 'Moisture Sensor'\" or \"Find all devices related to asset 'Building A'\"" +
                    "\n\nOptional **key filters** allow to filter results of the **entity filter** by complex criteria against " +
                    "main entity fields (name, label, type, etc), attributes and telemetry. " +
                    "For example, \"temperature > 20 or temperature< 10\" or \"name starts with 'T', and attribute 'model' is 'T1000', and time series field 'batteryLevel' > 40\"." +
                    "\n\nThe **entity fields** and **latest values** contains list of entity fields and latest attribute/telemetry fields to fetch for each entity." +
                    "\n\nThe **page link** contains information about the page to fetch and the sort ordering." +
                    "\n\nLet's review the example:" +
                    "\n\n" + MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  \"entityFilter\": {\n" +
                    "    \"type\": \"entityType\",\n" +
                    "    \"resolveMultiple\": true,\n" +
                    "    \"entityType\": \"DEVICE\"\n" +
                    "  },\n" +
                    "  \"keyFilters\": [\n" +
                    "    {\n" +
                    "      \"key\": {\n" +
                    "        \"type\": \"TIME_SERIES\",\n" +
                    "        \"key\": \"temperature\"\n" +
                    "      },\n" +
                    "      \"valueType\": \"NUMERIC\",\n" +
                    "      \"predicate\": {\n" +
                    "        \"operation\": \"GREATER\",\n" +
                    "        \"value\": {\n" +
                    "          \"defaultValue\": 0,\n" +
                    "          \"dynamicValue\": {\n" +
                    "            \"sourceType\": \"CURRENT_USER\",\n" +
                    "            \"sourceAttribute\": \"temperatureThreshold\",\n" +
                    "            \"inherit\": false\n" +
                    "          }\n" +
                    "        },\n" +
                    "        \"type\": \"NUMERIC\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"entityFields\": [\n" +
                    "    {\n" +
                    "      \"type\": \"ENTITY_FIELD\",\n" +
                    "      \"key\": \"name\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"type\": \"ENTITY_FIELD\",\n" +
                    "      \"key\": \"label\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"type\": \"ENTITY_FIELD\",\n" +
                    "      \"key\": \"additionalInfo\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"latestValues\": [\n" +
                    "    {\n" +
                    "      \"type\": \"ATTRIBUTE\",\n" +
                    "      \"key\": \"model\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"type\": \"TIME_SERIES\",\n" +
                    "      \"key\": \"temperature\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"pageLink\": {\n" +
                    "    \"page\": 0,\n" +
                    "    \"pageSize\": 10,\n" +
                    "    \"sortOrder\": {\n" +
                    "      \"key\": {\n" +
                    "        \"key\": \"name\",\n" +
                    "        \"type\": \"ENTITY_FIELD\"\n" +
                    "      },\n" +
                    "      \"direction\": \"ASC\"\n" +
                    "    }\n" +
                    "  }\n" +
                    "}" +
                    MARKDOWN_CODE_BLOCK_END +
                    "\n\n Example mentioned above search all devices which have attribute 'active' set to 'true'. Now let's review available entity filters and key filters syntax:" +
                    ENTITY_FILTERS +
                    KEY_FILTERS +
                    ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

    public static final String ALARM_DATA_QUERY_DESCRIPTION = "This method description defines how Alarm Data Query extends the Entity Data Query. " +
            "See method 'Find Entity Data by Query' first to get the info about 'Entity Data Query'." +
            "\n\n The platform will first search the entities that match the entity and key filters. Then, the platform will use 'Alarm Page Link' to filter the alarms related to those entities. " +
            "Finally, platform fetch the properties of alarm that are defined in the **'alarmFields'** and combine them with the other entity, attribute and latest time series fields to return the result. " +
            "\n\n See example of the alarm query below. The query will search first 100 active alarms with type 'Temperature Alarm' or 'Fire Alarm' for any device with current temperature > 0. " +
            "The query will return combination of the entity fields: name of the device, device model and latest temperature reading and alarms fields: createdTime, type, severity and status: " +
            "\n\n" + MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"entityFilter\": {\n" +
            "    \"type\": \"entityType\",\n" +
            "    \"resolveMultiple\": true,\n" +
            "    \"entityType\": \"DEVICE\"\n" +
            "  },\n" +
            "  \"pageLink\": {\n" +
            "    \"page\": 0,\n" +
            "    \"pageSize\": 100,\n" +
            "    \"textSearch\": null,\n" +
            "    \"searchPropagatedAlarms\": false,\n" +
            "    \"statusList\": [\n" +
            "      \"ACTIVE\"\n" +
            "    ],\n" +
            "    \"severityList\": [\n" +
            "      \"CRITICAL\",\n" +
            "      \"MAJOR\"\n" +
            "    ],\n" +
            "    \"typeList\": [\n" +
            "      \"Temperature Alarm\",\n" +
            "      \"Fire Alarm\"\n" +
            "    ],\n" +
            "    \"sortOrder\": {\n" +
            "      \"key\": {\n" +
            "        \"key\": \"createdTime\",\n" +
            "        \"type\": \"ALARM_FIELD\"\n" +
            "      },\n" +
            "      \"direction\": \"DESC\"\n" +
            "    },\n" +
            "    \"timeWindow\": 86400000\n" +
            "  },\n" +
            "  \"keyFilters\": [\n" +
            "    {\n" +
            "      \"key\": {\n" +
            "        \"type\": \"TIME_SERIES\",\n" +
            "        \"key\": \"temperature\"\n" +
            "      },\n" +
            "      \"valueType\": \"NUMERIC\",\n" +
            "      \"predicate\": {\n" +
            "        \"operation\": \"GREATER\",\n" +
            "        \"value\": {\n" +
            "          \"defaultValue\": 0,\n" +
            "          \"dynamicValue\": null\n" +
            "        },\n" +
            "        \"type\": \"NUMERIC\"\n" +
            "      }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"alarmFields\": [\n" +
            "    {\n" +
            "      \"type\": \"ALARM_FIELD\",\n" +
            "      \"key\": \"createdTime\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"ALARM_FIELD\",\n" +
            "      \"key\": \"type\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"ALARM_FIELD\",\n" +
            "      \"key\": \"severity\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"ALARM_FIELD\",\n" +
            "      \"key\": \"status\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"entityFields\": [\n" +
            "    {\n" +
            "      \"type\": \"ENTITY_FIELD\",\n" +
            "      \"key\": \"name\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"latestValues\": [\n" +
            "    {\n" +
            "      \"type\": \"ATTRIBUTE\",\n" +
            "      \"key\": \"model\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"TIME_SERIES\",\n" +
            "      \"key\": \"temperature\"\n" +
            "    }\n" +
            "  ]\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "";

    public static final String ALARM_FILTER_KEY = "## Alarm Filter Key" + NEW_LINE +
            "Filter Key defines either entity field, attribute, telemetry or constant. It is a JSON object that consists the key name and type. The following filter key types are supported:\n" +
            " * 'ATTRIBUTE' - used for attributes values;\n" +
            " * 'TIME_SERIES' - used for time series values;\n" +
            " * 'ENTITY_FIELD' - used for accessing entity fields like 'name', 'label', etc. The list of available fields depends on the entity type;\n" +
            " * 'CONSTANT' - constant value specified." + NEW_LINE + "Let's review the example:" + NEW_LINE +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"TIME_SERIES\",\n" +
            "  \"key\": \"temperature\"\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END;

    public static final String DEVICE_PROFILE_FILTER_PREDICATE = NEW_LINE + "## Filter Predicate" + NEW_LINE +
            "Filter Predicate defines the logical expression to evaluate. The list of available operations depends on the filter value type, see above. " +
            "Platform supports 4 predicate types: 'STRING', 'NUMERIC', 'BOOLEAN' and 'COMPLEX'. The last one allows to combine multiple operations over one filter key." + NEW_LINE +
            "Simple predicate example to check 'value < 100': " + NEW_LINE +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"operation\": \"LESS\",\n" +
            "  \"value\": {\n" +
            "    \"userValue\": null,\n" +
            "    \"defaultValue\": 100,\n" +
            "    \"dynamicValue\": null\n" +
            "  },\n" +
            "  \"type\": \"NUMERIC\"\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END + NEW_LINE +
            "Complex predicate example, to check 'value < 10 or value > 20': " + NEW_LINE +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"COMPLEX\",\n" +
            "  \"operation\": \"OR\",\n" +
            "  \"predicates\": [\n" +
            "    {\n" +
            "      \"operation\": \"LESS\",\n" +
            "      \"value\": {\n" +
            "        \"userValue\": null,\n" +
            "        \"defaultValue\": 10,\n" +
            "        \"dynamicValue\": null\n" +
            "      },\n" +
            "      \"type\": \"NUMERIC\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"operation\": \"GREATER\",\n" +
            "      \"value\": {\n" +
            "        \"userValue\": null,\n" +
            "        \"defaultValue\": 20,\n" +
            "        \"dynamicValue\": null\n" +
            "      },\n" +
            "      \"type\": \"NUMERIC\"\n" +
            "    }\n" +
            "  ]\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END + NEW_LINE +
            "More complex predicate example, to check 'value < 10 or (value > 50 && value < 60)': " + NEW_LINE +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"COMPLEX\",\n" +
            "  \"operation\": \"OR\",\n" +
            "  \"predicates\": [\n" +
            "    {\n" +
            "      \"operation\": \"LESS\",\n" +
            "      \"value\": {\n" +
            "        \"userValue\": null,\n" +
            "        \"defaultValue\": 10,\n" +
            "        \"dynamicValue\": null\n" +
            "      },\n" +
            "      \"type\": \"NUMERIC\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"COMPLEX\",\n" +
            "      \"operation\": \"AND\",\n" +
            "      \"predicates\": [\n" +
            "        {\n" +
            "          \"operation\": \"GREATER\",\n" +
            "          \"value\": {\n" +
            "            \"userValue\": null,\n" +
            "            \"defaultValue\": 50,\n" +
            "            \"dynamicValue\": null\n" +
            "          },\n" +
            "          \"type\": \"NUMERIC\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"operation\": \"LESS\",\n" +
            "          \"value\": {\n" +
            "            \"userValue\": null,\n" +
            "            \"defaultValue\": 60,\n" +
            "            \"dynamicValue\": null\n" +
            "          },\n" +
            "          \"type\": \"NUMERIC\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END + NEW_LINE +
            "You may also want to replace hardcoded values (for example, temperature > 20) with the more dynamic " +
            "expression (for example, temperature > value of the tenant attribute with key 'temperatureThreshold'). " +
            "It is possible to use 'dynamicValue' to define attribute of the tenant, customer or device. " +
            "See example below:" + NEW_LINE +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"operation\": \"GREATER\",\n" +
            "  \"value\": {\n" +
            "    \"userValue\": null,\n" +
            "    \"defaultValue\": 0,\n" +
            "    \"dynamicValue\": {\n" +
            "      \"inherit\": false,\n" +
            "      \"sourceType\": \"CURRENT_TENANT\",\n" +
            "      \"sourceAttribute\": \"temperatureThreshold\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"type\": \"NUMERIC\"\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END + NEW_LINE +
            "Note that you may use 'CURRENT_DEVICE', 'CURRENT_CUSTOMER' and 'CURRENT_TENANT' as a 'sourceType'. The 'defaultValue' is used when the attribute with such a name is not defined for the chosen source. " +
            "The 'sourceAttribute' can be inherited from the owner of the specified 'sourceType' if 'inherit' is set to true.";

    public static final String ATTRIBUTES_SCOPE_DESCRIPTION = "A string value representing the attributes scope. For example, 'SERVER_SCOPE'.";
    public static final String ATTRIBUTES_KEYS_DESCRIPTION = "A string value representing the comma-separated list of attributes keys. For example, 'active,inactivityAlarmTime'.";
    public static final String ATTRIBUTES_JSON_REQUEST_DESCRIPTION = "A string value representing the json object. For example, '{\"key\":\"value\"}'. See API call description for more details.";

    public static final String TELEMETRY_KEYS_BASE_DESCRIPTION = "A string value representing the comma-separated list of telemetry keys.";
    public static final String TELEMETRY_KEYS_DESCRIPTION = TELEMETRY_KEYS_BASE_DESCRIPTION + " If keys are not selected, the result will return all latest time series. For example, 'temperature,humidity'.";
    public static final String TELEMETRY_JSON_REQUEST_DESCRIPTION = "A JSON with the telemetry values. See API call description for more details.";

    public static final String STRICT_DATA_TYPES_DESCRIPTION = "Enables/disables conversion of telemetry values to strings. Conversion is enabled by default. Set parameter to 'true' in order to disable the conversion.";
    public static final String INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION = "Referencing a non-existing entity Id or invalid entity type will cause an error. ";

    public static final String ENTITY_ATTRIBUTE_SCOPES_TEMPLATE = " List of possible attribute scopes depends on the entity type: " +
            "\n\n * SERVER_SCOPE - supported for all entity types;" +
            "\n * SHARED_SCOPE - supported for devices";
    public static final String ENTITY_SAVE_ATTRIBUTE_SCOPES = ENTITY_ATTRIBUTE_SCOPES_TEMPLATE + ".\n\n";
    public static final String ENTITY_GET_ATTRIBUTE_SCOPES = ENTITY_ATTRIBUTE_SCOPES_TEMPLATE +
            ";\n * CLIENT_SCOPE - supported for devices. " + "\n\n";

    public static final String ATTRIBUTE_DATA_EXAMPLE = """
            [
              {"key": "stringAttributeKey", "value": "value", "lastUpdateTs": 1609459200000},
              {"key": "booleanAttributeKey", "value": false, "lastUpdateTs": 1609459200001},
              {"key": "doubleAttributeKey", "value": 42.2, "lastUpdateTs": 1609459200002},
              {"key": "longKeyExample", "value": 73, "lastUpdateTs": 1609459200003},
              {"key": "jsonKeyExample",
                "value": {
                  "someNumber": 42,
                  "someArray": [1,2,3],
                  "someNestedObject": {"key": "value"}
                },
                "lastUpdateTs": 1609459200004
              }
            ]""";

    public static final String LATEST_TS_STRICT_DATA_EXAMPLE = """
            {
              "stringTsKey": [{ "value": "value", "ts": 1609459200000}],
              "booleanTsKey": [{ "value": false, "ts": 1609459200000}],
              "doubleTsKey": [{ "value": 42.2, "ts": 1609459200000}],
              "longTsKey": [{ "value": 73, "ts": 1609459200000}],
              "jsonTsKey": [{\s
                "value": {
                  "someNumber": 42,
                  "someArray": [1,2,3],
                  "someNestedObject": {"key": "value"}
                },\s
                "ts": 1609459200000}]
            }
            """;

    public static final String LATEST_TS_NON_STRICT_DATA_EXAMPLE = """
            {
              "stringTsKey": [{ "value": "value", "ts": 1609459200000}],
              "booleanTsKey": [{ "value": "false", "ts": 1609459200000}],
              "doubleTsKey": [{ "value": "42.2", "ts": 1609459200000}],
              "longTsKey": [{ "value": "73", "ts": 1609459200000}],
              "jsonTsKey": [{ "value": "{\\"someNumber\\": 42,\\"someArray\\": [1,2,3],\\"someNestedObject\\": {\\"key\\": \\"value\\"}}", "ts": 1609459200000}]
            }
            """;

    public static final String SAVE_ATTRIBUTES_REQUEST_PAYLOAD = "The request payload is a JSON object with key-value format of attributes to create or update. " +
            "For example:\n\n"
            + MARKDOWN_CODE_BLOCK_START
            + "{\n" +
            " \"stringKey\":\"value1\", \n" +
            " \"booleanKey\":true, \n" +
            " \"doubleKey\":42.0, \n" +
            " \"longKey\":73, \n" +
            " \"jsonKey\": {\n" +
            "    \"someNumber\": 42,\n" +
            "    \"someArray\": [1,2,3],\n" +
            "    \"someNestedObject\": {\"key\": \"value\"}\n" +
            " }\n" +
            "}"
            + MARKDOWN_CODE_BLOCK_END + "\n";

    public static final String SAVE_TIMESERIES_REQUEST_PAYLOAD = "The request payload is a JSON document with three possible formats:\n\n" +
            "Simple format without timestamp. In such a case, current server time will be used: \n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\"temperature\": 26}" +
            MARKDOWN_CODE_BLOCK_END +
            "\n\n Single JSON object with timestamp: \n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\"ts\":1634712287000,\"values\":{\"temperature\":26, \"humidity\":87}}" +
            MARKDOWN_CODE_BLOCK_END +
            "\n\n JSON array with timestamps: \n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "[{\"ts\":1634712287000,\"values\":{\"temperature\":26, \"humidity\":87}}, {\"ts\":1634712588000,\"values\":{\"temperature\":25, \"humidity\":88}}]" +
            MARKDOWN_CODE_BLOCK_END;

    public static final String RBAC_GROUP_READ_CHECK = " Security check is performed to verify that the user has 'READ' permission for specified group.";
    public static final String RBAC_GROUP_WRITE_CHECK = " Security check is performed to verify that the user has 'WRITE' permission for specified group.";
    public static final String RBAC_GROUP_DELETE_CHECK = " Security check is performed to verify that the user has 'DELETE' permission for specified group.";
    public static final String RBAC_GROUP_ADD_CHECK = " Security check is performed to verify that the user has 'ADD_TO_GROUP' permission for specified group.";
    public static final String RBAC_GROUP_REMOVE_CHECK = " Security check is performed to verify that the user has 'REMOVE_FROM_GROUP' permission for specified group.";

    public static final String RBAC_READ_CHECK = " Security check is performed to verify that the user has 'READ' permission for the entity (entities).";
    public static final String RBAC_WRITE_CHECK = " Security check is performed to verify that the user has 'WRITE' permission for the entity (entities).";
    public static final String RBAC_DELETE_CHECK = " Security check is performed to verify that the user has 'DELETE' permission for the entity (entities).";

}
