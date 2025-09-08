package com.vamsi.saripudi.piiscannerredactor.pipeline;

import com.vamsi.saripudi.piiscannerredactor.model.MatchType;
import com.vamsi.saripudi.piiscannerredactor.model.DetectionResult;
import com.vamsi.saripudi.piiscannerredactor.config.PiiCryptoProperties;
import com.vamsi.saripudi.piiscannerredactor.encryption.CryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class RedactorTest {

    @Autowired
    private Redactor redactor;

    @BeforeEach
    public void setup() {
        CryptoService cryptoService = mock(CryptoService.class);
        PiiCryptoProperties props = mock(PiiCryptoProperties.class);
        PiiCryptoProperties.Token token = mock(PiiCryptoProperties.Token.class);
        when(props.getToken()).thenReturn(token);
        when(token.getPrefix()).thenReturn("TOKEN");
        redactor = new Redactor(cryptoService, props);
    }

    @Test
    public void testMaskApiKey() {
        String apiKey = "ABCD1234EFGH5678";
        String masked = redactor.mask(apiKey, MatchType.API_KEY);
        assertEquals("ABCD****5678", masked);
    }

    @Test
    public void testMaskJwt() {
        String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        String masked = redactor.mask(jwt, MatchType.JWT);
        assertTrue(masked.startsWith("ey***.***.5c"));
    }

    @Test
    public void testMaskPassword() {
        String password = "SuperSecret123!";
        String masked = redactor.mask(password, MatchType.PASSWORD);
        assertEquals("********", masked);
    }

    @Test
    public void testMaskPhysicalAddress() {
        String address = "1234 Main St, Springfield, IL";
        String masked = redactor.mask(address, MatchType.PHYSICAL_ADDRESS);
        assertTrue(masked.startsWith("*** Main St"));
    }
}
