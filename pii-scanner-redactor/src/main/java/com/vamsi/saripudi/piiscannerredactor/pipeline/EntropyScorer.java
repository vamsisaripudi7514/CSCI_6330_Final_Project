package com.vamsi.saripudi.piiscannerredactor.pipeline;

import com.vamsi.saripudi.piiscannerredactor.util.EntropyUtil;
import org.springframework.stereotype.Component;

@Component
public class EntropyScorer {

    //Starter Entropy Scores
    private static final double LOW_ENTROPY_THRESHOLD = 2.0;
    private static final double HIGH_ENTROPY_THRESHOLD = 4.0;

    public double score(String s){
        return EntropyUtil.shannonEntropy(s);
    }

    public double highThreshold(){
        return HIGH_ENTROPY_THRESHOLD;
    }

    public double lowThreshold(){
        return LOW_ENTROPY_THRESHOLD;
    }
}
