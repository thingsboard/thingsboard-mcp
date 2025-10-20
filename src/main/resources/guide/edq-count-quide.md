# Entity Count Query (ECQ)

Entity Count Query returns the **number of entities** that match your filter criteria without fetching the actual entity data. Perfect for:
- Dashboard counters and statistics
- Checking if entities exist before querying
- Performance optimization (faster than full data queries)
- Monitoring and alerting based on counts

**Key Difference from EDQ:**
- **Entity Data Query (EDQ)**: Returns actual entity data (paginated)
- **Entity Count Query (ECQ)**: Returns only the count (single number)

---

## Quick Start Examples

### Example 1: Count all devices
```json
{
  "entityFilter": {
    "type": "entityType",
    "resolveMultiple": true,
    "entityType": "DEVICE"
  }
}
```
**Returns:** `{"count": 157}`

### Example 2: Count devices with high temperature
Devices where temperature > 30:
```json
{
  "entityFilter": {
    "type": "entityType",
    "resolveMultiple": true,
    "entityType": "DEVICE"
  },
  "keyFilters": [
    {
      "key": {"type": "TIME_SERIES", "key": "temperature"},
      "valueType": "NUMERIC",
      "predicate": {
        "operation": "GREATER",
        "value": {"defaultValue": 30},
        "type": "NUMERIC"
      }
    }
  ]
}
```
**Returns:** `{"count": 23}`

### Example 3: Count active devices with low battery
Active = true AND batteryLevel < 20:
```json
{
  "entityFilter": {
    "type": "entityType",
    "resolveMultiple": true,
    "entityType": "DEVICE"
  },
  "keyFilters": [
    {
      "key": {"type": "ATTRIBUTE", "key": "active"},
      "valueType": "BOOLEAN",
      "predicate": {
        "operation": "EQUAL",
        "value": {"defaultValue": true},
        "type": "BOOLEAN"
      }
    },
    {
      "key": {"type": "TIME_SERIES", "key": "batteryLevel"},
      "valueType": "NUMERIC",
      "predicate": {
        "operation": "LESS",
        "value": {"defaultValue": 20},
        "type": "NUMERIC"
      }
    }
  ]
}
```
**Returns:** `{"count": 7}`

### Example 4: Count by device profile
All "Temperature Sensor" devices:
```json
{
  "entityFilter": {
    "type": "deviceType",
    "resolveMultiple": true,
    "deviceType": "Temperature Sensor"
  }
}
```
**Returns:** `{"count": 45}`

---

## Query Structure

An Entity Count Query has only two parts (simpler than EDQ):

```json
{
  "entityFilter": {},
  "keyFilters": []
}
```

**What's NOT needed (compared to EDQ):**
- ❌ No `entityFields` - not fetching data
- ❌ No `latestValues` - not fetching data
- ❌ No `pageLink` - no pagination needed for count

---

## Part 1: Entity Filter (Required)

Same as Entity Data Query - defines **what entities** to count.

### Common Entity Filter Types

| Type | Purpose | Example |
|------|---------|---------|
| `entityType` | Count all of a type | All devices |
| `deviceType` | Count by device profile | All "Temperature Sensor" devices |
| `assetType` | Count by asset profile | All "Building" assets |
| `entityName` | Count by name pattern | Devices with "sensor" in name |
| `relationsQuery` | Count related entities | Devices related to Asset X |

**Entity filter types:**

| Type                   | Purpose                                          | Example Use Case                                                |
|------------------------|--------------------------------------------------|-----------------------------------------------------------------|
| `singleEntity`         | Count one specific entity                        | Single device by ID                                             |
| `entityGroup`          | Count all entities by entity group type and id   | Count entities from Device group                                |
| `entityList`           | Count all entities by type and ids               | Devices with ids: 'ID1', 'ID2'                                  |
| `entityGroupList`      | Count all groups by type and ids                 | Device groups with ids: 'ID1', 'ID2'                            |
| `entityGroupName`      | Count all groups by type and name pattern        | Device groups named "Sensor*"                                   |
| `entitiesByGroupName`  | Count all entities by group type and name        | Device groups with name "Water Meters"                          |
| `entityName`           | Count by name pattern                            | Devices named "Sensor*"                                         |
| `entityType`           | Count all entities of a type                     | Count devices, all assets                                       |
| `stateEntityOwner`     | Count owner of the specified entity              | Owner of device with id 'ID'                                    |
| `deviceType`           | Count devices by device profile and name pattern | Count "Temperature Sensor" devices named "Sensor*               |
| `assetType`            | Count assets by asset profile                    | Count "Building" assets named "Sensor*                          |
| `assetType`            | Count edges by type                              | Count "default" edges named "Edge*                              |
| `entityViewType`       | Count entity view by type                        | Count "test" entity view named "EV*                             |
| `apiUsageState`        | Count api usage for customer                     | Count usage for customer with id 'ID'                           |
| `relationsQuery`       | Count related entities                           | Count devices and assets related to Asset X                     |
| `assetSearchQuery`     | Count assets related to the root entity          | Count assets related to Asset X using 'Contains' relation       |
| `deviceSearchQuery`    | Count devices related to the root entity         | Count devices related to Device X using 'Contains' relation     |
| `entityViewSeachQuery` | Count entity view related to the root entity     | Count entity views related to Asset X using 'Contains' relation |
| `edgeSeachQuery`       | Count entity view related to the root entity     | Count edges related to Asset X using 'Contains' relation        |

### Examples

**Count all devices:**
```json
{
  "type": "entityType",
  "resolveMultiple": true,
  "entityType": "DEVICE"
}
```

**Count all assets:**
```json
{
  "type": "entityType",
  "resolveMultiple": true,
  "entityType": "ASSET"
}
```

**Count devices by profile:**
```json
{
  "type": "deviceType",
  "resolveMultiple": true,
  "deviceType": "Temperature Sensor"
}
```

**Count devices related to an asset:**
```json
{
  "type": "relationsQuery",
  "rootEntity": {
    "id": "abc123...",
    "entityType": "ASSET"
  },
  "direction": "FROM",
  "filters": [
    {
      "relationType": "Contains",
      "entityTypes": ["DEVICE"]
    }
  ]
}
```

---

## Part 2: Key Filters (Optional)

Same as Entity Data Query - filters entities by field/attribute/telemetry values.

**When to use:**
- Count entities matching specific conditions
- Filter by telemetry thresholds
- Filter by attribute values
- Complex business logic

### Examples

**Count by temperature threshold:**
```json
[
  {
    "key": {"type": "TIME_SERIES", "key": "temperature"},
    "valueType": "NUMERIC",
    "predicate": {
      "operation": "GREATER",
      "value": {"defaultValue": 25},
      "type": "NUMERIC"
    }
  }
]
```

**Count by multiple conditions (AND):**
```json
[
  {
    "key": {"type": "TIME_SERIES", "key": "temperature"},
    "valueType": "NUMERIC",
    "predicate": {"operation": "GREATER", "value": {"defaultValue": 30}, "type": "NUMERIC"}
  },
  {
    "key": {"type": "ATTRIBUTE", "key": "active"},
    "valueType": "BOOLEAN",
    "predicate": {"operation": "EQUAL", "value": {"defaultValue": true}, "type": "BOOLEAN"}
  }
]
```

📖 **For detailed keyFilter syntax, see:** `getKeyFiltersGuide()`

---

## Response Format

Entity Count Query returns a simple count:

```json
{
  "count": 42
}
```

**That's it!** Just a single number.

---

## Use Cases

### Use Case 1: Dashboard Statistics

**Scenario:** Show device status counts on dashboard

```json
{"entityFilter": {"type": "entityType", "resolveMultiple": true, "entityType": "DEVICE"}}
```
**Returns:** `{"count": 250}`

```json
{"entityFilter": {}, "keyFilters": [{"key": {"type": "ATTRIBUTE", "key": "active"}}]}
```
**Returns:** `{"count": 237}`

```json
{"entityFilter": {}, "keyFilters": [{"key": {"type": "TIME_SERIES", "key": "lastActivityTime"}}]}
```
**Returns:** `{"count": 13}`

### Use Case 2: Alert Conditions

**Scenario:** Trigger alert if more than 10 devices have a high temperature

```json
{
  "entityFilter": {
    "type": "entityType",
    "resolveMultiple": true,
    "entityType": "DEVICE"
  },
  "keyFilters": [
    {
      "key": {"type": "TIME_SERIES", "key": "temperature"},
      "valueType": "NUMERIC",
      "predicate": {"operation": "GREATER", "value": {"defaultValue": 80}, "type": "NUMERIC"}
    }
  ]
}
```
**Returns:** `{"count": 15}` → Trigger alert!

### Use Case 3: Pre-Query Validation

**Scenario:** Check if entities exist before running an expensive data query

```json
{
  "entityFilter": {
    "type": "deviceType",
    "resolveMultiple": true,
    "deviceType": "Temperature Sensor"
  },
  "keyFilters": []
}
```
**Returns:** `{"count": 1}` → Run full EDQ!

### Use Case 4: Monitoring Device Health

**Scenario:** Count devices needing attention

```json
{
  "entityFilter": {"type": "entityType", "resolveMultiple": true, "entityType": "DEVICE"},
  "keyFilters": [
    {"key": {"type": "TIME_SERIES", "key": "batteryLevel"}, "valueType": "NUMERIC", 
     "predicate": {"operation": "LESS", "value": {"defaultValue": 20}, "type": "NUMERIC"}}
  ]
}
```
**Returns:** `{"count": 8}`

```json 
{
  "entityFilter": {"type": "entityType", "resolveMultiple": true, "entityType": "DEVICE"},
  "keyFilters": [
    {"key": {"type": "ATTRIBUTE", "key": "errorState"}, "valueType": "BOOLEAN",
     "predicate": {"operation": "EQUAL", "value": {"defaultValue": true}, "type": "BOOLEAN"}}
  ]
}
```
**Returns:** `{"count": 3}`

### Use Case 5: Dynamic Threshold Monitoring

**Scenario:** Count devices exceeding tenant-configured threshold

```json
{
  "entityFilter": {
    "type": "entityType",
    "resolveMultiple": true,
    "entityType": "DEVICE"
  },
  "keyFilters": [
    {
      "key": {"type": "TIME_SERIES", "key": "temperature"},
      "valueType": "NUMERIC",
      "predicate": {
        "operation": "GREATER",
        "value": {
          "defaultValue": 25,
          "dynamicValue": {
            "sourceType": "CURRENT_TENANT",
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
**If tenant has `temperatureThreshold = 30`:** Returns count of devices > 30°  
**If attribute doesn't exist:** Uses defaultValue (25)

---

## Quick Reference Card

```
┌──────────────────────────────────────────────────────────┐
│ Entity Count Query Structure                             │
├──────────────────────────────────────────────────────────┤
│ {                                                        │
│   "entityFilter": { /* REQUIRED */ },                    │
│   "keyFilters": [ /* OPTIONAL */ ]                       │
│ }                                                        │
│                                                          │
│ Response: {"count": <number>}                            │
└──────────────────────────────────────────────────────────┘

Common Entity Filters:
  {type: "entityType", entityType: "DEVICE"}  → All devices
  {type: "deviceType", deviceType: "..."}     → By profile
  {type: "entityName", entityNameFilter: "..."} → By name

KeyFilters:
  Same as Entity Data Query (EDQ)
  See getKeyFiltersGuide() for details

Use Cases:
  ✅ Dashboard statistics
  ✅ Alert conditions (count > threshold)
  ✅ Existence checks
  ✅ Performance optimization
  
  ❌ Need entity data → Use EDQ instead
  ❌ Need entity names → Use EDQ instead
  ❌ Need telemetry values → Use EDQ instead

Performance:
  ECQ >> EDQ (much faster for counts)
```

## Summary

**Entity Count Query returns just a number - fast and efficient.**

Key points:
- ✅ Simpler than EDQ (no entityFields, latestValues, pageLink)
- ✅ Much faster than EDQ for counting
- ✅ Perfect for dashboards and statistics
- ✅ Same entityFilter and keyFilters as EDQ
- ✅ Returns: `{"count": <number>}`
