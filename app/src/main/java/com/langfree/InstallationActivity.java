package com.langfree;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.langfree.ai.ModelManager;
import com.langfree.utils.AudioUtils;
import com.langfree.utils.DownloadManager;
import com.langfree.utils.NetworkUtils;
import com.langfree.utils.PrefManager;

import java.io.File;

public class InstallationActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int PICK_FILE_REQUEST_CODE = 101;
    
    private ModelManager modelManager;
    private PrefManager prefManager;
    private DownloadManager downloadManager;
    private ProgressBar progressBar;
    private TextView progressText;
    private TextView statusText;
    private TextView profileInfoText;
    private Button installButton;
    private Button importButton;
    private Button continueButton;

    private boolean isDownloading = false;
    private int currentStep = 0; // 0: Vosk, 1: LLM
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_installation);
        
        modelManager = ModelManager.getInstance(this);
        prefManager = PrefManager.getInstance(this);
        downloadManager = new DownloadManager(this);
        
        // Initialiser les vues
        progressBar = findViewById(R.id.progressBar);
        progressText = findViewById(R.id.progressText);
        statusText = findViewById(R.id.statusText);
        profileInfoText = findViewById(R.id.profileInfoText);
        installButton = findViewById(R.id.installButton);
        importButton = findViewById(R.id.importButton);
        continueButton = findViewById(R.id.continueButton);
        
        // Afficher le profil détecté
        String profile = AudioUtils.getRecommendedModelProfile(this);
        String profileName = "ultra_fast".equals(profile) ? "Ultra rapide" : "Rapide";
        profileInfoText.setText("Profil détecté : " + profileName);
        profileInfoText.setVisibility(View.VISIBLE);
        
        // Bouton installer
        installButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PERMISSION_REQUEST_CODE);
                } else {
                    startDownload();
                }
            } else {
                startDownload();
            }
        });
        
        // Bouton importer
        importButton.setOnClickListener(v -> {
            // Ouvrir le sélecteur de fichiers
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, PICK_FILE_REQUEST_CODE);
        });
        
        // Bouton continuer
        continueButton.setOnClickListener(v -> {
            prefManager.setModelsInstalled(true);
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        
        // Vérifier si les modèles sont déjà installés
        if (prefManager.areModelsInstalled()) {
            showInstallationComplete();
        }
    }
    
    private void startDownload() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "Pas de connexion internet", Toast.LENGTH_LONG).show();
            return;
        }
        
        long totalSize = modelManager.getTotalDownloadSize();
        if (!NetworkUtils.hasEnoughSpace(this, totalSize)) {
            Toast.makeText(this, "Pas assez d'espace libre", Toast.LENGTH_LONG).show();
            return;
        }
        
        isDownloading = true;
        installButton.setEnabled(false);
        importButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        progressText.setVisibility(View.VISIBLE);
        statusText.setVisibility(View.VISIBLE);
        statusText.setText("Vérification de l'espace...");
        
        // Créer le répertoire des modèles
        File modelsDir = modelManager.getModelsDir();
        if (!modelsDir.exists()) {
            modelsDir.mkdirs();
        }
        
        // Télécharger le modèle Vosk
        statusText.setText("Téléchargement du modèle de reconnaissance vocale...");
        downloadVoskModel();
    }
    
    private void downloadVoskModel() {
        currentStep = 0;
        downloadManager.setListener(new DownloadManager.DownloadProgressListener() {
            @Override
            public void onProgress(int percent) {
                progressBar.setProgress(percent / 2); // Vosk = 50% du total
                progressText.setText(getString(R.string.download_progress, percent / 2));
            }

            @Override
            public void onStatusUpdate(String status) {
                statusText.setText(status);
            }

            @Override
            public void onSuccess() {
                downloadLlmModel();
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    isDownloading = false;
                    installButton.setEnabled(true);
                    importButton.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    progressText.setVisibility(View.GONE);
                    Toast.makeText(InstallationActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });

        downloadManager.downloadAndExtractZip(
            modelManager.getVoskModelUrl(),
            modelManager.getVoskModelDir()
        );
    }

    private void downloadLlmModel() {
        currentStep = 1;
        statusText.setText("Téléchargement du modèle IA...");

        downloadManager.setListener(new DownloadManager.DownloadProgressListener() {
            @Override
            public void onProgress(int percent) {
                progressBar.setProgress(50 + percent / 2); // LLM = 50% du total (de 50% à 100%)
                progressText.setText(getString(R.string.download_progress, 50 + percent / 2));
            }

            @Override
            public void onStatusUpdate(String status) {
                statusText.setText(status);
            }

            @Override
            public void onSuccess() {
                runOnUiThread(() -> showInstallationComplete());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    isDownloading = false;
                    installButton.setEnabled(true);
                    importButton.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    progressText.setVisibility(View.GONE);
                    Toast.makeText(InstallationActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });

        File llmDir = modelManager.getLlmModelFile().getParentFile();
        if (!llmDir.exists()) {
            llmDir.mkdirs();
        }

        downloadManager.downloadFile(
            modelManager.getLlmModelUrl(),
            modelManager.getLlmModelFile()
        );
    }
    
    private void showInstallationComplete() {
        isDownloading = false;
        progressBar.setVisibility(View.GONE);
        progressText.setVisibility(View.GONE);
        statusText.setVisibility(View.GONE);
        installButton.setVisibility(View.GONE);
        importButton.setVisibility(View.GONE);
        continueButton.setVisibility(View.VISIBLE);
        
        Toast.makeText(this, "Installation terminée !", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startDownload();
            } else {
                Toast.makeText(this, "Permission refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                // Gérer l'import de fichier (à implémenter)
                Toast.makeText(this, "Import non implémenté dans cette version", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public void onBackPressed() {
        if (isDownloading) {
            Toast.makeText(this, "Téléchargement en cours...", Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }
}
