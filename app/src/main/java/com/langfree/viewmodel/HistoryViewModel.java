package com.langfree.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.langfree.data.AppDatabase;
import com.langfree.data.ChatRepository;
import com.langfree.model.Conversation;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Liste des conversations enregistrées. */
public class HistoryViewModel extends AndroidViewModel {
    public final LiveData<List<Conversation>> conversations;
    private final ChatRepository repository;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        AppDatabase database = AppDatabase.getInstance(application);
        repository = new ChatRepository(database);
        conversations = database.conversationDao().observeAll();
    }

    public void delete(Conversation conversation) {
        io.execute(() -> repository.deleteConversation(conversation));
    }

    @Override
    protected void onCleared() {
        io.shutdownNow();
        super.onCleared();
    }
}
