package com.langfree.ai;

import android.content.Context;

public class LlamaEngine {
    private static LlamaEngine instance;
    private boolean modelLoaded = false;
    private Context context;
    
    // Charger la bibliothèque native
    static {
        System.loadLibrary("llama_wrapper");
    }
    
    private LlamaEngine(Context context) {
        this.context = context.getApplicationContext();
    }
    
    public static synchronized LlamaEngine getInstance(Context context) {
        if (instance == null) {
            instance = new LlamaEngine(context);
        }
        return instance;
    }
    
    // Méthodes natives
    private native boolean loadModel(String modelPath);
    private native void unloadModel();
    private native boolean isModelLoaded();
    private native String generate(String prompt, int maxTokens, float temperature);
    private native void setThreads(int threads);
    private native void setContextSize(int contextSize);
    
    /**
     * Charge le modèle GGUF
     */
    public boolean load(String modelPath) {
        modelLoaded = loadModel(modelPath);
        return modelLoaded;
    }
    
    /**
     * Décharge le modèle
     */
    public void unload() {
        unloadModel();
        modelLoaded = false;
    }
    
    /**
     * Génère une réponse
     */
    public String generateResponse(String prompt, int maxTokens, float temperature) {
        if (!modelLoaded) {
            return "Model not loaded";
        }
        return generate(prompt, maxTokens, temperature);
    }
    
    /**
     * Configure le nombre de threads
     */
    public void configureThreads(int threads) {
        setThreads(threads);
    }
    
    /**
     * Configure la taille du contexte
     */
    public void configureContext(int contextSize) {
        setContextSize(contextSize);
    }
    
    public boolean isLoaded() {
        return modelLoaded;
    }
}
