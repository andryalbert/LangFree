package com.langfree.model;

import java.util.List;

public class Correction {
    private String originalText;
    private String correctedText;
    private String explanation;
    private List<Integer> errorIndices; // Positions des erreurs dans le texte original
    private boolean hasErrors;
    
    public Correction(String originalText, String correctedText, String explanation, 
                      List<Integer> errorIndices, boolean hasErrors) {
        this.originalText = originalText;
        this.correctedText = correctedText;
        this.explanation = explanation;
        this.errorIndices = errorIndices;
        this.hasErrors = hasErrors;
    }
    
    // Getters
    public String getOriginalText() { return originalText; }
    public String getCorrectedText() { return correctedText; }
    public String getExplanation() { return explanation; }
    public List<Integer> getErrorIndices() { return errorIndices; }
    public boolean hasErrors() { return hasErrors; }
    
    // Setters
    public void setOriginalText(String originalText) { this.originalText = originalText; }
    public void setCorrectedText(String correctedText) { this.correctedText = correctedText; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public void setErrorIndices(List<Integer> errorIndices) { this.errorIndices = errorIndices; }
    public void setHasErrors(boolean hasErrors) { this.hasErrors = hasErrors; }
}
