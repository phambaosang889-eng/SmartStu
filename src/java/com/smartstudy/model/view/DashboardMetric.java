package com.smartstudy.model.view;

/** Read-only presentation data; independent of HTTP controllers. */
public record DashboardMetric(String label, String value, String note) {}
