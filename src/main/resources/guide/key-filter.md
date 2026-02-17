# KeyFilter

KeyFilter allows you to define complex logical expressions to filter entities based on their:

- **Entity fields** (name, label, type, etc.)
- **Attributes** (client, shared, server)
- **Time-series values** (telemetry)

Multiple filters are combined with **AND** logic by default.

---

## Quick Start

### Pattern 1: Simple threshold check

Temperature > 25:

```json
[
    {
        "keyType": "TIME_SERIES",
        "key": "temperature",
        "valueType": "NUMERIC",
        "predicateType": "NUMERIC",
        "operation": "GREATER",
        "defaultValue": 25
    }
]
```

### Pattern 2: Range check (between X and Y)

Temperature between 20 and 30:

```json
[
    {
        "keyType": "TIME_SERIES",
        "key": "temperature",
        "valueType": "NUMERIC",
        "predicateType": "COMPLEX",
        "complexOperation": "AND",
        "nestedPredicates": [
            {
                "predicateType": "NUMERIC",
                "operation": "GREATER_OR_EQUAL",
                "defaultValue": 20
            },
            {
                "predicateType": "NUMERIC",
                "operation": "LESS_OR_EQUAL",
                "defaultValue": 30
            }
        ]
    }
]
```

### Pattern 3: String contains

Name contains "sensor":

```json
[
    {
        "keyType": "ENTITY_FIELD",
        "key": "name",
        "valueType": "STRING",
        "predicateType": "STRING",
        "operation": "CONTAINS",
        "defaultValue": "sensor"
    }
]
```

### Pattern 4: Boolean check

Active = true:

```json
[
    {
        "keyType": "ATTRIBUTE",
        "key": "active",
        "valueType": "BOOLEAN",
        "predicateType": "BOOLEAN",
        "operation": "EQUAL",
        "defaultValue": true
    }
]
```

### Pattern 5: Multiple conditions (AND)

Temperature > 25 AND humidity < 60:

```json
[
    {
        "keyType": "TIME_SERIES",
        "key": "temperature",
        "valueType": "NUMERIC",
        "predicateType": "NUMERIC",
        "operation": "GREATER",
        "defaultValue": 25
    },
    {
        "keyType": "TIME_SERIES",
        "key": "humidity",
        "valueType": "NUMERIC",
        "predicateType": "NUMERIC",
        "operation": "LESS",
        "defaultValue": 60
    }
]
```

### Pattern 6: OR logic

Temperature < 10 OR temperature > 30:

```json
[
    {
        "keyType": "TIME_SERIES",
        "key": "temperature",
        "valueType": "NUMERIC",
        "predicateType": "COMPLEX",
        "complexOperation": "OR",
        "nestedPredicates": [
            {
                "predicateType": "NUMERIC",
                "operation": "LESS",
                "defaultValue": 10
            },
            {
                "predicateType": "NUMERIC",
                "operation": "GREATER",
                "defaultValue": 30
            }
        ]
    }
]
```

---

## Filter Structure

Every KeyFilter has these fields:

```json
{
    "keyType": "TIME_SERIES | ATTRIBUTE | ENTITY_FIELD | ...",
    "key": "fieldName",
    "valueType": "NUMERIC | STRING | BOOLEAN | DATE_TIME",
    "predicateType": "NUMERIC | STRING | BOOLEAN | COMPLEX",
    "operation": "GREATER | LESS | EQUAL | ...",
    "defaultValue": ""
}
```

---

## Part 1: keyType (What to filter)

The **keyType** specifies which field to filter on.

### Available Key Types

| keyType            | Usage                        | Example key values            |
|--------------------|------------------------------|-------------------------------|
| `TIME_SERIES`      | Telemetry data               | temperature, humidity          |
| `ATTRIBUTE`        | Any attribute type           | active, model                  |
| `CLIENT_ATTRIBUTE` | Client attributes only       | config                         |
| `SHARED_ATTRIBUTE` | Shared attributes only       | threshold                      |
| `SERVER_ATTRIBUTE` | Server attributes only       | location                       |
| `ENTITY_FIELD`     | Entity properties            | name, label, type, createdTime |
| `ALARM_FIELD`      | Alarm fields (alarm queries) | status, severity               |

### Common Entity Fields

When using `ENTITY_FIELD`, these keys are available:

- `name` - Entity name
- `label` - Entity label
- `type` - Entity type
- `createdTime` - Creation timestamp

---

## Part 2: valueType (Data type)

The **valueType** determines:

1. How the value is interpreted
2. Which operations are available

### Value Types & Operations

| valueType   | Used For                     | Available Operations                                             |
|-------------|------------------------------|------------------------------------------------------------------|
| `NUMERIC`   | Numbers (integers, decimals) | EQUAL, NOT_EQUAL, GREATER, LESS, GREATER_OR_EQUAL, LESS_OR_EQUAL |
| `STRING`    | Text, JSON strings           | EQUAL, NOT_EQUAL, STARTS_WITH, ENDS_WITH, CONTAINS, NOT_CONTAINS |
| `BOOLEAN`   | true/false values            | EQUAL, NOT_EQUAL                                                 |
| `DATE_TIME` | Timestamps (milliseconds)    | EQUAL, NOT_EQUAL, GREATER, LESS, GREATER_OR_EQUAL, LESS_OR_EQUAL |

### Operation Details

**NUMERIC operations:**

- `GREATER` - Greater than
- `LESS` - Less than
- `EQUAL` - Equal to
- `NOT_EQUAL` - Not equal to
- `GREATER_OR_EQUAL` - Greater than or equal
- `LESS_OR_EQUAL` - Less than or equal

**STRING operations:**

- `EQUAL` - Exact match (case-sensitive)
- `NOT_EQUAL` - Not exact match
- `STARTS_WITH` - Begins with substring
- `ENDS_WITH` - Ends with substring
- `CONTAINS` - Contains substring (case-sensitive)
- `NOT_CONTAINS` - Does not contain substring

**BOOLEAN operations:**

- `EQUAL` - Equals true or false
- `NOT_EQUAL` - Not equals

**DATE_TIME operations:**
Same as NUMERIC (timestamps are numbers in milliseconds since epoch)

---

## Part 3: predicateType (The condition type)

The **predicateType** determines the predicate structure.

### Simple Predicates (NUMERIC, STRING, BOOLEAN)

For single conditions, set `predicateType` to match the `valueType` and provide `operation` + `defaultValue`:

```json
{
    "keyType": "TIME_SERIES",
    "key": "temperature",
    "valueType": "NUMERIC",
    "predicateType": "NUMERIC",
    "operation": "GREATER",
    "defaultValue": 20
}
```

### Complex Predicates (OR/AND Logic)

For multiple conditions on the **same key**, use `predicateType: "COMPLEX"` with `complexOperation` and `nestedPredicates`:

```json
{
    "keyType": "TIME_SERIES",
    "key": "temperature",
    "valueType": "NUMERIC",
    "predicateType": "COMPLEX",
    "complexOperation": "OR",
    "nestedPredicates": [
        {
            "predicateType": "NUMERIC",
            "operation": "LESS",
            "defaultValue": 10
        },
        {
            "predicateType": "NUMERIC",
            "operation": "GREATER",
            "defaultValue": 30
        }
    ]
}
```

### Nested complex predicates

Value < 10 OR (value > 50 AND value < 60):

```json
{
    "keyType": "TIME_SERIES",
    "key": "temperature",
    "valueType": "NUMERIC",
    "predicateType": "COMPLEX",
    "complexOperation": "OR",
    "nestedPredicates": [
        {
            "predicateType": "NUMERIC",
            "operation": "LESS",
            "defaultValue": 10
        },
        {
            "predicateType": "COMPLEX",
            "complexOperation": "AND",
            "nestedPredicates": [
                {
                    "predicateType": "NUMERIC",
                    "operation": "GREATER",
                    "defaultValue": 50
                },
                {
                    "predicateType": "NUMERIC",
                    "operation": "LESS",
                    "defaultValue": 60
                }
            ]
        }
    ]
}
```

---

## Dynamic Values

Instead of hardcoding values, you can reference attributes from tenant, customer, user, or device:

```json
{
    "keyType": "TIME_SERIES",
    "key": "temperature",
    "valueType": "NUMERIC",
    "predicateType": "NUMERIC",
    "operation": "GREATER",
    "defaultValue": 20,
    "dynamicValueSourceType": "CURRENT_USER",
    "dynamicValueSourceAttribute": "tempThreshold",
    "dynamicValueInherit": false
}
```

**How it works:**

1. Tries to read `tempThreshold` attribute from current user
2. If attribute doesn't exist, uses `defaultValue` (20)
3. If `dynamicValueInherit: true`, will look up hierarchy (user -> customer -> tenant)

### Source Types

| dynamicValueSourceType | Reads From                    |
|------------------------|-------------------------------|
| `CURRENT_USER`         | Current user's attributes     |
| `CURRENT_CUSTOMER`     | Current customer's attributes |
| `CURRENT_TENANT`       | Tenant attributes             |
| `CURRENT_DEVICE`       | Device attributes             |

---

## Complete Examples

### Use Case 1: Overheating devices

Temperature > 80 AND humidity > 90:

```json
[
    {
        "keyType": "TIME_SERIES",
        "key": "temperature",
        "valueType": "NUMERIC",
        "predicateType": "NUMERIC",
        "operation": "GREATER",
        "defaultValue": 80
    },
    {
        "keyType": "TIME_SERIES",
        "key": "humidity",
        "valueType": "NUMERIC",
        "predicateType": "NUMERIC",
        "operation": "GREATER",
        "defaultValue": 90
    }
]
```

*Note: Multiple filters in array = AND logic. Both conditions must be true.*

### Use Case 2: Name pattern + active status

Name contains "sensor" AND active = true:

```json
[
    {
        "keyType": "ENTITY_FIELD",
        "key": "name",
        "valueType": "STRING",
        "predicateType": "STRING",
        "operation": "CONTAINS",
        "defaultValue": "sensor"
    },
    {
        "keyType": "ATTRIBUTE",
        "key": "active",
        "valueType": "BOOLEAN",
        "predicateType": "BOOLEAN",
        "operation": "EQUAL",
        "defaultValue": true
    }
]
```

### Use Case 3: Temperature out of range

Temperature < 10 OR temperature > 30:

```json
[
    {
        "keyType": "TIME_SERIES",
        "key": "temperature",
        "valueType": "NUMERIC",
        "predicateType": "COMPLEX",
        "complexOperation": "OR",
        "nestedPredicates": [
            {
                "predicateType": "NUMERIC",
                "operation": "LESS",
                "defaultValue": 10
            },
            {
                "predicateType": "NUMERIC",
                "operation": "GREATER",
                "defaultValue": 30
            }
        ]
    }
]
```

### Use Case 4: Offline devices

Last activity > 24 hours ago (calculate current timestamp minus 86400000):

```json
[
    {
        "keyType": "TIME_SERIES",
        "key": "lastActivityTime",
        "valueType": "DATE_TIME",
        "predicateType": "NUMERIC",
        "operation": "LESS",
        "defaultValue": 1729036800000
    }
]
```

### Use Case 5: Dynamic tenant threshold

Temperature > tenant's configured threshold:

```json
[
    {
        "keyType": "TIME_SERIES",
        "key": "temperature",
        "valueType": "NUMERIC",
        "predicateType": "NUMERIC",
        "operation": "GREATER",
        "defaultValue": 25,
        "dynamicValueSourceType": "CURRENT_TENANT",
        "dynamicValueSourceAttribute": "temperatureThreshold",
        "dynamicValueInherit": false
    }
]
```

### Use Case 6: String pattern matching

Label starts with "Room" AND ends with "Sensor":

```json
[
    {
        "keyType": "ENTITY_FIELD",
        "key": "label",
        "valueType": "STRING",
        "predicateType": "COMPLEX",
        "complexOperation": "AND",
        "nestedPredicates": [
            {
                "predicateType": "STRING",
                "operation": "STARTS_WITH",
                "defaultValue": "Room"
            },
            {
                "predicateType": "STRING",
                "operation": "ENDS_WITH",
                "defaultValue": "Sensor"
            }
        ]
    }
]
```

---

## Quick Reference Card

```
Filter Fields:
  keyType        → What to filter (TIME_SERIES, ATTRIBUTE, ENTITY_FIELD, etc.)
  key            → Field name (temperature, active, name, etc.)
  valueType      → Data type (NUMERIC, STRING, BOOLEAN, DATE_TIME)
  predicateType  → Condition type (NUMERIC, STRING, BOOLEAN, COMPLEX)
  operation      → Comparison (GREATER, LESS, EQUAL, CONTAINS, etc.)
  defaultValue   → Value to compare against

Key Types:
  TIME_SERIES        → Telemetry
  ATTRIBUTE          → Any attribute
  CLIENT_ATTRIBUTE   → Client attributes
  SHARED_ATTRIBUTE   → Shared attributes
  SERVER_ATTRIBUTE   → Server attributes
  ENTITY_FIELD       → Entity properties (name, label, etc.)
  ALARM_FIELD        → Alarm fields

Value Types → Operations:
  NUMERIC    → GREATER, LESS, EQUAL, NOT_EQUAL,
               GREATER_OR_EQUAL, LESS_OR_EQUAL
  STRING     → EQUAL, NOT_EQUAL, STARTS_WITH, ENDS_WITH,
               CONTAINS, NOT_CONTAINS
  BOOLEAN    → EQUAL, NOT_EQUAL
  DATE_TIME  → Same as NUMERIC

Multiple Filters:
  [{filter1}, {filter2}]  → Automatic AND logic

OR Logic (same key):
  Use predicateType: "COMPLEX" with complexOperation: "OR"
  and nestedPredicates array

Dynamic Values:
  dynamicValueSourceType: CURRENT_USER | CURRENT_CUSTOMER | CURRENT_TENANT | CURRENT_DEVICE
  dynamicValueSourceAttribute: attribute name
  dynamicValueInherit: true|false (search up hierarchy)
```
