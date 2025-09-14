package com.vamsi.saripudi.piiscannerredactor.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FileWalkerTest {

    @Test
    public void testListFiles() {
        try{
            List<Path> paths = FileWalker.listFiles(List.of(Path.of(    "/Users/vamsisaripudi/Documents/1MTSU/SEM3/Parallel Processing Concepts/Final Project/Codebase/CSCI_6330_Final_Project/pii-scanner-redactor/src/test/java/com/vamsi/saripudi/piiscannerredactor/util/")));
            assertTrue(paths.size() > 0);
            assertTrue(paths.stream().allMatch(p -> p.toString().endsWith(".java")));
            System.out.println(paths);
        }
        catch(IOException e){
            fail(e.getMessage());
        }
    }

    @Test
    public void testListFiles1() {
        try{
            List<Path> paths = FileWalker.listFiles(List.of(Path.of(    "/Users/vamsisaripudi/Documents/1MTSU/SEM3/Parallel Processing Concepts/Final Project/Codebase/CSCI_6330_Final_Project/pii-scanner-redactor/src/main/java/com/vamsi/saripudi/piiscannerredactor/")));
            assertTrue(paths.size() > 0);
            assertTrue(paths.stream().allMatch(p -> p.toString().endsWith(".java")));
            paths.stream().forEach(System.out::println);
        }
        catch(IOException e){
            fail(e.getMessage());
        }
    }
}
