package com.langfree.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.langfree.data.AppDatabase;
import com.langfree.data.ChatRepository;
import com.langfree.model.Mistake;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Carnet d'erreurs trié par fréquence. */
public class MistakesViewModel extends AndroidViewModel {
    public final LiveData<List<Mistake>> mistakes;
    private final ChatRepository repository;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public MistakesViewModel(@NonNull Application application) {
        super(application);
        AppDatabase database = AppDatabase.getInstance(application);
        repository = new ChatRepository(database);
        mistakes = database.mistakeDao().observeAll();
    }

    public void delete(Mistake mistake) {
        io.execute(() -> repository.deleteMistake(mistake));
    }

    public void clearAll() {
        io.execute(repository::clearMistakes);
    }

    @Override
    protected void onCleared() {
        io.shutdownNow();
        super.onCleared();
    }
}
