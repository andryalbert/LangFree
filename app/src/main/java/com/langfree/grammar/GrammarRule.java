package com.langfree.grammar;

public class GrammarRule {
    private String pattern;
    private String replacement;
    private String explanationFr;
    private String explanationEnSimple;
    
    public GrammarRule(String pattern, String replacement, String explanationFr, String explanationEnSimple) {
        this.pattern = pattern;
        this.replacement = replacement;
        this.explanationFr = explanationFr;
        this.explanationEnSimple = explanationEnSimple;
    }
    
    // Getters
    public String getPattern() { return pattern; }
    public String getReplacement() { return replacement; }
    public String getExplanationFr() { return explanationFr; }
    public String getExplanationEnSimple() { return explanationEnSimple; }
    
    // Setters
    public void setPattern(String pattern) { this.pattern = pattern; }
    public void setReplacement(String replacement) { this.replacement = replacement; }
    public void setExplanationFr(String explanationFr) { this.explanationFr = explanationFr; }
    public void setExplanationEnSimple(String explanationEnSimple) { this.explanationEnSimple = explanationEnSimple; }
}
