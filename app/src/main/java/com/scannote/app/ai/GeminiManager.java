package com.scannote.app.ai;

import android.graphics.Bitmap;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.ListenableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GeminiManager {
    private static final String API_KEY = "AIzaSyD3BnXV3-Gmca-bzH0KlOh_X4uR2NgU7Zg";
    private final GenerativeModelFutures model;

    @Inject
    public GeminiManager() {
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", API_KEY);
        this.model = GenerativeModelFutures.from(gm);
    }

    public ListenableFuture<GenerateContentResponse> summarize(String text) {
        Content content = new Content.Builder()
                .addText("Eres un asistente profesional. Resume de forma seria y DIRECTA el siguiente texto. REGLA ESTRICTA: El resumen DEBE SER EXTREMADAMENTE CORTO. Máximo 4 puntos en viñetas <ul>, y cada viñeta NO debe tener más de 15 palabras. Ve directo al grano. Devuelve ÚNICAMENTE código HTML puro (sin bloques ```html o markdown). Utiliza títulos <h2>. NO uses emojis. NO apliques atributos 'style' en HTML. Agrupa la información en un <div class='card'>:\n\n" + text)
                .build();
        return model.generateContent(content);
    }

    public ListenableFuture<GenerateContentResponse> generateExamQuestions(String text) {
        Content content = new Content.Builder()
                .addText("Eres un tutor riguroso. Crea 3 preguntas de opción múltiple interactivas. REGLA ESTRICTA: Las preguntas y CADA OPCIÓN deben ser ULTRA CORTAS (máximo 8 palabras por botón). Devuelve ÚNICAMENTE código HTML puro (sin ```html ni markdown). Envuelve cada pregunta en un <div class='card'>. Usa <h3>. Para opciones usa <button>. Si es correcta añade onclick=\"this.className='correct'\", si es falsa añade onclick=\"this.className='wrong'\". NO uses emojis ni atributos 'style'. Sé breve y muy directo:\n\n" + text)
                .build();
        return model.generateContent(content);
    }

    public ListenableFuture<GenerateContentResponse> correctText(String text) {
        Content content = new Content.Builder()
                .addText("El siguiente texto fue reconocido por OCR (reconocimiento óptico de caracteres) y puede contener errores de reconocimiento, palabras mal escritas, letras confundidas (como 'l' por '1', 'o' por '0', etc.) o palabras cortadas. Corrige todos los errores preservando el significado original. Devuelve ÚNICAMENTE el texto corregido, sin explicaciones:\n\n" + text)
                .build();
        return model.generateContent(content);
    }

    public ListenableFuture<GenerateContentResponse> translateText(String text) {
        Content content = new Content.Builder()
                .addText("Traduce de forma estrictamente profesional el siguiente texto al inglés (o al español si ya está en inglés). Devuelve ÚNICAMENTE código HTML puro (sin ```html). Presenta el resultado dentro de un <div class='card'> con texto en <i> o <b> y un título <h2>Traducción</h2>. NO uses estilos en línea corporales ni emojis. Mantén seriedad absoluta:\n\n" + text)
                .build();
        return model.generateContent(content);
    }

    public ListenableFuture<GenerateContentResponse> extractKeywords(String text) {
        Content content = new Content.Builder()
                .addText("Extrae de forma seria los conceptos principales. REGLA ESTRICTA: Sé EXTREMADAMENTE BREVE. Cada concepto explicado en 10 palabras máximo. Devuelve ÚNICAMENTE código HTML puro (sin markdown ni ```html). Usa <div class='card'>. Usa <h2> para encabezados y <ul> para las listas. NO incluyas emojis. NO apliques atributos CSS en línea ('style'). Tono académico y ultra-conciso:\n\n" + text)
                .build();
        return model.generateContent(content);
    }

    public ListenableFuture<GenerateContentResponse> askQuestion(String context, String question) {
        Content content = new Content.Builder()
                .addText("Basándote en el siguiente contexto:\n\n" + context + "\n\nResponde de forma seria y concisa a la siguiente pregunta: " + question + "\n\nDevuelve ÚNICAMENTE código HTML puro (sin ```html ni markdown). Envuelve la respuesta en un <div class='card'>. Usa títulos <h2> si es necesario. NO uses emojis.")
                .build();
        return model.generateContent(content);
    }

    public ListenableFuture<GenerateContentResponse> analyzeImage(Bitmap bitmap, String prompt) {
        Content content = new Content.Builder()
                .addImage(bitmap)
                .addText(prompt + ". REGLA: Extrae el texto si es posible o resume lo que ves de forma seria. Devuelve ÚNICAMENTE el texto extraído o el resumen, sin formato markdown complejo.")
                .build();
        return model.generateContent(content);
    }
}
