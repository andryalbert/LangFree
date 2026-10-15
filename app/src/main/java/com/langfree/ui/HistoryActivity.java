package com.langfree.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.langfree.R;
import com.langfree.databinding.ActivityHistoryBinding;
import com.langfree.model.Conversation;
import com.langfree.utils.ModelStorage;
import com.langfree.viewmodel.HistoryViewModel;

/** Liste des conversations, reprise et suppression. */
public class HistoryActivity extends AppCompatActivity implements ConversationAdapter.Listener {
    private ActivityHistoryBinding binding;
    private HistoryViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        ConversationAdapter adapter = new ConversationAdapter(this);
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);
        viewModel.conversations.observe(this, conversations -> {
            adapter.submit(conversations);
            boolean empty = conversations == null || conversations.isEmpty();
            binding.empty.setVisibility(empty ? View.VISIBLE : View.GONE);
        });
        binding.newChat.setOnClickListener(v -> openNewChat());
    }

    @Override
    public void onOpen(Conversation conversation) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversation.id);
        startActivity(intent);
    }

    @Override
    public void onDelete(Conversation conversation) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.delete_conversation)
                .setPositiveButton(R.string.delete, (dialog, which) -> viewModel.delete(conversation))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void openNewChat() {
        if (!ModelStorage.isReady(this)) {
            Snackbar.make(binding.getRoot(), R.string.model_required, Snackbar.LENGTH_LONG)
                    .setAction(R.string.go_setup, v -> startActivity(new Intent(this, SetupActivity.class)))
                    .show();
            return;
        }
        startActivity(new Intent(this, ChatActivity.class));
    }
}
