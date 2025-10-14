<B>Entity Сount Query</B>

Allows you to run complex queries to search the count of platform entities (devices, assets, customers, etc) based on the combination of a main entity filter and one or more key filters.
Returns the number of entities that match the query definition.

**Query definition**

**Entity filter** is mandatory and defines generic search criteria. For example, "find all devices with profile 'Moisture Sensor'"
or "Find all devices related to asset 'Building A'".

Optional **key filters** allow to filter results of the **entity filter** by complex criteria against main entity fields (name, label, type, etc.), attributes, and telemetry.
For example, "temperature > 20 or temperature< 10" or "name starts with 'T', and attribute 'model' is 'T1000', and time series field 'batteryLevel' > 40".

Example: find the number of devices whose temperature is higher than the value of the tenant attribute with key 'temperatureThreshold'.
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
  ]
}
```

