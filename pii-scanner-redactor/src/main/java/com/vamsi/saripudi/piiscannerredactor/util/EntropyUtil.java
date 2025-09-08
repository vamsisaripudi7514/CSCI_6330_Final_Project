package com.vamsi.saripudi.piiscannerredactor.util;

import org.springframework.stereotype.Component;

//@Component
public class EntropyUtil {

    public static double shannonEntropy(String s) {
        if (s == null || s.isEmpty()) return 0.0;
        int[] freq = new int[128];
        int n = 0;
        for (char c : s.toCharArray()) {
            if (c < 128) { freq[c]++; n++; }
        }
        if (n == 0) return 0.0;
        double h = 0.0;
        for (int f : freq) {
            if (f == 0) continue;
            double p = (double) f / n;
            h -= p * (Math.log(p) / Math.log(2));
        }
        return h;
    }
}
