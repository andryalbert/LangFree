package com.langfree.model;

/**
 * Niveau CECRL du dialogue. La valeur {@link #promptValue} est injectée dans le prompt.
 */
public enum Level {
    A1_A2("Débutant (A1-A2)", "A1-A2 (beginner)"),
    B1_B2("Intermédiaire (B1-B2)", "B1-B2 (intermediate)"),
    C1_C2("Avancé (C1-C2)", "C1-C2 (advanced)");

    public final String labelFr;
    public final String promptValue;

    Level(String labelFr, String promptValue) {
        this.labelFr = labelFr;
        this.promptValue = promptValue;
    }

    public static Level fromName(String name) {
        if (name == null) {
            return A1_A2;
        }
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return A1_A2;
        }
    }

    public static String labelOf(String name) {
        return fromName(name).labelFr;
    }
}
