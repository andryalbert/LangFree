#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "LlamaWrapper"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Classe de gestion du modèle (stub pour l'intégration complète)
class LlamaModel {
public:
    bool loaded = false;
    std::string modelPath;
    
    bool load(const char* path) {
        modelPath = path;
        // TODO: Intégration complète avec llama.cpp
        // Pour l'instant, on simule le chargement
        loaded = true;
        LOGI("Model loaded from: %s", path);
        return true;
    }
    
    void unload() {
        loaded = false;
        modelPath.clear();
        LOGI("Model unloaded");
    }
    
    std::string generate(const char* prompt, int maxTokens, float temperature) {
        // TODO: Intégration complète avec llama.cpp
        // Pour l'instant, on retourne une réponse simulée
        if (!loaded) {
            return "Error: Model not loaded";
        }
        
        // Réponse simulée pour tester l'interface
        return "That's interesting! Tell me more.";
    }
    
    bool isLoaded() {
        return loaded;
    }
};

// Instance globale du modèle
static LlamaModel g_model;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_langfree_ai_LlamaEngine_loadModel(JNIEnv* env, jobject thiz, jstring modelPath) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    bool result = g_model.load(path);
    env->ReleaseStringUTFChars(modelPath, path);
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_langfree_ai_LlamaEngine_unloadModel(JNIEnv* env, jobject thiz) {
    g_model.unload();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_langfree_ai_LlamaEngine_isModelLoaded(JNIEnv* env, jobject thiz) {
    return g_model.isLoaded();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_langfree_ai_LlamaEngine_generate(JNIEnv* env, jobject thiz, 
                                          jstring prompt, 
                                          jint maxTokens, 
                                          jfloat temperature) {
    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    std::string result = g_model.generate(promptStr, maxTokens, temperature);
    env->ReleaseStringUTFChars(prompt, promptStr);
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_langfree_ai_LlamaEngine_setThreads(JNIEnv* env, jobject thiz, jint threads) {
    // TODO: Configurer le nombre de threads pour llama.cpp
    LOGI("Threads set to: %d", threads);
}

extern "C" JNIEXPORT void JNICALL
Java_com_langfree_ai_LlamaEngine_setContextSize(JNIEnv* env, jobject thiz, jint contextSize) {
    // TODO: Configurer la taille du contexte pour llama.cpp
    LOGI("Context size set to: %d", contextSize);
}
