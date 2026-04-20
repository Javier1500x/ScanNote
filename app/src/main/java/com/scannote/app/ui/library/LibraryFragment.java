package com.scannote.app.ui.library;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.scannote.app.database.AppDatabase;
import com.scannote.app.database.DocumentEntry;
import com.scannote.app.databinding.FragmentLibraryBinding;
import com.scannote.app.ui.adapters.ScanItemAdapter;
import com.scannote.app.ui.detail.DocumentDetailActivity;
import java.util.ArrayList;
import java.util.List;

public class LibraryFragment extends Fragment {

    private FragmentLibraryBinding binding;
    private ScanItemAdapter adapter;
    private List<DocumentEntry> allDocs = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new ScanItemAdapter(
            entry -> openDetail(entry),
            entry -> deleteScan(entry)
        );

        binding.recyclerLibrary.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerLibrary.setAdapter(adapter);

        // Cargar todos los documentos de Room DB
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        db.documentDao().getAllDocuments().observe(getViewLifecycleOwner(), documents -> {
            allDocs = documents != null ? documents : new ArrayList<>();
            applyFilter(binding.editSearch.getText().toString());
        });

        // Búsqueda en tiempo real
        binding.editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                applyFilter(s.toString());
            }
        });
    }

    private void applyFilter(String query) {
        List<DocumentEntry> filtered;
        if (query == null || query.trim().isEmpty()) {
            filtered = allDocs;
        } else {
            String q = query.toLowerCase().trim();
            filtered = new ArrayList<>();
            for (DocumentEntry doc : allDocs) {
                if ((doc.title != null && doc.title.toLowerCase().contains(q))
                        || (doc.content != null && doc.content.toLowerCase().contains(q))) {
                    filtered.add(doc);
                }
            }
        }

        adapter.submitList(filtered);

        if (filtered.isEmpty()) {
            binding.recyclerLibrary.setVisibility(View.GONE);
            binding.emptyStateLibrary.setVisibility(View.VISIBLE);
        } else {
            binding.recyclerLibrary.setVisibility(View.VISIBLE);
            binding.emptyStateLibrary.setVisibility(View.GONE);
        }
    }

    private void openDetail(DocumentEntry entry) {
        Intent intent = new Intent(requireContext(), DocumentDetailActivity.class);
        intent.putExtra("EXTRA_CONTENT", entry.content);
        intent.putExtra("EXTRA_TITLE", entry.title);
        startActivity(intent);
    }

    private void deleteScan(DocumentEntry entry) {
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            db.documentDao().delete(entry);
        });
        android.widget.Toast.makeText(requireContext(), "Escaneo eliminado", android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
