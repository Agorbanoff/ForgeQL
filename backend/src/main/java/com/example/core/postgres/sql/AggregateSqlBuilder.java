package com.example.core.postgres.sql;

import com.example.common.exceptions.InvalidExecutionPlanException;
import com.example.core.postgres.ast.AggregateSelectionAst;
import com.example.core.postgres.ast.FilterAst;
import com.example.core.postgres.ast.ReadFilterOperator;
import com.example.core.postgres.plan.AggregateExecutionPlan;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

@Component
public class AggregateSqlBuilder {

    private static final String TABLE_ALIAS = "t";

    private final PostgresIdentifierQuoter identifierQuoter;

    public AggregateSqlBuilder(PostgresIdentifierQuoter identifierQuoter) {
        this.identifierQuoter = identifierQuoter;
    }

    public SqlCommand build(AggregateExecutionPlan executionPlan) {
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
        appendGroupByClause(sql, executionPlan.groupBy());

        return new SqlCommand(sql.toString(), Collections.unmodifiableList(parameters));
    }

    private void validateExecutionPlan(AggregateExecutionPlan executionPlan) {
        if (executionPlan == null) {
            throw new InvalidExecutionPlanException("Aggregate execution plan is required");
        }
        if (executionPlan.tableIdentifier() == null || executionPlan.tableIdentifier().isBlank()) {
            throw new InvalidExecutionPlanException("Aggregate execution plan table identifier is required");
        }
        if (executionPlan.selections() == null || executionPlan.selections().isEmpty()) {
            throw new InvalidExecutionPlanException("Aggregate execution plan selections are required");
        }
        if (executionPlan.groupBy() == null) {
            throw new InvalidExecutionPlanException("Aggregate execution plan groupBy is required");
        }
        if (executionPlan.filters() == null) {
            throw new InvalidExecutionPlanException("Aggregate execution plan filters are required");
        }

        validateOutputLabels(executionPlan);
    }

    private void validateOutputLabels(AggregateExecutionPlan executionPlan) {
        Set<String> outputLabels = new LinkedHashSet<>();

        for (String field : executionPlan.groupBy()) {
            if (field == null || field.isBlank()) {
                throw new InvalidExecutionPlanException("Aggregate execution plan groupBy field is required");
            }
            String normalizedField = field.trim();
            if (!outputLabels.add(normalizedField)) {
                throw new InvalidExecutionPlanException(
                        "Aggregate execution plan contains duplicate output label " + normalizedField
                );
            }
        }

        for (AggregateSelectionAst selection : executionPlan.selections()) {
            if (selection == null) {
                throw new InvalidExecutionPlanException("Aggregate execution plan selection is required");
            }
            if (selection.alias() == null || selection.alias().isBlank()) {
                throw new InvalidExecutionPlanException("Aggregate execution plan selection alias is required");
            }
            String normalizedAlias = selection.alias().trim();
            if (!outputLabels.add(normalizedAlias)) {
                throw new InvalidExecutionPlanException(
                        "Aggregate execution plan contains duplicate output label " + normalizedAlias
                );
            }
        }
    }

    private String buildProjectionClause(AggregateExecutionPlan executionPlan) {
        StringJoiner projectionJoiner = new StringJoiner(", ");

        for (String groupByField : executionPlan.groupBy()) {
            projectionJoiner.add(
                    identifierQuoter.quoteColumnReference(TABLE_ALIAS, groupByField)
                            + " AS "
                            + identifierQuoter.quote(groupByField)
            );
        }

        for (AggregateSelectionAst selection : executionPlan.selections()) {
            projectionJoiner.add(buildAggregateExpression(selection));
        }

        return projectionJoiner.toString();
    }

    private String buildAggregateExpression(AggregateSelectionAst selection) {
        String aggregateSql = switch (selection.function()) {
            case COUNT -> selection.field() == null
                    ? "COUNT(*)"
                    : "COUNT(" + identifierQuoter.quoteColumnReference(TABLE_ALIAS, selection.field()) + ")";
            case SUM, AVG, MIN, MAX -> selection.function().wireName().toUpperCase()
                    + "(" + identifierQuoter.quoteColumnReference(TABLE_ALIAS, selection.field()) + ")";
        };

        return aggregateSql + " AS " + identifierQuoter.quote(selection.alias());
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
                    "Aggregate execution plan filter " + filter.field() + "." + filter.operator().wireName()
                            + " requires exactly one value"
            );
        }

        parameters.add(filter.values().get(0));
        return columnReference + " " + operator + " ?";
    }

    private String bindInExpression(String columnReference, FilterAst filter, List<Object> parameters) {
        if (filter.values().isEmpty()) {
            throw new InvalidExecutionPlanException(
                    "Aggregate execution plan filter " + filter.field() + "." + ReadFilterOperator.IN.wireName()
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
                    "Aggregate execution plan filter " + filter.field() + "." + ReadFilterOperator.BETWEEN.wireName()
                            + " requires exactly two values"
            );
        }

        parameters.add(filter.values().get(0));
        parameters.add(filter.values().get(1));
        return columnReference + " BETWEEN ? AND ?";
    }

    private void appendGroupByClause(StringBuilder sql, List<String> groupBy) {
        if (groupBy.isEmpty()) {
            return;
        }

        StringJoiner groupByJoiner = new StringJoiner(", ");
        for (String field : groupBy) {
            groupByJoiner.add(identifierQuoter.quoteColumnReference(TABLE_ALIAS, field));
        }

        sql.append(" GROUP BY ").append(groupByJoiner);
    }
}

