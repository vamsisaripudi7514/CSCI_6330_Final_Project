package com.vamsi.saripudi.piiscannerredactor.config;

import com.vamsi.saripudi.piiscannerredactor.encryption.CryptoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(classes = CryptoConfig.class)
public class CryptoConfigTest {


    @Autowired
    private CryptoService cryptoService;

    @Value("${pii.enc.keyB64}")
    private String keyB64;

    @Test
    public void testCryptoService() {
        assertNotNull(cryptoService);
        System.out.println(cryptoService.getKey());
        System.out.println(keyB64);
    }
}
