package com.langfree;

import android.app.Application;

import com.langfree.ai.ModelManager;
import com.langfree.database.DatabaseHelper;
import com.langfree.grammar.GrammarEngine;
import com.langfree.speech.AnaSpeaker;
import com.langfree.speech.VoskRecognizer;
import com.langfree.utils.PrefManager;

public class LangFreeApp extends Application {
    private static LangFreeApp instance;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        try {
            // Initialiser les singletons
            PrefManager.getInstance(this);
            GrammarEngine.getInstance(this);
            VoskRecognizer.getInstance(this);
            AnaSpeaker.getInstance(this);
            ModelManager.getInstance(this);
            DatabaseHelper.getInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static LangFreeApp getInstance() {
        return instance;
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        
        // Libérer les ressources
        VoskRecognizer vosk = VoskRecognizer.getInstance(this);
        if (vosk != null) {
            vosk.release();
        }
        
        AnaSpeaker speaker = AnaSpeaker.getInstance(this);
        if (speaker != null) {
            speaker.release();
        }
        
        ModelManager modelManager = ModelManager.getInstance(this);
        if (modelManager != null) {
            modelManager.unloadLlmModel();
        }
    }
}
