package com.langfree.speech;

import android.content.Context;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Locale;

public class AnaSpeaker implements TextToSpeech.OnInitListener {
    private static AnaSpeaker instance;
    private TextToSpeech tts;
    private Context context;
    private boolean isInitialized = false;
    private boolean isSpeaking = false;
    private SpeakingListener listener;
    
    public interface SpeakingListener {
        void onSpeakingStarted();
        void onSpeakingFinished();
        void onError(String error);
    }
    
    public AnaSpeaker(Context context) {
        this.context = context.getApplicationContext();
        tts = new TextToSpeech(context, this);
    }
    
    public static synchronized AnaSpeaker getInstance(Context context) {
        if (instance == null) {
            instance = new AnaSpeaker(context);
        }
        return instance;
    }
    
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // Essayer de définir la langue anglaise
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Essayer avec UK
                result = tts.setLanguage(Locale.UK);
            }
            
            if (result == TextToSpeech.SUCCESS || result == TextToSpeech.LANG_AVAILABLE) {
                isInitialized = true;
            }
        }
    }
    
    /**
     * Configure la vitesse de la voix (0.0 à 2.0, 1.0 = normal)
     */
    public void setSpeechRate(float rate) {
        if (tts != null) {
            tts.setSpeechRate(rate);
        }
    }
    
    /**
     * Lit un texte phrase par phrase
     */
    public void speak(String text, SpeakingListener listener) {
        if (!isInitialized) {
            if (listener != null) {
                listener.onError("TTS not initialized");
            }
            return;
        }
        
        this.listener = listener;
        
        // Diviser le texte en phrases
        String[] sentences = text.split("(?<=[.!?])\\s+");
        
        // Lire la première phrase immédiatement
        if (sentences.length > 0) {
            speakSentence(sentences[0], 0, sentences);
        }
    }
    
    private void speakSentence(String sentence, int index, String[] allSentences) {
        if (index >= allSentences.length) {
            // Toutes les phrases ont été lues
            isSpeaking = false;
            if (listener != null) {
                listener.onSpeakingFinished();
            }
            return;
        }
        
        isSpeaking = true;
        
        if (listener != null && index == 0) {
            listener.onSpeakingStarted();
        }
        
        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "sentence_" + index);
        
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                // Phrase commencée
            }
            
            @Override
            public void onDone(String utteranceId) {
                // Phrase terminée, passer à la suivante
                speakSentence(allSentences[index + 1], index + 1, allSentences);
            }
            
            @Override
            public void onError(String utteranceId) {
                // Erreur, continuer quand même
                speakSentence(allSentences[index + 1], index + 1, allSentences);
            }
        });
        
        tts.speak(sentence, TextToSpeech.QUEUE_ADD, params);
    }
    
    /**
     * Arrête la lecture en cours
     */
    public void stop() {
        if (tts != null) {
            tts.stop();
            isSpeaking = false;
        }
    }
    
    /**
     * Lit un texte complet (sans découpage en phrases)
     */
    public void speakSimple(String text) {
        if (!isInitialized) {
            return;
        }
        
        tts.speak(text, TextToSpeech.QUEUE_ADD, null);
    }
    
    public boolean isSpeaking() {
        return isSpeaking || (tts != null && tts.isSpeaking());
    }
    
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Vérifie si la voix anglaise est disponible
     */
    public boolean isEnglishVoiceAvailable() {
        if (!isInitialized) {
            return false;
        }
        
        int result = tts.setLanguage(Locale.US);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            return false;
        }
        return true;
    }
    
    /**
     * Libère les ressources
     */
    public void release() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        isInitialized = false;
    }
}
