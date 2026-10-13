package com.langfree;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.langfree.ai.ModelManager;
import com.langfree.database.DatabaseHelper;
import com.langfree.grammar.GrammarEngine;
import com.langfree.model.Correction;
import com.langfree.model.Message;
import com.langfree.model.VocabularyItem;
import com.langfree.speech.AnaSpeaker;
import com.langfree.speech.VoiceActivityDetector;
import com.langfree.speech.VoskRecognizer;
import com.langfree.ui.MessageAdapter;
import com.langfree.utils.PrefManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    // Vues
    private RecyclerView messageRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ImageButton micButton;
    private ImageButton settingsButton;
    private Spinner themeSpinner;
    private TextView silenceCountdown;
    private TextView statusText;
    private View audioLevelIndicator;
    
    // Composants
    private MessageAdapter messageAdapter;
    private VoskRecognizer voskRecognizer;
    private VoiceActivityDetector vad;
    private AnaSpeaker anaSpeaker;
    private GrammarEngine grammarEngine;
    private ModelManager modelManager;
    private PrefManager prefManager;
    private DatabaseHelper dbHelper;
    
    // État
    private List<Message> messages = new ArrayList<>();
    private boolean isListening = false;
    private String currentRecognizedText = "";
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable silenceRunnable;
    private boolean isAnaSpeaking = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            // Vérifier si c'est le premier lancement
            prefManager = PrefManager.getInstance(this);
            if (prefManager.isFirstLaunch()) {
                prefManager.setFirstLaunch(false);
                // Rediriger vers l'installation
                Intent intent = new Intent(this, InstallationActivity.class);
                startActivity(intent);
                finish();
                return;
            }
            
            // Vérifier si les modèles sont installés
            if (!prefManager.areModelsInstalled()) {
                Intent intent = new Intent(this, InstallationActivity.class);
                startActivity(intent);
                finish();
                return;
            }
            
            setContentView(R.layout.activity_main);
            
            // Initialiser les composants
            initViews();
            initComponents();
            setupListeners();
            loadMessages();
            loadModels();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur de démarrage: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void initViews() {
        messageRecyclerView = findViewById(R.id.messageRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        micButton = findViewById(R.id.micButton);
        settingsButton = findViewById(R.id.settingsButton);
        themeSpinner = findViewById(R.id.themeSpinner);
        silenceCountdown = findViewById(R.id.silenceCountdown);
        statusText = findViewById(R.id.statusText);
        audioLevelIndicator = findViewById(R.id.audioLevelIndicator);
    }
    
    private void initComponents() {
        // Initialiser les singletons
        voskRecognizer = VoskRecognizer.getInstance(this);
        vad = new VoiceActivityDetector();
        anaSpeaker = AnaSpeaker.getInstance(this);
        grammarEngine = GrammarEngine.getInstance(this);
        modelManager = ModelManager.getInstance(this);
        dbHelper = DatabaseHelper.getInstance(this);
        
        // Configurer le VAD
        vad.setSilenceDelay(prefManager.getSilenceDelay() * 1000);
        vad.setSensitivity(prefManager.getMicSensitivity());
        
        // Configurer l'adaptateur
        messageAdapter = new MessageAdapter(message -> {
            // Ajouter au carnet de vocabulaire
            VocabularyItem item = new VocabularyItem(
                message.getCorrection().getCorrectedText(),
                message.getText()
            );
            dbHelper.addVocabularyItem(item);
            Toast.makeText(this, "Ajouté au carnet", Toast.LENGTH_SHORT).show();
        });
        messageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageRecyclerView.setAdapter(messageAdapter);
        
        // Configurer le spinner de thème
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.themes_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        themeSpinner.setAdapter(adapter);
        
        String currentTheme = prefManager.getTheme();
        int themeIndex = getThemeIndex(currentTheme);
        themeSpinner.setSelection(themeIndex);
    }
    
    private int getThemeIndex(String theme) {
        String[] themes = getResources().getStringArray(R.array.themes_array);
        for (int i = 0; i < themes.length; i++) {
            if (themes[i].equalsIgnoreCase(theme.replace("_", " "))) {
                return i;
            }
        }
        return 0;
    }
    
    private void setupListeners() {
        // Envoyer message écrit
        sendButton.setOnClickListener(v -> {
            String text = messageInput.getText().toString().trim();
            if (!text.isEmpty()) {
                processTextMessage(text);
                messageInput.setText("");
            }
        });
        
        // Bouton micro
        micButton.setOnClickListener(v -> {
            if (isListening) {
                stopListening();
            } else {
                startListening();
            }
        });
        
        // Bouton réglages
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
        
        // Changement de thème
        themeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] themes = getResources().getStringArray(R.array.themes_array);
                String theme = themes[position].toLowerCase().replace(" ", "_");
                prefManager.setTheme(theme);
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        // Configurer AnaSpeaker
        anaSpeaker.setSpeechRate(prefManager.getVoiceSpeed());
    }
    
    private void loadMessages() {
        messages = dbHelper.getAllMessages();
        messageAdapter.setMessages(messages);
        
        // Afficher le message de bienvenue si pas de messages
        if (messages.isEmpty()) {
            showWelcomeMessage();
        }
    }
    
    private void showWelcomeMessage() {
        // Vérifier si le message de bienvenue existe déjà dans la base de données
        List<Message> allMessages = dbHelper.getAllMessages();
        for (Message msg : allMessages) {
            if (msg.getType() == Message.TYPE_ANA && 
                msg.getText().equals(getString(R.string.welcome_message))) {
                // Le message existe déjà, ne pas l'ajouter
                return;
            }
        }
        
        Message welcome = new Message(Message.TYPE_ANA, getString(R.string.welcome_message));
        messages.add(welcome);
        messageAdapter.addMessage(welcome);
        dbHelper.addMessage(welcome);
        
        // Faire lire le message de bienvenue par Ana
        speakAnaResponse(welcome.getText());
    }
    
    private void loadModels() {
        try {
            // Charger le modèle Vosk
            String voskPath = modelManager.getVoskModelDir().getAbsolutePath();
            boolean loaded = voskRecognizer.loadModel(voskPath);
            
            if (!loaded) {
                Toast.makeText(this, "Erreur de chargement du modèle Vosk", Toast.LENGTH_LONG).show();
            }
            
            // Charger le modèle LLM
            boolean llmLoaded = modelManager.loadLlmModel();
            if (!llmLoaded) {
                Toast.makeText(this, "Erreur de chargement du modèle IA", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur lors du chargement des modèles", Toast.LENGTH_LONG).show();
        }
    }
    
    private void startListening() {
        // Vérifier la permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        PERMISSION_REQUEST_CODE);
                return;
            }
        }
        
        // Vérifier si Ana parle
        if (isAnaSpeaking) {
            Toast.makeText(this, "Attendez qu'Ana finisse de parler", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isListening = true;
        currentRecognizedText = "";
        vad.reset();
        
        // UI updates
        micButton.setImageResource(android.R.drawable.ic_media_pause);
        statusText.setText(R.string.listening);
        statusText.setVisibility(View.VISIBLE);
        silenceCountdown.setVisibility(View.GONE);
        
        // Démarrer la reconnaissance
        voskRecognizer.startListening(new VoskRecognizer.RecognitionListener() {
            @Override
            public void onPartialResult(String text) {
                try {
                    JSONObject json = new JSONObject(text);
                    String partial = json.optString("partial", "");
                    if (!partial.isEmpty()) {
                        currentRecognizedText = partial;
                        // Afficher le texte en temps réel (optionnel)
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            @Override
            public void onFinalResult(String text) {
                try {
                    JSONObject json = new JSONObject(text);
                    String finalText = json.optString("text", "");
                    if (!finalText.isEmpty()) {
                        currentRecognizedText = finalText;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                stopListening();
            }
        });
        
        // Démarrer le timer de silence
        startSilenceTimer();
    }
    
    private void stopListening() {
        isListening = false;
        voskRecognizer.stopListening();
        
        // UI updates
        micButton.setImageResource(android.R.drawable.ic_btn_speak_now);
        statusText.setVisibility(View.GONE);
        silenceCountdown.setVisibility(View.GONE);
        
        // Annuler le timer
        if (silenceRunnable != null) {
            handler.removeCallbacks(silenceRunnable);
        }
        
        // Traiter le texte reconnu
        if (!currentRecognizedText.isEmpty()) {
            processVoiceMessage(currentRecognizedText);
        } else {
            Toast.makeText(this, R.string.nothing_heard, Toast.LENGTH_SHORT).show();
        }
        
        currentRecognizedText = "";
    }
    
    private void startSilenceTimer() {
        if (silenceRunnable != null) {
            handler.removeCallbacks(silenceRunnable);
        }
        
        final int delay = prefManager.getSilenceDelay();
        silenceRunnable = new Runnable() {
            int countdown = delay;
            
            @Override
            public void run() {
                if (!isListening) return;
                
                if (countdown > 0) {
                    silenceCountdown.setText(String.valueOf(countdown));
                    silenceCountdown.setVisibility(View.VISIBLE);
                    countdown--;
                    handler.postDelayed(this, 1000);
                } else {
                    stopListening();
                }
            }
        };
        
        handler.postDelayed(silenceRunnable, 1000);
    }
    
    private void processTextMessage(String text) {
        // Corriger le texte
        String explanationLang = prefManager.getExplanationLanguage();
        Correction correction = grammarEngine.correct(text, explanationLang);
        
        // Créer le message utilisateur
        Message userMessage = new Message(Message.TYPE_USER, text, correction);
        messages.add(userMessage);
        messageAdapter.addMessage(userMessage);
        dbHelper.addMessage(userMessage);
        
        // Générer la réponse d'Ana
        generateAnaResponse(text);
    }
    
    private void processVoiceMessage(String text) {
        processTextMessage(text);
    }
    
    private void generateAnaResponse(String userMessage) {
        statusText.setText(R.string.thinking);
        statusText.setVisibility(View.VISIBLE);
        
        new Thread(() -> {
            String systemPrompt = modelManager.getSystemPrompt();
            String history = buildConversationHistory();
            String response = modelManager.generateResponse(userMessage, systemPrompt, history);
            
            handler.post(() -> {
                statusText.setVisibility(View.GONE);
                
                // Créer le message d'Ana
                Message anaMessage = new Message(Message.TYPE_ANA, response);
                messages.add(anaMessage);
                messageAdapter.addMessage(anaMessage);
                dbHelper.addMessage(anaMessage);
                
                // Lire la réponse à voix haute (toujours activé pour la conversation)
                speakAnaResponse(response);
            });
        }).start();
    }
    
    private String buildConversationHistory() {
        // Garder seulement les 4 derniers échanges
        int historySize = Math.min(messages.size(), 8); // 4 échanges = 8 messages
        StringBuilder history = new StringBuilder();
        
        for (int i = messages.size() - historySize; i < messages.size(); i++) {
            Message msg = messages.get(i);
            if (msg.isUserMessage()) {
                history.append("User: ").append(msg.getText()).append("\n");
            } else {
                history.append("Assistant: ").append(msg.getText()).append("\n");
            }
        }
        
        return history.toString();
    }
    
    private void speakAnaResponse(String text) {
        isAnaSpeaking = true;
        statusText.setText(R.string.speaking);
        statusText.setVisibility(View.VISIBLE);
        
        anaSpeaker.speak(text, new AnaSpeaker.SpeakingListener() {
            @Override
            public void onSpeakingStarted() {
                isAnaSpeaking = true;
            }
            
            @Override
            public void onSpeakingFinished() {
                isAnaSpeaking = false;
                statusText.setVisibility(View.GONE);
            }
            
            @Override
            public void onError(String error) {
                isAnaSpeaking = false;
                statusText.setVisibility(View.GONE);
            }
        });
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                Toast.makeText(this, R.string.enable_micro, Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (isListening) {
            stopListening();
        }
        
        if (anaSpeaker != null) {
            anaSpeaker.stop();
        }
    }
}
