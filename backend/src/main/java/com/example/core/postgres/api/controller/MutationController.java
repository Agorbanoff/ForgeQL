package com.example.core.postgres.api.controller;

import com.example.core.postgres.execution.MutationExecutor;
import com.example.core.postgres.plan.DeletePlanner;
import com.example.core.postgres.plan.InsertPlanner;
import com.example.core.postgres.plan.UpdatePlanner;
import com.example.core.postgres.sql.DeleteSqlBuilder;
import com.example.core.postgres.sql.InsertSqlBuilder;
import com.example.core.postgres.sql.UpdateSqlBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/core/datasources/{datasourceId}/tables")
public class MutationController {

    private final InsertPlanner insertPlanner;
    private final UpdatePlanner updatePlanner;
    private final DeletePlanner deletePlanner;
    private final InsertSqlBuilder insertSqlBuilder;
    private final UpdateSqlBuilder updateSqlBuilder;
    private final DeleteSqlBuilder deleteSqlBuilder;
    private final MutationExecutor mutationExecutor;

    @Autowired
    public MutationController(
            InsertPlanner insertPlanner,
            UpdatePlanner updatePlanner,
            DeletePlanner deletePlanner,
            InsertSqlBuilder insertSqlBuilder,
            UpdateSqlBuilder updateSqlBuilder,
            DeleteSqlBuilder deleteSqlBuilder,
            MutationExecutor mutationExecutor
    ) {
        this.insertPlanner = insertPlanner;
        this.updatePlanner = updatePlanner;
        this.deletePlanner = deletePlanner;
        this.insertSqlBuilder = insertSqlBuilder;
        this.updateSqlBuilder = updateSqlBuilder;
        this.deleteSqlBuilder = deleteSqlBuilder;
        this.mutationExecutor = mutationExecutor;
    }
}

