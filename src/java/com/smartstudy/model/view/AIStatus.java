package com.smartstudy.model.view;

public record AIStatus(
        boolean configured,
        boolean enabled,
        int limit,
        long successes,
        long failures,
        String lastState) {}
