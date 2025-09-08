package com.vamsi.saripudi.piiscannerredactor.model;

public enum MatchType {
    EMAIL,
    PHONE,
    SSN,
    CREDIT_CARD,
    JWT,
    IPV4,
    IPV6,
    HIGH_ENTROPY,
    API_KEY,
    PASSWORD,
    PHYSICAL_ADDRESS;

    public boolean isHighEntropy() {
        return this == HIGH_ENTROPY;
    }
}
