package com.langfree.model;

/**
 * Événement à usage unique pour un Snackbar. Pas de equals : chaque instance est notifiée.
 */
public class UiSignal {
    public final String message;

    public UiSignal(String message) {
        this.message = message == null ? "" : message;
    }
}
