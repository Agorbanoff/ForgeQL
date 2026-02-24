package com.example.SigmaQL.registry;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelationSchema {
    private String targetEntity;
    private String type;
    private String joinColumn;
    private String referencedColumn;
}
