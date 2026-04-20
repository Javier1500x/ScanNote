package com.scannote.app.ui.detail;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.scannote.app.ai.GeminiManager;
import com.scannote.app.databinding.ActivityDocumentDetailBinding;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DocumentDetailActivity extends AppCompatActivity {

    private ActivityDocumentDetailBinding binding;
    @Inject GeminiManager geminiManager;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDocumentDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String content = getIntent().getStringExtra("EXTRA_CONTENT");
        if (content == null) content = "";
        binding.editContent.setText(content);

        // Fecha actual
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        binding.textScanDate.setText(sdf.format(new Date()));

        // Contador de palabras dinámico
        updateWordCount(content);
        final String[] currentText = {content};
        binding.editContent.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                currentText[0] = s.toString();
                updateWordCount(currentText[0]);
            }
        });

        // Botón atrás
        if (binding.btnBackDetail != null) {
            binding.btnBackDetail.setOnClickListener(v -> finish());
        }

        // Botones IA
        binding.btnSummarize.setOnClickListener(v -> callGemini("summarize"));
        binding.btnStudy.setOnClickListener(v -> callGemini("study"));
        binding.btnCorrect.setOnClickListener(v -> callGemini("correct"));
        binding.btnKeywords.setOnClickListener(v -> callGemini("keywords"));
        binding.btnTranslate.setOnClickListener(v -> callGemini("translate"));

        // Nuevas funciones
        binding.btnAskAi.setOnClickListener(v -> {
            binding.layoutAskInput.setVisibility(
                binding.layoutAskInput.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE
            );
        });

        binding.btnSendQuestion.setOnClickListener(v -> {
            String q = binding.editAskQuestion.getText().toString().trim();
            if (!q.isEmpty()) {
                callGemini("ask:" + q);
                binding.editAskQuestion.setText("");
            }
        });

        binding.btnExportPdf.setOnClickListener(v -> showExportOptions());

        // TTS
        tts = new TextToSpeech(this, status -> {
            ttsReady = (status != TextToSpeech.ERROR);
            if (ttsReady) tts.setLanguage(new Locale("es", "ES"));
        });

        binding.btnTts.setOnClickListener(v -> {
            if (!ttsReady) {
                Toast.makeText(this, "TTS no disponible", Toast.LENGTH_SHORT).show();
                return;
            }
            String text = binding.editContent.getText().toString();
            if (text.isEmpty()) {
                Toast.makeText(this, "No hay texto para leer", Toast.LENGTH_SHORT).show();
                return;
            }
            if (tts.isSpeaking()) {
                tts.stop();
                binding.btnTts.setText("Escuchar");
            } else {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_utterance");
                binding.btnTts.setText("Detener");
            }
        });
    }

    private void updateWordCount(String text) {
        if (text == null || text.trim().isEmpty()) {
            binding.textWordCount.setText("0 palabras");
            return;
        }
        int words = text.trim().split("\\s+").length;
        binding.textWordCount.setText(words + (words == 1 ? " palabra" : " palabras"));
    }

    private void callGemini(String action) {
        String text = binding.editContent.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "El texto está vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        // Actualizar etiqueta del resultado según la acción
        String label;
        switch (action) {
            case "summarize":  label = "📄 Resumen";            break;
            case "study":      label = "📚 Preguntas de Estudio"; break;
            case "correct":    label = "✏️ Texto Corregido";    break;
            case "keywords":   label = "🔑 Palabras Clave";     break;
            case "translate":  label = "🌐 Traducción";         break;
            default:           label = "✦ Resultado IA";        break;
        }
        binding.textAiLabel.setText(label);

        binding.progressAi.setVisibility(View.VISIBLE);
        binding.layoutAiOutput.setVisibility(View.GONE);

        var future = switch (action) {
            case "summarize" -> geminiManager.summarize(text);
            case "study"     -> geminiManager.generateExamQuestions(text);
            case "correct"   -> geminiManager.correctText(text);
            case "keywords"  -> geminiManager.extractKeywords(text);
            case "translate" -> geminiManager.translateText(text);
            default -> {
                if (action.startsWith("ask:")) {
                    yield geminiManager.askQuestion(text, action.substring(4));
                }
                yield geminiManager.summarize(text);
            }
        };

        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                runOnUiThread(() -> {
                    binding.progressAi.setVisibility(View.GONE);
                    String output = result.getText();
                    if (output == null) output = "Sin respuesta de la IA.";

                    // Si es corrección, actualizar directamente el texto
                    if (action.equals("correct")) {
                        binding.editContent.setText(output.trim());
                        Toast.makeText(DocumentDetailActivity.this,
                                "Texto corregido", Toast.LENGTH_SHORT).show();
                    } else {
                        String cleanHtml = output.replace("```html", "").replace("```", "").trim();
                        String htmlTemplate = "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
                            "<style>" +
                            "body { background-color: transparent !important; color: #E2E8F0 !important; font-family: sans-serif; line-height: 1.6; padding: 0; margin: 0; }" +
                            "div:not(.card), p, span, li, ul, ol { color: #E2E8F0 !important; background-color: transparent !important; font-size: 15px !important; }" +
                            "h1, h2, h3, h4 { color: #38BDF8 !important; font-size: 18px !important; margin-top: 12px !important; margin-bottom: 8px !important; font-weight: 600 !important; background-color: transparent !important; }" +
                            ".card { background-color: #1E293B !important; padding: 16px !important; border-radius: 12px !important; margin-bottom: 12px !important; border: 1px solid #334155 !important; color: #E2E8F0 !important; }" +
                            "button { background: #334155 !important; color: white !important; border: 1px solid #475569 !important; padding: 12px 16px !important; border-radius: 8px !important; margin: 6px 0 !important; font-size: 14px !important; cursor: pointer; width: 100% !important; text-align: left !important; display: block !important; font-family: sans-serif !important; }" +
                            ".correct { background: #10B981 !important; color: white !important; border-color: #059669 !important; font-weight: bold !important; }" +
                            ".wrong { background: #EF4444 !important; color: white !important; border-color: #B91C1C !important; text-decoration: line-through !important; }" +
                            "hr { border: 0 !important; height: 1px !important; background-color: #334155 !important; margin: 16px 0 !important; }" +
                            "b, strong { color: #FFFFFF !important; font-weight: 700 !important; }" +
                            "</style></head><body>" +
                            cleanHtml +
                            "</body></html>";

                        binding.webviewAiOutput.getSettings().setJavaScriptEnabled(true);
                        binding.webviewAiOutput.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                        binding.webviewAiOutput.loadDataWithBaseURL(null, htmlTemplate, "text/html", "UTF-8", null);
                        binding.layoutAiOutput.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> {
                    binding.progressAi.setVisibility(View.GONE);
                    String msg = t.getMessage();
                    if (msg == null) msg = "Error desconocido";
                    Toast.makeText(DocumentDetailActivity.this,
                            "Error: " + msg, Toast.LENGTH_LONG).show();
                });
            }
        }, getMainExecutor());
    }

    private void showExportOptions() {
        String[] options = {"Compartir Texto", "Exportar a PDF", "Enviar por WhatsApp"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Exportar / Compartir")
            .setItems(options, (dialog, which) -> {
                String text = binding.editContent.getText().toString();
                switch (which) {
                    case 0: shareText(text); break;
                    case 1: exportToPdf(text); break;
                    case 2: shareWhatsApp(text); break;
                }
            }).show();
    }

    private void shareText(String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, "Compartir con..."));
    }

    private void shareWhatsApp(String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.setPackage("com.whatsapp");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp no instalado", Toast.LENGTH_SHORT).show();
        }
    }

    private void exportToPdf(String text) {
        try {
            File pdfFile = new File(getExternalFilesDir(null), "ScanNote_Export.pdf");
            PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            
            document.add(new Paragraph("ScanNote - Exportación").setFontSize(20));
            document.add(new Paragraph("\n" + text));
            document.close();

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", pdfFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Guardar PDF..."));

        } catch (Exception e) {
            Toast.makeText(this, "Error al crear PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
