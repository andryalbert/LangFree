package com.langfree.model;

/**
 * Thème imposé au tuteur. « Libre » le laisse suivre l'élève.
 */
public enum Topic {
    FREE("Libre", "free conversation; follow the student"),
    TRAVEL("Voyage", "travel: airports, hotels, directions and bookings"),
    WORK("Travail", "work: meetings, emails and colleagues"),
    RESTAURANT("Restaurant", "a restaurant: ordering, preferences and paying"),
    INTERVIEW("Entretien d'embauche", "a job interview"),
    DAILY("Quotidien", "daily life: family, hobbies and routines");

    public final String labelFr;
    public final String promptHint;

    Topic(String labelFr, String promptHint) {
        this.labelFr = labelFr;
        this.promptHint = promptHint;
    }

    public static Topic fromName(String name) {
        if (name == null) {
            return FREE;
        }
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return FREE;
        }
    }

    public static String labelOf(String name) {
        return fromName(name).labelFr;
    }
}
