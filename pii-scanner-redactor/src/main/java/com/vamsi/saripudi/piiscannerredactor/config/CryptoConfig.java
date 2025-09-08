package com.vamsi.saripudi.piiscannerredactor.config;

import com.vamsi.saripudi.piiscannerredactor.encryption.CryptoService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;

@Configuration
@EnableConfigurationProperties(PiiCryptoProperties.class)
public class CryptoConfig {

    @Bean
    public CryptoService cryptoService(PiiCryptoProperties props) {
        String b64 = props.getEnc().getKeyB64();
        if (b64 == null || b64.isBlank()) {
            throw new IllegalStateException("Missing AES key: set pii.enc.keyB64 or env PII_ENC_KEY_B64");
        }
        byte[] key = Base64.getDecoder().decode(b64);
        // CryptoService enforces 16/24/32-byte keys internally
        return new CryptoService(key);
    }
}
