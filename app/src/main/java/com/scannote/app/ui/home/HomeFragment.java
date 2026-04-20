package com.scannote.app.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.scannote.app.database.AppDatabase;
import com.scannote.app.database.DocumentEntry;
import com.scannote.app.databinding.FragmentHomeBinding;
import com.scannote.app.ui.adapters.ScanItemAdapter;
import com.scannote.app.ui.detail.DocumentDetailActivity;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ScanItemAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new ScanItemAdapter(
            entry -> openDetail(entry),
            entry -> deleteScan(entry)
        );

        binding.recyclerRecent.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerRecent.setAdapter(adapter);

        setupChips();

        loadDocuments(null);
    }

    private void setupChips() {
        binding.chipUniversity.setOnClickListener(v -> toggleFilter("Universidad", binding.chipUniversity));
        binding.chipWork.setOnClickListener(v -> toggleFilter("Trabajo", binding.chipWork));
        binding.chipPersonal.setOnClickListener(v -> toggleFilter("Personal", binding.chipPersonal));
        binding.chipIdeas.setOnClickListener(v -> toggleFilter("Ideas", binding.chipIdeas));
    }

    private String currentFilter = null;
    private View lastSelectedChip = null;

    private void toggleFilter(String category, View chip) {
        if (category.equals(currentFilter)) {
            currentFilter = null;
            chip.setBackgroundResource(com.scannote.app.R.drawable.bg_category_chip);
            lastSelectedChip = null;
            loadDocuments(null);
        } else {
            if (lastSelectedChip != null) {
                lastSelectedChip.setBackgroundResource(com.scannote.app.R.drawable.bg_category_chip);
            }
            currentFilter = category;
            chip.setBackgroundResource(com.scannote.app.R.drawable.bg_category_chip_selected);
            lastSelectedChip = chip;
            loadDocuments(category);
        }
    }

    private void loadDocuments(String filter) {
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        LiveData<List<DocumentEntry>> liveData = (filter == null)
                ? db.documentDao().getAllDocuments()
                : db.documentDao().getDocumentsByType(filter);

        liveData.observe(getViewLifecycleOwner(), documents -> {
            if (documents == null || documents.isEmpty()) {
                binding.recyclerRecent.setVisibility(View.GONE);
                binding.emptyState.setVisibility(View.VISIBLE);
                binding.textScanCount.setText(filter == null ? "Aún no tienes escaneos" : "No hay documentos en " + filter);
            } else {
                binding.recyclerRecent.setVisibility(View.VISIBLE);
                binding.emptyState.setVisibility(View.GONE);
                List<DocumentEntry> displayDocs = documents.size() > 20
                        ? documents.subList(0, 20) : documents;
                adapter.submitList(displayDocs);
                int count = documents.size();
                binding.textScanCount.setText(count + (count == 1 ? " escaneo guardado" : " escaneos guardados"));
            }
        });
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
