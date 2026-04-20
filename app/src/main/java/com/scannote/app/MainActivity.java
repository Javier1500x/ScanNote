package com.scannote.app;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import android.net.Uri;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.scannote.app.camera.CameraActivity;
import com.scannote.app.databinding.ActivityMainBinding;
import com.scannote.app.ui.detail.DocumentDetailActivity;
import com.scannote.app.utils.DocumentParser;
import com.scannote.app.ai.GeminiManager;
import com.scannote.app.database.AppDatabase;
import com.scannote.app.database.DocumentEntry;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    @Inject GeminiManager geminiManager;
    private ExecutorService dbExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        dbExecutor = Executors.newSingleThreadExecutor();

        // Configurar navegación
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.bottomNavigationView, navController);
        }

        // FAB para abrir la cámara o cargar archivo
        binding.fabScan.setOnClickListener(v -> {
            showActionSelector();
        });
    }

    private final ActivityResultLauncher<String[]> filePickerLauncher =
        registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                processSelectedFile(uri);
            }
        });

    private void showActionSelector() {
        String[] options = {"📷 Tomar Foto", "📄 Cargar Documento (PDF, Word...)"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Nuevo Escaneo")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                    startActivity(intent);
                } else {
                    filePickerLauncher.launch(new String[]{
                        "application/pdf",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "text/plain",
                        "image/*"
                    });
                }
            }).show();
    }

    private void processSelectedFile(Uri uri) {
        String type = getContentResolver().getType(uri);
        
        if (type != null && type.equals("application/pdf")) {
            String text = DocumentParser.extractTextFromPdf(this, uri);
            if (text != null && text.trim().length() > 50) {
                saveAndOpenDocument(text, "Documento PDF");
            } else {
                processPdfWithAi(uri);
            }
        } else if (type != null && (type.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") || type.equals("application/msword"))) {
            String text = DocumentParser.extractTextFromDocx(this, uri);
            if (text != null && !text.isEmpty()) {
                saveAndOpenDocument(text, "Documento Word");
            } else {
                Toast.makeText(this, "No se pudo extraer texto del Word", Toast.LENGTH_SHORT).show();
            }
        } else if (type != null && type.startsWith("image/")) {
            processImageWithAi(uri);
        } else {
            Toast.makeText(this, "Formato no compatible para extracción", Toast.LENGTH_SHORT).show();
        }
    }

    private void processPdfWithAi(Uri uri) {
        Toast.makeText(this, "Documento complejo. Usando IA...", Toast.LENGTH_SHORT).show();
        try (ParcelFileDescriptor fd = getContentResolver().openFileDescriptor(uri, "r")) {
            PdfRenderer renderer = new PdfRenderer(fd);
            PdfRenderer.Page page = renderer.openPage(0);
            Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();
            renderer.close();
            
            sendImageToGemini(bitmap, "Extrae y resume el contenido de este documento");
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al procesar PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void processImageWithAi(Uri uri) {
        try {
            Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            sendImageToGemini(bitmap, "Extrae y resume el texto de esta imagen");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendImageToGemini(Bitmap bitmap, String prompt) {
        var future = geminiManager.analyzeImage(bitmap, prompt);
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                runOnUiThread(() -> {
                    String text = result.getText();
                    if (text != null && !text.isEmpty()) {
                        saveAndOpenDocument(text, "Escaneo Inteligente");
                    } else {
                        Toast.makeText(MainActivity.this, "La IA no pudo procesar la imagen", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error IA: " + t.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }, getMainExecutor());
    }

    private void saveAndOpenDocument(String text, String defaultTitle) {
        String title = defaultTitle + " " + new java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault()).format(new java.util.Date());
        
        // Intentar categorizar automáticamente
        geminiManager.suggestCategory(text).addListener(() -> {
            String category = "General";
            try {
                GenerateContentResponse res = geminiManager.suggestCategory(text).get();
                if (res.getText() != null) category = res.getText().trim();
            } catch (Exception e) {}
            
            final String finalCat = category;
            DocumentEntry entry = new DocumentEntry(title, text, System.currentTimeMillis(), finalCat, "General", "");
            
            dbExecutor.execute(() -> {
                AppDatabase.getDatabase(this).documentDao().insert(entry);
                runOnUiThread(() -> {
                    Intent intent = new Intent(this, DocumentDetailActivity.class);
                    intent.putExtra("EXTRA_CONTENT", text);
                    intent.putExtra("EXTRA_TITLE", title);
                    startActivity(intent);
                });
            });
        }, getMainExecutor());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbExecutor != null) dbExecutor.shutdown();
    }
}
