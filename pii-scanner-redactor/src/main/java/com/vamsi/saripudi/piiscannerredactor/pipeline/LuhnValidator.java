package com.vamsi.saripudi.piiscannerredactor.pipeline;

import org.springframework.stereotype.Component;

@Component
public class LuhnValidator {

    public boolean isValid(String s){
        String digitsOnly = s.replaceAll("[^0-9]", "");
        if(digitsOnly.length() < 13 || digitsOnly.length() > 19){
            return false;
        }
        int sum = 0;
        boolean second = false;
        for(int i = digitsOnly.length() - 1; i >= 0; i--){
            int c = digitsOnly.charAt(i) - '0';
            if(second){
                c *= 2;
                if(c > 9){
                    c -= 9;
                }
            }
            second = !second;
            sum += c;
        }
        return sum % 10 == 0;
    }
}
