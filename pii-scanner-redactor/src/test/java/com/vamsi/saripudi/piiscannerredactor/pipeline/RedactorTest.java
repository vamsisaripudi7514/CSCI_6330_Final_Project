package com.vamsi.saripudi.piiscannerredactor.pipeline;

import com.vamsi.saripudi.piiscannerredactor.model.MatchType;
import com.vamsi.saripudi.piiscannerredactor.model.DetectionResult;
import com.vamsi.saripudi.piiscannerredactor.config.PiiCryptoProperties;
import com.vamsi.saripudi.piiscannerredactor.encryption.CryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    private CryptoService cryptoService;

    @Autowired
    private PiiCryptoProperties props;

    @Value("${pii.enc.keyB64}")
    private String keyB64;

//    @BeforeEach
//    public void setup() {
//        CryptoService cryptoService = mock(CryptoService.class);
//        PiiCryptoProperties props = mock(PiiCryptoProperties.class);
//        PiiCryptoProperties.Token token = mock(PiiCryptoProperties.Token.class);
//        when(props.getToken()).thenReturn(token);
//        when(token.getPrefix()).thenReturn("TOKEN");
//        redactor = new Redactor(cryptoService, props);
//    }

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

    @Test
    public void testEncryptTokenForEmail(){
        String original = "vamsi@gmail.com";
        MatchType matchType = MatchType.EMAIL;
        Path filePath = Path.of("test.txt");
        String result = redactor.encryptToken(original, matchType, filePath);
        assertTrue(result.contains("EMAIL::"));
        System.out.println(result);
    }

    @Test
    public void testEncryptTokenForPhoneNumber(){
        String original = "123-456-7890";
        MatchType matchType = MatchType.PHONE;
        Path filePath = Path.of("test.txt");
        String result = redactor.encryptToken(original, matchType, filePath);
        assertTrue(result.contains("PHONE::"));
        System.out.println(result);
        String input = result.split("::")[2];
        System.out.println(input);
        String decrypted = cryptoService.decrypt(input, "");
        System.out.println("Decrypted: " + decrypted);
    }

    @Test
    public void testEncryptTokenForIPV4(){
        String original = "127.0.0.1";
        MatchType matchType = MatchType.IPV4;
        Path filePath = Path.of("test.txt");
        String result = redactor.encryptToken(original, matchType, filePath);
        assertTrue(result.contains("IPV4::"));
        String input = result.split("::")[2];
        System.out.println(input);
        String decrypted = cryptoService.decrypt(input, "");
        System.out.println("Decrypted: " + decrypted);
        System.out.println(result);
    }

    @Test
    public void testByteArrayForEncryptionKey(){
        byte[] input = keyB64.getBytes();
        System.out.println(input);
    }
}
