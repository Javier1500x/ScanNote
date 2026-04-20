package com.scannote.app.utils;

import android.content.Context;
import android.net.Uri;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static String extractTextFromDocx(Context context, Uri uri) {
        try (InputStream is = context.getContentResolver().openInputStream(uri);
             ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("word/document.xml")) {
                    StringBuilder sb = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zis));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    // Simple regex to extract text from XML tags <w:t>
                    String xml = sb.toString();
                    Pattern p = Pattern.compile("<w:t[^>]*>(.*?)</w:t>");
                    Matcher m = p.matcher(xml);
                    StringBuilder text = new StringBuilder();
                    while (m.find()) {
                        text.append(m.group(1)).append(" ");
                    }
                    return text.toString().trim();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
