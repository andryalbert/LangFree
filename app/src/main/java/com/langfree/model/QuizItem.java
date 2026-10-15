package com.langfree.model;

/**
 * Question du mini-exercice construit à partir du carnet d'erreurs.
 * Les champs {@link #attempt} et {@link #feedback} vivent le temps de l'écran de révision.
 */
public class QuizItem {
    public final String prompt;
    public final String answer;
    public final String explanation;
    public String attempt = "";
    public String feedback = "";
    public boolean correct;

    public QuizItem(String prompt, String answer, String explanation) {
        this.prompt = prompt == null ? "" : prompt;
        this.answer = answer == null ? "" : answer;
        this.explanation = explanation == null ? "" : explanation;
    }
}
