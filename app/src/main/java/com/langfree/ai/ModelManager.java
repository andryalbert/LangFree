package com.langfree.ai;

import android.content.Context;
import android.os.Build;

import com.langfree.utils.AudioUtils;
import com.langfree.utils.PrefManager;

import java.io.File;

public class ModelManager {
    private static ModelManager instance;
    private Context context;
    private PrefManager prefManager;
    private LlamaEngine llamaEngine;
    
    // Chemins des modèles
    private static final String MODELS_DIR = "models";
    private static final String VOSK_MODEL_DIR = "vosk_model";
    private static final String LLM_MODEL_DIR = "llm_model";
    
    // URLs de téléchargement
    private static final String VOSK_MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip";
    private static final String SMOLLM_MODEL_URL = "https://huggingface.co/mradermacher/SmolLM2-135M-Instruct-GGUF/resolve/main/SmolLM2-135M-Instruct-Q4_K_M.gguf";
    private static final String QWEN_MODEL_URL = "https://huggingface.co/bartowski/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/Qwen2.5-0.5B-Instruct-Q4_K_M.gguf";
    
    private ModelManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefManager = PrefManager.getInstance(context);
        this.llamaEngine = LlamaEngine.getInstance(context);
    }
    
    public static synchronized ModelManager getInstance(Context context) {
        if (instance == null) {
            instance = new ModelManager(context);
        }
        return instance;
    }
    
    /**
     * Retourne le répertoire des modèles
     */
    public File getModelsDir() {
        return new File(context.getFilesDir(), MODELS_DIR);
    }
    
    /**
     * Retourne le répertoire du modèle Vosk
     */
    public File getVoskModelDir() {
        return new File(getModelsDir(), VOSK_MODEL_DIR);
    }
    
    /**
     * Retourne le fichier du modèle LLM
     */
    public File getLlmModelFile() {
        File llmDir = new File(getModelsDir(), LLM_MODEL_DIR);
        return new File(llmDir, getModelFileName());
    }
    
    /**
     * Retourne le nom du fichier selon le profil
     */
    private String getModelFileName() {
        String profile = prefManager.getModelProfile();
        if ("auto".equals(profile)) {
            profile = AudioUtils.getRecommendedModelProfile(context);
        }
        
        if ("ultra_fast".equals(profile)) {
            return "SmolLM2-135M-Instruct-Q4_K_M.gguf";
        } else {
            return "Qwen2.5-0.5B-Instruct-Q4_K_M.gguf";
        }
    }
    
    /**
     * Retourne l'URL du modèle selon le profil
     */
    public String getLlmModelUrl() {
        String profile = prefManager.getModelProfile();
        if ("auto".equals(profile)) {
            profile = AudioUtils.getRecommendedModelProfile(context);
        }
        
        if ("ultra_fast".equals(profile)) {
            return SMOLLM_MODEL_URL;
        } else {
            return QWEN_MODEL_URL;
        }
    }
    
    /**
     * Retourne l'URL du modèle Vosk
     */
    public String getVoskModelUrl() {
        return VOSK_MODEL_URL;
    }
    
    /**
     * Charge le modèle L выбран
     */
    public boolean loadLlmModel() {
        File modelFile = getLlmModelFile();
        if (!modelFile.exists()) {
            return false;
        }
        
        // Configurer les paramètres
        int threads = AudioUtils.getRecommendedThreads();
        llamaEngine.configureThreads(threads);
        llamaEngine.configureContext(512);
        
        // Charger le modèle
        return llamaEngine.load(modelFile.getAbsolutePath());
    }
    
    /**
     * Décharge le modèle LLM
     */
    public void unloadLlmModel() {
        llamaEngine.unload();
    }
    
    /**
     * Retourne si le modèle LLM est chargé
     */
    public boolean isLlmModelLoaded() {
        return llamaEngine.isLoaded();
    }
    
    /**
     * Génère une réponse avec le modèle LLM
     */
    public String generateResponse(String userMessage, String systemPrompt, String conversationHistory) {
        if (!isLlmModelLoaded()) {
            return "Model not loaded. Please reinstall.";
        }
        
        // Construire le prompt complet
        String fullPrompt = systemPrompt + "\n\n";
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            fullPrompt += conversationHistory + "\n\n";
        }
        fullPrompt += "User: " + userMessage + "\nAssistant:";
        
        // Générer la réponse
        return llamaEngine.generateResponse(fullPrompt, 40, 0.6f);
    }
    
    /**
     * Retourne le prompt système selon le niveau et le thème
     */
    public String getSystemPrompt() {
        String level = prefManager.getLevel();
        String theme = prefManager.getTheme();
        
        String basePrompt = "You are Ana, a friendly English partner. ";
        
        // Ajuster selon le niveau
        switch (level) {
            case "A1":
                basePrompt += "Use very simple words and short sentences. ";
                break;
            case "A2":
                basePrompt += "Use simple words and short sentences. ";
                break;
            case "B1":
                basePrompt += "Use common vocabulary and medium-length sentences. ";
                break;
            case "B2":
                basePrompt += "Use varied vocabulary and natural expressions. ";
                break;
        }
        
        // Ajuster selon le thème
        switch (theme) {
            case "restaurant":
                basePrompt += "Talk about food, restaurants, and ordering meals. ";
                break;
            case "travel":
                basePrompt += "Talk about travel, transportation, and tourism. ";
                break;
            case "work":
                basePrompt += "Talk about jobs, office life, and business. ";
                break;
            case "job_interview":
                basePrompt += "Help practice for job interviews. Ask interview questions. ";
                break;
            case "daily_life":
                basePrompt += "Talk about daily activities, family, and routines. ";
                break;
            default:
                // Conversation libre
                break;
        }
        
        basePrompt += "Reply in 1 or 2 very short, simple sentences. End with a short question to keep the conversation going.";
        
        return basePrompt;
    }
    
    /**
     * Calcule la taille totale nécessaire pour le téléchargement
     */
    public long getTotalDownloadSize() {
        String profile = prefManager.getModelProfile();
        if ("auto".equals(profile)) {
            profile = AudioUtils.getRecommendedModelProfile(context);
        }
        
        // Vosk: ~40 Mo
        long voskSize = 40 * 1024 * 1024;
        
        // LLM: ~150 Mo pour ultra-fast, ~400 Mo pour fast
        long llmSize = "ultra_fast".equals(profile) ? 150 * 1024 * 1024 : 400 * 1024 * 1024;
        
        return voskSize + llmSize;
    }
}
