package com.langfree.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copie locale (sélecteur de fichiers) ou téléchargement unique du modèle Gemma.
 * Le jeton n'est envoyé qu'à huggingface.co. Les adresses de téléchargement signées
 * (CDN) refusent la requête si le jeton les accompagne.
 */
public final class ModelDownloader {
    public enum Reason {
        NO_NETWORK,
        AUTH,
        TOO_SMALL,
        NO_SPACE,
        FAILED
    }

    public static class TransferException extends Exception {
        public final Reason reason;

        public TransferException(Reason reason, String detail) {
            super(detail);
            this.reason = reason;
        }
    }

    public interface Progress {
        void onProgress(int percent, long bytesCopied, long totalBytes);
    }

    private static final long MAX_BYTES = 2_500_000_000L;
    private static final int CONNECT_TIMEOUT_MS = 20_000;
    private static final int READ_TIMEOUT_MS = 60_000;
    private static final Pattern TOKEN_PATTERN = Pattern.compile("hf_[A-Za-z0-9]{8,}");

    private ModelDownloader() {
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        Network network = manager.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    public static void importFromUri(Context context, Uri uri, Progress progress) throws TransferException {
        long length = queryLength(context, uri);
        ensureSpace(context, length > 0 ? length : ModelStorage.MIN_BYTES);
        File partial = ModelStorage.partialFile(context);
        try {
            try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                if (input == null) {
                    throw new TransferException(Reason.FAILED, "flux nul");
                }
                copy(input, partial, length, progress);
            }
            validateAndInstall(context, partial);
        } catch (TransferException error) {
            //noinspection ResultOfMethodCallIgnored
            partial.delete();
            throw error;
        } catch (IOException error) {
            //noinspection ResultOfMethodCallIgnored
            partial.delete();
            throw new TransferException(Reason.FAILED, error.getMessage());
        }
    }

    public static void download(Context context, String token, Progress progress) throws TransferException {
        if (!isNetworkAvailable(context)) {
            throw new TransferException(Reason.NO_NETWORK, "hors ligne");
        }
        ensureSpace(context, 700L * 1024L * 1024L);
        String accessToken = normalizeToken(token);
        if (accessToken.isEmpty()) {
            throw new TransferException(Reason.AUTH, "jeton vide");
        }
        File partial = ModelStorage.partialFile(context);
        HttpURLConnection connection = null;
        try {
            connection = openFollowingRedirects(ModelStorage.DOWNLOAD_URL, accessToken);
            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN) {
                throw new TransferException(Reason.AUTH, "HTTP " + code);
            }
            if (code != HttpURLConnection.HTTP_OK) {
                throw new TransferException(Reason.FAILED, "HTTP " + code);
            }
            long length = connection.getContentLengthLong();
            try (InputStream input = connection.getInputStream()) {
                copy(input, partial, length, progress);
            }
            validateAndInstall(context, partial);
        } catch (TransferException error) {
            //noinspection ResultOfMethodCallIgnored
            partial.delete();
            throw error;
        } catch (IOException error) {
            //noinspection ResultOfMethodCallIgnored
            partial.delete();
            throw new TransferException(Reason.FAILED, error.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static void validateAndInstall(Context context, File partial) throws TransferException {
        if (partial.length() < ModelStorage.MIN_BYTES) {
            throw new TransferException(Reason.TOO_SMALL, String.valueOf(partial.length()));
        }
        if (!ModelStorage.install(context, partial)) {
            throw new TransferException(Reason.FAILED, "installation impossible");
        }
        if (!ModelStorage.isReady(context)) {
            throw new TransferException(Reason.FAILED, "modèle illisible");
        }
    }

    private static void copy(InputStream input, File destination, long total, Progress progress)
            throws IOException, TransferException {
        byte[] buffer = new byte[64 * 1024];
        long copied = 0L;
        int lastPercent = -1;
        try (OutputStream output = new FileOutputStream(destination)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
                copied += read;
                if (copied > MAX_BYTES) {
                    throw new TransferException(Reason.FAILED, "fichier trop volumineux");
                }
                int percent = total > 0 ? (int) Math.min(100, (copied * 100L) / total) : -1;
                if (progress != null && percent != lastPercent) {
                    lastPercent = percent;
                    progress.onProgress(percent, copied, total);
                }
            }
            output.flush();
        }
        if (progress != null) {
            progress.onProgress(100, copied, copied);
        }
    }

    private static long queryLength(Context context, Uri uri) {
        try (android.content.res.AssetFileDescriptor descriptor =
                     context.getContentResolver().openAssetFileDescriptor(uri, "r")) {
            if (descriptor == null) {
                return -1L;
            }
            return descriptor.getLength();
        } catch (Exception ignored) {
            return -1L;
        }
    }

    private static void ensureSpace(Context context, long needed) throws TransferException {
        File parent = ModelStorage.modelFile(context).getParentFile();
        long usable = parent == null ? 0L : parent.getUsableSpace();
        if (usable > 0 && usable < needed + (64L * 1024L * 1024L)) {
            throw new TransferException(Reason.NO_SPACE, String.valueOf(usable));
        }
    }

    private static HttpURLConnection openFollowingRedirects(String urlString, String token) throws IOException {
        URL current = new URL(urlString);
        HttpURLConnection connection = null;
        for (int hop = 0; hop < 8; hop++) {
            connection = (HttpURLConnection) current.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "huggingface_hub/0.26.0; langfree/1.0");
            connection.setRequestProperty("Accept", "*/*");
            // Uniquement le site du modèle. cdn-lfs et cas-bridge refusent le jeton (403).
            if (token != null && !token.isEmpty() && isModelHubHost(current.getHost())) {
                connection.setRequestProperty("Authorization", "Bearer " + token);
            }
            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_MOVED_PERM
                    || code == HttpURLConnection.HTTP_MOVED_TEMP
                    || code == HttpURLConnection.HTTP_SEE_OTHER
                    || code == 307
                    || code == 308) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                if (location == null || location.isEmpty()) {
                    throw new IOException("redirection sans adresse");
                }
                current = new URL(current, location);
                continue;
            }
            return connection;
        }
        if (connection != null) {
            connection.disconnect();
        }
        throw new IOException("trop de redirections");
    }

    /** Garde le jeton hf_… même s'il est collé avec « Bearer », des guillemets ou une phrase. */
    static String normalizeToken(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            trimmed = trimmed.substring(7).trim();
        }
        trimmed = trimmed.replace("\"", "").replace("'", "");
        Matcher matcher = TOKEN_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group();
        }
        return trimmed;
    }

    private static boolean isModelHubHost(String host) {
        return "huggingface.co".equalsIgnoreCase(host) || "www.huggingface.co".equalsIgnoreCase(host);
    }
}
