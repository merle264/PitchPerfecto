package com.example.karaoke;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StarryBackgroundView extends View {

    private Paint paint;
    private Random random;
    private List<Star> stars;
    private int screenWidth;
    private int screenHeight;

    public StarryBackgroundView(Context context) {
        super(context);
        init();
    }

    public StarryBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StarryBackgroundView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        random = new Random();
        stars = new ArrayList<>();
        generateStars();
    }

    private void generateStars() {
        stars.clear();
        for (int i = 0; i < 100; i++) {
            stars.add(new Star(
                    random.nextFloat() * getWidth(),  // Random X position
                    random.nextFloat() * getHeight(),  // Random Y position
                    random.nextFloat() * 3 + 1,  // Random radius between 1 and 4
                    random.nextFloat() * 2 - 1,  // Random horizontal speed (-1 to 1)
                    random.nextFloat() * 1 + 0.5f  // Random vertical speed (slower than horizontal)
            ));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Get the screen width and height for boundary calculations
        screenWidth = getWidth();
        screenHeight = getHeight();

        // Draw stars (white color)
        paint.setColor(0xFFFFFFFF);  // White for stars
        paint.setAntiAlias(true);

        for (Star star : stars) {
            // Move stars and draw them
            star.x += star.speedX;
            star.y += star.speedY;

            // If the star goes off the screen, reset it to the other side
            if (star.x > screenWidth) star.x = 0;
            if (star.x < 0) star.x = screenWidth;
            if (star.y > screenHeight) star.y = 0;
            if (star.y < 0) star.y = screenHeight;

            // Draw the star as a circle
            canvas.drawCircle(star.x, star.y, star.radius, paint);
        }

        // Invalidate to continuously redraw and animate stars
        invalidate();  // This keeps the stars moving by repeatedly calling onDraw()
    }

    // Star class to hold properties of each star
    private static class Star {
        float x, y, radius;
        float speedX, speedY;

        Star(float x, float y, float radius, float speedX, float speedY) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.speedX = speedX;
            this.speedY = speedY;
        }
    }
}
