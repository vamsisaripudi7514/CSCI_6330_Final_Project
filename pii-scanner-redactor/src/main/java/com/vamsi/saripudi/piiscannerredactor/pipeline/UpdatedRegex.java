package com.vamsi.saripudi.piiscannerredactor.pipeline;
import com.vamsi.saripudi.piiscannerredactor.model.DetectionResult;
import com.vamsi.saripudi.piiscannerredactor.model.MatchType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class UpdatedRegex {
    private final Map<MatchType, Pattern> patterns = new EnumMap<>(MatchType.class);

    // Tunable base scores by type (kept small; final scorer can add context/entropy)
    private static final Map<MatchType, Double> BASE_SCORES = Map.ofEntries(
            Map.entry(MatchType.CREDIT_CARD, 5.0),
            Map.entry(MatchType.JWT,         5.0),
            Map.entry(MatchType.API_KEY,     4.0),
            Map.entry(MatchType.EMAIL,       3.0),
            Map.entry(MatchType.IPV4,        3.0),
            Map.entry(MatchType.IPV6,        3.0),
            Map.entry(MatchType.PHONE,       3.0),
            Map.entry(MatchType.SSN,         4.0),
            Map.entry(MatchType.PHYSICAL_ADDRESS, 2.5)
    );

    public UpdatedRegex() {
        // EMAIL: require TLD 2-24 chars, avoid trailing dot, allow +, subdomains
        patterns.put(MatchType.EMAIL, Pattern.compile(
                "(?<![A-Za-z0-9._%+-])" +                              // no word char just before
                        "[A-Za-z0-9._%+-]+@" +
                        "(?:[A-Za-z0-9-]+\\.)+[A-Za-z]{2,24}" +
                        "(?![A-Za-z0-9._%+-])"                                 // no word char just after
        ));

        // IPV4: strict 0-255 octets, already tight; we’ll add post-filters (e.g., 0.0.0.0)
        patterns.put(MatchType.IPV4, Pattern.compile(
                "\\b(?:(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}" +
                        "(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\b"
        ));

        // IPV6 (basic full/::1/IPv4-mapped). Keeping conservative to limit FPs.
        patterns.put(MatchType.IPV6, Pattern.compile(
                "\\b(?:(?:[A-Fa-f0-9]{1,4}:){7}[A-Fa-f0-9]{1,4}|::1|::ffff:(?:\\d{1,3}\\.){3}\\d{1,3})\\b"
        ));

        // SSN: classic ddd-dd-dddd with gates
        patterns.put(MatchType.SSN, Pattern.compile(
                "\\b(?!000|666)(?:[0-8]\\d{2})-(?!00)\\d{2}-(?!0000)\\d{4}\\b"
        ));

        // PHONE (US-like): +1 optional, separators or space, with boundaries
        patterns.put(MatchType.PHONE, Pattern.compile(
                "(?<!\\d)(?:\\+?1[-.\\s]?)?(?:\\(\\d{3}\\)|\\d{3})[-.\\s]?\\d{3}[-.\\s]?\\d{4}(?!\\d)"
        ));

        // CREDIT CARD: 13–19 digits with separators; post-validate with Luhn & digit count
        patterns.put(MatchType.CREDIT_CARD, Pattern.compile(
                "(?<!\\d)(?:\\d[ -]?){13,19}(?!\\d)"
        ));

        // JWT: three base64url segments with sensible length; post-validate by decoding
        patterns.put(MatchType.JWT, Pattern.compile(
                "\\b[A-Za-z0-9_-]{10,}\\.([A-Za-z0-9_-]{10,})\\.[A-Za-z0-9_-]{10,}\\b"
        ));

        // API keys (examples): Google API, UUID v4, OpenAI sk-*, Slack xox[b|p|o]...
        patterns.put(MatchType.API_KEY, Pattern.compile(
                "\\b(?:" +
                        "AIza[0-9A-Za-z_-]{35}" +                             // Google API key
                        "|" +
                        "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}" + // UUID v4
                        "|" +
                        "sk-[A-Za-z0-9]{32,}" +                               // sk- (OpenAI etc., length >= 32)
                        "|" +
                        "xox(?:p|b|o)-[A-Za-z0-9-]{10,}" +                    // Slack tokens
                        ")\\b", Pattern.CASE_INSENSITIVE
        ));

        // Physical US-like address (still heuristic; keep case-insensitive street types)
        patterns.put(MatchType.PHYSICAL_ADDRESS, Pattern.compile(
                "\\b\\d{1,5}\\s+(?:[NSEW]\\s+)?[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*\\s+" +
                        "(?:St|Street|Ave|Avenue|Rd|Road|Blvd|Boulevard|Dr|Drive|Ln|Lane|Ct|Court|Pl|Place|Way|Pkwy|Parkway)\\b",
                Pattern.CASE_INSENSITIVE
        ));
    }

    public Map<MatchType, Pattern> patterns() {
        return patterns;
    }

    public List<DetectionResult> matchesToken(String token, Path file, int lineNo, int offset) {
        if (token == null || token.isEmpty()) return List.of();

        // Collect raw matches first
        List<DetectionResult> raw = new ArrayList<>();
        for (Map.Entry<MatchType, Pattern> entry : patterns.entrySet()) {
            MatchType type = entry.getKey();
            Pattern pattern = entry.getValue();
            Matcher matcher = pattern.matcher(token);

            while (matcher.find()) {
                int s = matcher.start();
                int e = matcher.end();
                String matched = token.substring(s, e);

                // Type-specific validation / suppression
                if (!passesPostValidation(type, matched)) continue;

                double score = BASE_SCORES.getOrDefault(type, 2.0);
                raw.add(DetectionResult.builder()
                        .filePath(file)
                        .line(lineNo)
                        .startCol(offset + s)
                        .endCol(offset + e)
                        .type(type)
                        .value(matched)
                        .score(score)
                        .build());
            }
        }

        if (raw.isEmpty()) return raw;

        // De-duplicate overlaps: keep the longest span; if tie, prefer higher BASE_SCORES
        raw.sort(Comparator
                .comparingInt((DetectionResult r) -> r.getStartCol())
                .thenComparingInt(r -> -r.getEndCol())); // longer first when same start

        List<DetectionResult> dedup = new ArrayList<>();
        for (DetectionResult r : raw) {
            boolean overlaps = false;
            for (DetectionResult kept : dedup) {
                if (spansOverlap(kept.getStartCol(), kept.getEndCol(), r.getStartCol(), r.getEndCol())) {
                    // keep the one with longer span or higher base score
                    double keptScore = BASE_SCORES.getOrDefault(kept.getType(), 2.0);
                    double rScore    = BASE_SCORES.getOrDefault(r.getType(), 2.0);
                    int keptLen = kept.getEndCol() - kept.getStartCol();
                    int rLen    = r.getEndCol() - r.getStartCol();

                    boolean replace = (rLen > keptLen) || (rLen == keptLen && rScore > keptScore);
                    if (replace) {
                        // replace in place
                        int idx = dedup.indexOf(kept);
                        dedup.set(idx, r);
                    }
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) dedup.add(r);
        }
        return dedup;
    }

    // ---------------- helpers ----------------

    private static boolean spansOverlap(int aStart, int aEnd, int bStart, int bEnd) {
        return aStart < bEnd && bStart < aEnd;
    }

    private static boolean passesPostValidation(MatchType type, String value) {
        switch (type) {
            case CREDIT_CARD:
                String digits = value.replaceAll("[^0-9]", "");
                if (digits.length() < 13 || digits.length() > 19) return false;
                return luhn(digits);

            case JWT:
                return looksLikeValidJwt(value);

            case IPV4:
                return isValidUsableIPv4(value);

            default:
                return true; // others rely on regex tightness
        }
    }

    // Luhn mod-10
    private static boolean luhn(String digits) {
        int sum = 0;
        boolean alt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alt) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alt = !alt;
        }
        return sum % 10 == 0;
    }

    // JWT quick sanity: 3 segments, header/payload base64url-decodable, header JSON contains {"alg":..., "typ":...} (heuristic)
    private static boolean looksLikeValidJwt(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) return false;
        try {
            String header = new String(base64UrlDecode(parts[0]), StandardCharsets.UTF_8).trim();
            String payload = new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8).trim();
            // extremely light JSON sanity without a JSON parser
            if (!(header.startsWith("{") && header.endsWith("}"))) return false;
            if (!(payload.startsWith("{") && payload.endsWith("}"))) return false;
            // presence checks
            String hLower = header.toLowerCase(Locale.ROOT);
            return (hLower.contains("\"alg\"") && hLower.contains("\"typ\""));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static byte[] base64UrlDecode(String s) {
        String pad = s.length() % 4 == 0 ? "" : "====".substring(s.length() % 4);
        return Base64.getUrlDecoder().decode(s + pad);
    }

    private static boolean isValidUsableIPv4(String s) {
        // Reject 0.0.0.0 and 255.255.255.255 and addresses with leading-zero octets like 001.002.003.004
        if ("0.0.0.0".equals(s) || "255.255.255.255".equals(s)) return false;
        String[] parts = s.split("\\.");
        if (parts.length != 4) return false;
        for (String p : parts) {
            if (p.length() > 1 && p.startsWith("0")) return false; // leading zero suppression
            int v = Integer.parseInt(p);
            if (v < 0 || v > 255) return false;
        }
        return true;
    }
}

