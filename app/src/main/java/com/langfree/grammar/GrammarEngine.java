package com.langfree.grammar;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.langfree.model.Correction;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GrammarEngine {
    private static GrammarEngine instance;
    private List<GrammarRule> rules;
    private Context context;
    
    private GrammarEngine(Context context) {
        this.context = context.getApplicationContext();
        loadRules();
    }
    
    public static synchronized GrammarEngine getInstance(Context context) {
        if (instance == null) {
            instance = new GrammarEngine(context);
        }
        return instance;
    }
    
    private void loadRules() {
        try {
            InputStream is = context.getAssets().open("grammar_rules.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String json = new String(buffer, "UTF-8");
            Gson gson = new Gson();
            Type listType = new TypeToken<List<GrammarRule>>(){}.getType();
            // Parse the JSON object and extract the "rules" array
            com.google.gson.JsonObject jsonObject = gson.fromJson(json, com.google.gson.JsonObject.class);
            rules = gson.fromJson(jsonObject.get("rules"), listType);
        } catch (IOException e) {
            e.printStackTrace();
            rules = new ArrayList<>();
        }
    }
    
    /**
     * Corrige une phrase en appliquant les règles de grammaire
     * @param text Le texte à corriger
     * @param explanationLanguage "fr" pour français, "en" pour anglais simple, "none" pour aucune
     * @return L'objet Correction avec le résultat
     */
    public Correction correct(String text, String explanationLanguage) {
        if (text == null || text.trim().isEmpty()) {
            return new Correction(text, text, "", new ArrayList<Integer>(), false);
        }
        
        String correctedText = text;
        List<Integer> errorIndices = new ArrayList<>();
        String explanation = "";
        boolean hasErrors = false;
        
        // Appliquer chaque règle
        for (GrammarRule rule : rules) {
            try {
                Pattern pattern = Pattern.compile(rule.getPattern(), Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(correctedText);
                
                if (matcher.find()) {
                    // Remplacer le texte
                    correctedText = matcher.replaceAll(rule.getReplacement());
                    hasErrors = true;
                    
                    // Ajouter l'explication (première règle qui matche)
                    if (explanation.isEmpty()) {
                        if ("fr".equals(explanationLanguage)) {
                            explanation = rule.getExplanationFr();
                        } else if ("en".equals(explanationLanguage)) {
                            explanation = rule.getExplanationEnSimple();
                        }
                    }
                }
            } catch (Exception e) {
                // Ignorer les règles invalides
            }
        }
        
        // Si aucune erreur trouvée, afficher "Bien joué !"
        if (!hasErrors) {
            explanation = "Bien joué !";
        }
        
        // Trouver les indices des différences (simplifié)
        if (hasErrors) {
            errorIndices = findErrorIndices(text, correctedText);
        }
        
        return new Correction(text, correctedText, explanation, errorIndices, hasErrors);
    }
    
    /**
     * Trouve les positions des erreurs dans le texte original
     * Version simplifiée pour la performance
     */
    private List<Integer> findErrorIndices(String original, String corrected) {
        List<Integer> indices = new ArrayList<>();
        // Version simplifiée : on ne marque pas précisément les erreurs
        // Pour une implémentation plus précise, il faudrait un algorithme de diff
        return indices;
    }
    
    /**
     * Filtre les règles selon le niveau (A1, A2, B1, B2)
     * Pour l'instant, toutes les règles sont actives
     */
    public void filterByLevel(String level) {
        // Pour l'instant, on garde toutes les règles
        // On pourrait ajouter un champ "level" dans le JSON et filtrer ici
    }
}
