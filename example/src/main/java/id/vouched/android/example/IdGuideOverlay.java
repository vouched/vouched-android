package id.vouched.android.example;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
/*
 * simple overlay, so that the user is guided to maximize ID image
 */
public class IdGuideOverlay extends View {
    private Paint guidePaint;
    private final double widthPercent = .70;
    private final double aspectRatio = 1.60;
    private final float cornerRadius = 40;

    public IdGuideOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaint();
    }

    private void initPaint() {
        Paint guide = new Paint();
        guide.setColor(Color.WHITE);
        guide.setStyle(Paint.Style.STROKE);
        guide.setStrokeWidth(8);
        guidePaint = guide;
    }

    @Override
    public void onDraw(Canvas canvas) {
        float centerX = getWidth()/2;
        float centerY = getHeight()/2;
        float halfGuideLength = (int) (getWidth()/2 * widthPercent);
        float halfGuideHeight = (int) (halfGuideLength * aspectRatio);

        canvas.drawRoundRect(
            centerX - halfGuideLength,
            centerY - halfGuideHeight,
            centerX + halfGuideLength,
            centerY + halfGuideHeight,
            cornerRadius,
            cornerRadius,
            guidePaint);
    }

}