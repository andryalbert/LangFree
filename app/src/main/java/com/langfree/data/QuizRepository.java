package com.langfree.data;

import com.langfree.model.QuizSession;
import com.langfree.model.QuizTurn;

/** Enregistre les quiz et leurs questions. */
public class QuizRepository {
    private final QuizSessionDao sessions;
    private final QuizTurnDao turns;

    public QuizRepository(AppDatabase database) {
        sessions = database.quizSessionDao();
        turns = database.quizTurnDao();
    }

    public long createSession() {
        long now = System.currentTimeMillis();
        QuizSession session = new QuizSession();
        session.startedAt = now;
        session.updatedAt = now;
        session.levelEstimate = "";
        session.questionCount = 0;
        return sessions.insert(session);
    }

    public long addQuestion(long sessionId, String question) {
        QuizTurn turn = new QuizTurn();
        turn.sessionId = sessionId;
        turn.question = question;
        turn.answer = "";
        turn.correction = "";
        turn.explanation = "";
        turn.levelEstimate = "";
        turn.createdAt = System.currentTimeMillis();
        return turns.insert(turn);
    }

    public java.util.List<String> recentQuestions(long sessionId) {
        return turns.recentQuestions(sessionId);
    }

    public void saveGrade(long sessionId, long turnId, String answer, String correction,
                          String explanation, String level) {
        QuizTurn stored = turns.getById(turnId);
        QuizSession session = sessions.getById(sessionId);
        if (stored == null || session == null) {
            return;
        }
        stored.answer = answer;
        stored.correction = correction;
        stored.explanation = explanation;
        stored.levelEstimate = level == null ? "" : level;
        turns.update(stored);
        session.updatedAt = System.currentTimeMillis();
        session.questionCount = session.questionCount + 1;
        if (level != null && !level.isEmpty()) {
            session.levelEstimate = level;
        }
        sessions.update(session);
    }

    public void delete(QuizSession session) {
        turns.deleteForSession(session.id);
        sessions.delete(session);
    }
}
