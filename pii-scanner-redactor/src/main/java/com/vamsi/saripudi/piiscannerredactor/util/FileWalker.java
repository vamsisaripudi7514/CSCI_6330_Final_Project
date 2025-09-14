package com.vamsi.saripudi.piiscannerredactor.util;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

@Data
@Component
public class FileWalker {

    public static List<Path> listFiles(List<Path> inputs) throws IOException {
        List<Path> result = new ArrayList<>();
        for (Path input : inputs) {
            if (Files.isDirectory(input)) {
                try (var stream = Files.walk(input)) {
                    stream.filter(Files::isRegularFile)
                            .filter(p -> !p.getFileName().toString().startsWith("."))
                            .forEach(result::add);
                }
            } else if (Files.isRegularFile(input)) {
                result.add(input);
            }
        }
        return result;
    }
}
