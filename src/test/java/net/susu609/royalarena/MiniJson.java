package net.susu609.royalarena;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A strict little JSON reader for tests: objects become maps, arrays lists,
 * numbers doubles. Anything that is not valid JSON throws.
 */
final class MiniJson {
    private final String s;
    private int i;

    private MiniJson(String s) {
        this.s = s;
    }

    static Object parse(String json) {
        MiniJson p = new MiniJson(json);
        Object value = p.value();
        p.space();
        if (p.i != json.length())
            throw p.error("trailing text");
        return value;
    }

    private Object value() {
        space();
        if (i >= s.length())
            throw error("unexpected end");
        char c = s.charAt(i);
        switch (c) {
            case '{': return object();
            case '[': return array();
            case '"': return string();
            case 't': return word("true", Boolean.TRUE);
            case 'f': return word("false", Boolean.FALSE);
            case 'n': return word("null", null);
            default: return number();
        }
    }

    private Map<String, Object> object() {
        Map<String, Object> map = new LinkedHashMap<>();
        i++;
        space();
        if (peek() == '}') {
            i++;
            return map;
        }
        while (true) {
            space();
            String key = string();
            space();
            expect(':');
            if (map.put(key, value()) != null)
                throw error("duplicate key " + key);
            space();
            if (peek() == ',') {
                i++;
                continue;
            }
            expect('}');
            return map;
        }
    }

    private List<Object> array() {
        List<Object> list = new ArrayList<>();
        i++;
        space();
        if (peek() == ']') {
            i++;
            return list;
        }
        while (true) {
            list.add(value());
            space();
            if (peek() == ',') {
                i++;
                continue;
            }
            expect(']');
            return list;
        }
    }

    private String string() {
        expect('"');
        StringBuilder out = new StringBuilder();
        while (true) {
            if (i >= s.length())
                throw error("unclosed string");
            char c = s.charAt(i++);
            if (c == '"')
                return out.toString();
            if (c < 0x20)
                throw error("control character in string");
            if (c != '\\') {
                out.append(c);
                continue;
            }
            char e = s.charAt(i++);
            switch (e) {
                case '"', '\\', '/' -> out.append(e);
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'u' -> {
                    out.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                    i += 4;
                }
                default -> throw error("bad escape");
            }
        }
    }

    private Double number() {
        int start = i;
        if (peek() == '-')
            i++;
        while (i < s.length() && "0123456789.eE+-".indexOf(s.charAt(i)) >= 0)
            i++;
        String text = s.substring(start, i);
        if (text.isEmpty() || !text.matches("-?(0|[1-9]\\d*)(\\.\\d+)?([eE][+-]?\\d+)?"))
            throw error("bad number '" + text + "'");
        return Double.parseDouble(text);
    }

    private Object word(String word, Object value) {
        if (!s.startsWith(word, i))
            throw error("expected " + word);
        i += word.length();
        return value;
    }

    private void expect(char c) {
        if (peek() != c)
            throw error("expected '" + c + "'");
        i++;
    }

    private char peek() {
        return i < s.length() ? s.charAt(i) : '\0';
    }

    private void space() {
        while (i < s.length() && " \t\r\n".indexOf(s.charAt(i)) >= 0)
            i++;
    }

    private IllegalArgumentException error(String what) {
        return new IllegalArgumentException(what + " at " + i);
    }
}
