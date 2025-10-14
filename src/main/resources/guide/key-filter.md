<B>Key Filters</B>

**Key Filter** allows you to define complex logical expressions over entity field, attribute or latest time series value. The filter is defined using 'key', 
'valueType' and 'predicate' objects. Single Entity Query may have zero, one or multiple predicates. If multiple filters are defined, they are evaluated using logical 'AND'.

The example below checks that the temperature of the entity is above 20 degrees:
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
            "dynamicValue": null
        },
        "type": "NUMERIC"
    }
}
```

Let's review 'key', 'valueType' and 'predicate' objects of keyFilter object:

**Key**

Filter Key defines either entity field, attribute or telemetry. It is a JSON object that consists of the key name and type. " +
The following filter key types are supported:
   * 'CLIENT_ATTRIBUTE' - used for client attributes;
   * 'SHARED_ATTRIBUTE' - used for shared attributes;
   * 'SERVER_ATTRIBUTE' - used for server attributes;
   * 'ATTRIBUTE' - used for any of the above;
   * 'TIME_SERIES' - used for time series values;
   * 'ENTITY_FIELD' - used for accessing entity fields like 'name', 'label', etc. The list of available fields depends on the entity type;
   * 'ALARM_FIELD' - similar to entity field, but is used in alarm queries only.

The example:
```json
{
    "type": "TIME_SERIES",
    "key": "temperature"
}
```

**Value type**

Provides a hint about the data type of the entity field that is defined in the filter key. 
The value type impacts the list of possible operations that you may use in the corresponding predicate. 
For example, you may use 'STARTS_WITH' or 'END_WITH', but you can't use 'GREATER_OR_EQUAL' for string values.

The following filter value types and corresponding predicate operations are supported:
   * 'STRING' - used to filter any 'String' or 'JSON' values. Operations: EQUAL, NOT_EQUAL, STARTS_WITH, ENDS_WITH, CONTAINS, NOT_CONTAINS;
   * 'NUMERIC' - used for 'Long' and 'Double' values. Operations: EQUAL, NOT_EQUAL, GREATER, LESS, GREATER_OR_EQUAL, LESS_OR_EQUAL;
   * 'BOOLEAN' - used for boolean values. Operations: EQUAL, NOT_EQUAL;
   * 'DATE_TIME' - similar to numeric, transforms value to milliseconds since epoch. Operations: EQUAL, NOT_EQUAL, GREATER, LESS, GREATER_OR_EQUAL, LESS_OR_EQUAL.

**Predicate**

Filter Predicate defines the logical expression to evaluate. The list of available operations depends on the filter value type, see above. 
The Platform supports four predicate types: 'STRING', 'NUMERIC', 'BOOLEAN' and 'COMPLEX'. The last one allows combining multiple operations over one filter key.

Simple predicate example to check 'value < 100':
```json
{
    "operation": "LESS",
    "value": {
        "defaultValue": 100,
        "dynamicValue": null
    },
    "type": "NUMERIC"
}
```

Complex predicate example, to check 'value < 10 or value > 20':
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
            "operation": "GREATER",
            "value": {
                "defaultValue": 20,
                "dynamicValue": null
            },
            "type": "NUMERIC"
        }
    ]
}
```

More complex predicate example, to check 'value < 10 or (value > 50 && value < 60)':
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

You may also want to replace hardcoded values (for example, temperature > 20) with the more dynamic 
expression (for example, temperature > 'value of the tenant attribute with key 'temperatureThreshold').
It is possible to use 'dynamicValue' to define the attribute of the tenant, customer or user that is performing the API call.

See the example below:
```json
{
    "operation": "GREATER",
    "value": {
        "defaultValue": 0,
        "dynamicValue": {
            "sourceType": "CURRENT_USER",
            "sourceAttribute": "temperatureThreshold"
        }
    },
    "type": "NUMERIC"
}
```
