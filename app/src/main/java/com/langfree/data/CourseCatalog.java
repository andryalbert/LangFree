package com.langfree.data;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Cours English for Work & Business, extrait du livre et lu hors ligne. */
public final class CourseCatalog {
    public final String intro;
    public final List<Lesson> lessons;

    private CourseCatalog(String intro, List<Lesson> lessons) {
        this.intro = intro;
        this.lessons = lessons;
    }

    public static CourseCatalog load(Context context) {
        try {
            InputStream in = context.getAssets().open("lessons.json");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
            in.close();
            JSONObject root = new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
            JSONArray array = root.getJSONArray("lessons");
            List<Lesson> lessons = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                lessons.add(Lesson.from(array.getJSONObject(i)));
            }
            return new CourseCatalog(root.optString("intro"), lessons);
        } catch (Exception exception) {
            throw new IllegalStateException("Le cours n'a pas pu être lu.", exception);
        }
    }

    public Lesson find(String id) {
        for (Lesson lesson : lessons) {
            if (lesson.id.equals(id)) {
                return lesson;
            }
        }
        return null;
    }

    public static final class Lesson {
        public final String id;
        public final String part;
        public final String kicker;
        public final String title;
        public final List<String> goals;
        public final List<Vocab> vocabulary;
        public final List<String> phrases;
        public final List<Turn> dialogue;
        public final String exerciseALead;
        public final List<String> exerciseA;
        public final String exerciseBLead;
        public final List<String> exerciseB;
        public final List<String> exerciseC;
        public final String answers;

        private Lesson(String id, String part, String kicker, String title, List<String> goals,
                List<Vocab> vocabulary, List<String> phrases, List<Turn> dialogue,
                String exerciseALead, List<String> exerciseA, String exerciseBLead,
                List<String> exerciseB, List<String> exerciseC, String answers) {
            this.id = id;
            this.part = part;
            this.kicker = kicker;
            this.title = title;
            this.goals = goals;
            this.vocabulary = vocabulary;
            this.phrases = phrases;
            this.dialogue = dialogue;
            this.exerciseALead = exerciseALead;
            this.exerciseA = exerciseA;
            this.exerciseBLead = exerciseBLead;
            this.exerciseB = exerciseB;
            this.exerciseC = exerciseC;
            this.answers = answers;
        }

        static Lesson from(JSONObject json) throws Exception {
            return new Lesson(
                    json.getString("id"),
                    json.getString("part"),
                    json.getString("kicker"),
                    json.getString("title"),
                    strings(json.getJSONArray("goals")),
                    vocab(json.getJSONArray("vocabulary")),
                    strings(json.getJSONArray("phrases")),
                    turns(json.getJSONArray("dialogue")),
                    json.optString("exerciseALead"),
                    strings(json.getJSONArray("exerciseA")),
                    json.optString("exerciseBLead"),
                    strings(json.getJSONArray("exerciseB")),
                    strings(json.getJSONArray("exerciseC")),
                    json.optString("answers"));
        }
    }

    public static final class Vocab {
        public final String word;
        public final String meaning;
        public final String example;

        Vocab(String word, String meaning, String example) {
            this.word = word;
            this.meaning = meaning;
            this.example = example;
        }
    }

    public static final class Turn {
        public final String speaker;
        public final String line;

        Turn(String speaker, String line) {
            this.speaker = speaker;
            this.line = line;
        }
    }

    private static List<String> strings(JSONArray array) throws Exception {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            values.add(array.getString(i));
        }
        return values;
    }

    private static List<Vocab> vocab(JSONArray array) throws Exception {
        List<Vocab> values = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            values.add(new Vocab(item.getString("word"), item.getString("meaning"), item.getString("example")));
        }
        return values;
    }

    private static List<Turn> turns(JSONArray array) throws Exception {
        List<Turn> values = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            values.add(new Turn(item.getString("speaker"), item.getString("line")));
        }
        return values;
    }
}
