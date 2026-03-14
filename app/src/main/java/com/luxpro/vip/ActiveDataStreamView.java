package com.luxpro.vip;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

public class ActiveDataStreamView extends View {

    private Paint paint;
    private int width, height;
    private int textSize = 28;
    private char[] chars = "01アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワン23456789ABCDEFGHIJKLMNOPQRSTUVWXYZLUXPRO"
            .toCharArray();
    private int[] drops;
    private Random random = new Random();

    public ActiveDataStreamView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        // Cybernetic Purple/Cyan neon text
        paint.setColor(Color.parseColor("#33DF00FF"));
        paint.setTextSize(textSize);
        paint.setAntiAlias(true);
        // Add a small neon glow to text
        paint.setShadowLayer(5f, 0, 0, Color.parseColor("#DF00FF"));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;

        int columns = width / textSize;
        drops = new int[columns];
        for (int i = 0; i < drops.length; i++) {
            drops[i] = random.nextInt(height / textSize);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw black background with high transparency so nebula shows through
        canvas.drawColor(Color.parseColor("#1A000000"));

        for (int i = 0; i < drops.length; i++) {
            String text = String.valueOf(chars[random.nextInt(chars.length)]);

            // Randomly highlight some chars in Cyan for that true 'alien OS' look
            if (random.nextInt(10) > 8) {
                paint.setColor(Color.parseColor("#5500FFFF"));
                paint.setShadowLayer(8f, 0, 0, Color.parseColor("#00FFFF"));
            } else {
                paint.setColor(Color.parseColor("#33DF00FF"));
                paint.setShadowLayer(5f, 0, 0, Color.parseColor("#DF00FF"));
            }

            canvas.drawText(text, i * textSize, drops[i] * textSize, paint);

            // Reset drops if they reach end of screen or randomly
            if (drops[i] * textSize > height && random.nextDouble() > 0.975) {
                drops[i] = 0;
            }
            drops[i]++;
        }

        // Cap the frame rate around 30FPS for the smooth falling effect without lagging
        // the UI
        postInvalidateDelayed(33);
    }
}
