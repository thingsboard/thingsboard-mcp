<B>Entity Data Query (EDQ)</B>

Allows you to run complex queries over platform entities (devices, assets, customers, etc) based on the combination of a main entity filter and one or more key filters.
Returns a paginated result containing requested entity fields and the latest values of requested attributes and time-series keys.

**Query definition**

**Entity filter** is mandatory and defines generic search criteria. For example, "find all devices with profile 'Moisture Sensor'" 
or "Find all devices related to asset 'Building A'".

Optional **key filters** allow to filter results of the **entity filter** by complex criteria against main entity fields (name, label, type, etc.), attributes, and telemetry.
For example, "temperature > 20 or temperature< 10" or "name starts with 'T', and attribute 'model' is 'T1000', and time series field 'batteryLevel' > 40".

The **entity fields** and **latest values** contains list of entity fields and latest attribute/telemetry fields to fetch for each entity."

The **page link** contains information about the page to fetch and the sort ordering.

Example: find all devices whose temperature is higher than the value of the tenant attribute with key 'temperatureThreshold'.
```json
{
  "entityFilter": {
    "type": "entityType",
    "resolveMultiple": true,
    "entityType": "DEVICE"
  },
  "keyFilters": [
    {
      "key": {
        "type": "TIME_SERIES",
        "key": "temperature"
      },
      "valueType": "NUMERIC",
      "predicate": {
        "operation": "GREATER",
        "value": {
          "defaultValue": 0,
          "dynamicValue": {
            "sourceType": "CURRENT_USER",
            "sourceAttribute": "temperatureThreshold",
            "inherit": false
          }
        },
        "type": "NUMERIC"
      }
    }
  ],
  "entityFields": [
    {
      "type": "ENTITY_FIELD",
      "key": "name"
    }
  ]
}
```

