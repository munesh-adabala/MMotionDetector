package com.example.opencv_mobile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class BoundingBoxView extends View {
    private static final String TAG="BoundingBoxView";

    private RectF rect;
    float[] landmarkPoints;
    double[][] landMarkMultiDimArray;
    private boolean clearCanvas = false;
    private final Paint rectPaint;
    private float cx, cy;
    private RectF location;

    public BoundingBoxView(final Context context, final AttributeSet set) {
        super(context, set);
        rectPaint = new Paint();
        rectPaint.setColor(Color.RED);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(5);
        rectPaint.setAntiAlias(true);
    }

    public void setResults(RectF location) {
        this.location=location;
        postInvalidate();
    }

    @Override
    public void onDraw(final Canvas canvas) {
        //denso changes
        if (location != null) {
            canvas.drawRect(location,rectPaint);
        }else{
            canvas.drawColor(Color.TRANSPARENT);
        }
    }
}