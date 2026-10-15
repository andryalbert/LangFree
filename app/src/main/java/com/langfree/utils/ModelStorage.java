package com.langfree.utils;

import android.content.Context;

import java.io.File;

/**
 * Emplacement unique du modèle dans le stockage interne de l'application.
 * Une fois copié, plus aucun accès réseau n'est nécessaire.
 */
public final class ModelStorage {
    public static final String FILE_NAME = "llm-model.task";
    /** En dessous de ce poids, le fichier est un échec de téléchargement (page HTML, etc.). */
    public static final long MIN_BYTES = 80L * 1024L * 1024L;
    /** Release publique du dépôt. Aucun compte ni jeton n'est demandé à l'utilisateur. */
    public static final String DOWNLOAD_URL =
            "https://github.com/andryalbert/LangFree/releases/download/gemma3-1b-it-int4/gemma3-1b-it-int4.task";
    public static final String TERMS_URL = "https://ai.google.dev/gemma/terms";

    private ModelStorage() {
    }

    public static File modelFile(Context context) {
        File directory = new File(context.getApplicationContext().getFilesDir(), "models");
        if (!directory.exists() && !directory.mkdirs()) {
            // Le répertoire peut déjà exister : l'appelant vérifiera le fichier.
        }
        return new File(directory, FILE_NAME);
    }

    public static File partialFile(Context context) {
        return new File(modelFile(context).getParentFile(), FILE_NAME + ".part");
    }

    public static boolean isReady(Context context) {
        return isReady(modelFile(context));
    }

    public static boolean isReady(File file) {
        return file != null && file.isFile() && file.length() >= MIN_BYTES;
    }

    public static boolean delete(Context context) {
        File partial = partialFile(context);
        if (partial.exists()) {
            //noinspection ResultOfMethodCallIgnored
            partial.delete();
        }
        File model = modelFile(context);
        return !model.exists() || model.delete();
    }

    /** Remplace le modèle installé par un fichier temporaire déjà complet. */
    public static boolean install(Context context, File temporary) {
        File destination = modelFile(context);
        File parent = destination.getParentFile();
        if (parent != null && !parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        if (destination.exists() && !destination.delete()) {
            return false;
        }
        return temporary.renameTo(destination);
    }
}
