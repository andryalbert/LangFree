package com.langfree.ui;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.langfree.R;
import com.langfree.data.CourseCatalog;
import com.langfree.databinding.ActivityLessonBinding;
import com.langfree.databinding.ItemVocabBinding;

/** Une leçon : objectifs, vocabulaire, phrases, dialogue et exercices. */
public class LessonActivity extends AppCompatActivity {
    public static final String EXTRA_ID = "lesson_id";
    public static final String INTRO_ID = "intro";

    private ActivityLessonBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLessonBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        CourseCatalog catalog = CourseCatalog.load(this);
        String id = getIntent().getStringExtra(EXTRA_ID);
        if (INTRO_ID.equals(id)) {
            showIntro(catalog.intro);
            return;
        }
        CourseCatalog.Lesson lesson = catalog.find(id);
        if (lesson == null) {
            finish();
            return;
        }
        binding.toolbar.setTitle(lesson.title);
        binding.toolbar.setSubtitle(lesson.kicker);
        addHeading(R.string.lesson_goals);
        for (String goal : lesson.goals) {
            addBullet(goal);
        }
        addHeading(R.string.lesson_vocab);
        for (CourseCatalog.Vocab vocab : lesson.vocabulary) {
            ItemVocabBinding card = ItemVocabBinding.inflate(getLayoutInflater(), binding.content, true);
            card.word.setText(vocab.word);
            card.meaning.setText(vocab.meaning);
            card.example.setText(vocab.example);
        }
        addHeading(R.string.lesson_phrases);
        for (String phrase : lesson.phrases) {
            addBullet(phrase);
        }
        addHeading(R.string.lesson_dialogue);
        for (CourseCatalog.Turn turn : lesson.dialogue) {
            addBody(turn.speaker + " — " + turn.line);
        }
        addExercise(R.string.lesson_exercise_a, lesson.exerciseALead, lesson.exerciseA);
        addExercise(R.string.lesson_exercise_b, lesson.exerciseBLead, lesson.exerciseB);
        addHeading(R.string.lesson_exercise_c);
        for (String play : lesson.exerciseC) {
            addBody(play);
        }
        if (!lesson.answers.isEmpty()) {
            TextView answers = addBody(lesson.answers);
            answers.setVisibility(android.view.View.GONE);
            MaterialButton toggle = new MaterialButton(this, null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            toggle.setText(R.string.lesson_show_answers);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.topMargin = getResources().getDimensionPixelSize(R.dimen.space_m);
            toggle.setLayoutParams(params);
            toggle.setOnClickListener(v -> {
                boolean show = answers.getVisibility() != android.view.View.VISIBLE;
                answers.setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
                toggle.setText(show ? R.string.lesson_hide_answers : R.string.lesson_show_answers);
            });
            binding.content.addView(toggle);
        }
    }

    private void showIntro(String intro) {
        binding.toolbar.setTitle(R.string.lesson_guide);
        for (String paragraph : intro.split("\n\n")) {
            addBody(paragraph.trim());
        }
    }

    private void addExercise(int title, String lead, java.util.List<String> items) {
        addHeading(title);
        if (lead != null && !lead.isEmpty()) {
            addBody(lead);
        }
        for (String item : items) {
            addBody(item);
        }
    }

    private void addHeading(int text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        view.setTextColor(getColor(R.color.md_primary));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = getResources().getDimensionPixelSize(R.dimen.space_l);
        view.setLayoutParams(params);
        binding.content.addView(view);
    }

    private TextView addBody(String text) {
        TextView view = textView(text);
        view.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        return view;
    }

    private void addBullet(String text) {
        TextView view = textView("•  " + text);
        view.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
    }

    private TextView textView(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(getColor(R.color.md_on_surface));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = getResources().getDimensionPixelSize(R.dimen.space_s);
        view.setLayoutParams(params);
        binding.content.addView(view);
        return view;
    }
}
