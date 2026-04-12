package com.example.core.postgres.execution;

import com.example.common.exceptions.InvalidMutationValueException;
import com.example.common.exceptions.MissingRequiredFieldException;
import com.example.common.exceptions.UnsupportedMutationTypeException;
import com.example.core.postgres.schema.model.SchemaColumn;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ValueCoercionService {

    private static final BigInteger SMALLINT_MIN = BigInteger.valueOf(Short.MIN_VALUE);
    private static final BigInteger SMALLINT_MAX = BigInteger.valueOf(Short.MAX_VALUE);
    private static final BigInteger INTEGER_MIN = BigInteger.valueOf(Integer.MIN_VALUE);
    private static final BigInteger INTEGER_MAX = BigInteger.valueOf(Integer.MAX_VALUE);
    private static final BigInteger BIGINT_MIN = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger BIGINT_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    private static final Set<String> STRING_LIKE_TYPES = Set.of(
            "text",
            "varchar",
            "bpchar",
            "char",
            "citext",
            "name"
    );

    private final ObjectMapper objectMapper;

    public ValueCoercionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Object coerceValue(SchemaColumn column, Object value) {
        if (column == null) {
            throw new MissingRequiredFieldException("mutation column metadata is required");
        }

        return coerceValue(column, value, column.name(), false, column.nullable());
    }

    public Map<String, Object> coerceValues(
            Map<String, Object> values,
            Map<String, SchemaColumn> columnsByName
    ) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        if (columnsByName == null) {
            throw new MissingRequiredFieldException("mutation column metadata map is required");
        }

        LinkedHashMap<String, Object> coercedValues = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String fieldName = normalizeRequiredFieldName(entry.getKey());
            SchemaColumn column = columnsByName.get(fieldName);
            if (column == null) {
                throw new InvalidMutationValueException("Unknown mutation column: " + fieldName);
            }
            coercedValues.put(fieldName, coerceValue(column, entry.getValue()));
        }

        return Collections.unmodifiableMap(coercedValues);
    }

    private Object coerceValue(
            SchemaColumn column,
            Object value,
            String fieldPath,
            boolean arrayElement,
            boolean nullable
    ) {
        if (value == null) {
            if (!nullable) {
                throw new InvalidMutationValueException("Column " + fieldPath + " does not allow null values");
            }
            return null;
        }

        if (!arrayElement && column.arrayType()) {
            return coerceArray(column, value, fieldPath);
        }
        if (column.enumType()) {
            return coerceEnum(column, value, fieldPath);
        }
        if (column.uuidType()) {
            return coerceUuid(value, fieldPath);
        }
        if (column.jsonType() || column.jsonbType()) {
            return coerceJson(value, fieldPath);
        }
        if (column.timestampWithTimeZone()) {
            return coerceTimestampWithTimeZone(value, fieldPath);
        }
        if (column.timestampWithoutTimeZone()) {
            return coerceTimestampWithoutTimeZone(value, fieldPath);
        }
        if (column.numericType()) {
            return coerceNumeric(column, value, fieldPath);
        }

        String typeName = normalizeTypeName(arrayElement ? column.arrayElementTypeName() : column.postgresTypeName());
        return switch (typeName) {
            case "bool" -> coerceBoolean(value, fieldPath);
            case "int2" -> coerceSmallint(value, fieldPath);
            case "int4" -> coerceInteger(value, fieldPath);
            case "int8" -> coerceBigint(value, fieldPath);
            default -> {
                if (STRING_LIKE_TYPES.contains(typeName)) {
                    yield coerceString(column, value, fieldPath);
                }
                throw unsupportedType(column, fieldPath, arrayElement);
            }
        };
    }

    private List<Object> coerceArray(SchemaColumn column, Object value, String fieldPath) {
        List<Object> rawElements = toArrayElements(value, fieldPath);
        List<Object> coercedElements = new ArrayList<>(rawElements.size());
        for (int index = 0; index < rawElements.size(); index++) {
            coercedElements.add(
                    coerceValue(
                            column,
                            rawElements.get(index),
                            fieldPath + "[" + index + "]",
                            true,
                            column.nullable()
                    )
            );
        }
        return List.copyOf(coercedElements);
    }

    private List<Object> toArrayElements(Object value, String fieldPath) {
        if (value instanceof List<?> list) {
            return new ArrayList<>(list);
        }

        if (value instanceof Iterable<?> iterable) {
            List<Object> elements = new ArrayList<>();
            for (Object element : iterable) {
                elements.add(element);
            }
            return elements;
        }

        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> elements = new ArrayList<>(length);
            for (int index = 0; index < length; index++) {
                elements.add(Array.get(value, index));
            }
            return elements;
        }

        throw new InvalidMutationValueException(
                "Column " + fieldPath + " must receive an array-style value"
        );
    }

    private Object coerceBoolean(Object value, String fieldPath) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        throw new InvalidMutationValueException("Column " + fieldPath + " must receive a boolean value");
    }

    private Short coerceSmallint(Object value, String fieldPath) {
        return requireIntegralNumber(value, fieldPath, SMALLINT_MIN, SMALLINT_MAX).shortValueExact();
    }

    private Integer coerceInteger(Object value, String fieldPath) {
        return requireIntegralNumber(value, fieldPath, INTEGER_MIN, INTEGER_MAX).intValueExact();
    }

    private Long coerceBigint(Object value, String fieldPath) {
        return requireIntegralNumber(value, fieldPath, BIGINT_MIN, BIGINT_MAX).longValueExact();
    }

    private BigDecimal coerceNumeric(SchemaColumn column, Object value, String fieldPath) {
        BigDecimal numericValue = requireNumericValue(value, fieldPath);

        Integer precision = column.precision();
        Integer scale = column.scale();
        if (precision != null || scale != null) {
            int fractionalDigits = Math.max(numericValue.stripTrailingZeros().scale(), 0);
            int integerDigits = computeIntegerDigits(numericValue);

            if (scale != null && fractionalDigits > scale) {
                throw new InvalidMutationValueException(
                        "Column " + fieldPath + " exceeds declared numeric scale " + scale
                );
            }
            if (precision != null) {
                int allowedIntegerDigits = scale == null ? precision : precision - scale;
                if (allowedIntegerDigits < 0 || integerDigits > allowedIntegerDigits) {
                    throw new InvalidMutationValueException(
                            "Column " + fieldPath + " exceeds declared numeric precision " + precision
                                    + (scale == null ? "" : " and scale " + scale)
                    );
                }
                if (scale == null && integerDigits + fractionalDigits > precision) {
                    throw new InvalidMutationValueException(
                            "Column " + fieldPath + " exceeds declared numeric precision " + precision
                    );
                }
            }
        }

        return numericValue;
    }

    private UUID coerceUuid(Object value, String fieldPath) {
        if (value instanceof UUID uuidValue) {
            return uuidValue;
        }
        if (!(value instanceof String stringValue)) {
            throw new InvalidMutationValueException("Column " + fieldPath + " must receive a canonical UUID string");
        }

        String normalizedValue = stringValue.trim();
        try {
            UUID uuid = UUID.fromString(normalizedValue);
            if (!uuid.toString().equalsIgnoreCase(normalizedValue)) {
                throw new InvalidMutationValueException(
                        "Column " + fieldPath + " must receive a canonical UUID string"
                );
            }
            return uuid;
        } catch (IllegalArgumentException e) {
            throw new InvalidMutationValueException(
                    "Column " + fieldPath + " must receive a canonical UUID string"
            );
        }
    }

    private String coerceEnum(SchemaColumn column, Object value, String fieldPath) {
        if (!(value instanceof String stringValue)) {
            throw new InvalidMutationValueException("Column " + fieldPath + " must receive a string enum label");
        }

        String enumValue = stringValue.trim();
        if (enumValue.isEmpty()) {
            throw new InvalidMutationValueException("Column " + fieldPath + " must not be blank");
        }
        if (!column.enumLabels().contains(enumValue)) {
            throw new InvalidMutationValueException(
                    "Column " + fieldPath + " must match one of the PostgreSQL enum labels"
            );
        }

        return enumValue;
    }

    private String coerceJson(Object value, String fieldPath) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new InvalidMutationValueException("Column " + fieldPath + " must receive a valid JSON value");
        }
    }

    private OffsetDateTime coerceTimestampWithTimeZone(Object value, String fieldPath) {
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        if (!(value instanceof String stringValue)) {
            throw new InvalidMutationValueException(
                    "Column " + fieldPath + " must receive an ISO-8601 timestamp with offset or Z"
            );
        }

        try {
            return OffsetDateTime.parse(stringValue.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidMutationValueException(
                    "Column " + fieldPath + " must receive an ISO-8601 timestamp with offset or Z"
            );
        }
    }

    private LocalDateTime coerceTimestampWithoutTimeZone(Object value, String fieldPath) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (!(value instanceof String stringValue)) {
            throw new InvalidMutationValueException(
                    "Column " + fieldPath + " must receive an ISO-8601 timestamp without timezone"
            );
        }

        String normalizedValue = stringValue.trim();
        try {
            return LocalDateTime.parse(normalizedValue);
        } catch (DateTimeParseException e) {
            throw new InvalidMutationValueException(
                    "Column " + fieldPath + " must receive an ISO-8601 timestamp without timezone"
            );
        }
    }

    private String coerceString(SchemaColumn column, Object value, String fieldPath) {
        if (!(value instanceof String stringValue)) {
            throw new InvalidMutationValueException("Column " + fieldPath + " must receive a string value");
        }

        String normalizedValue = stringValue.trim();
        if (column.length() != null && normalizedValue.length() > column.length()) {
            throw new InvalidMutationValueException(
                    "Column " + fieldPath + " exceeds declared maximum length " + column.length()
            );
        }

        return normalizedValue;
    }

    private BigInteger requireIntegralNumber(
            Object value,
            String fieldPath,
            BigInteger minimum,
            BigInteger maximum
    ) {
        BigDecimal numericValue = requireNumericValue(value, fieldPath);
        BigDecimal normalized = numericValue.stripTrailingZeros();
        if (normalized.scale() > 0) {
            throw new InvalidMutationValueException("Column " + fieldPath + " must receive an integer numeric value");
        }

        BigInteger integerValue;
        try {
            integerValue = normalized.toBigIntegerExact();
        } catch (ArithmeticException e) {
            throw new InvalidMutationValueException("Column " + fieldPath + " must receive an integer numeric value");
        }

        if (integerValue.compareTo(minimum) < 0 || integerValue.compareTo(maximum) > 0) {
            throw new InvalidMutationValueException("Column " + fieldPath + " is out of range");
        }

        return integerValue;
    }

    private BigDecimal requireNumericValue(Object value, String fieldPath) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof BigInteger bigInteger) {
            return new BigDecimal(bigInteger);
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return BigDecimal.valueOf(((Number) value).longValue());
        }
        if (value instanceof Float || value instanceof Double) {
            double doubleValue = ((Number) value).doubleValue();
            if (!Double.isFinite(doubleValue)) {
                throw new InvalidMutationValueException("Column " + fieldPath + " must receive a finite numeric value");
            }
            return BigDecimal.valueOf(doubleValue);
        }

        throw new InvalidMutationValueException("Column " + fieldPath + " must receive a numeric value");
    }

    private int computeIntegerDigits(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        int scale = normalized.scale();
        if (scale < 0) {
            return normalized.precision() - scale;
        }

        BigDecimal absoluteValue = normalized.abs();
        if (absoluteValue.compareTo(BigDecimal.ONE) < 0) {
            return 0;
        }

        return absoluteValue.precision() - scale;
    }

    private UnsupportedMutationTypeException unsupportedType(
            SchemaColumn column,
            String fieldPath,
            boolean arrayElement
    ) {
        String postgresTypeName = arrayElement
                ? normalizeTypeName(column.arrayElementTypeName())
                : normalizeTypeName(column.postgresTypeName());
        String dbType = column.dbType() == null || column.dbType().isBlank() ? postgresTypeName : column.dbType();
        return new UnsupportedMutationTypeException(
                "Column " + fieldPath + " uses unsupported PostgreSQL mutation type " + dbType
        );
    }

    private String normalizeRequiredFieldName(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            throw new MissingRequiredFieldException("mutation field name is required");
        }
        return fieldName.trim();
    }

    private String normalizeTypeName(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            return "";
        }
        return typeName.trim().toLowerCase(Locale.ROOT);
    }
}
