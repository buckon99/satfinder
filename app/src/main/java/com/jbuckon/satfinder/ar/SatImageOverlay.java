package com.jbuckon.satfinder.ar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;

final public class SatImageOverlay extends android.support.v7.widget.AppCompatImageView {
    float rToD = (float) (180 / Math.PI);
    float dToR = (float) (Math.PI / 180);
    float textSize;
    float xDim, yDim;
    float xAngle, yAngle;
    float scalingFactor;
    int[] satColors = {Color.rgb(0, 64, 0), Color.rgb(64, 0, 0), Color.rgb(0, 0, 64),
            Color.rgb(64, 64, 0), Color.rgb(64, 0, 64)};
    int[] lightSatColors = {Color.rgb(128, 255, 128), Color.rgb(255, 128, 128), Color.rgb(128, 128, 255),
            Color.rgb(255, 255, 128), Color.rgb(255, 128, 255)};

    SatFinderAndroidActivity activity = null;

    public SatImageOverlay(Context context) {
        super(context);
        setup(context);
    }

    public SatImageOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    public SatImageOverlay(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setup(context);
    }

    private void setup(Context context) {
        activity = (SatFinderAndroidActivity) context;
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawOverlay(canvas);
    }

    protected void drawOverlay(Canvas canvas) {

        textSize = activity.metrics.density * 22;
        canvas.save();

        Rect rect = canvas.getClipBounds();
        xDim = rect.right;
        yDim = rect.bottom;
        if (xDim == 0 || yDim == 0) {
            return;
        }
        switch (activity.cameraOrientation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                xAngle = activity.cameraYAngle;
                yAngle = activity.cameraXAngle;
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                xAngle = activity.cameraXAngle;
                yAngle = activity.cameraYAngle;
                break;
            default:
                Log.e("Error", "No orientation!");
                break;
        }
        scalingFactor = .9f;
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(3);
        paint.setStyle(Paint.Style.FILL);
        int c = (activity.lightColorScheme) ? Color.rgb(128, 128, 255) : Color.rgb(0, 0, 128);
        paint.setColor(c);
        float vOffset = scaler(-activity.sensorPitch, yAngle, yDim) / scalingFactor;
        canvas.translate(xDim / 2.0f, (yDim / 2.0f) + vOffset);
        canvas.rotate(-activity.sensorRoll * rToD);
        paint.setStrokeWidth(4);
        // deliberately excessive line length
        canvas.drawLine(-xDim, 0, xDim, 0, paint);
        paint.setStrokeWidth(1);
        paint.setTextSize(textSize);
        paint.setStyle(Paint.Style.FILL);
        float len = paint.measureText("Horizon") / 2;
        canvas.drawText("Horizon", -len, -paint.ascent(), paint);
        drawCompassScale(canvas, paint);
        drawSats(canvas, paint);
        canvas.restore();
        invalidate();
    }

    // arguments radians

    private float convertAzimuth(double a, double magDec) {
        double az = activity.sensorAzimuth + magDec - Math.PI/2;
        double xp = (float) (a - az);
        xp = (xp + Math.PI * 4) % (Math.PI * 2) - Math.PI/2;
        return scaler((float) (xp), xAngle, xDim);
    }

    private void drawCompassScale(Canvas canvas, Paint paint) {
        int c = (activity.lightColorScheme) ? Color.rgb(255, 128, 128) : Color.rgb(128, 0, 0);
        paint.setColor(c);
        for (int a = 0; a < 360; a += 20) {
            float az = convertAzimuth(a * dToR, activity.useMagDec);
            String s = String.format("%d°", a);
            float len = paint.measureText(s) / 2;
            canvas.drawText(s, az - len, -paint.descent(), paint);
        }
    }

    private void drawSats(Canvas canvas, Paint paint) {
        paint.setTextSize(textSize);
        int i = 0;
        int c = (activity.lightColorScheme) ? lightSatColors[i % satColors.length] : satColors[i % satColors.length];

        drawSat(canvas, paint, 180 * dToR, 60 * dToR,
                "test", c);
        if (false) {
            // extra data for calibration
            paint.setColor(Color.rgb(64, 0, 0));
            float xstep = 20;
            float ystep = 20;
            for (float x = 0; x < 360; x += xstep) {
                for (float y = 0; y < 60; y += ystep) {
                    drawSat(canvas, paint, x * dToR, y * dToR, String.format("%.0f/%.0f°", x, y), Color.RED);
                }
            }
        }
    }

    private void drawSat(Canvas canvas, Paint paint, double az, double el,
                         String name, int col) {
        int radius = (int) xDim / 128;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(col);
        float alt = scaler((float) -el, yAngle, yDim);
        float xp = convertAzimuth(az, activity.magDec);
        canvas.drawCircle(xp, alt, radius, paint);
        float len = paint.measureText(name) / 2;
        canvas.drawText(name, xp - len, alt - (radius + paint.descent()), paint);
    }

    private float scaler(float angle, float maxAngle, float size) {
        return angle * size * scalingFactor / maxAngle;
    }

}
