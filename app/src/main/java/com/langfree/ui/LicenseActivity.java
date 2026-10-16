package com.langfree.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.langfree.R;
import com.langfree.databinding.ActivityLicenseBinding;
import com.langfree.utils.DeviceLicense;

/** Premier écran : ce téléphone doit être inscrit dans la liste GitHub. */
public class LicenseActivity extends AppCompatActivity {
    private ActivityLicenseBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLicenseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.code.setText(DeviceLicense.deviceCode(this));
        binding.copy.setOnClickListener(v -> copyCode());
        binding.verify.setOnClickListener(v -> verify());
        setBusy(true);
        binding.form.setVisibility(View.GONE);
        Thread thread = new Thread(() -> {
            boolean stored = DeviceLicense.isStored(this);
            runOnUiThread(() -> {
                if (isFinishing()) {
                    return;
                }
                if (stored) {
                    openApp();
                    return;
                }
                setBusy(false);
                binding.form.setVisibility(View.VISIBLE);
            });
        }, "langfree-license-local");
        thread.start();
    }

    private void copyCode() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("code", binding.code.getText()));
        }
        Snackbar.make(binding.getRoot(), R.string.license_copied, Snackbar.LENGTH_SHORT).show();
    }

    private void verify() {
        setBusy(true);
        showMessage(0);
        DeviceLicense.checkAsync(this, (allowed, reachable) -> runOnUiThread(() -> {
            if (isFinishing()) {
                return;
            }
            setBusy(false);
            if (!reachable) {
                showMessage(R.string.license_offline);
                return;
            }
            if (!allowed) {
                showMessage(R.string.license_denied);
                return;
            }
            Thread thread = new Thread(() -> {
                DeviceLicense.remember(LicenseActivity.this);
                runOnUiThread(() -> {
                    if (!isFinishing()) {
                        openApp();
                    }
                });
            }, "langfree-license-save");
            thread.start();
        }));
    }

    private void openApp() {
        startActivity(new Intent(this, SetupActivity.class));
        finish();
    }

    private void setBusy(boolean busy) {
        binding.progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.verify.setEnabled(!busy);
        binding.copy.setEnabled(!busy);
    }

    private void showMessage(int text) {
        if (text == 0) {
            binding.message.setVisibility(View.GONE);
            return;
        }
        binding.message.setText(text);
        binding.message.setVisibility(View.VISIBLE);
    }
}
