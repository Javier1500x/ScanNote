package com.scannote.app.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.scannote.app.database.AppDatabase;
import com.scannote.app.database.DocumentEntry;
import com.scannote.app.databinding.ActivityCameraBinding;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {
    private ActivityCameraBinding binding;
    private ExecutorService cameraExecutor;
    private ExecutorService dbExecutor;
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    private String lastDetectedText = "";
    private androidx.activity.result.ActivityResultLauncher<String> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbExecutor = Executors.newSingleThreadExecutor();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        if (binding.btnBack != null) {
            binding.btnBack.setOnClickListener(v -> finish());
        }

        binding.btnCapture.setOnClickListener(v -> {
            if (!lastDetectedText.isEmpty()) {
                saveAndOpen(lastDetectedText);
            } else {
                Toast.makeText(this, "Apunta la cámara al texto primero", Toast.LENGTH_SHORT).show();
            }
        });

        galleryLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        processImageFromUri(uri);
                    }
                }
        );

        if (binding.btnGallery != null) {
            binding.btnGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        }

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void saveAndOpen(String text) {
        // Generar título automático con fecha y hora
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        String titleDate = sdf.format(new Date());
        // Usar las primeras palabras del texto como parte del título
        String preview = text.length() > 30 ? text.substring(0, 30).trim() + "…" : text.trim();
        String title = "Escaneo " + titleDate;

        DocumentEntry entry = new DocumentEntry(title, text, System.currentTimeMillis(), "Documento", "General");

        // Guardar en hilo de fondo, luego abrir el detalle
        dbExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
            db.documentDao().insert(entry);

            runOnUiThread(() -> {
                android.content.Intent intent = new android.content.Intent(
                        this, com.scannote.app.ui.detail.DocumentDetailActivity.class);
                intent.putExtra("EXTRA_CONTENT", text);
                intent.putExtra("EXTRA_TITLE", title);
                startActivity(intent);
                finish();
            });
        });
    }

    private void processImageFromUri(android.net.Uri uri) {
        try {
            com.google.mlkit.vision.common.InputImage image = com.google.mlkit.vision.common.InputImage.fromFilePath(this, uri);
            com.google.mlkit.vision.text.TextRecognizer recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS);
            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        StringBuilder sb = new StringBuilder();
                        for (com.google.mlkit.vision.text.Text.TextBlock block : visionText.getTextBlocks()) {
                            for (com.google.mlkit.vision.text.Text.Line line : block.getLines()) {
                                String cleanLine = OcrAnalyzer.cleanOcrLine(line.getText());
                                if (!cleanLine.isEmpty()) {
                                    sb.append(cleanLine).append("\n");
                                }
                            }
                            sb.append("\n");
                        }
                        String result = sb.toString().trim();
                        result = OcrAnalyzer.postProcessOcr(result);
                        if (!result.isEmpty()) {
                            saveAndOpen(result);
                        } else {
                            Toast.makeText(this, "No se encontró texto en la imagen", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error OCR: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } catch (java.io.IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al abrir imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());

                // Resolución más alta para mejor OCR
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new android.util.Size(1920, 1080))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor,
                        new OcrAnalyzer(binding.scanningOverlay, text -> {
                            lastDetectedText = text;
                        }));

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permiso de cámara requerido", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        dbExecutor.shutdown();
    }
}
