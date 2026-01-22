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
        "key": {
            "type": "TIME_SERIES",
            "key": "temperature"
        },
        "valueType": "NUMERIC",
        "predicate": {
            "operation": "GREATER",
            "value": {
                "defaultValue": 25,
                "dynamicValue": {
                    "sourceType": "CURRENT_USER",
                    "sourceAttribute": "tempThreshold",
                    "inherit": false
                }
            },
            "type": "NUMERIC"
        }
    }
]
```

### Pattern 2: Range check (between X and Y)

Temperature between 20 and 30:

```json
[
    {
        "key": {
            "type": "TIME_SERIES",
            "key": "temperature"
        },
        "valueType": "NUMERIC",
        "predicate": {
            "type": "COMPLEX",
            "operation": "AND",
            "predicates": [
                {
                    "operation": "GREATER_OR_EQUAL",
                    "value": {
                        "defaultValue": 20
                    },
                    "type": "NUMERIC"
                },
                {
                    "operation": "LESS_OR_EQUAL",
                    "value": {
                        "defaultValue": 30
                    },
                    "type": "NUMERIC"
                }
            ]
        }
    }
]
```

### Pattern 3: String contains

Name contains "sensor":

```json
[
    {
        "key": {
            "type": "ENTITY_FIELD",
            "key": "name"
        },
        "valueType": "STRING",
        "predicate": {
            "operation": "CONTAINS",
            "value": {
                "defaultValue": "sensor",
                "dynamicValue": {
                    "sourceType": "CURRENT_USER",
                    "sourceAttribute": "tempThreshold",
                    "inherit": false
                }
            },
            "type": "STRING"
        }
    }
]
```

### Pattern 4: Boolean check

Active = true:

```json
[
    {
        "key": {
            "type": "ATTRIBUTE",
            "key": "active"
        },
        "valueType": "BOOLEAN",
        "predicate": {
            "operation": "EQUAL",
            "value": {
                "defaultValue": true,
                "dynamicValue": {
                    "sourceType": "CURRENT_USER",
                    "sourceAttribute": "tempThreshold",
                    "inherit": false
                }
            },
            "type": "BOOLEAN"
        }
    }
]
```

### Pattern 5: Multiple conditions (AND)

Temperature > 25 AND humidity < 60:

```json
[
    {
        "key": {
            "type": "TIME_SERIES",
            "key": "temperature"
        },
        "valueType": "NUMERIC",
        "predicate": {
            "operation": "GREATER",
            "value": {
                "defaultValue": 25
            },
            "type": "NUMERIC"
        }
    },
    {
        "key": {
            "type": "TIME_SERIES",
            "key": "humidity"
        },
        "valueType": "NUMERIC",
        "predicate": {
            "operation": "LESS",
            "value": {
                "defaultValue": 60
            },
            "type": "NUMERIC"
        }
    }
]
```

### Pattern 6: OR logic

Temperature < 10 OR temperature > 30:

```json
[
    {
        "key": {
            "type": "TIME_SERIES",
            "key": "temperature"
        },
        "valueType": "NUMERIC",
        "predicate": {
            "type": "COMPLEX",
            "operation": "OR",
            "predicates": [
                {
                    "operation": "LESS",
                    "value": {
                        "defaultValue": 10
                    },
                    "type": "NUMERIC"
                },
                {
                    "operation": "GREATER",
                    "value": {
                        "defaultValue": 30
                    },
                    "type": "NUMERIC"
                }
            ]
        }
    }
]
```

---

## Filter Structure

Every KeyFilter has three required parts:

```json
{
    "key": {
        "type": "TIME_SERIES | ATTRIBUTE | ENTITY_FIELD | ...",
        "key": "fieldName"
    },
    "valueType": "NUMERIC | STRING | BOOLEAN | DATE_TIME",
    "predicate": {
        "operation": "GREATER | LESS | EQUAL | ...",
        "value": {
            "defaultValue": "",
            "dynamicValue": {
                "sourceType": "CURRENT_USER",
                "sourceAttribute": "tempThreshold",
                "inherit": false
            }
        },
        "type": "NUMERIC | STRING | BOOLEAN | DATE_TIME | COMPLEX"
    }
}
```

---

## Part 1: Key (What to filter)

The **key** specifies which field to filter on.

### Available Key Types

| Type               | Usage                        | Example                                            |
|--------------------|------------------------------|----------------------------------------------------|
| `TIME_SERIES`      | Telemetry data               | `{"type": "TIME_SERIES", "key": "temperature"}`    |
| `ATTRIBUTE`        | Any attribute type           | `{"type": "ATTRIBUTE", "key": "active"}`           |
| `CLIENT_ATTRIBUTE` | Client attributes only       | `{"type": "CLIENT_ATTRIBUTE", "key": "config"}`    |
| `SHARED_ATTRIBUTE` | Shared attributes only       | `{"type": "SHARED_ATTRIBUTE", "key": "threshold"}` |
| `SERVER_ATTRIBUTE` | Server attributes only       | `{"type": "SERVER_ATTRIBUTE", "key": "location"}`  |
| `ENTITY_FIELD`     | Entity properties            | `{"type": "ENTITY_FIELD", "key": "name"}`          |
| `ALARM_FIELD`      | Alarm fields (alarm queries) | `{"type": "ALARM_FIELD", "key": "status"}`         |

### Common Entity Fields

When using `ENTITY_FIELD`, these keys are available:

- `name` - Entity name
- `label` - Entity label
- `type` - Entity type
- `createdTime` - Creation timestamp

### Examples

**Filter by telemetry:**

```json
{
    "type": "TIME_SERIES",
    "key": "temperature"
}
```

**Filter by attribute:**

```json
{
    "type": "ATTRIBUTE",
    "key": "active"
}
```

**Filter by entity name:**

```json
{
    "type": "ENTITY_FIELD",
    "key": "name"
}
```

---

## Part 2: Value Type (Data type)

The **valueType** determines:

1. How the value is interpreted
2. Which operations are available

### Value Types & Operations

| Value Type  | Used For                     | Available Operations                                             |
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
- `GREATER_OR_EQUAL` - Greater than or equal (≥)
- `LESS_OR_EQUAL` - Less than or equal (≤)

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

⚠️ **Important:** Using wrong operation for valueType will cause errors!

- ❌ `STARTS_WITH` on NUMERIC → Error
- ❌ `GREATER` on STRING → Error
- ✅ `CONTAINS` on STRING → OK
- ✅ `GREATER` on NUMERIC → OK

---

## Part 3: Predicate (The condition)

The **predicate** defines the logical expression to evaluate.

### Simple Predicates

For single conditions, use a simple predicate:

```json
{
    "operation": "GREATER",
    "value": {
        "defaultValue": 20,
        "dynamicValue": {
            "sourceType": "CURRENT_USER",
            "sourceAttribute": "tempThreshold",
            "inherit": false
        }
    },
    "type": "NUMERIC"
}
```

**Structure:**

- `operation` - The comparison operation (see tables above)
- `value.defaultValue` - The value to compare against
- `value.dynamicValue` - Optional dynamic value source (see below)
- `type` - Predicate type (NUMERIC, STRING, BOOLEAN, or COMPLEX)

### Examples by Type

**NUMERIC predicate:**

```json
{
    "operation": "GREATER_OR_EQUAL",
    "value": {
        "defaultValue": 100,
        "dynamicValue": {
            "sourceType": "CURRENT_USER",
            "sourceAttribute": "tempThreshold",
            "inherit": false
        }
    },
    "type": "NUMERIC"
}
```

**STRING predicate:**

```json
{
    "operation": "CONTAINS",
    "value": {
        "defaultValue": "sensor",
        "dynamicValue": {
            "sourceType": "CURRENT_USER",
            "sourceAttribute": "tempThreshold",
            "inherit": false
        }
    },
    "type": "STRING"
}
```

**BOOLEAN predicate:**

```json
{
    "operation": "EQUAL",
    "value": {
        "defaultValue": true,
        "dynamicValue": {
            "sourceType": "CURRENT_USER",
            "sourceAttribute": "tempThreshold",
            "inherit": false
        }
    },
    "type": "BOOLEAN"
}
```

---

## Complex Predicates (OR/AND Logic)

For multiple conditions on the **same key**, use COMPLEX predicates.

### Structure

```json
{
    "type": "COMPLEX",
    "operation": "OR",
    "predicates": [
        {},
        {}
    ]
}
```

### Example 1: OR logic

Value < 10 OR value > 20:

```json
{
    "type": "COMPLEX",
    "operation": "OR",
    "predicates": [
        {
            "operation": "LESS",
            "value": {
                "defaultValue": 10,
                "dynamicValue": {
                    "sourceType": "CURRENT_USER",
                    "sourceAttribute": "tempThreshold",
                    "inherit": false
                }
            },
            "type": "NUMERIC"
        },
        {
            "operation": "GREATER",
            "value": {
                "defaultValue": 20,
                "dynamicValue": {
                    "sourceType": "CURRENT_USER",
                    "sourceAttribute": "tempThreshold",
                    "inherit": false
                }
            },
            "type": "NUMERIC"
        }
    ]
}
```

### Example 2: AND logic (range)

Value >= 20 AND value <= 30:

```json
{
    "type": "COMPLEX",
    "operation": "AND",
    "predicates": [
        {
            "operation": "GREATER_OR_EQUAL",
            "value": {
                "defaultValue": 20,
                "dynamicValue": {
                    "sourceType": "CURRENT_USER",
                    "sourceAttribute": "tempThreshold",
                    "inherit": false
                }
            },
            "type": "NUMERIC"
        },
        {
            "operation": "LESS_OR_EQUAL",
            "value": {
                "defaultValue": 30,
                "dynamicValue": {
                    "sourceType": "CURRENT_USER",
                    "sourceAttribute": "tempThreshold",
                    "inherit": false
                }
            },
            "type": "NUMERIC"
        }
    ]
}
```

### Example 3: Nested complex predicates

Value < 10 OR (value > 50 AND value < 60):

```json
{
    "type": "COMPLEX",
    "operation": "OR",
    "predicates": [
        {
            "operation": "LESS",
            "value": {
                "defaultValue": 10,
                "dynamicValue": null
            },
            "type": "NUMERIC"
        },
        {
            "type": "COMPLEX",
            "operation": "AND",
            "predicates": [
                {
                    "operation": "GREATER",
                    "value": {
                        "defaultValue": 50,
                        "dynamicValue": null
                    },
                    "type": "NUMERIC"
                },
                {
                    "operation": "LESS",
                    "value": {
                        "defaultValue": 60,
                        "dynamicValue": null
                    },
                    "type": "NUMERIC"
                }
            ]
        }
    ]
}
```

---

## Dynamic Values

Instead of hardcoding values, you can reference attributes from using sourceType(which are values of DynamicValueSourceType):

- Current user
- Current customer
- Current tenant
- Current device

### Structure

```json
{
    "operation": "GREATER",
    "value": {
        "defaultValue": 0,
        "dynamicValue": {
            "sourceType": "CURRENT_TENANT",
            "sourceAttribute": "attributeName",
            "inherit": false
        }
    },
    "type": "NUMERIC"
}
```

### Example: User-specific threshold

Temperature > user's personal threshold:

```json
{
    "key": {
        "type": "TIME_SERIES",
        "key": "temperature"
    },
    "valueType": "NUMERIC",
    "predicate": {
        "operation": "GREATER",
        "value": {
            "defaultValue": 20,
            "dynamicValue": {
                "sourceType": "CURRENT_USER",
                "sourceAttribute": "tempThreshold",
                "inherit": false
            }
        },
        "type": "NUMERIC"
    }
}
```

**How it works:**

1. Tries to read `tempThreshold` attribute from current user
2. If attribute doesn't exist, uses `defaultValue` (20)
3. If `inherit: true`, will look up hierarchy (user → customer → tenant)

### Source Types (DynamicValueSourceType)

'DynamicValueSourceType' field:

| Source Type        | Reads From                    |
|--------------------|-------------------------------|
| `CURRENT_USER`     | Current user's attributes     |
| `CURRENT_CUSTOMER` | Current customer's attributes |
| `CURRENT_TENANT`   | Tenant attributes             |
| `CURRENT_DEVICE`   | Device attributes             |

---

## Complete Examples

### Use Case 1: Overheating devices

Temperature > 80 OR humidity > 90:

```json
[
    {
        "key": {
            "type": "TIME_SERIES",
            "key": "temperature"
        },
        "valueType": "NUMERIC",
        "predicate": {
            "operation": "GREATER",
            "value": {
                "defaultValue": 80,
                "dynamicValue": null
            },
            "type": "NUMERIC"
        }
    },
    {
        "key": {
            "type": "TIME_SERIES",
            "key": "humidity"
        },
        "valueType": "NUMERIC",
        "predicate": {
            "operation": "GREATER",
            "value": {
                "defaultValue": 90,
                "dynamicValue": null
            },
            "type": "NUMERIC"
        }
    }
]
```

*Note: Multiple filters in array = AND logic. Both conditions must be true.*

### Use Case 2: Name pattern + active status

Name contains "sensor" AND active = true:

```json
[
    {
        "key": {
            "type": "ENTITY_FIELD",
            "key": "name"
        },
        "valueType": "STRING",
        "predicate": {
            "operation": "CONTAINS",
            "value": {
                "defaultValue": "sensor",
                "dynamicValue": null
            },
            "type": "STRING"
        }
    },
    {
        "key": {
            "type": "ATTRIBUTE",
            "key": "active"
        },
        "valueType": "BOOLEAN",
        "predicate": {
            "operation": "EQUAL",
            "value": {
                "defaultValue": true,
                "dynamicValue": null
            },
            "type": "BOOLEAN"
        }
    }
]
```

### Use Case 3: Temperature out of range

Temperature < 10 OR temperature > 30 (single filter with OR):

```json
[
    {
        "key": {
            "type": "TIME_SERIES",
            "key": "temperature"
        },
        "valueType": "NUMERIC",
        "predicate": {
            "type": "COMPLEX",
            "operation": "OR",
            "predicates": [
                {
                    "operation": "LESS",
                    "value": {
                        "defaultValue": 10,
                        "dynamicValue": null
                    },
                    "type": "NUMERIC"
                },
                {
                    "operation": "GREATER",
                    "value": {
                        "defaultValue": 30,
                        "dynamicValue": null
                    },
                    "type": "NUMERIC"
                }
            ]
        }
    }
]
```

### Use Case 4: Offline devices

Last activity > 24 hours ago:

```json
[
    {
        "key": {
            "type": "TIME_SERIES",
            "key": "lastActivityTime"
        },
        "valueType": "DATE_TIME",
        "predicate": {
            "operation": "LESS",
            "value": {
                "defaultValue": 1729036800000,
                "dynamicValue": null
            },
            "type": "NUMERIC"
        }
    }
]
```

*Note: Calculate current timestamp minus 86400000 (24 hours in milliseconds)*

### Use Case 5: Complex business logic

(batteryLevel < 20 AND active = true) OR (lastMaintenance > 30 days ago):

```json
[
    {
        "key": {
            "type": "TIME_SERIES",
            "key": "batteryLevel"
        },
        "valueType": "NUMERIC",
        "predicate": {
            "operation": "LESS",
            "value": {
                "defaultValue": 20,
                "dynamicValue": null
            },
            "type": "NUMERIC"
        }
    },
    {
        "key": {
            "type": "ATTRIBUTE",
            "key": "active"
        },
        "valueType": "BOOLEAN",
        "predicate": {
            "operation": "EQUAL",
            "value": {
                "defaultValue": true,
                "dynamicValue": null
            },
            "type": "BOOLEAN"
        }
    }
]
```

*Note: This creates AND logic. For true OR at top level, you need separate queries or complex predicates.*

### Use Case 6: Dynamic tenant threshold

Temperature > tenant's configured threshold:

```json
[
    {
        "key": {
            "type": "TIME_SERIES",
            "key": "temperature"
        },
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
```

### Use Case 7: String pattern matching

Label starts with "Room" AND ends with "Sensor":

```json
[
    {
        "key": {
            "type": "ENTITY_FIELD",
            "key": "label"
        },
        "valueType": "STRING",
        "predicate": {
            "type": "COMPLEX",
            "operation": "AND",
            "predicates": [
                {
                    "operation": "STARTS_WITH",
                    "value": {
                        "defaultValue": "Room",
                        "dynamicValue": null
                    },
                    "type": "STRING"
                },
                {
                    "operation": "ENDS_WITH",
                    "value": {
                        "defaultValue": "Sensor",
                        "dynamicValue": null
                    },
                    "type": "STRING"
                }
            ]
        }
    }
]
```

---

## Quick Reference Card

```
┌──────────────────────────────────────────────────────────────┐
│ KeyFilter Structure                                          │
├──────────────────────────────────────────────────────────────┤
│ [{                                                           │
│   "key": {"type": "...", "key": "..."},                      │
│   "valueType": "NUMERIC|STRING|BOOLEAN|DATE_TIME",           │
│   "predicate": {                                             │
│     "operation": "...",                                      │
│     "value": {"defaultValue": ..., "dynamicValue": null},    │
│     "type": "NUMERIC|STRING|BOOLEAN|COMPLEX"                 │
│   }                                                          │
│ }]                                                           │
└──────────────────────────────────────────────────────────────┘

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

Predicate Types:
  NUMERIC    → Simple numeric comparison
  STRING     → Simple string comparison
  BOOLEAN    → Simple boolean comparison
  COMPLEX    → Combine multiple with AND/OR

Multiple Filters:
  [{filter1}, {filter2}]  → Automatic AND logic
  
OR Logic (same key):
  Use COMPLEX predicate with "operation": "OR"
  
Dynamic Values:
  sourceType: CURRENT_USER | CURRENT_CUSTOMER | CURRENT_TENANT | CURRENT_DEVICE
  sourceAttribute: attribute name
  inherit: true|false (search up hierarchy)
```
