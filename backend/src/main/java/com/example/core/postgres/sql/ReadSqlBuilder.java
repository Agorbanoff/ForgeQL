package com.example.core.postgres.sql;

import com.example.common.exceptions.InvalidExecutionPlanException;
import com.example.core.postgres.ast.FilterAst;
import com.example.core.postgres.ast.ReadFilterOperator;
import com.example.core.postgres.ast.SortAst;
import com.example.core.postgres.plan.ReadExecutionPlan;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

@Component
public class ReadSqlBuilder {

    private static final String TABLE_ALIAS = "t";

    private final PostgresIdentifierQuoter identifierQuoter;

    public ReadSqlBuilder(PostgresIdentifierQuoter identifierQuoter) {
        this.identifierQuoter = identifierQuoter;
    }

    public SqlCommand build(ReadExecutionPlan executionPlan) {
        validateExecutionPlan(executionPlan);

        List<Object> parameters = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(buildProjectionClause(executionPlan));
        sql.append(" FROM ");
        sql.append(identifierQuoter.quoteQualified(executionPlan.tableIdentifier()));
        sql.append(" AS ");
        sql.append(identifierQuoter.quote(TABLE_ALIAS));

        appendWhereClause(sql, parameters, executionPlan.filters());
        appendOrderByClause(sql, executionPlan.sorts());
        appendPaginationClause(sql, parameters, executionPlan);

        return new SqlCommand(sql.toString(), Collections.unmodifiableList(parameters));
    }

    private void validateExecutionPlan(ReadExecutionPlan executionPlan) {
        if (executionPlan == null) {
            throw new InvalidExecutionPlanException("Read execution plan is required");
        }
        if (executionPlan.tableIdentifier() == null || executionPlan.tableIdentifier().isBlank()) {
            throw new InvalidExecutionPlanException("Read execution plan table identifier is required");
        }
        if (executionPlan.projection() == null || executionPlan.projection().columns().isEmpty()) {
            throw new InvalidExecutionPlanException("Read execution plan projection must contain explicit columns");
        }
        if (executionPlan.filters() == null) {
            throw new InvalidExecutionPlanException("Read execution plan filters are required");
        }
        if (executionPlan.sorts() == null) {
            throw new InvalidExecutionPlanException("Read execution plan sorts are required");
        }
        if (executionPlan.pagination() == null) {
            throw new InvalidExecutionPlanException("Read execution plan pagination is required");
        }
        if (executionPlan.pagination().offset() == null) {
            throw new InvalidExecutionPlanException("Read execution plan pagination offset must be normalized");
        }
    }

    private String buildProjectionClause(ReadExecutionPlan executionPlan) {
        StringJoiner projectionJoiner = new StringJoiner(", ");
        for (String column : executionPlan.projection().columns()) {
            projectionJoiner.add(
                    identifierQuoter.quoteColumnReference(TABLE_ALIAS, column)
                            + " AS "
                            + identifierQuoter.quote(column)
            );
        }
        return projectionJoiner.toString();
    }

    private void appendWhereClause(StringBuilder sql, List<Object> parameters, List<FilterAst> filters) {
        if (filters.isEmpty()) {
            return;
        }

        StringJoiner filterJoiner = new StringJoiner(" AND ");
        for (FilterAst filter : filters) {
            filterJoiner.add(buildFilterExpression(filter, parameters));
        }

        sql.append(" WHERE ").append(filterJoiner);
    }

    private String buildFilterExpression(FilterAst filter, List<Object> parameters) {
        String columnReference = identifierQuoter.quoteColumnReference(TABLE_ALIAS, filter.field());
        return switch (filter.operator()) {
            case EQ -> bindSingleValueExpression(columnReference, "=", filter, parameters);
            case NE -> bindSingleValueExpression(columnReference, "<>", filter, parameters);
            case GT -> bindSingleValueExpression(columnReference, ">", filter, parameters);
            case GTE -> bindSingleValueExpression(columnReference, ">=", filter, parameters);
            case LT -> bindSingleValueExpression(columnReference, "<", filter, parameters);
            case LTE -> bindSingleValueExpression(columnReference, "<=", filter, parameters);
            case LIKE -> bindSingleValueExpression(columnReference, "LIKE", filter, parameters);
            case ILIKE -> bindSingleValueExpression(columnReference, "ILIKE", filter, parameters);
            case IN -> bindInExpression(columnReference, filter, parameters);
            case BETWEEN -> bindBetweenExpression(columnReference, filter, parameters);
            case IS_NULL -> columnReference + " IS NULL";
            case IS_NOT_NULL -> columnReference + " IS NOT NULL";
        };
    }

    private String bindSingleValueExpression(
            String columnReference,
            String operator,
            FilterAst filter,
            List<Object> parameters
    ) {
        if (filter.values().size() != 1) {
            throw new InvalidExecutionPlanException(
                    "Read execution plan filter " + filter.field() + "." + filter.operator().wireName()
                            + " requires exactly one value"
            );
        }

        parameters.add(filter.values().get(0));
        return columnReference + " " + operator + " ?";
    }

    private String bindInExpression(String columnReference, FilterAst filter, List<Object> parameters) {
        if (filter.values().isEmpty()) {
            throw new InvalidExecutionPlanException(
                    "Read execution plan filter " + filter.field() + "." + ReadFilterOperator.IN.wireName()
                            + " must contain at least one value"
            );
        }

        StringJoiner placeholders = new StringJoiner(", ", "(", ")");
        for (Object value : filter.values()) {
            placeholders.add("?");
            parameters.add(value);
        }

        return columnReference + " IN " + placeholders;
    }

    private String bindBetweenExpression(String columnReference, FilterAst filter, List<Object> parameters) {
        if (filter.values().size() != 2) {
            throw new InvalidExecutionPlanException(
                    "Read execution plan filter " + filter.field() + "." + ReadFilterOperator.BETWEEN.wireName()
                            + " requires exactly two values"
            );
        }

        parameters.add(filter.values().get(0));
        parameters.add(filter.values().get(1));
        return columnReference + " BETWEEN ? AND ?";
    }

    private void appendOrderByClause(StringBuilder sql, List<SortAst> sorts) {
        if (sorts.isEmpty()) {
            return;
        }

        StringJoiner orderByJoiner = new StringJoiner(", ");
        for (SortAst sort : sorts) {
            orderByJoiner.add(
                    identifierQuoter.quoteColumnReference(TABLE_ALIAS, sort.field())
                            + " "
                            + sort.direction().name()
            );
        }

        sql.append(" ORDER BY ").append(orderByJoiner);
    }

    private void appendPaginationClause(
            StringBuilder sql,
            List<Object> parameters,
            ReadExecutionPlan executionPlan
    ) {
        Integer limit = executionPlan.pagination().limit();
        Integer offset = executionPlan.pagination().offset();

        if (limit != null) {
            sql.append(" LIMIT ?");
            parameters.add(limit);

            sql.append(" OFFSET ?");
            parameters.add(offset);
            return;
        }

        if (offset != null && offset > 0) {
            sql.append(" OFFSET ?");
            parameters.add(offset);
        }
    }
}
