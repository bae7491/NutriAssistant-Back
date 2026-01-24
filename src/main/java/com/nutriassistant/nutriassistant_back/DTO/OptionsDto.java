package com.nutriassistant.nutriassistant_back.DTO;

public record OptionsDto(
        ConstraintsDto constraints,
        int numGenerations
) {}
