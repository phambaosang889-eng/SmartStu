package com.smartstudy.model.view;

import java.util.List;

public record GrowthChart(String title, List<ChartBar> bars) {
    public GrowthChart {
        bars = List.copyOf(bars);
    }
}
