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

        if (DeviceLicense.isAccepted(this)) {
            openApp();
        }
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
            DeviceLicense.remember(this);
            openApp();
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
