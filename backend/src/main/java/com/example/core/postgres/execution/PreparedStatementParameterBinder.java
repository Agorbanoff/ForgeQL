package com.example.core.postgres.execution;

import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class PreparedStatementParameterBinder {

    public BoundSqlArrayResources bind(
            Connection connection,
            PreparedStatement preparedStatement,
            List<Object> parameters
    ) throws SQLException {
        List<java.sql.Array> createdArrays = new ArrayList<>();
        try {
            for (int index = 0; index < parameters.size(); index++) {
                bindParameter(connection, preparedStatement, index + 1, parameters.get(index), createdArrays);
            }
            return new BoundSqlArrayResources(createdArrays);
        } catch (SQLException | RuntimeException exception) {
            freeArraysQuietly(createdArrays);
            throw exception;
        }
    }

    private void bindParameter(
            Connection connection,
            PreparedStatement preparedStatement,
            int parameterIndex,
            Object parameter,
            List<java.sql.Array> createdArrays
    ) throws SQLException {
        if (parameter == null) {
            preparedStatement.setObject(parameterIndex, null);
            return;
        }

        if (parameter instanceof String stringValue) {
            preparedStatement.setObject(parameterIndex, stringValue, Types.OTHER);
            return;
        }

        if (parameter instanceof List<?> listValue) {
            bindArray(connection, preparedStatement, parameterIndex, listValue, createdArrays);
            return;
        }

        if (parameter.getClass().isArray()) {
            bindArray(connection, preparedStatement, parameterIndex, toList(parameter), createdArrays);
            return;
        }

        preparedStatement.setObject(parameterIndex, parameter);
    }

    private void bindArray(
            Connection connection,
            PreparedStatement preparedStatement,
            int parameterIndex,
            List<?> values,
            List<java.sql.Array> createdArrays
    ) throws SQLException {
        Object[] elements = values.toArray();
        String postgresTypeName = resolveArrayElementType(elements);
        java.sql.Array sqlArray = connection.createArrayOf(postgresTypeName, elements);
        createdArrays.add(sqlArray);
        preparedStatement.setArray(parameterIndex, sqlArray);
    }

    private String resolveArrayElementType(Object[] elements) {
        for (Object element : elements) {
            if (element == null) {
                continue;
            }
            if (element instanceof String) {
                return "text";
            }
            if (element instanceof Integer) {
                return "int4";
            }
            if (element instanceof Long) {
                return "int8";
            }
            if (element instanceof Short || element instanceof Byte) {
                return "int2";
            }
            if (element instanceof BigDecimal || element instanceof BigInteger) {
                return "numeric";
            }
            if (element instanceof Boolean) {
                return "bool";
            }
            if (element instanceof UUID) {
                return "uuid";
            }
            if (element instanceof LocalDateTime) {
                return "timestamp";
            }
            if (element instanceof OffsetDateTime) {
                return "timestamptz";
            }
            if (element instanceof LocalDate) {
                return "date";
            }
            if (element instanceof LocalTime) {
                return "time";
            }
            throw new IllegalArgumentException(
                    "Unsupported PostgreSQL array element type: " + element.getClass().getName()
            );
        }

        return "text";
    }

    private List<Object> toList(Object arrayValue) {
        int length = Array.getLength(arrayValue);
        List<Object> values = new ArrayList<>(length);
        for (int index = 0; index < length; index++) {
            values.add(Array.get(arrayValue, index));
        }
        return values;
    }

    private void freeArraysQuietly(List<java.sql.Array> arrays) {
        for (java.sql.Array array : arrays) {
            try {
                array.free();
            } catch (SQLException ignored) {
                // Ignore cleanup errors because the original JDBC failure is more relevant.
            }
        }
    }

    public record BoundSqlArrayResources(List<java.sql.Array> arrays) implements AutoCloseable {
        public BoundSqlArrayResources {
            arrays = arrays == null ? List.of() : List.copyOf(arrays);
        }

        @Override
        public void close() {
            for (java.sql.Array array : arrays) {
                try {
                    array.free();
                } catch (SQLException ignored) {
                    // Ignore cleanup errors during statement teardown.
                }
            }
        }
    }
}
