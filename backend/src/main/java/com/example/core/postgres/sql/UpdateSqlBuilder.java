package com.example.core.postgres.sql;

import com.example.common.exceptions.InvalidExecutionPlanException;
import com.example.core.postgres.plan.UpdateExecutionPlan;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

@Component
public class UpdateSqlBuilder {

    private final PostgresIdentifierQuoter identifierQuoter;

    public UpdateSqlBuilder(PostgresIdentifierQuoter identifierQuoter) {
        this.identifierQuoter = identifierQuoter;
    }

    public SqlCommand build(UpdateExecutionPlan executionPlan) {
        validateExecutionPlan(executionPlan);

        List<Object> parameters = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ");
        sql.append(identifierQuoter.quoteQualified(executionPlan.tableIdentifier()));
        sql.append(" SET ");
        sql.append(buildSetClause(executionPlan, parameters));
        sql.append(" WHERE ");
        sql.append(identifierQuoter.quote(executionPlan.primaryKeyColumn()));
        sql.append(" = ?");
        parameters.add(executionPlan.primaryKeyValue());
        sql.append(" RETURNING ");
        sql.append(buildReturningClause(executionPlan));

        return new SqlCommand(sql.toString(), Collections.unmodifiableList(parameters));
    }

    private void validateExecutionPlan(UpdateExecutionPlan executionPlan) {
        if (executionPlan == null) {
            throw new InvalidExecutionPlanException("Update execution plan is required");
        }
        if (executionPlan.tableIdentifier() == null || executionPlan.tableIdentifier().isBlank()) {
            throw new InvalidExecutionPlanException("Update execution plan table identifier is required");
        }
        if (executionPlan.primaryKeyColumn() == null || executionPlan.primaryKeyColumn().isBlank()) {
            throw new InvalidExecutionPlanException("Update execution plan primary key column is required");
        }
        if (executionPlan.primaryKeyValue() == null) {
            throw new InvalidExecutionPlanException("Update execution plan primary key value is required");
        }
        if (executionPlan.values() == null || executionPlan.values().isEmpty()) {
            throw new InvalidExecutionPlanException("Update execution plan values are required");
        }
        if (executionPlan.returningColumns() == null || executionPlan.returningColumns().isEmpty()) {
            throw new InvalidExecutionPlanException("Update execution plan returning columns are required");
        }
    }

    private String buildSetClause(UpdateExecutionPlan executionPlan, List<Object> parameters) {
        StringJoiner setJoiner = new StringJoiner(", ");
        for (var entry : executionPlan.values().entrySet()) {
            setJoiner.add(identifierQuoter.quote(entry.getKey()) + " = ?");
            parameters.add(entry.getValue());
        }
        return setJoiner.toString();
    }

    private String buildReturningClause(UpdateExecutionPlan executionPlan) {
        StringJoiner returningJoiner = new StringJoiner(", ");
        for (String column : executionPlan.returningColumns()) {
            returningJoiner.add(
                    identifierQuoter.quote(column) + " AS " + identifierQuoter.quote(column)
            );
        }
        return returningJoiner.toString();
    }
}
