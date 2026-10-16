package com.langfree.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.langfree.R;
import com.langfree.data.CourseCatalog;
import com.langfree.databinding.ActivityLessonsBinding;
import com.langfree.databinding.ItemLessonBinding;

/** Catalogue des leçons du cours, à la place de l'entrée « Mes erreurs ». */
public class LessonsActivity extends AppCompatActivity {
    private ActivityLessonsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLessonsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        CourseCatalog catalog = CourseCatalog.load(this);
        addGuide();
        String part = "";
        for (CourseCatalog.Lesson lesson : catalog.lessons) {
            if (!lesson.part.equals(part)) {
                part = lesson.part;
                addPart(part);
            }
            addLesson(lesson);
        }
        binding.notebook.setOnClickListener(v ->
                startActivity(new Intent(this, MistakesActivity.class)));
    }

    private void addGuide() {
        ItemLessonBinding card = ItemLessonBinding.inflate(getLayoutInflater(), binding.lessons, true);
        card.kicker.setText(R.string.lesson_guide_kicker);
        card.title.setText(R.string.lesson_guide);
        card.summary.setText(R.string.lesson_guide_summary);
        card.getRoot().setOnClickListener(v -> open(LessonActivity.INTRO_ID));
    }

    private void addPart(String part) {
        TextView header = new TextView(this);
        header.setText("A".equals(part) ? R.string.lesson_part_a : R.string.lesson_part_b);
        header.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        header.setTextColor(getColor(R.color.md_primary));
        int margin = getResources().getDimensionPixelSize(R.dimen.space_m);
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = margin;
        params.bottomMargin = getResources().getDimensionPixelSize(R.dimen.space_s);
        params.setMarginStart(margin);
        params.setMarginEnd(margin);
        header.setLayoutParams(params);
        binding.lessons.addView(header);
    }

    private void addLesson(CourseCatalog.Lesson lesson) {
        ItemLessonBinding card = ItemLessonBinding.inflate(getLayoutInflater(), binding.lessons, true);
        card.kicker.setText(lesson.kicker);
        card.title.setText(lesson.title);
        card.summary.setText(lesson.goals.isEmpty() ? "" : lesson.goals.get(0));
        card.getRoot().setOnClickListener(v -> open(lesson.id));
    }

    private void open(String id) {
        startActivity(new Intent(this, LessonActivity.class).putExtra(LessonActivity.EXTRA_ID, id));
    }
}
