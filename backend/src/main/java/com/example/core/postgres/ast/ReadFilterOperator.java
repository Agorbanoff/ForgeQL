package com.example.core.postgres.ast;

public enum ReadFilterOperator {
    EQ("eq"),
    NE("ne"),
    GT("gt"),
    GTE("gte"),
    LT("lt"),
    LTE("lte"),
    IN("in"),
    BETWEEN("between"),
    LIKE("like"),
    ILIKE("ilike"),
    IS_NULL("isNull"),
    IS_NOT_NULL("isNotNull");

    private final String wireName;

    ReadFilterOperator(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }
}
