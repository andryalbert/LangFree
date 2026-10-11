package com.langfree.speech;

import android.media.AudioRecord;

public class VoiceActivityDetector {
    // États du VAD
    public static final int STATE_WAITING = 0;      // Attend le début de la parole
    public static final int STATE_SPEAKING = 1;     // Parole en cours
    public static final int STATE_SILENCE = 2;       // Silence après parole
    public static final int STATE_FINISHED = 3;      // Fin de l'enregistrement
    
    private int currentState = STATE_WAITING;
    private int silenceDelayMs = 3000; // 3 secondes par défaut
    private long lastSpeechTime = 0;
    private long recordingStartTime = 0;
    private boolean hasSpoken = false;
    
    // Paramètres audio
    private static final int BUFFER_SIZE_MS = 30; // 30 ms
    private static final int CALIBRATION_MS = 400; // 400 ms de calibrage
    private static final int MIN_SPEECH_DURATION_MS = 250; // 250 ms minimum
    private static final int MAX_RECORDING_DURATION_MS = 60000; // 60 secondes max
    private static final int PRE_SPEECH_BUFFER_MS = 300; // 300 ms avant le début
    
    // Seuils
    private double noiseLevel = 0;
    private double speechThreshold = 0;
    private static final double THRESHOLD_MULTIPLIER = 3.0;
    private static final double MIN_THRESHOLD = 500.0;
    
    private int sensitivity = 1; // 0 = faible, 1 = normal, 2 = élevé
    
    public VoiceActivityDetector() {
        reset();
    }
    
    public void reset() {
        currentState = STATE_WAITING;
        lastSpeechTime = 0;
        recordingStartTime = 0;
        hasSpoken = false;
        noiseLevel = 0;
    }
    
    public void setSilenceDelay(int delayMs) {
        this.silenceDelayMs = delayMs;
    }
    
    public void setSensitivity(int sensitivity) {
        this.sensitivity = sensitivity;
    }
    
    /**
     * Traite un bloc audio et retourne le nouvel état
     */
    public int processAudio(short[] audioData, long currentTime) {
        double rms = calculateRMS(audioData);
        
        // Calibrage du bruit pendant les premières 400 ms
        if (recordingStartTime == 0) {
            recordingStartTime = currentTime;
            noiseLevel = rms;
            speechThreshold = Math.max(noiseLevel * THRESHOLD_MULTIPLIER, MIN_THRESHOLD);
            return currentState;
        }
        
        long elapsedTime = currentTime - recordingStartTime;
        
        // Ajuster le seuil selon la sensibilité
        double adjustedThreshold = speechThreshold;
        if (sensitivity == 0) {
            adjustedThreshold *= 1.5; // Plus sensible = seuil plus bas
        } else if (sensitivity == 2) {
            adjustedThreshold *= 0.7; // Moins sensible = seuil plus haut
        }
        
        switch (currentState) {
            case STATE_WAITING:
                // Attendre que l'utilisateur parle
                if (rms > adjustedThreshold) {
                    currentState = STATE_SPEAKING;
                    lastSpeechTime = currentTime;
                    hasSpoken = true;
                } else if (elapsedTime > 10000) {
                    // 10 secondes sans parole, annuler
                    currentState = STATE_FINISHED;
                }
                break;
                
            case STATE_SPEAKING:
                if (rms > adjustedThreshold) {
                    lastSpeechTime = currentTime;
                } else {
                    // Passage en silence
                    currentState = STATE_SILENCE;
                    lastSpeechTime = currentTime;
                }
                
                // Vérifier la durée max
                if (elapsedTime > MAX_RECORDING_DURATION_MS) {
                    currentState = STATE_FINISHED;
                }
                break;
                
            case STATE_SILENCE:
                if (rms > adjustedThreshold) {
                    // La parole reprend
                    currentState = STATE_SPEAKING;
                    lastSpeechTime = currentTime;
                } else {
                    // Vérifier si le silence dure assez longtemps
                    long silenceDuration = currentTime - lastSpeechTime;
                    if (silenceDuration >= silenceDelayMs) {
                        // Vérifier qu'on a parlé assez longtemps
                        if (hasSpoken && (elapsedTime > MIN_SPEECH_DURATION_MS)) {
                            currentState = STATE_FINISHED;
                        } else {
                            // Trop court, revenir en attente
                            currentState = STATE_WAITING;
                            hasSpoken = false;
                        }
                    }
                }
                break;
        }
        
        return currentState;
    }
    
    /**
     * Calcule le RMS (Root Mean Square) d'un bloc audio
     */
    private double calculateRMS(short[] audioData) {
        double sum = 0;
        for (short sample : audioData) {
            sum += sample * sample;
        }
        return Math.sqrt(sum / audioData.length);
    }
    
    public int getCurrentState() {
        return currentState;
    }
    
    public boolean hasSpoken() {
        return hasSpoken;
    }
    
    public long getRecordingDuration(long currentTime) {
        if (recordingStartTime == 0) return 0;
        return currentTime - recordingStartTime;
    }
    
    /**
     * Retourne le niveau audio normalisé (0.0 à 1.0) pour l'affichage
     */
    public double getAudioLevel(short[] audioData) {
        double rms = calculateRMS(audioData);
        double maxLevel = Math.max(speechThreshold * 2, MIN_THRESHOLD * 2);
        return Math.min(rms / maxLevel, 1.0);
    }
}
