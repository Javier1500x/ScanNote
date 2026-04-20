package com.scannote.app.ui.study;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.scannote.app.databinding.FragmentHomeBinding;

public class StudyFragment extends Fragment {
    private FragmentHomeBinding binding;

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
        binding.recyclerRecent.setVisibility(View.GONE);
        binding.emptyState.setVisibility(View.VISIBLE);
        // Aquí se podrían cargar exámenes guardados en el futuro
    }
}
