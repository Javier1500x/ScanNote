package com.scannote.app.ui.study;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.scannote.app.databinding.FragmentHomeBinding;

import androidx.recyclerview.widget.LinearLayoutManager;
import com.scannote.app.database.AppDatabase;
import com.scannote.app.ui.adapters.ScanItemAdapter;
import com.scannote.app.ui.detail.DocumentDetailActivity;
import android.content.Intent;

public class StudyFragment extends Fragment {
    private FragmentHomeBinding binding;
    private ScanItemAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.textHomeTitle.setText("Área de Estudio");
        binding.textScanCount.setText("Tus sesiones de repaso");
        
        adapter = new ScanItemAdapter(
            entry -> {
                Intent intent = new Intent(requireContext(), DocumentDetailActivity.class);
                intent.putExtra("EXTRA_CONTENT", entry.content);
                intent.putExtra("EXTRA_TITLE", entry.title);
                startActivity(intent);
            },
            entry -> {
                java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                    AppDatabase.getDatabase(requireContext()).documentDao().delete(entry);
                });
            }
        );

        binding.recyclerRecent.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerRecent.setAdapter(adapter);

        AppDatabase.getDatabase(requireContext()).documentDao().getDocumentsByType("Estudio").observe(getViewLifecycleOwner(), docs -> {
            if (docs == null || docs.isEmpty()) {
                binding.recyclerRecent.setVisibility(View.GONE);
                binding.emptyState.setVisibility(View.VISIBLE);
            } else {
                binding.recyclerRecent.setVisibility(View.VISIBLE);
                binding.emptyState.setVisibility(View.GONE);
                adapter.submitList(docs);
            }
        });
    }
}
