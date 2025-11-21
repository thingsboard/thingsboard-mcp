# Entity Data Query (EDQ)

Entity Data Query allows you to run complex queries over platform entities (devices, assets, customers, etc.) and retrieve their data in a structured, paginated format.

**What you can do:**
- Find entities matching specific criteria
- Filter by entity properties, attributes, and telemetry values
- Fetch only the fields you need
- Sort and paginate results

---

## Quick Start Examples

### Example 1: Find all devices
Simplest query: get all devices with their names:

```json
{
  "entityFilter": {
    "type": "entityType",
    "entityType": "DEVICE"
  },
  "entityFields": [
    {"type": "ENTITY_FIELD", "key": "name"}
  ]
}
```

### Example 2: Find devices by temperature
Get devices where temperature > 25:

```json
{
  "entityFilter": {
    "type": "entityType",
    "entityType": "DEVICE"
  },
  "keyFilters": [
    {
      "key": {"type": "TIME_SERIES", "key": "temperature"},
      "valueType": "NUMERIC",
      "predicate": {
        "operation": "GREATER",
        "value": {"defaultValue": 25},
        "type": "NUMERIC"
      }
    }
  ],
  "entityFields": [
    {"type": "ENTITY_FIELD", "key": "name"}
  ],
  "latestValues": [
    {"type": "TIME_SERIES", "key": "temperature"}
  ]
}
```

### Example 3: Complex filter with multiple conditions
Devices where (temperature > 25 AND humidity < 60) OR batteryLevel < 20:

```json
{
  "entityFilter": {
    "type": "entityType",
    "entityType": "DEVICE"
  },
  "keyFilters": [
    {
      "key": {"type": "TIME_SERIES", "key": "temperature"},
      "valueType": "NUMERIC",
      "predicate": {
        "operation": "GREATER",
        "value": {"defaultValue": 25},
        "type": "NUMERIC"
      }
    },
    {
      "key": {"type": "TIME_SERIES", "key": "humidity"},
      "valueType": "NUMERIC",
      "predicate": {
        "operation": "LESS",
        "value": {"defaultValue": 60},
        "type": "NUMERIC"
      }
    }
  ]
}
```
*Note: Multiple keyFilters are combined with AND. For OR logic, use COMPLEX predicates (see KeyFilters guide).*

---

## Query Structure

An EDQ query consists of 5 parts:

```json
{
  "entityFilter": {},
  "keyFilters": [],
  "entityFields": [],
  "latestValues": [],
  "pageLink": {}
}
```

Where 'entityFilter' and 'pageLink' are required. 'keyFilters', 'entityFields', 'latestValues' are optional

### 1. Entity Filter (Required)

Defines **what entities** to search.

**Entity filter types:**

| Type                   | Purpose                                         | Example Use Case                                              |
|------------------------|-------------------------------------------------|---------------------------------------------------------------|
| `singleEntity`         | Find one specific entity                        | Single device by ID                                           |
| `entityGroup`          | Find all entities by entity group type and id   | All entities from Device group                                |
| `entityList`           | Find all entities by type and ids               | Devices with ids: 'ID1', 'ID2'                                |
| `entityGroupList`      | Find all groups by type and ids                 | Device groups with ids: 'ID1', 'ID2'                          |
| `entityGroupName`      | Find all groups by type and name pattern        | Device groups named "Sensor*"                                 |
| `entitiesByGroupName`  | Find all entities by group type and name        | Device groups with name "Water Meters"                        |
| `entityName`           | Find by name pattern                            | Devices named "Sensor*"                                       |
| `entityType`           | Find all entities of a type                     | All devices, all assets                                       |
| `stateEntityOwner`     | Find owner of the specified entity              | Owner of device with id 'ID'                                  |
| `deviceType`           | Find devices by device profile and name pattern | All "Temperature Sensor" devices named "Sensor*               |
| `assetType`            | Find assets by asset profile                    | All "Building" assets named "Sensor*                          |
| `assetType`            | Find edges by type                              | All "default" edges named "Edge*                              |
| `entityViewType`       | Find entity view by type                        | All "test" entity view named "EV*                             |
| `apiUsageState`        | Find api usage for customer                     | Api usage for customer with id 'ID'                           |
| `relationsQuery`       | Find related entities                           | All devices and assets related to Asset X                     |
| `assetSearchQuery`     | Find assets related to the root entity          | All assets related to Asset X using 'Contains' relation       |
| `deviceSearchQuery`    | Find devices related to the root entity         | All devices related to Device X using 'Contains' relation     |
| `entityViewSeachQuery` | Find entity view related to the root entity     | All entity views related to Asset X using 'Contains' relation |
| `edgeSeachQuery`       | Find entity view related to the root entity     | All edges related to Asset X using 'Contains' relation        |


**Example: All devices:**
```json
{
  "type": "entityType",
  "entityType": "DEVICE"
}
```

**Example - Single device:**
```json
{
  "type": "singleEntity",
  "singleEntity": {
    "id": "d521edb0-2a7a-11ec-94eb-213c95f54092",
    "entityType": "DEVICE"
  }
}
```

**Example - Devices of specific type:**
```json
{
  "type": "deviceType",
  "deviceType": "Temperature Sensor"
}
```

**Example - Find by name:**
```json
{
  "type": "entityName",
  "entityType": "DEVICE",
  "entityNameFilter": "Sensor"
}
```

### 2. Key Filters (Optional)

**Refines** the entity filter results by filtering on specific attribute or telemetry values.

⚠️ **Important:** Multiple keyFilters use **AND** logic. For OR/complex logic, see the KeyFilters guide.

**When to use:**
- Filter by telemetry values (temperature > 20)
- Filter by attributes (active = true)
- Filter by entity fields (name contains "sensor")

**Example - Temperature filter:**
```json
[
  {
    "key": {"type": "TIME_SERIES", "key": "temperature"},
    "valueType": "NUMERIC",
    "predicate": {
      "operation": "GREATER",
      "value": {"defaultValue": 20},
      "type": "NUMERIC"
    }
  }
]
```

📖 **For detailed keyFilter syntax, call `getKeyFiltersGuide()`**

### 3. Entity Fields (Optional)

Specifies **which entity properties** to include in the response.

**Available entity fields (vary by entity type):**
- `name` - Entity name
- `type` - Entity type
- `label` - Entity label
- `createdTime` - Creation timestamp
- `additionalInfo` - Additional metadata

**Example:**
```json
[
  {"type": "ENTITY_FIELD", "key": "name"},
  {"type": "ENTITY_FIELD", "key": "label"},
  {"type": "ENTITY_FIELD", "key": "createdTime"}
]
```

**If omitted:** No entity fields are returned (only ID and type).

### 4. Latest Values (Optional)

Specifies **which attributes and telemetry** to include in the response.

**Example - Fetch temperature and humidity:**
```json
[
  {"type": "TIME_SERIES", "key": "temperature"},
  {"type": "TIME_SERIES", "key": "humidity"}
]
```

**Example - Fetch attributes:**
```json
[
  {"type": "ATTRIBUTE", "key": "active"},
  {"type": "SERVER_ATTRIBUTE", "key": "configuration"}
]
```

**Key types:**
- `TIME_SERIES` - Telemetry values
- `ATTRIBUTE` - Any attribute
- `CLIENT_ATTRIBUTE` - Client attributes only
- `SHARED_ATTRIBUTE` - Shared attributes only
- `SERVER_ATTRIBUTE` - Server attributes only

**If omitted:** No attribute/telemetry values are returned.

### 5. Page Link (Required)

Controls **pagination and sorting**.

**Structure:**
```json
{
  "pageSize": 10,
  "page": 0,
  "textSearch": null,
  "sortOrder": {
    "key": {
      "type": "ENTITY_FIELD",
      "key": "createdTime"
    },
    "direction": "DESC"
  }
}
```

**Parameters:**

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `pageSize` | number | Results per page | 10, 50, 100 |
| `page` | number | Page number (0-indexed) | 0, 1, 2 |
| `textSearch` | string | Search in entity data | "sensor" |
| `sortOrder.key` | object | Field to sort by | `{"type": "ENTITY_FIELD", "key": "name"}` |
| `sortOrder.direction` | string | Sort direction | "ASC" or "DESC" |

---

## Complete Examples

### Use Case 1: Find offline devices

Devices that haven't reported in 24+ hours:

```json
{
  "entityFilter": {
    "type": "entityType",
    "entityType": "DEVICE"
  },
  "keyFilters": [
    {
      "key": {"type": "TIME_SERIES", "key": "lastActivityTime"},
      "valueType": "DATE_TIME",
      "predicate": {
        "operation": "LESS",
        "value": {"defaultValue": 1729036800000},
        "type": "NUMERIC"
      }
    }
  ],
  "entityFields": [
    {"type": "ENTITY_FIELD", "key": "name"},
    {"type": "ENTITY_FIELD", "key": "label"}
  ],
  "latestValues": [
    {"type": "TIME_SERIES", "key": "lastActivityTime"}
  ],
  "pageLink": {
    "pageSize": 50,
    "page": 0
  }
}
```

### Use Case 2: Active devices with low battery

Devices where active=true AND batteryLevel < 20:

```json
{
  "entityFilter": {
    "type": "entityType",
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
  ],
  "entityFields": [
    {"type": "ENTITY_FIELD", "key": "name"}
  ],
  "latestValues": [
    {"type": "TIME_SERIES", "key": "batteryLevel"}
  ]
}
```

### Use Case 3: Dynamic threshold from tenant attribute

Filter devices where temperature > tenant's configured threshold:

```json
{
  "entityFilter": {
    "type": "entityType",
    "entityType": "DEVICE"
  },
  "keyFilters": [
    {
      "key": {"type": "TIME_SERIES", "key": "temperature"},
      "valueType": "NUMERIC",
      "predicate": {
        "operation": "GREATER",
        "value": {
          "defaultValue": 0,
          "dynamicValue": {
            "sourceType": "CURRENT_TENANT",
            "sourceAttribute": "temperatureThreshold",
            "inherit": false
          }
        },
        "type": "NUMERIC"
      }
    }
  ],
  "entityFields": [
    {"type": "ENTITY_FIELD", "key": "name"}
  ],
  "latestValues": [
    {"type": "TIME_SERIES", "key": "temperature"}
  ]
}
```

### Use Case 4: Search devices by name

Find devices with "sensor" in the name:

```json
{
  "entityFilter": {
    "type": "entityType",
    "entityType": "DEVICE"
  },
  "entityFields": [
    {"type": "ENTITY_FIELD", "key": "name"},
    {"type": "ENTITY_FIELD", "key": "label"}
  ],
  "pageLink": {
    "pageSize": 20,
    "page": 0,
    "textSearch": "sensor"
  }
}
```

---

## Response Format

The query returns a paginated response:

```json
{
  "data": [
    {
      "entityId": {
        "id": "d521edb0-2a7a-11ec-94eb-213c95f54092",
        "entityType": "DEVICE"
      },
      "latest": {
        "TIME_SERIES": {
          "temperature": {
            "ts": 1634567890000,
            "value": "26.5"
          }
        }
      },
      "field": {
        "name": "Temperature Sensor 1",
        "label": "Room A"
      }
    }
  ],
  "totalPages": 5,
  "totalElements": 47,
  "hasNext": true
}
```

**Response fields:**
- `data[]` - Array of matching entities
    - `entityId` - Entity identifier
    - `latest` - Latest values (if requested)
    - `field` - Entity fields (if requested)
- `totalPages` - Total number of pages
- `totalElements` - Total matching entities
- `hasNext` - Whether more pages exist

---

## Common Patterns

### Pattern 1: Get all entities with specific fields
```json
{
  "entityFilter": {"type": "entityType", "entityType": "DEVICE"},
  "entityFields": [{"type": "ENTITY_FIELD", "key": "name"}],
  "pageLink": {"pageSize": 100, "page": 0}
}
```

### Pattern 2: Filter + fetch latest values
```json
{
  "entityFilter": {"type": "entityType", "entityType": "DEVICE"},
  "keyFilters": [],
  "latestValues": [{"type": "TIME_SERIES", "key": "temperature"}],
  "pageLink": {"pageSize": 50, "page": 0}
}
```

### Pattern 3: Search with sorting
```json
{
  "entityFilter": {"type": "entityType", "entityType": "DEVICE"},
  "entityFields": [{"type": "ENTITY_FIELD", "key": "name"}],
  "pageLink": {
    "pageSize": 20,
    "page": 0,
    "textSearch": "sensor",
    "sortOrder": {
      "key": {"type": "ENTITY_FIELD", "key": "createdTime"},
      "direction": "DESC"
    }
  }
}
```

## Quick Summary

```
┌─────────────────────────────────────────────────────────┐
│ EDQ Query Structure                                     │
├─────────────────────────────────────────────────────────┤
│ entityFilter    → What entities to search               │
│ keyFilters      → Filter by attributes/telemetry        │
│ entityFields    → Which entity fields to return         │
│ latestValues    → Which attributes/telemetry to return  │
│ pageLink        → Pagination and sorting                │
└─────────────────────────────────────────────────────────┘

Entity Filter Types:
  entityType     → All entities of type
  singleEntity   → One specific entity
  deviceType     → Devices by profile
  entityName     → Find by name pattern
  
Key Filter Structure:
  key           → {type, key}
  valueType     → NUMERIC | STRING | BOOLEAN | DATE_TIME
  predicate     → {operation, value, type}
  
Common Operations:
  NUMERIC:  GREATER, LESS, EQUAL, GREATER_OR_EQUAL, LESS_OR_EQUAL
  STRING:   CONTAINS, STARTS_WITH, ENDS_WITH, EQUAL
  BOOLEAN:  EQUAL, NOT_EQUAL
```