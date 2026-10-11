package com.langfree.utils;

import android.app.ActivityManager;
import android.content.Context;

public class AudioUtils {
    
    /**
     * Calcule la quantité de RAM disponible en Mo
     */
    public static long getAvailableMemoryMB(Context context) {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        
        if (activityManager != null) {
            activityManager.getMemoryInfo(memoryInfo);
            return memoryInfo.availMem / (1024 * 1024);
        }
        
        return 0;
    }
    
    /**
     * Calcule la RAM totale en Mo
     */
    public static long getTotalMemoryMB(Context context) {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        
        if (activityManager != null) {
            activityManager.getMemoryInfo(memoryInfo);
            return memoryInfo.totalMem / (1024 * 1024);
        }
        
        return 0;
    }
    
    /**
     * Détermine le profil de modèle approprié selon la RAM
     */
    public static String getRecommendedModelProfile(Context context) {
        long totalMemoryMB = getTotalMemoryMB(context);
        
        if (totalMemoryMB < 3072) { // Moins de 3 Go
            return "ultra_fast";
        } else {
            return "fast";
        }
    }
    
    /**
     * Détermine le nombre de threads CPU disponibles
     */
    public static int getAvailableCores() {
        return Runtime.getRuntime().availableProcessors();
    }
    
    /**
     * Retourne le nombre de threads recommandés pour l'IA
     * (nombre de cœurs - 1 pour laisser de la marge)
     */
    public static int getRecommendedThreads() {
        int cores = getAvailableCores();
        return Math.max(1, cores - 1);
    }
}
