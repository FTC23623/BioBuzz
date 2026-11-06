package org.firstinspires.ftc.teamcode.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads JSON text into plain Java: an object becomes a {@code Map<String, Object>}, an array
 * a {@code List<Object>}, a number a {@code Double}, plus {@code String}, {@code Boolean} and
 * {@code null}. Just enough for the visualizer's {@code .pp} files on a laptop, where the
 * Android JSON classes don't run (docs/decisions/0051).
 */
public final class MiniJson {
    private final String mText;
    private int mAt = 0;

    private MiniJson(String text) {
        mText = text;
    }

    /** The whole text as one value. Throws IllegalArgumentException on anything that isn't JSON. */
    public static Object Parse(String text) {
        MiniJson reader = new MiniJson(text);
        Object value = reader.Value();
        reader.SkipSpace();
        if (reader.mAt != text.length()) {
            throw reader.Error("extra text after the end");
        }
        return value;
    }

    private Object Value() {
        SkipSpace();
        if (mAt >= mText.length()) {
            throw Error("the text ended early");
        }
        char c = mText.charAt(mAt);
        if (c == '{') {
            return Object();
        }
        if (c == '[') {
            return Array();
        }
        if (c == '"') {
            return Text();
        }
        if (mText.startsWith("true", mAt)) {
            mAt = mAt + 4;
            return Boolean.TRUE;
        }
        if (mText.startsWith("false", mAt)) {
            mAt = mAt + 5;
            return Boolean.FALSE;
        }
        if (mText.startsWith("null", mAt)) {
            mAt = mAt + 4;
            return null;
        }
        return Number();
    }

    private Map<String, Object> Object() {
        Map<String, Object> object = new LinkedHashMap<>();
        mAt = mAt + 1;
        SkipSpace();
        if (Peek() == '}') {
            mAt = mAt + 1;
            return object;
        }
        while (true) {
            SkipSpace();
            String key = Text();
            SkipSpace();
            Expect(':');
            object.put(key, Value());
            SkipSpace();
            if (Peek() == ',') {
                mAt = mAt + 1;
            } else {
                Expect('}');
                return object;
            }
        }
    }

    private List<Object> Array() {
        List<Object> array = new ArrayList<>();
        mAt = mAt + 1;
        SkipSpace();
        if (Peek() == ']') {
            mAt = mAt + 1;
            return array;
        }
        while (true) {
            array.add(Value());
            SkipSpace();
            if (Peek() == ',') {
                mAt = mAt + 1;
            } else {
                Expect(']');
                return array;
            }
        }
    }

    private String Text() {
        Expect('"');
        StringBuilder text = new StringBuilder();
        while (true) {
            if (mAt >= mText.length()) {
                throw Error("a text never closed");
            }
            char c = mText.charAt(mAt);
            mAt = mAt + 1;
            if (c == '"') {
                return text.toString();
            }
            if (c != '\\') {
                text.append(c);
                continue;
            }
            char escaped = mText.charAt(mAt);
            mAt = mAt + 1;
            if (escaped == 'n') {
                text.append('\n');
            } else if (escaped == 't') {
                text.append('\t');
            } else if (escaped == 'r') {
                text.append('\r');
            } else if (escaped == 'b') {
                text.append('\b');
            } else if (escaped == 'f') {
                text.append('\f');
            } else if (escaped == 'u') {
                text.append((char) Integer.parseInt(mText.substring(mAt, mAt + 4), 16));
                mAt = mAt + 4;
            } else {
                text.append(escaped);
            }
        }
    }

    private Double Number() {
        int start = mAt;
        while (mAt < mText.length() && "+-0123456789.eE".indexOf(mText.charAt(mAt)) >= 0) {
            mAt = mAt + 1;
        }
        if (start == mAt) {
            throw Error("expected a value");
        }
        return Double.valueOf(mText.substring(start, mAt));
    }

    private void SkipSpace() {
        while (mAt < mText.length() && Character.isWhitespace(mText.charAt(mAt))) {
            mAt = mAt + 1;
        }
    }

    private char Peek() {
        if (mAt >= mText.length()) {
            throw Error("the text ended early");
        }
        return mText.charAt(mAt);
    }

    private void Expect(char wanted) {
        if (Peek() != wanted) {
            throw Error("expected '" + wanted + "'");
        }
        mAt = mAt + 1;
    }

    private IllegalArgumentException Error(String what) {
        return new IllegalArgumentException("Not valid JSON at character " + mAt + ": " + what);
    }
}
