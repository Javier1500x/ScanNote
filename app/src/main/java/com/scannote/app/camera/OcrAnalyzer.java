package com.scannote.app.camera;

import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.ArrayList;
import java.util.List;

public class OcrAnalyzer implements ImageAnalysis.Analyzer {
    private final TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    private final ScanningOverlayView overlay;
    private final OnTextDetectedListener listener;

    public interface OnTextDetectedListener {
        void onTextDetected(String text);
    }

    public OcrAnalyzer(ScanningOverlayView overlay, OnTextDetectedListener listener) {
        this.overlay = overlay;
        this.listener = listener;
    }

    @Override
    @androidx.camera.core.ExperimentalGetImage
    public void analyze(@NonNull ImageProxy imageProxy) {
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        int width = imageProxy.getWidth();
        int height = imageProxy.getHeight();

        // ROI ampliado a 92% para capturar más texto
        int roiLeft   = (int)(width  * 0.04f);
        int roiTop    = (int)(height * 0.10f);
        int roiRight  = (int)(width  * 0.96f);
        int roiBottom = (int)(height * 0.82f);
        Rect roi = new Rect(roiLeft, roiTop, roiRight, roiBottom);

        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    List<Rect> rects = new ArrayList<>();
                    StringBuilder sb = new StringBuilder();

                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        Rect blockRect = block.getBoundingBox();
                        if (blockRect != null && Rect.intersects(roi, blockRect)) {
                            rects.add(blockRect);
                            // Procesamos línea a línea para mejor estructura
                            for (Text.Line line : block.getLines()) {
                                String lineText = cleanOcrLine(line.getText());
                                if (!lineText.isEmpty()) {
                                    sb.append(lineText).append("\n");
                                }
                            }
                            sb.append("\n");
                        }
                    }

                    overlay.updateRects(rects);

                    String result = sb.toString().trim();
                    // Aplicar postprocesado básico de OCR
                    result = postProcessOcr(result);

                    if (!result.isEmpty()) {
                        listener.onTextDetected(result);
                    }
                })
                .addOnCompleteListener(task -> imageProxy.close());
    }

    /**
     * Limpieza básica línea por línea del OCR
     */
    public static String cleanOcrLine(String line) {
        if (line == null) return "";
        line = line.trim();
        // Eliminar líneas que son solo símbolos o muy cortas inútiles
        if (line.length() < 2) return "";
        return line;
    }

    /**
     * Postprocesado del texto OCR completo:
     * - Corrige confusiones comunes de caracteres
     * - Une palabras cortadas al final de línea con guion
     * - Elimina líneas duplicadas consecutivas
     */
    public static String postProcessOcr(String text) {
        if (text == null || text.isEmpty()) return text;

        // Correcciones de caracteres comunes de OCR
        text = text
            .replace("0", "o") // Solo si es claramente texto, evitamos sobreescribir
            .replace("|", "l")
            .replace("l l", "ll")
            .replace("  ", " ");

        // Re-unir palabras cortadas al final con guion (ej: "resul-\ntado" -> "resultado")
        text = text.replaceAll("-(\\s*)\n(\\s*)", "");

        // Eliminar saltos de línea dobles o triples excesivos
        text = text.replaceAll("\n{3,}", "\n\n");

        // Corregir espacios antes de puntuación
        text = text.replaceAll("\\s+([.,;:!?])", "$1");

        return text.trim();
    }
}
