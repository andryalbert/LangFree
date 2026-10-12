package com.langfree.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DownloadManager {
    private static final String TAG = "DownloadManager";

    public interface DownloadProgressListener {
        void onProgress(int percent);
        void onStatusUpdate(String status);
        void onSuccess();
        void onError(String error);
    }

    private Context context;
    private DownloadProgressListener listener;
    private Handler mainHandler;
    private boolean isCancelled = false;

    public DownloadManager(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void setListener(DownloadProgressListener listener) {
        this.listener = listener;
    }

    public void cancel() {
        isCancelled = true;
    }

    /**
     * Télécharge un fichier depuis une URL
     */
    public void downloadFile(String urlString, File outputFile) {
        Log.d(TAG, "downloadFile called: " + urlString);
        new Thread(() -> {
            HttpURLConnection connection = null;
            InputStream input = null;
            OutputStream output = null;

            try {
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                Log.d(TAG, "Response code: " + connection.getResponseCode());
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    notifyError("Server returned HTTP " + connection.getResponseCode());
                    return;
                }

                int fileLength = connection.getContentLength();
                Log.d(TAG, "File length: " + fileLength);
                input = connection.getInputStream();
                output = new FileOutputStream(outputFile);

                byte[] data = new byte[4096];
                long total = 0;
                int count;
                int lastPercent = 0;

                while ((count = input.read(data)) != -1) {
                    if (isCancelled) {
                        notifyError("Download cancelled");
                        return;
                    }

                    total += count;
                    output.write(data, 0, count);

                    if (fileLength > 0) {
                        int percent = (int) (total * 100 / fileLength);
                        if (percent != lastPercent) {
                            notifyProgress(percent);
                            lastPercent = percent;
                        }
                    }
                }

                output.flush();
                Log.d(TAG, "Download completed successfully");
                notifySuccess();

            } catch (Exception e) {
                Log.e(TAG, "Download failed", e);
                notifyError("Download failed: " + e.getMessage());
            } finally {
                try {
                    if (output != null) output.close();
                    if (input != null) input.close();
                    if (connection != null) connection.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Télécharge et extrait un fichier ZIP
     */
    public void downloadAndExtractZip(String urlString, File outputDir) {
        Log.d(TAG, "downloadAndExtractZip called: " + urlString + " to " + outputDir.getAbsolutePath());
        new Thread(() -> {
            File tempFile = new File(outputDir.getParent(), "temp_download.zip");

            try {
                notifyStatusUpdate("Téléchargement en cours...");
                downloadFileSync(urlString, tempFile);

                if (isCancelled) return;

                notifyStatusUpdate("Extraction en cours...");
                extractZip(tempFile, outputDir);

                if (isCancelled) return;

                // Supprimer le fichier temporaire
                if (tempFile.exists()) {
                    tempFile.delete();
                }

                Log.d(TAG, "Download and extract completed successfully");
                notifySuccess();

            } catch (Exception e) {
                Log.e(TAG, "Download and extract failed", e);
                notifyError("Extraction failed: " + e.getMessage());
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }
        }).start();
    }

    /**
     * Téléchargement synchrone (pour usage interne)
     */
    private void downloadFileSync(String urlString, File outputFile) throws Exception {
        HttpURLConnection connection = null;
        InputStream input = null;
        OutputStream output = null;

        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Server returned HTTP " + connection.getResponseCode());
            }

            int fileLength = connection.getContentLength();
            input = connection.getInputStream();
            output = new FileOutputStream(outputFile);

            byte[] data = new byte[4096];
            long total = 0;
            int count;
            int lastPercent = 0;

            while ((count = input.read(data)) != -1) {
                if (isCancelled) {
                    throw new IOException("Download cancelled");
                }

                total += count;
                output.write(data, 0, count);

                if (fileLength > 0) {
                    int percent = (int) (total * 100 / fileLength);
                    if (percent != lastPercent) {
                        notifyProgress(percent);
                        lastPercent = percent;
                    }
                }
            }

            output.flush();

        } finally {
            try {
                if (output != null) output.close();
                if (input != null) input.close();
                if (connection != null) connection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Extrait un fichier ZIP
     */
    private void extractZip(File zipFile, File outputDir) throws Exception {
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        ZipInputStream zipInput = new ZipInputStream(new java.io.FileInputStream(zipFile));
        ZipEntry entry;

        byte[] buffer = new byte[4096];
        int totalEntries = 0;
        int extractedEntries = 0;
        String rootDir = null;

        // Compter le nombre total d'entrées et trouver le répertoire racine
        ZipInputStream countInput = new ZipInputStream(new java.io.FileInputStream(zipFile));
        ZipEntry countEntry;
        while ((countEntry = countInput.getNextEntry()) != null) {
            totalEntries++;
            // Déterminer le répertoire racine (premier niveau de répertoire)
            if (rootDir == null && !countEntry.isDirectory()) {
                String[] parts = countEntry.getName().split("/");
                if (parts.length > 1) {
                    rootDir = parts[0];
                }
            }
            countInput.closeEntry();
        }
        countInput.close();

        Log.d(TAG, "Total entries: " + totalEntries + ", Root dir: " + rootDir);

        while ((entry = zipInput.getNextEntry()) != null) {
            if (isCancelled) {
                zipInput.closeEntry();
                throw new IOException("Extraction cancelled");
            }

            String entryName = entry.getName();
            // Supprimer le répertoire racine si présent
            if (rootDir != null && entryName.startsWith(rootDir + "/")) {
                entryName = entryName.substring(rootDir.length() + 1);
            }
            
            // Ignorer les entrées vides après suppression du root
            if (entryName.isEmpty()) {
                zipInput.closeEntry();
                continue;
            }

            File outputFile = new File(outputDir, entryName);

            if (entry.isDirectory()) {
                outputFile.mkdirs();
            } else {
                // Créer les répertoires parents si nécessaire
                if (outputFile.getParentFile() != null) {
                    outputFile.getParentFile().mkdirs();
                }

                FileOutputStream output = new FileOutputStream(outputFile);
                int count;
                while ((count = zipInput.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
                output.close();
            }

            extractedEntries++;
            int percent = (int) (extractedEntries * 100 / totalEntries);
            notifyProgress(percent);

            zipInput.closeEntry();
        }

        zipInput.close();
        Log.d(TAG, "Extraction completed. Extracted " + extractedEntries + " entries to " + outputDir.getAbsolutePath());
    }

    private void notifyProgress(int percent) {
        if (listener != null) {
            mainHandler.post(() -> listener.onProgress(percent));
        }
    }

    private void notifyStatusUpdate(String status) {
        if (listener != null) {
            mainHandler.post(() -> listener.onStatusUpdate(status));
        }
    }

    private void notifySuccess() {
        if (listener != null) {
            mainHandler.post(() -> listener.onSuccess());
        }
    }

    private void notifyError(String error) {
        if (listener != null) {
            mainHandler.post(() -> listener.onError(error));
        }
    }
}
