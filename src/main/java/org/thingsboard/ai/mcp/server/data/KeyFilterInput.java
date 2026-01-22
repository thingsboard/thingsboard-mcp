package org.thingsboard.ai.mcp.server.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.thingsboard.server.common.data.query.BooleanFilterPredicate;
import org.thingsboard.server.common.data.query.ComplexFilterPredicate;
import org.thingsboard.server.common.data.query.DynamicValue;
import org.thingsboard.server.common.data.query.DynamicValueSourceType;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.KeyFilterPredicate;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.query.StringFilterPredicate;

import lombok.NoArgsConstructor;

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
        KeyFilter filter = new KeyFilter();

        EntityKey entityKey = new EntityKey(
                EntityKeyType.valueOf(keyType), key);
        filter.setKey(entityKey);

        if (valueType != null) {
            filter.setValueType(EntityKeyValueType.valueOf(valueType));
        }

        KeyFilterPredicate predicate = buildPredicate();
        filter.setPredicate(predicate);

        return filter;
    }

    public KeyFilterInput(KeyFilter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("filter cannot be null");
        }
        if (filter.getKey() == null) {
            throw new IllegalArgumentException("filter.key cannot be null");
        }
        if (filter.getPredicate() == null) {
            throw new IllegalArgumentException("filter.predicate cannot be null");
        }

        this.keyType = filter.getKey().getType() != null ? filter.getKey().getType().name() : null;
        this.key = filter.getKey().getKey();

        this.valueType = filter.getValueType() != null ? filter.getValueType().name() : null;

        var p = filter.getPredicate();
        this.predicateType = p.getType().name();

        switch (p.getType()) {
            case STRING -> {
                var sp = (StringFilterPredicate) p;
                this.operation = sp.getOperation().name();
                this.ignoreCase = sp.isIgnoreCase();

                var v = sp.getValue();
                if (v != null) {
                    this.defaultValue = v.getDefaultValue();
                    this.userValue = v.getUserValue();
                    var dv = v.getDynamicValue();
                    if (dv != null) {
                        this.dynamicValueInherit = dv.isInherit();
                        this.dynamicValueSourceType = dv.getSourceType() != null ? dv.getSourceType().name() : null;
                        this.dynamicValueSourceAttribute = dv.getSourceAttribute();
                    }
                }
            }
            case NUMERIC -> {
                var np = (NumericFilterPredicate) p;
                this.operation = np.getOperation().name();

                var v = np.getValue();
                if (v != null) {
                    this.defaultValue = v.getDefaultValue();
                    this.userValue = v.getUserValue();
                    var dv = v.getDynamicValue();
                    if (dv != null) {
                        this.dynamicValueInherit = dv.isInherit();
                        this.dynamicValueSourceType = dv.getSourceType() != null ? dv.getSourceType().name() : null;
                        this.dynamicValueSourceAttribute = dv.getSourceAttribute();
                    }
                }
            }
            case BOOLEAN -> {
                var bp = (BooleanFilterPredicate) p;
                this.operation = bp.getOperation().name();

                var v = bp.getValue();
                if (v != null) {
                    this.defaultValue = v.getDefaultValue();
                    this.userValue = v.getUserValue();
                    var dv = v.getDynamicValue();
                    if (dv != null) {
                        this.dynamicValueInherit = dv.isInherit();
                        this.dynamicValueSourceType = dv.getSourceType() != null ? dv.getSourceType().name() : null;
                        this.dynamicValueSourceAttribute = dv.getSourceAttribute();
                    }
                }
            }
            case COMPLEX -> {
                var cp = (ComplexFilterPredicate) p;
                this.complexOperation = cp.getOperation().name();
                if (cp.getPredicates() != null && !cp.getPredicates().isEmpty()) {
                    this.nestedPredicates = cp.getPredicates().stream()
                            .map(kfp -> {
                                KeyFilter kf = new KeyFilter();
                                kf.setKey(filter.getKey());
                                kf.setValueType(filter.getValueType());
                                kf.setPredicate(kfp);
                                return new KeyFilterInput(kf);
                            })
                            .toList();
                }
            }
        }
    }

    private KeyFilterPredicate buildPredicate() {
        FilterPredicateType type = FilterPredicateType.valueOf(predicateType);

        return switch (type) {
            case STRING -> buildStringPredicate();
            case NUMERIC -> buildNumericPredicate();
            case BOOLEAN -> buildBooleanPredicate();
            case COMPLEX -> buildComplexPredicate();
        };
    }

    private StringFilterPredicate buildStringPredicate() {
        StringFilterPredicate predicate = new StringFilterPredicate();
        predicate.setOperation(StringFilterPredicate.StringOperation.valueOf(operation));
        predicate.setIgnoreCase(ignoreCase != null && ignoreCase);

        FilterPredicateValue<String> value = buildFilterValue(String.class);
        predicate.setValue(value);

        return predicate;
    }

    private NumericFilterPredicate buildNumericPredicate() {
        NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setOperation(NumericFilterPredicate.NumericOperation.valueOf(operation));

        FilterPredicateValue<Double> value = buildFilterValue(Double.class);
        predicate.setValue(value);

        return predicate;
    }

    private BooleanFilterPredicate buildBooleanPredicate() {
        BooleanFilterPredicate predicate = new BooleanFilterPredicate();
        predicate.setOperation(BooleanFilterPredicate.BooleanOperation.valueOf(operation));

        FilterPredicateValue<Boolean> value = buildFilterValue(Boolean.class);
        predicate.setValue(value);

        return predicate;
    }

    private ComplexFilterPredicate buildComplexPredicate() {
        ComplexFilterPredicate predicate = new ComplexFilterPredicate();
        predicate.setOperation(ComplexFilterPredicate.ComplexOperation.valueOf(complexOperation));

        if (nestedPredicates != null && !nestedPredicates.isEmpty()) {
            List<KeyFilterPredicate> predicates = nestedPredicates.stream()
                    .map(KeyFilterInput::buildPredicate)
                    .collect(Collectors.toList());
            predicate.setPredicates(predicates);
        }

        return predicate;
    }

    @SuppressWarnings("unchecked")
    private <T> FilterPredicateValue<T> buildFilterValue(Class<T> clazz) {
        T defaultVal = castValue(defaultValue, clazz);
        T userVal = castValue(userValue, clazz);
        DynamicValue<T> dynamicVal = null;

        if (dynamicValueSourceType != null) {
            DynamicValueSourceType sourceType = DynamicValueSourceType.valueOf(dynamicValueSourceType);
            boolean inherit = dynamicValueInherit != null && dynamicValueInherit;
            dynamicVal = new DynamicValue<>(sourceType, dynamicValueSourceAttribute, inherit);
        }

        return new FilterPredicateValue<>(defaultVal, userVal, dynamicVal);
    }

    @SuppressWarnings("unchecked")
    private <T> T castValue(Object value, Class<T> clazz) {
        if (value == null) {
            return null;
        }

        if (clazz == String.class) {
            return (T) String.valueOf(value);
        } else if (clazz == Double.class) {
            if (value instanceof Number) {
                return (T) Double.valueOf(((Number) value).doubleValue());
            }
            return (T) Double.valueOf(String.valueOf(value));
        } else if (clazz == Boolean.class) {
            if (value instanceof Boolean) {
                return (T) value;
            }
            return (T) Boolean.valueOf(String.valueOf(value));
        }

        return (T) value;
    }

}
