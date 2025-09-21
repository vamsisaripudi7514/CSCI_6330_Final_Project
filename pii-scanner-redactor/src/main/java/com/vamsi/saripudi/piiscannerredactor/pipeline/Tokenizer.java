package com.vamsi.saripudi.piiscannerredactor.pipeline;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public final class Tokenizer {

    public static final class Token {
        public final String text;
        public final int start;       // inclusive
        public final int end;         // exclusive
        public final String keyHint;  // if this token is a value from key[:=]value

        public Token(String text, int start, int end, String keyHint) {
            this.text = text;
            this.start = start;
            this.end = end;
            this.keyHint = keyHint;
        }

        @Override
        public String toString() {
            return "Token(" + start + "," + end + ":'" + text + "',key=" + keyHint + ")";
        }
    }

    // quoted values: "abc", 'abc', `abc`
    private static final Pattern P_QUOTED = Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'|`(?:[^`\\\\]|\\\\.)*`");

    // key[:=]value — tolerant of spaces, used in logs/configs
    private static final Pattern P_KEYVAL = Pattern.compile("([A-Za-z0-9_\\-.]+)\\s*[:=]\\s*(.+)");

    private Tokenizer() {}

    public static List<Token> tokenize(String line) {
        boolean[] quoted = new boolean[line.length()];
        Matcher q = P_QUOTED.matcher(line);
        while (q.find()) mark(quoted, q.start(), q.end());

        List<Token> raw = new ArrayList<>();
        int i = 0, n = line.length();
        while (i < n) {
            if (!isQuoted(quoted, i) && Character.isWhitespace(line.charAt(i))) { i++; continue; }
            int start = i;
            while (i < n && (isQuoted(quoted, i) || !Character.isWhitespace(line.charAt(i)))) i++;
            raw.add(new Token(line.substring(start, i), start, i, null));
        }

        List<Token> out = new ArrayList<>(raw.size() + 4);
        for (Token t : raw) {
            Matcher m = P_KEYVAL.matcher(t.text);
            if (m.matches()) {
                int keyStart = t.start + m.start(1), keyEnd = t.start + m.end(1);
                int valStart = t.start + m.start(2), valEnd = t.start + m.end(2);
                String key = m.group(1), val = m.group(2);

                // trim trailing punctuation (, ; ) ])})
                while (valEnd > valStart && ",;)]}".indexOf(line.charAt(valEnd - 1)) >= 0) valEnd--;

                out.add(new Token(line.substring(keyStart, keyEnd), keyStart, keyEnd, null));
                out.add(new Token(line.substring(valStart, valEnd), valStart, valEnd, key));
            } else {
                // trim trailing punctuation but keep indices intact for redaction
                int s = t.start, e = t.end;
                while (e > s && ",;)]}".indexOf(line.charAt(e - 1)) >= 0) e--;
                out.add(new Token(line.substring(s, e), s, e, null));
            }
        }
        return out;
    }

    private static void mark(boolean[] arr, int s, int e) {
        for (int i = Math.max(0, s); i < Math.min(arr.length, e); i++) arr[i] = true;
    }

    private static boolean isQuoted(boolean[] arr, int idx) {
        return idx >= 0 && idx < arr.length && arr[idx];
    }
}
