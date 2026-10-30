package com.smartstudy.persistence;

import com.smartstudy.dao.JdbcMap;
import java.lang.reflect.Type;
import java.util.*;

public final class Stores {
    private Stores() {}

    public static <V> Map<String, V> map(String table, Type type) {
        return Database.memory() ? new LinkedHashMap<>() : new JdbcMap<>(table, type);
    }

    public static boolean seed() {
        return Database.memory() || "true".equals(Database.setting("SMARTSTU_SEED", "false"));
    }
}
