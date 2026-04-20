package com.scannote.app.utils;

import android.content.Context;
import android.net.Uri;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import java.io.InputStream;

public class DocumentParser {

    public static void init(Context context) {
        PDFBoxResourceLoader.init(context);
    }

    public static String extractTextFromPdf(Context context, Uri uri) {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            PDDocument document = PDDocument.load(is);
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            document.close();
            return text;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
