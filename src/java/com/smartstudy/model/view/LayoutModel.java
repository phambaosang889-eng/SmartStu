package com.smartstudy.model.view;

import author.User;
import java.util.List;

public record LayoutModel(
        User viewer,
        String base,
        String current,
        String title,
        String roleLabel,
        List<NavigationItem> navigation,
        String flash,
        int year) {
    public LayoutModel {
        navigation = List.copyOf(navigation);
    }

    public record NavigationItem(String key, String label, String icon, boolean active) {}
}
