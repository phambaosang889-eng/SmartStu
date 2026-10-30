package com.smartstudy.persistence;

import author.*;
import com.smartstudy.service.Json;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;

/** Explicit target types, no polymorphic class names from stored JSON. */
public final class RecordCodec {
    private RecordCodec() {}

    public abstract static class TypeRef<T> {
        public Type type() {
            return ((ParameterizedType) getClass().getGenericSuperclass())
                    .getActualTypeArguments()[0];
        }
    }

    public static String encode(Object value) {
        return Json.write(tree(value));
    }

    private static Object tree(Object o) {
        if (o == null || o instanceof String || o instanceof Number || o instanceof Boolean)
            return o;
        if (o instanceof Enum<?>
                || o instanceof Instant
                || o instanceof LocalDate
                || o instanceof LocalDateTime) return o.toString();
        if (o instanceof byte[] bytes) return Base64.getEncoder().encodeToString(bytes);
        if (o instanceof User u)
            return Map.of("email", u.getEmail(), "name", u.getName(), "role", u.getRole().name());
        if (o instanceof Collection<?> list) return list.stream().map(RecordCodec::tree).toList();
        if (o.getClass().isRecord()) {
            Map<String, Object> fields = new LinkedHashMap<>();
            try {
                for (RecordComponent c : o.getClass().getRecordComponents()) {
                    var accessor = c.getAccessor();
                    accessor.setAccessible(true);
                    fields.put(c.getName(), tree(accessor.invoke(o)));
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            return fields;
        }
        throw new IllegalArgumentException("Unsupported stored type: " + o.getClass().getName());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T decode(String json, Type type) {
        return (T) convert(Json.parseStored(json), type);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convert(Object o, Type type) {
        if (o == null) return null;
        if (type instanceof ParameterizedType p) {
            Collection<Object> out =
                    p.getRawType() == Set.class ? new LinkedHashSet<>() : new ArrayList<>();
            for (Object item : (List<?>) o) out.add(convert(item, p.getActualTypeArguments()[0]));
            return out;
        }
        Class<?> c = (Class<?>) type;
        if (c == String.class || c == boolean.class || c == Boolean.class) return o;
        if (c == int.class || c == Integer.class) return ((Number) o).intValue();
        if (c == long.class || c == Long.class) return ((Number) o).longValue();
        if (c == double.class || c == Double.class) return ((Number) o).doubleValue();
        if (c == byte[].class) return Base64.getDecoder().decode((String) o);
        if (c == Instant.class) return Instant.parse((String) o);
        if (c == LocalDate.class) return LocalDate.parse((String) o);
        if (c == LocalDateTime.class) return LocalDateTime.parse((String) o);
        if (c.isEnum()) return Enum.valueOf((Class) c, (String) o);
        Map<?, ?> fields = (Map<?, ?>) o;
        if (c == User.class)
            return new User(
                    (String) fields.get("email"),
                    (String) fields.get("name"),
                    Role.valueOf((String) fields.get("role")));
        if (!c.isRecord()) throw new IllegalArgumentException("Unsupported record target");
        try {
            RecordComponent[] components = c.getRecordComponents();
            Object[] args = new Object[components.length];
            Class<?>[] types = new Class<?>[components.length];
            for (int i = 0; i < args.length; i++) {
                types[i] = components[i].getType();
                args[i] =
                        convert(
                                fields.get(components[i].getName()),
                                components[i].getGenericType());
            }
            Constructor<?> constructor = c.getDeclaredConstructor(types);
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Stored record cannot be decoded", e);
        }
    }
}
