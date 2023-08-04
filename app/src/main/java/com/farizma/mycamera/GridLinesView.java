package com.farizma.mycamera;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
public class GridLinesView extends View {
    private Paint paint;
    private boolean showGridLines = true;


    public GridLinesView(Context context) {
        super(context);
        init();
    }

    public GridLinesView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(1);
    }

    public void setShowGridLines(boolean show) {
        showGridLines = show;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (showGridLines) {
            int width = getWidth();
            int height = getHeight();

            int horizontalGap = width / 3;
            int verticalGap = height / 3;

            for (int i = 1; i < 3; i++) {
                // Draw horizontal grid lines
                canvas.drawLine(0, i * verticalGap, width, i * verticalGap, paint);
                // Draw vertical grid lines
                canvas.drawLine(i * horizontalGap, 0, i * horizontalGap, height, paint);
            }
        }
    }
}
