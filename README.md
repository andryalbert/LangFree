# LangFree - Apprenez l'anglais par conversation avec une IA hors ligne

LangFree est une application Android 100% hors ligne qui vous permet d'apprendre l'anglais en conversant avec Ana, une IA amicale. L'application corrige vos fautes grammaticales en temps réel et vous aide à progresser à votre rythme.

## Fonctionnalités principales

- **Conversation vocale** : Parlez en anglais avec Ana, elle vous répond à voix haute
- **Correction instantanée** : Vos fautes sont corrigées immédiatement avec des explications en français
- **100% hors ligne** : Fonctionne sans connexion internet après l'installation des modèles
- **Mode chat écrit** : Vous pouvez aussi écrire vos messages
- **Carnet de vocabulaire** : Sauvegardez les phrases corrigées pour les réviser
- **Niveaux adaptés** : A1, A2, B1, B2 pour adapter les réponses à votre niveau
- **Thèmes de conversation** : Vie quotidienne, restaurant, voyage, travail, entretien d'embauche

## Configuration requise

- **Android 5.0 (API 21) ou supérieur**
- **Architecture** : ARM 32 bits (armeabi-v7a) ou ARM 64 bits (arm64-v8a)
- **Espace disque** : Environ 150 à 450 Mo pour les modèles (selon le profil)
- **RAM** : Fonctionne sur tous les téléphones, profil adapté automatiquement

## Compilation de l'APK

### Avec Android Studio

1. Clonez ce dépôt :
   ```bash
   git clone https://github.com/votre-username/LangFree.git
   cd LangFree
   ```

2. Ouvrez le projet dans Android Studio

3. Synchronisez Gradle (File > Sync Project with Gradle Files)

4. Cliquez sur Build > Build Bundle(s) / APK(s) > Build APK(s)

5. L'APK sera généré dans `app/build/outputs/apk/debug/app-debug.apk`

### Avec GitHub Actions

1. Poussez votre code sur GitHub

2. Allez dans l'onglet "Actions" de votre dépôt

3. Lancez le workflow "Build Android APK"

4. Téléchargez l'APK depuis les artefacts

### Avec la ligne de commande (Linux/Mac)

```bash
./gradlew assembleDebug
```

L'APK sera dans `app/build/outputs/apk/debug/app-debug.apk`

## Installation de l'APK

### Sur Android

1. Activez l'installation depuis des sources inconnues dans les réglages de sécurité

2. Transférez l'APK sur votre téléphone

3. Ouvrez le fichier APK et installez-le

4. Au premier lancement, l'application vous demandera d'installer les modèles (nécessite internet)

5. Une fois les modèles installés, l'application fonctionne 100% hors ligne

## Résolution de problèmes courants

### L'application ne se lance pas

- Vérifiez que vous avez Android 5.0 ou supérieur
- Assurez-vous d'avoir suffisamment d'espace disque (au moins 500 Mo)
- Réinstallez l'APK

### Le micro ne fonctionne pas

- Vérifiez que l'application a la permission d'utiliser le micro
- Allez dans Réglages > Applications > LangFree > Permissions > Micro
- Si le problème persiste, vérifiez que votre micro fonctionne avec d'autres applications

### Ana ne parle pas

- Vérifiez que votre téléphone a une voix anglaise installée
- Allez dans Réglages > Langue et saisie > Synthèse vocale
- Installez les données vocales anglaises si nécessaire
- Dans l'application, vous pouvez ajuster la vitesse de la voix dans les réglages

### L'application est lente

- Le profil "Ultra rapide" est automatiquement sélectionné sur les téléphones avec moins de 3 Go de RAM
- Vous pouvez forcer ce profil dans les réglages
- Ferrez d'autres applications en arrière-plan pour libérer de la mémoire

### Erreur lors du téléchargement des modèles

- Vérifiez votre connexion internet
- Assurez-vous d'avoir suffisamment d'espace disque
- Si le téléchargement échoue, réessayez (l'application supporte la reprise)

## Architecture technique

### Composants principaux

- **Reconnaissance vocale** : Vosk (modèle small anglais, ~40 Mo)
- **Correction grammaticale** : Moteur de règles Java avec 60+ règles
- **IA de conversation** : llama.cpp avec SmolLM2-135M ou Qwen2.5-0.5B (GGUF)
- **Synthèse vocale** : TextToSpeech d'Android

### Structure du projet

```
LangFree/
├── app/
│   ├── src/main/
│   │   ├── java/com/langfree/
│   │   │   ├── MainActivity.java
│   │   │   ├── InstallationActivity.java
│   │   │   ├── SettingsActivity.java
│   │   │   ├── ai/ (LlamaEngine, ModelManager)
│   │   │   ├── grammar/ (GrammarEngine, GrammarRule)
│   │   │   ├── speech/ (VoskRecognizer, VoiceActivityDetector, AnaSpeaker)
│   │   │   ├── database/ (DatabaseHelper, VocabularyDao)
│   │   │   ├── ui/ (MessageAdapter, VocabularyAdapter)
│   │   │   ├── utils/ (PrefManager, AudioUtils, NetworkUtils)
│   │   │   └── model/ (Message, Correction, VocabularyItem)
│   │   ├── cpp/ (llama_wrapper.cpp pour JNI)
│   │   └── res/ (layouts, strings, assets/grammar_rules.json)
│   └── build.gradle
├── .github/workflows/build.yml
└── README.md
```

## Liens de téléchargement des modèles

### Modèle de reconnaissance vocale (Vosk)
- **URL** : https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip
- **Taille** : ~40 Mo
- **Note** : Ce lien est fourni par le projet Vosk officiel

### Modèle IA - Profil Ultra rapide (SmolLM2-135M)
- **URL** : https://huggingface.co/mradermacher/SmolLM2-135M-Instruct-GGUF/resolve/main/SmolLM2-135M-Instruct-Q4_K_M.gguf
- **Taille** : ~105 Mo
- **Note** : Hébergé sur Hugging Face (dépôt mradermacher), quantisation Q4_K_M recommandée

### Modèle IA - Profil Rapide (Qwen2.5-0.5B)
- **URL** : https://huggingface.co/bartowski/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/Qwen2.5-0.5B-Instruct-Q4_K_M.gguf
- **Taille** : ~400 Mo
- **Note** : Hébergé sur Hugging Face (dépôt bartowski), quantisation Q4_K_M recommandée

> **Important** : Les liens des modèles IA sont fournis à titre indicatif. L'application télécharge automatiquement le bon profil selon votre téléphone. Si un lien ne fonctionne plus, consultez les dépôts Hugging Face correspondants pour trouver les versions les plus récentes.

## Personnalisation

### Ajouter des règles de grammaire

Les règles de grammaire sont dans `app/src/main/assets/grammar_rules.json`. Vous pouvez ajouter vos propres règles en suivant ce format :

```json
{
  "pattern": "\\bma faute\\b",
  "replacement": "ma correction",
  "explanation_fr": "Explication en français",
  "explanation_en_simple": "Simple explanation in English"
}
```

### Changer le prompt système

Le prompt d'Ana est dans `ModelManager.java`, méthode `getSystemPrompt()`. Vous pouvez le modifier pour changer la personnalité d'Ana.

## Licence

Ce projet est fourni à des fins éducatives. Les modèles IA ont leurs propres licences respectives.

## Support

Pour les problèmes ou questions :
- Ouvrez une issue sur GitHub
- Consultez la section "Résolution de problèmes courants" ci-dessus

## Remerciements

- Vosk pour la reconnaissance vocale hors ligne
- llama.cpp pour l'inférence de modèles de langage
- Hugging Face pour les modèles de langage
- La communauté open source Android
