package tom.turmtechnik;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.Calendar;

/**
 * Analoguhr-View für den Bildschirmschoner (portiert aus FashionClockAnalogClean).
 * Schwarzer Hintergrund, weißer Ziffernblatt-Ring, Striche, 12/3/6/9, Zeiger, Datum.
 * Die ganze Uhr wandert langsam über den Bildschirm, um Einbrennen zu vermeiden.
 */
public class AnalogClockView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint datePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Calendar cal = Calendar.getInstance();

    /** Anteil der Breite/Höhe, um den die Uhr maximal wandert (z. B. 0,15 = 15 %). */
    private static final float WANDER_MARGIN = 0.18f;
    /** Geschwindigkeit der Wanderung (kleiner = langsamer). */
    private static final double WANDER_SPEED = 0.12;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            invalidate();
            postDelayed(this, 16L);
        }
    };

    public AnalogClockView(Context context) {
        super(context);
        init();
    }

    public AnalogClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(4f);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        datePaint.setColor(Color.WHITE);
        datePaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        removeCallbacks(ticker);
        post(ticker);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(ticker);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float minDim = Math.min(w, h);
        double t = System.currentTimeMillis() * 0.001 * WANDER_SPEED;
        float dx = (float) (minDim * WANDER_MARGIN * Math.sin(t));
        float dy = (float) (minDim * WANDER_MARGIN * Math.cos(t * 0.73));
        float cx = w / 2f + dx;
        float cy = h / 2f + dy;
        float radius = minDim * 0.32f;

        canvas.drawColor(Color.BLACK);

        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(radius * 0.02f);
        canvas.drawCircle(cx, cy, radius, paint);

        tickPaint.setColor(Color.WHITE);
        for (int i = 0; i < 60; i++) {
            double angle = Math.toRadians((i * 6) - 90.0);
            boolean isHour = (i % 5) == 0;
            float inner = radius * (isHour ? 0.8f : 0.88f);
            float x1 = (float) (cx + inner * Math.cos(angle));
            float y1 = (float) (cy + inner * Math.sin(angle));
            float x2 = (float) (cx + radius * Math.cos(angle));
            float y2 = (float) (cy + radius * Math.sin(angle));
            tickPaint.setStrokeWidth(isHour ? radius * 0.015f : radius * 0.006f);
            canvas.drawLine(x1, y1, x2, y2, tickPaint);
        }

        textPaint.setTextSize(radius * 0.15f);
        canvas.drawText("12", cx, cy - radius * 0.62f, textPaint);
        canvas.drawText("3", cx + radius * 0.62f, cy + textPaint.getTextSize() * 0.35f, textPaint);
        canvas.drawText("6", cx, cy + radius * 0.75f, textPaint);
        canvas.drawText("9", cx - radius * 0.62f, cy + textPaint.getTextSize() * 0.35f, textPaint);

        cal.setTimeInMillis(System.currentTimeMillis());
        int hour = cal.get(Calendar.HOUR);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);
        int millis = cal.get(Calendar.MILLISECOND);

        double secAngle = ((second + millis / 1000.0) / 60.0) * 2.0 * Math.PI - Math.PI / 2;
        double minAngle = ((minute + second / 60.0) / 60.0) * 2.0 * Math.PI - Math.PI / 2;
        double hourAngle = (((hour % 12) + minute / 60.0) / 12.0) * 2.0 * Math.PI - Math.PI / 2;

        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(radius * 0.035f);
        drawHand(canvas, cx, cy, hourAngle, radius * 0.5f, paint);

        paint.setStrokeWidth(radius * 0.025f);
        drawHand(canvas, cx, cy, minAngle, radius * 0.72f, paint);

        Paint secPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        secPaint.setColor(Color.RED);
        secPaint.setStyle(Paint.Style.STROKE);
        secPaint.setStrokeWidth(radius * 0.012f);
        secPaint.setStrokeCap(Paint.Cap.ROUND);
        drawHand(canvas, cx, cy, secAngle, radius * 0.8f, secPaint);

        Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerPaint.setColor(Color.WHITE);
        centerPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, radius * 0.03f, centerPaint);

        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);
        datePaint.setTextSize(radius * 0.25f);
        datePaint.setColor(Color.WHITE);
        datePaint.setStyle(Paint.Style.FILL);
        String dateStr = String.format("%02d.%02d.%04d", day, month, year);
        float dateY = cy - radius * 1.15f;
        canvas.drawText(dateStr, cx, dateY, datePaint);
    }

    private void drawHand(Canvas canvas, float cx, float cy, double angle, float length, Paint p) {
        float x = (float) (cx + length * Math.cos(angle));
        float y = (float) (cy + length * Math.sin(angle));
        canvas.drawLine(cx, cy, x, y, p);
    }
}
