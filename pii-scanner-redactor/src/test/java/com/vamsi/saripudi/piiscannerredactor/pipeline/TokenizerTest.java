package com.vamsi.saripudi.piiscannerredactor.pipeline;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TokenizerTest {

    @Autowired
    public Tokenizer tokenizer;

    @Test
    public void testTokenize1() {
        List<Tokenizer.Token> tokens = tokenizer.tokenize("37.57.6.3 - - [07/Sep/2025:18:24:48 +0000] \"PUT /assets/app.js HTTP/1.1\" 201 162871 \"-\"");
        assertTrue(tokens.get(0).text.startsWith("37.57.6.3"));
        tokens.forEach(token -> System.out.println(token.text));
    }

    @Test
    public void testTokenize2() {
        List<Tokenizer.Token> tokens = tokenizer.tokenize("FEATURE_X_ENABLED=1368\n" +
                "DB_HOST=desktop-47.ballard.biz Authorization='Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJWWGNRS1NWdTlFIiwiaWF0IjoxNzU3MjY3ODUyLCJleHAiOjE3NTcyNzE0NTIsInNjb3BlIjoicmVhZDphbGwifQ.7FUNI5REXzDELJXOTqPs9vtLRIc2-0OpOfcJ69r5cW8'");
        assertTrue(tokens.get(0).text.startsWith("FEATURE"));
        assertTrue(tokens.getLast().text.endsWith("W8\'"));
        tokens.forEach(token -> System.out.println(token.text));
    }
}

