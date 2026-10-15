package com.langfree.utils;

import android.content.Context;

import com.langfree.R;
import com.langfree.ai.LlmManager;

/** Transforme une erreur du moteur en phrase française. */
public final class LlmMessages {
    private LlmMessages() {
    }

    public static String toFrench(Context context, LlmManager.LlmError error) {
        if (error == null) {
            return context.getString(R.string.error_model_failed);
        }
        switch (error) {
            case NOT_LOADED:
                return context.getString(R.string.error_model_not_loaded);
            case OUT_OF_MEMORY:
                return context.getString(R.string.error_out_of_memory);
            case MISSING_MODEL:
                return context.getString(R.string.error_model_missing);
            case UNSUPPORTED_DEVICE:
                return context.getString(R.string.error_unsupported_device);
            case TIMEOUT:
                return context.getString(R.string.error_timeout);
            case CANCELLED:
                return context.getString(R.string.error_cancelled);
            case FAILED:
            default:
                return context.getString(R.string.error_model_failed);
        }
    }
}
