package com.example.core.postgres.sql;

import com.example.common.exceptions.InvalidExecutionPlanException;
import com.example.core.postgres.plan.DeleteExecutionPlan;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.StringJoiner;

@Component
public class DeleteSqlBuilder {

    private final PostgresIdentifierQuoter identifierQuoter;

    public DeleteSqlBuilder(PostgresIdentifierQuoter identifierQuoter) {
        this.identifierQuoter = identifierQuoter;
    }

    public SqlCommand build(DeleteExecutionPlan executionPlan) {
        validateExecutionPlan(executionPlan);

        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ");
        sql.append(identifierQuoter.quoteQualified(executionPlan.tableIdentifier()));
        sql.append(" WHERE ");
        sql.append(identifierQuoter.quote(executionPlan.primaryKeyColumn()));
        sql.append(" = ?");
        sql.append(" RETURNING ");
        sql.append(buildReturningClause(executionPlan));

        return new SqlCommand(sql.toString(), List.of(executionPlan.primaryKeyValue()));
    }

    private void validateExecutionPlan(DeleteExecutionPlan executionPlan) {
        if (executionPlan == null) {
            throw new InvalidExecutionPlanException("Delete execution plan is required");
        }
        if (executionPlan.tableIdentifier() == null || executionPlan.tableIdentifier().isBlank()) {
            throw new InvalidExecutionPlanException("Delete execution plan table identifier is required");
        }
        if (executionPlan.primaryKeyColumn() == null || executionPlan.primaryKeyColumn().isBlank()) {
            throw new InvalidExecutionPlanException("Delete execution plan primary key column is required");
        }
        if (executionPlan.primaryKeyValue() == null) {
            throw new InvalidExecutionPlanException("Delete execution plan primary key value is required");
        }
        if (executionPlan.returningColumns() == null || executionPlan.returningColumns().isEmpty()) {
            throw new InvalidExecutionPlanException("Delete execution plan returning columns are required");
        }
    }

    private String buildReturningClause(DeleteExecutionPlan executionPlan) {
        StringJoiner returningJoiner = new StringJoiner(", ");
        for (String column : executionPlan.returningColumns()) {
            returningJoiner.add(
                    identifierQuoter.quote(column) + " AS " + identifierQuoter.quote(column)
            );
        }
        return returningJoiner.toString();
    }
}