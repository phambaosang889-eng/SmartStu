package com.smartstudy.model.view;

import java.time.LocalDate;

public record ChartBar(LocalDate day, long count, int percent) {}
