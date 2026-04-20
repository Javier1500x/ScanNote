package com.scannote.app.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ScanningOverlayView extends View {

    // Paints para los bounding boxes del texto detectado
    private final Paint textBoxPaint = new Paint();
    // Paint para las esquinas del visor
    private final Paint cornerPaint = new Paint();
    // Paint para el fondo oscuro fuera del ROI
    private final Paint dimPaint = new Paint();
    // Paint para el borde del ROI
    private final Paint roiBorderPaint = new Paint();

    private List<Rect> rects = new ArrayList<>();
    private float animPhase = 0f;
    private final Runnable animRunnable = new Runnable() {
        @Override
        public void run() {
            animPhase += 0.08f;
            if (animPhase > 1f) animPhase = 0f;
            invalidate();
            postDelayed(this, 60);
        }
    };

    public ScanningOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaints();
        post(animRunnable);
    }

    private void initPaints() {
        // Paint para los cuadros de texto detectado
        textBoxPaint.setColor(Color.parseColor("#14B8A6"));
        textBoxPaint.setStyle(Paint.Style.STROKE);
        textBoxPaint.setStrokeWidth(3f);
        textBoxPaint.setAlpha(200);
        textBoxPaint.setPathEffect(new CornerPathEffect(4f));
        textBoxPaint.setAntiAlias(true);

        // Paint para las esquinas decorativas del visor
        cornerPaint.setColor(Color.WHITE);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(4f);
        cornerPaint.setStrokeCap(Paint.Cap.ROUND);
        cornerPaint.setAntiAlias(true);

        // Paint para el overlay oscuro
        dimPaint.setColor(Color.parseColor("#99000000"));
        dimPaint.setStyle(Paint.Style.FILL);

        // Paint para el borde del ROI
        roiBorderPaint.setColor(Color.parseColor("#4414B8A6"));
        roiBorderPaint.setStyle(Paint.Style.STROKE);
        roiBorderPaint.setStrokeWidth(1.5f);
        roiBorderPaint.setPathEffect(new DashPathEffect(new float[]{12, 8}, 0));
        roiBorderPaint.setAntiAlias(true);
    }

    public void updateRects(List<Rect> newRects) {
        this.rects = newRects;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        // Zona del ROI (95% para mayor captura)
        float roiLeft = w * 0.04f;
        float roiTop = h * 0.10f;
        float roiRight = w * 0.96f;
        float roiBottom = h * 0.82f;

        // Dim las áreas fuera del ROI
        canvas.drawRect(0, 0, w, roiTop, dimPaint);
        canvas.drawRect(0, roiBottom, w, h, dimPaint);
        canvas.drawRect(0, roiTop, roiLeft, roiBottom, dimPaint);
        canvas.drawRect(roiRight, roiTop, w, roiBottom, dimPaint);

        // Borde punteado del ROI
        RectF roiRect = new RectF(roiLeft, roiTop, roiRight, roiBottom);
        canvas.drawRoundRect(roiRect, 12f, 12f, roiBorderPaint);

        // Esquinas del visor
        float cornerLen = 36f;
        float cStroke = 4f;
        cornerPaint.setAlpha(220);

        // Top-left
        canvas.drawLine(roiLeft, roiTop + cornerLen, roiLeft, roiTop, cornerPaint);
        canvas.drawLine(roiLeft, roiTop, roiLeft + cornerLen, roiTop, cornerPaint);
        // Top-right
        canvas.drawLine(roiRight - cornerLen, roiTop, roiRight, roiTop, cornerPaint);
        canvas.drawLine(roiRight, roiTop, roiRight, roiTop + cornerLen, cornerPaint);
        // Bottom-left
        canvas.drawLine(roiLeft, roiBottom - cornerLen, roiLeft, roiBottom, cornerPaint);
        canvas.drawLine(roiLeft, roiBottom, roiLeft + cornerLen, roiBottom, cornerPaint);
        // Bottom-right
        canvas.drawLine(roiRight - cornerLen, roiBottom, roiRight, roiBottom, cornerPaint);
        canvas.drawLine(roiRight, roiBottom, roiRight, roiBottom - cornerLen, cornerPaint);

        // Línea de escaneo animada (pulso)
        float scanY = roiTop + (roiBottom - roiTop) * ((animPhase % 1f));
        Paint scanLinePaint = new Paint();
        int alpha = (int)(80 + 100 * Math.abs(Math.sin(animPhase * Math.PI)));
        scanLinePaint.setColor(Color.parseColor("#14B8A6"));
        scanLinePaint.setAlpha(alpha);
        scanLinePaint.setStrokeWidth(2f);
        scanLinePaint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(roiLeft + 8, scanY, roiRight - 8, scanY, scanLinePaint);

        // Cuadros del texto detectado
        for (Rect rect : rects) {
            if (rect != null) {
                canvas.drawRect(rect, textBoxPaint);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(animRunnable);
    }
}
