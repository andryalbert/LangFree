package com.langfree;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.langfree.database.DatabaseHelper;
import com.langfree.database.VocabularyDao;
import com.langfree.model.VocabularyItem;
import com.langfree.speech.AnaSpeaker;
import com.langfree.utils.AudioUtils;
import com.langfree.utils.PrefManager;

import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    private PrefManager prefManager;
    private DatabaseHelper dbHelper;
    private AnaSpeaker anaSpeaker;
    
    // Vues
    private Spinner levelSpinner;
    private Spinner explanationLanguageSpinner;
    private SeekBar voiceSpeedSeekBar;
    private SeekBar silenceDelaySeekBar;
    private TextView silenceDelayText;
    private Spinner micSensitivitySpinner;
    private Spinner modelProfileSpinner;
    private CheckBox readCorrectionCheckBox;
    private Button clearHistoryButton;
    private Button vocabularyButton;
    private Button backButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        prefManager = PrefManager.getInstance(this);
        dbHelper = DatabaseHelper.getInstance(this);
        anaSpeaker = AnaSpeaker.getInstance(this);
        
        initViews();
        loadSettings();
        setupListeners();
    }
    
    private void initViews() {
        levelSpinner = findViewById(R.id.levelSpinner);
        explanationLanguageSpinner = findViewById(R.id.explanationLanguageSpinner);
        voiceSpeedSeekBar = findViewById(R.id.voiceSpeedSeekBar);
        silenceDelaySeekBar = findViewById(R.id.silenceDelaySeekBar);
        silenceDelayText = findViewById(R.id.silenceDelayText);
        micSensitivitySpinner = findViewById(R.id.micSensitivitySpinner);
        modelProfileSpinner = findViewById(R.id.modelProfileSpinner);
        readCorrectionCheckBox = findViewById(R.id.readCorrectionCheckBox);
        clearHistoryButton = findViewById(R.id.clearHistoryButton);
        vocabularyButton = findViewById(R.id.vocabularyButton);
        backButton = findViewById(R.id.backButton);
    }
    
    private void loadSettings() {
        // Niveau
        ArrayAdapter<CharSequence> levelAdapter = ArrayAdapter.createFromResource(this,
                R.array.levels_array, android.R.layout.simple_spinner_item);
        levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        levelSpinner.setAdapter(levelAdapter);
        
        String level = prefManager.getLevel();
        int levelIndex = getLevelIndex(level);
        levelSpinner.setSelection(levelIndex);
        
        // Langue d'explication
        ArrayAdapter<CharSequence> langAdapter = ArrayAdapter.createFromResource(this,
                R.array.explanation_languages_array, android.R.layout.simple_spinner_item);
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        explanationLanguageSpinner.setAdapter(langAdapter);
        
        String lang = prefManager.getExplanationLanguage();
        int langIndex = getLanguageIndex(lang);
        explanationLanguageSpinner.setSelection(langIndex);
        
        // Vitesse de voix
        float speed = prefManager.getVoiceSpeed();
        voiceSpeedSeekBar.setProgress((int) (speed * 10));
        
        // Délai de silence
        int delay = prefManager.getSilenceDelay();
        silenceDelaySeekBar.setProgress(delay - 1);
        silenceDelayText.setText(getString(R.string.seconds, delay));
        
        // Sensibilité du micro
        ArrayAdapter<CharSequence> sensitivityAdapter = ArrayAdapter.createFromResource(this,
                R.array.sensitivity_array, android.R.layout.simple_spinner_item);
        sensitivityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        micSensitivitySpinner.setAdapter(sensitivityAdapter);
        
        int sensitivity = prefManager.getMicSensitivity();
        micSensitivitySpinner.setSelection(sensitivity);
        
        // Profil de modèle
        ArrayAdapter<CharSequence> profileAdapter = ArrayAdapter.createFromResource(this,
                R.array.model_profiles_array, android.R.layout.simple_spinner_item);
        profileAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelProfileSpinner.setAdapter(profileAdapter);
        
        String profile = prefManager.getModelProfile();
        if ("auto".equals(profile)) {
            profile = AudioUtils.getRecommendedModelProfile(this);
        }
        int profileIndex = getProfileIndex(profile);
        modelProfileSpinner.setSelection(profileIndex);
        
        // Lire la correction
        readCorrectionCheckBox.setChecked(prefManager.shouldReadCorrection());
    }
    
    private int getLevelIndex(String level) {
        String[] levels = getResources().getStringArray(R.array.levels_array);
        for (int i = 0; i < levels.length; i++) {
            if (levels[i].equalsIgnoreCase(level)) {
                return i;
            }
        }
        return 1; // A2 par défaut
    }
    
    private int getLanguageIndex(String lang) {
        String[] langs = getResources().getStringArray(R.array.explanation_languages_array);
        for (int i = 0; i < langs.length; i++) {
            if (langs[i].equalsIgnoreCase(lang)) {
                return i;
            }
        }
        return 0; // Français par défaut
    }
    
    private int getProfileIndex(String profile) {
        String[] profiles = getResources().getStringArray(R.array.model_profiles_array);
        for (int i = 0; i < profiles.length; i++) {
            if (profiles[i].toLowerCase().contains(profile.replace("_", " "))) {
                return i;
            }
        }
        return 0;
    }
    
    private void setupListeners() {
        // Niveau
        levelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] levels = getResources().getStringArray(R.array.levels_array);
                prefManager.setLevel(levels[position]);
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        // Langue d'explication
        explanationLanguageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] langs = getResources().getStringArray(R.array.explanation_languages_array);
                String lang = "Français".equals(langs[position]) ? "fr" : 
                             "Anglais simple".equals(langs[position]) ? "en" : "none";
                prefManager.setExplanationLanguage(lang);
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        // Vitesse de voix
        voiceSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float speed = progress / 10.0f;
                prefManager.setVoiceSpeed(speed);
                anaSpeaker.setSpeechRate(speed);
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Délai de silence
        silenceDelaySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int delay = progress + 1;
                prefManager.setSilenceDelay(delay);
                silenceDelayText.setText(getString(R.string.seconds, delay));
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Sensibilité du micro
        micSensitivitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefManager.setMicSensitivity(position);
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        // Profil de modèle
        modelProfileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] profiles = getResources().getStringArray(R.array.model_profiles_array);
                String profile = profiles[position].toLowerCase().replace(" ", "_");
                prefManager.setModelProfile(profile);
                Toast.makeText(SettingsActivity.this, 
                    "Le nouveau profil sera appliqué au prochain redémarrage", 
                    Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        // Lire la correction
        readCorrectionCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefManager.setReadCorrection(isChecked);
        });
        
        // Effacer l'historique
        clearHistoryButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle(R.string.clear_history)
                .setMessage(R.string.clear_history_confirm)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    dbHelper.clearMessages();
                    Toast.makeText(this, "Historique effacé", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.no, null)
                .show();
        });
        
        // Carnet de vocabulaire
        vocabularyButton.setOnClickListener(v -> {
            showVocabularyDialog();
        });
        
        // Retour
        backButton.setOnClickListener(v -> finish());
    }
    
    private void showVocabularyDialog() {
        VocabularyDao vocabDao = new VocabularyDao(this);
        List<VocabularyItem> items = vocabDao.getAll();
        
        if (items.isEmpty()) {
            Toast.makeText(this, R.string.no_vocabulary, Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.vocabulary);
        
        StringBuilder text = new StringBuilder();
        for (VocabularyItem item : items) {
            text.append("• ").append(item.getEnglish()).append(" = ").append(item.getFrench()).append("\n");
        }
        
        builder.setMessage(text.toString());
        builder.setPositiveButton("OK", null);
        builder.setNegativeButton("Effacer tout", (dialog, which) -> {
            vocabDao.clearAll();
            Toast.makeText(this, "Carnet effacé", Toast.LENGTH_SHORT).show();
        });
        
        builder.show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
