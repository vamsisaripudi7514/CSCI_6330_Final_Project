package com.vamsi.saripudi.piiscannerredactor.pipeline;

import com.vamsi.saripudi.piiscannerredactor.model.DetectionResult;
import com.vamsi.saripudi.piiscannerredactor.model.MatchType;
import com.vamsi.saripudi.piiscannerredactor.util.EntropyUtil;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Data
@Component
public class EntropyScorer {

    //Starter Entropy Scores
    private static final double LOW_ENTROPY_THRESHOLD = 2.0;
    private static final double HIGH_ENTROPY_THRESHOLD = 4.0;

    public double score(String s){
        return EntropyUtil.shannonEntropy(s);
    }

    public static double highThreshold(){
        return HIGH_ENTROPY_THRESHOLD;
    }

    public static double lowThreshold(){
        return LOW_ENTROPY_THRESHOLD;
    }

    public List<DetectionResult> evaluateToken(String token, Path file, int lineNo, int start){
        double score = score(token);
        if(score < lowThreshold()){
            return List.of();
        }
        if(score >= highThreshold()){
            return List.of(DetectionResult.builder().
                    filePath(file).
                    line(lineNo).
                    startCol(start).
                    endCol(start + token.length()).
                    type(MatchType.HIGH_ENTROPY).
                    value(token).
                    score(score).
                    build());
        }
        return List.of();
    }
}
