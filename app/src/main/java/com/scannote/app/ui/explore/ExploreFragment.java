package com.scannote.app.ui.explore;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.scannote.app.database.AppDatabase;
import com.scannote.app.databinding.FragmentLibraryBinding;
import com.scannote.app.ui.adapters.ScanItemAdapter;
import com.scannote.app.ui.detail.DocumentDetailActivity;
import java.util.ArrayList;

public class ExploreFragment extends Fragment {
    private FragmentLibraryBinding binding;
    private ScanItemAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.textLibraryTitle.setText("Explorar Categorías");
        
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

        binding.recyclerLibrary.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        binding.recyclerLibrary.setAdapter(adapter);

        AppDatabase.getDatabase(requireContext()).documentDao().getAllDocuments().observe(getViewLifecycleOwner(), docs -> {
            if (docs != null) adapter.submitList(docs);
        });
    }
}
