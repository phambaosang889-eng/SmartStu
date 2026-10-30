package com.smartstudy.dao;

import com.smartstudy.persistence.*;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;

/**
 * Typed aggregate DAO. Nested assessment snapshots remain JSON; identity and ownership are indexed
 * SQL columns.
 */
public final class JdbcMap<V> extends AbstractMap<String, V> {
    private final String table;
    private final Type type;

    public JdbcMap(String table, Type type) {
        if (!table.matches("[a-z_]+")) throw new IllegalArgumentException("Invalid DAO table");
        this.table = table;
        this.type = type;
    }

    @Override
    public V get(Object key) {
        if (key == null) return null;
        return Database.cached(
                table + "/key/" + key,
                () ->
                        Database.use(
                                c -> {
                                    try (var s =
                                            c.prepareStatement(
                                                    "SELECT payload FROM "
                                                            + table
                                                            + " WHERE row_key=?")) {
                                        s.setString(1, key.toString());
                                        try (var r = s.executeQuery()) {
                                            return r.next()
                                                    ? RecordCodec.decode(r.getString(1), type)
                                                    : null;
                                        }
                                    }
                                }));
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public V put(String key, V value) {
        Objects.requireNonNull(value);
        V old = get(key);
        Database.use(
                c -> {
                    try (var s =
                            c.prepareStatement(
                                    "INSERT INTO "
                                            + table
                                            + " (row_key,payload) VALUES (?,?) ON DUPLICATE KEY"
                                            + " UPDATE payload=VALUES(payload)")) {
                        s.setString(1, key);
                        s.setString(2, RecordCodec.encode(value));
                        s.executeUpdate();
                        return null;
                    }
                });
        Database.invalidate();
        return old;
    }

    @Override
    public V remove(Object key) {
        V old = get(key);
        if (old == null) return null;
        Database.use(
                c -> {
                    try (var s = c.prepareStatement("DELETE FROM " + table + " WHERE row_key=?")) {
                        s.setString(1, key.toString());
                        s.executeUpdate();
                        return null;
                    }
                });
        Database.invalidate();
        return old;
    }

    @Override
    public Set<Entry<String, V>> entrySet() {
        return Database.cached(
                table + "/all",
                () ->
                        Database.use(
                                c -> {
                                    Set<Entry<String, V>> rows = new LinkedHashSet<>();
                                    try (var s =
                                                    c.prepareStatement(
                                                            "SELECT row_key,payload FROM "
                                                                    + table
                                                                    + " ORDER BY sequence_id");
                                            var r = s.executeQuery()) {
                                        while (r.next())
                                            rows.add(
                                                    new SimpleImmutableEntry<>(
                                                            r.getString(1),
                                                            RecordCodec.decode(
                                                                    r.getString(2), type)));
                                    }
                                    return rows;
                                }));
    }

    public java.util.List<V> latest(int limit) {
        return Database.use(
                c -> {
                    java.util.List<V> rows = new ArrayList<>();
                    try (var s =
                            c.prepareStatement(
                                    "SELECT payload FROM "
                                            + table
                                            + " ORDER BY sequence_id DESC LIMIT ?")) {
                        s.setInt(1, Math.max(1, Math.min(limit, 1000)));
                        try (var r = s.executeQuery()) {
                            while (r.next()) rows.add(RecordCodec.decode(r.getString(1), type));
                        }
                    }
                    return rows;
                });
    }

    @Override
    public int size() {
        return Database.use(
                c -> {
                    try (var s = c.prepareStatement("SELECT COUNT(*) FROM " + table);
                            var r = s.executeQuery()) {
                        r.next();
                        return r.getInt(1);
                    }
                });
    }

    @Override
    public void replaceAll(
            java.util.function.BiFunction<? super String, ? super V, ? extends V> fn) {
        for (var e : entrySet()) put(e.getKey(), fn.apply(e.getKey(), e.getValue()));
    }
}
