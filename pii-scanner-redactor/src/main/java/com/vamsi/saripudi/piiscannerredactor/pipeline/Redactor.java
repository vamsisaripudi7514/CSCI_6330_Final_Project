package com.vamsi.saripudi.piiscannerredactor.pipeline;

import com.vamsi.saripudi.piiscannerredactor.config.PiiCryptoProperties;
import com.vamsi.saripudi.piiscannerredactor.encryption.CryptoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
}
