package com.example.core.postgres.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.Map;

public record ReadRowsRequest(
        List<@NotBlank(message = "columns cannot contain blank values") String> columns,
        Map<@NotBlank(message = "filter fields cannot be blank") String, @Valid ReadFieldFilterRequest> filter,
        List<@Valid ReadSortRequest> sort,
        @Positive(message = "limit must be positive")
        Integer limit,
        @PositiveOrZero(message = "offset must be zero or positive")
        Integer offset
) {
}
