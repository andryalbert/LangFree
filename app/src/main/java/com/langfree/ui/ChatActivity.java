package com.langfree.ui;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.langfree.R;
import com.langfree.data.SettingsRepository;
import com.langfree.databinding.ActivityChatBinding;
import com.langfree.model.Conversation;
import com.langfree.model.CorrectionMode;
import com.langfree.model.Level;
import com.langfree.model.Topic;
import com.langfree.utils.SpeechInput;
import com.langfree.utils.SpeechOutput;
import com.langfree.viewmodel.ChatViewModel;

/**
 * Discussion : saisie, micro hors ligne, streaming, puis bulle en trois parties.
 */
public class ChatActivity extends AppCompatActivity {
    public static final String EXTRA_CONVERSATION_ID = "conversation_id";

    private ActivityChatBinding binding;
    private ChatViewModel viewModel;
    private MessageAdapter adapter;
    private SpeechInput speechInput;
    private SpeechOutput speechOutput;
    private boolean englishVoiceReady;
    private boolean speechPackNoticeShown;

    private final ActivityResultLauncher<String> micPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (Boolean.TRUE.equals(granted)) {
                    startListening();
                } else {
                    Snackbar.make(binding.getRoot(), R.string.error_mic_permission, Snackbar.LENGTH_LONG)
                            .setAction(R.string.open_settings, v -> {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.fromParts("package", getPackageName(), null));
                                startActivity(intent);
                            })
                            .show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new MessageAdapter(this::speak);
        binding.messages.setLayoutManager(new LinearLayoutManager(this));
        binding.messages.setAdapter(adapter);

        speechInput = new SpeechInput();
        speechOutput = new SpeechOutput();
        speechOutput.init(this, available -> englishVoiceReady = available);

        long restored = savedInstanceState != null
                ? savedInstanceState.getLong(EXTRA_CONVERSATION_ID, -1L) : -1L;
        long requested = restored > 0
                ? restored
                : getIntent().getLongExtra(EXTRA_CONVERSATION_ID, -1L);
        viewModel.open(requested);

        viewModel.messages.observe(this, messages -> {
            adapter.setMessages(messages);
            boolean empty = messages == null || messages.isEmpty();
            binding.empty.setVisibility(empty && !Boolean.TRUE.equals(viewModel.generating.getValue())
                    ? View.VISIBLE : View.GONE);
            scrollToEnd();
        });
        viewModel.conversation.observe(this, this::showTitle);
        viewModel.streamingText.observe(this, text -> {
            adapter.setStreaming(text);
            refreshTyping();
            scrollToEnd();
        });
        viewModel.generating.observe(this, generating -> {
            boolean busy = Boolean.TRUE.equals(generating);
            refreshTyping();
            binding.send.setEnabled(!busy && hasDraft());
            binding.mic.setEnabled(!busy);
            if (busy) {
                binding.empty.setVisibility(View.GONE);
            }
        });
        viewModel.textSizeSp.observe(this, size -> {
            if (size != null) {
                adapter.setTextSizeSp(size);
                binding.input.setTextSize(size);
            }
        });
        viewModel.modelPhase.observe(this, this::showModelPhase);
        viewModel.modelMessage.observe(this, text -> {
            if (text != null) {
                binding.loadingText.setText(text);
            }
        });
        viewModel.error.observe(this, signal -> {
            if (signal != null && !signal.message.isEmpty()) {
                snack(signal.message, false);
            }
        });

        binding.input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.send.setEnabled(hasDraft() && !Boolean.TRUE.equals(viewModel.generating.getValue()));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        binding.input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendDraft();
                return true;
            }
            return false;
        });
        binding.send.setOnClickListener(v -> sendDraft());
        binding.mic.setOnClickListener(v -> onMicClicked());
        binding.retry.setOnClickListener(v -> viewModel.ensureModelLoaded());
        binding.backToSetup.setOnClickListener(v -> {
            startActivity(new Intent(this, SetupActivity.class));
            finish();
        });
        binding.send.setEnabled(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        viewModel.refreshTextSize();
        viewModel.applyCurrentSettings();
        viewModel.ensureModelLoaded();
        speechOutput.setRate(new SettingsRepository(this).load().speechRate);
    }

    @Override
    protected void onStop() {
        speechInput.stop();
        speechOutput.stop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        speechInput.release();
        speechOutput.shutdown();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Long id = viewModel.getConversationId();
        if (id != null && id > 0) {
            outState.putLong(EXTRA_CONVERSATION_ID, id);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_new_chat) {
            startActivity(new Intent(this, ChatActivity.class));
            finish();
            return true;
        }
        if (id == R.id.action_history) {
            startActivity(new Intent(this, HistoryActivity.class));
            return true;
        }
        if (id == R.id.action_mistakes) {
            startActivity(new Intent(this, MistakesActivity.class));
            return true;
        }
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshTyping() {
        boolean busy = Boolean.TRUE.equals(viewModel.generating.getValue());
        String partial = viewModel.streamingText.getValue();
        boolean hasPartial = partial != null && !partial.isEmpty();
        binding.typing.setVisibility(busy && !hasPartial ? View.VISIBLE : View.GONE);
    }

    private void showTitle(Conversation conversation) {
        if (conversation == null || getSupportActionBar() == null) {
            return;
        }
        getSupportActionBar().setTitle(Level.labelOf(conversation.level));
        getSupportActionBar().setSubtitle(Topic.labelOf(conversation.topic)
                + " · " + CorrectionMode.fromName(conversation.correctionMode).labelFr);
    }

    private void showModelPhase(ChatViewModel.ModelPhase phase) {
        if (phase == null || phase == ChatViewModel.ModelPhase.READY) {
            binding.loadingOverlay.setVisibility(View.GONE);
            return;
        }
        binding.loadingOverlay.setVisibility(View.VISIBLE);
        boolean failed = phase == ChatViewModel.ModelPhase.FAILED;
        binding.loadingProgress.setVisibility(failed ? View.GONE : View.VISIBLE);
        binding.retry.setVisibility(failed ? View.VISIBLE : View.GONE);
        binding.backToSetup.setVisibility(failed ? View.VISIBLE : View.GONE);
    }

    private void sendDraft() {
        String text = binding.input.getText() == null ? "" : binding.input.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        speechInput.stop();
        viewModel.send(text);
        binding.input.setText("");
    }

    private boolean hasDraft() {
        return binding.input.getText() != null && binding.input.getText().toString().trim().length() > 0;
    }

    private void onMicClicked() {
        if (speechInput.isBusy()) {
            if (speechInput.isListening()) {
                speechInput.stop();
            }
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            micPermission.launch(Manifest.permission.RECORD_AUDIO);
            return;
        }
        startListening();
    }

    private void startListening() {
        speechOutput.stop();
        try {
            speechInput.start(this, new SpeechInput.Callback() {
                @Override
                public void onPartial(String text) {
                    binding.input.setText(text);
                    binding.input.setSelection(binding.input.getText() == null ? 0 : binding.input.getText().length());
                }

                @Override
                public void onResult(String text) {
                    speechPackNoticeShown = false;
                    if (!text.isEmpty()) {
                        binding.input.setText(text);
                        binding.input.setSelection(text.length());
                    }
                }

                @Override
                public void onDownloadProgress(int percent) {
                    if (percent < 0) {
                        speechPackNoticeShown = false;
                        snack(getString(R.string.speech_pack_scheduled), false);
                        return;
                    }
                    if (!speechPackNoticeShown) {
                        speechPackNoticeShown = true;
                        snack(getString(R.string.speech_pack_downloading), false);
                    }
                }

                @Override
                public void onError(int code, String message) {
                    speechPackNoticeShown = false;
                    if (needsSpeechPack(code)) {
                        offerSpeechPack();
                        return;
                    }
                    snack(speechError(code), false);
                }
            });
        } catch (RuntimeException exception) {
            snack(getString(R.string.error_mic_generic), false);
        }
    }

    private String speechError(int code) {
        switch (code) {
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return getString(R.string.error_mic_permission);
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
            case SpeechRecognizer.ERROR_SERVER:
            case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED:
            case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE:
                return getString(R.string.error_mic_offline);
            case SpeechRecognizer.ERROR_NO_MATCH:
                return getString(R.string.error_mic_no_match);
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return getString(R.string.error_mic_timeout);
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return getString(R.string.error_mic_busy);
            case SpeechRecognizer.ERROR_CLIENT:
                return getString(R.string.error_mic_unavailable);
            default:
                return getString(R.string.error_mic_generic);
        }
    }

    private void speak(String text) {
        if (!englishVoiceReady) {
            Snackbar.make(binding.getRoot(), R.string.error_tts_missing, Snackbar.LENGTH_LONG)
                    .setAction(R.string.install_voice, v -> installTts())
                    .show();
            return;
        }
        speechOutput.setRate(new SettingsRepository(this).load().speechRate);
        speechOutput.speak(text);
    }

    private void installTts() {
        try {
            startActivity(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA));
        } catch (ActivityNotFoundException exception) {
            snack(getString(R.string.error_tts_missing), false);
        }
    }

    private boolean needsSpeechPack(int code) {
        return code == SpeechRecognizer.ERROR_NETWORK
                || code == SpeechRecognizer.ERROR_NETWORK_TIMEOUT
                || code == SpeechRecognizer.ERROR_SERVER
                || code == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED
                || code == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE;
    }

    private void offerSpeechPack() {
        Snackbar.make(binding.getRoot(), R.string.error_mic_offline, Snackbar.LENGTH_LONG)
                .setAction(R.string.download_speech_pack, v -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startListening();
                    } else {
                        openVoiceOrAppSettings();
                    }
                })
                .show();
    }

    private void snack(String text, boolean offerSettings) {
        Snackbar bar = Snackbar.make(binding.getRoot(), text, Snackbar.LENGTH_LONG);
        if (offerSettings) {
            bar.setAction(R.string.open_settings, v -> openVoiceOrAppSettings());
        }
        bar.show();
    }

    private void openVoiceOrAppSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS));
        } catch (ActivityNotFoundException exception) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(intent);
        }
    }

    private void scrollToEnd() {
        int count = adapter.getItemCount();
        if (count > 0) {
            binding.messages.scrollToPosition(count - 1);
        }
    }
}
