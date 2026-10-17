package net.susu609.royalarena.stats;

import java.util.List;
import java.util.Map;

/**
 * Just enough JSON writing for maps, lists, strings, numbers and booleans
 * (the match log and the stats report). Doubles that are whole numbers are
 * written without a fraction; NaN and infinity become null.
 */
public final class Json {
    /** Already written JSON, put in as it is. */
    public record Raw(String json) {
    }

    private Json() {
    }

    public static String write(Object value) {
        StringBuilder out = new StringBuilder();
        append(out, value);
        return out.toString();
    }

    private static void append(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof Raw raw) {
            out.append(raw.json());
        } else if (value instanceof String s) {
            out.append(quote(s));
        } else if (value instanceof Double d) {
            out.append(d.isNaN() || d.isInfinite() ? "null" :
                    d == Math.rint(d) && Math.abs(d) < 1.0E15D ?
                            Long.toString(d.longValue()) : d.toString());
        } else if (value instanceof Number || value instanceof Boolean) {
            out.append(value);
        } else if (value instanceof Map<?, ?> map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first)
                    out.append(',');
                first = false;
                out.append(quote(e.getKey().toString())).append(':');
                append(out, e.getValue());
            }
            out.append('}');
        } else if (value instanceof List<?> list) {
            out.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0)
                    out.append(',');
                append(out, list.get(i));
            }
            out.append(']');
        } else {
            out.append(quote(value.toString()));
        }
    }

    public static String quote(String s) {
        StringBuilder out = new StringBuilder("\"");
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (ch < 0x20 || ch == '<')
                        out.append(String.format("\\u%04x", (int) ch));
                    else
                        out.append(ch);
                }
            }
        }
        return out.append('"').toString();
    }
}
