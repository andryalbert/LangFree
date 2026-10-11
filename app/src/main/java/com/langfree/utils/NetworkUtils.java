package com.langfree.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class NetworkUtils {
    
    public interface DownloadProgressListener {
        void onProgress(int percent);
        void onComplete(String filePath);
        void onError(String error);
    }
    
    /**
     * Vérifie si la connexion internet est disponible
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
        return false;
    }
    
    /**
     * Télécharge un fichier avec support de la reprise
     */
    public static void downloadFile(String urlStr, String destPath, long existingFileSize,
                                     DownloadProgressListener listener) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            InputStream input = null;
            FileOutputStream output = null;
            
            try {
                URL url = new URL(urlStr);
                connection = (HttpURLConnection) url.openConnection();
                
                // Support de la reprise
                if (existingFileSize > 0) {
                    connection.setRequestProperty("Range", "bytes=" + existingFileSize + "-");
                }
                
                connection.connect();
                
                // Vérifier le code de réponse
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                    if (listener != null) {
                        listener.onError("Server returned code: " + responseCode);
                    }
                    return;
                }
                
                // Obtenir la taille totale du fichier
                long fileSize = connection.getContentLength();
                if (existingFileSize > 0) {
                    fileSize += existingFileSize;
                }
                
                File destFile = new File(destPath);
                destFile.getParentFile().mkdirs();
                
                boolean append = existingFileSize > 0;
                output = new FileOutputStream(destFile, append);
                input = connection.getInputStream();
                
                byte[] buffer = new byte[8192];
                long totalBytesRead = existingFileSize;
                int bytesRead;
                
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    
                    // Calculer le pourcentage
                    if (fileSize > 0 && listener != null) {
                        int percent = (int) ((totalBytesRead * 100) / fileSize);
                        listener.onProgress(percent);
                    }
                }
                
                if (listener != null) {
                    listener.onComplete(destPath);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                if (listener != null) {
                    listener.onError("Download failed: " + e.getMessage());
                }
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
     * Vérifie l'espace disque disponible
     */
    public static boolean hasEnoughSpace(Context context, long requiredBytes) {
        File filesDir = context.getFilesDir();
        long availableBytes = filesDir.getFreeSpace();
        return availableBytes >= requiredBytes;
    }
}
