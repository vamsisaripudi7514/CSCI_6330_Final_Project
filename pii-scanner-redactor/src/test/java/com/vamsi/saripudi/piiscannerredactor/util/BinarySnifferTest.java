package com.vamsi.saripudi.piiscannerredactor.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class BinarySnifferTest {

    @Autowired
    public BinarySniffer binarySniffer;

    @Test
    public void testIsBinary() {
        Path path = Paths.get("/Users/vamsisaripudi/Desktop/batch-2023-24.jpg");
        try {
            assertTrue(binarySniffer.isBinary(path));
        }
        catch(IOException e){
            fail(e.getMessage());
        }
    }

    @Test
    public void testIsNotBinary() {
        Path path = Paths.get("/Users/vamsisaripudi/Documents/1MTSU/GTA/GTA Summer 2025/CSCI 2170/Lab 1/evaluate_cpp.sh");
        try {
            assertFalse(binarySniffer.isBinary(path));
        }
        catch(IOException e){
            fail(e.getMessage());
        }
    }
}
