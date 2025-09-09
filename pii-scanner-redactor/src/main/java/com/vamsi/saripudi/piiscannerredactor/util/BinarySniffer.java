package com.vamsi.saripudi.piiscannerredactor.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Heuristic binary detector to avoid scanning non-text files.
 *
 * Rules (in order):
 *  1) Magic bytes: common binary formats (PDF/PNG/JPEG/GIF/ZIP/JAR/GZ/BZ2/EXE) → binary.
 *  2) Null byte present in the header window → binary.
 *  3) Control-char ratio (excluding \t, \r, \n) ≥ threshold → binary.
 *
 * Defaults: read up to 4 KiB, control threshold = 0.15 (15%).
 * Tunable via constructor.
 */
@Component
public class BinarySniffer {

    private static final int DEFAULT_HEADER_BYTES = 4096;
    private static final double DEFAULT_CONTROL_RATIO_THRESHOLD = 0.15;

    // Allowed control characters to not count as "bad"
    private static final Set<Byte> ALLOWED_CONTROLS = Set.of(
            (byte) '\t', (byte) '\r', (byte) '\n'
    );

    private final int headerBytes;
    private final double controlRatioThreshold;

    public BinarySniffer() {
        this(DEFAULT_HEADER_BYTES, DEFAULT_CONTROL_RATIO_THRESHOLD);
    }

    public BinarySniffer(int headerBytes, double controlRatioThreshold) {
        if (headerBytes <= 0) throw new IllegalArgumentException("headerBytes must be > 0");
        if (controlRatioThreshold < 0 || controlRatioThreshold > 1)
            throw new IllegalArgumentException("controlRatioThreshold must be in [0,1]");
        this.headerBytes = headerBytes;
        this.controlRatioThreshold = controlRatioThreshold;
    }

    public boolean isBinary(Path path) throws IOException {
        if (!Files.isRegularFile(path)) return true; // be safe
        try (InputStream in = Files.newInputStream(path)) {
            byte[] buf = readHeader(in, headerBytes);
            if (buf.length == 0) return false; // empty files treated as text

            if (matchesKnownBinaryMagic(buf)) return true;

            for (byte b : buf) {
                if (b == 0x00) return true;
            }

            int controlCount = 0;
            for (byte b : buf) {
                int ub = b & 0xFF;
                if (ub < 0x20 || ub == 0x7F) {
                    // skip common text controls
                    if (!ALLOWED_CONTROLS.contains(b)) {
                        controlCount++;
                    }
                }
            }
            double ratio = (double) controlCount / buf.length;
            return ratio >= controlRatioThreshold;
        }
    }

    public boolean isBinary(byte[] header) {
        if (header == null || header.length == 0) return false;
        if (matchesKnownBinaryMagic(header)) return true;
        for (byte b : header) {
            if (b == 0x00) return true;
        }
        int controlCount = 0;
        for (byte b : header) {
            int ub = b & 0xFF;
            if (ub < 0x20 || ub == 0x7F) {
                if (!ALLOWED_CONTROLS.contains(b)) controlCount++;
            }
        }
        double ratio = (double) controlCount / header.length;
        return ratio >= controlRatioThreshold;
    }

    private static byte[] readHeader(InputStream in, int max) throws IOException {
        byte[] buf = new byte[max];
        int off = 0;
        while (off < max) {
            int r = in.read(buf, off, max - off);
            if (r == -1) break;
            off += r;
        }
        if (off == buf.length) return buf;
        byte[] exact = new byte[off];
        System.arraycopy(buf, 0, exact, 0, off);
        return exact;
    }

    private static boolean matchesKnownBinaryMagic(byte[] b) {
        return  startsWith(b, "%PDF-")                  || // PDF
                startsWith(b, new byte[]{(byte)0x89, 'P','N','G', 0x0D, 0x0A, 0x1A, 0x0A}) || // PNG
                startsWith(b, new byte[]{(byte)0xFF, (byte)0xD8}) || // JPEG
                startsWith(b, "GIF87a") || startsWith(b, "GIF89a") || // GIF
                startsWith(b, "PK\u0003\u0004") || // ZIP/JAR/DOCX/XLSX
                startsWith(b, new byte[]{0x1F, (byte)0x8B}) || // GZIP
                startsWith(b, "BZh") || // BZIP2
                startsWith(b, new byte[]{'7','z',(byte)0xBC,(byte)0xAF,0x27,0x1C}) || // 7z
                startsWith(b, "Rar!") || // RAR
                startsWith(b, "MZ"); // Windows EXE/DLL
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    private static boolean startsWith(byte[] data, String asciiPrefix) {
        byte[] p = asciiPrefix.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        return startsWith(data, p);
    }
}

