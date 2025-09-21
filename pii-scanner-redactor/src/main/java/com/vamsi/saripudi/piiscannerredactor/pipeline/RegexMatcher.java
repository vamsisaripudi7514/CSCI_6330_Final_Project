package com.vamsi.saripudi.piiscannerredactor.pipeline;

import com.vamsi.saripudi.piiscannerredactor.model.DetectionResult;
import com.vamsi.saripudi.piiscannerredactor.model.MatchType;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RegexMatcher {
    private final Map<MatchType, Pattern> patterns = new EnumMap<>(MatchType.class);


    public RegexMatcher() {
        // More precise and conservative patterns to reduce false positives
        patterns.put(MatchType.EMAIL, Pattern.compile("\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b", Pattern.CASE_INSENSITIVE));
        patterns.put(MatchType.IPV4, Pattern.compile("\\b(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\b"));
        patterns.put(MatchType.SSN, Pattern.compile("\\b(?!000|666)(?:[0-8]\\d{2})-(?!00)\\d{2}-(?!0000)\\d{4}\\b"));
        patterns.put(MatchType.PHONE, Pattern.compile("\\b(?:\\+?1[-.\\s]?)?(?:\\(\\d{3}\\)|\\d{3})[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b"));
        patterns.put(MatchType.CREDIT_CARD, Pattern.compile("\\b(?:\\d[ -]*?){13,19}\\b"));
        patterns.put(MatchType.JWT, Pattern.compile("\\beyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\b"));
        patterns.put(MatchType.IPV6, Pattern.compile("\\b(?:[A-Fa-f0-9]{1,4}:){7}[A-Fa-f0-9]{1,4}\\b|\\b::1\\b|\\b::ffff:[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\b"));
        patterns.put(MatchType.API_KEY, Pattern.compile("\\b(?:AIza[0-9A-Za-z_-]{35}|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}|sk-[0-9A-Za-z]{48}|xoxb-[0-9]{11}-[0-9]{11}-[0-9A-Za-z]{24})\\b"));

        // Much more restrictive password pattern - look for actual password-like strings
//        patterns.put(MatchType.PASSWORD, Pattern.compile("\\b(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};:,.<>/?])[A-Za-z\\d!@#$%^&*()_+\\-=\\[\\]{};:,.<>/?]{8,20}\\b"));

        // More specific physical address pattern
        patterns.put(MatchType.PHYSICAL_ADDRESS, Pattern.compile("\\b\\d{1,5}\\s+(?:[NSEW]\\s+)?[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*\\s+(?:St|Street|Ave|Avenue|Rd|Road|Blvd|Boulevard|Dr|Drive|Ln|Lane|Ct|Court|Pl|Place|Way|Pkwy|Parkway)\\b", Pattern.CASE_INSENSITIVE));
    }

    public Map<MatchType, Pattern> patterns() {
        return patterns;
    }

    public List<DetectionResult> matchesToken(String token, Path file, int lineNo, int offset) {
        if(token == null || token.isEmpty())
            return List.of();
        List<DetectionResult> results = new ArrayList<>();

        for(Map.Entry<MatchType, Pattern> entry : patterns.entrySet()) {
            MatchType type = entry.getKey();
            Pattern pattern = entry.getValue();
            Matcher matcher = pattern.matcher(token);

            while(matcher.find()) {
                // Extract the actual matched substring
                String matchedValue = matcher.group();

                results.add(
                        DetectionResult.builder()
                                .filePath(file)
                                .line(lineNo)
                                .startCol(offset + matcher.start())  // Add offset to get correct column position
                                .endCol(offset + matcher.end())      // Add offset to get correct column position
                                .type(type)
                                .value(matchedValue)  // Use matched substring, not full token
                                .score(2.0)  // Use fixed score instead of EntropyScorer method
                                .build()
                );
            }
        }
        return results;
    }
}