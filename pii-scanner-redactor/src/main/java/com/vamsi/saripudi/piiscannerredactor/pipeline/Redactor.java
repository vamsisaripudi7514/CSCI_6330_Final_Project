package com.vamsi.saripudi.piiscannerredactor.pipeline;

import com.vamsi.saripudi.piiscannerredactor.config.PiiCryptoProperties;
import com.vamsi.saripudi.piiscannerredactor.encryption.CryptoService;
import com.vamsi.saripudi.piiscannerredactor.model.DetectionResult;
import com.vamsi.saripudi.piiscannerredactor.model.MatchType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class Redactor {
    private final CryptoService crypto;
    private final String tokenPrefix;

    @Autowired
    public Redactor(CryptoService crypto, PiiCryptoProperties props) {
        this.crypto = crypto;
        this.tokenPrefix = props.getToken().getPrefix();
    }
    //redact line
    public String redactLine(String line, List<DetectionResult> results, Path file, int lineNo, String mode) {
        String out = line;
        final String upperMode = (mode == null ? "MASK" : mode).toUpperCase(Locale.ROOT);

        for (DetectionResult r : results) {
            if (!Objects.equals(r.getFilePath(), file.toString()) || r.getLine() != lineNo) continue;

            final String replacement = switch (upperMode) {
                case "REMOVE" -> "";
                case "TAG"    -> "[REDACTED:" + r.getType().name() + "]";
                case "ENC"    -> encryptToken(r.getValue(), r.getType(), file);
                default       -> mask(r.getValue(), r.getType());
            };

            out = replaceOnce(out, r.getValue(), replacement);
        }
        return out;
    }

    //encrypts the token
    public String encryptToken(String original, MatchType type, Path file) {
        // Bind ciphertext to a context to prevent cut/paste misuse across files/types.
        final String aad = type.name() + "|" + file.getFileName();
        System.out.println(crypto.getKey());
        final String b64 = crypto.encrypt(original, aad);

        // Optional readability hints (do not affect decryption).
        final String suffix = switch (type) {
            case CREDIT_CARD -> "::" + last4Digits(original);
            case EMAIL       -> "::" + emailDomain(original);
            case PHONE       -> "::" + last4Digits(original);
            case SSN         -> "::" + last4Digits(original);
            default          -> "";
        };

        return tokenPrefix + "::" + type.name() + "::" + b64 + suffix;
    }
    //helper methods
    private String last4Digits(String v) {
        String digits = v.replaceAll("[^0-9]", "");
        if (digits.length() >= 4) return digits.substring(digits.length() - 4);
        return "";
    }

    private String emailDomain(String v) {
        int at = v.indexOf('@');
        return at >= 0 ? v.substring(at + 1) : "";
    }

    /** Replaces the first occurrence of a literal target with replacement. */
    private String replaceOnce(String input, String targetLiteral, String replacement) {
        if (targetLiteral == null || targetLiteral.isEmpty()) return input;
        Pattern p = Pattern.compile(Pattern.quote(targetLiteral));
        Matcher m = p.matcher(input);
        return m.find() ? new StringBuilder(input.length() - targetLiteral.length() + replacement.length())
                .append(input, 0, m.start())
                .append(replacement)
                .append(input, m.end(), input.length())
                .toString()
                : input;
    }

    //Traditional masking incase of no encryption
    public String mask(String value, MatchType type) {
        switch (type) {
            case CREDIT_CARD:     return maskCreditCard(value);
            case EMAIL:           return maskEmail(value);
            case PHONE:           return maskPhone(value);
            case SSN:             return maskSSN(value);
            case IPV4:      return maskIPV4(value);
            case IPV6:          return maskIPV6(value);
            case API_KEY:         return maskApiKey(value);
            case JWT:             return maskJwt(value);
            case PASSWORD:        return maskPassword(value);
            case PHYSICAL_ADDRESS: return maskPhysicalAddress(value);
            default:              return genericMask(value);
        }
    }

    private String genericMask(String v) {
        if (v == null || v.isEmpty()) return "***";
        if (v.length() <= 6) return "***";
        return v.substring(0, 2) + "****" + v.substring(v.length() - 2);
    }

    private String maskCreditCard(String v) {
        String digits = v.replaceAll("[^0-9]", "");
        if (digits.length() < 12) return genericMask(v);
        String last4 = digits.substring(digits.length() - 4);
        return "**** **** **** " + last4;
    }

    private String maskEmail(String v) {
        int at = v.indexOf('@');
        if (at <= 1) return genericMask(v);
        String user = v.substring(0, at);
        String dom  = v.substring(at);
        if (user.length() <= 2) return "***" + dom;
        return user.substring(0, 1) + "***" + user.substring(user.length() - 1) + dom;
    }

    private String maskPhone(String v) {
        String digits = v.replaceAll("[^0-9]", "");
        if (digits.length() < 7) return genericMask(v);
        String last4 = digits.substring(digits.length() - 4);
        return "***-***-" + last4;
    }

    private String maskSSN(String v) {
        String digits = v.replaceAll("[^0-9]", "");
        if (digits.length() == 9) return "***-**-" + digits.substring(5);
        return genericMask(v);
    }

    private String maskIPV4(String v) {
        if (v.contains(":")) { // IPv6
            return "****:****:****:****";
        }
        return "***.***.***.***"; // IPv4
    }
    private String maskIPV6(String v) {
        // IPv6 addresses have 8 groups separated by ':'
        String[] groups = v.split(":");
        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < groups.length; i++) {
            masked.append("****");
            if (i < groups.length - 1) masked.append(":");
        }
        return masked.toString();
    }

    private String maskApiKey(String v) {
        if (v == null || v.length() < 8) return genericMask(v);
        return v.substring(0, 4) + "****" + v.substring(v.length() - 4);
    }

    private String maskJwt(String v) {
        if (v == null || v.isEmpty()) return genericMask(v);
        String[] parts = v.split("\\.");
        if (parts.length != 3) return genericMask(v);
        return parts[0].substring(0, 2) + "***.***." + parts[2].substring(Math.max(0, parts[2].length() - 2));
    }

    private String maskPassword(String v) {
        if (v == null || v.isEmpty()) return "********";
        return "********";
    }

    private String maskPhysicalAddress(String v) {
        if (v == null || v.isEmpty()) return genericMask(v);
        // Mask house number, keep street name and city/state if possible
        return v.replaceAll("^\\d+", "***");
    }

}
