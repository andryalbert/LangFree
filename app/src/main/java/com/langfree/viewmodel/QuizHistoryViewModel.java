package com.langfree.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.langfree.data.AppDatabase;
import com.langfree.data.QuizRepository;
import com.langfree.model.QuizSession;
import com.langfree.model.QuizTurn;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Historique des quiz terminés et détail d'un passage. */
public class QuizHistoryViewModel extends AndroidViewModel {
    public final LiveData<List<QuizSession>> sessions;
    public final LiveData<List<QuizTurn>> turns;

    private final QuizRepository repository;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Long> sessionId = new MutableLiveData<>();

    public QuizHistoryViewModel(@NonNull Application application) {
        super(application);
        AppDatabase database = AppDatabase.getInstance(application);
        repository = new QuizRepository(database);
        sessions = database.quizSessionDao().observeAnswered();
        turns = Transformations.switchMap(sessionId, id -> {
            if (id == null || id <= 0) {
                MutableLiveData<List<QuizTurn>> empty = new MutableLiveData<>();
                empty.setValue(Collections.emptyList());
                return empty;
            }
            return database.quizTurnDao().observe(id);
        });
    }

    public void open(long id) {
        sessionId.setValue(id);
    }

    public void delete(QuizSession session) {
        io.execute(() -> repository.delete(session));
    }

    @Override
    protected void onCleared() {
        io.shutdownNow();
        super.onCleared();
    }
}
