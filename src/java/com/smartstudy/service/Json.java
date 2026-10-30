package com.smartstudy.service;

import java.util.*;

/** Small bounded JSON codec for the optional server-side AI gateway contract. */
public final class Json {
    private final String text;
    private int pos;
    private int arrayLimit = 200;

    private Json(String text) {
        this.text = text;
    }

    /**
     * Separate limits for trusted database aggregates; AI parsing keeps its original strict bound.
     */
    public static Object parseStored(String text) {
        if (text == null || text.length() > 4_000_000)
            throw new IllegalArgumentException("Dữ liệu lưu quá lớn.");
        Json p = new Json(text);
        p.arrayLimit = 10000;
        Object value = p.value(0);
        p.space();
        if (p.pos != text.length())
            throw new IllegalArgumentException("Dữ liệu JSON có phần thừa.");
        return value;
    }

    public static Object parse(String text) {
        if (text == null || text.length() > 65536)
            throw new IllegalArgumentException("Phản hồi JSON quá lớn.");
        Json p = new Json(text);
        Object value = p.value(0);
        p.space();
        if (p.pos != text.length()) throw new IllegalArgumentException("JSON có dữ liệu thừa.");
        return value;
    }

    private void space() {
        while (pos < text.length() && " \t\r\n".indexOf(text.charAt(pos)) >= 0) pos++;
    }

    private char take() {
        if (pos >= text.length()) throw new IllegalArgumentException("JSON bị cắt ngắn.");
        return text.charAt(pos++);
    }

    private Object value(int depth) {
        if (depth > 20) throw new IllegalArgumentException("JSON lồng quá sâu.");
        space();
        if (pos >= text.length()) throw new IllegalArgumentException("JSON trống.");
        char c = text.charAt(pos);
        if (c == '"') return string();
        if (c == '{') {
            pos++;
            Map<String, Object> map = new LinkedHashMap<>();
            space();
            if (pos < text.length() && text.charAt(pos) == '}') {
                pos++;
                return map;
            }
            while (true) {
                space();
                if (pos >= text.length() || text.charAt(pos) != '"')
                    throw new IllegalArgumentException("Khóa JSON không hợp lệ.");
                String key = string();
                space();
                if (take() != ':') throw new IllegalArgumentException("Thiếu dấu hai chấm.");
                if (map.containsKey(key)) throw new IllegalArgumentException("JSON có khóa trùng.");
                map.put(key, value(depth + 1));
                space();
                char end = take();
                if (end == '}') return map;
                if (end != ',') throw new IllegalArgumentException("Đối tượng JSON không hợp lệ.");
            }
        }
        if (c == '[') {
            pos++;
            List<Object> list = new ArrayList<>();
            space();
            if (pos < text.length() && text.charAt(pos) == ']') {
                pos++;
                return list;
            }
            while (true) {
                if (list.size() >= arrayLimit)
                    throw new IllegalArgumentException("Mảng JSON quá dài.");
                list.add(value(depth + 1));
                space();
                char end = take();
                if (end == ']') return list;
                if (end != ',') throw new IllegalArgumentException("Mảng JSON không hợp lệ.");
            }
        }
        for (String literal : List.of("true", "false", "null"))
            if (text.startsWith(literal, pos)) {
                pos += literal.length();
                return literal.equals("null") ? null : Boolean.valueOf(literal);
            }
        int start = pos;
        while (pos < text.length() && "-+0123456789.eE".indexOf(text.charAt(pos)) >= 0) pos++;
        String n = text.substring(start, pos);
        if (!n.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?"))
            throw new IllegalArgumentException("Số JSON không hợp lệ.");
        try {
            double v = Double.parseDouble(n);
            if (!Double.isFinite(v)) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Số JSON ngoài phạm vi.");
        }
    }

    private String string() {
        if (take() != '"') throw new IllegalArgumentException();
        StringBuilder out = new StringBuilder();
        while (true) {
            char c = take();
            if (c == '"') return out.toString();
            if (c < 32) throw new IllegalArgumentException("Ký tự điều khiển trong JSON.");
            if (c == '\\') {
                char e = take();
                switch (e) {
                    case '"', '\\', '/' -> out.append(e);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        try {
                            if (pos + 4 > text.length()) throw new NumberFormatException();
                            out.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                            pos += 4;
                        } catch (NumberFormatException ex) {
                            throw new IllegalArgumentException("Unicode JSON không hợp lệ.");
                        }
                    }
                    default -> throw new IllegalArgumentException("Escape JSON không hợp lệ.");
                }
            } else out.append(c);
        }
    }

    public static String write(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) {
            StringBuilder out = new StringBuilder("\"");
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '"' -> out.append("\\\"");
                    case '\\' -> out.append("\\\\");
                    case '\n' -> out.append("\\n");
                    case '\r' -> out.append("\\r");
                    case '\t' -> out.append("\\t");
                    default -> {
                        if (c < 32) out.append(String.format("\\u%04x", (int) c));
                        else out.append(c);
                    }
                }
            }
            return out.append('"').toString();
        }
        if (value instanceof Boolean) return value.toString();
        if (value instanceof Number n) {
            if (!Double.isFinite(n.doubleValue()))
                throw new IllegalArgumentException("Số không hữu hạn.");
            return n.toString();
        }
        if (value instanceof Map<?, ?> m) {
            List<String> parts = new ArrayList<>();
            m.forEach((k, v) -> parts.add(write(k.toString()) + ":" + write(v)));
            return "{" + String.join(",", parts) + "}";
        }
        if (value instanceof Collection<?> list)
            return "[" + String.join(",", list.stream().map(Json::write).toList()) + "]";
        throw new IllegalArgumentException("Kiểu JSON không hỗ trợ.");
    }

    public static Map<?, ?> object(Object value) {
        if (!(value instanceof Map<?, ?> map))
            throw new IllegalArgumentException("AI cần trả về đối tượng JSON.");
        return map;
    }

    public static List<?> array(Object value) {
        if (!(value instanceof List<?> list))
            throw new IllegalArgumentException("AI thiếu danh sách kết quả.");
        return list;
    }

    public static String string(Object value, int max) {
        if (!(value instanceof String s) || s.isBlank() || s.length() > max)
            throw new IllegalArgumentException("AI thiếu nội dung hoặc nội dung quá dài.");
        return s.trim();
    }

    public static double number(Object value) {
        if (!(value instanceof Number n) || !Double.isFinite(n.doubleValue()))
            throw new IllegalArgumentException("Điểm AI không hợp lệ.");
        return n.doubleValue();
    }
}
