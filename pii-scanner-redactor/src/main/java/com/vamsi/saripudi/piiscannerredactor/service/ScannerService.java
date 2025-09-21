package com.vamsi.saripudi.piiscannerredactor.service;

import com.vamsi.saripudi.piiscannerredactor.model.DetectionResult;
import com.vamsi.saripudi.piiscannerredactor.model.FileSummary;
import com.vamsi.saripudi.piiscannerredactor.model.MatchType;
import com.vamsi.saripudi.piiscannerredactor.model.ScanJob;
import com.vamsi.saripudi.piiscannerredactor.pipeline.*;
import com.vamsi.saripudi.piiscannerredactor.util.BinarySniffer;
import com.vamsi.saripudi.piiscannerredactor.util.FileWalker;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Core scanning engine:
 *  - Walk input paths to files.
 *  - For each file, submit a virtual-thread task (file-level parallelism).
 *  - For each line, tokenize (space-first w/ tiny key=value support), detect, redact left->right.
 *  - Merge findings to CSV/JSONL and write redacted mirror via ReportingService.
 */
@Service
public class ScannerService {

    private final ExecutorService fileExecutor;     // virtual threads
    private final ReportingService reporting;
    private final BinarySniffer sniffer;
    private final RegexMatcher regex;
    private final EntropyScorer entropy;
    private final Redactor redactor;
    private final LuhnValidator luhn;

    @Autowired
    public ScannerService(@Qualifier("fileExecutor") ExecutorService fileExecutor,
                         ReportingService reporting,
                         BinarySniffer sniffer,
                         RegexMatcher regex,
                         EntropyScorer entropy,
                         Redactor redactor,
                         LuhnValidator luhn) {
        this.fileExecutor = fileExecutor;
        this.reporting = reporting;
        this.sniffer = sniffer;
        this.regex = regex;
        this.entropy = entropy;
        this.redactor = redactor;
        this.luhn = luhn;
    }

    /** Run a scan job across all files under the given inputs. */
    public void scan(ScanJob job, List<Path> inputs) throws Exception {
        List<Path> files = FileWalker.listFiles(inputs);
        job.setFilesTotal(files.size());

        var futures = files.stream()
                .map(p -> fileExecutor.submit(() -> processOne(job, p)))
                .collect(Collectors.toList());

        for (var f : futures) {
            if (job.isCancelRequested()) break;
            FileSummary summary = f.get(); // join per file
            reporting.merge(job, summary);
            job.incFilesScanned();
            job.addBytes(summary.getBytes());
            // (optional) bump per-type aggregates
//            summary.getCountsByType().forEach(job::bumpTypeCount);
        }

        reporting.finalizeOutputs(job);
    }

    /** Process a single file sequentially (line by line). */
    private FileSummary processOne(ScanJob job, Path file) throws IOException {
        long size = Files.isRegularFile(file) ? Files.size(file) : 0L;

        if (sniffer.isBinary(file)) {
            // Skip binaries; still count bytes and mark as binary summary
            return FileSummary.binary(file, size);
        }

        List<DetectionResult> findings = new ArrayList<>();
        StringBuilder redacted = new StringBuilder(Math.min((int) Math.max(4096, size), 1 << 20));

        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                LineResult lr = processLine(file, lineNo, line, "ENC"); // default mode; make configurable
                findings.addAll(lr.findings);
                redacted.append(lr.redacted).append('\n');

                if (job.isCancelRequested()) break;
            }
        }

        return FileSummary.text(file, size, findings, redacted.toString());
    }

    // ---- per-line pipeline (space-first tokenization) ----

//    private static final class Tok {
//        final String text; final int start; final int end; final String keyHint;
//        Tok(String t, int s, int e, String key) { text = t; start = s; end = e; keyHint = key; }
//    }

    private record LineResult(String redacted, List<DetectionResult> findings) {}

    /**
     * Tokenize -> detect -> merge -> redact (left->right).
     * Tokenization: split by spaces, but keep quoted segments intact; emit value side of key=value with keyHint.
     */
    private LineResult processLine(Path file, int lineNo, String line, String mode) {
        if (line == null || line.isEmpty()) return new LineResult("", List.of());

        List<Tokenizer.Token> tokens = Tokenizer.tokenize(line);
        ArrayList<DetectionResult> hits = new ArrayList<>();

        for (Tokenizer.Token t : tokens) {
            // Regex against token (detector returns line-relative offsets via supplied offset)
            List<DetectionResult> rx = regex.matchesToken(t.text, file, lineNo, t.start);
            // Yet to implement this feature
//            if (t.keyHint != null && !t.keyHint.isBlank()) {
//                regex.maybeBoostByKeyContext(rx, t.keyHint);
//            }

            // Entropy detector (token-wise)
            List<DetectionResult> en = entropy.evaluateToken(t.text, file, lineNo, t.start);

            // Merge and dedupe overlaps (same type: keep longer/higher score)
            List<DetectionResult> merged = new ArrayList<>(mergeAndDedupe(rx, en));
            // Luhn-gate credit cards
            merged.removeIf(r -> r.getType() == MatchType.CREDIT_CARD && !luhn.isValid(r.getValue()));

            hits.addAll(merged);
        }

        hits.sort(Comparator.comparingInt(DetectionResult::getStartCol));
        String red = redactLeftToRight(line, hits, file, lineNo, mode);

        return new LineResult(red, hits);
    }

    /** Space-first tokenizer with two carve-outs: quoted strings and key=value (emit value token w/ key hint). */
//    private static List<Tok> tokenizeSpaceFirst(String line) {
//        List<Tok> out = new ArrayList<>();
//        int n = line.length();
//
//        // Mark quoted spans so we don't split them by spaces
//        boolean[] quoted = new boolean[n];
//        for (int i = 0; i < n; i++) {
//            char c = line.charAt(i);
//            if (c == '"' || c == '\'') {
//                int j = i + 1;
//                while (j < n && line.charAt(j) != c) j++;
//                int end = Math.min(j + 1, n);
//                for (int k = i; k < end; k++) quoted[k] = true;
//                i = end - 1;
//            }
//        }
//
//        int i = 0;
//        while (i < n) {
//            // skip whitespace outside quotes
//            if (!quoted[i] && Character.isWhitespace(line.charAt(i))) { i++; continue; }
//            int start = i;
//            while (i < n && (quoted[i] || !Character.isWhitespace(line.charAt(i)))) i++;
//            int end = i;
//            String tok = line.substring(start, end);
//
//            // key=value split (tolerant; value can contain non-spaces thanks to our segment)
//            int eq = tok.indexOf('=');
//            if (eq > 0 && eq < tok.length() - 1) {
//                String key = tok.substring(0, eq);
//                String val = tok.substring(eq + 1);
//                int keyStart = start;
//                int keyEnd = start + eq;
//                int valStart = start + eq + 1;
//                int valEnd = end;
//
//                // key token (mostly for context); value token (primary for detection)
//                out.add(new Tok(key, keyStart, keyEnd, null));
//                out.add(new Tok(val, valStart, valEnd, key));
//            } else {
//                out.add(new Tok(tok, start, end, null));
//            }
//        }
//        return out;
//    }

    /** Merge lists then dedupe overlaps (same type: keep longer / higher score). */
    private static List<DetectionResult> mergeAndDedupe(List<DetectionResult> a, List<DetectionResult> b) {
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a;
        ArrayList<DetectionResult> list = new ArrayList<>(a.size() + b.size());
        list.addAll(a); list.addAll(b);

        list.sort(Comparator.<DetectionResult>comparingInt(DetectionResult::getStartCol)
                .thenComparingInt(DetectionResult::getEndCol));

        ArrayList<DetectionResult> out = new ArrayList<>();
        for (DetectionResult r : list) {
            if (out.isEmpty()) { out.add(r); continue; }
            DetectionResult last = out.get(out.size() - 1);
            boolean overlap = r.getStartCol() <= last.getEndCol();
            boolean sameType = r.getType() == last.getType();
            if (overlap && sameType) {
                int rLen = r.getEndCol() - r.getStartCol();
                int lLen = last.getEndCol() - last.getStartCol();
                if (rLen > lLen || r.getScore() > last.getScore()) {
                    out.set(out.size() - 1, r);
                }
            } else {
                out.add(r);
            }
        }
        return out;
    }

    /** Build redacted string by splicing replacements left->right (avoid index shifts). */
    private String redactLeftToRight(String line, List<DetectionResult> hits,
                                     Path file, int lineNo, String mode) {
        StringBuilder sb = new StringBuilder(line.length() + 32);
        int cursor = 0;
        for (DetectionResult r : hits) {
            int s = r.getStartCol(), e = r.getEndCol();
            if (s < cursor) continue;                 // already replaced by previous span
            if (s > cursor) sb.append(line, cursor, s);
            sb.append(redactor.encryptToken(r.getValue(), r.getType() ,file));
            cursor = e;
        }
        if (cursor < line.length()) sb.append(line, cursor, line.length());
        return sb.toString();
    }
}
