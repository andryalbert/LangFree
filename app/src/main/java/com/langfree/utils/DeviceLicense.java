package com.langfree.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * Autorise un seul téléphone : son code doit figurer dans licenses.json sur GitHub.
 * L'application ne possède aucun droit d'écriture sur le dépôt.
 */
public final class DeviceLicense {
    public static final String LIST_URL =
            "https://raw.githubusercontent.com/andryalbert/LangFree/main/licenses.json";

    private static final String PREFS = "langfree_license";
    private static final String KEY_CODE = "accepted_code";
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;

    public interface Result {
        void onResult(boolean allowed, boolean reachable);
    }

    private DeviceLicense() {
    }

    public static String deviceCode(Context context) {
        String androidId = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId == null || androidId.isEmpty()) {
            androidId = "missing";
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(("com.langfree:" + androidId).getBytes(StandardCharsets.UTF_8));
            StringBuilder code = new StringBuilder();
            for (int i = 0; i < CODE_LENGTH; i++) {
                if (i == 4) {
                    code.append('-');
                }
                code.append(ALPHABET.charAt(digest[i] & 31));
            }
            return code.toString();
        } catch (Exception exception) {
            return "XXXX-XXXX";
        }
    }

    public static boolean isAccepted(Context context) {
        String saved = prefs(context).getString(KEY_CODE, "");
        return deviceCode(context).equals(saved);
    }

    public static void remember(Context context) {
        prefs(context).edit().putString(KEY_CODE, deviceCode(context)).apply();
    }

    public static void clear(Context context) {
        prefs(context).edit().remove(KEY_CODE).apply();
    }

    /** @return vrai si le code de cet appareil est dans la liste. */
    public static boolean listedOnline(Context context) throws Exception {
        String body = fetchList();
        if (body.isEmpty()) {
            return false;
        }
        String expected = normalize(deviceCode(context));
        JSONArray codes = codesOf(body);
        for (int i = 0; i < codes.length(); i++) {
            if (expected.equals(normalize(codes.optString(i)))) {
                return true;
            }
        }
        return false;
    }

    public static void checkAsync(Context context, Result result) {
        Context app = context.getApplicationContext();
        Thread thread = new Thread(() -> {
            try {
                result.onResult(listedOnline(app), true);
            } catch (Exception exception) {
                result.onResult(false, false);
            }
        }, "langfree-license");
        thread.start();
    }

    private static JSONArray codesOf(String body) throws Exception {
        String trimmed = body.trim();
        if (trimmed.startsWith("[")) {
            return new JSONArray(trimmed);
        }
        JSONObject root = new JSONObject(trimmed);
        return root.optJSONArray("codes") == null ? new JSONArray() : root.getJSONArray("codes");
    }

    private static String fetchList() throws Exception {
        URL url = new URL(LIST_URL + "?t=" + System.currentTimeMillis());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "LangFree/1.0");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Accept", "application/json");
        try {
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_NOT_FOUND) {
                return "";
            }
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("HTTP " + status);
            }
            return read(connection.getInputStream());
        } finally {
            connection.disconnect();
        }
    }

    private static String read(InputStream input) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int count;
        while ((count = input.read(buffer)) >= 0) {
            out.write(buffer, 0, count);
        }
        input.close();
        return out.toString(StandardCharsets.UTF_8.name());
    }

    static String normalize(String code) {
        if (code == null) {
            return "";
        }
        return code.toUpperCase(Locale.ROOT).replaceAll("[^A-Z2-9]", "");
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
