package com.vamsi.saripudi.piiscannerredactor.pipeline;

import com.vamsi.saripudi.piiscannerredactor.model.MatchType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class RegexMatcher {
    private final Map<MatchType, Pattern> patterns = new EnumMap<>(MatchType.class);


    public RegexMatcher() {
        // Conservative, practical regexes for a starter
        patterns.put(MatchType.EMAIL, Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE));
        patterns.put(MatchType.IPV4, Pattern.compile("\\b(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\b"));
        patterns.put(MatchType.SSN, Pattern.compile("\\b(?!000|666)(?:[0-8]\\d{2})-(?!00)\\d{2}-(?!0000)\\d{4}\\b"));
        patterns.put(MatchType.PHONE, Pattern.compile("\\b(?:\\+?1[-.\\s]?)?(?:\\(\\d{3}\\)|\\d{3})[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b"));
        patterns.put(MatchType.CREDIT_CARD, Pattern.compile("\\b(?:\\d[ -]*?){13,19}\\b"));
        patterns.put(MatchType.JWT, Pattern.compile("\\beyJ[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\b"));
        patterns.put(MatchType.IPV6, Pattern.compile("\\b(?:[A-Fa-f0-9]{1,4}:){7}[A-Fa-f0-9]{1,4}\\b"));
        patterns.put(MatchType.API_KEY, Pattern.compile("\\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\b"));
        patterns.put(MatchType.PASSWORD, Pattern.compile("\\b[A-Za-z0-9!@#$%^&*()_+\\-=\\[\\]{};:,.<>/?]{8,}\\b"));
        patterns.put(MatchType.PHYSICAL_ADDRESS, Pattern.compile("\\b\\d{1,4} [A-Za-z\\s]+\\b"));

    }

    public Map<MatchType, Pattern> patterns() {
        return patterns;
    }
}