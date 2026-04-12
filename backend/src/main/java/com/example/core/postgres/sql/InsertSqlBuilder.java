package com.example.core.postgres.sql;

import com.example.common.exceptions.InvalidExecutionPlanException;
import com.example.core.postgres.plan.InsertExecutionPlan;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

@Component
public class InsertSqlBuilder {

    private final PostgresIdentifierQuoter identifierQuoter;

    public InsertSqlBuilder(PostgresIdentifierQuoter identifierQuoter) {
        this.identifierQuoter = identifierQuoter;
    }

    public SqlCommand build(InsertExecutionPlan executionPlan) {
        validateExecutionPlan(executionPlan);

        List<Object> parameters = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ");
        sql.append(identifierQuoter.quoteQualified(executionPlan.tableIdentifier()));
        sql.append(" (");
        sql.append(buildInsertColumnsClause(executionPlan, parameters));
        sql.append(") VALUES (");
        sql.append(buildValuesClause(executionPlan.values().size()));
        sql.append(") RETURNING ");
        sql.append(buildReturningClause(executionPlan));

        return new SqlCommand(sql.toString(), Collections.unmodifiableList(parameters));
    }

    private void validateExecutionPlan(InsertExecutionPlan executionPlan) {
        if (executionPlan == null) {
            throw new InvalidExecutionPlanException("Insert execution plan is required");
        }
        if (executionPlan.tableIdentifier() == null || executionPlan.tableIdentifier().isBlank()) {
            throw new InvalidExecutionPlanException("Insert execution plan table identifier is required");
        }
        if (executionPlan.primaryKeyColumn() == null || executionPlan.primaryKeyColumn().isBlank()) {
            throw new InvalidExecutionPlanException("Insert execution plan primary key column is required");
        }
        if (executionPlan.values() == null || executionPlan.values().isEmpty()) {
            throw new InvalidExecutionPlanException("Insert execution plan values are required");
        }
        if (executionPlan.returningColumns() == null || executionPlan.returningColumns().isEmpty()) {
            throw new InvalidExecutionPlanException("Insert execution plan returning columns are required");
        }
    }

    private String buildInsertColumnsClause(InsertExecutionPlan executionPlan, List<Object> parameters) {
        StringJoiner columnsJoiner = new StringJoiner(", ");
        for (var entry : executionPlan.values().entrySet()) {
            columnsJoiner.add(identifierQuoter.quote(entry.getKey()));
            parameters.add(entry.getValue());
        }
        return columnsJoiner.toString();
    }

    private String buildValuesClause(int valueCount) {
        StringJoiner placeholders = new StringJoiner(", ");
        for (int index = 0; index < valueCount; index++) {
            placeholders.add("?");
        }
        return placeholders.toString();
    }

    private String buildReturningClause(InsertExecutionPlan executionPlan) {
        StringJoiner returningJoiner = new StringJoiner(", ");
        for (String column : executionPlan.returningColumns()) {
            returningJoiner.add(
                    identifierQuoter.quote(column) + " AS " + identifierQuoter.quote(column)
            );
        }
        return returningJoiner.toString();
    }
}
