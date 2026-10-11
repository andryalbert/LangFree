package com.langfree.speech;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.File;
import java.io.IOException;

public class VoskRecognizer {
    private static VoskRecognizer instance;
    private Model model;
    private Recognizer recognizer;
    private AudioRecord audioRecord;
    private Context context;
    
    private boolean isListening = false;
    private RecognitionListener listener;
    
    public interface RecognitionListener {
        void onPartialResult(String text);
        void onFinalResult(String text);
        void onError(String error);
    }
    
    private VoskRecognizer(Context context) {
        this.context = context.getApplicationContext();
        LibVosk.setLogLevel(LogLevel.INFO);
    }
    
    public static synchronized VoskRecognizer getInstance(Context context) {
        if (instance == null) {
            instance = new VoskRecognizer(context);
        }
        return instance;
    }
    
    /**
     * Charge le modèle Vosk depuis le stockage interne
     */
    public boolean loadModel(String modelPath) {
        try {
            File modelDir = new File(modelPath);
            if (!modelDir.exists()) {
                return false;
            }
            model = new Model(modelPath);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Démarre la reconnaissance vocale
     */
    public boolean startListening(RecognitionListener listener) {
        if (model == null) {
            if (this.listener != null) {
                this.listener.onError("Model not loaded");
            }
            return false;
        }
        
        this.listener = listener;
        
        try {
            // Créer le recognizer avec le modèle
            recognizer = new Recognizer(model, 16000.0f);
            
            // Configuration de l'audio
            int sampleRate = 16000;
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            int channelConfig = AudioFormat.CHANNEL_IN_MONO;
            
            int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2;
            
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            );
            
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                if (this.listener != null) {
                    this.listener.onError("Failed to initialize audio recorder");
                }
                return false;
            }
            
            audioRecord.startRecording();
            isListening = true;
            
            // Démarrer le thread de traitement audio
            new Thread(new AudioProcessingThread(bufferSize)).start();
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (this.listener != null) {
                this.listener.onError("Failed to start recognition: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Arrête la reconnaissance vocale
     */
    public void stopListening() {
        isListening = false;
        
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            audioRecord = null;
        }
        
        if (recognizer != null) {
            recognizer.free();
            recognizer = null;
        }
    }
    
    /**
     * Libère les ressources
     */
    public void release() {
        stopListening();
        
        if (model != null) {
            model.close();
            model = null;
        }
    }
    
    private class AudioProcessingThread implements Runnable {
        private int bufferSize;
        
        AudioProcessingThread(int bufferSize) {
            this.bufferSize = bufferSize;
        }
        
        @Override
        public void run() {
            short[] buffer = new short[bufferSize / 2];
            
            while (isListening && audioRecord != null) {
                int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                
                if (bytesRead > 0) {
                    if (recognizer != null) {
                        if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                            // Résultat final
                            String result = recognizer.getResult();
                            if (listener != null) {
                                listener.onFinalResult(result);
                            }
                        } else {
                            // Résultat partiel
                            String partial = recognizer.getPartialResult();
                            if (listener != null) {
                                listener.onPartialResult(partial);
                            }
                        }
                    }
                } else {
                    // Erreur de lecture
                    if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                        if (listener != null) {
                            listener.onError("Audio record error");
                        }
                        break;
                    }
                }
            }
        }
    }
    
    public boolean isListening() {
        return isListening;
    }
    
    public boolean isModelLoaded() {
        return model != null;
    }
}
