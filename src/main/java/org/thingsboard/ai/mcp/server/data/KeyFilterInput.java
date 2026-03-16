package org.thingsboard.ai.mcp.server.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.client.model.BooleanFilterPredicate;
import org.thingsboard.client.model.BooleanOperation;
import org.thingsboard.client.model.ComplexFilterPredicate;
import org.thingsboard.client.model.ComplexOperation;
import org.thingsboard.client.model.DynamicValueBoolean;
import org.thingsboard.client.model.DynamicValueDouble;
import org.thingsboard.client.model.DynamicValueSourceType;
import org.thingsboard.client.model.DynamicValueString;
import org.thingsboard.client.model.EntityKey;
import org.thingsboard.client.model.EntityKeyType;
import org.thingsboard.client.model.EntityKeyValueType;
import org.thingsboard.client.model.FilterPredicateValueBoolean;
import org.thingsboard.client.model.FilterPredicateValueDouble;
import org.thingsboard.client.model.FilterPredicateValueString;
import org.thingsboard.client.model.KeyFilter;
import org.thingsboard.client.model.KeyFilterPredicate;
import org.thingsboard.client.model.NumericFilterPredicate;
import org.thingsboard.client.model.NumericOperation;
import org.thingsboard.client.model.StringFilterPredicate;
import org.thingsboard.client.model.StringOperation;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class KeyFilterInput {

    @JsonProperty(required = true)
    private String keyType;

    @JsonProperty(required = true)
    private String key;

    @JsonProperty
    private String valueType;

    @JsonProperty(required = true)
    private String predicateType;

    @JsonProperty
    private String operation;

    @JsonProperty
    private Object defaultValue;

    @JsonProperty
    private Object userValue;

    @JsonProperty
    private String dynamicValueSourceType;

    @JsonProperty
    private String dynamicValueSourceAttribute;

    @JsonProperty
    private Boolean dynamicValueInherit;

    @JsonProperty
    private Boolean ignoreCase;

    @JsonProperty
    private String complexOperation;

    @JsonProperty
    private List<KeyFilterInput> nestedPredicates;

    public KeyFilter toKeyFilter() {
        EntityKey entityKey = new EntityKey()
                .type(EntityKeyType.valueOf(keyType))
                .key(key);

        KeyFilter filter = new KeyFilter().key(entityKey);

        if (valueType != null) {
            filter.valueType(EntityKeyValueType.valueOf(valueType));
        }

        KeyFilterPredicate predicate = buildPredicate();
        filter.predicate(predicate);

        return filter;
    }

    KeyFilterPredicate buildPredicate() {
        return switch (predicateType) {
            case "STRING" -> buildStringPredicate();
            case "NUMERIC" -> buildNumericPredicate();
            case "BOOLEAN" -> buildBooleanPredicate();
            case "COMPLEX" -> buildComplexPredicate();
            default -> throw new IllegalArgumentException("Unknown predicate type: " + predicateType);
        };
    }

    private StringFilterPredicate buildStringPredicate() {
        FilterPredicateValueString value = new FilterPredicateValueString()
                .defaultValue(castString(defaultValue))
                .userValue(castString(userValue));

        if (dynamicValueSourceType != null) {
            DynamicValueString dv = new DynamicValueString()
                    .sourceType(DynamicValueSourceType.valueOf(dynamicValueSourceType))
                    .sourceAttribute(dynamicValueSourceAttribute)
                    .inherit(dynamicValueInherit != null && dynamicValueInherit);
            value.dynamicValue(dv);
        }

        return new StringFilterPredicate()
                .operation(StringOperation.valueOf(operation))
                .ignoreCase(ignoreCase != null && ignoreCase)
                .value(value);
    }

    private NumericFilterPredicate buildNumericPredicate() {
        FilterPredicateValueDouble value = new FilterPredicateValueDouble()
                .defaultValue(castDouble(defaultValue))
                .userValue(castDouble(userValue));

        if (dynamicValueSourceType != null) {
            DynamicValueDouble dv = new DynamicValueDouble()
                    .sourceType(DynamicValueSourceType.valueOf(dynamicValueSourceType))
                    .sourceAttribute(dynamicValueSourceAttribute)
                    .inherit(dynamicValueInherit != null && dynamicValueInherit);
            value.dynamicValue(dv);
        }

        return new NumericFilterPredicate()
                .operation(NumericOperation.valueOf(operation))
                .value(value);
    }

    private BooleanFilterPredicate buildBooleanPredicate() {
        FilterPredicateValueBoolean value = new FilterPredicateValueBoolean()
                .defaultValue(castBoolean(defaultValue))
                .userValue(castBoolean(userValue));

        if (dynamicValueSourceType != null) {
            DynamicValueBoolean dv = new DynamicValueBoolean()
                    .sourceType(DynamicValueSourceType.valueOf(dynamicValueSourceType))
                    .sourceAttribute(dynamicValueSourceAttribute)
                    .inherit(dynamicValueInherit != null && dynamicValueInherit);
            value.dynamicValue(dv);
        }

        return new BooleanFilterPredicate()
                .operation(BooleanOperation.valueOf(operation))
                .value(value);
    }

    private ComplexFilterPredicate buildComplexPredicate() {
        ComplexFilterPredicate predicate = new ComplexFilterPredicate()
                .operation(ComplexOperation.valueOf(complexOperation));

        if (nestedPredicates != null && !nestedPredicates.isEmpty()) {
            List<KeyFilterPredicate> predicates = nestedPredicates.stream()
                    .map(KeyFilterInput::buildPredicate)
                    .collect(Collectors.toList());
            predicate.predicates(predicates);
        }

        return predicate;
    }

    private static String castString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Double castDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.doubleValue();
        return Double.valueOf(String.valueOf(value));
    }

    private static Boolean castBoolean(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean b) return b;
        return Boolean.valueOf(String.valueOf(value));
    }

}
