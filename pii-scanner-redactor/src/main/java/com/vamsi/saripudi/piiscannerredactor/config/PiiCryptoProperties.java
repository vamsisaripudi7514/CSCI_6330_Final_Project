package com.vamsi.saripudi.piiscannerredactor.config;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pii")
public class PiiCryptoProperties {
    public static class Enc {
        private String keyB64;
        public String getKeyB64() { return keyB64; }
        public void setKeyB64(String keyB64) { this.keyB64 = keyB64; }
    }
    public static class Token {
        private String prefix = "PIIENC";
        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }
    }

    private Enc enc = new Enc();
    private Token token = new Token();

    public Enc getEnc() { return enc; }
    public Token getToken() { return token; }
}
