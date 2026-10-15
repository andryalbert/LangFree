package com.langfree.viewmodel;

import android.app.ActivityManager;
import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.langfree.LangFreeApp;
import com.langfree.R;
import com.langfree.ai.LlmManager;
import com.langfree.model.UiSignal;
import com.langfree.utils.ModelDownloader;
import com.langfree.utils.ModelStorage;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Écran d'accueil : présence du modèle, import, téléchargement, mémoire disponible.
 */
public class SetupViewModel extends AndroidViewModel {
    public final MutableLiveData<Boolean> modelReady = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    public final MutableLiveData<Integer> progress = new MutableLiveData<>(-1);
    public final MutableLiveData<String> status = new MutableLiveData<>();
    public final MutableLiveData<String> memoryHint = new MutableLiveData<>();
    public final MutableLiveData<UiSignal> message = new MutableLiveData<>();

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final LlmManager llmManager;

    public SetupViewModel(@NonNull Application application) {
        super(application);
        llmManager = ((LangFreeApp) application).getLlmManager();
        refresh();
    }

    public void refresh() {
        boolean ready = ModelStorage.isReady(getApplication());
        modelReady.setValue(ready);
        if (ready) {
            long mega = ModelStorage.modelFile(getApplication()).length() / (1024L * 1024L);
            status.setValue(getApplication().getString(R.string.model_ready, mega));
        } else {
            status.setValue(getApplication().getString(R.string.model_absent));
        }
        memoryHint.setValue(readMemoryHint());
    }

    /** L'accueil ne garde pas le modèle en RAM : on le recharge en entrant dans le chat. */
    public void releaseModel() {
        llmManager.release();
    }

    public void importModel(Uri uri) {
        if (uri == null || Boolean.TRUE.equals(busy.getValue())) {
            return;
        }
        busy.setValue(true);
        progress.setValue(0);
        status.setValue(getApplication().getString(R.string.model_importing));
        io.execute(() -> {
            try {
                awaitRelease();
                ModelDownloader.importFromUri(getApplication(), uri, (percent, copied, total) ->
                        progress.postValue(percent));
                busy.postValue(false);
                progress.postValue(100);
                refreshFromBackground();
                message.postValue(new UiSignal(getApplication().getString(R.string.model_import_done)));
            } catch (ModelDownloader.TransferException error) {
                failTransfer(error);
            } catch (Exception error) {
                fail(getApplication().getString(R.string.error_import));
            }
        });
    }

    public void downloadModel() {
        if (Boolean.TRUE.equals(busy.getValue())) {
            return;
        }
        busy.setValue(true);
        progress.setValue(-1);
        status.setValue(getApplication().getString(R.string.model_downloading));
        io.execute(() -> {
            try {
                awaitRelease();
                ModelDownloader.download(getApplication(), (percent, copied, total) ->
                        progress.postValue(percent));
                busy.postValue(false);
                progress.postValue(100);
                refreshFromBackground();
                message.postValue(new UiSignal(getApplication().getString(R.string.model_import_done)));
            } catch (ModelDownloader.TransferException error) {
                failTransfer(error);
            } catch (Exception error) {
                fail(getApplication().getString(R.string.error_download));
            }
        });
    }

    /** Attend la fermeture du modèle avant de remplacer le fichier mappé en mémoire. */
    private void awaitRelease() {
        CountDownLatch latch = new CountDownLatch(1);
        llmManager.releaseThen(latch::countDown);
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void failTransfer(ModelDownloader.TransferException error) {
        String text;
        switch (error.reason) {
            case NO_NETWORK:
                text = getApplication().getString(R.string.error_no_network);
                break;
            case AUTH:
                text = getApplication().getString(R.string.error_model_unavailable);
                break;
            case TOO_SMALL:
                text = getApplication().getString(R.string.error_file_too_small);
                break;
            case NO_SPACE:
                text = getApplication().getString(R.string.error_no_space);
                break;
            case FAILED:
            default:
                text = getApplication().getString(R.string.error_download);
                break;
        }
        fail(text);
    }

    private void fail(String text) {
        busy.postValue(false);
        progress.postValue(-1);
        message.postValue(new UiSignal(text));
        refreshFromBackground();
    }

    private void refreshFromBackground() {
        boolean ready = ModelStorage.isReady(getApplication());
        modelReady.postValue(ready);
        if (ready) {
            long mega = ModelStorage.modelFile(getApplication()).length() / (1024L * 1024L);
            status.postValue(getApplication().getString(R.string.model_ready, mega));
        } else {
            status.postValue(getApplication().getString(R.string.model_absent));
        }
    }

    private String readMemoryHint() {
        ActivityManager manager = (ActivityManager) getApplication().getSystemService(Application.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        if (manager != null) {
            manager.getMemoryInfo(info);
        }
        long totalMb = info.totalMem / (1024L * 1024L);
        long availMb = info.availMem / (1024L * 1024L);
        String base = getApplication().getString(R.string.memory_hint, availMb, totalMb);
        if (info.availMem > 0 && info.availMem < 1500L * 1024L * 1024L) {
            return base + "\n" + getApplication().getString(R.string.memory_low);
        }
        return base;
    }

    public String formatProgress(int percent) {
        if (percent < 0) {
            return getApplication().getString(R.string.progress_unknown);
        }
        return String.format(Locale.FRANCE, "%d %%", percent);
    }

    @Override
    protected void onCleared() {
        io.shutdownNow();
        super.onCleared();
    }
}
