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
        public final int start;   // inclusive
        public final int end;     // exclusive
        public final String keyHint;

        public Token(String text, int start, int end, String keyHint) {
            this.text = text; this.start = start; this.end = end; this.keyHint = keyHint;
        }
        @Override public String toString() { return "Token(" + start + "," + end + ":'" + text + "',key=" + keyHint + ")"; }
    }

    private static final Pattern P_QUOTED = Pattern.compile("\"[^\"]*\"|'[^']*'");
    private static final Pattern P_KEYVAL  = Pattern.compile("([A-Za-z0-9_\\-.]+)=(.+)"); // tolerant; value may include non-spaces if carved

    private Tokenizer() {}

    public static List<Token> tokenize(String line) {
        //marking the quoted ranges
        boolean[] quoted = new boolean[line.length()];
        Matcher q = P_QUOTED.matcher(line);
        while (q.find()) mark(quoted, q.start(), q.end());

        //splitting the line by whitespace outside quotes
        List<Token> raw = new ArrayList<>();
        int i = 0, n = line.length();
        while (i < n) {
            // skip spaces
            if (!isQuoted(quoted, i) && Character.isWhitespace(line.charAt(i))) { i++; continue; }
            int start = i;
            while (i < n && (isQuoted(quoted, i) || !Character.isWhitespace(line.charAt(i)))) i++;
            raw.add(new Token(line.substring(start, i), start, i, null));
        }

        //splitting the line by key=value
        List<Token> out = new ArrayList<>(raw.size() + 4);
        for (Token t : raw) {
            Matcher m = P_KEYVAL.matcher(t.text);
            if (m.matches()) {
                int keyStart = t.start + m.start(1), keyEnd = t.start + m.end(1);
                int valStart = t.start + m.start(2), valEnd = t.start + m.end(2);
                String key = m.group(1), val = m.group(2);


                out.add(new Token(key, keyStart, keyEnd, null));
                out.add(new Token(val, valStart, valEnd, key));
            } else {
                out.add(t);
            }
        }
        return out;
    }

    private static void mark(boolean[] arr, int s, int e) {
        for (int i = Math.max(0, s); i < Math.min(arr.length, e); i++) arr[i] = true;
    }
    private static boolean isQuoted(boolean[] arr, int idx) { return idx >= 0 && idx < arr.length && arr[idx]; }
}
